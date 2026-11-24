package io.github.leejihoon1592.blacksmith.guide;

import io.github.leejihoon1592.blacksmith.item.GuidebookItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MiningGuideScreen extends Screen {
    private final GuidebookItem.Book book;
    private boolean mining;
    private boolean miningRead;
    private int left;
    private int top;

    private MiningGuideScreen(GuidebookItem.Book book, boolean miningRead) {
        super(Component.translatable("item.blacksmith." + book.id()));
        this.book = book;
        this.miningRead = miningRead;
    }

    static void open(GuidebookItem.Book book, boolean miningRead) {
        Minecraft.getInstance().setScreen(new MiningGuideScreen(book, miningRead));
    }

    private static Component text(String key) {
        return Component.translatable("guide.blacksmith." + key);
    }

    @Override
    protected void init() {
        left = (width - 292) / 2;
        top = (height - 174) / 2;
        if (book == GuidebookItem.Book.PRIMER) {
            Component label = mining ? text("contents") : text("mining").copy()
                    .append(miningRead ? "\u25cb" : "\u25b3")
                    .withStyle(style -> style.withColor(miningRead ? 0x287035 : 0xA77900));
            addRenderableWidget(new PlainTextButton(left + (mining ? 12 : 157),
                    top + (mining ? 137 : 57), font.width(label), font.lineHeight, label, button -> {
                mining = !mining;
                if (mining && !miningRead) {
                    miningRead = true;
                    GuideNetwork.markMiningRead();
                }
                rebuildWidgets();
            }, font) {
                @Override
                public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    Component link = getMessage().copy().withStyle(style -> style.withUnderlined(isHoveredOrFocused()));
                    graphics.drawString(font, link, getX(), getY(), 0xff493326, false);
                }
            });
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        int x = left, y = top;
        g.fill(x-4,y-3,x+296,y+178,0xff50392f);
        g.fill(x,y,x+146,y+174,0xffeee1c3);
        g.fill(x+146,y,x+292,y+174,0xfff5ead0);
        g.fill(x+143,y,x+148,y+174,0xffc3ac88);
        g.drawWordWrap(font,title,x+12,y+15,121,0xff634735);
        g.drawWordWrap(font,text(book.id() + ".intro"),x+12,y+48,119,0xff44382e);
        Component heading = book == GuidebookItem.Book.PRIMER
                ? text(mining ? "mining" : "contents") : text("records");
        g.drawString(font,heading,x+157,y+15,0xff493326,false);
        g.fill(x+157,y+34,x+280,y+35,0xffb69d75);
        if (mining) {
            g.renderItem(new ItemStack(Items.IRON_PICKAXE),x+157,y+45);
            g.drawWordWrap(font,text("iron"),x+177,y+46,103,0xff44382e);
            g.renderItem(new ItemStack(Items.DIAMOND_PICKAXE),x+157,y+75);
            g.drawWordWrap(font,text("diamond"),x+177,y+76,103,0xff44382e);
            g.drawWordWrap(font,text("enchantments"),x+157,y+105,123,0xff594236);
        } else if (book != GuidebookItem.Book.PRIMER) {
            g.drawWordWrap(font,text(book.id() + ".empty"),x+157,y+52,120,0xff594236);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }
}
