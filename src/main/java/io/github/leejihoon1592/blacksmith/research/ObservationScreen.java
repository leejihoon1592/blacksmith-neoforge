package io.github.leejihoon1592.blacksmith.research;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ObservationScreen extends Screen {
    private final ItemStack paper;
    private ObservationScreen(ItemStack paper) {super(Component.translatable("item.blacksmith.research_note"));this.paper=paper.copy();}
    public static void open(ItemStack paper) {Minecraft.getInstance().setScreen(new ObservationScreen(paper));}
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        renderBackground(g);
        int x=width/2-140,y=height/2-70;
        g.fill(x-4,y-4,x+284,y+144,0xff443324);g.fill(x,y,x+280,y+140,0xffe5d7b4);
        g.drawString(font,title,x+12,y+12,0xff392d20,false);
        int lineY=y+38;
        for(var text:ObservationItem.lines(paper)) for(var line:font.split(text,254)) {g.drawString(font,line,x+12,lineY,0xff392d20,false);lineY+=14;}
        super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen() {return false;}
}
