package io.github.leejihoon1592.blacksmith.research;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class ResearchStationScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    protected ResearchStationScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 340;
        imageHeight = 232;
    }

    protected static Component tr(String key) {
        return Component.translatable("research.blacksmith." + key);
    }

    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.fill(leftPos-4,topPos-4,leftPos+imageWidth+4,topPos+imageHeight+4,0xff443324);
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xffe5d7b4);
        for(var slot:menu.slots) {
            g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff594f3c);
            g.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xffb4ab96);
        }
    }
    protected void text(GuiGraphics g,Component text,int x,int y) { g.drawString(font,text,x,y,0xff392d20,false); }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {super.render(g,x,y,partial);renderTooltip(g,x,y);}
    @Override protected void renderTooltip(GuiGraphics g,int x,int y) {
        if(hoveredSlot==null || !hoveredSlot.hasItem() || !menu.getCarried().isEmpty()) return;
        var stack=hoveredSlot.getItem();
        var lines=stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(minecraft.level), minecraft.player,net.minecraft.world.item.TooltipFlag.Default.NORMAL)
            .stream().flatMap(c->font.split(c,Math.min(250,width-24)).stream()).toList();
        int w=lines.stream().mapToInt(font::width).max().orElse(0),h=lines.size()*12;
        int tx=Math.max(4,Math.min(x+10,width-w-8)),ty=Math.max(4,Math.min(y+10,height-h-8));
        g.pose().pushPose();g.pose().translate(0,0,400);
        g.fill(tx-4,ty-4,tx+w+4,ty+h+4,0xff25211b);
        for(int i=0;i<lines.size();i++) g.drawString(font,lines.get(i),tx,ty+i*12,0xfff5e7cc,false);
        g.pose().popPose();
    }
}
