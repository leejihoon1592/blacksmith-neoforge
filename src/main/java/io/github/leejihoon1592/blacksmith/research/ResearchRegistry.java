package io.github.leejihoon1592.blacksmith.research;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ResearchRegistry {
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BlacksmithMod.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BlacksmithMod.MODID);
    public static final RegistryObject<ResearchBlock> FURNACE = BlacksmithMod.BLOCKS.register("research_furnace",
            // The research furnace has no vanilla LIT property.
            () -> new ResearchBlock(false, BlockBehaviour.Properties.copy(Blocks.FURNACE).lightLevel(state -> 0)));
    public static final RegistryObject<ResearchBlock> TABLE = BlacksmithMod.BLOCKS.register("research_table",
            () -> new ResearchBlock(true, BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)));
    public static final RegistryObject<Item> FURNACE_ITEM = BlacksmithMod.ITEMS.register("research_furnace", () -> new BlockItem(FURNACE.get(), new Item.Properties()));
    public static final RegistryObject<Item> TABLE_ITEM = BlacksmithMod.ITEMS.register("research_table", () -> new BlockItem(TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> PEN = BlacksmithMod.ITEMS.register("research_pen", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NOTE = BlacksmithMod.ITEMS.register("research_note", ObservationItem::new);
    public static final RegistryObject<Item> EXTRACT = BlacksmithMod.ITEMS.register("research_extract", () -> new Item(new Item.Properties()) {
        @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
                java.util.List<net.minecraft.network.chat.Component> lines, TooltipFlag flag) {
            lines.add(net.minecraft.network.chat.Component.translatable("research.blacksmith.unidentified_extract"));
        }
    });
    public static final RegistryObject<BlockEntityType<ResearchBlockEntity>> ENTITY = ENTITIES.register("research_station",
            () -> BlockEntityType.Builder.of(ResearchBlockEntity::new, FURNACE.get(), TABLE.get()).build(null));
    public static final RegistryObject<MenuType<ResearchMenu>> MENU = MENUS.register("research",
            () -> IForgeMenuType.create(ResearchMenu::new));
    public static void register(IEventBus bus) { ENTITIES.register(bus); MENUS.register(bus); ResearchNetwork.register(); }
    public static void creative(CreativeModeTab.Output output) {
        output.accept(FURNACE_ITEM.get()); output.accept(TABLE_ITEM.get()); output.accept(PEN.get());
    }
    private ResearchRegistry() {}
}
