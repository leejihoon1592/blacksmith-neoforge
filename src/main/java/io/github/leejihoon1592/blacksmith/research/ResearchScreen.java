package io.github.leejihoon1592.blacksmith.research;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@net.neoforged.fml.common.EventBusSubscriber(modid="blacksmith",value=Dist.CLIENT)
public final class ResearchScreen extends AbstractContainerScreen<ResearchMenu> {
    private boolean pumping;
    private int ticks,selected;
    private EditBox point,seconds;
    private int displayedScratchTurn = -1;
    private final Button[] scratchButtons = new Button[StrengthCandidates.COUNT];
    public ResearchScreen(ResearchMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title); imageWidth=340;imageHeight=menu.mode==2?170:232;
    }
    @SubscribeEvent public static void setup(RegisterMenuScreensEvent event) { event.register(ResearchRegistry.MENU.get(), ResearchScreen::new); }
    private static Component tr(String key) { return Component.translatable("research.blacksmith."+key); }
    private void button(int x,int y,int width,Component text,Runnable action) {
        addRenderableWidget(new Button(leftPos+x,topPos+y,width,20,text,b->action.run(),supplier->supplier.get()) {
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial) {
                g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),isHoveredOrFocused()?0xff85714e:0xff5d4b32);
                g.drawString(font,getMessage(),getX()+(getWidth()-font.width(getMessage()))/2,getY()+6,0xfff2e4c4,false);
            }
        });
    }
    private EditBox input(int x,int y) {
        EditBox box=new EditBox(font,leftPos+x,topPos+y,85,18,tr("value")) {
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial) {
                g.fill(getX()-1,getY()-1,getX()+getWidth()+1,getY()+getHeight()+1,isFocused()?0xffcfb56d:0xff66583f);
                g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),0xff191919);
                g.drawString(font,getValue()+(isFocused()?"|":""),getX()+3,getY()+5,0xffeeeeee,false);
            }
        };
        box.setMaxLength(10);box.setFilter(s->s.matches("[0-9]*\\.?[0-9]*"));addRenderableWidget(box);return box;
    }
    @Override protected void init() {
        super.init();pumping=false;
        if(menu.mode==0) {
            button(185,72,130,tr("start"),()->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0));
            button(185,100,130,tr("bellows"),()->{pumping=true;pump();});
        } else if (menu.mode != ResearchMenu.STRENGTH_MODE) {
            button(150,34,22,Component.literal("<"),()->select(-1));
            button(306,34,22,Component.literal(">"),()->select(1));
            point=input(150,80); seconds=input(150,116);
            button(246,79,80,tr("record"),()->save("point",point));
            button(246,115,80,tr("record"),()->save("seconds",seconds));
        }
        if (menu.mode == ResearchMenu.STRENGTH_MODE) {
                for (int i = 0; i < scratchButtons.length; i++) {
                    int choice = i;
                    scratchButtons[i] = addRenderableWidget(Button.builder(Component.empty(),
                            b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                                    ResearchMenu.FIRST_SCRATCH_BUTTON + menu.scratchCount() * StrengthCandidates.COUNT + choice))
                            .bounds(leftPos + 116 + 22 * i, topPos + 136, 22, 16).build());
                }
                updateScratchButtons();
        }
    }
    private void select(int delta) { if(!menu.materials.isEmpty()) selected=Math.floorMod(selected+delta,menu.materials.size());point.setValue("");seconds.setValue(""); }
    private void save(String property,EditBox box) {
        if(menu.materials.isEmpty()) return;
        try { ResearchNetwork.record(menu,menu.materials.get(selected),property,Double.parseDouble(box.getValue())); } catch(NumberFormatException ignored) {}
    }
    private void pump() { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1); }
    @Override protected void containerTick() {
        super.containerTick();
        if (pumping && ++ticks % 4 == 0) pump();
        if (menu.mode == ResearchMenu.STRENGTH_MODE) updateScratchButtons();
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

    @Override public boolean mouseReleased(double x,double y,int button) { pumping=false;return super.mouseReleased(x,y,button); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key!=256) {
            if(point!=null&&point.isFocused()) { point.keyPressed(key,scan,modifiers);return true; }
            if(seconds!=null&&seconds.isFocused()) { seconds.keyPressed(key,scan,modifiers);return true; }
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.fill(leftPos-4,topPos-4,leftPos+imageWidth+4,topPos+imageHeight+4,0xff443324);
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xffe5d7b4);
        for(var slot:menu.slots) {
            g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff594f3c);
            g.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xffb4ab96);
        }
    }
    private void text(GuiGraphics g,Component text,int x,int y) { g.drawString(font,text,x,y,0xff392d20,false); }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        text(g,title,12,12);
        if(menu.mode==0) {
            text(g,tr("inputs"),20,29);
            text(g,tr("notes"),20,86);text(g,tr("extracts"),20,119);
            text(g,Component.literal(menu.value(0)+" °C"),185,33);
            text(g,tr(menu.value(2)==1?"running":"idle"),185,49);
            text(g,tr("hold_hint"),180,132);
            text(g,Component.translatable("research.blacksmith.fuel",menu.value(1)/20),180,143);
        } else if (menu.mode == ResearchMenu.STRENGTH_MODE) {
                text(g, tr("scratch_supplies"), 49, 29);
                text(g, tr("appraisal_sample"), 96, 29);
                int count = menu.scratchCount();
                if (count > 0) {
                    text(g, scratchText(menu.scratchObservation(count - 1).result()), 150, 21);
                    text(g, Component.translatable("research.blacksmith.scratch_history", count, StrengthCandidates.TURNS), 268, 21);
                }
                text(g, tr("notes"), 20, 69);
                text(g, tr("hover_notes"), 20, 109);
        } else {
            text(g, tr("book"), 20, 29);
            if (menu.mode == 1) {
                text(g, tr("notes"), 20, 69);
                text(g, tr("hover_notes"), 20, 109);
            }
            if(!menu.materials.isEmpty()) {
                String id=menu.materials.get(selected);
                String key=java.util.Set.of("stone","deepslate","netherrack","end_stone").contains(id)?"block.minecraft."+id:"item.blacksmith."+id+"_ingot";
                text(g,Component.translatable(key),178,40);
                text(g,tr("point"),150,65);text(g,tr("seconds_label"),150,104);
                var root=menu.book().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("ResearchEntries");
                if(!root.isEmpty()) {var entry=root.getCompound(id);grade(g,entry,"point",20,122);grade(g,entry,"seconds",20,136);}
            }
        }
    }
    private void grade(GuiGraphics g,net.minecraft.nbt.CompoundTag data,String property,int x,int y) {
        if(!data.contains(property)) return;
        int grade=data.getInt(property+"Grade");
        g.drawString(font,Component.literal(""+data.getDouble(property)+(property.equals("point")?" °C ":" s ")).append(tr("grade."+grade)),x,y,grade==2?0xff21632a:grade==1?0xff876300:0xffb02525,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {super.render(g,x,y,partial);renderTooltip(g,x,y);}
    @Override protected void renderTooltip(GuiGraphics g,int x,int y) {
        if (menu.mode == ResearchMenu.STRENGTH_MODE && menu.scratchCount() > 0
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
