package net.fretux.ailments.effect;

import net.fretux.ailments.api.AilmentApi;
import net.fretux.ailments.config.AilmentsConfig;
import net.fretux.ailments.util.EffectSourceUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class SoulRotEffect extends MobEffect {
    public SoulRotEffect() { super(MobEffectCategory.HARMFUL, 0x8E0E0E); }

    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 20 == 0; }

    @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !AilmentApi.canSoulRot(entity)
                || !(entity instanceof Player player)) return;
        float base = (float) (AilmentsConfig.value(AilmentsConfig.SOUL_ROT_EXHAUSTION_BASE)
                + AilmentsConfig.value(AilmentsConfig.SOUL_ROT_EXHAUSTION_PER_AMP) * amplifier);
        player.causeFoodExhaustion(base * EffectSourceUtil.getPotency(entity, EffectSourceUtil.SOUL_ROT));
    }
}
