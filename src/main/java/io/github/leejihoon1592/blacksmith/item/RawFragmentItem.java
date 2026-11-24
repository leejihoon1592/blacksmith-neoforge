package io.github.leejihoon1592.blacksmith.item;


import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class RawFragmentItem extends Item {
    public RawFragmentItem(Properties properties) {
        super(properties);
    }

    public static String researchHost(ItemStack stack) {
        String host = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString("ResearchHost");
        return host.isEmpty() ? "stone" : host;
    }
}
