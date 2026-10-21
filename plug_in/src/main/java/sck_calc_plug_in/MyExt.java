package sck_calc_plug_in;

import java.util.Set;

import javax.swing.SwingUtilities;

import info.openrocket.core.rocketcomponent.RecoveryDevice;
import info.openrocket.core.simulation.FlightDataBranch;
import info.openrocket.core.simulation.FlightDataType;
import info.openrocket.core.simulation.SimulationConditions;
import info.openrocket.core.simulation.SimulationStatus;
import info.openrocket.core.simulation.exception.SimulationException;
import info.openrocket.core.simulation.extension.AbstractSimulationExtension;
import info.openrocket.core.simulation.listeners.AbstractSimulationListener;

public class MyExt extends AbstractSimulationExtension {

    @Override
    public void initialize(SimulationConditions conditions) throws SimulationException {
        conditions.getSimulationListenerList().add(new MyListener());
    }

    @Override
    public String getName() {
        return "冲击载荷计算器";
    }

    @Override
    public String getDescription() {
        return "计算开伞冲击、分离冲击和隔板持续载荷，仿真结束后弹出结果窗口。";
    }

    // ---- 用户参数（随 .ork 文件保存）----
    public double getOpeningForceFactor() { return config.getDouble("openingForceFactor", 1.5); }
    public void setOpeningForceFactor(double v) { config.put("openingForceFactor", v); fireChangeEvent(); }

    public double getSafetyFactor() { return config.getDouble("safetyFactor", 2.0); }
    public void setSafetyFactor(double v) { config.put("safetyFactor", v); fireChangeEvent(); }

    public double getCordStiffness() { return config.getDouble("cordStiffness", 10000.0); }
    public void setCordStiffness(double v) { config.put("cordStiffness", v); fireChangeEvent(); }

    public double getUpperMass() { return config.getDouble("upperMass", 3.0); }
    public void setUpperMass(double v) { config.put("upperMass", v); fireChangeEvent(); }

    public double getLowerMass() { return config.getDouble("lowerMass", 5.0); }
    public void setLowerMass(double v) { config.put("lowerMass", v); fireChangeEvent(); }

    // ---- 监听器 ----
    private class MyListener extends AbstractSimulationListener {
        private boolean captured;
        private double v, rho, mass, cd, area;

        @Override
        public void startSimulation(SimulationStatus status) throws SimulationException {
            captured = false;
        }

        @Override
        public void postStep(SimulationStatus status) throws SimulationException {
            if (captured) return;

            Set<RecoveryDevice> deployed = status.getDeployedRecoveryDevices();
            if (deployed == null || deployed.isEmpty()) return;

            RecoveryDevice dev = deployed.iterator().next();
            FlightDataBranch data = status.getFlightDataBranch();

            v = status.getRocketVelocity().length();
            mass = data.getLast(FlightDataType.TYPE_MASS);
            double p = data.getLast(FlightDataType.TYPE_AIR_PRESSURE);
            double t = data.getLast(FlightDataType.TYPE_AIR_TEMPERATURE);
            rho = p / (287.05 * t);           // 理想气体：rho = P / (R * T)
            cd = dev.getCD();
            area = dev.getArea();
            captured = true;
        }

        @Override
        public void endSimulation(SimulationStatus status, SimulationException exception) {
            if (!captured) return;   // 没有展开过降落伞，就不弹窗

            double opening = LoadCalculator.openingShock(rho, v, cd, area, getOpeningForceFactor());
            double snatch = LoadCalculator.snatchLoad(v, getCordStiffness(), getUpperMass(), getLowerMass());
            double steady = LoadCalculator.steadyLoad(mass, getSafetyFactor());

            final double fv = v, frho = rho, fm = mass;
            SwingUtilities.invokeLater(() ->
                    new ResultsDialog(fv, frho, fm, opening, snatch, steady).setVisible(true));
        }
    }
}