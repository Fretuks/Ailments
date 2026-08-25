package net.fretux.ailments.util;

import net.minecraft.world.entity.LivingEntity;

public final class MentalControlUtil {
    public static boolean isMentalControlResistant(LivingEntity target) {
        return target.getType().is(ModEntityTypeTags.MENTAL_CONTROL_RESISTANT);
    }
    public static boolean isMentalControlImmune(LivingEntity target) {
        return target.getType().is(ModEntityTypeTags.MENTAL_CONTROL_IMMUNE);
    }
    public static int resistantDuration(int duration, int divisor) {
        return Math.max(20, duration / divisor);
    }
    private MentalControlUtil() {}
}
