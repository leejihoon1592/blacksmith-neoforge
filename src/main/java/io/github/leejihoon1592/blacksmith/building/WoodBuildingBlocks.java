package io.github.leejihoon1592.blacksmith.building;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import io.github.leejihoon1592.blacksmith.building.CornerWoodBlock.Form;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

public final class WoodBuildingBlocks {
    private WoodBuildingBlocks() {}

    public static void register() {
        List<DeferredHolder<Item, Item>> items = new ArrayList<>();
        addWood(items, "oak", Blocks.OAK_PLANKS);
        addWood(items, "spruce", Blocks.SPRUCE_PLANKS);
        addWood(items, "birch", Blocks.BIRCH_PLANKS);
        addWood(items, "jungle", Blocks.JUNGLE_PLANKS);
        addWood(items, "acacia", Blocks.ACACIA_PLANKS);
        addWood(items, "dark_oak", Blocks.DARK_OAK_PLANKS);
        addWood(items, "mangrove", Blocks.MANGROVE_PLANKS);
        addWood(items, "cherry", Blocks.CHERRY_PLANKS);
        addWood(items, "bamboo", Blocks.BAMBOO_PLANKS);
        addWood(items, "crimson", Blocks.CRIMSON_PLANKS);
        addWood(items, "warped", Blocks.WARPED_PLANKS);
        addMaterial(items, "andesite", Blocks.ANDESITE);
        addMaterial(items, "bamboo_mosaic", Blocks.BAMBOO_MOSAIC);
        addMaterial(items, "blackstone", Blocks.BLACKSTONE);
        addMaterial(items, "bricks", Blocks.BRICKS);
        addMaterial(items, "cobbled_deepslate", Blocks.COBBLED_DEEPSLATE);
        addMaterial(items, "cobblestone", Blocks.COBBLESTONE);
        addMaterial(items, "cut_copper", Blocks.CUT_COPPER);
        addMaterial(items, "dark_prismarine", Blocks.DARK_PRISMARINE);
        addMaterial(items, "deepslate_bricks", Blocks.DEEPSLATE_BRICKS);
        addMaterial(items, "deepslate_tiles", Blocks.DEEPSLATE_TILES);
        addMaterial(items, "diorite", Blocks.DIORITE);
        addMaterial(items, "end_stone_bricks", Blocks.END_STONE_BRICKS);
        addMaterial(items, "exposed_cut_copper", Blocks.EXPOSED_CUT_COPPER);
        addMaterial(items, "granite", Blocks.GRANITE);
        addMaterial(items, "mossy_cobblestone", Blocks.MOSSY_COBBLESTONE);
        addMaterial(items, "mossy_stone_bricks", Blocks.MOSSY_STONE_BRICKS);
        addMaterial(items, "mud_bricks", Blocks.MUD_BRICKS);
        addMaterial(items, "nether_bricks", Blocks.NETHER_BRICKS);
        addMaterial(items, "oxidized_cut_copper", Blocks.OXIDIZED_CUT_COPPER);
        addMaterial(items, "polished_andesite", Blocks.POLISHED_ANDESITE);
        addMaterial(items, "polished_blackstone_bricks", Blocks.POLISHED_BLACKSTONE_BRICKS);
        addMaterial(items, "polished_blackstone", Blocks.POLISHED_BLACKSTONE);
        addMaterial(items, "polished_deepslate", Blocks.POLISHED_DEEPSLATE);
        addMaterial(items, "polished_diorite", Blocks.POLISHED_DIORITE);
        addMaterial(items, "polished_granite", Blocks.POLISHED_GRANITE);
        addMaterial(items, "polished_tuff", Blocks.POLISHED_TUFF);
        addMaterial(items, "prismarine_bricks", Blocks.PRISMARINE_BRICKS);
        addMaterial(items, "prismarine", Blocks.PRISMARINE);
        addMaterial(items, "purpur_block", Blocks.PURPUR_BLOCK);
        addMaterial(items, "quartz_block", Blocks.QUARTZ_BLOCK);
        addMaterial(items, "red_nether_bricks", Blocks.RED_NETHER_BRICKS);
        addMaterial(items, "red_sandstone", Blocks.RED_SANDSTONE);
        addMaterial(items, "sandstone", Blocks.SANDSTONE);
        addMaterial(items, "smooth_quartz", Blocks.SMOOTH_QUARTZ);
        addMaterial(items, "smooth_red_sandstone", Blocks.SMOOTH_RED_SANDSTONE);
        addMaterial(items, "smooth_sandstone", Blocks.SMOOTH_SANDSTONE);
        addMaterial(items, "stone_bricks", Blocks.STONE_BRICKS);
        addMaterial(items, "stone", Blocks.STONE);
        addMaterial(items, "tuff_bricks", Blocks.TUFF_BRICKS);
        addMaterial(items, "tuff", Blocks.TUFF);
        addMaterial(items, "waxed_cut_copper", Blocks.WAXED_CUT_COPPER);
        addMaterial(items, "waxed_exposed_cut_copper", Blocks.WAXED_EXPOSED_CUT_COPPER);
        addMaterial(items, "waxed_oxidized_cut_copper", Blocks.WAXED_OXIDIZED_CUT_COPPER);
        addMaterial(items, "waxed_weathered_cut_copper", Blocks.WAXED_WEATHERED_CUT_COPPER);
        addMaterial(items, "weathered_cut_copper", Blocks.WEATHERED_CUT_COPPER);
        BlacksmithMod.CREATIVE_MODE_TABS.register("building", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.blacksmith.building"))
                .icon(() -> new ItemStack(items.getFirst().get()))
                .displayItems((parameters, output) -> items.forEach(item -> output.accept(item.get())))
                .build());
    }

    private static void addWood(List<DeferredHolder<Item, Item>> items, String name, Block planks) {
        items.add(registerBlock(name + "_quarter_planks", planks, Form.QUARTER));
        items.add(registerBlock(name + "_vertical_stairs", planks, Form.VERTICAL_STAIR));
        items.add(registerMaterial(name + "_vertical_slab", planks, Form.VERTICAL_SLAB));
    }

    private static void addMaterial(List<DeferredHolder<Item, Item>> items, String id, Block base) {
        items.add(registerMaterial(id + "_quarter", base, Form.QUARTER));
        items.add(registerMaterial(id + "_vertical_stairs", base, Form.VERTICAL_STAIR));
        items.add(registerMaterial(id + "_vertical_slab", base, Form.VERTICAL_SLAB));
    }

    private static DeferredHolder<Item, Item> registerMaterial(String id, Block base, Form form) {
        String name = switch (form) {
            case QUARTER -> "block.blacksmith.building.material_quarter";
            case VERTICAL_STAIR -> "block.blacksmith.building.material_stairs";
            case VERTICAL_SLAB -> "block.blacksmith.building.material_vertical_slab";
        };
        var block = BlacksmithMod.BLOCKS.register(id,
                () -> new CornerWoodBlock(BlockBehaviour.Properties.ofFullCopy(base).noOcclusion(), form));
        return BlacksmithMod.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()) {
            @Override
            public Component getName(ItemStack stack) {
                return Component.translatable(name, base.getName());
            }
        });
    }

    private static DeferredHolder<Item, Item> registerBlock(String id, Block planks, Form form) {
        var block = BlacksmithMod.BLOCKS.register(id,
                () -> new CornerWoodBlock(BlockBehaviour.Properties.ofFullCopy(planks).noOcclusion(), form));
        return BlacksmithMod.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
