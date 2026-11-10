package sck_calc_plug_in;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.function.IntConsumer;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

        addHeading(panel, "Opening force");
        addChoice(panel, "Method:", MyExt.METHOD_NAMES, ext.getOpeningMethod(), ext::setOpeningMethod,
                "Pflanz accounts for the rocket slowing down while the canopy fills.");
        addRow(panel, "Opening-force factor Cx:", ext, "OpeningForceFactor",
                "Infinite-mass opening-force coefficient of the canopy type (about 1.0 to 1.8).");
        addRow(panel, "Canopy fill constant n:", ext, "FillConstant",
                "Filling time = n x nominal diameter / speed. About 8 for solid cloth canopies, 14 for ribbon.");
        addRow(panel, "Inflation exponent j:", ext, "InflationExponent",
                "Drag area grows as (t / tf)^j. Pflanz: 2 for solid cloth, 1 for slotted, 0.5 for reefed canopies.");

        addHeading(panel, "Shock cord snatch load");
        addRow(panel, "Separation speed (m/s):", ext, "SeparationSpeed",
                "Relative speed of the two bodies when the shock cord goes taut.");
        addRow(panel, "Shock cord stiffness k (N/m):", ext, "CordStiffness",
                "k = breaking strength / (elongation at break x cord length).");

        addHeading(panel, "Safety factor sweep (exported table and chart)");
        addRow(panel, "Safety factor, minimum:", ext, "SafetyFactorMin", null);
        addRow(panel, "Safety factor, maximum:", ext, "SafetyFactorMax", null);

        addHeading(panel, "Bolt and bulkhead sizing");
        addRow(panel, "Sizing safety factor:", ext, "SizingSafetyFactor",
                "Design load = this factor x governing limit load.");
        addRow(panel, "Threaded legs sharing the load:", ext, "BoltLegs",
                "2 for a U-bolt, 1 for an eye-bolt.");
        addChoice(panel, "Bolt material:", Sizing.BOLT_NAMES, ext.getBoltMaterial(), ext::setBoltMaterial,
                "Allowable tensile stress = proof strength.");
        addRow(panel, "Custom bolt strength (MPa):", ext, "BoltCustomStrength",
                "Used only when the bolt material is Custom.");
        addChoice(panel, "Bulkhead material:", Sizing.PLATE_NAMES, ext.getPlateMaterial(), ext::setPlateMaterial,
                "Allowable bending stress / allowable shear stress.");
        addRow(panel, "Custom bulkhead bending strength (MPa):", ext, "PlateCustomBending",
                "Used only when the bulkhead material is Custom.");
        addRow(panel, "Custom bulkhead shear strength (MPa):", ext, "PlateCustomShear",
                "Used only when the bulkhead material is Custom.");
        addRow(panel, "Load circle radius (mm):", ext, "LoadRadiusMm",
                "Half the leg spacing of the U-bolt, or the washer radius of an eye-bolt.");
        addRow(panel, "Washer or nut outer diameter (mm):", ext, "WasherDiameterMm",
                "Diameter of the cylinder each leg would shear out of the bulkhead.");
        addRow(panel, "Bulkhead radius (mm, 0 = automatic):", ext, "BulkheadRadiusMm",
                "Automatic uses the inner radius of the body tube at the joint.");

        addHeading(panel, "Sections");
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

        // On a small screen the settings scroll, so the Close button stays reachable
        Dimension preferred = panel.getPreferredSize();
        int maxHeight = Toolkit.getDefaultToolkit().getScreenSize().height - 220;
        if (preferred.height <= maxHeight) {
            return panel;
        }
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(preferred.width + 24, maxHeight));
        return scroll;
    }

    private void addHeading(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, "span, gaptop 8lp, wrap");
    }

    private void addRow(JPanel panel, String label, MyExt ext, String property, String tooltip) {
        JLabel l = new JLabel(label);
        panel.add(l);
        DoubleModel model = new DoubleModel(ext, property, UnitGroup.UNITS_NONE, 0);
        JSpinner spinner = new JSpinner(model.getSpinnerModel());
        spinner.setEditor(new SpinnerEditor(spinner));
        if (tooltip != null) {
            l.setToolTipText(tooltip);
            spinner.setToolTipText(tooltip);
        }
        panel.add(spinner, "w 80lp!, wrap");
    }

    private void addChoice(JPanel panel, String label, String[] options, int selected,
                           IntConsumer setter, String tooltip) {
        JLabel l = new JLabel(label);
        l.setToolTipText(tooltip);
        panel.add(l);
        JComboBox<String> box = new JComboBox<>(options);
        box.setSelectedIndex(Math.max(0, Math.min(options.length - 1, selected)));
        box.setToolTipText(tooltip);
        box.addActionListener(e -> setter.accept(box.getSelectedIndex()));
        panel.add(box, "wrap");
    }
}
