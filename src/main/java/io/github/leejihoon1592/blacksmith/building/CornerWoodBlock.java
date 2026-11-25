package io.github.leejihoon1592.blacksmith.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class CornerWoodBlock extends Block {
    private static final IntegerProperty CORNERS = IntegerProperty.create("corners", 1, 15);
    private static final int ALL_CORNERS = 15;
    private static final VoxelShape[] SHAPES = buildShapes();
    enum Form {
        QUARTER(1), VERTICAL_STAIR(14), VERTICAL_SLAB(3);

        final int defaultCorners;

        Form(int defaultCorners) {
            this.defaultCorners = defaultCorners;
        }
    }

    private final Form form;

    CornerWoodBlock(Properties properties, Form form) {
        super(properties);
        this.form = form;
        registerDefaultState(stateDefinition.any().setValue(CORNERS, form.defaultCorners));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CORNERS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (form != Form.QUARTER) {
            return defaultBlockState().setValue(CORNERS, verticalCorners(context));
        }

        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        int corners = selectedCorner(context);
        if (existing.is(this)) corners |= existing.getValue(CORNERS);
        return defaultBlockState().setValue(CORNERS, corners);
    }

    private int verticalCorners(BlockPlaceContext context) {
        var player = context.getPlayer();
        if (player == null) return form.defaultCorners;

        BlockPos pos = context.getClickedPos();
        boolean sneaking = player.isShiftKeyDown();
        if (sneaking && context.getClickedFace() == Direction.UP) {
            BlockState below = context.getLevel().getBlockState(pos.below());
            if (below.getBlock() instanceof CornerWoodBlock block && block.form == form) {
                return below.getValue(CORNERS);
            }
        }

        if (form == Form.VERTICAL_SLAB) {
            Direction side = context.getHorizontalDirection();
            if (!sneaking) side = side.getOpposite();
            return switch (side) {
                case NORTH -> 3;
                case EAST -> 10;
                case SOUTH -> 12;
                case WEST -> 5;
                default -> throw new IllegalStateException("Expected a horizontal placement direction");
            };
        }

        Vec3 center = pos.getCenter();
        int nearX = player.getX() < center.x ? 0 : 1;
        int nearZ = player.getZ() < center.z ? 0 : 1;
        int nearCorner = nearX + 2 * nearZ;
        int emptyCorner = sneaking ? nearCorner : nearCorner ^ 3;
        return ALL_CORNERS ^ (1 << emptyCorner);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return form == Form.QUARTER && context.getItemInHand().is(asItem())
                && (state.getValue(CORNERS) & selectedCorner(context)) == 0;
    }

    private static int selectedCorner(BlockPlaceContext context) {
        Vec3 hit = context.getClickLocation();
        BlockPos pos = context.getClickedPos();
        int x = hit.x - pos.getX() + context.getClickedFace().getStepX() * 0.0001 < 0.5 ? 0 : 1;
        int z = hit.z - pos.getZ() + context.getClickedFace().getStepZ() * 0.0001 < 0.5 ? 0 : 1;
        return 1 << (x + 2 * z);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(CORNERS)];
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case NONE -> state;
            case CLOCKWISE_90 -> remap(state, 1, 3, 0, 2);
            case CLOCKWISE_180 -> remap(state, 3, 2, 1, 0);
            case COUNTERCLOCKWISE_90 -> remap(state, 2, 0, 3, 1);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case NONE -> state;
            case LEFT_RIGHT -> remap(state, 2, 3, 0, 1);
            case FRONT_BACK -> remap(state, 1, 0, 3, 2);
        };
    }

    private static BlockState remap(BlockState state, int... target) {
        int old = state.getValue(CORNERS);
        int result = 0;
        for (int i = 0; i < target.length; i++) result |= ((old >> i) & 1) << target[i];
        return state.setValue(CORNERS, result);
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[ALL_CORNERS + 1];
        shapes[0] = Shapes.empty();
        for (int mask = 1; mask <= ALL_CORNERS; mask++) {
            int bit = Integer.numberOfTrailingZeros(mask);
            int x = (bit & 1) * 8;
            int z = (bit >> 1) * 8;
            shapes[mask] = Shapes.or(shapes[mask & (mask - 1)], Block.box(x, 0, z, x + 8, 16, z + 8));
        }
        return shapes;
    }
}
