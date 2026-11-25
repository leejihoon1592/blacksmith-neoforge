package io.github.leejihoon1592.blacksmith.smelting;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "blacksmith", value = Dist.CLIENT)
public final class SmeltingScreen extends AbstractContainerScreen<SmeltingScreenMenu> {
    private static final ResourceLocation PANEL = texture("panel");
    private static final ResourceLocation COLD = texture("furnace_cold");
    private static final ResourceLocation HOT = texture("furnace_hot");
    private static final ResourceLocation TEMPERATURE = texture("temperature");
    private static final ResourceLocation POINTER = texture("pointer");
    private static final int LABEL_COLOR = 0xfff4dbb4;

    public SmeltingScreen(SmeltingScreenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 250;
        imageHeight = 252;
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(SmeltingBlocks.FURNACE_MENU.get(), SmeltingScreen::new);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(PANEL, x, y, 0, 0, 250, 252, 250, 252);
        graphics.blit(menu.burning() ? HOT : COLD, x + 54, y + 8, 0, 0, 104, 145, 104, 145);
        graphics.blit(TEMPERATURE, x + 36, y + 15, 0, 0, 8, 103, 8, 103);
        int pointerY = 112 - Math.clamp(menu.temperature(), 0, 2500) * 96 / 2500;
        graphics.blit(POINTER, x + 26, y + pointerY, 0, 0, 11, 9, 11, 9);

        graphics.fill(x + 166, y + 50, x + 241, y + 143, 0xff31241d);
        drawGauge(graphics, x + 170, y + 76, menu.progress(), 100, 0xffdc944a);
        drawGauge(graphics, x + 170, y + 120, menu.molten(), MetalVolume.BLOCK, 0xffe86b38);
    }

    private static void drawGauge(GuiGraphics graphics, int x, int y, int value, int maximum, int color) {
        graphics.fill(x, y, x + 66, y + 6, 0xff1c1613);
        int width = Math.clamp(value, 0, maximum) * 64 / maximum;
        graphics.fill(x + 1, y + 1, x + 1 + width, y + 5, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, LABEL_COLOR, true);
        graphics.drawString(font, menu.temperature() + " °C", 8, 145, LABEL_COLOR, true);
        graphics.drawString(font, Component.translatable("smelting.blacksmith.gui.progress", menu.progress()),
                170, 62, LABEL_COLOR, true);
        graphics.drawString(font, Component.translatable("smelting.blacksmith.gui.molten"),
                170, 93, LABEL_COLOR, true);
        graphics.drawString(font, MetalVolume.format(menu.molten()) + "/" + MetalVolume.format(MetalVolume.BLOCK) + " mB",
                170, 106, LABEL_COLOR, true);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("blacksmith", "textures/gui/smelting/" + name + ".png");
    }
}
