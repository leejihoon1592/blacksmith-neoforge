package io.github.leejihoon1592.blacksmith.research;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ResearchNetwork {
    private record Record(int menu, String material, String property, double value) implements CustomPacketPayload {
        private static final Type<Record> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("blacksmith", "record_research"));
        private static final StreamCodec<FriendlyByteBuf, Record> CODEC = StreamCodec.of(
                (buf, msg) -> {
                    buf.writeInt(msg.menu());
                    buf.writeUtf(msg.material(), 128);
                    buf.writeUtf(msg.property(), 16);
                    buf.writeDouble(msg.value());
                },
                buf -> new Record(buf.readInt(), buf.readUtf(128), buf.readUtf(16), buf.readDouble()));
        @Override public Type<Record> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Record.TYPE, Record.CODEC, (msg, context) -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof ResearchMenu menu && menu.containerId == msg.menu()) {
                menu.record(player, msg.material(), msg.property(), msg.value());
            }
        });
    }

    public static void record(ResearchMenu menu, String material, String property, double value) {
        PacketDistributor.sendToServer(new Record(menu.containerId, material, property, value));
    }

    private ResearchNetwork() {}
}
