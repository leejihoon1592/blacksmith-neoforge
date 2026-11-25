package io.github.leejihoon1592.blacksmith.smelting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.Map;

@EventBusSubscriber(modid = "blacksmith", value = Dist.CLIENT)
public final class MouldContentsRenderer implements BlockEntityRenderer<MouldStandEntity> {
    private static final ResourceLocation SURFACE = ResourceLocation.fromNamespaceAndPath(
            "blacksmith", "textures/block/cast_metal_surface.png");
    private static final int YELLOW = 0xFFE469;
    private static final int ORANGE = 0xFF8A24;
    private static final int RED = 0xE63226;
    private static final float SURFACE_HEIGHT = 0.187F;
    // Median opaque pixel colors from the corresponding ingot textures.
    private static final Map<String, Integer> INGOT_COLORS = Map.ofEntries(
            Map.entry("accutyrone", 0x704B60),
            Map.entry("biklis", 0x618F2C),
            Map.entry("brackstal", 0x585863),
            Map.entry("charvin", 0x615962),
            Map.entry("clinsor", 0x777A41),
            Map.entry("conductite", 0x962539),
            Map.entry("criminaltalloi", 0x2A3491),
            Map.entry("duracilium", 0x5E3685),
            Map.entry("durathorn", 0x426479),
            Map.entry("edgastium", 0x8C2F4E),
            Map.entry("enduri", 0x1A59A1),
            Map.entry("flexarite", 0x318A34),
            Map.entry("ginspa", 0x819328),
            Map.entry("granteron", 0x5D308B),
            Map.entry("liselborn", 0x407B6B),
            Map.entry("lumesor", 0x5B5368),
            Map.entry("sprinal", 0x5A6259),
            Map.entry("stevalite", 0x9C1F4B),
            Map.entry("tenaloct", 0x5DA516),
            Map.entry("voltarite", 0x5B5368)
    );

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SmeltingBlocks.MOULD_STAND_ENTITY.get(), context -> new MouldContentsRenderer());
    }

    @Override
    public void render(MouldStandEntity mould, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        if (mould.volume() <= 0 || MouldStandBlock.capacity(mould.getBlockState()) != MetalVolume.INGOT) return;

        float progress = mould.coolingProgress(partialTick);
        int finalColor = INGOT_COLORS.getOrDefault(mould.metal(), 0x858585);
        int color = 0xFF000000 | coolingColor(progress, finalColor);
        float glow = 1 - progress;
        int surfaceLight = LightTexture.pack(interpolate(LightTexture.block(light), 15, glow),
                interpolate(LightTexture.sky(light), 15, glow));
        VertexConsumer vertices = buffers.getBuffer(RenderType.entitySolid(SURFACE));
        PoseStack.Pose pose = poses.last();
        vertex(vertices, pose, 0.2234F, 0.3900F, 0, 0, color, surfaceLight, overlay);
        vertex(vertices, pose, 0.2234F, 0.6100F, 0, 1, color, surfaceLight, overlay);
        vertex(vertices, pose, 0.7766F, 0.6100F, 1, 1, color, surfaceLight, overlay);
        vertex(vertices, pose, 0.7766F, 0.3900F, 1, 0, color, surfaceLight, overlay);
    }

    private static int coolingColor(float progress, int finalColor) {
        float segment = progress * 3;
        if (segment < 1) return blend(YELLOW, ORANGE, segment);
        if (segment < 2) return blend(ORANGE, RED, segment - 1);
        return blend(RED, finalColor, segment - 2);
    }

    private static int blend(int start, int end, float amount) {
        int red = interpolate(start >> 16 & 255, end >> 16 & 255, amount);
        int green = interpolate(start >> 8 & 255, end >> 8 & 255, amount);
        int blue = interpolate(start & 255, end & 255, amount);
        return red << 16 | green << 8 | blue;
    }

    private static int interpolate(int start, int end, float amount) {
        return Math.round(start + (end - start) * amount);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float z,
                               float u, float v, int color, int light, int overlay) {
        vertices.addVertex(pose, x, SURFACE_HEIGHT, z).setColor(color).setUv(u, v)
                .setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
    }
}
