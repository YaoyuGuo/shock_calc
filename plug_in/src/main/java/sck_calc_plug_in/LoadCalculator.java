package sck_calc_plug_in;

/**
 * Load formulas. All inputs and outputs use SI units (N, kg, m, s, kg/m^3).
 *
 * The load methods return LIMIT loads (no safety factor applied).
 * {@link #designLoad(double, double)} converts a limit load into a design load.
 * The finite-mass opening force is computed in {@link PflanzModel}.
 */
public final class LoadCalculator {

    private static final double G = PflanzModel.G;

    private LoadCalculator() {
    }

    /**
     * Parachute opening shock at infinite mass (limit load): F = 1/2 * rho * v^2 * Cd * A * Cx.
     * This ignores the deceleration of the rocket during inflation, so it is an upper bound.
     */
    public static double openingShock(double density, double speed, double cd, double area, double cx) {
        return 0.5 * density * speed * speed * cd * area * cx;
    }

    /**
     * Separation (shock cord) snatch load (limit load): F = dv * sqrt(k * mu),
     * where dv is the relative speed of the two bodies when the cord goes taut,
     * k is the cord stiffness and mu = m1 * m2 / (m1 + m2) is the reduced mass.
     * The cord is treated as an undamped linear spring, which is conservative.
     */
    public static double snatchLoad(double separationSpeed, double stiffness, double upperMass, double lowerMass) {
        if (upperMass <= 0 || lowerMass <= 0 || stiffness <= 0) {
            return 0;
        }
        double reducedMass = upperMass * lowerMass / (upperMass + lowerMass);
        return separationSpeed * Math.sqrt(stiffness * reducedMass);
    }

    /** Sustained load (limit load): the weight of the rocket, F = m * g. */
    public static double sustainedLoad(double mass) {
        return mass * G;
    }

    /** Design load: F_design = safetyFactor * F_limit. */
    public static double designLoad(double limitLoad, double safetyFactor) {
        return limitLoad * safetyFactor;
    }

    /** Largest of the given loads, ignoring NaN ("not available") entries. */
    public static double governing(double... loads) {
        double max = Double.NaN;
        for (double f : loads) {
            if (!Double.isNaN(f) && (Double.isNaN(max) || f > max)) {
                max = f;
            }
        }
        return max;
    }
}
