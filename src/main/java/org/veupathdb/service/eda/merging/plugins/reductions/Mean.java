package org.veupathdb.service.eda.merging.plugins.reductions;

public class Mean extends SingleNumericVarReduction {

  @Override
  public String getFunctionName() {
    return "mean";
  }

  @Override
  public Reducer createReducer() {
    return new SingleNumericVarReducer() {

      private int _numRows = 0;
      private double _sum = 0D;

      @Override
      protected void processValue(double d) {
        _numRows++;
        _sum += d;
      }

      @Override
      public String getResultingValue() {
        return _numRows == 0 ? "" : String.valueOf(_sum / _numRows);
      }
    };
  }
}
