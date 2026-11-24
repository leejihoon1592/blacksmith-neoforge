package io.github.leejihoon1592.blacksmith.research.elasticity;

import java.util.OptionalInt;
import java.util.Random;

import io.github.leejihoon1592.blacksmith.item.MetalIngotItem;
import io.github.leejihoon1592.blacksmith.item.ObservationItem;
import io.github.leejihoon1592.blacksmith.research.ResearchRegistry;
import io.github.leejihoon1592.blacksmith.research.ResearchSupplies;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ElasticityResearchMenu extends AbstractContainerMenu {
    static final int START = 0;
    static final int DROP = 1;
    static final int MAX_ATTEMPTS = 3;
    private static final int SAMPLE = 7;
    private static final int PAPER = 8;
    private static final int PEN = 9;
    private static final int FIRST_OUTPUT = 1;
    private static final int STATION_SLOTS = 3 + MAX_ATTEMPTS;
    private static final String ATTEMPTS_KEY = "ElasticityAttempts";
    private final Container contents;
    private final Player viewer;
    private final DataSlot elapsed = addDataSlot(DataSlot.standalone());
    private final DataSlot result = addDataSlot(DataSlot.standalone());
    private ItemStack sample = ItemStack.EMPTY;
    private long startedAt;

    public ElasticityResearchMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(13));
    }

    public ElasticityResearchMenu(int id, Inventory inventory, Container contents) {
        super(ResearchRegistry.ELASTICITY_MENU.get(), id);
        this.contents = contents;
        this.viewer = inventory.player;
        elapsed.set(-1);
        addSlot(new Slot(contents, SAMPLE, 32, 49) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof MetalIngotItem;
            }

            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(contents, PAPER, 54, 49) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(net.minecraft.world.item.Items.PAPER); }
        });
        addSlot(new Slot(contents, PEN, 76, 49) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ResearchRegistry.PEN.get()); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            addSlot(new Slot(contents, FIRST_OUTPUT + i, 32 + 22 * i, 81) {
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

    boolean hasSample() { return contents.getItem(SAMPLE).getItem() instanceof MetalIngotItem; }
    int attempts() {
        return contents.getItem(SAMPLE).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(ATTEMPTS_KEY);
    }

    boolean canMeasure() {
        return hasSample() && attempts() < MAX_ATTEMPTS && firstFreeOutput() >= 0
                && ResearchSupplies.available(contents, PAPER, PEN);
    }

    private int firstFreeOutput() {
        for (int slot = FIRST_OUTPUT; slot < FIRST_OUTPUT + MAX_ATTEMPTS; slot++) {
            if (contents.getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    int elapsed() { return elapsed.get(); }
    int result() { return result.get(); }

    static ElasticityTrajectory trajectory(int packed) {
        var timing = ElasticityMeasurement.Timing.values()[(packed & 3) - 1];
        var measurement = timing == ElasticityMeasurement.Timing.MISS
                ? OptionalInt.empty() : OptionalInt.of(((packed >>> 2) & 63) - 2);
        return ElasticityTrajectory.create(timing, measurement, new Random(packed >>> 8));
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        ItemStack current = contents.getItem(SAMPLE);
        if (!(current.getItem() instanceof MetalIngotItem ingot)) return false;
        long now = player.level().getGameTime();
        if (button == START && elapsed() < 0 && canMeasure()) {
            sample = current;
            startedAt = now;
            result.set(0);
            elapsed.set(0);
            return true;
        }
        if (button != DROP || elapsed() < 0 || sample != current) return false;
        return finish(player, ingot, now - startedAt);
    }

    @Override public boolean stillValid(Player player) {
        return contents.stillValid(player);
    }

    @Override public void broadcastChanges() {
        if (!viewer.level().isClientSide && stillValid(viewer)) {
            ItemStack current = contents.getItem(SAMPLE);
            if (current != sample) {
                elapsed.set(-1);
                result.set(0);
            } else if (elapsed() >= 0 && current.getItem() instanceof MetalIngotItem ingot) {
                long ticks = viewer.level().getGameTime() - startedAt;
                if (ticks > ElasticityTiming.LAST_CLICK_TICK) finish(viewer, ingot, ticks);
                else elapsed.set((int) ticks);
            }
        }
        super.broadcastChanges();
    }

    private boolean finish(Player player, MetalIngotItem ingot, long ticks) {
        elapsed.set(-1);
        if (!canMeasure()) return false;
        int output = firstFreeOutput();
        if (!ResearchSupplies.consumePaper(contents, PAPER, PEN)) return false;
        var timing = ElasticityTiming.judge(ticks);
        var random = player.getRandom();
        var measured = ElasticityMeasurement.measure((int) Math.round(ingot.baseStats().elasticity() * 2),
                timing, random.nextBoolean());
        // Current ingot stats and the ±1 error fit in six bits, biased by two.
        int packed = (random.nextInt(128) << 8) | ((measured.orElse(0) + 2) << 2) | (timing.ordinal() + 1);
        int attempt = attempts() + 1;
        CustomData.update(DataComponents.CUSTOM_DATA, sample, tag -> tag.putInt(ATTEMPTS_KEY, attempt));
        contents.setItem(output, ObservationItem.createElasticityNote(attempt, measured));
        result.set(packed);
        return true;
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
