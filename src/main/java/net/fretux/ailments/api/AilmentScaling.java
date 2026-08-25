package net.fretux.ailments.api;

import net.fretux.ailments.compat.AscendCompat;
import net.fretux.ailments.config.AilmentsConfig;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/** Central, application-time Arcane scaling. The normalized curve is concave and reaches 1 at 100 Arcane. */
public final class AilmentScaling {
    public static double getDurationMultiplier(@Nullable LivingEntity source) {
        return getDurationMultiplierForArcane(AscendCompat.getArcane(source));
    }

    public static double getPotencyMultiplier(@Nullable LivingEntity source, AilmentType type) {
        return getPotencyMultiplierForArcane(AscendCompat.getArcane(source), type);
    }

    static double getPotencyMultiplierForArcane(int arcane, AilmentType type) {
        double maximumBonus = switch (type) {
            case SOUL_ROT -> AilmentsConfig.value(AilmentsConfig.SOUL_ROT_ARCANE_POTENCY);
            case BLEED -> AilmentsConfig.value(AilmentsConfig.BLEED_ARCANE_POTENCY);
            default -> 0.0;
        };
        return 1.0 + normalizedArcane(arcane) * maximumBonus;
    }

    public static int scaleDuration(int requestedTicks, @Nullable LivingEntity source) {
        if (requestedTicks <= 0) return 1;
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE,
                Math.round(requestedTicks * getDurationMultiplier(source))));
    }

    public static double getDurationMultiplierForArcane(int arcane) {
        return 1.0 + normalizedArcane(arcane) * AilmentsConfig.value(AilmentsConfig.ARCANE_DURATION_BONUS);
    }

    static double normalizedArcane(int arcane) {
        return arcane <= 0 ? 0.0 : Math.pow(Math.min(100, arcane) / 100.0, 0.8);
    }
    private AilmentScaling() {}
}
