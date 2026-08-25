package net.fretux.ailments.util;

import net.fretux.ailments.AscendAilments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Public item tags consumed by Ascend: Ailments integrations. */
public final class ModItemTags {
    /** Items in this tag apply one Bleed stack on direct melee hits. */
    public static final TagKey<Item> BLEED_ON_HIT = tag("bleed_on_hit");
    /** Items in these tags apply the corresponding ailment on direct melee hits. */
    public static final TagKey<Item> SOUL_ROT_ON_HIT = tag("soul_rot_on_hit");
    public static final TagKey<Item> FRACTURE_ON_HIT = tag("fracture_on_hit");
    public static final TagKey<Item> FEAR_ON_HIT = tag("fear_on_hit");
    public static final TagKey<Item> CHARM_ON_HIT = tag("charm_on_hit");
    public static final TagKey<Item> TAUNT_ON_HIT = tag("taunt_on_hit");
    public static final TagKey<Item> OVERCHARM_ON_HIT = tag("overcharm_on_hit");
    public static final TagKey<Item> WINDED_ON_HIT = tag("winded_on_hit");

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(AscendAilments.MOD_ID, path));
    }

    private ModItemTags() {}
}
