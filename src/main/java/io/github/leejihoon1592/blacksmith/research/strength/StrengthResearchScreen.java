package io.github.leejihoon1592.blacksmith.research.strength;

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
public final class StrengthResearchScreen extends ResearchStationScreen<StrengthResearchMenu> {
    private int displayedScratchTurn = -1;
    private final Button[] scratchButtons = new Button[StrengthCandidates.COUNT];

    public StrengthResearchScreen(StrengthResearchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @SubscribeEvent public static void setup(RegisterMenuScreensEvent event) {
        event.register(ResearchRegistry.STRENGTH_MENU.get(), StrengthResearchScreen::new);
    }

    @Override protected void init() {
        super.init();
        for (int i = 0; i < scratchButtons.length; i++) {
            int choice = i;
            scratchButtons[i] = addRenderableWidget(Button.builder(Component.empty(),
                    b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                            StrengthResearchMenu.FIRST_SCRATCH_BUTTON + menu.scratchCount() * StrengthCandidates.COUNT + choice))
                    .bounds(leftPos + 116 + 22 * i, topPos + 136, 22, 16).build());
        }
        updateScratchButtons();
    }

    @Override protected void containerTick() {
        super.containerTick();
        updateScratchButtons();
    }

    private void updateScratchButtons() {
        int start = menu.firstScratchCandidate();
        int turn = menu.scratchCount();
        if (displayedScratchTurn != turn) {
            for (Button button : scratchButtons) {
                if (getFocused() == button) setFocused(null);
                button.setFocused(false);
            }
            displayedScratchTurn = turn;
        }
        for (int i = 0; i < scratchButtons.length; i++) {
            Button button = scratchButtons[i];
            int value = start + i;
            button.visible = start > 0;
            button.active = button.visible && menu.hasScratchSupplies();
            button.setMessage(Component.literal(formatStrength(value)));
        }
    }
    private static String formatStrength(int value) {
        return value % 2 == 0 ? Integer.toString(value / 2) : value / 2 + ".5";
    }

    private static Component scratchText(ScratchResult result) {
        return tr("scratch." + result.name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        text(g, title, 12, 12);
        text(g, tr("scratch_supplies"), 49, 29);
        text(g, tr("appraisal_sample"), 96, 29);
        int count = menu.scratchCount();
        if (count > 0) {
            text(g, scratchText(menu.scratchObservation(count - 1).result()), 150, 21);
            text(g, Component.translatable("research.blacksmith.scratch_history", count, StrengthCandidates.TURNS), 268, 21);
        }
        text(g, tr("notes"), 20, 69);
        text(g, tr("hover_notes"), 20, 109);
    }

    @Override protected void renderTooltip(GuiGraphics g,int x,int y) {
        if (menu.scratchCount() > 0
                && isHovering(268, 18, 68, 13, x, y)) {
            var lines = new java.util.ArrayList<Component>();
            for (int i = 0; i < menu.scratchCount(); i++) {
                var record = menu.scratchObservation(i);
                lines.add(Component.translatable("research.blacksmith.scratch_record", i + 1,
                        formatStrength(record.start()), formatStrength(record.start() + StrengthCandidates.COUNT - 1),
                        formatStrength(record.selected()), scratchText(record.result())));
            }
            g.renderComponentTooltip(font, lines, x, y);
            return;
        }
        super.renderTooltip(g, x, y);
    }
}
