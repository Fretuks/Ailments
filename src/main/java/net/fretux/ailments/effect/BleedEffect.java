package net.fretux.ailments.effect;

import net.fretux.ailments.api.AilmentApi;
import net.fretux.ailments.config.AilmentsConfig;
import net.fretux.ailments.damage.ModDamageSources;
import net.fretux.ailments.util.EffectSourceUtil;
import net.fretux.ailments.util.HemorrhageTracker;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class BleedEffect extends MobEffect {
    public BleedEffect() { super(MobEffectCategory.HARMFUL, 0xAA0000); }

    @Override public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = amplifier <= 0 ? AilmentsConfig.value(AilmentsConfig.BLEED_INTERVAL_0)
                : amplifier == 1 ? AilmentsConfig.value(AilmentsConfig.BLEED_INTERVAL_1)
                : AilmentsConfig.value(AilmentsConfig.BLEED_INTERVAL_2);
        return duration % interval == 0;
    }

    @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        if (!AilmentApi.canBleed(entity)) {
            HemorrhageTracker.clearIfPresent(entity);
            return;
        }
        double base = amplifier <= 0 ? AilmentsConfig.value(AilmentsConfig.BLEED_DAMAGE_0)
                : amplifier == 1 ? AilmentsConfig.value(AilmentsConfig.BLEED_DAMAGE_1)
                : AilmentsConfig.value(AilmentsConfig.BLEED_DAMAGE_2);
        float potency = EffectSourceUtil.getPotency(entity, EffectSourceUtil.BLEED);
        LivingEntity source = EffectSourceUtil.getSourceInLevel(entity, EffectSourceUtil.BLEED);
        boolean damaged = entity.hurt(ModDamageSources.bleed(entity.level(), source), (float) (base * potency));
        if (damaged) HemorrhageTracker.recordBleedDamage(entity, amplifier, potency);
    }
}
