package io.github.leejihoon1592.blacksmith.research.strength;

import io.github.leejihoon1592.blacksmith.item.MetalIngotItem;
import io.github.leejihoon1592.blacksmith.item.ObservationItem;
import io.github.leejihoon1592.blacksmith.research.ResearchRegistry;
import io.github.leejihoon1592.blacksmith.research.ResearchSupplies;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StrengthResearchMenu extends AbstractContainerMenu {
    private final Container contents;
    private final boolean serverSide;
    private final int machineSlots;

    static final int FIRST_SCRATCH_BUTTON = 2;
    private static final int APPRAISAL_SLOT = 7;
    private static final int PAPER_SLOT = 8;
    private static final int PEN_SLOT = 9;
    private final DataSlot[] scratchObservations = new DataSlot[StrengthCandidates.TURNS];
    private final DataSlot candidateStart = DataSlot.standalone();
    private ItemStack scratchSample = ItemStack.EMPTY;

    public StrengthResearchMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(13), false);
    }

    public StrengthResearchMenu(int id, Inventory inventory, Container contents) {
        this(id, inventory, contents, true);
    }

    private StrengthResearchMenu(int id, Inventory inventory, Container contents, boolean serverSide) {
        super(ResearchRegistry.STRENGTH_MENU.get(), id);
        this.contents = contents;
        this.serverSide = serverSide;
        for (int i = 0; i < StrengthCandidates.TURNS; i++) {
            addSlot(new Slot(contents, i + 1, 20 + i * 19, 82) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        for (int i = 0; i < scratchObservations.length; i++) {
            scratchObservations[i] = addDataSlot(DataSlot.standalone());
        }
        addDataSlot(candidateStart);
        addSlot(new Slot(contents, PAPER_SLOT, 49, 42) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.PAPER); }
        });
        addSlot(new Slot(contents, PEN_SLOT, 69, 42) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ResearchRegistry.PEN.get()); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(contents, APPRAISAL_SLOT, 96, 42) {
            @Override public void setChanged() {
                super.setChanged();
                if (serverSide) updateScratchSample();
            }

            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof MetalIngotItem;
            }

            @Override public int getMaxStackSize() {
                return 1;
            }
        });
        if (serverSide) updateScratchSample();
        machineSlots = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 79 + column * 18, 154 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 79 + column * 18, 212));
        }
    }

    private void updateScratchSample() {
        for (DataSlot observation : scratchObservations) observation.set(0);
        scratchSample = contents.getItem(APPRAISAL_SLOT);
        ItemStack sample = scratchSample;
        if (sample.getItem() instanceof MetalIngotItem ingot) {
            candidateStart.set(StrengthCandidates.forTurn(halfPointStrength(ingot), 1).getFirst());
        } else {
            candidateStart.set(0);
        }
    }

    private static int halfPointStrength(MetalIngotItem ingot) {
        return (int) Math.round(ingot.baseStats().hardness() * 2);
    }

    public int firstScratchCandidate() { return candidateStart.get(); }

    public boolean hasScratchSupplies() {
        return ResearchSupplies.available(contents, PAPER_SLOT, PEN_SLOT);
    }

    public int scratchCount() {
        int count = 0;
        while (count < scratchObservations.length && scratchObservations[count].get() != 0) count++;
        return count;
    }

    public ScratchObservation scratchObservation(int index) {
        int packed = scratchObservations[index].get() - 1;
        int start = packed >>> 6;
        int choice = (packed >>> 2) & 15;
        return new ScratchObservation(start, start + choice, ScratchResult.values()[packed & 3]);
    }

    public record ScratchObservation(int start, int selected, ScratchResult result) {}

    @Override public void broadcastChanges() {
        if (serverSide && scratchSample != contents.getItem(APPRAISAL_SLOT)) {
            updateScratchSample();
        }
        super.broadcastChanges();
    }

    private boolean scratch(Player player, int button) {
        if (scratchSample != contents.getItem(APPRAISAL_SLOT)) {
            updateScratchSample();
            return false;
        }
        int turn = scratchCount();
        int choice = button - FIRST_SCRATCH_BUTTON - turn * StrengthCandidates.COUNT;
        if (!serverSide || !stillValid(player) || turn >= StrengthCandidates.TURNS
                || choice < 0 || choice >= StrengthCandidates.COUNT) return false;
        if (!(scratchSample.getItem() instanceof MetalIngotItem ingot)) return false;

        int strength = halfPointStrength(ingot);
        var candidates = StrengthCandidates.forTurn(strength, turn + 1);
        int selected = candidates.get(choice);
        ScratchResult result = ScratchResult.compare(strength, selected);
        int outputSlot = 1;
        while (outputSlot <= StrengthCandidates.TURNS && !contents.getItem(outputSlot).isEmpty()) outputSlot++;
        if (outputSlot > StrengthCandidates.TURNS || !ResearchSupplies.consumePaper(contents, PAPER_SLOT, PEN_SLOT)) return false;

        contents.setItem(outputSlot, ObservationItem.createScratchNote(selected, result));
        player.sendSystemMessage(ObservationItem.scratchLine(selected, result));
        // Store candidate start, selected offset, and result in one synced value. Zero means unobserved.
        int packed = (candidates.getFirst() << 6) | (choice << 2) | result.ordinal();
        scratchObservations[turn].set(packed + 1);
        candidateStart.set(turn + 1 < StrengthCandidates.TURNS
                ? StrengthCandidates.forTurn(strength, turn + 2).getFirst() : 0);
        return true;
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        return serverSide && scratch(player, button);
    }

    @Override public boolean stillValid(Player player) {
        return contents.stillValid(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
