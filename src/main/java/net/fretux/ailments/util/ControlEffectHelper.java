package net.fretux.ailments.util;

import net.fretux.ailments.api.AilmentApi;
import net.fretux.ailments.config.AilmentsConfig;
import net.fretux.ailments.registry.ModEffects;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;

public final class ControlEffectHelper {
    private static final String FEAR_DISTANCE = "ascend_ailments.fear.last_distance_sqr";
    private static final String SOUL_FOOD = "ascend_ailments.soul_rot.last_food";
    private static final String SOUL_SATURATION = "ascend_ailments.soul_rot.last_saturation";

    public static void handleLivingTick(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        handleSoulRotFood(entity);
        cleanDotData(entity);
        if (entity.hasEffect(ModEffects.FEAR.get())) tickFear(entity);
        else {
            EffectSourceUtil.clear(entity, EffectSourceUtil.FEAR);
            entity.getPersistentData().remove(FEAR_DISTANCE);
        }
        if (entity.hasEffect(ModEffects.TAUNT.get())) tickTaunt(entity);
        else EffectSourceUtil.clear(entity, EffectSourceUtil.TAUNT);
        if (entity.hasEffect(ModEffects.CHARM.get())) validateCharm(entity);
        else EffectSourceUtil.clear(entity, EffectSourceUtil.CHARM);
        if (!entity.hasEffect(ModEffects.OVERCHARM.get()))
            EffectSourceUtil.clear(entity, EffectSourceUtil.OVERCHARM);
    }

    private static void tickFear(LivingEntity entity) {
        LivingEntity source = validControlSource(entity, EffectSourceUtil.FEAR);
        if (source == null) {
            entity.removeEffect(ModEffects.FEAR.get());
            EffectSourceUtil.clear(entity, EffectSourceUtil.FEAR);
            entity.getPersistentData().remove(FEAR_DISTANCE);
            return;
        }
        if (MentalControlUtil.isMentalControlResistant(entity)) {
            MobEffectInstance winded = entity.getEffect(ModEffects.WINDED.get());
            if (winded == null || winded.getDuration() < 10)
                entity.addEffect(new MobEffectInstance(ModEffects.WINDED.get(), 25, 0, false, false, false));
            return;
        }
        if (entity instanceof Mob mob) flee(mob, source);
        else if (entity instanceof Player player) restrictFearedPlayer(player, source);
    }

    private static void flee(Mob mob, LivingEntity source) {
        mob.setTarget(null);
        if (mob.distanceToSqr(source) > 400.0) return;
        Vec3 away = mob.position().subtract(source.position());
        Vec3 horizontal = new Vec3(away.x, 0, away.z);
        if (horizontal.lengthSqr() < 1.0E-8) horizontal = new Vec3(1, 0, 0);
        Vec3 destination = mob.position().add(horizontal.normalize()
                .scale(AilmentsConfig.value(AilmentsConfig.FEAR_FLEE_DISTANCE)));
        mob.getNavigation().moveTo(destination.x, destination.y, destination.z,
                AilmentsConfig.value(AilmentsConfig.FEAR_FLEE_SPEED));
    }

    private static void restrictFearedPlayer(Player player, LivingEntity source) {
        double distance = player.distanceToSqr(source);
        CompoundTag data = player.getPersistentData();
        if (distance <= 144.0) {
            Vec3 toward = source.position().subtract(player.position());
            Vec3 horizontal = new Vec3(toward.x, 0, toward.z);
            if (horizontal.lengthSqr() >= 1.0E-8) {
                Vec3 direction = horizontal.normalize();
                Vec3 velocity = player.getDeltaMovement();
                double towardSpeed = velocity.x * direction.x + velocity.z * direction.z;
                if (towardSpeed > 0)
                    player.setDeltaMovement(velocity.x - direction.x * towardSpeed, velocity.y,
                            velocity.z - direction.z * towardSpeed);
                if (data.contains(FEAR_DISTANCE) && distance < data.getDouble(FEAR_DISTANCE)) {
                    double push = AilmentsConfig.value(AilmentsConfig.FEAR_PLAYER_PUSH);
                    player.push(-direction.x * push, 0, -direction.z * push);
                    player.hurtMarked = true;
                }
            }
        }
        data.putDouble(FEAR_DISTANCE, distance);
        if (distance <= 36.0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 0, false, false, false));
        }
    }

    private static void tickTaunt(LivingEntity entity) {
        LivingEntity source = validControlSource(entity, EffectSourceUtil.TAUNT);
        if (source == null) {
            entity.removeEffect(ModEffects.TAUNT.get());
            EffectSourceUtil.clear(entity, EffectSourceUtil.TAUNT);
            return;
        }
        if (MentalControlUtil.isMentalControlResistant(entity)) return;
        if (entity instanceof Mob mob && mob.getTarget() != source) mob.setTarget(source);
        else if (entity instanceof Player player)
            player.lookAt(EntityAnchorArgument.Anchor.EYES, source.getEyePosition());
    }

    private static void validateCharm(LivingEntity entity) {
        if (validControlSource(entity, EffectSourceUtil.CHARM) == null) {
            entity.removeEffect(ModEffects.CHARM.get());
            EffectSourceUtil.clear(entity, EffectSourceUtil.CHARM);
        }
    }

    public static boolean isOwnCharmSource(LivingEntity charmed, LivingEntity possibleSource) {
        LivingEntity source = EffectSourceUtil.getSource(charmed, EffectSourceUtil.CHARM);
        return source != null && source.getUUID().equals(possibleSource.getUUID());
    }

    public static LivingEntity validTauntSource(LivingEntity entity) {
        return validControlSource(entity, EffectSourceUtil.TAUNT);
    }

    private static LivingEntity validControlSource(LivingEntity target, String key) {
        LivingEntity source = EffectSourceUtil.getSource(target, key);
        return source != null && source.isAlive() && source.level() == target.level() ? source : null;
    }

    private static void handleSoulRotFood(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        CompoundTag data = player.getPersistentData();
        if (!player.hasEffect(ModEffects.SOUL_ROT.get())) {
            data.remove(SOUL_FOOD); data.remove(SOUL_SATURATION); return;
        }
        FoodData food = player.getFoodData();
        if (data.contains(SOUL_FOOD)) food.setFoodLevel(Math.min(food.getFoodLevel(), data.getInt(SOUL_FOOD)));
        if (data.contains(SOUL_SATURATION))
            food.setSaturation(Math.min(food.getSaturationLevel(), data.getFloat(SOUL_SATURATION)));
        data.putInt(SOUL_FOOD, food.getFoodLevel());
        data.putFloat(SOUL_SATURATION, food.getSaturationLevel());
    }

    private static void cleanDotData(LivingEntity entity) {
        if (!entity.hasEffect(ModEffects.SOUL_ROT.get())) EffectSourceUtil.clear(entity, EffectSourceUtil.SOUL_ROT);
        if (!entity.hasEffect(ModEffects.BLEED.get())) EffectSourceUtil.clear(entity, EffectSourceUtil.BLEED);
        if (!entity.hasEffect(ModEffects.FRACTURE.get())) EffectSourceUtil.clear(entity, EffectSourceUtil.FRACTURE);
    }
    private ControlEffectHelper() {}
}
