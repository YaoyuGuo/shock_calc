package sck_calc_plug_in;

/**
 * Load formulas. All inputs and outputs use SI units (N, kg, m, s, kg/m^3).
 *
 * The first three methods return LIMIT loads (no safety factor applied).
 * {@link #designLoad(double, double)} converts a limit load into a design load.
 */
public final class LoadCalculator {

    private static final double G = 9.80665;

    private LoadCalculator() {
    }

    /** Parachute opening shock (limit load): F = 1/2 * rho * v^2 * Cd * A * Cx. */
    public static double openingShock(double density, double speed, double cd, double area, double cx) {
        return 0.5 * density * speed * speed * cd * area * cx;
    }

    /**
     * Separation (shock cord) snatch load (limit load): F = v * sqrt(k * mu),
     * where mu = m1 * m2 / (m1 + m2) is the reduced mass of the two bodies.
     */
    public static double snatchLoad(double speed, double stiffness, double upperMass, double lowerMass) {
        if (upperMass + lowerMass <= 0) {
            return 0;
        }
        double reducedMass = upperMass * lowerMass / (upperMass + lowerMass);
        return speed * Math.sqrt(stiffness * reducedMass);
    }

    /** Sustained load (limit load): the weight of the rocket, F = m * g. */
    public static double sustainedLoad(double mass) {
        return mass * G;
    }

    /** Design load: F_design = safetyFactor * F_limit. */
    public static double designLoad(double limitLoad, double safetyFactor) {
        return limitLoad * safetyFactor;
    }
}
