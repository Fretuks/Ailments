package net.fretux.ailments.util;

import net.fretux.ailments.api.AilmentApi;
import net.fretux.ailments.config.AilmentsConfig;
import net.fretux.ailments.registry.ModEffects;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ControlEffectHelper {
    private static final String FEAR_DISTANCE = "ascend_ailments.fear.last_distance_sqr";
    private static final String SOUL_FOOD = "ascend_ailments.soul_rot.last_food";
    private static final String SOUL_SATURATION = "ascend_ailments.soul_rot.last_saturation";
    private static final UUID FEAR_MOVEMENT_UUID = UUID.fromString("8158dc2a-8de9-49a2-b086-bd99ade47661");
    private static final UUID FEAR_ATTACK_UUID = UUID.fromString("4468e408-78e8-49f6-a7bc-9ae9028a0f48");

    public static void handleLivingTick(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        handleSoulRotFood(entity);
        cleanDotData(entity);
        if (entity.hasEffect(ModEffects.FEAR.get())) tickFear(entity);
        else {
            EffectSourceUtil.clear(entity, EffectSourceUtil.FEAR);
            entity.getPersistentData().remove(FEAR_DISTANCE);
            clearFearPlayerPenalties(entity);
        }
        if (entity.hasEffect(ModEffects.TAUNT.get())) tickTaunt(entity);
        else EffectSourceUtil.clear(entity, EffectSourceUtil.TAUNT);
        if (entity.hasEffect(ModEffects.CHARM.get())) validateCharm(entity);
        else EffectSourceUtil.clear(entity, EffectSourceUtil.CHARM);
        if (!entity.hasEffect(ModEffects.OVERCHARM.get()))
            EffectSourceUtil.clear(entity, EffectSourceUtil.OVERCHARM);
    }

    private static void tickFear(LivingEntity entity) {
        if (EffectSourceUtil.getSourceUuid(entity, EffectSourceUtil.FEAR) == null) {
            removeControlEffect(entity, ModEffects.FEAR.get(), EffectSourceUtil.FEAR);
            entity.getPersistentData().remove(FEAR_DISTANCE);
            return;
        }
        LivingEntity source = validControlSource(entity, EffectSourceUtil.FEAR);
        if (source == null) {
            if (entity instanceof Mob mob) mob.getNavigation().stop();
            entity.getPersistentData().remove(FEAR_DISTANCE);
            clearFearPlayerPenalties(entity);
            return;
        }
        if (MentalControlUtil.isMentalControlResistant(entity)) {
            clearFearPlayerPenalties(entity);
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
        setFearPlayerPenalties(player, distance <= 36.0);
    }

    private static void tickTaunt(LivingEntity entity) {
        if (EffectSourceUtil.getSourceUuid(entity, EffectSourceUtil.TAUNT) == null) {
            removeControlEffect(entity, ModEffects.TAUNT.get(), EffectSourceUtil.TAUNT);
            return;
        }
        LivingEntity source = validControlSource(entity, EffectSourceUtil.TAUNT);
        if (source == null) {
            if (entity instanceof Mob mob && mob.getTarget() != null) mob.setTarget(null);
            return;
        }
        if (MentalControlUtil.isMentalControlResistant(entity)) return;
        if (entity instanceof Mob mob && mob.getTarget() != source) mob.setTarget(source);
        else if (entity instanceof Player player)
            player.lookAt(EntityAnchorArgument.Anchor.EYES, source.getEyePosition());
    }

    private static void validateCharm(LivingEntity entity) {
        if (EffectSourceUtil.getSourceUuid(entity, EffectSourceUtil.CHARM) == null)
            removeControlEffect(entity, ModEffects.CHARM.get(), EffectSourceUtil.CHARM);
    }

    public static boolean isOwnCharmSource(LivingEntity charmed, LivingEntity possibleSource) {
        LivingEntity source = EffectSourceUtil.getSource(charmed, EffectSourceUtil.CHARM);
        return source != null && source.getUUID().equals(possibleSource.getUUID());
    }

    public static LivingEntity validTauntSource(LivingEntity entity) {
        return validControlSource(entity, EffectSourceUtil.TAUNT);
    }

    private static LivingEntity validControlSource(LivingEntity target, String key) {
        LivingEntity source = EffectSourceUtil.getSourceInLevel(target, key);
        return source != null && source.isAlive() ? source : null;
    }

    private static void setFearPlayerPenalties(Player player, boolean active) {
        setTransientModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), FEAR_MOVEMENT_UUID,
                "Ascend Ailments fear movement", -0.60, AttributeModifier.Operation.MULTIPLY_TOTAL, active);
        setTransientModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), FEAR_ATTACK_UUID,
                "Ascend Ailments fear weakness", -4.0, AttributeModifier.Operation.ADDITION, active);
    }

    private static void setTransientModifier(AttributeInstance attribute, UUID id, String name, double amount,
                                             AttributeModifier.Operation operation, boolean active) {
        if (attribute == null) return;
        AttributeModifier existing = attribute.getModifier(id);
        if (!active) {
            if (existing != null) attribute.removeModifier(id);
        } else if (existing == null) {
            attribute.addTransientModifier(new AttributeModifier(id, name, amount, operation));
        }
    }

    private static void clearFearPlayerPenalties(LivingEntity entity) {
        if (entity instanceof Player player) setFearPlayerPenalties(player, false);
    }

    private static void removeControlEffect(LivingEntity entity, net.minecraft.world.effect.MobEffect effect,
                                            String key) {
        entity.removeEffect(effect);
        EffectSourceUtil.clear(entity, key);
        if (key.equals(EffectSourceUtil.FEAR)) clearFearPlayerPenalties(entity);
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
