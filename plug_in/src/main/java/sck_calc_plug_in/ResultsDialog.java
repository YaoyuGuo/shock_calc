package sck_calc_plug_in;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class ResultsDialog extends JDialog {

    private static final double N_TO_LBF = 0.224809;
    private static final int TABLE_WIDTH = 1180;

    private final List<EventResult> events;
    private final List<SafetyFactorSweep.Row> rows;
    private final SafetyFactorSweep.Settings settings;

    public ResultsDialog(Report report) {
        super((java.awt.Frame) null, "Shock Load Results", false);
        RocketSections sections = report.sections();
        double[] sectionMasses = report.sectionMasses();
        this.events = report.events();
        this.settings = report.sweep();
        this.rows = SafetyFactorSweep.build(events, settings.minFactor(), settings.maxFactor());

        // ---- Section table ----
        int sectionCount = sections.sectionCount();
        String[] sectionColumns = {"#", "Components", "Range (cm from nose tip)", "Mass (kg)"};
        String[][] sectionRows = new String[sectionCount + 1][];
        double sum = 0;
        for (int i = 0; i < sectionCount; i++) {
            sum += sectionMasses[i];
            sectionRows[i] = new String[] {
                    String.valueOf(i + 1), sections.name(i), sections.rangeLabel(i),
                    String.format(Locale.ROOT, "%.3f", sectionMasses[i])};
        }
        sectionRows[sectionCount] = new String[] {"", "Total", "", String.format(Locale.ROOT, "%.3f", sum)};

        double residual = sections.residual(events.get(0).mass());
        String note = String.format(Locale.ROOT,
                "Mass not assigned to any component (motor, etc.): %.3f kg, added to the last section.",
                residual);
        if (residual < -0.005) {
            note += " The value is negative: check for a multi-stage rocket.";
        }

        // ---- Event table (limit loads) ----
        String[] eventColumns = {"#", "Device", "Time (s)", "Altitude (m)", "Speed (m/s)",
                "Joint (cm)", "Upper (kg)", "Lower (kg)",
                "Opening N / lbf", "Snatch N / lbf", "Sustained N / lbf",
                "Governing N / lbf"};
        String[][] eventRows = new String[events.size()][];
        for (int i = 0; i < events.size(); i++) {
            EventResult e = events.get(i);
            eventRows[i] = new String[] {
                    String.valueOf(i + 1), e.device(),
                    num(e.time(), "%.2f"), num(e.altitude(), "%.1f"), num(e.speed(), "%.2f"),
                    e.joint() >= 0 ? num(e.jointX() * 100.0, "%.1f") : "n/a",
                    num(e.upperMass(), "%.3f"), num(e.lowerMass(), "%.3f"),
                    force(e.openingLimit()), force(e.snatchLimit()), force(e.sustainedLimit()),
                    force(e.governingLimit())};
        }

        // ---- Inflation table (Pflanz) ----
        String[] inflationColumns = {"#", "Device", "Density (kg/m3)", "CdS (m2)", "D0 (m)",
                "Fill time (s)", "A", "X1 Pflanz", "X1 + gravity",
                "Inf. mass N / lbf", "Finite mass N / lbf", "Peak (g)",
                "Upper / lower (N)"};
        String[][] inflationRows = new String[events.size()][];
        for (int i = 0; i < events.size(); i++) {
            EventResult e = events.get(i);
            PflanzModel.Result p = e.pflanz();
            inflationRows[i] = new String[] {
                    String.valueOf(i + 1), e.device(),
                    num(e.airDensity(), "%.3f"), num(e.dragCoefficient() * e.area(), "%.3f"),
                    num(e.nominalDiameter(), "%.3f"),
                    num(p.fillTime(), "%.3f"), num(p.ballisticParameter(), "%.3f"),
                    num(p.x1Analytic(), "%.3f"), num(p.x1(), "%.3f"),
                    force(e.openingInfinite()), force(p.peakForce()),
                    num(e.peakDecelerationG(), "%.2f"),
                    e.joint() >= 0
                            ? num(e.openingUpperShare(), "%.0f") + " / " + num(e.openingLowerShare(), "%.0f")
                            : "n/a"};
        }

        // ---- Sizing table ----
        String[] sizingColumns = {"#", "Device", "Limit load (N)", "Design N / lbf",
                "Area per leg (mm2)", "Metric", "UNC",
                "Bulkhead R (mm)", "t bending (mm)", "t shear (mm)",
                "t minimum (mm)"};
        String[][] sizingRows = new String[events.size()][];
        for (int i = 0; i < events.size(); i++) {
            EventResult e = events.get(i);
            Sizing.Result z = e.sizing();
            sizingRows[i] = new String[] {
                    String.valueOf(i + 1), e.device(),
                    num(e.governingLimit(), "%.1f"), force(z.designLoad()),
                    num(z.areaPerLeg(), "%.2f"), z.metric(), z.unc(),
                    num(z.bulkheadRadius() * 1000, "%.1f"),
                    num(z.thicknessBending() * 1000, "%.2f"), num(z.thicknessShear() * 1000, "%.2f"),
                    num(z.thickness() * 1000, "%.2f")};
        }

        String openingInfo = String.format(Locale.ROOT,
                "Opening shock method: %s. Cx = %.2f, fill constant n = %.1f, exponent j = %.1f. "
                        + "Snatch load: separation speed %.1f m/s, cord stiffness %.0f N/m.",
                settings.openingMethod(), settings.openingForceFactor(), settings.fillConstant(),
                settings.inflationExponent(), settings.separationSpeed(), settings.cordStiffness());

        String sweepInfo = String.format(Locale.ROOT,
                "Design load = safety factor x limit load. Sweep: %.2f to %.2f, step %.2f (%d rows per event).",
                settings.minFactor(), settings.maxFactor(), SafetyFactorSweep.STEP,
                SafetyFactorSweep.factors(settings.minFactor(), settings.maxFactor()).size());

        // ---- Layout ----
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));

        content.add(left(new JLabel("Sections (split along the rocket axis)")));
        content.add(Box.createVerticalStrut(4));
        content.add(tablePane(sectionColumns, sectionRows, 640));
        content.add(Box.createVerticalStrut(4));
        content.add(left(new JLabel(note)));
        content.add(Box.createVerticalStrut(12));

        content.add(left(new JLabel(
                "Recovery events, sorted by deployment time (limit loads, before the safety factor)")));
        content.add(Box.createVerticalStrut(4));
        content.add(tablePane(eventColumns, eventRows, TABLE_WIDTH));
        content.add(Box.createVerticalStrut(4));
        content.add(left(new JLabel(openingInfo)));
        content.add(Box.createVerticalStrut(12));

        content.add(left(new JLabel("Canopy inflation (Pflanz method)")));
        content.add(Box.createVerticalStrut(4));
        content.add(tablePane(inflationColumns, inflationRows, TABLE_WIDTH));
        content.add(Box.createVerticalStrut(12));

        content.add(left(new JLabel("Bolt and bulkhead sizing for the governing load")));
        content.add(Box.createVerticalStrut(4));
        content.add(tablePane(sizingColumns, sizingRows, TABLE_WIDTH));
        content.add(Box.createVerticalStrut(4));
        content.add(left(new JLabel(Sizing.describe(report.sizing()))));
        content.add(Box.createVerticalStrut(12));
        content.add(left(new JLabel(sweepInfo)));

        JButton exportCsv = new JButton("Export table (CSV)...");
        exportCsv.addActionListener(ev -> exportCsv());
        JButton exportPng = new JButton("Export chart (PNG)...");
        exportPng.addActionListener(ev -> exportPng());
        JButton exportInflation = new JButton("Export inflation curves (PNG)...");
        exportInflation.addActionListener(ev -> exportInflation());
        JButton copy = new JButton("Copy summary");
        String summary = toText(sectionColumns, sectionRows) + "\n" + toText(eventColumns, eventRows)
                + "\n" + toText(inflationColumns, inflationRows) + "\n" + toText(sizingColumns, sizingRows);
        copy.addActionListener(ev -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(summary), null));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        buttons.add(exportCsv);
        buttons.add(exportPng);
        buttons.add(exportInflation);
        buttons.add(copy);

        // The content scrolls if it does not fit on the screen
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(getWidth() + 24, screen.width - 80), Math.min(getHeight() + 24, screen.height - 80));
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
    }

    // ---- Export actions ----

    private void exportCsv() {
        File file = chooseFile("shock_load_sweep.csv", ".csv");
        if (file == null) {
            return;
        }
        try {
            SafetyFactorSweep.writeCsv(file.toPath(), rows, settings);
            JOptionPane.showMessageDialog(this, "Saved " + file.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the file: " + ex.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportPng() {
        File file = chooseFile("shock_load_sweep.png", ".png");
        if (file == null) {
            return;
        }
        try {
            ChartExporter.writePng(file, rows);
            JOptionPane.showMessageDialog(this, "Saved " + file.getAbsolutePath());
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the chart: " + ex.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportInflation() {
        File file = chooseFile("shock_load_inflation.png", ".png");
        if (file == null) {
            return;
        }
        try {
            ChartExporter.writeInflationPng(file, events);
            JOptionPane.showMessageDialog(this, "Saved " + file.getAbsolutePath());
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the chart: " + ex.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Shows a save dialog and returns the chosen file with the given extension, or null if cancelled. */
    private File chooseFile(String defaultName, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(extension)) {
            file = new File(file.getParentFile(), file.getName() + extension);
        }
        return file;
    }

    // ---- Helpers ----

    private static JScrollPane tablePane(String[] columns, String[][] data, int width) {
        JTable table = new JTable(new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setPreferredScrollableViewportSize(
                new Dimension(width, table.getRowHeight() * Math.max(data.length, 1)));
        return left(new JScrollPane(table));
    }

    private static <T extends JComponent> T left(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    private static String num(double value, String format) {
        return Double.isNaN(value) || Double.isInfinite(value)
                ? "n/a"
                : String.format(Locale.ROOT, format, value);
    }

    private static String force(double newtons) {
        return Double.isNaN(newtons)
                ? "n/a"
                : String.format(Locale.ROOT, "%.1f / %.1f", newtons, newtons * N_TO_LBF);
    }

    private static String toText(String[] columns, String[][] data) {
        StringBuilder sb = new StringBuilder(String.join("\t", columns)).append("\n");
        for (String[] r : data) {
            sb.append(String.join("\t", r)).append("\n");
        }
        return sb.toString();
    }
}
