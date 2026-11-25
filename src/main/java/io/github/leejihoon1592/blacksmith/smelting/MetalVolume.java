package io.github.leejihoon1592.blacksmith.smelting;

/** Metal volume in half-millibuckets. */
final class MetalVolume {
    static final int DUST = 5;
    static final int FRAGMENT = 4 * DUST;
    static final int INGOT = 9 * FRAGMENT;
    static final int BLOCK = 9 * INGOT;

    private MetalVolume() {}

    static String format(int units) {
        return (units / 2) + (units % 2 == 0 ? "" : ".5");
    }

    static int fromLegacyIngots(int count) {
        return Math.clamp(count, 0, BLOCK / INGOT) * INGOT;
    }

    static int fromWholeMillibuckets(int amount) {
        return Math.clamp(amount, 0, BLOCK / 2) * 2;
    }
}
