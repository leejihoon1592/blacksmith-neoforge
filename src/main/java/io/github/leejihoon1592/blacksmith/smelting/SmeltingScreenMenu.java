package io.github.leejihoon1592.blacksmith.smelting;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class SmeltingScreenMenu extends AbstractContainerMenu {
    private final SmeltingFurnaceEntity furnace;
    private final DataSlot temperature = addDataSlot(DataSlot.standalone());
    private final DataSlot progress = addDataSlot(DataSlot.standalone());
    private final DataSlot molten = addDataSlot(DataSlot.standalone());
    private final DataSlot burning = addDataSlot(DataSlot.standalone());

    SmeltingScreenMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    SmeltingScreenMenu(int id, Inventory inventory, SmeltingFurnaceEntity furnace) {
        super(SmeltingBlocks.FURNACE_MENU.get(), id);
        this.furnace = furnace;
        var inputs = furnace == null ? new SimpleContainer(2) : new SimpleContainer(2) {
            @Override
            public ItemStack getItem(int slot) { return furnace.input(slot); }

            @Override
            public void setItem(int slot, ItemStack stack) { furnace.setInput(slot, stack); }

            @Override
            public ItemStack removeItem(int slot, int count) {
                ItemStack previous = getItem(slot);
                if (count <= 0 || previous.isEmpty()) return ItemStack.EMPTY;
                int removed = Math.min(count, previous.getCount());
                setItem(slot, previous.copyWithCount(previous.getCount() - removed));
                return previous.copyWithCount(removed);
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                return removeItem(slot, getItem(slot).getCount());
            }
        };
        addSlot(new InputSlot(inputs, 0, 170, 18));
        addSlot(new InputSlot(inputs, 1, 206, 18));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 44 + column * 18, 171 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 44 + column * 18, 230));
        }
        refresh();
    }

    private void refresh() {
        if (furnace == null) return;
        temperature.set(furnace.temperature());
        progress.set(furnace.progressPercent());
        molten.set(furnace.moltenVolume());
        burning.set(furnace.burnTicks() > 0 ? 1 : 0);
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return furnace == null || furnace.canView(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem().copy();
        ItemStack stack = index < 2 ? original.copy() : slot.getItem();
        boolean moved = index < 2 ? moveItemStackTo(stack, 2, slots.size(), true)
                : moveItemStackTo(stack, 0, 2, false);
        if (!moved) return ItemStack.EMPTY;
        if (index < 2 || stack.isEmpty()) slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        else slot.setChanged();
        return original;
    }

    int temperature() { return temperature.get(); }
    int progress() { return progress.get(); }
    int molten() { return molten.get(); }
    boolean burning() { return burning.get() != 0; }

    private static final class InputSlot extends Slot {
        private InputSlot(SimpleContainer inputs, int index, int x, int y) {
            super(inputs, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return SmeltingFurnaceEntity.acceptsInput(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() { return 1; }
    }
}
