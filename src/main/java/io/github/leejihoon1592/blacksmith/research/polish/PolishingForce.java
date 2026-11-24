package io.github.leejihoon1592.blacksmith.research.polish;

public final class PolishingForce {
    public enum Timing {
        GOOD,
        BAD,
        MISS
    }

    private PolishingForce() {}

    /** Computes the applied force in half-point units using the chosen error direction. */
    public static int apply(int selectedHalfPoints, Timing timing, boolean errorUpwards) {
        int errorHalfPoints = switch (timing) {
            case GOOD -> 0;
            case BAD -> 2;
            case MISS -> 6;
        };
        return selectedHalfPoints + (errorUpwards ? errorHalfPoints : -errorHalfPoints);
    }
}
