package net.fretux.ailments.client;

import net.fretux.ailments.AscendAilments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AscendAilments.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HemorrhageOverlay {
    private static final int BAR_WIDTH = 96;
    private static final int BAR_HEIGHT = 5;
    private static final int BORDER = 1;

    private static final IGuiOverlay OVERLAY = (gui, graphics, partialTick, screenWidth, screenHeight) -> {
        float progress = ClientHemorrhageState.getProgress();
        Minecraft minecraft = Minecraft.getInstance();
        if (progress <= 0.0F || minecraft.options.hideGui || minecraft.player == null
                || !minecraft.player.isAlive()) return;

        // Souls-like status buildup placement: centered below the crosshair, clear of boss bars and the hotbar.
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = Math.min(screenHeight - 52, screenHeight / 2 + 54);
        int filled = Math.round((BAR_WIDTH - BORDER * 2) * progress);

        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xB8000000);
        graphics.fill(x + BORDER, y + BORDER, x + BAR_WIDTH - BORDER,
                y + BAR_HEIGHT - BORDER, 0xA02A0A0A);
        if (filled > 0) graphics.fill(x + BORDER, y + BORDER, x + BORDER + filled,
                y + BAR_HEIGHT - BORDER, 0xFFE04444);

        Font font = minecraft.font;
        Component label = Component.translatable("gui.ascend_ailments.hemorrhage");
        int labelX = (screenWidth - font.width(label)) / 2;
        graphics.drawString(font, label, labelX, y - 10, 0xFFD8A0A0, true);
    };

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("hemorrhage", OVERLAY);
    }

    private HemorrhageOverlay() {}
}
