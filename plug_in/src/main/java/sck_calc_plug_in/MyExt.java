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
        return "Reads the section masses from the rocket model and computes the opening shock, separation load and sustained load of every parachute";
    }

    // ---- 用户参数（随 .ork 文件保存）----
    public double getOpeningForceFactor() { return config.getDouble("openingForceFactor", 1.5); }
    public void setOpeningForceFactor(double v) { config.put("openingForceFactor", v); fireChangeEvent(); }

    public double getSafetyFactor() { return config.getDouble("safetyFactor", 2.0); }
    public void setSafetyFactor(double v) { config.put("safetyFactor", v); fireChangeEvent(); }

    public double getCordStiffness() { return config.getDouble("cordStiffness", 10000.0); }
    public void setCordStiffness(double v) { config.put("cordStiffness", v); fireChangeEvent(); }

    /** 手动指定分离位置（cm，距头锥尖，逗号分隔）。留空表示自动划分。 */
    public String getBoundariesCm() { return config.getString("boundariesCm", ""); }
    public void setBoundariesCm(String s) { config.put("boundariesCm", s); fireChangeEvent(); }

    // ---- 监听器 ----
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
            for (RecoveryDevice dev : new ArrayList<>(deployed)) {
                if (!processed.add(dev)) {
                    continue;   // 这个降落伞已经处理过
                }
                try {
                    capture(status, dev);
                } catch (RuntimeException e) {
                    // 出错时不要中断仿真，错误信息打印到控制台
                    e.printStackTrace();
                }
            }
        }

        private void capture(SimulationStatus status, RecoveryDevice dev) {
            FlightDataBranch data = status.getFlightDataBranch();

            double v = status.getRocketVelocity().length();
            double mass = data.getLast(FlightDataType.TYPE_MASS);
            double p = data.getLast(FlightDataType.TYPE_AIR_PRESSURE);
            double t = data.getLast(FlightDataType.TYPE_AIR_TEMPERATURE);
            double rho = p / (287.05 * t);   // 理想气体：rho = P / (R * T)
            double cd = dev.getCD();
            double area = dev.getArea();

            // 从火箭模型读取各段质量
            RocketComponent root = status.getConfiguration().getRocket();
            if (sections == null) {
                sections = RocketSections.analyze(root, getBoundariesCm());
            }
            double[] sm = sections.sectionMasses(mass);
            if (firstSectionMasses == null) {
                firstSectionMasses = sm;
            }

            // 这个降落伞对应哪个分离位置：离降落伞最近的那个
            RocketComponent chute = (RocketComponent) dev;
            double x = chute.toAbsolute(new Coordinate(0, 0, 0))[0].x + chute.getLength() / 2.0;
            int joint = sections.nearestJoint(x);

            double upper = Double.NaN;
            double lower = Double.NaN;
            double jointX = Double.NaN;
            double snatch = Double.NaN;
            if (joint >= 0) {
                upper = RocketSections.upperMass(joint, sm);
                lower = RocketSections.lowerMass(joint, sm);
                jointX = sections.jointX(joint);
                snatch = LoadCalculator.snatchLoad(v, getCordStiffness(), upper, lower);
            }

            double opening = LoadCalculator.openingShock(rho, v, cd, area, getOpeningForceFactor());
            double steady = LoadCalculator.sustainedLoad(mass, getSafetyFactor());

            results.add(new EventResult(chute.getName(), status.getSimulationTime(),
                    status.getRocketPosition().z, v, rho, mass, cd, area,
                    joint, jointX, upper, lower, opening, snatch, steady));
        }

        @Override
        public void endSimulation(SimulationStatus status, SimulationException exception) {
            if (results.isEmpty()) {
                return;   // 没有降落伞展开，不弹窗
            }
            final List<EventResult> events = new ArrayList<>(results);
            final RocketSections secs = sections;
            final double[] sm = firstSectionMasses;
            SwingUtilities.invokeLater(() -> new ResultsDialog(secs, sm, events).setVisible(true));
        }
    }
}