package io.github.leejihoon1592.blacksmith.research.strength;

import java.util.List;
import java.util.stream.IntStream;

public final class StrengthCandidates {
    private static final int MIN_HALF_POINTS = 2;
    private static final int MAX_HALF_POINTS = 30;
    static final int COUNT = 10;
    private static final int[] PREFERRED_OFFSETS = {8, 3, 0, 5};

    static final int TURNS = PREFERRED_OFFSETS.length;

    private StrengthCandidates() {}

    /** Both the ingot strength and the returned candidates are in half-point units. */
    public static List<Integer> forTurn(int ingotHalfPoints, int turn) {
        if (ingotHalfPoints < MIN_HALF_POINTS || ingotHalfPoints > MAX_HALF_POINTS) {
            throw new IllegalArgumentException("Strength must be between 1 and 15");
        }
        if (turn < 1 || turn > PREFERRED_OFFSETS.length) {
            throw new IllegalArgumentException("Turn must be between 1 and " + PREFERRED_OFFSETS.length);
        }

        int preferredStart = ingotHalfPoints - PREFERRED_OFFSETS[turn - 1];
        int lastStart = MAX_HALF_POINTS - COUNT + 1;
        int start = Math.clamp(preferredStart, MIN_HALF_POINTS, lastStart);
        return IntStream.range(start, start + COUNT).boxed().toList();
    }
}
