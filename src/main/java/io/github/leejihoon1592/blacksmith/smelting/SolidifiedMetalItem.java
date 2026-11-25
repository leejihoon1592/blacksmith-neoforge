package io.github.leejihoon1592.blacksmith.smelting;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

final class SolidifiedMetalItem extends Item {
    SolidifiedMetalItem(Properties properties) {
        super(properties);
    }

    static ItemStack create(String metal, int volume, boolean identified) {
        ItemStack stack = new ItemStack(SmeltingBlocks.SOLIDIFIED_METAL.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("Metal", metal);
            tag.putInt("Volume", volume);
            tag.putBoolean("Identified", identified);
        });
        return stack;
    }

    static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Override
    public Component getName(ItemStack stack) {
        var data = data(stack);
        if (!data.getBoolean("Identified")) return super.getName(stack);
        return Component.translatable("smelting.blacksmith.solidified_metal",
                Component.translatable("metal.blacksmith." + data.getString("Metal")));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int volume = data(stack).getInt("Volume");
        if (volume > 0) {
            tooltip.add(Component.translatable("smelting.blacksmith.metal_volume", MetalVolume.format(volume)));
        }
    }
}
