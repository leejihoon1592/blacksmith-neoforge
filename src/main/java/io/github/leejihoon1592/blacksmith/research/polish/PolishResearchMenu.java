package io.github.leejihoon1592.blacksmith.research.polish;

import io.github.leejihoon1592.blacksmith.item.MetalIngotItem;
import io.github.leejihoon1592.blacksmith.item.ObservationItem;
import io.github.leejihoon1592.blacksmith.research.ResearchSupplies;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import io.github.leejihoon1592.blacksmith.research.ResearchBlockEntity;
import io.github.leejihoon1592.blacksmith.research.ResearchRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PolishResearchMenu extends AbstractContainerMenu {
    static final int MIN_FORCE = 0;
    static final int MAX_FORCE = 30;
    static final int START = 31;
    static final int APPLY = 32;
    static final int MAX_ATTEMPTS = 4;
    private static final int SAMPLE_SLOT = 7;
    private static final int PAPER_SLOT = 8;
    private static final int PEN_SLOT = 9;
    private static final int STATION_SLOTS = 3 + MAX_ATTEMPTS;
    private static final String ATTEMPTS_KEY = "PolishAttempts";
    private final Container station;
    private final DataSlot force = addDataSlot(DataSlot.standalone());

    private final DataSlot elapsed = addDataSlot(DataSlot.standalone());
    private final DataSlot timing = addDataSlot(DataSlot.standalone());
    private final Player viewer;
    private ItemStack sample = ItemStack.EMPTY;
    private long startTick;
    private int selectedForce;
    private final DataSlot cutDepth = addDataSlot(DataSlot.standalone());
    private final DataSlot observation = addDataSlot(DataSlot.standalone());

    public PolishResearchMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(ResearchBlockEntity.SIZE));
    }

    public PolishResearchMenu(int id, Inventory inventory, Container station) {
        super(ResearchRegistry.POLISH_MENU.get(), id);
        this.station = station;
        this.viewer = inventory.player;
        elapsed.set(-1);
        addSlot(new Slot(station, SAMPLE_SLOT, 32, 49) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof MetalIngotItem;
            }

            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(station, PAPER_SLOT, 57, 49) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.PAPER); }
        });
        addSlot(new Slot(station, PEN_SLOT, 82, 49) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ResearchRegistry.PEN.get()); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            addSlot(new Slot(station, i + 1, 222 + i * 22, 79) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 79 + column * 18, 154 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 79 + column * 18, 212));
        }
    }

    int force() { return force.get(); }

    int elapsed() { return elapsed.get(); }
    int timing() { return timing.get(); }
    boolean hasSample() { return station.getItem(SAMPLE_SLOT).getItem() instanceof MetalIngotItem; }
    int attempts() {
        return station.getItem(SAMPLE_SLOT).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(ATTEMPTS_KEY);
    }

    boolean canPolish() {
        return hasSample() && attempts() < MAX_ATTEMPTS && firstFreeOutput() >= 0
                && ResearchSupplies.available(station, PAPER_SLOT, PEN_SLOT);
    }

    private int firstFreeOutput() {
        for (int slot = 1; slot <= MAX_ATTEMPTS; slot++) {
            if (station.getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    int cutDepthHalfMillimeters() { return cutDepth.get(); }

    PolishingDepth.Observation observation() {
        int value = observation.get();
        return value == 0 ? null : PolishingDepth.Observation.values()[value - 1];
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        if (elapsed() < 0) {
            if (button >= MIN_FORCE && button <= MAX_FORCE) {
                force.set(button);
                return true;
            }
            if (button != START || !canPolish()) return false;
            sample = station.getItem(SAMPLE_SLOT);
            selectedForce = force();
            startTick = player.level().getGameTime();
            timing.set(0);
            observation.set(0);
            cutDepth.set(0);
            elapsed.set(0);
            return true;
        }
        if (button != APPLY || station.getItem(SAMPLE_SLOT) != sample) return false;
        return finish(player.level().getGameTime() - startTick);
    }

    @Override public void broadcastChanges() {
        if (!viewer.level().isClientSide && stillValid(viewer)) {
            if (station.getItem(SAMPLE_SLOT) != sample) {
                elapsed.set(-1);
                timing.set(0);
                observation.set(0);
                cutDepth.set(0);
            } else if (elapsed() >= 0) {
                long ticks = viewer.level().getGameTime() - startTick;
                if (ticks >= PolishingTiming.END_TICK) finish(ticks);
                else elapsed.set((int) ticks);
            }
        }
        super.broadcastChanges();
    }

    private boolean finish(long ticks) {
        elapsed.set(-1);
        if (!canPolish()) return false;
        int output = firstFreeOutput();
        if (!ResearchSupplies.consumePaper(station, PAPER_SLOT, PEN_SLOT)) return false;

        var grade = PolishingTiming.judge(ticks);
        var ingot = (MetalIngotItem) sample.getItem();
        int appliedForce = PolishingForce.apply(selectedForce, grade, viewer.getRandom().nextBoolean());
        var result = PolishingDepth.measure((int) Math.round(ingot.baseStats().polish() * 2), appliedForce);
        cutDepth.set((int) Math.round(result.centimeters() * 20));
        observation.set(result.observation().ordinal() + 1);
        timing.set(grade.ordinal() + 1);
        int attempt = attempts() + 1;
        CustomData.update(DataComponents.CUSTOM_DATA, sample, tag -> tag.putInt(ATTEMPTS_KEY, attempt));
        station.setItem(output, ObservationItem.createPolishNote(attempt, selectedForce, result.observation()));
        return true;
    }

    @Override public boolean stillValid(Player player) {
        return station.stillValid(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!(index < STATION_SLOTS ? moveItemStackTo(stack, STATION_SLOTS, slots.size(), true)
                : moveItemStackTo(stack, 0, STATION_SLOTS, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
