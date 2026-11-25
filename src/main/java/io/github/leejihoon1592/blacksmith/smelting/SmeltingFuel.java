package io.github.leejihoon1592.blacksmith.smelting;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

record SmeltingFuel(int baseLimit, int bellowsLimit) {
    private static final SmeltingFuel STANDARD = new SmeltingFuel(900, 2000);
    private static final SmeltingFuel COKE = new SmeltingFuel(1400, 2500);
    private static final TagKey<Item> COKES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(BlacksmithMod.MODID, "coke_fuels"));

    static SmeltingFuel of(ItemStack stack) {
        return stack.is(COKES) ? COKE : STANDARD;
    }

    static int burnTime(ItemStack stack) {
        int vanillaTime = stack.getBurnTime(RecipeType.SMELTING);
        return vanillaTime > 0 ? vanillaTime : (stack.is(COKES) ? 1600 : 0);
    }

    static SmeltingFuel restore(String profile) {
        return "coke".equals(profile) ? COKE : STANDARD;
    }

    String savedName() {
        return this == COKE ? "coke" : "standard";
    }
}
