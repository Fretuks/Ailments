package net.fretux.ailments.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Three-stack armor-breaking ailment. Arcane scaling is handled only when its duration is applied. */
public final class FractureEffect extends MobEffect {
    public static final String ARMOR_UUID = "9d353786-61e3-43d8-b980-1471e9b84e58";
    public static final String TOUGHNESS_UUID = "6610760d-38d6-42de-b3ab-621d84896d42";

    public FractureEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B8178);
        addAttributeModifier(Attributes.ARMOR, ARMOR_UUID, -2.0, AttributeModifier.Operation.ADDITION);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_UUID, -1.0,
                AttributeModifier.Operation.ADDITION);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        int stacks = Math.min(3, Math.max(0, amplifier) + 1);
        if (modifier.getId().toString().equals(ARMOR_UUID)) return -2.0 * stacks;
        if (modifier.getId().toString().equals(TOUGHNESS_UUID)) return -1.0 * stacks;
        return super.getAttributeModifierValue(amplifier, modifier);
    }
}
