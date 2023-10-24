package org.veupathdb.service.eda.merge.plugins.transforms;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.EcmaScriptExpressionEvalConfig;
import org.veupathdb.service.eda.generated.model.VariableReference;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.derivedvars.Transform;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EcmaScriptExpressionEval extends Transform<EcmaScriptExpressionEvalConfig> {

  private static final String JS_FUNCTION_NAME = "evalExpression";

  private String _scriptExpression;
  private List<VariableReference> _variableRefs;
  private APIVariableType _expectedType;
  private APIVariableDataShape _expectedShape;
  private boolean _nullResultOnAnyMissingInput;
  private ScriptEngine _engine;
  private List<TwoTuple<String, APIVariableType>> _scriptParams;

  @Override
  protected Class<EcmaScriptExpressionEvalConfig> getConfigClass() {
    return EcmaScriptExpressionEvalConfig.class;
  }

  @Override
  protected void acceptConfig(EcmaScriptExpressionEvalConfig config) throws ValidationException {
    _scriptExpression = config.getEcmaScriptExpression();
    _variableRefs = config.getInputVariables();
    _expectedType = config.getExpectedType();
    _expectedShape = config.getExpectedShape();
    _nullResultOnAnyMissingInput = config.getNullResultOnAnyMissingInput();
  }

  @Override
  protected void performSupplementalDependedVariableValidation() throws ValidationException {
    try {
      for (VariableSpec spec : getRequiredInputVars()) {
        checkVariable("Input variable", spec, List.of(
            // only allow these input types for now
            APIVariableType.INTEGER,
            APIVariableType.NUMBER,
            APIVariableType.STRING
        ), null); // any shape is fine
      }
      // need to wait and set up the script engine here since we need to know the types of the input vars
      _engine = GraalJSScriptEngine.create(null,
        Context.newBuilder("js")
          .allowHostAccess(HostAccess.ALL)
          .allowHostClassLookup(s -> true)
          .allowAllAccess(true));
      String parameterList = _variableRefs.stream().map(VariableReference::getName).collect(Collectors.joining(", "));
      _engine.eval("function " + JS_FUNCTION_NAME + "(" + parameterList + ") { return " + _scriptExpression + "; }");
      _scriptParams = _variableRefs.stream()
          .map(ref -> _metadata.getVariable(ref.getVariable()).orElseThrow())
          .map(var -> new TwoTuple<>(VariableDef.toDotNotation(var), var.getType()))
          .toList();
    }
    catch (ScriptException e) {
      throw new ValidationException("JavaScript expression is not valid: " + _scriptExpression);
    }
  }

  @Override
  public String getFunctionName() {
    return "ecmaScriptExpressionEval";
  }

  @Override
  public List<VariableSpec> getRequiredInputVars() {
    return _variableRefs.stream().map(VariableReference::getVariable).toList();
  }

  @Override
  public APIVariableType getVariableType() {
    return _expectedType;
  }

  @Override
  public APIVariableDataShape getDataShape() {
    return _expectedShape;
  }

  /*************************************************************
   * TODO: TBD what about range, vocab, units?
   *************************************************************/

  @Override
  public String getValue(Map<String, String> row) {
    return getParameters(row).map(this::callFunction).orElse(EMPTY_VALUE);
  }

  /**
   * Gathers the needed expression parameters from this row of data and converts them
   * into their native data types.  May return an empty optional if null result flag is
   * true and any columns have empty values.
   *
   * @param row row of data
   * @return parameters for the JS expression function
   */
  private Optional<List<Object>> getParameters(Map<String, String> row) {
    List<Object> parameters = new ArrayList<>();
    for (TwoTuple<String, APIVariableType> var : _scriptParams) {
      String valueStr = row.get(var.getKey());
      if (valueStr.isEmpty()) {
        if (_nullResultOnAnyMissingInput)
          return Optional.empty();
        else
          parameters.add(null);
      }
      else { // non-empty value; coerce to the correct object
        Object obj = switch (var.getSecond()) {
          case INTEGER -> Integer.parseInt(valueStr);
          case NUMBER -> Double.parseDouble(valueStr);
          case STRING -> valueStr;
          default -> Functions.doThrow(() -> new IllegalStateException("Should already have checked variable types."));
        };
        parameters.add(obj);
      }
    }
    return Optional.of(parameters);
  }

  private String callFunction(List<Object> args) {
    try {
      // TODO: (maybe?) can coerce object the same way as above based on incoming data type; this is not strictly
      //   necessary since we will always write strings, but would be an outgoing data integrity check.  Users should
      //   not be able to claim they will produce an integer with the expression and then produce non-integer strings.
      //   Downside is conversion of every produced value to native type and back to String is expensive.
      return ((Invocable)_engine).invokeFunction(JS_FUNCTION_NAME, args.toArray()).toString();
    }
    catch (NoSuchMethodException e) {
      // this should never happen since function is defined above (and should already have compiled)
      throw new RuntimeException("Function called that was not defined in script engine.", e);
    }
    catch (ScriptException e) {
      throw new RuntimeException("Unable to perform expression evaluation with the following args: " +
          args.stream().map(Object::toString).collect(Collectors.joining(", ")), e);
    }
  }
}
