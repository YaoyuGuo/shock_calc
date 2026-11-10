package sck_calc_plug_in;

/**
 * Results for one recovery-device deployment. All loads are LIMIT loads (no safety factor applied).
 * Units: kg, N, m/s, m, s, kg/m^3. NaN means "not available".
 */
public record EventResult(
        String device,              // name of the recovery device
        double time,                // simulation time of deployment (s)
        double altitude,            // altitude above the launch site (m)
        double speed,               // rocket speed at deployment (m/s)
        double verticalSpeed,       // vertical component of the speed, positive up (m/s)
        double airDensity,          // air density at deployment (kg/m^3)
        double mass,                // total rocket mass at deployment (kg)
        double dragCoefficient,     // parachute drag coefficient Cd
        double area,                // parachute reference area (m^2)
        double nominalDiameter,     // parachute nominal diameter D0 (m)
        int joint,                  // index of the associated joint, -1 if there is none
        double jointX,              // joint position measured from the nose tip (m)
        double upperMass,           // mass of everything in front of the joint (kg)
        double lowerMass,           // mass of everything behind the joint (kg)
        PflanzModel.Result pflanz,  // finite-mass inflation calculation
        double openingInfinite,     // opening shock at infinite mass, q * CdS * Cx (N)
        double openingLimit,        // opening shock used for design, by the selected method (N)
        double snatchLimit,         // separation (shock cord) load (N)
        double sustainedLimit,      // sustained load (N)
        double governingLimit,      // largest of the three limit loads (N)
        Sizing.Result sizing        // bolt and bulkhead sizing for the governing load
) {

    /** Peak deceleration of the whole rocket during inflation, in g. */
    public double peakDecelerationG() {
        return mass > 0 ? openingLimit / (mass * PflanzModel.G) : Double.NaN;
    }

    /** Share of the opening force that accelerates the bodies in front of the joint (N). */
    public double openingUpperShare() {
        return openingLimit * upperMass / (upperMass + lowerMass);
    }

    /** Share of the opening force that accelerates the bodies behind the joint (N). */
    public double openingLowerShare() {
        return openingLimit * lowerMass / (upperMass + lowerMass);
    }
}
