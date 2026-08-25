package net.fretux.ailments.damage;

import net.fretux.ailments.AscendAilments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class ModDamageSources {
    public static final ResourceKey<DamageType> BLEED = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AscendAilments.MOD_ID, "bleed"));

    public static DamageSource bleed(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(BLEED));
    }
    private ModDamageSources() {}
}
