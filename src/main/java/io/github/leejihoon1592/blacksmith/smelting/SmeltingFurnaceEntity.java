package io.github.leejihoon1592.blacksmith.smelting;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import io.github.leejihoon1592.blacksmith.research.thermal.ResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

final class SmeltingFurnaceEntity extends BlockEntity {
    private ItemStack ore = ItemStack.EMPTY;
    private ItemStack fuel = ItemStack.EMPTY;

    private static final int HEAT_PER_PUMP = 20;
    private static final int HEAT_PER_TICK = 10;
    private static final int TICKS_PER_DEGREE_OF_COOLING = 5;
    private int temperature;
    private int burnTicks;
    private SmeltingFuel burningFuel = SmeltingFuel.restore("");
    private int meltTicks;
    private String moltenMetal = "";
    private int moltenVolume;

    static void tick(Level level, BlockPos pos, BlockState state, SmeltingFurnaceEntity furnace) {
        int previousTemperature = furnace.temperature;
        int previousBurn = furnace.burnTicks;
        int previousProgress = furnace.meltTicks;
        var material = material(furnace.ore);
        boolean assembled = SmeltingFurnaceBlock.structureProblem(state, level, pos) == null;
        int inputVolume = inputVolume(furnace.ore);
        boolean canMelt = material != null && inputVolume > 0 && furnace.canStore(material.id(), inputVolume);
        int targetTicks = canMelt ? duration(material) : 0;
        if (assembled && furnace.burnTicks == 0 && furnace.meltTicks < targetTicks) {
            int burn = SmeltingFuel.burnTime(furnace.fuel);
            if (burn > 0) {
                furnace.burningFuel = SmeltingFuel.of(furnace.fuel);
                furnace.burnTicks = burn;
                furnace.fuel = furnace.fuel.getCraftingRemainingItem();
            }
        }

        boolean burning = furnace.burnTicks > 0;
        if (burning) {
            furnace.burnTicks--;
        }
        int targetTemperature = assembled && burning ? furnace.burningFuel.baseLimit() : 0;
        if (furnace.temperature < targetTemperature) {
            furnace.temperature = Math.min(targetTemperature, furnace.temperature + HEAT_PER_TICK);
        } else if (furnace.temperature > targetTemperature
                && level.getGameTime() % TICKS_PER_DEGREE_OF_COOLING == 0) {
            furnace.temperature = furnace.temperature - 1;
        }
        if (assembled && canMelt && furnace.temperature >= material.point()
                && furnace.meltTicks < targetTicks) {
            furnace.meltTicks++;
        }
        if (assembled && canMelt && furnace.meltTicks >= targetTicks) {
            furnace.moltenMetal = material.id();
            furnace.moltenVolume += inputVolume;
            furnace.ore = ItemStack.EMPTY;
            furnace.meltTicks = 0;
            furnace.setChanged();
        }
        if (previousTemperature != furnace.temperature || previousBurn != furnace.burnTicks
                || previousProgress != furnace.meltTicks) {
            furnace.setChanged();
        }
    }

    private boolean canStore(String metal, int volume) {
        return moltenVolume <= MetalVolume.BLOCK - volume && (moltenVolume == 0 || moltenMetal.equals(metal));
    }

    record Pour(String metal, int volume) {}

    Pour pour(int capacity, boolean allowPartial) {
        if (moltenVolume == 0 || (!allowPartial && moltenVolume < capacity)) return null;
        int volume = Math.min(moltenVolume, capacity);
        Pour poured = new Pour(moltenMetal, volume);
        moltenVolume -= volume;
        if (moltenVolume == 0) moltenMetal = "";
        setChanged();
        return poured;
    }

    void pumpBellows() {
        if (burnTicks <= 0 || temperature >= burningFuel.bellowsLimit()) return;
        temperature = Math.min(burningFuel.bellowsLimit(), temperature + HEAT_PER_PUMP);
        setChanged();
    }

    int progressPercent() {
        var material = material(ore);
        return material == null ? 0 : Math.min(100, (int) (100L * meltTicks / duration(material)));
    }

    int temperature() { return temperature; }
    int burnTicks() { return burnTicks; }
    int moltenVolume() { return moltenVolume; }
    ItemStack input(int slot) { return slot == 0 ? ore : fuel; }

    void setInput(int slot, ItemStack stack) {
        if (slot == 0) {
            if (!ItemStack.matches(ore, stack)) meltTicks = 0;
            ore = stack;
        } else {
            fuel = stack;
        }
        setChanged();
    }

    static boolean acceptsInput(int slot, ItemStack stack) {
        return slot == 0 ? isSmeltingInput(stack) && material(stack) != null
                : SmeltingFuel.burnTime(stack) > 0;
    }

    boolean canView(Player player) {
        return level != null && !isRemoved() && level == player.level()
                && player.distanceToSqr(worldPosition.getCenter()) <= 64
                && SmeltingFurnaceBlock.structureProblem(getBlockState(), level, worldPosition) == null;
    }

    static boolean isRawOre(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals(BlacksmithMod.MODID) && id.getPath().startsWith("raw_")
                && !id.getPath().endsWith("_block");
    }

    static boolean isSmeltingInput(ItemStack stack) {
        return isRawOre(stack) || (stack.is(SmeltingBlocks.SOLIDIFIED_METAL.get()) && inputVolume(stack) > 0);
    }

    private static int inputVolume(ItemStack stack) {
        if (!stack.is(SmeltingBlocks.SOLIDIFIED_METAL.get())) return MetalVolume.INGOT;
        int volume = SolidifiedMetalItem.data(stack).getInt("Volume");
        return volume > 0 && volume < MetalVolume.INGOT ? volume : 0;
    }

    private static ResearchData.Material material(ItemStack ore) {
        if (ore.is(SmeltingBlocks.SOLIDIFIED_METAL.get())) {
            return ResearchData.material(SolidifiedMetalItem.data(ore).getString("Metal"));
        }
        if (!isRawOre(ore)) return null;
        return ResearchData.material(BuiltInRegistries.ITEM.getKey(ore.getItem()).getPath().substring("raw_".length()));
    }

    private static int duration(ResearchData.Material material) {
        return Math.max(1, (int) Math.round(material.seconds() * 20));
    }


    SmeltingFurnaceEntity(BlockPos pos, BlockState state) {
        super(SmeltingBlocks.FURNACE_ENTITY.get(), pos, state);
    }

    boolean insert(Player player, ItemStack stack, boolean asFuel) {
        int slot = asFuel ? 1 : 0;
        if (!input(slot).isEmpty() || !acceptsInput(slot, stack)) return false;
        setInput(slot, stack.copyWithCount(1));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return true;
    }

    void returnInputs(Player player) {
        ItemStack previousOre = ore;
        ItemStack previousFuel = fuel;
        ore = ItemStack.EMPTY;
        fuel = ItemStack.EMPTY;
        meltTicks = 0;
        setChanged();
        if (!previousOre.isEmpty()) player.getInventory().placeItemBackInInventory(previousOre);
        if (!previousFuel.isEmpty()) player.getInventory().placeItemBackInInventory(previousFuel);
    }

    void dropInputs() {
        if (level == null) return;
        if (!ore.isEmpty()) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), ore);
        if (!fuel.isEmpty()) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), fuel);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Temperature", temperature);
        tag.putInt("BurnTicks", burnTicks);
        tag.putString("BurningFuel", burningFuel.savedName());
        tag.putInt("MeltTicks", meltTicks);
        tag.putString("MoltenMetal", moltenMetal);
        tag.putInt("MoltenHalfMillibuckets", moltenVolume);
        if (!ore.isEmpty()) tag.put("Ore", ore.save(registries));
        if (!fuel.isEmpty()) tag.put("Fuel", fuel.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = Math.max(0, tag.getInt("Temperature"));
        burnTicks = Math.max(0, tag.getInt("BurnTicks"));
        burningFuel = SmeltingFuel.restore(tag.getString("BurningFuel"));
        meltTicks = Math.max(0, tag.getInt("MeltTicks"));
        moltenMetal = tag.getString("MoltenMetal");
        int storedAmount;
        if (tag.contains("MoltenHalfMillibuckets")) {
            storedAmount = tag.getInt("MoltenHalfMillibuckets");
        } else if (tag.contains("MoltenMillibucketsV2")) {
            storedAmount = MetalVolume.fromWholeMillibuckets(tag.getInt("MoltenMillibucketsV2"));
        } else {
            int oldIngotCount = tag.contains("MoltenMillibuckets")
                    ? tag.getInt("MoltenMillibuckets") / 100
                    : tag.getInt("MoltenUnits");
            storedAmount = MetalVolume.fromLegacyIngots(oldIngotCount);
        }
        moltenVolume = moltenMetal.isEmpty() ? 0 : Math.clamp(storedAmount, 0, MetalVolume.BLOCK);
        ore = ItemStack.parseOptional(registries, tag.getCompound("Ore"));
        fuel = ItemStack.parseOptional(registries, tag.getCompound("Fuel"));
    }
}
