package io.github.leejihoon1592.blacksmith.research;

import java.util.ArrayList;
import java.util.List;

/** Deterministic server-tick experiment, independent of menus and rendering. */
public final class ThermalExperiment {
    public record Component(String id, double meltingPoint, int durationTicks, int amount, String quantity) {
        public Component {
            if (id.isBlank() || !Double.isFinite(meltingPoint) || meltingPoint <= 0
                    || durationTicks < 1 || amount < 1) throw new IllegalArgumentException("Invalid component");
        }
    }
    public record Observation(boolean onset, int component, double temperature, int elapsedTicks,
                              String quantity, boolean mixed) {}
    public static final class Progress {
        public int activeTicks;
        public int elapsedTicks;
        public boolean started;
        public boolean done;
        public boolean mixed;
    }

    private final List<Component> components;
    private final List<Progress> progress;

    public ThermalExperiment(List<Component> components) {
        this.components = List.copyOf(components);
        this.progress = components.stream().map(c -> new Progress()).toList();
    }

    public List<Observation> tick(double temperature) {
        List<Observation> observations = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            Component c = components.get(i);
            Progress p = progress.get(i);
            if (!p.started && temperature >= c.meltingPoint()) {
                p.started = true;
                observations.add(new Observation(true, i, c.meltingPoint(), 0, "", false));
            }
        }
        // Once liquids overlap, cooling cannot undo the loss of identity.
        long active = progress.stream().filter(p -> p.started && !p.done).count();
        if (active > 1) progress.stream().filter(p -> p.started && !p.done).forEach(p -> p.mixed = true);
        for (int i = 0; i < components.size(); i++) {
            Component c = components.get(i);
            Progress p = progress.get(i);
            if (!p.started || p.done) continue;
            p.elapsedTicks++;
            if (temperature >= c.meltingPoint()) p.activeTicks++;
            if (p.activeTicks >= c.durationTicks()) {
                p.done = true;
                observations.add(new Observation(false, i, temperature, p.elapsedTicks, c.quantity(), p.mixed));
            }
        }
        return observations;
    }

    public boolean complete() { return progress.stream().allMatch(p -> p.done); }
    public List<Component> components() { return components; }
    public List<Progress> progress() { return progress; }
}
