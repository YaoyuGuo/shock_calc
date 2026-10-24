package sck_calc_plug_in;

/**
 * Results for one recovery-device deployment. All loads are LIMIT loads (no safety factor applied).
 * Units: kg, N, m/s, m, s, kg/m^3. NaN means "not available".
 */
public record EventResult(
        String device,          // name of the recovery device
        double time,            // simulation time of deployment (s)
        double altitude,        // altitude above the launch site (m)
        double speed,           // rocket speed at deployment (m/s)
        double airDensity,      // air density at deployment (kg/m^3)
        double mass,            // total rocket mass at deployment (kg)
        double dragCoefficient, // parachute drag coefficient Cd
        double area,            // parachute reference area (m^2)
        int joint,              // index of the associated joint, -1 if there is none
        double jointX,          // joint position measured from the nose tip (m)
        double upperMass,       // mass of everything in front of the joint (kg)
        double lowerMass,       // mass of everything behind the joint (kg)
        double openingLimit,    // opening shock, limit load (N)
        double snatchLimit,     // separation (shock cord) load, limit load (N)
        double sustainedLimit   // sustained load, limit load (N)
) {
}
