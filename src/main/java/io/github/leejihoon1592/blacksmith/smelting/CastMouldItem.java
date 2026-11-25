package io.github.leejihoon1592.blacksmith.smelting;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.shapes.CollisionContext;

final class CastMouldItem extends Item {
    private final int mould;

    CastMouldItem(Properties properties, int mould) {
        super(properties);
        this.mould = mould;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var placement = new BlockPlaceContext(context);
        var level = context.getLevel();
        var pos = placement.getClickedPos();
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        var state = MouldStandBlock.placedMould(mould);
        if (!placement.canPlace() || !level.isInWorldBounds(pos)
                || !level.isUnobstructed(state, pos, player == null ? CollisionContext.empty() : CollisionContext.of(player))
                || (player != null && (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)))) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            if (!level.setBlockAndUpdate(pos, state)) return InteractionResult.FAIL;
            if (!(level.getBlockEntity(pos) instanceof MouldStandEntity stand)) {
                level.removeBlock(pos, false);
                return InteractionResult.FAIL;
            }
            stand.loadCast(stack, MouldStandBlock.capacity(state));
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
