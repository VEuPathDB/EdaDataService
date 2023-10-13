package org.veupathdb.service.eda.common.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Units metadata information, including (database) string values for various units, the type of unit (e.g. length), and
 * conversion between units of the same type.  This class will need to be continually updated as new studies are loaded
 * to incorporate more string values and possibly new unit types.  Last update was against live EDA in April 2023.
 */
public class Units {

  // static class
  private Units(){}

  /**
   * Represents the type of the unit.  Units can only be converted to other units of the same type.  e.g. a length
   * unit cannot be converted to a mass unit.
   */
  public enum UnitType {
    LENGTH,
    MASS,
    VOLUME,
    TEMPERATURE,
    TIME_LARGE,
    TIME_SMALL,
    MASS_PROPORTION,
    MASS_BY_VOLUME,
    BIOLOGICAL_EFFECT_BY_VOLUME
  }

  /**
   * Allowed units, grouped by type.  To facilitate unit conversion, one unit of each type is selected to be the
   * "baseline" unit for that type.  This is somewhat arbitrary and ultimately does not matter.  To convert from any
   * unit of a given type to another unit, the input unit is first converted to the baseline unit, then converted again
   * to the output unit.  Thus, we need only need n*2 conversion formulas within a type instead of n^2.  Baseline types
   * are indicated by the identity functions below (no conversion necessary to convert baseline to baseline).
   */
  public enum Unit {

    // length units
    MILLIMETER (List.of("mm"),   UnitType.LENGTH, n -> n * 0.001, n -> n * 1000),
    CENTIMETER (List.of("cm"),   UnitType.LENGTH, n -> n * 0.01, n -> n * 100),
    FEET       (List.of("feet"), UnitType.LENGTH, n -> n * 0.3048, n -> n * 3.280839895),
    METER      (List.of("m"),    UnitType.LENGTH, Function.identity(), Function.identity()),
    KILOMETER  (List.of("km"),   UnitType.LENGTH, n -> n * 1000, n -> n * 0.001),

    // mass units
    MICROGRAM (List.of("ug"), UnitType.MASS, n -> n * 0.000001, n -> n * 1000000),
    MILLIGRAM (List.of("mg"), UnitType.MASS, n -> n * 0.001, n -> n * 1000),
    GRAM      (List.of("g"),  UnitType.MASS, Function.identity(), Function.identity()),
    KILOGRAM  (List.of("kg"), UnitType.MASS, n -> n * 1000, n -> n * 0.001),

    // volume units
    MILLILITER (List.of("mL","ml"), UnitType.VOLUME, n -> n * 0.001, n -> n * 1000),
    LITER      (List.of("L"),       UnitType.VOLUME, Function.identity(), Function.identity()),

    // temperature units
    DEG_CELSIUS    (List.of("C"), UnitType.TEMPERATURE, Function.identity(), Function.identity()),
    DEG_FAHRENHEIT (List.of("F"), UnitType.TEMPERATURE, n -> (n - 32) * 5 / 9, n -> (n * 9 / 5) + 32),

    // large time units
    MONTH (List.of("months"),         UnitType.TIME_LARGE, n -> n / 12, n -> n * 12),
    YEAR  (List.of("years", "Years"), UnitType.TIME_LARGE, Function.identity(), Function.identity()),

    // small time units
    MILLISECOND (List.of("ms"),            UnitType.TIME_SMALL, n -> n / 60000, n -> n * 60000),
    SECOND      (List.of("sec"),           UnitType.TIME_SMALL, n -> n / 60, n -> n * 60),
    MINUTE      (List.of("min","minutes"), UnitType.TIME_SMALL, Function.identity(), Function.identity()),
    HOUR        (List.of("hours"),         UnitType.TIME_SMALL, n -> n * 60, n -> n / 60),
    DAY         (List.of("days"),          UnitType.TIME_SMALL, n -> n * 60 * 24, n -> n / 60 / 24),
    WEEK        (List.of("weeks"),         UnitType.TIME_SMALL, n -> n * 60 * 24 * 7, n -> n / 60 / 24 / 7),

    // mass proportion units
    MICROGRAM_PER_GRAM (List.of("ug/g"), UnitType.MASS_PROPORTION, n -> n * 0.001, n -> n * 1000),
    MILLIGRAM_PER_GRAM (List.of("mg/g"), UnitType.MASS_PROPORTION, Function.identity(), Function.identity());
/*
    TO BE CALCULATED: holding off until this plugin is approved

    // mass by volume units
    MICROGRAM_PER_LITER      (List.of("ug/L"),  UnitType.MASS_BY_VOLUME, , ),
    MICROGRAM_PER_DECILITER  (List.of("ug/dL"), UnitType.MASS_BY_VOLUME, , ),
    MICROGRAM_PER_MILLILITER (List.of("ug/mL"), UnitType.MASS_BY_VOLUME, , ),
    MILLIGRAM_PER_LITER      (List.of("mg/L"),  UnitType.MASS_BY_VOLUME, Function.identity(), Function.identity()),
    MILLIGRAM_PER_DECILITER  (List.of("mg/dL"), UnitType.MASS_BY_VOLUME, , ),
    GRAM_PER_DECILITER       (List.of("g/dL"),  UnitType.MASS_BY_VOLUME, , ),

    // effect by volume units
    IU_PER_LITER      (List.of("IU/L","U/L"),   UnitType.BIOLOGICAL_EFFECT_BY_VOLUME, , ),
    IU_PER_MILLILITER (List.of("IU/mL","U/mL"), UnitType.BIOLOGICAL_EFFECT_BY_VOLUME, Function.identity(), Function.identity()),
    IU_PER_MICROLITER (List.of("IU/uL"),        UnitType.BIOLOGICAL_EFFECT_BY_VOLUME, , ),
*/

    private final List<String> _values;
    private final UnitType _type;
    private final Function<Double, Double> _toBaseline;
    private final Function<Double, Double> _fromBaseline;

    Unit(List<String> values, UnitType type, Function<Double, Double> toBaseline, Function<Double, Double> fromBaseline) {
      _values = Collections.unmodifiableList(values);
      _type = type;
      _fromBaseline = fromBaseline;
      _toBaseline = toBaseline;
    }

    public static Optional<Unit> findUnit(String unitValue) {
      for (Unit unit : values()) {
        for (String value : unit._values) {
          if (value.equals(unitValue)) return Optional.of(unit);
        }
      }
      return Optional.empty();
    }

    public boolean isCompatibleWith(Unit unit) {
      return _type == unit._type;
    }

    public double convertTo(Unit outputUnit, double value) {
      // convert from this unit to baseline, then from baseline to output unit
      return outputUnit._fromBaseline.apply(_toBaseline.apply(value));
    }

    public double convertFrom(Unit inputUnit, double value) {
      // convert from input unit to baseline, then from baseline to this unit
      return _fromBaseline.apply(inputUnit._toBaseline.apply(value));
    }

    public UnitType getType() {
      return _type;
    }

    public List<String> getValues() {
      return _values;
    }
  }
}
