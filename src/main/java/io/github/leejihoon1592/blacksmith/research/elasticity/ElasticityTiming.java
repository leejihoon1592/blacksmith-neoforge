package io.github.leejihoon1592.blacksmith.research.elasticity;

import io.github.leejihoon1592.blacksmith.research.elasticity.ElasticityMeasurement.Timing;

public final class ElasticityTiming {
    public static final int TARGET_TICK = 30;
    private static final int GOOD_WINDOW_TICKS = 2;
    private static final int BAD_WINDOW_TICKS = 5;
    public static final int LAST_CLICK_TICK = TARGET_TICK + BAD_WINDOW_TICKS;

    private ElasticityTiming() {}

    /** Judges a click using the elapsed game ticks since the attempt started. */
    public static Timing judge(long elapsedTicks) {
        if (elapsedTicks < TARGET_TICK - BAD_WINDOW_TICKS || elapsedTicks > LAST_CLICK_TICK) {
            return Timing.MISS;
        }
        return Math.abs(elapsedTicks - TARGET_TICK) <= GOOD_WINDOW_TICKS
                ? Timing.GOOD : Timing.BAD;
    }
}
