package sck_calc_plug_in;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Draws the result charts and saves them as PNG files: design load versus safety factor,
 * and the opening force history of the canopy inflation.
 *
 * This is optional. It relies on JFreeChart, which OpenRocket bundles for its own plots.
 * If these imports do not resolve, delete this class and the PNG button in ResultsDialog;
 * the CSV export does not depend on it.
 */
public final class ChartExporter {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;

    private ChartExporter() {
    }

    public static void writePng(File file, List<SafetyFactorSweep.Row> rows) throws IOException {
        // One line per event and load type
        Map<String, XYSeries> series = new LinkedHashMap<>();
        for (SafetyFactorSweep.Row r : rows) {
            add(series, "Event " + r.event() + " opening shock", r.safetyFactor(), r.openingDesign());
            add(series, "Event " + r.event() + " snatch load", r.safetyFactor(), r.snatchDesign());
            add(series, "Event " + r.event() + " sustained load", r.safetyFactor(), r.sustainedDesign());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries s : series.values()) {
            dataset.addSeries(s);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Design load vs. safety factor",
                "Safety factor (-)",
                "Design load (N)",
                dataset,
                PlotOrientation.VERTICAL,
                true, false, false);

        save(chart, file);
    }

    /** Draws the opening force history of every deployment event and saves it as a PNG. */
    public static void writeInflationPng(File file, List<EventResult> events) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (int i = 0; i < events.size(); i++) {
            EventResult e = events.get(i);
            XYSeries s = new XYSeries("Event " + (i + 1) + ": " + e.device());
            double[] t = e.pflanz().curveTime();
            double[] f = e.pflanz().curveForce();
            for (int k = 0; k < t.length; k++) {
                s.add(t[k], f[k]);
            }
            dataset.addSeries(s);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Opening force during canopy inflation (Pflanz, limit load)",
                "Time after line stretch (s)",
                "Opening force (N)",
                dataset,
                PlotOrientation.VERTICAL,
                true, false, false);
        save(chart, file);
    }

    private static void save(JFreeChart chart, File file) throws IOException {
        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        BufferedImage image = chart.createBufferedImage(WIDTH, HEIGHT);
        if (!ImageIO.write(image, "png", file)) {
            throw new IOException("No PNG writer available");
        }
    }

    private static void add(Map<String, XYSeries> series, String name, double x, double y) {
        if (Double.isNaN(y)) {
            return;   // e.g. no joint associated with this event, so no snatch load
        }
        series.computeIfAbsent(name, k -> new XYSeries(k)).add(x, y);
    }
}
