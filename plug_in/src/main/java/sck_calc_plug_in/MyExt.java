package sck_calc_plug_in;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import info.openrocket.core.rocketcomponent.Parachute;
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

    /** Opening load method: finite mass (Pflanz). */
    public static final int METHOD_PFLANZ = 0;
    /** Opening load method: infinite mass, F = q * CdS * Cx (the method of version 2.0). */
    public static final int METHOD_INFINITE_MASS = 1;

    public static final String[] METHOD_NAMES = {
            "Pflanz, finite mass (recommended)",
            "Infinite mass, q x CdS x Cx (upper bound)"
    };

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
        return "Computes the parachute opening force (Pflanz method), the shock cord snatch load and the "
                + "sustained load of every parachute from the simulated deployment conditions, "
                + "and sizes the attachment bolts and the bulkhead for the governing load.";
    }

    /**
     * Called on the simulation thread when a run has finished and at least one parachute deployed.
     * Shows the results window; does nothing when no display is available.
     */
    protected void publish(Report report) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeLater(() -> new ResultsDialog(report).setVisible(true));
    }

    // ---- User parameters (stored in the .ork file) ----
    // DoubleModel needs a double getter and setter for every numeric property.

    // -- Opening force --

    public int getOpeningMethod() {
        return config.getInt("openingMethod", METHOD_PFLANZ);
    }

    public void setOpeningMethod(int v) {
        config.put("openingMethod", v);
        fireChangeEvent();
    }

    public double getOpeningForceFactor() {
        return config.getDouble("openingForceFactor", 1.5);
    }

    public void setOpeningForceFactor(double v) {
        config.put("openingForceFactor", v);
        fireChangeEvent();
    }

    /** Canopy fill constant n: the canopy fills after travelling n nominal diameters. */
    public double getFillConstant() {
        return config.getDouble("fillConstant", 8.0);
    }

    public void setFillConstant(double v) {
        config.put("fillConstant", v);
        fireChangeEvent();
    }

    /** Exponent j of the drag-area growth (t / tf)^j. */
    public double getInflationExponent() {
        return config.getDouble("inflationExponent", 2.0);
    }

    public void setInflationExponent(double v) {
        config.put("inflationExponent", v);
        fireChangeEvent();
    }

    // -- Snatch load --

    /** Relative speed of the two bodies when the shock cord goes taut (m/s). */
    public double getSeparationSpeed() {
        return config.getDouble("separationSpeed", 10.0);
    }

    public void setSeparationSpeed(double v) {
        config.put("separationSpeed", v);
        fireChangeEvent();
    }

    public double getCordStiffness() {
        return config.getDouble("cordStiffness", 10000.0);
    }

    public void setCordStiffness(double v) {
        config.put("cordStiffness", v);
        fireChangeEvent();
    }

    // -- Safety factor sweep --

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

    // -- Bolt and bulkhead sizing --

    public double getSizingSafetyFactor() {
        return config.getDouble("sizingSafetyFactor", 2.0);
    }

    public void setSizingSafetyFactor(double v) {
        config.put("sizingSafetyFactor", v);
        fireChangeEvent();
    }

    /** Threaded legs sharing the load: 2 for a U-bolt, 1 for an eye-bolt. */
    public double getBoltLegs() {
        return config.getDouble("boltLegs", 2.0);
    }

    public void setBoltLegs(double v) {
        config.put("boltLegs", (double) Math.max(1, Math.round(v)));
        fireChangeEvent();
    }

    public int getBoltMaterial() {
        return config.getInt("boltMaterial", 0);
    }

    public void setBoltMaterial(int v) {
        config.put("boltMaterial", v);
        fireChangeEvent();
    }

    /** Allowable tensile stress for the custom bolt material (MPa). */
    public double getBoltCustomStrength() {
        return config.getDouble("boltCustomStrength", 450.0);
    }

    public void setBoltCustomStrength(double v) {
        config.put("boltCustomStrength", v);
        fireChangeEvent();
    }

    public int getPlateMaterial() {
        return config.getInt("plateMaterial", 0);
    }

    public void setPlateMaterial(int v) {
        config.put("plateMaterial", v);
        fireChangeEvent();
    }

    /** Allowable bending stress for the custom bulkhead material (MPa). */
    public double getPlateCustomBending() {
        return config.getDouble("plateCustomBending", 276.0);
    }

    public void setPlateCustomBending(double v) {
        config.put("plateCustomBending", v);
        fireChangeEvent();
    }

    /** Allowable shear stress for the custom bulkhead material (MPa). */
    public double getPlateCustomShear() {
        return config.getDouble("plateCustomShear", 159.0);
    }

    public void setPlateCustomShear(double v) {
        config.put("plateCustomShear", v);
        fireChangeEvent();
    }

    /** Radius of the circle on which the bolt legs load the bulkhead (mm). */
    public double getLoadRadiusMm() {
        return config.getDouble("loadRadiusMm", 15.0);
    }

    public void setLoadRadiusMm(double v) {
        config.put("loadRadiusMm", v);
        fireChangeEvent();
    }

    /** Outer diameter of the washer or nut under each leg (mm). */
    public double getWasherDiameterMm() {
        return config.getDouble("washerDiameterMm", 12.0);
    }

    public void setWasherDiameterMm(double v) {
        config.put("washerDiameterMm", v);
        fireChangeEvent();
    }

    /** Bulkhead radius in mm. Zero means automatic: the inner radius of the body tube at the joint. */
    public double getBulkheadRadiusMm() {
        return config.getDouble("bulkheadRadiusMm", 0.0);
    }

    public void setBulkheadRadiusMm(double v) {
        config.put("bulkheadRadiusMm", v);
        fireChangeEvent();
    }

    // -- Sections --

    /** Manual joint positions in cm from the nose tip, comma separated. Empty means automatic. */
    public String getBoundariesCm() {
        return config.getString("boundariesCm", "");
    }

    public void setBoundariesCm(String s) {
        config.put("boundariesCm", s);
        fireChangeEvent();
    }

    private Sizing.Settings sizingSettings() {
        return new Sizing.Settings(
                getSizingSafetyFactor(), (int) Math.round(getBoltLegs()),
                getBoltMaterial(), getBoltCustomStrength(),
                getPlateMaterial(), getPlateCustomBending(), getPlateCustomShear(),
                getLoadRadiusMm() / 1000.0, getWasherDiameterMm() / 1000.0, getBulkheadRadiusMm() / 1000.0);
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

            // Flight conditions at deployment
            Coordinate velocity = status.getRocketVelocity();
            double speed = velocity.length();
            double horizontalSpeed = Math.hypot(velocity.x, velocity.y);
            double verticalSpeed = velocity.z;
            double mass = data.getLast(FlightDataType.TYPE_MASS);
            double pressure = data.getLast(FlightDataType.TYPE_AIR_PRESSURE);
            double temperature = data.getLast(FlightDataType.TYPE_AIR_TEMPERATURE);
            double density = pressure / (AIR_GAS_CONSTANT * temperature);   // ideal gas: rho = P / (R * T)

            // Parachute
            double cd = device.getCD();
            double area = device.getArea();
            double diameter = device instanceof Parachute p
                    ? p.getDiameter()
                    : Math.sqrt(4 * area / Math.PI);

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
                snatchLimit = LoadCalculator.snatchLoad(getSeparationSpeed(), getCordStiffness(), upper, lower);
            }

            // Opening force: finite mass (Pflanz) and infinite mass
            double cx = getOpeningForceFactor();
            PflanzModel.Result pflanz = PflanzModel.solve(mass, cd * area, density,
                    horizontalSpeed, verticalSpeed, diameter,
                    getFillConstant(), getInflationExponent(), cx);
            double openingInfinite = LoadCalculator.openingShock(density, speed, cd, area, cx);
            double openingLimit = getOpeningMethod() == METHOD_INFINITE_MASS
                    ? openingInfinite
                    : pflanz.peakForce();

            double sustainedLimit = LoadCalculator.sustainedLoad(mass);
            double governing = LoadCalculator.governing(openingLimit, snatchLimit, sustainedLimit);

            // Bolt and bulkhead sizing for the governing load
            Sizing.Settings sizing = sizingSettings();
            double bulkheadRadius = sizing.bulkheadRadius() > 0
                    ? sizing.bulkheadRadius()
                    : sections.tubeInnerRadius(Double.isNaN(jointX) ? x : jointX);
            Sizing.Result sized = Sizing.size(governing, bulkheadRadius, sizing);

            results.add(new EventResult(chute.getName(), status.getSimulationTime(),
                    status.getRocketPosition().z, speed, verticalSpeed, density, mass, cd, area, diameter,
                    joint, jointX, upper, lower,
                    pflanz, openingInfinite, openingLimit, snatchLimit, sustainedLimit, governing, sized));
        }

        @Override
        public void endSimulation(SimulationStatus status, SimulationException exception) {
            if (results.isEmpty()) {
                return;   // no parachute deployed, so there is nothing to show
            }
            SafetyFactorSweep.Settings sweep = new SafetyFactorSweep.Settings(
                    getSafetyFactorMin(), getSafetyFactorMax(),
                    getOpeningForceFactor(), getCordStiffness(),
                    METHOD_NAMES[Math.max(0, Math.min(METHOD_NAMES.length - 1, getOpeningMethod()))],
                    getFillConstant(), getInflationExponent(), getSeparationSpeed());

            publish(new Report(sections, firstSectionMasses, new ArrayList<>(results), sweep, sizingSettings()));
        }
    }
}
