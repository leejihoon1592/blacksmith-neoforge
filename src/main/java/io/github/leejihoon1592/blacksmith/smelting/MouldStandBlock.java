package io.github.leejihoon1592.blacksmith.smelting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class MouldStandBlock extends Block implements EntityBlock {
    private static final MapCodec<MouldStandBlock> CODEC = simpleCodec(MouldStandBlock::new);
    private static final IntegerProperty MOULD = IntegerProperty.create("mould", 0, 2);
    private static final BooleanProperty PORTABLE = BooleanProperty.create("portable");
    private static final BooleanProperty OPEN = BooleanProperty.create("open");
    private static final VoxelShape INGOT_CAST_SHAPE = Block.box(0.72, 0, 3.76, 15.28, 3.56, 12.24);

    MouldStandBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MOULD, 0).setValue(OPEN, false).setValue(PORTABLE, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MouldStandEntity(pos, state);
    }

    @Override
    protected MapCodec<MouldStandBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOULD, OPEN, PORTABLE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(MOULD) == 1 ? INGOT_CAST_SHAPE : Shapes.block();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        int mould = stack.is(SmeltingBlocks.INGOT_CAST.get()) ? 1 : stack.is(SmeltingBlocks.BLOCK_CAST.get()) ? 2 : 0;
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (mould == 0 || state.getValue(MOULD) != 0) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MouldStandEntity stand) {
            BlockState installed = state.setValue(MOULD, mould).setValue(OPEN, false);
            level.setBlockAndUpdate(pos, installed);
            stand.loadCast(stack, capacity(installed));
            stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        int mould = state.getValue(MOULD);
        if (mould == 0) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MouldStandEntity stand) {
            if (isPortable(state)) {
                if (player.isShiftKeyDown()) {
                    ItemStack detached = mouldStack(mould, stand);
                    level.setBlockAndUpdate(pos, state.setValue(MOULD, 0));
                    level.removeBlock(pos, false);
                    player.getInventory().placeItemBackInInventory(detached);
                } else if (stand.volume() > 0 && stand.coolingPercent() == 100) {
                    ItemStack result = solidifiedCast(mould == 2, stand.metal(), stand.volume());
                    if (!result.isEmpty()) {
                        stand.setContents("", 0);
                        player.getInventory().placeItemBackInInventory(result);
                    }
                } else if (stand.volume() > 0) {
                    player.displayClientMessage(Component.translatable("smelting.blacksmith.cooling", stand.coolingPercent()), true);
                }
            } else if (player.isShiftKeyDown()) {
                ItemStack detached = mouldStack(mould, stand);
                stand.setContents("", 0);
                level.setBlockAndUpdate(pos, state.setValue(MOULD, 0).setValue(OPEN, false));
                player.getInventory().placeItemBackInInventory(detached);
            } else if (state.getValue(OPEN)) {
                level.setBlockAndUpdate(pos, state.setValue(OPEN, false));
            } else if (stand.metal().isEmpty()) {
                SmeltingFurnaceEntity furnace = findAssembledFurnace(level, pos);
                if (furnace != null) {
                    var poured = furnace.pour(capacity(state), mould == 1);
                    if (poured != null) {
                        stand.setContents(poured.metal(), poured.volume());
                        level.setBlockAndUpdate(pos, state.setValue(OPEN, true));
                    } else {
                        player.displayClientMessage(Component.translatable("smelting.blacksmith.insufficient_metal"), true);
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static ItemStack solidifiedCast(boolean block, String metal, int volume) {
        if (metal.isBlank() || volume <= 0 || volume > (block ? MetalVolume.BLOCK : MetalVolume.INGOT)) {
            return ItemStack.EMPTY;
        }
        if (block && volume != MetalVolume.BLOCK) return ItemStack.EMPTY;
        if (volume < MetalVolume.INGOT) return SolidifiedMetalItem.create(metal, volume, true);
        ResourceLocation id = ResourceLocation.tryBuild(BlacksmithMod.MODID, metal + (block ? "_block" : "_ingot"));
        if (id == null) return ItemStack.EMPTY;
        Item result = BuiltInRegistries.ITEM.get(id);
        return result == Items.AIR ? ItemStack.EMPTY : new ItemStack(result);
    }

    private static SmeltingFurnaceEntity findAssembledFurnace(Level level, BlockPos stand) {
        for (Direction towardFurnace : Direction.Plane.HORIZONTAL) {
            BlockPos controller = stand.relative(towardFurnace).above();
            if (!level.hasChunkAt(controller)) continue;
            BlockState state = level.getBlockState(controller);
            if (state.is(SmeltingBlocks.SMELTING_FURNACE.get())
                    && SmeltingFurnaceBlock.mouldPosition(state, controller).equals(stand)
                    && SmeltingFurnaceBlock.structureProblem(state, level, controller) == null
                    && level.getBlockEntity(controller) instanceof SmeltingFurnaceEntity furnace) return furnace;
        }
        return null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        int mould = state.getValue(MOULD);
        if (!level.isClientSide && !moved && !state.is(next.getBlock()) && mould != 0) {
            var stand = level.getBlockEntity(pos) instanceof MouldStandEntity entity ? entity : null;
            var stack = stand == null ? new ItemStack(mouldItem(mould)) : mouldStack(mould, stand);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    static int capacity(BlockState state) {
        return switch (state.getValue(MOULD)) {
            case 1 -> MetalVolume.INGOT;
            case 2 -> MetalVolume.BLOCK;
            default -> 0;
        };
    }

    static boolean isPortable(BlockState state) {
        return state.getValue(PORTABLE);
    }

    static BlockState withVisibleContents(BlockState state, boolean filled) {
        return state.setValue(OPEN, filled);
    }

    static BlockState placedMould(int mould) {
        return SmeltingBlocks.INGOT_MOULD.get().defaultBlockState().setValue(PORTABLE, true).setValue(MOULD, mould);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!isPortable(state) || type != SmeltingBlocks.MOULD_STAND_ENTITY.get()) return null;
        return (world, pos, currentState, entity) -> ((MouldStandEntity) entity).cool();
    }

    private static ItemStack mouldStack(int mould, MouldStandEntity stand) {
        ItemStack stack = new ItemStack(mouldItem(mould));
        stand.saveCast(stack);
        return stack;
    }

    private static Item mouldItem(int mould) {
        return mould == 1 ? SmeltingBlocks.INGOT_CAST.get() : SmeltingBlocks.BLOCK_CAST.get();
    }
}
