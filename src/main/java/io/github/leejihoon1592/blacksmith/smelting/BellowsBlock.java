package io.github.leejihoon1592.blacksmith.smelting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

final class BellowsBlock extends Block {
    BellowsBlock(Properties properties) {
        super(properties);
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
