package io.github.leejihoon1592.blacksmith.smelting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public final class SmeltingFurnaceBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final MapCodec<SmeltingFurnaceBlock> CODEC = simpleCodec(SmeltingFurnaceBlock::new);

    SmeltingFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<SmeltingFurnaceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof SmeltingFurnaceEntity furnace) {
                furnace.returnInputs(player);
            } else {
                Component problem = structureProblem(state, level, pos);
                player.displayClientMessage(problem == null
                        ? Component.translatable("smelting.blacksmith.assembled")
                                .append(" ").append(level.getBlockEntity(pos) instanceof SmeltingFurnaceEntity furnace
                                        ? furnace.status() : Component.empty())
                        : problem, true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmeltingFurnaceEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        boolean ore = SmeltingFurnaceEntity.isSmeltingInput(stack);
        boolean fuel = SmeltingFuel.burnTime(stack) > 0;
        if (!ore && !fuel) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            Component problem = structureProblem(state, level, pos);
            if (problem != null) {
                player.displayClientMessage(problem, true);
            } else if (level.getBlockEntity(pos) instanceof SmeltingFurnaceEntity furnace) {
                boolean inserted = furnace.insert(player, stack, !ore);
                player.displayClientMessage(Component.translatable(inserted
                        ? "smelting.blacksmith.input_added" : "smelting.blacksmith.input_full"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!level.isClientSide && !state.is(next.getBlock())
                && level.getBlockEntity(pos) instanceof SmeltingFurnaceEntity furnace) {
            furnace.dropInputs();
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != SmeltingBlocks.FURNACE_ENTITY.get()) return null;
        return (world, pos, currentState, entity) ->
                SmeltingFurnaceEntity.tick(world, pos, currentState, (SmeltingFurnaceEntity) entity);
    }

    static BlockPos bellowsPosition(BlockState state, BlockPos pos) {
        return pos.below().relative(state.getValue(FACING).getCounterClockWise(), 2);
    }

    static BlockPos mouldPosition(BlockState state, BlockPos pos) {
        return pos.below().relative(state.getValue(FACING));
    }

    static Component structureProblem(BlockState state, Level level, BlockPos pos) {
        Direction front = state.getValue(FACING);
        Direction right = front.getCounterClockWise();
        BlockPos base = pos.below();
        BlockPos[] positions = {pos.above(), base, mouldPosition(state, pos), base.relative(right),
                bellowsPosition(state, pos)};
        Block[] required = {SmeltingBlocks.ORE_INLET.get(), SmeltingBlocks.TAPPING_SPOUT.get(),
                SmeltingBlocks.INGOT_MOULD.get(), SmeltingBlocks.BELLOWS_INLET.get(), SmeltingBlocks.BELLOWS.get()};
        for (int i = 0; i < positions.length; i++) {
            BlockPos part = positions[i];
            if (!level.hasChunkAt(part) || !level.getBlockState(part).is(required[i])
                    || (required[i] == SmeltingBlocks.INGOT_MOULD.get() && MouldStandBlock.isPortable(level.getBlockState(part)))) {
                return Component.translatable("smelting.blacksmith.missing_part", required[i].getName(),
                        part.getX(), part.getY(), part.getZ());
            }
        }
        BlockPos handle = base.relative(right, 3);
        if (!level.hasChunkAt(handle) || !level.isEmptyBlock(handle)) {
            return Component.translatable("smelting.blacksmith.clear_handle", handle.getX(), handle.getY(), handle.getZ());
        }
        return null;
    }
}
