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
        super((java.awt.Frame) null, "冲击载荷计算结果", false);

        // ---- 分离段表 ----
        int ns = sections.sectionCount();
        String[] secCols = {"#", "构件", "范围 (cm，距头锥尖)", "质量 (kg)"};
        String[][] secRows = new String[ns + 1][];
        double sum = 0;
        for (int i = 0; i < ns; i++) {
            sum += sectionMasses[i];
            secRows[i] = new String[] {
                    String.valueOf(i + 1), sections.name(i), sections.rangeLabel(i),
                    String.format("%.3f", sectionMasses[i])};
        }
        secRows[ns] = new String[] {"", "合计", "", String.format("%.3f", sum)};

        double residual = sections.residual(events.get(0).mass());
        String note = String.format("未归入任何组件的质量（发动机等）：%.3f kg，已计入最后一段。", residual);
        if (residual < -0.005) {
            note += " 该值为负，可能是多级火箭或质量覆盖设置，请核对。";
        }

        // ---- 事件表 ----
        String[] evCols = {"#", "回收装置", "时间 (s)", "高度 (m)", "速度 (m/s)",
                "分离位置 (cm)", "上方质量 (kg)", "下方质量 (kg)",
                "开伞冲击 N / lbf", "分离冲击 N / lbf", "持续载荷 N / lbf"};
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

        content.add(left(new JLabel("分离段（按轴向位置自动划分）")));
        content.add(Box.createVerticalStrut(4));
        content.add(tablePane(secCols, secRows, 640));
        content.add(Box.createVerticalStrut(4));
        content.add(left(new JLabel(note)));
        content.add(Box.createVerticalStrut(12));
        content.add(left(new JLabel("回收事件（按展开时间排序）")));
        content.add(Box.createVerticalStrut(4));
        content.add(tablePane(evCols, evRows, 1100));

        JButton copy = new JButton("复制结果");
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