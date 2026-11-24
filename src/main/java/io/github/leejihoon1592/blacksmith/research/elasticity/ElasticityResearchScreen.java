package io.github.leejihoon1592.blacksmith.research.elasticity;

import io.github.leejihoon1592.blacksmith.research.ResearchRegistry;
import io.github.leejihoon1592.blacksmith.research.ResearchStationScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "blacksmith", value = Dist.CLIENT)
public final class ElasticityResearchScreen extends ResearchStationScreen<ElasticityResearchMenu> {
    private Button action;
    private ElasticityTrajectory trajectory;
    private int displayedResult;
    private int animationTick;

    public ElasticityResearchScreen(ElasticityResearchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(ResearchRegistry.ELASTICITY_MENU.get(), ElasticityResearchScreen::new);
    }

    @Override protected void init() {
        super.init();
        action = addRenderableWidget(Button.builder(tr("elasticity_start"), button -> {
            int id = menu.elapsed() < 0 ? ElasticityResearchMenu.START : ElasticityResearchMenu.DROP;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(leftPos + 16, topPos + 103, 94, 20).build());
        updateButton();
    }

    @Override protected void containerTick() {
        super.containerTick();
        if (menu.result() != displayedResult) {
            displayedResult = menu.result();
            trajectory = displayedResult == 0 ? null : ElasticityResearchMenu.trajectory(displayedResult);
            animationTick = 0;
        } else if (trajectory != null && animationTick < ElasticityTrajectory.END_TICK) {
            animationTick++;
        }
        updateButton();
    }

    private void updateButton() {
        boolean timing = menu.elapsed() >= 0;
        action.setMessage(tr(timing ? "elasticity_drop" : "elasticity_start"));
        action.active = menu.canMeasure() && (timing || trajectory == null
                || animationTick >= ElasticityTrajectory.END_TICK);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        text(graphics, title, 12, 12);
        text(graphics, tr("appraisal_sample"), 27, 28);
        text(graphics, tr("scratch_supplies"), 70, 28);
        text(graphics, Component.translatable("research.blacksmith.scratch_history",
                menu.attempts(), ElasticityResearchMenu.MAX_ATTEMPTS), 32, 70);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        super.renderBg(graphics, partial, mouseX, mouseY);
        int originX = leftPos + 218;
        int floorY = topPos + 120;
        graphics.fill(leftPos + 121, topPos + 24, leftPos + 328, floorY + 2, 0xfff6ebd0);
        graphics.fill(originX - 12, floorY, originX + 12, floorY + 3, 0xff584938);
        for (int halfPoint = 0; halfPoint <= 32; halfPoint++) {
            int y = floorY - halfPoint * 3;
            boolean major = halfPoint % 4 == 0;
            graphics.fill(originX + 44, y, originX + (major ? 53 : 49), y + 1, 0xff584938);
            if (major) graphics.drawString(font, Integer.toString(halfPoint / 2), originX + 57, y - 4, 0xff392d20, false);
        }
        if (menu.hasSample()) {
            var point = trajectory == null ? new ElasticityTrajectory.Position(0.0, 16.0)
                    : trajectory.positionAt(animationTick + partial);
            int x = originX + (int) Math.round(point.x() * 6);
            int y = floorY - 3 - (int) Math.round(point.height() * 6);
            graphics.fill(x - 2, y - 3, x + 3, y + 3, 0xff51495b);
            graphics.fill(x - 1, y - 2, x + 1, y, 0xffc5c1d0);
        }
        if (menu.elapsed() >= 0) {
            int barX = leftPos + 121;
            int y = topPos + 133;
            graphics.fill(barX, y, barX + 181, y + 7, 0xff594f3c);
            int target = barX + ElasticityTiming.TARGET_TICK * 5;
            graphics.fill(target - 10, y, target + 11, y + 7, 0xff628342);
            int cursor = barX + menu.elapsed() * 5;
            graphics.fill(cursor, y - 1, cursor + 2, y + 8, 0xfff8eac8);
        }
    }
}
