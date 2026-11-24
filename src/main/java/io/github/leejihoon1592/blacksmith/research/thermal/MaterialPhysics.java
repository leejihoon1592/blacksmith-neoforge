package io.github.leejihoon1592.blacksmith.research.thermal;

import io.github.leejihoon1592.blacksmith.item.MetalIngotItem.Stats;

/** Sheet3 formulas. Thermal and weight properties use pre-bonus material stats. */
public final class MaterialPhysics {
    public static int meltingPoint(Stats s) {
        return roundUpTen(1000 + s.durability()*38 + s.hardness()*38
                + s.elasticity()*25 + s.polish()*15 - s.conductivity()*20);
    }

    public static double meltingSeconds(Stats s, double baseSeconds) {
        return baseSeconds + s.durability()*2 + s.hardness()*3
                + s.elasticity()*2 - s.polish()*2 - s.conductivity()*2;
    }

    public static int weight(Stats s) {
        double resistant = s.durability() + s.hardness() + s.elasticity();
        double receptive = s.polish() + s.conductivity();
        double first = 0;
        if (resistant <= 10) first = 2;
        else if (resistant >= 20) first = -4;
        else if (resistant >= 15) first = -2;
        double second = 0;
        if (receptive <= 10) second = 2;
        else if (receptive >= 15) second = -2;
        double subtotal = resistant - receptive*0.5 + first + second;
        double last = 0;
        if (subtotal >= 1 && subtotal <= 6) last = 4;
        else if (subtotal >= 7 && subtotal <= 10) last = 2;
        else if (subtotal >= 13 && subtotal <= 15) last = -2;
        else if (subtotal >= 16) last = -4;
        return (int)Math.ceil(Math.max(1, subtotal + last));
    }

    public static int difficulty(Stats finalStats, int weight) {
        return (int)Math.ceil((finalStats.durability() + finalStats.polish() + finalStats.hardness()
                + finalStats.elasticity() + finalStats.conductivity() + weight)/5);
    }

    public static int forgingMinimum(Stats s) { return roundUpTen(meltingPoint(s)*0.7); }
    public static int forgingMaximum(Stats s) { return roundUpTen(meltingPoint(s)*0.9); }
    private static int roundUpTen(double value) { return (int)Math.ceil(value/10)*10; }
    private MaterialPhysics() {}
}
