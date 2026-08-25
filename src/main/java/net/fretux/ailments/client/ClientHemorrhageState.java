package net.fretux.ailments.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientHemorrhageState {
    private static float progress;

    public static float getProgress() { return progress; }
    public static void setProgress(float value) {
        progress = Math.max(0.0F, Math.min(1.0F, value));
    }

    private ClientHemorrhageState() {}
}
