package io.github.leejihoon1592.blacksmith.material;

import com.mojang.serialization.MapCodec;
import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MiningToolPredicate implements LootItemCondition {
    public static final DeferredRegister<LootItemConditionType> TYPES =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, BlacksmithMod.MODID);
    private static final DeferredHolder<LootItemConditionType, LootItemConditionType> IRON =
            TYPES.register("iron_mining_tool", () -> new LootItemConditionType(
                    MapCodec.unit(() -> new MiningToolPredicate(Blocks.DIAMOND_ORE, false))));
    private static final DeferredHolder<LootItemConditionType, LootItemConditionType> DIAMOND =
            TYPES.register("diamond_mining_tool", () -> new LootItemConditionType(
                    MapCodec.unit(() -> new MiningToolPredicate(Blocks.OBSIDIAN, true))));

    private final Block referenceOre;
    private final boolean diamond;

    private MiningToolPredicate(Block referenceOre, boolean diamond) {
        this.referenceOre = referenceOre;
        this.diamond = diamond;
    }

    @Override public LootItemConditionType getType() { return (diamond ? DIAMOND : IRON).get(); }

    @Override public Set<LootContextParam<?>> getReferencedContextParams() { return Set.of(LootContextParams.TOOL); }

    @Override public boolean test(LootContext context) {
        ItemStack stack = context.getParamOrNull(LootContextParams.TOOL);
        return stack != null && stack.canPerformAction(ItemAbilities.PICKAXE_DIG)
                && stack.isCorrectToolForDrops(referenceOre.defaultBlockState());
    }
}
