package net.fretux.ailments.util;

import net.fretux.ailments.config.AilmentsConfig;
import net.fretux.ailments.damage.ModDamageSources;
import net.fretux.ailments.network.AilmentNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Server-authoritative Hemorrhage progress produced by stacked Bleed damage. */
public final class HemorrhageTracker {
    private static final String PROGRESS = "ascend_ailments.hemorrhage.progress";

    /**
     * Records one successful Bleed damage tick and triggers the max-health burst when full.
     * Arcane potency is snapshotted when Bleed is applied, so an unloaded source cannot change an active meter.
     */
    public static boolean recordBleedDamage(LivingEntity target, int amplifier, double arcanePotency) {
        if (target.level().isClientSide
                || amplifier + 1 < AilmentsConfig.value(AilmentsConfig.HEMORRHAGE_MINIMUM_STACKS)) return false;
        double threshold = AilmentsConfig.value(AilmentsConfig.HEMORRHAGE_THRESHOLD);
        double added = AilmentsConfig.value(AilmentsConfig.HEMORRHAGE_FILL_PER_TICK)
                * Math.max(0.0, arcanePotency);
        if (added <= 0) return false;

        double progress = Math.min(threshold, getProgress(target) + added);
        if (progress < threshold) {
            target.getPersistentData().putDouble(PROGRESS, progress);
            syncDisplay(target);
            return false;
        }

        clear(target);
        if (target.isAlive()) {
            float damage = (float) (target.getMaxHealth()
                    * AilmentsConfig.value(AilmentsConfig.HEMORRHAGE_MAX_HEALTH_DAMAGE));
            LivingEntity source = EffectSourceUtil.getSourceInLevel(target, EffectSourceUtil.BLEED);
            if (damage > 0) target.hurt(ModDamageSources.bleed(target.level(), source), damage);
        }
        return true;
    }

    public static double getProgress(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        return data.contains(PROGRESS) ? Math.max(0.0, data.getDouble(PROGRESS)) : 0.0;
    }

    public static double getFraction(LivingEntity target) {
        double threshold = AilmentsConfig.value(AilmentsConfig.HEMORRHAGE_THRESHOLD);
        return threshold <= 0 ? 0.0 : Math.min(1.0, getProgress(target) / threshold);
    }

    public static void clear(LivingEntity target) {
        target.getPersistentData().remove(PROGRESS);
        hideDisplay(target);
    }

    /** Synchronizes the compact client HUD after login or a meter update. */
    public static void syncDisplay(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) return;
        double fraction = getFraction(player);
        AilmentNetwork.sendHemorrhageProgress(player,
                AilmentsConfig.value(AilmentsConfig.HEMORRHAGE_SHOW_PLAYER_BAR) ? (float) fraction : 0.0F);
    }

    /** Hides the client HUD while retaining persistent meter progress. */
    public static void hideDisplay(LivingEntity target) {
        if (target instanceof ServerPlayer player) AilmentNetwork.sendHemorrhageProgress(player, 0.0F);
    }

    private HemorrhageTracker() {}
}
