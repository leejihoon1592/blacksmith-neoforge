package io.github.leejihoon1592.blacksmith.smelting;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class SmeltingBlocks {
    static final DeferredHolder<Block, Block> SMELTING_FURNACE = BlacksmithMod.BLOCKS.register("smelting_furnace", () -> new SmeltingFurnaceBlock(properties()));
    static final DeferredHolder<Block, Block> ORE_INLET = part("ore_inlet");
    static final DeferredHolder<Block, Block> TAPPING_SPOUT = part("tapping_spout");
    static final DeferredHolder<Block, Block> INGOT_MOULD = BlacksmithMod.BLOCKS.register("ingot_mould", () -> new MouldStandBlock(properties()));
    static final DeferredHolder<Block, Block> BELLOWS_INLET = part("bellows_inlet");
    static final DeferredHolder<Block, Block> BELLOWS = BlacksmithMod.BLOCKS.register("bellows", () -> new BellowsBlock(properties()));

    static final DeferredHolder<Item, Item> SOLIDIFIED_METAL =
            BlacksmithMod.ITEMS.register("solidified_metal", () -> new SolidifiedMetalItem(new Item.Properties()));

    static final DeferredHolder<Item, Item> INGOT_CAST =
            BlacksmithMod.ITEMS.register("ingot_cast", () -> new CastMouldItem(new Item.Properties(), 1));
    static final DeferredHolder<Item, Item> BLOCK_CAST =
            BlacksmithMod.ITEMS.register("block_cast", () -> new CastMouldItem(new Item.Properties(), 2));

    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BlacksmithMod.MODID);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmeltingFurnaceEntity>> FURNACE_ENTITY =
            ENTITIES.register("smelting_furnace",
                    () -> BlockEntityType.Builder.of(SmeltingFurnaceEntity::new, SMELTING_FURNACE.get()).build(null));

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MouldStandEntity>> MOULD_STAND_ENTITY =
            ENTITIES.register("mould_stand",
                    () -> BlockEntityType.Builder.of(MouldStandEntity::new, INGOT_MOULD.get()).build(null));

    static final DeferredHolder<Item, Item> WOODEN_MALLET =
            BlacksmithMod.ITEMS.register("wooden_mallet", () -> new Item(new Item.Properties().stacksTo(1)));
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BlacksmithMod.MODID);
    static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UnmouldRecipe>> UNMOULD_RECIPE =
            RECIPE_SERIALIZERS.register("unmould", () -> new SimpleCraftingRecipeSerializer<>(UnmouldRecipe::new));

    private SmeltingBlocks() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        item("smelting_furnace", SMELTING_FURNACE);
        item("ore_inlet", ORE_INLET);
        item("tapping_spout", TAPPING_SPOUT);
        item("ingot_mould", INGOT_MOULD);
        item("bellows_inlet", BELLOWS_INLET);
        item("bellows", BELLOWS);
    }

    private static DeferredHolder<Block, Block> part(String id) {
        return BlacksmithMod.BLOCKS.register(id, () -> new Block(properties()));
    }

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().strength(2.0F);
    }

    private static void item(String id, DeferredHolder<Block, Block> block) {
        BlacksmithMod.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
