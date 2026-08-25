package net.fretux.ailments.network;

import net.fretux.ailments.AscendAilments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AilmentNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(AscendAilments.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.messageBuilder(ClientboundHemorrhagePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundHemorrhagePacket::encode)
                .decoder(ClientboundHemorrhagePacket::decode)
                .consumerMainThread(ClientboundHemorrhagePacket::handle)
                .add();
    }

    public static void sendHemorrhageProgress(ServerPlayer player, float fraction) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundHemorrhagePacket(fraction));
    }

    private AilmentNetwork() {}
}
