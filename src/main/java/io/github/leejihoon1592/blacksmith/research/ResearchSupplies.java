package io.github.leejihoon1592.blacksmith.research;

import net.minecraft.world.Container;
import net.minecraft.world.item.Items;

final class ResearchSupplies {
    private ResearchSupplies() {}

    static boolean available(Container contents, int paperSlot, int penSlot) {
        return contents.getItem(paperSlot).is(Items.PAPER)
                && contents.getItem(penSlot).is(ResearchRegistry.PEN.get());
    }

    static boolean consumePaper(Container contents, int paperSlot, int penSlot) {
        if (!available(contents, paperSlot, penSlot)) return false;
        contents.removeItem(paperSlot, 1);
        return true;
    }
}
