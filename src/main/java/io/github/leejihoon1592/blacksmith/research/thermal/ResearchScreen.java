package io.github.leejihoon1592.blacksmith.research.thermal;

import io.github.leejihoon1592.blacksmith.research.ResearchRegistry;
import io.github.leejihoon1592.blacksmith.research.ResearchStationScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;

@net.neoforged.fml.common.EventBusSubscriber(modid="blacksmith",value=Dist.CLIENT)
public final class ResearchScreen extends ResearchStationScreen<ResearchMenu> {
    private boolean pumping;
    private int ticks,selected;
    private EditBox point,seconds,hardness,elasticity,polish;
    private Button hardnessRecord, elasticityRecord, polishRecord;
    public ResearchScreen(ResearchMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title); imageWidth=340;imageHeight=menu.mode==2?212:menu.mode==1?289:232;
    }
    @SubscribeEvent public static void setup(RegisterMenuScreensEvent event) { event.register(ResearchRegistry.MENU.get(), ResearchScreen::new); }
    private Button button(int x,int y,int width,Component text,Runnable action) {
        return addRenderableWidget(new Button(leftPos+x,topPos+y,width,20,text,b->action.run(),supplier->supplier.get()) {
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
            button(253,8,62,tr("clean_furnace"),()->{
                pumping = false;
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResearchMenu.CLEAN_FURNACE_BUTTON);
            });
            button(185,100,130,tr("bellows"),()->{pumping=true;pump();});
        } else {
            button(150,34,22,Component.literal("<"),()->select(-1));
            button(306,34,22,Component.literal(">"),()->select(1));
            point=input(150,70); seconds=input(150,97);
            button(246,69,80,tr("record"),()->save("point",point));
            button(246,96,80,tr("record"),()->save("seconds",seconds));
            if (menu.mode == 1) {
                hardness = input(150, 124);
                hardnessRecord = button(246, 123, 80, tr("record"), () -> save("hardness", hardness));
                elasticity = input(150, 151);
                elasticityRecord = button(246, 150, 80, tr("record"), () -> save("elasticity", elasticity));
                polish = input(150, 178);
                polishRecord = button(246, 177, 80, tr("record"), () -> save("polish", polish));
            }
        }
    }
    private void select(int delta) { if(!menu.materials.isEmpty()) selected=Math.floorMod(selected+delta,menu.materials.size());point.setValue("");seconds.setValue("");if(hardness!=null) hardness.setValue("");if(elasticity!=null) elasticity.setValue("");if(polish!=null) polish.setValue(""); }
    private void save(String property,EditBox box) {
        if(menu.materials.isEmpty() || (ResearchData.isMechanicalProperty(property) && !ResearchData.isMetal(menu.materials.get(selected)))) return;
        try { ResearchNetwork.record(menu,menu.materials.get(selected),property,Double.parseDouble(box.getValue())); } catch(NumberFormatException ignored) {}
    }
    private void pump() { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1); }
    @Override protected void containerTick() {
        super.containerTick();
        if (pumping && ++ticks % 4 == 0) pump();
    }

    @Override public boolean mouseReleased(double x,double y,int button) { pumping=false;return super.mouseReleased(x,y,button); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key!=256) {
            if(point!=null&&point.isFocused()) { point.keyPressed(key,scan,modifiers);return true; }
            if(hardness!=null&&hardness.visible&&hardness.isFocused()) { hardness.keyPressed(key,scan,modifiers);return true; }
            if(polish!=null&&polish.visible&&polish.isFocused()) { polish.keyPressed(key,scan,modifiers);return true; }
            if(elasticity!=null&&elasticity.visible&&elasticity.isFocused()) { elasticity.keyPressed(key,scan,modifiers);return true; }
            if(seconds!=null&&seconds.isFocused()) { seconds.keyPressed(key,scan,modifiers);return true; }
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        text(g,title,12,12);
        if(menu.mode==0) {
            text(g,tr("inputs"),20,29);
            text(g,tr("notes"),20,86);text(g,tr("extracts"),20,119);
            text(g,Component.literal(menu.value(0)+" °C"),185,33);
            text(g,tr(menu.value(2)==1?"running":"idle"),185,49);
            text(g,tr("hold_hint"),180,132);
            text(g,Component.translatable("research.blacksmith.fuel",menu.value(1)/20),180,143);
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
                text(g,tr("point"),150,58);text(g,tr("seconds_label"),150,85);
                var root=menu.book().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("ResearchEntries");
                if (elasticity != null) {
                    boolean metal = ResearchData.isMetal(id);
                    hardness.visible = metal;
                    hardnessRecord.visible = metal;
                    elasticity.visible = metal;
                    elasticityRecord.visible = metal;
                    polish.visible = metal;
                    polishRecord.visible = metal;
                    if (metal) {
                        text(g, tr("hardness_label"), 150, 112);
                        text(g, tr("elasticity_label"), 150, 139);
                        text(g, tr("polish_label"), 150, 166);
                    }
                }
                var entry = root.getCompound(id);
                grade(g, entry, "point", 20, 122);
                grade(g, entry, "seconds", 20, 136);
                if (entry.contains("point")) {
                    double recorded = entry.getDouble("point");
                    int lower = (int) Math.ceil(recorded * 0.07) * 10;
                    int upper = (int) Math.ceil(recorded * 0.09) * 10;
                    textResult(g, Component.translatable("research.blacksmith.forging_range", lower, upper),
                            entry.getInt("pointGrade"), 20, 148);
                }
                grade(g, entry, "hardness", 20, 160);
                grade(g, entry, "elasticity", 20, 172);
                grade(g, entry, "polish", 20, 184);
                grade(g, entry, "durability", 20, 196);
            }
        }
    }
    private void grade(GuiGraphics g,net.minecraft.nbt.CompoundTag data,String property,int x,int y) {
        if(!data.contains(property)) return;
        double value = data.getDouble(property);
        Component label = switch (property) {
            case "point" -> Component.literal(value + " °C");
            case "seconds" -> Component.literal(value + " s");
            case "hardness" -> Component.translatable("research.blacksmith.hardness_value", value);
            case "durability" -> Component.translatable("research.blacksmith.durability_value", value);
            case "polish" -> Component.translatable("research.blacksmith.polish_value", value);
            default -> Component.translatable("research.blacksmith.elasticity_value", value);
        };
        textResult(g, label, data.getInt(property + "Grade"), x, y);
    }

    private void textResult(GuiGraphics g, Component label, int grade, int x, int y) {
        g.drawString(font, label, x, y, grade == 2 ? 0xff21632a : grade == 1 ? 0xff876300 : 0xffb02525, false);
    }
}
