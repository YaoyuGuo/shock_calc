package sck_calc_plug_in;

/**
 * Results for one recovery-device deployment.
 * Masses are in kg, forces in N, speeds in m/s, positions in m. NaN means "not available".
 */
public record EventResult(
        String device,     // name of the recovery device
        double time,       // simulation time of deployment (s)
        double altitude,   // altitude above launch site (m)
        double speed,      // rocket speed at deployment (m/s)
        double rho,        // air density (kg/m^3)
        double mass,       // total rocket mass at deployment (kg)
        double cd,         // parachute drag coefficient
        double area,       // parachute area (m^2)
        int joint,         // index of the associated joint, -1 if none
        double jointX,     // joint position from the nose tip (m)
        double upper,      // mass in front of the joint (kg)
        double lower,      // mass behind the joint (kg)
        double opening,    // opening shock (N)
        double snatch,     // separation (shock cord) load (N)
        double steady      // steady load (N)
) {
}
