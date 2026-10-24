package sck_calc_plug_in;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import info.openrocket.core.rocketcomponent.RecoveryDevice;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.simulation.FlightDataBranch;
import info.openrocket.core.simulation.FlightDataType;
import info.openrocket.core.simulation.SimulationConditions;
import info.openrocket.core.simulation.SimulationStatus;
import info.openrocket.core.simulation.exception.SimulationException;
import info.openrocket.core.simulation.extension.AbstractSimulationExtension;
import info.openrocket.core.simulation.listeners.AbstractSimulationListener;
import info.openrocket.core.util.Coordinate;

public class MyExt extends AbstractSimulationExtension {

    /** Specific gas constant of dry air, J/(kg*K). */
    private static final double AIR_GAS_CONSTANT = 287.05;

    @Override
    public void initialize(SimulationConditions conditions) throws SimulationException {
        conditions.getSimulationListenerList().add(new MyListener());
    }

    @Override
    public String getName() {
        return "Shock Load Calculator";
    }

    @Override
    public String getDescription() {
        return "Reads the section masses from the rocket model and computes the opening shock, "
                + "separation load and sustained load of every parachute, swept over a safety factor range.";
    }

    // ---- User parameters (stored in the .ork file) ----

    public double getOpeningForceFactor() {
        return config.getDouble("openingForceFactor", 1.5);
    }

    public void setOpeningForceFactor(double v) {
        config.put("openingForceFactor", v);
        fireChangeEvent();
    }

    public double getSafetyFactorMin() {
        return config.getDouble("safetyFactorMin", 1.0);
    }

    public void setSafetyFactorMin(double v) {
        config.put("safetyFactorMin", v);
        fireChangeEvent();
    }

    public double getSafetyFactorMax() {
        return config.getDouble("safetyFactorMax", 3.0);
    }

    public void setSafetyFactorMax(double v) {
        config.put("safetyFactorMax", v);
        fireChangeEvent();
    }

    public double getCordStiffness() {
        return config.getDouble("cordStiffness", 10000.0);
    }

    public void setCordStiffness(double v) {
        config.put("cordStiffness", v);
        fireChangeEvent();
    }

    /** Manual joint positions in cm from the nose tip, comma separated. Empty means automatic. */
    public String getBoundariesCm() {
        return config.getString("boundariesCm", "");
    }

    public void setBoundariesCm(String s) {
        config.put("boundariesCm", s);
        fireChangeEvent();
    }

    // ---- Simulation listener ----

    private class MyListener extends AbstractSimulationListener {

        private final Set<RecoveryDevice> processed = new HashSet<>();
        private final List<EventResult> results = new ArrayList<>();
        private RocketSections sections;
        private double[] firstSectionMasses;

        @Override
        public void startSimulation(SimulationStatus status) throws SimulationException {
            processed.clear();
            results.clear();
            sections = null;
            firstSectionMasses = null;
        }

        @Override
        public void postStep(SimulationStatus status) throws SimulationException {
            Set<RecoveryDevice> deployed = status.getDeployedRecoveryDevices();
            if (deployed == null || deployed.isEmpty()) {
                return;
            }
            for (RecoveryDevice device : new ArrayList<>(deployed)) {
                if (!processed.add(device)) {
                    continue;   // this device has already been handled
                }
                try {
                    capture(status, device);
                } catch (RuntimeException e) {
                    // Never abort the simulation; the stack trace goes to the console
                    e.printStackTrace();
                }
            }
        }

        private void capture(SimulationStatus status, RecoveryDevice device) {
            FlightDataBranch data = status.getFlightDataBranch();

            double speed = status.getRocketVelocity().length();
            double mass = data.getLast(FlightDataType.TYPE_MASS);
            double pressure = data.getLast(FlightDataType.TYPE_AIR_PRESSURE);
            double temperature = data.getLast(FlightDataType.TYPE_AIR_TEMPERATURE);
            double density = pressure / (AIR_GAS_CONSTANT * temperature);   // ideal gas: rho = P / (R * T)
            double cd = device.getCD();
            double area = device.getArea();

            // Section masses are read from the rocket model
            RocketComponent root = status.getConfiguration().getRocket();
            if (sections == null) {
                sections = RocketSections.analyze(root, getBoundariesCm());
            }
            double[] sectionMasses = sections.sectionMasses(mass);
            if (firstSectionMasses == null) {
                firstSectionMasses = sectionMasses;
            }

            // The joint that belongs to this parachute is the one closest to it
            RocketComponent chute = (RocketComponent) device;
            double x = chute.toAbsolute(new Coordinate(0, 0, 0))[0].x + chute.getLength() / 2.0;
            int joint = sections.nearestJoint(x);

            double upper = Double.NaN;
            double lower = Double.NaN;
            double jointX = Double.NaN;
            double snatchLimit = Double.NaN;
            if (joint >= 0) {
                upper = RocketSections.upperMass(joint, sectionMasses);
                lower = RocketSections.lowerMass(joint, sectionMasses);
                jointX = sections.jointX(joint);
                snatchLimit = LoadCalculator.snatchLoad(speed, getCordStiffness(), upper, lower);
            }

            double openingLimit = LoadCalculator.openingShock(density, speed, cd, area, getOpeningForceFactor());
            double sustainedLimit = LoadCalculator.sustainedLoad(mass);

            results.add(new EventResult(chute.getName(), status.getSimulationTime(),
                    status.getRocketPosition().z, speed, density, mass, cd, area,
                    joint, jointX, upper, lower, openingLimit, snatchLimit, sustainedLimit));
        }

        @Override
        public void endSimulation(SimulationStatus status, SimulationException exception) {
            if (results.isEmpty()) {
                return;   // no parachute deployed, so there is nothing to show
            }
            final List<EventResult> events = new ArrayList<>(results);
            final RocketSections secs = sections;
            final double[] sectionMasses = firstSectionMasses;
            final SafetyFactorSweep.Settings settings = new SafetyFactorSweep.Settings(
                    getSafetyFactorMin(), getSafetyFactorMax(),
                    getOpeningForceFactor(), getCordStiffness());

            SwingUtilities.invokeLater(
                    () -> new ResultsDialog(secs, sectionMasses, events, settings).setVisible(true));
        }
    }
}
