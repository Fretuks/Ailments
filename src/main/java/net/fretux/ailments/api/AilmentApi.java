package net.fretux.ailments.api;

import net.fretux.ailments.config.AilmentsConfig;
import net.fretux.ailments.compat.AscendCompat;
import net.fretux.ailments.registry.ModEffects;
import net.fretux.ailments.util.EffectSourceUtil;
import net.fretux.ailments.util.MentalControlUtil;
import net.fretux.ailments.util.ModEntityTypeTags;
import net.fretux.ailments.util.HemorrhageTracker;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/** Stable, source-aware entry point for applying and querying Ascend: Ailments effects. */
public final class AilmentApi {
    /**
     * Applies one explicit ailment through the same validation, immunity, scaling, and source tracking used by
     * the effect-specific methods. This is the primary entry point for skill engines and item integrations.
     */
    public static boolean applyEffect(LivingEntity target, @Nullable LivingEntity source, AilmentType type,
                                      int requestedTicks, int amplifier) {
        Objects.requireNonNull(type, "type");
        if (requestedTicks < 1 || amplifier < 0) return false;
        return switch (type) {
            case SOUL_ROT -> applySoulRot(target, source, requestedTicks, amplifier);
            case BLEED -> applyBleed(target, source, requestedTicks, amplifier);
            case FRACTURE -> applyFracture(target, source, requestedTicks, amplifier);
            case FEAR -> applyFear(target, source, requestedTicks);
            case CHARM -> applyCharm(target, source, requestedTicks);
            case TAUNT -> applyTaunt(target, source, requestedTicks, amplifier);
            case OVERCHARM -> applyOvercharm(target, source, requestedTicks);
            case WINDED -> applyWinded(target, source, requestedTicks);
        };
    }

    /** Applies one explicit amplifier-zero ailment. */
    public static boolean applyEffect(LivingEntity target, @Nullable LivingEntity source, AilmentType type,
                                      int requestedTicks) {
        return applyEffect(target, source, type, requestedTicks, 0);
    }

    /** Applies one reusable application descriptor. */
    public static boolean applyEffect(LivingEntity target, @Nullable LivingEntity source,
                                      AilmentApplication application) {
        Objects.requireNonNull(application, "application");
        if (application.isStacking()) {
            return switch (application.type()) {
                case BLEED -> applyBleed(target, source);
                case SOUL_ROT -> applySoulRot(target, source);
                case FRACTURE -> applyFracture(target, source);
                default -> false;
            };
        }
        return applyEffect(target, source, application.type(), application.durationTicks(), application.amplifier());
    }

    /**
     * Applies one or more descriptors in order and returns the number accepted by their ailment rules.
     * A rejected effect does not prevent later effects from being attempted.
     */
    public static int applyEffects(LivingEntity target, @Nullable LivingEntity source,
                                   AilmentApplication... applications) {
        Objects.requireNonNull(applications, "applications");
        int applied = 0;
        for (AilmentApplication application : applications) {
            if (applyEffect(target, source, application)) applied++;
        }
        return applied;
    }

    /** Iterable overload for data-driven integrations. */
    public static int applyEffects(LivingEntity target, @Nullable LivingEntity source,
                                   Iterable<AilmentApplication> applications) {
        Objects.requireNonNull(applications, "applications");
        int applied = 0;
        for (AilmentApplication application : applications) {
            if (applyEffect(target, source, application)) applied++;
        }
        return applied;
    }

    /**
     * Applies an effect owned by another mod with standard Forge source propagation and persistent source tracking.
     * The requested duration is used exactly and is not Arcane-scaled. Vanilla effect-merging rules apply.
     */
    public static boolean applyEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                      int requestedTicks, int amplifier) {
        return applyExternalEffect(target, source, effect, requestedTicks, amplifier, false);
    }

    /** Applies an amplifier-zero external effect with source propagation and persistent source tracking. */
    public static boolean applyEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                      int requestedTicks) {
        return applyEffect(target, source, effect, requestedTicks, 0);
    }

    /**
     * Applies an external effect and scales its requested duration once using the optional Ascend Arcane curve.
     * Use this overload only when the owning mod wants Ailments to be the duration-scaling authority.
     */
    public static boolean applyScaledEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                            int requestedTicks, int amplifier) {
        return applyExternalEffect(target, source, effect, requestedTicks, amplifier, true);
    }

    /** Applies an amplifier-zero external effect with one-time Arcane duration scaling. */
    public static boolean applyScaledEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                            int requestedTicks) {
        return applyScaledEffect(target, source, effect, requestedTicks, 0);
    }

    /** Adds one amplifier stack to an external effect, refreshes its duration, and tracks the latest source. */
    public static boolean applyStackingEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                              int requestedTicks, int maximumStacks) {
        return applyStackingEffect(target, source, effect, requestedTicks, maximumStacks, false, false);
    }

    /**
     * Adds one stack to an external effect with configurable duration behavior and optional Arcane scaling.
     * {@code maximumStacks} uses a human-facing one-based count; the applied amplifier is capped at one less.
     * When {@code extendDuration} is true, the scaled requested duration is added to the remaining duration.
     */
    public static boolean applyStackingEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                              int requestedTicks, int maximumStacks, boolean extendDuration,
                                              boolean scaleDurationWithArcane) {
        Objects.requireNonNull(effect, "effect");
        if (!externalApplicationValid(target, source, effect, requestedTicks, 0)
                || maximumStacks < 1 || maximumStacks > 256) return false;
        MobEffectInstance current = target.getEffect(effect);
        if (current != null && current.isInfiniteDuration()) return false;
        int amplifier = current == null ? 0 : current.getAmplifier() >= maximumStacks - 1
                ? maximumStacks - 1 : current.getAmplifier() + 1;
        int duration = scaleDurationWithArcane ? AilmentScaling.scaleDuration(requestedTicks, source) : requestedTicks;
        if (extendDuration && current != null) duration = safeAdd(current.getDuration(), duration);
        boolean applied = addStackingEffect(target, source, effect, duration, amplifier, false, true, true);
        if (applied) trackExternalSource(target, source, effect);
        return applied;
    }

    /** Returns the UUID of the latest successful source tracked for an active external effect. */
    @Nullable public static UUID getEffectSourceUuid(LivingEntity target, MobEffect effect) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(effect, "effect");
        String key = externalSourceKey(effect);
        if (key == null) return null;
        if (!target.hasEffect(effect)) {
            EffectSourceUtil.clear(target, key);
            return null;
        }
        return EffectSourceUtil.getSourceUuid(target, key);
    }

    /** Returns the latest tracked source when it is currently loaded, or {@code null}. */
    @Nullable public static LivingEntity getEffectSource(LivingEntity target, MobEffect effect) {
        if (getEffectSourceUuid(target, effect) == null) return null;
        return EffectSourceUtil.getSource(target, externalSourceKey(effect));
    }

    /** Resolves the latest tracked source only when it is loaded in the target's current level. */
    @Nullable public static LivingEntity getEffectSourceInLevel(LivingEntity target, MobEffect effect) {
        if (getEffectSourceUuid(target, effect) == null) return null;
        return EffectSourceUtil.getSourceInLevel(target, externalSourceKey(effect));
    }

    /** Clears source metadata created by the generic external-effect API without removing the effect. */
    public static void clearEffectSource(LivingEntity target, MobEffect effect) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(effect, "effect");
        String key = externalSourceKey(effect);
        if (key != null) EffectSourceUtil.clear(target, key);
    }

    /** Returns the registered MobEffect represented by a stable public ailment identifier. */
    public static MobEffect getEffect(AilmentType type) {
        Objects.requireNonNull(type, "type");
        return switch (type) {
            case SOUL_ROT -> ModEffects.SOUL_ROT.get();
            case BLEED -> ModEffects.BLEED.get();
            case FRACTURE -> ModEffects.FRACTURE.get();
            case FEAR -> ModEffects.FEAR.get();
            case CHARM -> ModEffects.CHARM.get();
            case TAUNT -> ModEffects.TAUNT.get();
            case OVERCHARM -> ModEffects.OVERCHARM.get();
            case WINDED -> ModEffects.WINDED.get();
        };
    }

    /** Applies one Soul Rot stack. Extra stacks use 180/200/220/240 tick base durations. */
    public static boolean applySoulRot(LivingEntity target, @Nullable LivingEntity source) {
        return applySoulRot(target, source, true);
    }

    /** Applies one Soul Rot stack; disabling extension always refreshes the configured base duration. */
    public static boolean applySoulRot(LivingEntity target, @Nullable LivingEntity source,
                                       boolean extendWithStacks) {
        if (!serverValid(target) || !canSoulRot(target)
                || !pvpProcReady(target, source, AilmentType.SOUL_ROT)) return false;
        MobEffect effect = ModEffects.SOUL_ROT.get();
        MobEffectInstance current = target.getEffect(effect);
        int currentStacks = current == null ? 0 : current.getAmplifier() + 1;
        int maxStacks = AilmentsConfig.value(AilmentsConfig.SOUL_ROT_MAX_STACKS);
        int stacks = Math.min(currentStacks + 1, maxStacks);
        int amplifier = stacks - 1;
        int base = AilmentsConfig.value(AilmentsConfig.SOUL_ROT_DURATION);
        if (extendWithStacks) base = safeAdd(base, amplifier * 20);
        int duration = AilmentScaling.scaleDuration(base, source);
        boolean applied = addStackingEffect(target, source, effect, duration, amplifier, true);
        if (applied) {
            EffectSourceUtil.setSource(target, EffectSourceUtil.SOUL_ROT, source);
            EffectSourceUtil.setPotency(target, EffectSourceUtil.SOUL_ROT,
                    AilmentScaling.getPotencyMultiplier(source, AilmentType.SOUL_ROT));
        }
        return finishApplication(applied, target, source, AilmentType.SOUL_ROT);
    }

    /** Applies an explicit, capped Soul Rot amplifier and source-scaled requested duration. */
    public static boolean applySoulRot(LivingEntity target, @Nullable LivingEntity source,
                                       int requestedTicks, int amplifier) {
        if (!serverValid(target) || !canSoulRot(target)
                || !pvpProcReady(target, source, AilmentType.SOUL_ROT)) return false;
        int capped = Math.min(Math.max(0, amplifier),
                AilmentsConfig.value(AilmentsConfig.SOUL_ROT_MAX_STACKS) - 1);
        return finishApplication(applyDot(target, source, ModEffects.SOUL_ROT.get(), EffectSourceUtil.SOUL_ROT,
                requestedTicks, capped, AilmentType.SOUL_ROT), target, source, AilmentType.SOUL_ROT);
    }

    /** Applies or adds one Bleed stack, returning false for immune targets. */
    public static boolean applyBleed(LivingEntity target, @Nullable LivingEntity source) {
        if (!serverValid(target) || !canBleed(target) || !pvpProcReady(target, source, AilmentType.BLEED)) return false;
        MobEffect effect = ModEffects.BLEED.get();
        MobEffectInstance current = target.getEffect(effect);
        int amplifier = current == null ? 0 : Math.min(current.getAmplifier() + 1,
                AilmentsConfig.value(AilmentsConfig.BLEED_MAX_AMPLIFIER));
        int base = amplifier <= 0 ? AilmentsConfig.value(AilmentsConfig.BLEED_DURATION_0)
                : amplifier == 1 ? AilmentsConfig.value(AilmentsConfig.BLEED_DURATION_1)
                : AilmentsConfig.value(AilmentsConfig.BLEED_DURATION_2);
        int duration = AilmentScaling.scaleDuration(base, source);
        boolean applied = addStackingEffect(target, source, effect, duration, amplifier, true);
        if (applied) {
            EffectSourceUtil.setSource(target, EffectSourceUtil.BLEED, source);
            EffectSourceUtil.setPotency(target, EffectSourceUtil.BLEED,
                    AilmentScaling.getPotencyMultiplier(source, AilmentType.BLEED));
        }
        return finishApplication(applied, target, source, AilmentType.BLEED);
    }

    /** Applies an explicit, capped Bleed amplifier and source-scaled requested duration. */
    public static boolean applyBleed(LivingEntity target, @Nullable LivingEntity source,
                                     int requestedTicks, int amplifier) {
        if (!serverValid(target) || !canBleed(target) || !pvpProcReady(target, source, AilmentType.BLEED)) return false;
        int capped = Math.min(Math.max(0, amplifier),
                AilmentsConfig.value(AilmentsConfig.BLEED_MAX_AMPLIFIER));
        return finishApplication(applyDot(target, source, ModEffects.BLEED.get(), EffectSourceUtil.BLEED,
                requestedTicks, capped, AilmentType.BLEED), target, source, AilmentType.BLEED);
    }

    /** Applies one Fracture stack and refreshes its configured, source-scaled duration. */
    public static boolean applyFracture(LivingEntity target, @Nullable LivingEntity source) {
        return applyFracture(target, source, AilmentsConfig.value(AilmentsConfig.FRACTURE_DURATION), 0);
    }

    /** Applies one Fracture stack and refreshes the source-scaled requested duration. */
    public static boolean applyFracture(LivingEntity target, @Nullable LivingEntity source, int requestedTicks) {
        return applyFracture(target, source, requestedTicks, 0);
    }

    /**
     * Applies or increases Fracture, capped at three stacks. The requested amplifier acts as a minimum, while an
     * existing Fracture always gains one stack. Arcane scales only duration; attribute penalties remain fixed.
     */
    public static boolean applyFracture(LivingEntity target, @Nullable LivingEntity source,
                                        int requestedTicks, int amplifier) {
        if (!serverValid(target) || !canFracture(target) || requestedTicks < 1 || amplifier < 0
                || !pvpProcReady(target, source, AilmentType.FRACTURE)) return false;
        MobEffect effect = ModEffects.FRACTURE.get();
        MobEffectInstance current = target.getEffect(effect);
        int stackedAmplifier = current == null ? 0 : Math.min(2, current.getAmplifier() + 1);
        int appliedAmplifier = Math.min(2, Math.max(stackedAmplifier, amplifier));
        int duration = AilmentScaling.scaleDuration(requestedTicks, source);
        boolean applied = addStackingEffect(target, source, effect, duration, appliedAmplifier, true);
        if (applied) EffectSourceUtil.setSource(target, EffectSourceUtil.FRACTURE, source);
        return finishApplication(applied, target, source, AilmentType.FRACTURE);
    }

    public static boolean applyFear(LivingEntity target, @Nullable LivingEntity source, int requestedTicks) {
        if (!hostileControlValid(target, source) || MentalControlUtil.isMentalControlImmune(target)
                || target.getMaxHealth() > AilmentsConfig.value(AilmentsConfig.FEAR_IMMUNITY_HEALTH)
                || !pvpProcReady(target, source, AilmentType.FEAR)) return false;
        int duration = AilmentScaling.scaleDuration(requestedTicks, source);
        if (target.getMaxHealth() > AilmentsConfig.value(AilmentsConfig.FEAR_REDUCED_HEALTH)) duration /= 2;
        if (MentalControlUtil.isMentalControlResistant(target))
            duration = MentalControlUtil.resistantDuration(duration, 3);
        if (target instanceof EnderMan) duration = Math.max(1, duration / 2);
        return finishApplication(applySourced(target, source, ModEffects.FEAR.get(), EffectSourceUtil.FEAR,
                Math.max(1, duration), 0), target, source, AilmentType.FEAR);
    }

    public static boolean applyCharm(LivingEntity target, @Nullable LivingEntity source, int requestedTicks) {
        if (!hostileControlValid(target, source) || MentalControlUtil.isMentalControlImmune(target)
                || !pvpProcReady(target, source, AilmentType.CHARM)) return false;
        int duration = AilmentScaling.scaleDuration(requestedTicks, source);
        if (MentalControlUtil.isMentalControlResistant(target))
            duration = MentalControlUtil.resistantDuration(duration, 2);
        return finishApplication(applySourced(target, source, ModEffects.CHARM.get(), EffectSourceUtil.CHARM,
                duration, 0), target, source, AilmentType.CHARM);
    }

    public static boolean applyTaunt(LivingEntity target, @Nullable LivingEntity source, int requestedTicks, int amplifier) {
        if (!hostileControlValid(target, source) || MentalControlUtil.isMentalControlImmune(target)
                || !pvpProcReady(target, source, AilmentType.TAUNT)) return false;
        int duration = AilmentScaling.scaleDuration(requestedTicks, source);
        if (MentalControlUtil.isMentalControlResistant(target))
            duration = MentalControlUtil.resistantDuration(duration, 2);
        return finishApplication(applySourced(target, source, ModEffects.TAUNT.get(), EffectSourceUtil.TAUNT,
                duration, Math.max(0, amplifier)), target, source, AilmentType.TAUNT);
    }

    public static boolean applyOvercharm(LivingEntity target, @Nullable LivingEntity source, int requestedTicks) {
        if (!serverValid(target) || !pvpProcReady(target, source, AilmentType.OVERCHARM)) return false;
        int duration = AilmentScaling.scaleDuration(requestedTicks, source);
        boolean result = addSourcedEffect(target, source, ModEffects.OVERCHARM.get(), duration, 0, true);
        if (result) {
            EffectSourceUtil.setSource(target, EffectSourceUtil.OVERCHARM, source);
        }
        return finishApplication(result, target, source, AilmentType.OVERCHARM);
    }

    public static boolean applyWinded(LivingEntity target, @Nullable LivingEntity source, int requestedTicks) {
        if (!serverValid(target) || !pvpProcReady(target, source, AilmentType.WINDED)) return false;
        int duration = AilmentScaling.scaleDuration(requestedTicks, source);
        return finishApplication(addSourcedEffect(target, source, ModEffects.WINDED.get(), duration, 0, true),
                target, source, AilmentType.WINDED);
    }

    public static boolean canBleed(LivingEntity target) {
        if (target.getType().is(ModEntityTypeTags.BLEED_IMMUNE)) return false;
        return target instanceof Player || target.getMobType() != MobType.UNDEAD;
    }
    /** Returns false for the Soulless tag and for any entity classified as undead by Minecraft or another mod. */
    public static boolean canSoulRot(LivingEntity target) {
        return target.getMobType() != MobType.UNDEAD && !target.getType().is(ModEntityTypeTags.SOULLESS);
    }
    /** Returns false for entity types in the Sturdy tag. */
    public static boolean canFracture(LivingEntity target) {
        return !target.getType().is(ModEntityTypeTags.STURDY);
    }
    public static boolean isMentalControlResistant(LivingEntity target) {
        return MentalControlUtil.isMentalControlResistant(target);
    }
    public static boolean isMentalControlImmune(LivingEntity target) {
        return MentalControlUtil.isMentalControlImmune(target);
    }
    public static int getBleedStacks(LivingEntity target) { return stacks(target, ModEffects.BLEED.get()); }
    public static int getSoulRotStacks(LivingEntity target) { return stacks(target, ModEffects.SOUL_ROT.get()); }
    public static int getFractureStacks(LivingEntity target) { return stacks(target, ModEffects.FRACTURE.get()); }
    /** Returns raw Hemorrhage progress in the configured threshold's units. */
    public static double getHemorrhageProgress(LivingEntity target) {
        return HemorrhageTracker.getProgress(target);
    }
    /** Returns normalized Hemorrhage bar progress from 0.0 through 1.0. */
    public static double getHemorrhageFraction(LivingEntity target) {
        return HemorrhageTracker.getFraction(target);
    }
    public static void clearHemorrhage(LivingEntity target) { HemorrhageTracker.clear(target); }
    @Nullable public static LivingEntity getCharmSource(LivingEntity target) {
        return EffectSourceUtil.getSource(target, EffectSourceUtil.CHARM);
    }
    @Nullable public static LivingEntity getFearSource(LivingEntity target) {
        return EffectSourceUtil.getSource(target, EffectSourceUtil.FEAR);
    }
    @Nullable public static LivingEntity getTauntSource(LivingEntity target) {
        return EffectSourceUtil.getSource(target, EffectSourceUtil.TAUNT);
    }
    @Nullable public static LivingEntity getBleedSource(LivingEntity target) {
        return EffectSourceUtil.getSource(target, EffectSourceUtil.BLEED);
    }
    @Nullable public static LivingEntity getSoulRotSource(LivingEntity target) {
        return EffectSourceUtil.getSource(target, EffectSourceUtil.SOUL_ROT);
    }
    @Nullable public static LivingEntity getFractureSource(LivingEntity target) {
        return EffectSourceUtil.getSource(target, EffectSourceUtil.FRACTURE);
    }

    /** Removes all registered ailments and all associated source/potency metadata. */
    public static void clearAll(LivingEntity target) {
        target.removeEffect(ModEffects.SOUL_ROT.get()); target.removeEffect(ModEffects.BLEED.get());
        target.removeEffect(ModEffects.FEAR.get()); target.removeEffect(ModEffects.CHARM.get());
        target.removeEffect(ModEffects.TAUNT.get()); target.removeEffect(ModEffects.OVERCHARM.get());
        target.removeEffect(ModEffects.WINDED.get());
        target.removeEffect(ModEffects.FRACTURE.get());
        EffectSourceUtil.clear(target, EffectSourceUtil.SOUL_ROT);
        EffectSourceUtil.clear(target, EffectSourceUtil.BLEED);
        EffectSourceUtil.clear(target, EffectSourceUtil.FEAR);
        EffectSourceUtil.clear(target, EffectSourceUtil.CHARM);
        EffectSourceUtil.clear(target, EffectSourceUtil.TAUNT);
        EffectSourceUtil.clear(target, EffectSourceUtil.OVERCHARM);
        EffectSourceUtil.clear(target, EffectSourceUtil.FRACTURE);
        HemorrhageTracker.clear(target);
    }

    private static boolean applySourced(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                         String key, int duration, int amplifier) {
        boolean applied = addSourcedEffect(target, source, effect, Math.max(1, duration), amplifier, true);
        if (applied) EffectSourceUtil.setSource(target, key, source);
        return applied;
    }
    private static boolean applyDot(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                    String key, int requestedTicks, int amplifier, AilmentType type) {
        int duration = AilmentScaling.scaleDuration(requestedTicks, source);
        boolean applied = addSourcedEffect(target, source, effect, duration, amplifier, true);
        if (applied) {
            EffectSourceUtil.setSource(target, key, source);
            EffectSourceUtil.setPotency(target, key, AilmentScaling.getPotencyMultiplier(source, type));
        }
        return applied;
    }
    private static boolean applyExternalEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                               int requestedTicks, int amplifier, boolean scaleDurationWithArcane) {
        Objects.requireNonNull(effect, "effect");
        if (!externalApplicationValid(target, source, effect, requestedTicks, amplifier)) return false;
        int duration = scaleDurationWithArcane ? AilmentScaling.scaleDuration(requestedTicks, source) : requestedTicks;
        boolean applied = addSourcedEffect(target, source, effect, duration, amplifier, false, true, true);
        if (applied) trackExternalSource(target, source, effect);
        return applied;
    }
    private static MobEffectInstance instance(MobEffect effect, int duration, int amplifier, boolean icon) {
        return new MobEffectInstance(effect, duration, amplifier, false, false, icon);
    }
    private static MobEffectInstance instance(MobEffect effect, int duration, int amplifier, boolean ambient,
                                              boolean visible, boolean icon) {
        return new MobEffectInstance(effect, duration, amplifier, ambient, visible, icon);
    }
    private static boolean addSourcedEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                            int duration, int amplifier, boolean icon) {
        return addSourcedEffect(target, source, effect, duration, amplifier, false, false, icon);
    }
    private static boolean addSourcedEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                            int duration, int amplifier, boolean ambient, boolean visible,
                                            boolean icon) {
        boolean applied = target.addEffect(instance(effect, duration, amplifier, ambient, visible, icon), source);
        if (applied && AscendCompat.isAscendLoaded()) {
            // Ascend observes the standard Forge source and scales duration during addEffect. Ailments already
            // applied its source curve, so restore the requested duration to prevent double scaling.
            MobEffectInstance active = target.getEffect(effect);
            if (active != null && active.getAmplifier() == amplifier) active.mapDuration(ignored -> duration);
        }
        return applied;
    }
    private static boolean addStackingEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                             int duration, int amplifier, boolean icon) {
        return addStackingEffect(target, source, effect, duration, amplifier, false, false, icon);
    }
    private static boolean addStackingEffect(LivingEntity target, @Nullable LivingEntity source, MobEffect effect,
                                             int duration, int amplifier, boolean ambient, boolean visible,
                                             boolean icon) {
        MobEffectInstance previous = target.getEffect(effect);
        if (previous == null || previous.getAmplifier() >= amplifier)
            return addSourcedEffect(target, source, effect, duration, amplifier, ambient, visible, icon);

        // Vanilla preserves a lower-amplifier effect as a hidden fallback when upgrading. Stacking applications use
        // one authoritative stack state, so suppress that fallback without emitting a remove/add lifecycle.
        int previousDuration = previous.getDuration();
        previous.mapDuration(ignored -> 0);
        boolean applied = addSourcedEffect(target, source, effect, duration, amplifier, ambient, visible, icon);
        if (!applied && target.getEffect(effect) == previous)
            previous.mapDuration(ignored -> previousDuration);
        return applied;
    }
    private static boolean externalApplicationValid(LivingEntity target, @Nullable LivingEntity source,
                                                    MobEffect effect, int requestedTicks, int amplifier) {
        if (!serverValid(target) || requestedTicks < 1 || amplifier < 0) return false;
        if ((effect == ModEffects.BLEED.get() && !canBleed(target))
                || (effect == ModEffects.SOUL_ROT.get() && !canSoulRot(target))
                || (effect == ModEffects.FRACTURE.get() && !canFracture(target))) return false;
        if (source != null && (!source.isAlive() || source.isSpectator()
                || source.level().isClientSide || source.level() != target.level())) return false;
        return effect.getCategory() != MobEffectCategory.HARMFUL
                || !(source instanceof Player sourcePlayer && target instanceof Player targetPlayer)
                || source == target || sourcePlayer.canHarmPlayer(targetPlayer);
    }
    private static void trackExternalSource(LivingEntity target, @Nullable LivingEntity source, MobEffect effect) {
        String key = externalSourceKey(effect);
        if (key != null) EffectSourceUtil.setSource(target, key, source);
    }
    @Nullable private static String externalSourceKey(MobEffect effect) {
        var id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return id == null ? null : "ascend_ailments.external." + id;
    }
    private static boolean serverValid(LivingEntity target) {
        return target != null && !target.level().isClientSide && target.isAlive()
                && (!(target instanceof Player player) || !player.isSpectator());
    }
    private static boolean hostileControlValid(LivingEntity target, @Nullable LivingEntity source) {
        if (!serverValid(target) || source == null || !source.isAlive()
                || source.level().isClientSide || source.isSpectator()) return false;
        return !(source instanceof Player sourcePlayer && target instanceof Player targetPlayer)
                || sourcePlayer.canHarmPlayer(targetPlayer);
    }
    private static int stacks(LivingEntity target, MobEffect effect) {
        MobEffectInstance active = target.getEffect(effect);
        return active == null ? 0 : active.getAmplifier() + 1;
    }
    private static int safeAdd(int a, int b) {
        return (int) Math.min(Integer.MAX_VALUE, (long) a + b);
    }
    private static boolean pvpProcReady(LivingEntity target, @Nullable LivingEntity source, AilmentType type) {
        if (source != null && source.level() != target.level()) return false;
        if (!(source instanceof Player sourcePlayer) || !(target instanceof Player targetPlayer) || source == target)
            return true;
        if (!sourcePlayer.canHarmPlayer(targetPlayer)) return false;
        String key = pvpCooldownKey(type);
        long now = source.level().getGameTime();
        return !source.getPersistentData().contains(key)
                || now - source.getPersistentData().getLong(key)
                >= AilmentsConfig.value(AilmentsConfig.PVP_PROC_COOLDOWN_TICKS);
    }
    private static boolean finishApplication(boolean applied, LivingEntity target,
                                             @Nullable LivingEntity source, AilmentType type) {
        if (applied && source instanceof Player && target instanceof Player && source != target) {
            source.getPersistentData().putLong(pvpCooldownKey(type), source.level().getGameTime());
        }
        return applied;
    }
    private static String pvpCooldownKey(AilmentType type) {
        return "ascend_ailments.pvp_proc." + type.name().toLowerCase(java.util.Locale.ROOT);
    }
    private AilmentApi() {}
}
