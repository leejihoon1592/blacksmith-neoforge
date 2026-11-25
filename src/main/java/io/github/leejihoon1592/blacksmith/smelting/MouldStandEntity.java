package io.github.leejihoon1592.blacksmith.smelting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

final class MouldStandEntity extends BlockEntity {
    private String metal = "";

    static final int COOLING_TICKS = 200;
    private int volume;
    private int coolingTicks;

    void cool() {
        updatePortableModel();
        if (volume <= 0 || coolingTicks >= COOLING_TICKS) return;
        coolingTicks++;
        if (level != null && !level.isClientSide) {
            setChanged();
            if (coolingTicks == COOLING_TICKS) syncContents();
        }
    }

    int coolingPercent() {
        return coolingTicks * 100 / COOLING_TICKS;
    }

    float coolingProgress(float partialTick) {
        return Math.clamp((coolingTicks + (MouldStandBlock.isPortable(getBlockState()) ? partialTick : 0))
                / COOLING_TICKS, 0, 1);
    }

    static CompoundTag castData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    void loadCast(ItemStack stack, int capacity) {
        CompoundTag data = castData(stack);
        String castMetal = data.getString("CastMetal");
        int castVolume = castMetal.isEmpty() ? 0
                : Math.clamp(data.contains("CastVolume") ? data.getInt("CastVolume") : capacity, 0, capacity);
        this.metal = castVolume == 0 ? "" : castMetal;
        this.volume = castVolume;
        this.coolingTicks = castVolume == 0 ? 0 : Math.clamp(data.getInt("CastCoolingTicks"), 0, COOLING_TICKS);
        contentsChanged();
    }

    void saveCast(ItemStack stack) {
        if (volume <= 0) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("CastMetal", metal);
            tag.putInt("CastVolume", volume);
            tag.putInt("CastCoolingTicks", coolingTicks);
        });
    }

    MouldStandEntity(BlockPos pos, BlockState state) {
        super(SmeltingBlocks.MOULD_STAND_ENTITY.get(), pos, state);
    }

    String metal() {
        return metal;
    }

    int volume() {
        return volume;
    }

    void setContents(String metal, int volume) {
        coolingTicks = 0;
        this.volume = volume;
        this.metal = metal;
        contentsChanged();
    }

    private void contentsChanged() {
        setChanged();
        updatePortableModel();
        syncContents();
    }

    private void syncContents() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private void updatePortableModel() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!MouldStandBlock.isPortable(state)) return;
        BlockState updated = MouldStandBlock.withVisibleContents(state, volume > 0);
        if (updated != state) level.setBlockAndUpdate(worldPosition, updated);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Volume", volume);
        tag.putInt("CoolingTicks", coolingTicks);
        tag.putString("Metal", metal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        metal = tag.getString("Metal");
        int capacity = MouldStandBlock.capacity(getBlockState());
        volume = metal.isEmpty() ? 0 : Math.clamp(tag.contains("Volume") ? tag.getInt("Volume") : capacity, 0, capacity);
        if (volume == 0) metal = "";
        coolingTicks = volume == 0 ? 0 : Math.clamp(tag.getInt("CoolingTicks"), 0, COOLING_TICKS);
    }
}
