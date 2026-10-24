package sck_calc_plug_in;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sweeps the safety factor over a range and builds the result table.
 *
 * Independent variable: safety factor (from min to max in steps of {@link #STEP}).
 * Held constant: opening-force factor Cx, cord stiffness k, and everything read from the simulation.
 * Dependent variables: the design loads (safety factor x limit load) of every deployment event.
 */
public final class SafetyFactorSweep {

    /** Fill density of the sweep. */
    public static final double STEP = 0.05;

    /** Upper bound on the number of steps, to keep the table a sane size. */
    private static final int MAX_STEPS = 2000;

    /** Values that stay constant during the sweep; written to the CSV for traceability. */
    public record Settings(double minFactor, double maxFactor,
                           double openingForceFactor, double cordStiffness) {
    }

    /** One table row: one safety factor for one deployment event. */
    public record Row(double safetyFactor, int event, String device,
                      double openingLimit, double openingDesign,
                      double snatchLimit, double snatchDesign,
                      double sustainedLimit, double sustainedDesign) {
    }

    private SafetyFactorSweep() {
    }

    /**
     * Safety factor values from the lower to the upper bound in steps of {@link #STEP}.
     * The upper bound is always included, even if it does not fall on the grid.
     */
    public static List<Double> factors(double a, double b) {
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        int n = (int) Math.floor((hi - lo) / STEP + 1e-9);
        n = Math.min(n, MAX_STEPS);

        List<Double> out = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            out.add(round2(lo + i * STEP));
        }
        double last = out.get(out.size() - 1);
        if (round2(hi) - last > 1e-9) {
            out.add(round2(hi));
        }
        return out;
    }

    /** Builds the table: rows are ordered by event first, then by safety factor. */
    public static List<Row> build(List<EventResult> events, double minFactor, double maxFactor) {
        List<Double> factors = factors(minFactor, maxFactor);
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            EventResult e = events.get(i);
            for (double sf : factors) {
                rows.add(new Row(sf, i + 1, e.device(),
                        e.openingLimit(), LoadCalculator.designLoad(e.openingLimit(), sf),
                        e.snatchLimit(), LoadCalculator.designLoad(e.snatchLimit(), sf),
                        e.sustainedLimit(), LoadCalculator.designLoad(e.sustainedLimit(), sf)));
            }
        }
        return rows;
    }

    public static String toCsv(List<Row> rows, Settings s) {
        StringBuilder sb = new StringBuilder();
        sb.append("safety_factor,event,device,")
          .append("opening_limit_N,opening_design_N,")
          .append("snatch_limit_N,snatch_design_N,")
          .append("sustained_limit_N,sustained_design_N,")
          .append("opening_force_factor,cord_stiffness_N_per_m\n");

        for (Row r : rows) {
            sb.append(fmt(r.safetyFactor(), 2)).append(',')
              .append(r.event()).append(',')
              .append(quote(r.device())).append(',')
              .append(fmt(r.openingLimit(), 3)).append(',')
              .append(fmt(r.openingDesign(), 3)).append(',')
              .append(fmt(r.snatchLimit(), 3)).append(',')
              .append(fmt(r.snatchDesign(), 3)).append(',')
              .append(fmt(r.sustainedLimit(), 3)).append(',')
              .append(fmt(r.sustainedDesign(), 3)).append(',')
              .append(fmt(s.openingForceFactor(), 4)).append(',')
              .append(fmt(s.cordStiffness(), 2)).append('\n');
        }
        return sb.toString();
    }

    /** Writes the CSV as UTF-8 with a byte order mark, so Excel detects the encoding. */
    public static void writeCsv(Path file, List<Row> rows, Settings s) throws IOException {
        Files.writeString(file, "\uFEFF" + toCsv(rows, s), StandardCharsets.UTF_8);
    }

    // ---- helpers ----

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Fixed-point number with a decimal point regardless of locale; NaN becomes an empty cell. */
    private static String fmt(double v, int decimals) {
        if (Double.isNaN(v)) {
            return "";
        }
        return String.format(Locale.ROOT, "%." + decimals + "f", v);
    }

    private static String quote(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
