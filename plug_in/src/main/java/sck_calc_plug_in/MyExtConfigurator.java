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
        // The property names must match the getters and setters in MyExt exactly
        addRow(panel, "Opening-force factor Cx:", ext, "OpeningForceFactor");
        addRow(panel, "Safety factor, minimum:", ext, "SafetyFactorMin");
        addRow(panel, "Safety factor, maximum:", ext, "SafetyFactorMax");
        addRow(panel, "Shock cord stiffness k (N/m):", ext, "CordStiffness");

        // Optional: manual joint positions
        panel.add(new JLabel("Joint positions (cm from nose tip, comma separated; empty = automatic):"));
        JTextField field = new JTextField(ext.getBoundariesCm(), 18);
        field.setToolTipText("Example: 25, 48.5. Empty means every body tube starts a new section.");
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                ext.setBoundariesCm(field.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                ext.setBoundariesCm(field.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                ext.setBoundariesCm(field.getText());
            }
        });
        panel.add(field, "w 140lp!, wrap");

        return panel;
    }

    private void addRow(JPanel panel, String label, MyExt ext, String property) {
        panel.add(new JLabel(label));
        DoubleModel model = new DoubleModel(ext, property, UnitGroup.UNITS_NONE, 0);
        JSpinner spinner = new JSpinner(model.getSpinnerModel());
        spinner.setEditor(new SpinnerEditor(spinner));
        panel.add(spinner, "w 80lp!, wrap");
    }
}
