package io.github.leejihoon1592.blacksmith.smelting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

final class UnmouldRecipe extends CustomRecipe {
    UnmouldRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !result(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result(input);
    }

    private static ItemStack result(CraftingInput input) {
        ItemStack cast = ItemStack.EMPTY;
        boolean hammer = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(SmeltingBlocks.WOODEN_MALLET.get()) && !hammer) {
                hammer = true;
            } else if (stack.is(SmeltingBlocks.INGOT_CAST.get()) && cast.isEmpty()) {
                cast = stack;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (!hammer || cast.isEmpty()) return ItemStack.EMPTY;
        var data = MouldStandEntity.castData(cast);
        if (data.getInt("CastCoolingTicks") < MouldStandEntity.COOLING_TICKS) return ItemStack.EMPTY;
        return MouldStandBlock.solidifiedCast(false, data.getString("CastMetal"),
                data.getInt("CastVolume"));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remainder = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(SmeltingBlocks.WOODEN_MALLET.get())) remainder.set(i, stack.copyWithCount(1));
            if (stack.is(SmeltingBlocks.INGOT_CAST.get())) remainder.set(i, new ItemStack(stack.getItem()));
        }
        return remainder;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SmeltingBlocks.UNMOULD_RECIPE.get();
    }
}
