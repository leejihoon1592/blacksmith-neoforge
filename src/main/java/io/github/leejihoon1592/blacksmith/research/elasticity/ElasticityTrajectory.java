package io.github.leejihoon1592.blacksmith.research.elasticity;

import java.util.OptionalInt;
import java.util.random.RandomGenerator;

import io.github.leejihoon1592.blacksmith.research.elasticity.ElasticityMeasurement.Timing;

/** Ball coordinates in ruler units, relative to the point of impact. */
public final class ElasticityTrajectory {
    public static final int IMPACT_TICK = 12;
    public static final int PEAK_TICK = 24;
    public static final int END_TICK = 36;
    private static final double RELEASE_HEIGHT = 16.0;
    private static final double MISS_PEAK_HEIGHT = 5.0;

    private final double peakHeight;
    private final double horizontalPerHeight;

    private ElasticityTrajectory(double peakHeight, double angleDegrees) {
        this.peakHeight = peakHeight;
        this.horizontalPerHeight = Math.tan(Math.toRadians(angleDegrees));
    }

    public static ElasticityTrajectory create(Timing timing, OptionalInt measuredHalfPoints, RandomGenerator random) {
        return switch (timing) {
            case GOOD -> new ElasticityTrajectory(measuredHalfPoints.orElseThrow() / 2.0, 0.0);
            case BAD -> new ElasticityTrajectory(measuredHalfPoints.orElseThrow() / 2.0,
                    signedAngle(random.nextDouble(1.0, 2.0), random));
            case MISS -> new ElasticityTrajectory(MISS_PEAK_HEIGHT,
                    signedAngle(random.nextDouble(30.0, 45.0), random));
        };
    }

    /** Takes fractional ticks so the screen can interpolate between game ticks. */
    public Position positionAt(double elapsedTicks) {
        double tick = Math.clamp(elapsedTicks, 0.0, END_TICK);
        if (tick <= IMPACT_TICK) {
            double progress = tick / IMPACT_TICK;
            return new Position(0.0, RELEASE_HEIGHT * (1.0 - progress * progress));
        }

        double progress = (tick - IMPACT_TICK) / (END_TICK - IMPACT_TICK);
        double height = 4.0 * peakHeight * progress * (1.0 - progress);
        double offset = 2.0 * peakHeight * progress * horizontalPerHeight;
        return new Position(offset, height);
    }

    private static double signedAngle(double magnitude, RandomGenerator random) {
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    public record Position(double x, double height) {}
}
