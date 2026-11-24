package io.github.leejihoon1592.blacksmith.research;

import io.github.leejihoon1592.blacksmith.research.thermal.ResearchMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;

public final class ResearchBlock extends BaseEntityBlock {
    enum Kind {
        FURNACE(0, "research_furnace"),
        TABLE(1, "research_table"),
        STRENGTH(3, "strength_research_table"),
        ELASTICITY(4, "elasticity_research_table"),
        POLISH(5, "polish_research_table");

        final int menuMode;
        final String blockId;

        Kind(int menuMode, String blockId) {
            this.menuMode = menuMode;
            this.blockId = blockId;
        }
    }

    final Kind kind;

    ResearchBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }
    @Override protected MapCodec<ResearchBlock> codec() {
        return simpleCodec(properties -> new ResearchBlock(kind, properties));
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ResearchBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || kind != Kind.FURNACE ? null : createTickerHelper(type, ResearchRegistry.ENTITY.get(), ResearchBlockEntity::tick);
    }
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ResearchBlockEntity station) {
            if (kind == Kind.TABLE) station.returnPrototypeInputs(server);
            if (kind == Kind.STRENGTH || kind == Kind.ELASTICITY || kind == Kind.POLISH) server.openMenu(station);
            else server.openMenu(station, buf -> ResearchMenu.writeOpen(buf, kind.menuMode));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ResearchBlockEntity station) {
            if (!level.isClientSide) station.dropContents();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
