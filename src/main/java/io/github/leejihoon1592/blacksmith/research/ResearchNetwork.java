package io.github.leejihoon1592.blacksmith.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ResearchNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("blacksmith","research"),()->"1","1"::equals,"1"::equals);
    private record Record(int menu,String material,String property,double value) {}
    public static void register() {
        CHANNEL.messageBuilder(Record.class,0,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeInt(m.menu);b.writeUtf(m.material,128);b.writeUtf(m.property,16);b.writeDouble(m.value);})
            .decoder(b->new Record(b.readInt(),b.readUtf(128),b.readUtf(16),b.readDouble()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null && p.containerMenu instanceof ResearchMenu menu && menu.containerId==m.menu) menu.record(p,m.material,m.property,m.value);}).add();
    }
    public static void record(ResearchMenu menu,String material,String property,double value) { CHANNEL.sendToServer(new Record(menu.containerId,material,property,value)); }
}
