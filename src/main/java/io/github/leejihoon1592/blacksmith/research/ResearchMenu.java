package io.github.leejihoon1592.blacksmith.research;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.util.List;

public final class ResearchMenu extends AbstractContainerMenu {
    public final int mode;
    public final List<String> materials;
    private final Container contents;
    private final ResearchBlockEntity station;
    private final ContainerData values;
    private final int machineSlots;
    private InteractionHand hand;
    public ResearchMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readInt(), new SimpleContainer(13), null, new SimpleContainerData(4), buf.readList(b -> b.readUtf(128)));
    }
    public ResearchMenu(int id, Inventory inventory, ResearchBlockEntity station) {
        this(id, inventory, station.table()?1:0, station, station, new ContainerData() {
            public int get(int i) { return switch(i) { case 0 -> station.temperature(); case 1 -> station.fuel(); case 2 -> station.running()?1:0; default -> station.pendingCount(); }; }
            public void set(int i, int value) {}
            public int getCount() { return 4; }
        }, ResearchData.ids());
    }
    private ResearchMenu(int id, Inventory inventory, int mode, Container contents, ResearchBlockEntity station, ContainerData values, List<String> materials) {
        super(ResearchRegistry.MENU.get(), id);
        this.mode=mode; this.contents=contents; this.station=station; this.values=values; this.materials=materials;
        addDataSlots(values);
        if (mode==0) {
            for(int i=0;i<13;i++) {
                final int s=i;
                int x=i<4?20+i*30:i<10?20+(i-4)*19:20+(i-10)*19;
                int y=i<4?42:i<10?100:132;
                addSlot(new Slot(contents,i,x,y) {
                    public boolean mayPlace(ItemStack stack) {
                        return switch(s) {
                            case 0 -> (station==null || !station.running()) && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_raw_fragment");
                            case 1 -> stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING)>0;
                            case 2 -> stack.is(Items.PAPER);
                            case 3 -> stack.is(ResearchRegistry.PEN.get());
                            default -> false;
                        };
                    }
                    public int getMaxStackSize() { return s==0?1:super.getMaxStackSize(); }
                });
            }
        } else {
            addSlot(new Slot(contents,0,20,42) {
                public boolean mayPlace(ItemStack s) { return mode!=2 && s.is(BlacksmithMod.MATERIAL_JOURNAL.get()); }
                public boolean mayPickup(Player p) { return mode!=2; }
            });
            if(mode==1) for(int i=1;i<=6;i++) addSlot(new Slot(contents,i,20+(i-1)*19,82) {
                public boolean mayPlace(ItemStack s) { return s.is(ResearchRegistry.NOTE.get()); }
            });
        }
        machineSlots=slots.size();
        if(mode!=2) {
            for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(inventory,col+row*9+9,79+col*18,154+row*18));
            for(int col=0;col<9;col++) addSlot(new Slot(inventory,col,79+col*18,212));
        }
    }
    public static void writeOpen(FriendlyByteBuf buf, int mode) { buf.writeInt(mode); buf.writeCollection(ResearchData.ids(), (b,s)->b.writeUtf(s,128)); }
    public static void openBook(ServerPlayer player, InteractionHand hand) {
        ItemStack held=player.getItemInHand(hand);
        player.openMenu(new SimpleMenuProvider((id,inventory,p)-> {
            SimpleContainer contents=new SimpleContainer(13); contents.setItem(0,held);
            ResearchMenu menu=new ResearchMenu(id,inventory,2,contents,null,new SimpleContainerData(4),ResearchData.ids());
            menu.hand=hand; return menu;
        },Component.translatable("item.blacksmith.material_journal")),buf->writeOpen(buf,2));
    }
    public int value(int i) { return values.get(i); }
    public ItemStack book() { return contents.getItem(0); }
    public void record(Player player,String material,String property,double guess) {
        if(mode==0 || !stillValid(player) || !book().is(BlacksmithMod.MATERIAL_JOURNAL.get()) || !(property.equals("point")||property.equals("seconds"))) return;
        ResearchData.record(book(),material,property,guess);
        contents.setChanged(); player.getInventory().setChanged(); broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(station==null || mode!=0 || !stillValid(player)) return false;
        if(button==0) station.start(); else if(button==1) station.pump(); else return false;
        return true;
    }
    @Override public boolean stillValid(Player player) {
        return station!=null?station.stillValid(player):hand==null || player.getItemInHand(hand)==book();
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(mode==2 || index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot=slots.get(index); if(!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(), original=stack.copy();
        if(index<machineSlots) { if(!moveItemStackTo(stack,machineSlots,slots.size(),true)) return ItemStack.EMPTY; }
        else if(!moveItemStackTo(stack,0,mode==0?4:machineSlots,false)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,stack); return original;
    }
}
