package io.github.leejihoon1592.blacksmith.research.polish;

final class PolishingTiming {
    static final int END_TICK = 40;
    static final double GOOD_RADIUS = 0.08;
    static final double BAD_RADIUS = 0.20;

    private PolishingTiming() {}

    static double position(double ticks) {
        double progress = Math.clamp(ticks, 0, END_TICK) / END_TICK;
        return 1.0 - Math.abs(2.0 * progress - 1.0);
    }

    static PolishingForce.Timing judge(long ticks) {
        double distance = Math.abs(position(ticks) - 0.5);
        if (distance <= GOOD_RADIUS) return PolishingForce.Timing.GOOD;
        if (distance <= BAD_RADIUS + 1e-9) return PolishingForce.Timing.BAD;
        return PolishingForce.Timing.MISS;
    }
}
