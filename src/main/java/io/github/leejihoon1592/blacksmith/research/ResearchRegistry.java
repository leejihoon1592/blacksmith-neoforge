package io.github.leejihoon1592.blacksmith.research;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ResearchRegistry {
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BlacksmithMod.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BlacksmithMod.MODID);
    public static final DeferredHolder<Block, ResearchBlock> FURNACE = BlacksmithMod.BLOCKS.register("research_furnace",
            // The research furnace has no vanilla LIT property.
            () -> new ResearchBlock(ResearchBlock.Kind.FURNACE, BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).lightLevel(state -> 0)));
    public static final DeferredHolder<Block, ResearchBlock> TABLE = BlacksmithMod.BLOCKS.register("research_table",
            () -> new ResearchBlock(ResearchBlock.Kind.TABLE, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)));
    public static final DeferredHolder<Block, ResearchBlock> STRENGTH_TABLE = BlacksmithMod.BLOCKS.register(
            "strength_research_table",
            () -> new ResearchBlock(ResearchBlock.Kind.STRENGTH, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)));
    public static final DeferredHolder<Item, Item> STRENGTH_TABLE_ITEM = BlacksmithMod.ITEMS.register(
            "strength_research_table", () -> new BlockItem(STRENGTH_TABLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> FURNACE_ITEM = BlacksmithMod.ITEMS.register("research_furnace", () -> new BlockItem(FURNACE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> TABLE_ITEM = BlacksmithMod.ITEMS.register("research_table", () -> new BlockItem(TABLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> PEN = BlacksmithMod.ITEMS.register("research_pen", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> NOTE = BlacksmithMod.ITEMS.register("research_note", ObservationItem::new);
    public static final DeferredHolder<Item, Item> EXTRACT = BlacksmithMod.ITEMS.register("research_extract", () -> new Item(new Item.Properties()) {
        @Override public void appendHoverText(ItemStack stack, TooltipContext context,
                java.util.List<net.minecraft.network.chat.Component> lines, TooltipFlag flag) {
            lines.add(net.minecraft.network.chat.Component.translatable("research.blacksmith.unidentified_extract"));
        }
    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchBlockEntity>> ENTITY = ENTITIES.register("research_station",
            () -> BlockEntityType.Builder.of(ResearchBlockEntity::new, FURNACE.get(), TABLE.get(), STRENGTH_TABLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ResearchMenu>> MENU = MENUS.register("research",
            () -> IMenuTypeExtension.create(ResearchMenu::new));
    public static void register(IEventBus bus) { ENTITIES.register(bus); MENUS.register(bus); bus.addListener(ResearchNetwork::register); }
    public static void creative(CreativeModeTab.Output output) {
        output.accept(FURNACE_ITEM.get()); output.accept(TABLE_ITEM.get()); output.accept(STRENGTH_TABLE_ITEM.get()); output.accept(PEN.get());
    }
    private ResearchRegistry() {}
}
