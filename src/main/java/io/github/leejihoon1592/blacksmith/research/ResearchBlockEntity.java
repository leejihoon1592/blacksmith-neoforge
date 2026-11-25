package io.github.leejihoon1592.blacksmith.research;

import io.github.leejihoon1592.blacksmith.item.ObservationItem;
import io.github.leejihoon1592.blacksmith.research.elasticity.ElasticityResearchMenu;
import io.github.leejihoon1592.blacksmith.research.polish.PolishResearchMenu;
import io.github.leejihoon1592.blacksmith.research.thermal.ResearchData;
import io.github.leejihoon1592.blacksmith.research.thermal.ResearchMenu;
import io.github.leejihoon1592.blacksmith.research.thermal.ThermalExperiment;
import io.github.leejihoon1592.blacksmith.research.strength.StrengthResearchMenu;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.*;

public final class ResearchBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SIZE = 13;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private ThermalExperiment experiment;
    private UUID experimentId = UUID.randomUUID();
    private final List<ItemStack> pending = new ArrayList<>();
    private double temperature = 20;
    private int burnTicks;
    private long lastPump = -100;
    private int pumpTicks;
    private int power;

    public ResearchBlockEntity(BlockPos pos, BlockState state) { super(ResearchRegistry.ENTITY.get(), pos, state); }
    ResearchBlock.Kind kind() { return ((ResearchBlock) getBlockState().getBlock()).kind; }

    public int thermalMenuMode() { return kind().menuMode; }

    void returnPrototypeInputs(Player player) {
        for (int slot = 7; slot <= 9; slot++) {
            ItemStack stack = removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                setChanged();
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
    }
    public boolean running() { return experiment != null; }
    public int temperature() { return (int)Math.round(temperature); }
    public int fuel() { return burnTicks; }
    public int pendingCount() { return pending.size(); }

    public void start() {
        if (kind() != ResearchBlock.Kind.FURNACE || running() || !pending.isEmpty()) return;
        var parts = ResearchData.sample(items.get(0));
        if (parts.isEmpty()) return;
        items.get(0).shrink(1);
        experiment = new ThermalExperiment(parts);
        experimentId = UUID.randomUUID();
        setChanged();
    }

    public boolean cleanFurnace() {
        if (kind() != ResearchBlock.Kind.FURNACE || running()) return false;
        temperature = 0;
        burnTicks = 0;
        lastPump = -100;
        pumpTicks = 0;
        power = 0;
        setChanged();
        return true;
    }

    public void pump() {
        if (kind() != ResearchBlock.Kind.FURNACE || level == null || level.getGameTime() == lastPump) return;
        lastPump = level.getGameTime();
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ResearchBlockEntity station) {
        station.tickServer();
    }
    private void tickServer() {
        boolean pumping = level.getGameTime() - lastPump <= 5;
        if (pumping) pumpTicks = Math.min(40, pumpTicks + 1); else pumpTicks = 0;
        power = pumping ? 1 + pumpTicks/10 : 0;
        if (pumping && burnTicks == 0) {
            ItemStack fuel = items.get(1);
            int burn = fuel.getBurnTime(RecipeType.SMELTING);
            if (burn > 0) {
                ItemStack remainder = fuel.getCraftingRemainingItem();
                fuel.shrink(1);
                if (fuel.isEmpty()) items.set(1, remainder);
                burnTicks = burn;
            }
        }
        if (burnTicks > 0) burnTicks--;
        double previousTemperature = temperature;
        temperature = Math.max(0, Math.min(2200, temperature - 0.2 + (pumping && burnTicks > 0 ? power*0.5 : 0)));
        if (experiment != null) {
            for (var observation : experiment.tick(temperature)) {
                // Recording requires supplies at the instant of observation. Missing observations stay missed.
                if (ResearchSupplies.consumePaper(this, 2, 3)) {
                    pending.add(ObservationItem.create(observation, experimentId));
                }
                if (!observation.onset()) {
                    var component = experiment.components().get(observation.component());
                    ItemStack output = new ItemStack(ResearchRegistry.EXTRACT.get(), observation.mixed() ? 1 : component.amount());
                    var data = new CompoundTag();
                    data.putString("Material", observation.mixed() ? "mixed" : component.id());
                    data.putBoolean("Mixed", observation.mixed());
                    data.putUUID("Experiment", experimentId);
                    output.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                    pending.add(output);
                }
            }
            if (experiment.complete()) experiment = null;
        }
        flushPending();
        if (running() || burnTicks > 0 || temperature != previousTemperature || !pending.isEmpty()) setChanged();
    }

    private void flushPending() {
        Iterator<ItemStack> iterator = pending.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            int start = stack.is(ResearchRegistry.NOTE.get()) ? 4 : 10;
            int end = start == 4 ? 10 : SIZE;
            for (int slot = start; slot < end; slot++) {
                if (items.get(slot).isEmpty()) { items.set(slot, stack); iterator.remove(); break; }
            }
        }
    }
    public void dropContents() {
        Containers.dropContents(level, worldPosition, this);
        for (ItemStack stack : pending) Containers.dropItemStack(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, stack);
        pending.clear();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putDouble("Temperature", temperature);
        tag.putInt("Fuel", burnTicks);
        tag.putUUID("ExperimentId", experimentId);
        ListTag queue = new ListTag();
        pending.forEach(stack -> queue.add(stack.save(registries)));
        tag.put("Pending", queue);
        if (experiment != null) {
            ListTag parts = new ListTag();
            for (int i = 0; i < experiment.components().size(); i++) {
                var c = experiment.components().get(i);
                var p = experiment.progress().get(i);
                CompoundTag part = new CompoundTag();
                part.putString("Id", c.id()); part.putDouble("Point", c.meltingPoint());
                part.putInt("Duration", c.durationTicks()); part.putInt("Amount", c.amount()); part.putString("Quantity", c.quantity());
                part.putInt("Active", p.activeTicks); part.putInt("Elapsed", p.elapsedTicks);
                part.putBoolean("Started", p.started); part.putBoolean("Done", p.done); part.putBoolean("Mixed", p.mixed);
                parts.add(part);
            }
            tag.put("Components", parts);
        }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        temperature = tag.contains("Temperature") ? Math.max(0, tag.getDouble("Temperature")) : 20;
        burnTicks = tag.getInt("Fuel");
        if (tag.hasUUID("ExperimentId")) experimentId = tag.getUUID("ExperimentId");
        pending.clear();
        tag.getList("Pending", Tag.TAG_COMPOUND).forEach(t -> ItemStack.parse(registries, t).ifPresent(pending::add));
        experiment = null;
        ListTag parts = tag.getList("Components", Tag.TAG_COMPOUND);
        if (!parts.isEmpty()) {
            List<ThermalExperiment.Component> components = new ArrayList<>();
            for (Tag element : parts) {
                CompoundTag p = (CompoundTag)element;
                components.add(new ThermalExperiment.Component(p.getString("Id"), p.getDouble("Point"), p.getInt("Duration"), p.getInt("Amount"), p.getString("Quantity")));
            }
            experiment = new ThermalExperiment(components);
            for (int i = 0; i < parts.size(); i++) {
                var source = parts.getCompound(i); var target = experiment.progress().get(i);
                target.activeTicks = source.getInt("Active"); target.elapsedTicks = source.getInt("Elapsed");
                target.started = source.getBoolean("Started"); target.done = source.getBoolean("Done"); target.mixed = source.getBoolean("Mixed");
            }
        }
    }
    @Override public Component getDisplayName() { return Component.translatable("block.blacksmith." + kind().blockId); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return switch (kind()) {
        case STRENGTH -> new StrengthResearchMenu(id, inventory, this);
        case POLISH -> new PolishResearchMenu(id, inventory, this);
        case ELASTICITY -> new ElasticityResearchMenu(id, inventory, this);
        case FURNACE, TABLE -> new ResearchMenu(id, inventory, this);
    };  }
    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack result = ContainerHelper.removeItem(items, slot, amount); setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5) <= 64; }
    @Override public void clearContent() { items.clear(); setChanged(); }
}
