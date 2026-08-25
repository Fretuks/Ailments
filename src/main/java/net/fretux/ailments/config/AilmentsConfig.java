package net.fretux.ailments.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class AilmentsConfig {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue ARCANE_DURATION_BONUS;
    public static final ForgeConfigSpec.DoubleValue SOUL_ROT_ARCANE_POTENCY;
    public static final ForgeConfigSpec.DoubleValue BLEED_ARCANE_POTENCY;
    public static final ForgeConfigSpec.IntValue SOUL_ROT_DURATION, SOUL_ROT_MAX_STACKS;
    public static final ForgeConfigSpec.DoubleValue SOUL_ROT_EXHAUSTION_BASE, SOUL_ROT_EXHAUSTION_PER_AMP,
            SOUL_ROT_DAMAGE_TAKEN, SOUL_ROT_HEALING;
    public static final ForgeConfigSpec.IntValue BLEED_DURATION_0, BLEED_DURATION_1, BLEED_DURATION_2,
            BLEED_INTERVAL_0, BLEED_INTERVAL_1, BLEED_INTERVAL_2, BLEED_MAX_AMPLIFIER;
    public static final ForgeConfigSpec.DoubleValue BLEED_DAMAGE_0, BLEED_DAMAGE_1, BLEED_DAMAGE_2;
    public static final ForgeConfigSpec.IntValue HEMORRHAGE_MINIMUM_STACKS;
    public static final ForgeConfigSpec.DoubleValue HEMORRHAGE_THRESHOLD, HEMORRHAGE_FILL_PER_TICK,
            HEMORRHAGE_MAX_HEALTH_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue HEMORRHAGE_SHOW_PLAYER_BAR;
    public static final ForgeConfigSpec.DoubleValue FEAR_IMMUNITY_HEALTH, FEAR_REDUCED_HEALTH,
            FEAR_FLEE_SPEED, FEAR_FLEE_DISTANCE, FEAR_PLAYER_PUSH;
    public static final ForgeConfigSpec.IntValue FRACTURE_DURATION;
    public static final ForgeConfigSpec.DoubleValue CHARM_VICTIM_BONUS, CHARM_SOURCE_PENALTY, CHARM_OTHER_BONUS;
    public static final ForgeConfigSpec.DoubleValue TAUNT_VICTIM_MULTIPLIER, TAUNT_SOURCE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue OVERCHARM_CHARM_DURATION;
    public static final ForgeConfigSpec.DoubleValue OVERCHARM_DAMAGE_BONUS, WINDED_MOVEMENT_REDUCTION,
            WINDED_ATTACK_REDUCTION;
    public static final ForgeConfigSpec.IntValue PVP_PROC_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue AUTOMATIC_WEAPON_BLEED;
    public static final ForgeConfigSpec.DoubleValue AUTOMATIC_WEAPON_BLEED_CHANCE;
    public static final ForgeConfigSpec.IntValue TAGGED_WEAPON_EFFECT_DURATION;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AUTOMATIC_WEAPON_BLEED_KEYWORDS;

    static {
        B.push("arcane");
        ARCANE_DURATION_BONUS = d("durationBonusAt100", 0.30, 0, 5);
        SOUL_ROT_ARCANE_POTENCY = d("soulRotExhaustionBonusAt100", 0.20, 0, 5);
        BLEED_ARCANE_POTENCY = d("bleedDamageBonusAt100", 0.20, 0, 5);
        B.pop().push("soulRot");
        SOUL_ROT_DURATION = i("baseDurationTicks", 160, 1, Integer.MAX_VALUE);
        SOUL_ROT_MAX_STACKS = i("maximumStacks", 5, 1, 5);
        SOUL_ROT_EXHAUSTION_BASE = d("exhaustionBase", 1.2, 0, 1000);
        SOUL_ROT_EXHAUSTION_PER_AMP = d("exhaustionPerAmplifier", 0.4, 0, 1000);
        SOUL_ROT_DAMAGE_TAKEN = d("damageTakenMultiplier", 1.10, 0, 100);
        SOUL_ROT_HEALING = d("healingMultiplier", 0.25, 0, 100);
        B.pop().push("bleed");
        BLEED_DURATION_0 = i("durationStack1", 100, 1, Integer.MAX_VALUE);
        BLEED_DURATION_1 = i("durationStack2", 120, 1, Integer.MAX_VALUE);
        BLEED_DURATION_2 = i("durationStack3", 140, 1, Integer.MAX_VALUE);
        BLEED_INTERVAL_0 = i("intervalStack1", 30, 1, 1200);
        BLEED_INTERVAL_1 = i("intervalStack2", 25, 1, 1200);
        BLEED_INTERVAL_2 = i("intervalStack3", 20, 1, 1200);
        BLEED_DAMAGE_0 = d("damageStack1", 1, 0, 1000);
        BLEED_DAMAGE_1 = d("damageStack2", 1, 0, 1000);
        BLEED_DAMAGE_2 = d("damageStack3", 2, 0, 1000);
        BLEED_MAX_AMPLIFIER = i("maximumAmplifier", 2, 0, 2);
        HEMORRHAGE_MINIMUM_STACKS = B.comment(
                "Bleed stacks required before damaging Bleed ticks build Hemorrhage.")
                .defineInRange("hemorrhageMinimumStacks", 2, 2, 3);
        HEMORRHAGE_THRESHOLD = B.comment("Progress required to trigger Hemorrhage.")
                .defineInRange("hemorrhageThreshold", 100.0, 1.0, 1000000.0);
        HEMORRHAGE_FILL_PER_TICK = B.comment(
                "Base progress added by each successful Bleed damage tick; snapshotted Arcane potency multiplies it.")
                .defineInRange("hemorrhageFillPerBleedTick", 10.0, 0.0, 1000000.0);
        HEMORRHAGE_MAX_HEALTH_DAMAGE = B.comment(
                "Fraction of the victim's maximum health dealt as Bleed damage when Hemorrhage fills.")
                .defineInRange("hemorrhageMaxHealthDamage", 0.20, 0.0, 100.0);
        HEMORRHAGE_SHOW_PLAYER_BAR = B.comment(
                "Show player victims their current Hemorrhage progress as a compact HUD meter.")
                .define("showHemorrhagePlayerBar", true);
        B.pop().push("fracture");
        FRACTURE_DURATION = B.comment("Base duration refreshed whenever Fracture is reapplied.")
                .defineInRange("baseDurationTicks", 160, 1, Integer.MAX_VALUE);
        B.pop().push("fear");
        FEAR_IMMUNITY_HEALTH = d("immunityHealthThreshold", 150, 1, 1000000);
        FEAR_REDUCED_HEALTH = d("reducedDurationHealthThreshold", 50, 0, 1000000);
        FEAR_FLEE_SPEED = d("fleeSpeed", 1.25, 0, 100);
        FEAR_FLEE_DISTANCE = d("fleeDistance", 6, 0, 1000);
        FEAR_PLAYER_PUSH = d("playerPushStrength", 0.18, 0, 10);
        B.pop().push("charm");
        CHARM_VICTIM_BONUS = d("charmedVictimMultiplier", 1.10, 0, 100);
        CHARM_SOURCE_PENALTY = d("attackerVsSourceMultiplier", 0.85, 0, 100);
        CHARM_OTHER_BONUS = d("attackerVsOthersMultiplier", 1.15, 0, 100);
        B.pop().push("taunt");
        TAUNT_VICTIM_MULTIPLIER = d("victimDamageMultiplier", 1.20, 0, 100);
        TAUNT_SOURCE_MULTIPLIER = d("attackerVsSourceMultiplier", 1.20, 0, 100);
        B.pop().push("overcharm");
        OVERCHARM_CHARM_DURATION = i("charmDurationTicks", 100, 1, Integer.MAX_VALUE);
        OVERCHARM_DAMAGE_BONUS = d("preCharmedMeleeMultiplier", 1.10, 0, 100);
        B.pop().push("winded");
        WINDED_MOVEMENT_REDUCTION = d("movementReduction", 0.25, 0, 1);
        WINDED_ATTACK_REDUCTION = d("attackSpeedReduction", 0.25, 0, 1);
        B.pop().push("integration");
        PVP_PROC_COOLDOWN_TICKS = B.comment(
                "Minimum ticks between successful procs of the same ailment by one player against other players.",
                "This cannot be configured below 60 ticks (3 seconds).")
                .defineInRange("pvpProcCooldownTicks", 60, 60, Integer.MAX_VALUE);
        AUTOMATIC_WEAPON_BLEED = B.comment(
                "Automatically add one Bleed stack on direct melee hits with matching modded weapons.")
                .define("automaticWeaponBleed", true);
        AUTOMATIC_WEAPON_BLEED_CHANCE = B.comment(
                "Chance per eligible melee hit to apply automatic weapon Bleed.")
                .defineInRange("automaticWeaponBleedChance", 1.0, 0.0, 1.0);
        TAGGED_WEAPON_EFFECT_DURATION = B.comment(
                "Base duration used by fear, charm, taunt, overcharm, and winded on-hit item tags.",
                "Soul Rot, Bleed, and Fracture continue to use their ailment-specific configured durations.")
                .defineInRange("taggedWeaponEffectDurationTicks", 100, 1, Integer.MAX_VALUE);
        AUTOMATIC_WEAPON_BLEED_KEYWORDS = B.comment(
                "Case-insensitive registry-path fragments used to identify modded katana-like weapons.",
                "Items in #ascend_ailments:bleed_on_hit are eligible independently of this list.")
                .defineListAllowEmpty("automaticWeaponBleedKeywords",
                        List.of("katana", "wakizashi", "nodachi", "odachi", "uchigatana", "tachi"),
                        value -> value instanceof String keyword && !keyword.isBlank());
        B.pop();
    }

    public static final ForgeConfigSpec SPEC = B.build();
    private static ForgeConfigSpec.DoubleValue d(String key, double value, double min, double max) {
        return B.defineInRange(key, value, min, max);
    }
    private static ForgeConfigSpec.IntValue i(String key, int value, int min, int max) {
        return B.defineInRange(key, value, min, max);
    }
    public static double value(ForgeConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
    public static int value(ForgeConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
    public static boolean value(ForgeConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
    public static List<? extends String> weaponBleedKeywords() {
        return SPEC.isLoaded() ? AUTOMATIC_WEAPON_BLEED_KEYWORDS.get()
                : AUTOMATIC_WEAPON_BLEED_KEYWORDS.getDefault();
    }
    private AilmentsConfig() {}
}
