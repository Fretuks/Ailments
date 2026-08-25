package net.fretux.ailments.effect;

import net.fretux.ailments.config.AilmentsConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class WindedEffect extends MobEffect {
    public static final String MOVEMENT_UUID = "f0eeefea-3f57-4ebb-b8d6-d4ae352efb50";
    public static final String ATTACK_SPEED_UUID = "3b6a4969-0cd5-4e9c-9b64-a8cd0a0f8d0f";

    public WindedEffect() {
        super(MobEffectCategory.HARMFUL, 0x567051);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVEMENT_UUID, -0.25,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID, -0.25,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        if (modifier.getId().toString().equals(MOVEMENT_UUID))
            return -AilmentsConfig.value(AilmentsConfig.WINDED_MOVEMENT_REDUCTION);
        if (modifier.getId().toString().equals(ATTACK_SPEED_UUID))
            return -AilmentsConfig.value(AilmentsConfig.WINDED_ATTACK_REDUCTION);
        return super.getAttributeModifierValue(amplifier, modifier);
    }
}
