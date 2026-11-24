package io.github.leejihoon1592.blacksmith.research.polish;

public record PolishingDepth(double centimeters, Observation observation) {
    private static final double TARGET_CM = 1.0;
    private static final double CM_PER_HALF_POINT = 0.05;
    private static final int FAR_OVER_HALF_POINTS = 6;

    public enum Observation {
        TOO_SHALLOW,
        CLEAN,
        OVER_MARK,
        FAR_OVER_MARK
    }

    /** Both inputs are in half-point units. */
    public static PolishingDepth measure(int polishHalfPoints, int appliedForceHalfPoints) {
        int difference = appliedForceHalfPoints - polishHalfPoints;
        double depth = Math.max(0.0, TARGET_CM + difference * CM_PER_HALF_POINT);

        Observation observation;
        if (difference < 0) observation = Observation.TOO_SHALLOW;
        else if (difference == 0) observation = Observation.CLEAN;
        else if (difference < FAR_OVER_HALF_POINTS) observation = Observation.OVER_MARK;
        else observation = Observation.FAR_OVER_MARK;

        return new PolishingDepth(depth, observation);
    }
}
