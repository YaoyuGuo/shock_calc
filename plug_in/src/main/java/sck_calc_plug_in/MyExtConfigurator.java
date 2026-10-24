package sck_calc_plug_in;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
        addRow(panel, "Cx：", ext, "OpeningForceFactor");
        addRow(panel, "Safety Factor：", ext, "SafetyFactor");
        addRow(panel, "Cord Stiffness k (N/m)：", ext, "CordStiffness");

        // 可选：手动指定分离位置
        panel.add(new JLabel("Separation location (cm，to nose cone，sep. with comma，blank->auto)："));
        JTextField tf = new JTextField(ext.getBoundariesCm(), 18);
        tf.setToolTipText("e.g. 25, 48.5.default: use start of each cone.");
        tf.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { ext.setBoundariesCm(tf.getText()); }
            @Override public void removeUpdate(DocumentEvent e) { ext.setBoundariesCm(tf.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { ext.setBoundariesCm(tf.getText()); }
        });
        panel.add(tf, "w 140lp!, wrap");

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