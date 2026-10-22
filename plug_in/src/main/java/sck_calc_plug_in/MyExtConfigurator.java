package sck_calc_plug_in;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import info.openrocket.core.document.Simulation;
import info.openrocket.core.plugin.Plugin;
import info.openrocket.core.unit.UnitGroup;
import info.openrocket.swing.gui.SpinnerEditor;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.simulation.extension.AbstractSwingSimulationExtensionConfigurator;

@Plugin
public class MyExtConfigurator extends AbstractSwingSimulationExtensionConfigurator<MyExt> {

    public MyExtConfigurator() {
        super(MyExt.class);
    }

    @Override
    protected JComponent getConfigurationComponent(MyExt ext, Simulation sim, JPanel panel) {
        addRow(panel, "Opening Force Factor Cx：", ext, "OpeningForceFactor");
        addRow(panel, "Safety Factor：", ext, "SafetyFactor");
        addRow(panel, "Cord Stiffness k (N/m)：", ext, "CordStiffness");
        addRow(panel, "Upper Mass (kg)：", ext, "UpperMass");
        addRow(panel, "Lower Mass (kg)：", ext, "LowerMass");
        return panel;
    }

    private void addRow(JPanel panel, String label, MyExt ext, String prop) {
        panel.add(new JLabel(label));
        DoubleModel m = new DoubleModel(ext, prop, UnitGroup.UNITS_NONE, 0);
        JSpinner spin = new JSpinner(m.getSpinnerModel());
        spin.setEditor(new SpinnerEditor(spin));
        panel.add(spin, "w 80lp!, wrap");
    }
}