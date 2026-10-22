package sck_calc_plug_in;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ResultsDialog extends JDialog {

    private static final double N_TO_LBF = 0.224809;

    public ResultsDialog(double v, double rho, double mass,
                         double opening, double snatch, double steady) {
        super((java.awt.Frame) null, "Shock Load Calculation Result", false);

        StringBuilder text = new StringBuilder();
        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 6));
        grid.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));

        addRow(grid, text, "Velocity at Deployment", String.format("%.2f m/s", v));
        addRow(grid, text, "Air Density at Deployment", String.format("%.4f kg/m³", rho));
        addRow(grid, text, "Total Mass at Deployment", String.format("%.3f kg", mass));
        addRow(grid, text, "Opening Shock Load", fmtForce(opening));
        addRow(grid, text, "Separation Shock Load", fmtForce(snatch));
        addRow(grid, text, "Bulkhead Sustained Load", fmtForce(steady));

        JButton copy = new JButton("Copy Result");
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text.toString()), null));

        JPanel bottom = new JPanel();
        bottom.add(copy);

        getContentPane().add(grid, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
    }

    private static String fmtForce(double n) {
        return String.format("%.1f N  (%.1f lbf)", n, n * N_TO_LBF);
    }

    private static void addRow(JPanel grid, StringBuilder text, String name, String value) {
        grid.add(new JLabel(name + "："));
        grid.add(new JLabel(value));
        text.append(name).append("：").append(value).append("\n");
    }
}