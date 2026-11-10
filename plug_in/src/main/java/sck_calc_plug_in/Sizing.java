package sck_calc_plug_in;

import java.util.Locale;

/**
 * Structural sizing of the recovery attachment: U-bolt / eye-bolt thread size and bulkhead thickness.
 *
 * All strength values in the tables are typical handbook values in MPa. They are starting points:
 * check them against the data sheet of the actual hardware and material.
 */
public final class Sizing {

    // ---- Fastener materials: allowable tensile stress = proof strength (or 0.2 % yield where no proof is defined) ----

    public static final String[] BOLT_NAMES = {
            "Stainless A2-70 / 18-8 (450 MPa)",
            "Stainless A4-80 / 316 (600 MPa)",
            "Stainless 304 annealed (205 MPa)",
            "Steel ISO 8.8 (580 MPa)",
            "Steel ISO 10.9 (830 MPa)",
            "Steel ISO 12.9 (970 MPa)",
            "Steel SAE Grade 2 (379 MPa)",
            "Steel SAE Grade 5 (586 MPa)",
            "Steel SAE Grade 8 (827 MPa)",
            "Custom (value below)"
    };
    private static final double[] BOLT_STRENGTH = {450, 600, 205, 580, 830, 970, 379, 586, 827, Double.NaN};

    // ---- Bulkhead materials: allowable bending stress, allowable shear stress (MPa), Poisson's ratio ----

    public static final String[] PLATE_NAMES = {
            "Aluminum 6061-T6 (276 / 159 MPa)",
            "Birch plywood (40 / 8 MPa)",
            "G10 / FR4 fiberglass (380 / 130 MPa)",
            "Carbon fiber plate, quasi-isotropic (500 / 80 MPa)",
            "Custom (values below)"
    };
    private static final double[] PLATE_BENDING = {276, 40, 380, 500, Double.NaN};
    private static final double[] PLATE_SHEAR = {159, 8, 130, 80, Double.NaN};
    private static final double[] PLATE_POISSON = {0.33, 0.30, 0.13, 0.30, 0.30};

    // ---- Standard threads: tensile stress area in mm^2 ----

    private static final String[] METRIC_NAMES = {"M3", "M4", "M5", "M6", "M8", "M10", "M12", "M14", "M16", "M20", "M24"};
    private static final double[] METRIC_AREAS = {5.03, 8.78, 14.2, 20.1, 36.6, 58.0, 84.3, 115.0, 157.0, 245.0, 353.0};

    private static final String[] UNC_NAMES = {"#6-32", "#8-32", "#10-24", "1/4-20", "5/16-18", "3/8-16",
            "7/16-14", "1/2-13", "5/8-11", "3/4-10"};
    private static final double[] UNC_AREAS_IN2 = {0.00909, 0.0140, 0.0175, 0.0318, 0.0524, 0.0775,
            0.1063, 0.1419, 0.226, 0.334};
    private static final double IN2_TO_MM2 = 645.16;

    /** User choices for the sizing step. Lengths in m, stresses in MPa. */
    public record Settings(double safetyFactor, int legs,
                           int boltMaterial, double boltCustomStrength,
                           int plateMaterial, double plateCustomBending, double plateCustomShear,
                           double loadRadius, double washerDiameter, double bulkheadRadius) {

        public double boltStrength() {
            int i = clamp(boltMaterial, BOLT_NAMES.length);
            return Double.isNaN(BOLT_STRENGTH[i]) ? boltCustomStrength : BOLT_STRENGTH[i];
        }

        public double plateBending() {
            int i = clamp(plateMaterial, PLATE_NAMES.length);
            return Double.isNaN(PLATE_BENDING[i]) ? plateCustomBending : PLATE_BENDING[i];
        }

        public double plateShear() {
            int i = clamp(plateMaterial, PLATE_NAMES.length);
            return Double.isNaN(PLATE_SHEAR[i]) ? plateCustomShear : PLATE_SHEAR[i];
        }

        public double platePoisson() {
            return PLATE_POISSON[clamp(plateMaterial, PLATE_NAMES.length)];
        }

        public String boltName() {
            return BOLT_NAMES[clamp(boltMaterial, BOLT_NAMES.length)];
        }

        public String plateName() {
            return PLATE_NAMES[clamp(plateMaterial, PLATE_NAMES.length)];
        }
    }

    /**
     * Sizing result for one design load.
     *
     * @param designLoad        design load = safety factor x governing limit load (N)
     * @param areaPerLeg        required tensile stress area per threaded leg (mm^2)
     * @param metric            smallest metric coarse thread that provides it
     * @param unc               smallest UNC thread that provides it
     * @param bulkheadRadius    bulkhead radius used (m)
     * @param thicknessBending  minimum bulkhead thickness from plate bending (m), NaN if not available
     * @param thicknessShear    minimum bulkhead thickness from pull-through shear (m), NaN if not available
     */
    public record Result(double designLoad, double areaPerLeg, String metric, String unc,
                         double bulkheadRadius, double thicknessBending, double thicknessShear) {

        /** The larger of the two thickness requirements. */
        public double thickness() {
            return LoadCalculator.governing(thicknessBending, thicknessShear);
        }
    }

    private Sizing() {
    }

    public static Result size(double limitLoad, double bulkheadRadius, Settings s) {
        double design = LoadCalculator.designLoad(limitLoad, s.safetyFactor());
        int legs = Math.max(1, s.legs());

        double area = requiredStressArea(design, legs, s.boltStrength());
        double bending = bulkheadBendingThickness(design, s.plateBending(), s.platePoisson(),
                bulkheadRadius, s.loadRadius());
        double shear = bulkheadShearThickness(design, legs, s.washerDiameter(), s.plateShear());

        return new Result(design, area, smallestMetric(area), smallestUnc(area), bulkheadRadius, bending, shear);
    }

    /**
     * Required tensile stress area per leg in mm^2: A = F / (legs * sigma).
     * A U-bolt has two threaded legs sharing the load; an eye-bolt has one.
     */
    public static double requiredStressArea(double designLoad, int legs, double strengthMpa) {
        if (!(strengthMpa > 0) || Double.isNaN(designLoad)) {
            return Double.NaN;
        }
        return designLoad / (legs * strengthMpa);   // N / MPa = mm^2
    }

    /**
     * Minimum bulkhead thickness from plate bending, in m.
     *
     * The bulkhead is a simply supported circular plate of radius R loaded at its centre over a
     * circle of radius r (the bolt pattern). The maximum bending stress is
     *     sigma = 3 F / (2 pi t^2) * [(1 + nu) ln(R / r) + 1]
     * so  t = sqrt( 3 F / (2 pi sigma) * [(1 + nu) ln(R / r) + 1] ).
     */
    public static double bulkheadBendingThickness(double designLoad, double strengthMpa, double poisson,
                                                  double radius, double loadRadius) {
        if (!(strengthMpa > 0) || !(radius > 0) || !(loadRadius > 0) || Double.isNaN(designLoad)) {
            return Double.NaN;
        }
        double log = radius > loadRadius ? Math.log(radius / loadRadius) : 0;
        double factor = (1 + poisson) * log + 1;
        return Math.sqrt(3 * designLoad / (2 * Math.PI * strengthMpa * 1e6) * factor);
    }

    /**
     * Minimum bulkhead thickness from pull-through (punching) shear, in m.
     * Each leg shears a cylinder of diameter d (washer or nut) through the plate:
     *     t = F / (legs * pi * d * tau).
     */
    public static double bulkheadShearThickness(double designLoad, int legs, double washerDiameter,
                                                double strengthMpa) {
        if (!(strengthMpa > 0) || !(washerDiameter > 0) || Double.isNaN(designLoad)) {
            return Double.NaN;
        }
        return designLoad / (legs * Math.PI * washerDiameter * strengthMpa * 1e6);
    }

    public static String smallestMetric(double areaMm2) {
        if (Double.isNaN(areaMm2)) {
            return "n/a";
        }
        for (int i = 0; i < METRIC_AREAS.length; i++) {
            if (METRIC_AREAS[i] >= areaMm2) {
                return METRIC_NAMES[i];
            }
        }
        return "> " + METRIC_NAMES[METRIC_NAMES.length - 1];
    }

    public static String smallestUnc(double areaMm2) {
        if (Double.isNaN(areaMm2)) {
            return "n/a";
        }
        for (int i = 0; i < UNC_AREAS_IN2.length; i++) {
            if (UNC_AREAS_IN2[i] * IN2_TO_MM2 >= areaMm2) {
                return UNC_NAMES[i];
            }
        }
        return "> " + UNC_NAMES[UNC_NAMES.length - 1];
    }

    /** Human-readable summary of the settings, shown in the results window. */
    public static String describe(Settings s) {
        return String.format(Locale.ROOT,
                "Sizing safety factor %.2f, %d threaded leg(s), bolt: %s, bulkhead: %s, "
                        + "load circle radius %.1f mm, washer diameter %.1f mm.",
                s.safetyFactor(), Math.max(1, s.legs()), s.boltName(), s.plateName(),
                s.loadRadius() * 1000, s.washerDiameter() * 1000);
    }

    private static int clamp(int index, int length) {
        return Math.max(0, Math.min(length - 1, index));
    }
}
