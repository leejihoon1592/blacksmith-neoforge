package io.github.leejihoon1592.blacksmith.research.elasticity;

import java.util.OptionalInt;

public final class ElasticityMeasurement {
    public enum Timing {
        GOOD,
        BAD,
        MISS
    }

    private ElasticityMeasurement() {}

    /**
     * Uses half-point units for both the real elasticity and the measured value.
     * For BAD, the caller supplies the randomly selected direction of the error.
     */
    public static OptionalInt measure(int elasticityHalfPoints, Timing timing, boolean errorUpwards) {
        return switch (timing) {
            case GOOD -> OptionalInt.of(elasticityHalfPoints);
            case BAD -> OptionalInt.of(elasticityHalfPoints + (errorUpwards ? 2 : -2));
            case MISS -> OptionalInt.empty();
        };
    }
}
