package net.fretux.ailments.registry;

import net.fretux.ailments.AscendAilments;
import net.fretux.ailments.effect.BleedEffect;
import net.fretux.ailments.effect.FractureEffect;
import net.fretux.ailments.effect.SoulRotEffect;
import net.fretux.ailments.effect.WindedEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, AscendAilments.MOD_ID);

    public static final RegistryObject<MobEffect> SOUL_ROT = EFFECTS.register("soul_rot", SoulRotEffect::new);
    public static final RegistryObject<MobEffect> BLEED = EFFECTS.register("bleed", BleedEffect::new);
    public static final RegistryObject<MobEffect> FRACTURE = EFFECTS.register("fracture", FractureEffect::new);
    public static final RegistryObject<MobEffect> FEAR = EFFECTS.register("fear",
            () -> new SimpleAilmentEffect(MobEffectCategory.HARMFUL, 0xAA0000));
    public static final RegistryObject<MobEffect> CHARM = EFFECTS.register("charm",
            () -> new SimpleAilmentEffect(MobEffectCategory.HARMFUL, 0xCC7AB6));
    public static final RegistryObject<MobEffect> TAUNT = EFFECTS.register("taunt",
            () -> new SimpleAilmentEffect(MobEffectCategory.HARMFUL, 0xCC7AB6));
    public static final RegistryObject<MobEffect> OVERCHARM = EFFECTS.register("overcharm",
            () -> new SimpleAilmentEffect(MobEffectCategory.BENEFICIAL, 0xFFD966));
    public static final RegistryObject<MobEffect> WINDED = EFFECTS.register("winded", WindedEffect::new);

    public static void register(IEventBus bus) { EFFECTS.register(bus); }
    private ModEffects() {}

    private static final class SimpleAilmentEffect extends MobEffect {
        private SimpleAilmentEffect(MobEffectCategory category, int color) { super(category, color); }
    }
}
