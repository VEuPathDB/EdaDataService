package org.veupathdb.service.eda.merging.plugins.reductions;

public class Sum extends SingleNumericVarReduction {

  @Override
  public String getFunctionName() {
    return "sum";
  }

  @Override
  public Reducer createReducer() {
    return new SingleNumericVarReducer() {

      private double _sum = 0;

      @Override
      protected void processValue(double d) {
        _sum += d;
      }

      @Override
      public String getResultingValue() {
        return String.valueOf(_sum);
      }
    };
  }
}
