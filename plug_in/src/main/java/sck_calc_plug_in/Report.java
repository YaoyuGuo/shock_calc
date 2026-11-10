package sck_calc_plug_in;

import java.util.List;

/**
 * Everything one simulation run produced, handed from the simulation listener to the results window.
 *
 * @param sections        how the rocket was split
 * @param sectionMasses   mass of every section at the first deployment (kg)
 * @param events          one entry per deployed recovery device, in order of deployment
 * @param sweep           safety factor sweep settings and the constants used
 * @param sizing          bolt and bulkhead sizing settings
 */
public record Report(RocketSections sections, double[] sectionMasses, List<EventResult> events,
                     SafetyFactorSweep.Settings sweep, Sizing.Settings sizing) {
}
