package net.fretux.ailments.network;

import net.fretux.ailments.client.ClientHemorrhageState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundHemorrhagePacket(float fraction) {
    public ClientboundHemorrhagePacket {
        fraction = Math.max(0.0F, Math.min(1.0F, fraction));
    }

    public static void encode(ClientboundHemorrhagePacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.fraction);
    }

    public static ClientboundHemorrhagePacket decode(FriendlyByteBuf buffer) {
        return new ClientboundHemorrhagePacket(buffer.readFloat());
    }

    public static void handle(ClientboundHemorrhagePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHemorrhageState.setProgress(packet.fraction)));
        context.setPacketHandled(true);
    }
}
