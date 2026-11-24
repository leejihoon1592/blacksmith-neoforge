package io.github.leejihoon1592.blacksmith.item;

import net.minecraft.world.item.Item;

public final class MetalIngotItem extends Item {
    public record Stats(double durability, double polish, double hardness, double elasticity, double conductivity) {}

    private final Stats baseStats;

    public MetalIngotItem(Stats baseStats) {
        super(new Item.Properties());
        this.baseStats = baseStats;
    }

    public Stats baseStats() {
        return baseStats;
    }
}
