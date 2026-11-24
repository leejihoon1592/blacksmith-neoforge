package io.github.leejihoon1592.blacksmith.research.polish;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import io.github.leejihoon1592.blacksmith.research.ResearchRegistry;
import io.github.leejihoon1592.blacksmith.research.ResearchStationScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BlacksmithMod.MODID, value = Dist.CLIENT)
public final class PolishResearchScreen extends ResearchStationScreen<PolishResearchMenu> {
    private static final int FORCE_STEPS = PolishResearchMenu.MAX_FORCE - PolishResearchMenu.MIN_FORCE;

    private AbstractSliderButton forceSlider;
    private Button action;

    public PolishResearchScreen(PolishResearchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(ResearchRegistry.POLISH_MENU.get(), PolishResearchScreen::new);
    }

    @Override protected void init() {
        super.init();
        forceSlider = addRenderableWidget(new AbstractSliderButton(leftPos + 120, topPos + 48, 188, 20,
                forceLabel(menu.force()), (double) menu.force() / FORCE_STEPS) {
            @Override protected void updateMessage() {
                setMessage(forceLabel(selectedForce()));
            }

            @Override protected void applyValue() {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selectedForce());
            }

            private int selectedForce() {
                return (int) Math.round(value * FORCE_STEPS);
            }
        });
        action = addRenderableWidget(Button.builder(tr("polish_start"), button -> {
            int id = menu.elapsed() < 0 ? PolishResearchMenu.START : PolishResearchMenu.APPLY;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(leftPos + 120, topPos + 77, 94, 20).build());
        updateControls();
    }

    @Override protected void containerTick() {
        super.containerTick();
        updateControls();
    }

    private void updateControls() {
        boolean running = menu.elapsed() >= 0;
        forceSlider.active = !running;
        action.active = menu.canPolish();
        action.setMessage(tr(running ? "polish_apply" : "polish_start"));
    }

    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        super.renderBg(graphics, partial, mouseX, mouseY);
        if (menu.hasSample()) renderCut(graphics);
        if (menu.elapsed() < 0) return;
        int x = leftPos + 120;
        int y = topPos + 108;
        int width = 188;
        graphics.fill(x, y, x + width, y + 8, 0xff594f3c);
        graphics.fill(x + (int) (width * (0.5 - PolishingTiming.BAD_RADIUS)), y,
                x + (int) (width * (0.5 + PolishingTiming.BAD_RADIUS)), y + 8, 0xffb89456);
        graphics.fill(x + (int) (width * (0.5 - PolishingTiming.GOOD_RADIUS)), y,
                x + (int) (width * (0.5 + PolishingTiming.GOOD_RADIUS)), y + 8, 0xff628342);
        int cursor = x + (int) Math.round(width * PolishingTiming.position(menu.elapsed() + partial));
        graphics.fill(cursor - 1, y - 2, cursor + 2, y + 10, 0xfff8eac8);
    }

    private void renderCut(GuiGraphics graphics) {
        int x = leftPos + 22;
        int y = topPos + 80;
        int targetDepth = 20;
        int depth = Math.min(menu.cutDepthHalfMillimeters(), 57);
        graphics.fill(x, y, x + 80, y + 59, 0xff6b5d4e);
        graphics.fill(x + 2, y + 2, x + 78, y + 57, 0xff827789);
        if (depth > 0) graphics.fill(x + 24, y, x + 54, y + depth, 0xffe5d7b4);
        for (int mark = 2; mark < 78; mark += 6) {
            graphics.fill(x + mark, y + targetDepth, x + mark + 3, y + targetDepth + 1, 0xfff9e3a1);
        }
        if (depth > 0) graphics.fill(x + 24, y + depth, x + 54, y + depth + 1, 0xff2e2531);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        text(graphics, title, 12, 12);
        text(graphics, tr("appraisal_sample"), 20, 33);
        text(graphics, tr("scratch_supplies"), 65, 33);
        text(graphics, Component.translatable("research.blacksmith.scratch_history",
                menu.attempts(), PolishResearchMenu.MAX_ATTEMPTS), 222, 68);
        if (menu.hasSample()) text(graphics, Component.literal("1 cm"), 66, 104);
        var observation = menu.observation();
        if (observation != null) {
            text(graphics, tr("polish_" + observation.name().toLowerCase(java.util.Locale.ROOT)), 120, 125);
        }
        if (menu.timing() > 0) {
            var grade = PolishingForce.Timing.values()[menu.timing() - 1];
            text(graphics, Component.literal(grade.name()), 222, 101);
        }
    }

    private static Component forceLabel(int halfPoints) {
        String amount = halfPoints % 2 == 0 ? Integer.toString(halfPoints / 2) : halfPoints / 2 + ".5";
        return Component.translatable("research.blacksmith.polish_force", amount);
    }
}
