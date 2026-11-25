package io.github.leejihoon1592.blacksmith.smelting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

final class BellowsBlock extends HorizontalDirectionalBlock {
    BellowsBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected com.mojang.serialization.MapCodec<BellowsBlock> codec() {
        return simpleCodec(BellowsBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        for (Direction direction : new Direction[]{facing, facing.getClockWise(),
                facing.getCounterClockWise(), facing.getOpposite()}) {
            if (context.getLevel().getBlockState(context.getClickedPos().relative(direction))
                    .is(SmeltingBlocks.BELLOWS_INLET.get())) {
                return defaultBlockState().setValue(FACING, direction);
            }
        }
        return defaultBlockState().setValue(FACING, facing);
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
            for (Direction towardFurnace : Direction.Plane.HORIZONTAL) {
                BlockPos controller = pos.relative(towardFurnace, 2).above();
                if (!level.hasChunkAt(controller)
                        || !(level.getBlockEntity(controller) instanceof SmeltingFurnaceEntity furnace)) continue;
                BlockState furnaceState = furnace.getBlockState();
                if (SmeltingFurnaceBlock.bellowsPosition(furnaceState, controller).equals(pos)
                        && SmeltingFurnaceBlock.structureProblem(furnaceState, level, controller) == null) {
                    furnace.pumpBellows();
                    break;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
