package net.fretux.ailments.util;

import net.fretux.ailments.AscendAilments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModEntityTypeTags {
    public static final TagKey<EntityType<?>> BLEED_IMMUNE = tag("bleed_immune");
    public static final TagKey<EntityType<?>> SOULLESS = tag("soulless");
    public static final TagKey<EntityType<?>> STURDY = tag("sturdy");
    public static final TagKey<EntityType<?>> MENTAL_CONTROL_RESISTANT = tag("mental_control_resistant");
    public static final TagKey<EntityType<?>> MENTAL_CONTROL_IMMUNE = tag("mental_control_immune");
    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(AscendAilments.MOD_ID, path));
    }
    private ModEntityTypeTags() {}
}
