package net.fretux.ailments.event;

import net.fretux.ailments.AscendAilments;
import net.fretux.ailments.api.AilmentApi;
import net.fretux.ailments.config.AilmentsConfig;
import net.fretux.ailments.registry.ModEffects;
import net.fretux.ailments.util.ControlEffectHelper;
import net.fretux.ailments.util.EffectSourceUtil;
import net.fretux.ailments.util.MentalControlUtil;
import net.fretux.ailments.util.ModItemTags;
import net.fretux.ailments.util.HemorrhageTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

/** One deterministic damage pipeline: Soul Rot, Charm, Taunt, then Overcharm. */
@Mod.EventBusSubscriber(modid = AscendAilments.MOD_ID)
public final class AilmentEvents {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        HemorrhageTracker.syncDisplay(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HemorrhageTracker.hideDisplay(event.getEntity());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        HemorrhageTracker.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        ControlEffectHelper.handleLivingTick(event.getEntity());
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!event.isCanceled() && event.getEntity().hasEffect(ModEffects.SOUL_ROT.get()))
            event.setAmount((float) (event.getAmount() * AilmentsConfig.value(AilmentsConfig.SOUL_ROT_HEALING)));
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity living && !(living instanceof Player)
                && living.hasEffect(ModEffects.CHARM.get())
                && ControlEffectHelper.isOwnCharmSource(living, event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        LivingEntity taunter = mob.hasEffect(ModEffects.TAUNT.get())
                && !MentalControlUtil.isMentalControlResistant(mob)
                ? ControlEffectHelper.validTauntSource(mob) : null;
        if (taunter != null) {
            if (event.getNewTarget() != taunter) event.setNewTarget(taunter);
            return;
        }
        if (mob.hasEffect(ModEffects.CHARM.get()) && event.getNewTarget() != null
                && ControlEffectHelper.isOwnCharmSource(mob, event.getNewTarget())) event.setNewTarget(null);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        LivingEntity victim = event.getEntity();
        DamageSource damageSource = event.getSource();
        Entity causing = damageSource.getEntity();
        LivingEntity attacker = causing instanceof LivingEntity living ? living : null;
        float amount = event.getAmount();

        // 1. Victim vulnerability.
        if (victim.hasEffect(ModEffects.SOUL_ROT.get()))
            amount *= (float) AilmentsConfig.value(AilmentsConfig.SOUL_ROT_DAMAGE_TAKEN);

        // 2. Charm victim and attacker rules.
        if (attacker != null && !(victim instanceof Player) && victim.hasEffect(ModEffects.CHARM.get())
                && ControlEffectHelper.isOwnCharmSource(victim, attacker))
            amount *= (float) AilmentsConfig.value(AilmentsConfig.CHARM_VICTIM_BONUS);
        if (attacker != null && attacker.hasEffect(ModEffects.CHARM.get())) {
            amount *= (float) (ControlEffectHelper.isOwnCharmSource(attacker, victim)
                    ? AilmentsConfig.value(AilmentsConfig.CHARM_SOURCE_PENALTY)
                    : AilmentsConfig.value(AilmentsConfig.CHARM_OTHER_BONUS));
        }

        // 3. Taunt rules; each logically distinct rule is applied once.
        if (victim.hasEffect(ModEffects.TAUNT.get()))
            amount *= (float) AilmentsConfig.value(AilmentsConfig.TAUNT_VICTIM_MULTIPLIER);
        if (attacker != null && attacker.hasEffect(ModEffects.TAUNT.get())) {
            LivingEntity taunter = EffectSourceUtil.getSource(attacker, EffectSourceUtil.TAUNT);
            if (taunter != null && taunter.getUUID().equals(victim.getUUID()))
                amount *= (float) AilmentsConfig.value(AilmentsConfig.TAUNT_SOURCE_MULTIPLIER);
        }

        // 4. Snapshot pre-hit Charm before Overcharm refreshes it.
        if (attacker != null && attacker != victim && attacker.hasEffect(ModEffects.OVERCHARM.get())) {
            boolean alreadyCharmedByAttacker = victim.hasEffect(ModEffects.CHARM.get())
                    && ControlEffectHelper.isOwnCharmSource(victim, attacker);
            boolean directPlayerMelee = attacker instanceof Player && damageSource.getDirectEntity() == attacker;
            if (alreadyCharmedByAttacker && directPlayerMelee)
                amount *= (float) AilmentsConfig.value(AilmentsConfig.OVERCHARM_DAMAGE_BONUS);
            AilmentApi.applyCharm(victim, attacker,
                    AilmentsConfig.value(AilmentsConfig.OVERCHARM_CHARM_DURATION));
        }

        if (attacker != null && attacker != victim && amount > 0
                && damageSource.getDirectEntity() == attacker)
            applyTaggedWeaponAilments(attacker.getMainHandItem(), victim, attacker);
        event.setAmount(amount);
    }

    private static void applyTaggedWeaponAilments(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (stack.isEmpty()) return;
        if (isAutomaticBleedWeapon(stack) && attacker.getRandom().nextDouble()
                < AilmentsConfig.value(AilmentsConfig.AUTOMATIC_WEAPON_BLEED_CHANCE))
            AilmentApi.applyBleed(victim, attacker);
        if (stack.is(ModItemTags.SOUL_ROT_ON_HIT)) AilmentApi.applySoulRot(victim, attacker);
        if (stack.is(ModItemTags.FRACTURE_ON_HIT)) AilmentApi.applyFracture(victim, attacker);

        int duration = AilmentsConfig.value(AilmentsConfig.TAGGED_WEAPON_EFFECT_DURATION);
        if (stack.is(ModItemTags.FEAR_ON_HIT)) AilmentApi.applyFear(victim, attacker, duration);
        if (stack.is(ModItemTags.CHARM_ON_HIT)) AilmentApi.applyCharm(victim, attacker, duration);
        if (stack.is(ModItemTags.TAUNT_ON_HIT)) AilmentApi.applyTaunt(victim, attacker, duration, 0);
        if (stack.is(ModItemTags.OVERCHARM_ON_HIT)) AilmentApi.applyOvercharm(victim, attacker, duration);
        if (stack.is(ModItemTags.WINDED_ON_HIT)) AilmentApi.applyWinded(victim, attacker, duration);
    }

    private static boolean isAutomaticBleedWeapon(ItemStack stack) {
        if (!AilmentsConfig.value(AilmentsConfig.AUTOMATIC_WEAPON_BLEED) || stack.isEmpty()) return false;
        if (stack.is(ModItemTags.BLEED_ON_HIT)) return true;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || id.getNamespace().equals("minecraft")) return false;
        String path = id.getPath().toLowerCase(Locale.ROOT);
        return AilmentsConfig.weaponBleedKeywords().stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(path::contains);
    }
    private AilmentEvents() {}
}
