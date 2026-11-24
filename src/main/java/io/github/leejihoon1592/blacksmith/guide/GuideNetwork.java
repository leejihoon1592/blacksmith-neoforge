package io.github.leejihoon1592.blacksmith.guide;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.world.entity.player.Player;

public final class GuideNetwork {
    private static final String READ_KEY = "blacksmith.read.first_mining";

    private record Open(GuidebookItem.Book book, boolean read) implements CustomPacketPayload {
        private static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("blacksmith", "open_guide"));
        private static final StreamCodec<FriendlyByteBuf, Open> CODEC = StreamCodec.of(
                (buf, msg) -> { buf.writeEnum(msg.book()); buf.writeBoolean(msg.read()); },
                buf -> new Open(buf.readEnum(GuidebookItem.Book.class), buf.readBoolean()));
        @Override public Type<Open> type() { return TYPE; }
    }

    private record ReadMining() implements CustomPacketPayload {
        private static final Type<ReadMining> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("blacksmith", "read_mining"));
        private static final StreamCodec<FriendlyByteBuf, ReadMining> CODEC = StreamCodec.unit(new ReadMining());
        @Override public Type<ReadMining> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Open.TYPE, Open.CODEC,
                (msg, context) -> MiningGuideScreen.open(msg.book(), msg.read()));
        registrar.playToServer(ReadMining.TYPE, ReadMining.CODEC, (msg, context) -> {
            var root = context.player().getPersistentData();
            var data = root.getCompound(Player.PERSISTED_NBT_TAG);
            data.putBoolean(READ_KEY, true);
            root.put(Player.PERSISTED_NBT_TAG, data);
        });
    }

    static void open(ServerPlayer player, GuidebookItem.Book book) {
        boolean read = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(READ_KEY);
        PacketDistributor.sendToPlayer(player, new Open(book, read));
    }

    static void markMiningRead() { PacketDistributor.sendToServer(new ReadMining()); }

    private GuideNetwork() {}
}
