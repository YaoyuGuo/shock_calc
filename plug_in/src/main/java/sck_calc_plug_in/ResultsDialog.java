package sck_calc_plug_in;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class ResultsDialog extends JDialog {

    private static final double N_TO_LBF = 0.224809;

    public ResultsDialog(RocketSections sections, double[] sectionMasses, List<EventResult> events) {
        super((java.awt.Frame) null, "Shock Load Calc Result", false);

        // ---- 分离段表 ----
        int ns = sections.sectionCount();
        String[] secCols = {"#", "components", "range (cm，to nose cone)", "mass (kg)"};
        String[][] secRows = new String[ns + 1][];
        double sum = 0;
        for (int i = 0; i < ns; i++) {
            sum += sectionMasses[i];
            secRows[i] = new String[] {
                    String.valueOf(i + 1), sections.name(i), sections.rangeLabel(i),
                    String.format("%.3f", sectionMasses[i])};
        }
        secRows[ns] = new String[] {"", "total", "", String.format("%.3f", sum)};

        double residual = sections.residual(events.get(0).mass());
        String note = String.format("Mass not assigned to any component (motor, etc.): %.3f kg, added to the last section.", residual);
        if (residual < -0.005) {
            note += " The value is negative: check for a multi-stage rocket or mass overrides.";
        }

        // ---- 事件表 ----
        String[] evCols = {"#", "Device", "Time (s)", "Height (m)", "Speed (m/s)",
                "Joint (cm)", "upper mass (kg)", "lower mass (kg)",
                "opening shock N / lbf", "snatch load N / lbf", "sustained load N / lbf"};
        String[][] evRows = new String[events.size()][];
        for (int i = 0; i < events.size(); i++) {
            EventResult e = events.get(i);
            evRows[i] = new String[] {
                    String.valueOf(i + 1), e.device(),
                    num(e.time(), "%.2f"), num(e.altitude(), "%.1f"), num(e.speed(), "%.2f"),
                    e.joint() >= 0 ? num(e.jointX() * 100.0, "%.1f") : "n/a",
                    num(e.upper(), "%.3f"), num(e.lower(), "%.3f"),
                    force(e.opening()), force(e.snatch()), force(e.steady())};
        }

        // ---- 布局 ----
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));

        content.add(left(new JLabel("Sections (split along the rocket axis)")));
        content.add(Box.createVerticalStrut(4));
        content.add(tablePane(secCols, secRows, 640));
        content.add(Box.createVerticalStrut(4));
        content.add(left(new JLabel(note)));
        content.add(Box.createVerticalStrut(12));
        content.add(left(new JLabel("Recovery events, sorted by deployment time (limit loads, before the safety factor)")));
        content.add(Box.createVerticalStrut(4));
        content.add(tablePane(evCols, evRows, 1100));

        JButton copy = new JButton("Copy Result");
        String text = toText(secCols, secRows) + "\n" + toText(evCols, evRows);
        copy.addActionListener(ev -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null));
        JPanel bottom = new JPanel();
        bottom.add(copy);

        getContentPane().add(content, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
    }

    // ---- 辅助方法 ----

    private static JScrollPane tablePane(String[] cols, String[][] rows, int width) {
        JTable table = new JTable(new DefaultTableModel(rows, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setPreferredScrollableViewportSize(
                new Dimension(width, table.getRowHeight() * Math.max(rows.length, 1)));
        return left(new JScrollPane(table));
    }

    private static <T extends JComponent> T left(T c) {
        c.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return c;
    }

    private static String num(double d, String fmt) {
        return Double.isNaN(d) ? "n/a" : String.format(fmt, d);
    }

    private static String force(double n) {
        return Double.isNaN(n) ? "n/a" : String.format("%.1f / %.1f", n, n * N_TO_LBF);
    }

    private static String toText(String[] cols, String[][] rows) {
        StringBuilder sb = new StringBuilder(String.join("\t", cols)).append("\n");
        for (String[] r : rows) {
            sb.append(String.join("\t", r)).append("\n");
        }
        return sb.toString();
    }
}