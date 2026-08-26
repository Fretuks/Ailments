package net.fretux.ailments.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

public final class EffectSourceUtil {
    public static final String SOUL_ROT = "ascend_ailments.soul_rot";
    public static final String BLEED = "ascend_ailments.bleed";
    public static final String FRACTURE = "ascend_ailments.fracture";
    public static final String FEAR = "ascend_ailments.fear";
    public static final String CHARM = "ascend_ailments.charm";
    public static final String TAUNT = "ascend_ailments.taunt";
    public static final String OVERCHARM = "ascend_ailments.overcharm";

    public static void setSource(LivingEntity target, String ailment, @Nullable LivingEntity source) {
        CompoundTag data = target.getPersistentData();
        if (source == null) data.remove(sourceKey(ailment));
        else data.putUUID(sourceKey(ailment), source.getUUID());
    }

    @Nullable public static UUID getSourceUuid(LivingEntity target, String ailment) {
        CompoundTag data = target.getPersistentData();
        String key = sourceKey(ailment);
        return data.hasUUID(key) ? data.getUUID(key) : null;
    }

    @Nullable public static LivingEntity getSource(LivingEntity target, String ailment) {
        UUID id = getSourceUuid(target, ailment);
        if (id == null || !(target.level() instanceof ServerLevel level)) return null;
        MinecraftServer server = level.getServer();
        Entity player = server.getPlayerList().getPlayer(id);
        if (player instanceof LivingEntity living) return living;
        Entity local = level.getEntity(id);
        if (local instanceof LivingEntity living) return living;
        for (ServerLevel candidate : server.getAllLevels()) {
            if (candidate == level) continue;
            Entity entity = candidate.getEntity(id);
            if (entity instanceof LivingEntity living) return living;
        }
        return null;
    }

    /** Resolves a loaded source only in the target's current level without scanning every server dimension. */
    @Nullable public static LivingEntity getSourceInLevel(LivingEntity target, String ailment) {
        UUID id = getSourceUuid(target, ailment);
        if (id == null || !(target.level() instanceof ServerLevel level)) return null;
        Entity entity = level.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    public static void setPotency(LivingEntity target, String ailment, double potency) {
        target.getPersistentData().putFloat(potencyKey(ailment), (float) Math.max(0, potency));
    }

    public static float getPotency(LivingEntity target, String ailment) {
        CompoundTag data = target.getPersistentData();
        String key = potencyKey(ailment);
        return data.contains(key) ? Math.max(0, data.getFloat(key)) : 1.0F;
    }

    public static void clear(LivingEntity target, String ailment) {
        target.getPersistentData().remove(sourceKey(ailment));
        target.getPersistentData().remove(potencyKey(ailment));
    }

    private static String sourceKey(String ailment) { return ailment + ".source"; }
    private static String potencyKey(String ailment) { return ailment + ".potency"; }
    private EffectSourceUtil() {}
}
