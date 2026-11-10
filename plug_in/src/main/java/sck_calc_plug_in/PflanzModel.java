package sck_calc_plug_in;

/**
 * Parachute opening force by the Pflanz method (Knacke, "Parachute Recovery Systems Design Manual", ch. 5).
 *
 * The canopy drag area is assumed to grow as (CdS)(t) = CdS * (t / tf)^j during the filling time
 * tf = n * D0 / v0, where n is the canopy fill constant, D0 the nominal diameter and v0 the speed
 * at line stretch. The rocket decelerates while the canopy fills (finite mass), so the peak force is
 * lower than the infinite-mass value q0 * CdS * Cx by the force reduction factor X1:
 *
 *     F = q0 * CdS * Cx * X1,   q0 = 1/2 * rho * v0^2
 *
 * Two solutions are provided:
 *  - {@link #analyticX1}: the classical Pflanz result (no gravity), a function of the ballistic
 *    parameter A = 2 m / (rho * CdS * v0 * tf) and the exponent j only;
 *  - {@link #solve}: a numerical integration of the same equation of motion with gravity included,
 *    which also returns the force history. For a horizontal trajectory or negligible gravity it
 *    reproduces the analytic result.
 *
 * All quantities use SI units.
 */
public final class PflanzModel {

    public static final double G = 9.80665;

    /** Below this speed the filling time n*D0/v0 is evaluated with this speed instead (avoids tf -> infinity). */
    public static final double MIN_FILL_SPEED = 1.0;

    private static final int STEPS = 4000;
    private static final int CURVE_POINTS = 200;

    /**
     * Result of one inflation calculation.
     *
     * @param fillTime            filling time tf (s)
     * @param ballisticParameter  Pflanz ballistic parameter A (-)
     * @param dynamicPressure     dynamic pressure at line stretch q0 (Pa)
     * @param infiniteMassForce   q0 * CdS * Cx (N), the upper bound with no deceleration
     * @param x1Analytic          force reduction factor, classical Pflanz (no gravity)
     * @param x1                  force reduction factor with gravity (numerical)
     * @param peakForce           peak opening force with gravity, q0 * CdS * Cx * x1 (N)
     * @param peakTime            time of the peak after line stretch (s)
     * @param endSpeed            speed at the end of filling (m/s)
     * @param curveTime           time samples of the force history (s)
     * @param curveForce          force samples of the force history (N), Cx included
     */
    public record Result(double fillTime, double ballisticParameter, double dynamicPressure,
                         double infiniteMassForce, double x1Analytic, double x1,
                         double peakForce, double peakTime, double endSpeed,
                         double[] curveTime, double[] curveForce) {
    }

    private PflanzModel() {
    }

    /** Filling time tf = n * D0 / v0. */
    public static double fillTime(double fillConstant, double nominalDiameter, double speed) {
        return fillConstant * nominalDiameter / Math.max(speed, MIN_FILL_SPEED);
    }

    /** Ballistic parameter A = 2 m / (rho * CdS * v0 * tf). Infinite if the denominator is zero. */
    public static double ballisticParameter(double mass, double density, double dragArea,
                                            double speed, double fillTime) {
        double denominator = density * dragArea * speed * fillTime;
        return denominator > 0 ? 2.0 * mass / denominator : Double.POSITIVE_INFINITY;
    }

    /**
     * Classical Pflanz force reduction factor X1 (no gravity).
     *
     * With tau = t / tf the equation m dv/dt = -1/2 rho v^2 CdS tau^j integrates to
     *     v / v0 = 1 / (1 + tau^(j+1) / (A (j+1)))
     * so the force ratio is x(tau) = tau^j / (1 + tau^(j+1) / (A (j+1)))^2.
     * Its maximum lies at tau*^(j+1) = A j (j+1) / (j+2), or at tau = 1 if tau* > 1.
     */
    public static double analyticX1(double a, double j) {
        if (!(a > 0)) {
            return 0;
        }
        if (Double.isInfinite(a)) {
            return 1;
        }
        double k = a * (j + 1);
        double tauPow = a * j * (j + 1) / (j + 2);      // tau*^(j+1)
        if (j <= 0 || tauPow >= 1) {
            double d = 1 + 1 / k;
            return 1 / (d * d);
        }
        double tau = Math.pow(tauPow, 1 / (j + 1));
        double d = 1 + tauPow / k;
        return Math.pow(tau, j) / (d * d);
    }

    /**
     * Integrates the inflation with gravity.
     *
     * @param mass             mass decelerated by the parachute (kg)
     * @param dragArea         fully open drag area CdS (m^2)
     * @param density          air density (kg/m^3)
     * @param horizontalSpeed  horizontal speed at line stretch (m/s)
     * @param verticalSpeed    vertical speed at line stretch, positive up (m/s)
     * @param nominalDiameter  canopy nominal diameter D0 (m)
     * @param fillConstant     canopy fill constant n (-)
     * @param exponent         drag-area growth exponent j (-)
     * @param cx               opening-force coefficient Cx at infinite mass (-)
     */
    public static Result solve(double mass, double dragArea, double density,
                               double horizontalSpeed, double verticalSpeed,
                               double nominalDiameter, double fillConstant, double exponent, double cx) {
        double v0 = Math.hypot(horizontalSpeed, verticalSpeed);
        double tf = fillTime(fillConstant, nominalDiameter, v0);
        double q0 = 0.5 * density * v0 * v0;
        double infinite = q0 * dragArea * cx;
        double a = ballisticParameter(mass, density, dragArea, v0, tf);
        double x1Analytic = v0 > 0 ? analyticX1(a, exponent) : 0;

        double[] curveT = new double[CURVE_POINTS + 1];
        double[] curveF = new double[CURVE_POINTS + 1];

        if (!(mass > 0) || !(dragArea > 0) || !(density > 0) || !(tf > 0)) {
            return new Result(tf, a, q0, infinite, x1Analytic, 0, 0, 0, v0, curveT, curveF);
        }

        // State: horizontal and vertical speed. Classical fourth-order Runge-Kutta.
        double k = 0.5 * density * dragArea / mass;    // drag acceleration = k * area ratio * v^2
        double vh = horizontalSpeed;
        double vz = verticalSpeed;
        double dt = tf / STEPS;
        double peak = 0;
        double peakTime = 0;
        int every = STEPS / CURVE_POINTS;

        for (int i = 0; i <= STEPS; i++) {
            double t = i * dt;
            double v = Math.hypot(vh, vz);
            double force = cx * 0.5 * density * v * v * dragArea * ratio(t / tf, exponent);
            if (force > peak) {
                peak = force;
                peakTime = t;
            }
            if (i % every == 0) {
                curveT[i / every] = t;
                curveF[i / every] = force;
            }
            if (i == STEPS) {
                break;
            }

            double r1 = ratio(t / tf, exponent);
            double r2 = ratio((t + dt / 2) / tf, exponent);
            double r3 = ratio((t + dt) / tf, exponent);

            double[] k1 = derivative(vh, vz, k * r1);
            double[] k2 = derivative(vh + dt / 2 * k1[0], vz + dt / 2 * k1[1], k * r2);
            double[] k3 = derivative(vh + dt / 2 * k2[0], vz + dt / 2 * k2[1], k * r2);
            double[] k4 = derivative(vh + dt * k3[0], vz + dt * k3[1], k * r3);
            vh += dt / 6 * (k1[0] + 2 * k2[0] + 2 * k3[0] + k4[0]);
            vz += dt / 6 * (k1[1] + 2 * k2[1] + 2 * k3[1] + k4[1]);
        }

        double x1 = infinite > 0 ? peak / infinite : 0;
        return new Result(tf, a, q0, infinite, x1Analytic, x1, peak, peakTime,
                Math.hypot(vh, vz), curveT, curveF);
    }

    private static double ratio(double tau, double exponent) {
        if (tau <= 0) {
            return exponent == 0 ? 1 : 0;
        }
        return tau >= 1 ? 1 : Math.pow(tau, exponent);
    }

    /** Time derivative of (vh, vz): drag opposes the velocity, gravity acts on vz. */
    private static double[] derivative(double vh, double vz, double kr) {
        double v = Math.hypot(vh, vz);
        return new double[] {-kr * v * vh, -G - kr * v * vz};
    }
}
