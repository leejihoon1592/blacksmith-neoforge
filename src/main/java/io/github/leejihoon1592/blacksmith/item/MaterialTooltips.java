package io.github.leejihoon1592.blacksmith.item;

import java.util.List;
import java.util.OptionalDouble;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = BlacksmithMod.MODID, value = Dist.CLIENT)
public final class MaterialTooltips {
    private MaterialTooltips() {}

    @SubscribeEvent
    public static void appendDetails(ItemTooltipEvent event) {
        if (!Screen.hasShiftDown()) return;

        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (stack.getItem() instanceof RawFragmentItem) {
            String host = RawFragmentItem.researchHost(stack);
            switch (host) {
                case "stone", "deepslate", "netherrack", "end_stone" ->
                        tooltip.add(Component.translatable("block.minecraft." + host).withStyle(ChatFormatting.GRAY));
                default -> { }
            }
        } else if (stack.getItem() instanceof MetalIngotItem) {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String path = id.getPath();
            if (!id.getNamespace().equals(BlacksmithMod.MODID) || !path.endsWith("_ingot")) return;
            String material = path.substring(0, path.length() - "_ingot".length());
            Inventory inventory = event.getEntity() == null ? null : event.getEntity().getInventory();
            OptionalDouble point = knownValue(inventory, material, "point");
            add(tooltip, "point", point, " °C");
            String range = point.isEmpty() ? "??" : roundUpTen(point.getAsDouble() * 0.7)
                    + "–" + roundUpTen(point.getAsDouble() * 0.9) + " °C";
            addLine(tooltip, "range", range);
            add(tooltip, "seconds", knownValue(inventory, material, "seconds"), " s");
            for (String property : List.of("durability", "hardness", "elasticity", "polish", "conductivity", "weight", "difficulty")) {
                add(tooltip, property, knownValue(inventory, material, property), "");
            }
        }
    }

    private static OptionalDouble knownValue(Inventory inventory, String material, String property) {
        if (inventory == null) return OptionalDouble.empty();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack book = inventory.getItem(i);
            if (!book.is(BlacksmithMod.MATERIAL_JOURNAL.get())) continue;
            var entry = book.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getCompound("ResearchEntries").getCompound(material);
            if (entry.contains(property) && entry.getInt(property + "Grade") == 2) {
                return OptionalDouble.of(entry.getDouble(property));
            }
        }
        return OptionalDouble.empty();
    }

    private static void add(List<Component> tooltip, String property, OptionalDouble value, String unit) {
        addLine(tooltip, property, value.isEmpty() ? "??" : Double.toString(value.getAsDouble()) + unit);
    }

    private static void addLine(List<Component> tooltip, String property, String value) {
        tooltip.add(Component.translatable("tooltip.blacksmith.ingot." + property, value).withStyle(ChatFormatting.GRAY));
    }

    private static int roundUpTen(double value) {
        return (int) Math.ceil(value / 10) * 10;
    }
}
