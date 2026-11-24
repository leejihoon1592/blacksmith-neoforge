package io.github.leejihoon1592.blacksmith.research.strength;

public enum ScratchResult {
    NONE,
    FAINT,
    VISIBLE,
    DEEP;

    /** Strength values are counted in half-point units. */
    public static ScratchResult compare(int ingotHalfPoints, int sampleHalfPoints) {
        int difference = sampleHalfPoints - ingotHalfPoints;
        if (difference < 0) return NONE;
        if (difference == 0) return FAINT;
        if (difference >= 4) return DEEP;
        return VISIBLE;
    }
}
