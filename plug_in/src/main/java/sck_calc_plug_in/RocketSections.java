package sck_calc_plug_in;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import info.openrocket.core.rocketcomponent.BodyComponent;
import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.util.Coordinate;

/**
 * Splits the rocket along its axis into sections and sums the component masses of each section.
 *
 * Joints (section boundaries):
 *  - automatic: the start of every body tube (so the nose cone is its own section,
 *    and every body tube begins a new section; transitions stay with the section in front of them);
 *  - manual: positions in cm from the nose tip, entered by the user (replaces the automatic joints).
 *
 * N joints give N + 1 sections. A component belongs to the section that contains its axial center.
 * Mass overrides set in the model are respected.
 */
public final class RocketSections {

    private final double[] bounds;   // joint positions in metres from the nose tip, ascending
    private final double[] masses;   // component mass per section (kg), length = bounds.length + 1
    private final String[] names;    // body components found in each section

    private final List<double[]> tubes;   // body tubes: {start x, end x, inner radius} in metres

    private RocketSections(double[] bounds, double[] masses, String[] names, List<double[]> tubes) {
        this.bounds = bounds;
        this.masses = masses;
        this.names = names;
        this.tubes = tubes;
    }

    public static RocketSections analyze(RocketComponent root, String overrideCm) {
        List<RocketComponent> all = new ArrayList<>();
        collect(root, all);

        // 1. Joint positions
        List<Double> raw = parseCm(overrideCm);
        if (raw.isEmpty()) {
            for (RocketComponent c : all) {
                if (c instanceof BodyTube) {
                    for (Coordinate loc : c.toAbsolute(new Coordinate(0, 0, 0))) {
                        raw.add(loc.x);
                    }
                }
            }
        }
        Collections.sort(raw);

        List<Double> clean = new ArrayList<>();
        for (double b : raw) {
            if (b > 1e-4 && (clean.isEmpty() || b - clean.get(clean.size() - 1) > 1e-4)) {
                clean.add(b);
            }
        }
        double[] bounds = new double[clean.size()];
        for (int i = 0; i < bounds.length; i++) {
            bounds[i] = clean.get(i);
        }

        // 2. Masses and names per section
        int n = bounds.length + 1;
        double[] masses = new double[n];
        List<List<String>> parts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            parts.add(new ArrayList<>());
        }

        for (RocketComponent c : all) {
            double m = effectiveMass(c);
            boolean body = c instanceof BodyComponent;
            if (m <= 0 && !body) {
                continue;
            }
            // getMass() is the mass of all instances together (e.g. the whole fin set),
            // so it is shared equally between the instance locations.
            Coordinate[] locations = c.toAbsolute(new Coordinate(0, 0, 0));
            for (Coordinate loc : locations) {
                int idx = index(bounds, loc.x + c.getLength() / 2.0);
                if (m > 0) {
                    masses[idx] += m / locations.length;
                }
                if (body && !parts.get(idx).contains(c.getName())) {
                    parts.get(idx).add(c.getName());
                }
            }
        }

        // 3. Inner radius of every body tube, used as the default bulkhead radius
        List<double[]> tubes = new ArrayList<>();
        for (RocketComponent c : all) {
            if (c instanceof BodyTube tube) {
                for (Coordinate loc : c.toAbsolute(new Coordinate(0, 0, 0))) {
                    tubes.add(new double[] {loc.x, loc.x + tube.getLength(), tube.getInnerRadius()});
                }
            }
        }

        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            names[i] = parts.get(i).isEmpty() ? "-" : String.join(" + ", parts.get(i));
        }
        return new RocketSections(bounds, masses, names, tubes);
    }

    // ---- queries ----

    public int sectionCount() {
        return masses.length;
    }

    public int jointCount() {
        return bounds.length;
    }

    /** Joint position in metres from the nose tip. */
    public double jointX(int joint) {
        return bounds[joint];
    }

    public String name(int section) {
        return names[section];
    }

    public String rangeLabel(int section) {
        if (bounds.length == 0) {
            return "all";
        }
        if (section == 0) {
            return "< " + cm(bounds[0]);
        }
        if (section == bounds.length) {
            return ">= " + cm(bounds[section - 1]);
        }
        return cm(bounds[section - 1]) + " - " + cm(bounds[section]);
    }

    /** Index of the joint closest to the given axial position, or -1 if there are no joints. */
    public int nearestJoint(double x) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int j = 0; j < bounds.length; j++) {
            double d = Math.abs(bounds[j] - x);
            if (d < bestDist) {
                bestDist = d;
                best = j;
            }
        }
        return best;
    }

    /**
     * Inner radius of the body tube at the given axial position (metres), which is the radius of a
     * bulkhead fitted there. If no tube covers the position, the tube that starts closest to it is
     * used. Returns NaN if the rocket has no body tube.
     */
    public double tubeInnerRadius(double x) {
        double best = Double.NaN;
        double bestDist = Double.MAX_VALUE;
        for (double[] t : tubes) {
            if (x >= t[0] - 1e-6 && x < t[1]) {
                return t[2];
            }
            double d = Math.abs(t[0] - x);
            if (d < bestDist) {
                bestDist = d;
                best = t[2];
            }
        }
        return best;
    }

    /** Mass in the model that is not covered by any component (motor, casing, ...). */
    public double residual(double totalMass) {
        double sum = 0;
        for (double m : masses) {
            sum += m;
        }
        return totalMass - sum;
    }

    /** Section masses with the unassigned mass (motor etc.) added to the last (aft-most) section. */
    public double[] sectionMasses(double totalMass) {
        double[] out = masses.clone();
        out[out.length - 1] += residual(totalMass);
        return out;
    }

    /** Mass of all sections in front of (and including the section ending at) the given joint. */
    public static double upperMass(int joint, double[] sectionMasses) {
        double s = 0;
        for (int i = 0; i <= joint; i++) {
            s += sectionMasses[i];
        }
        return s;
    }

    /** Mass of all sections behind the given joint. */
    public static double lowerMass(int joint, double[] sectionMasses) {
        double s = 0;
        for (int i = joint + 1; i < sectionMasses.length; i++) {
            s += sectionMasses[i];
        }
        return s;
    }

    // ---- helpers ----

    private static void collect(RocketComponent c, List<RocketComponent> out) {
        out.add(c);
        for (RocketComponent child : c.getChildren()) {
            collect(child, out);
        }
    }

    /**
     * Mass of one component as OpenRocket uses it: the override value if the mass is overridden,
     * otherwise the computed component mass. A component whose ancestor overrides the mass of its
     * subcomponents contributes nothing, because the ancestor's override already includes it.
     */
    private static double effectiveMass(RocketComponent c) {
        for (RocketComponent p = c.getParent(); p != null; p = p.getParent()) {
            if (p.isMassOverridden() && p.isSubcomponentsOverriddenMass()) {
                return 0;
            }
        }
        return c.getMass();
    }

    private static int index(double[] bounds, double x) {
        int i = 0;
        while (i < bounds.length && x >= bounds[i]) {
            i++;
        }
        return i;
    }

    private static List<Double> parseCm(String s) {
        List<Double> out = new ArrayList<>();
        if (s == null) {
            return out;
        }
        for (String tok : s.split("[,;\\s\uFF0C\uFF1B]+")) {
            if (tok.isEmpty()) {
                continue;
            }
            try {
                out.add(Double.parseDouble(tok) / 100.0);
            } catch (NumberFormatException ignored) {
                // skip invalid tokens
            }
        }
        return out;
    }

    private static String cm(double metres) {
        return String.format(Locale.ROOT, "%.1f", metres * 100.0);
    }
}
