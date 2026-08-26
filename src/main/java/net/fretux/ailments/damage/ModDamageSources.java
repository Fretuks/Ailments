package net.fretux.ailments.damage;

import net.fretux.ailments.AscendAilments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public final class ModDamageSources {
    public static final ResourceKey<DamageType> BLEED = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AscendAilments.MOD_ID, "bleed"));

    public static DamageSource bleed(Level level) {
        return bleed(level, null);
    }

    public static DamageSource bleed(Level level, @Nullable Entity source) {
        var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLEED);
        // Periodic Bleed has an owning attacker for attribution, but no direct entity (it is not another melee hit).
        return source == null ? new DamageSource(type) : new DamageSource(type, null, source);
    }
    private ModDamageSources() {}
}
