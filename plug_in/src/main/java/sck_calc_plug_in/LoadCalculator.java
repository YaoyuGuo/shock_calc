package sck_calc_plug_in;

public final class LoadCalculator {
    private static final double G = 9.80665;

    private LoadCalculator() {}

    /** 开伞冲击 opening shock load：F = 1/2 * rho * v^2 * Cd * A * Cx */
    public static double openingShock(double rho, double v, double cd, double area, double cx) {
        return 0.5 * rho * v * v * cd * area * cx;
    }

    /** 分离冲击（减震绳）separation shock load：F = v * sqrt(k * mu)，mu = m1*m2/(m1+m2) */
    public static double snatchLoad(double v, double k, double m1, double m2) {
        if (m1 + m2 <= 0) return 0;
        double mu = m1 * m2 / (m1 + m2);
        return v * Math.sqrt(k * mu);
    }

    /** 隔板持续载荷 Bulkhead Sustained Load：F = m * g * safetyFactor */
    public static double sustainedLoad(double mass, double safetyFactor) {
        return mass * G * safetyFactor;
    }
}