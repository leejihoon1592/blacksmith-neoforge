package io.github.leejihoon1592.blacksmith.material;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;

import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

final class MetalForms {
    static List<DeferredHolder<? extends Item, ? extends Item>> register(String metal) {
        return List.of(
                item("raw_" + metal),
                item(metal + "_nugget"),
                item(metal + "_raw_fragment"),
                block(metal + "_block", Blocks.DIAMOND_BLOCK),
                block("raw_" + metal + "_block", Blocks.RAW_IRON_BLOCK),
                block(metal + "_ore", Blocks.IRON_ORE),
                block("deepslate_" + metal + "_ore", Blocks.DEEPSLATE_IRON_ORE),
                block("nether_" + metal + "_ore", Blocks.NETHER_GOLD_ORE),
                block("end_" + metal + "_ore", Blocks.END_STONE)
        );
    }

    private static DeferredHolder<Item, Item> item(String id) {
        return BlacksmithMod.ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    private static DeferredHolder<Item, BlockItem> block(String id, Block template) {
        var block = BlacksmithMod.BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.ofFullCopy(template)));
        return BlacksmithMod.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private MetalForms() {}
}
