# Ascend: Ailments

Forge 1.20.1 library mod providing Soul Rot, Bleed, Fracture, Fear, Charm, Taunt, Overcharm, and Winded.
The mod ID is `ascend_ailments`; Ascend is optional.

## Public API

Normal callers should use `net.fretux.ailments.api.AilmentApi`, for example:

```java
AilmentApi.applyBleed(target, source);
AilmentApi.applySoulRot(target, source, true);
AilmentApi.applyFear(target, source, 100);
AilmentApi.applyCharm(target, source, 100);
AilmentApi.applyTaunt(target, source, 100, 0);
AilmentApi.applyOvercharm(target, source, 200);
AilmentApi.applyWinded(target, source, 60);
AilmentApi.applyFracture(target, source, 160);
```

Skill nodes, modded weapons, and other data-driven callers can use the uniform application API:

```java
boolean applied = AilmentApi.applyEffect(target, source, AilmentType.FEAR, 100, 0);

int appliedCount = AilmentApi.applyEffects(target, source,
        AilmentApplication.stack(AilmentType.BLEED),
        AilmentApplication.timed(AilmentType.WINDED, 60));
```

`applyEffects` attempts every descriptor in order and returns how many were accepted. Use `stack` for configured
Bleed/Soul Rot stacking and `timed` for an explicit duration and amplifier. `AilmentApi.getEffect(type)` exposes the
registered `MobEffect` when an integration needs to inspect an active effect without depending on registry internals.

Other mods can use the same source-safe machinery for their own registered effects:

```java
MobEffect frost = MyEffects.FROST.get();

// Raw duration, authoritative merging, real Forge source, and persistent latest-source tracking.
AilmentApi.applyEffect(target, source, frost, 100, 0);

// Add one stack per call, cap at 4 stacks, and refresh to 100 ticks.
AilmentApi.applyStackingEffect(target, source, frost, 100, 4);

// Add one stack, extend remaining duration, and Arcane-scale each added duration chunk once.
AilmentApi.applyStackingEffect(target, source, frost, 100, 4, true, true);

LivingEntity loadedSource = AilmentApi.getEffectSource(target, frost);
UUID sourceId = AilmentApi.getEffectSourceUuid(target, frost);
```

`applyScaledEffect` provides the non-stacking Arcane-scaled variant. Source-aware applications reject infinite or
higher-amplifier active effects instead of creating vanilla hidden fallbacks, ensuring that the visible effect and its
single tracked owner cannot diverge. Generic applications are server-only, reject invalid or cross-dimension sources,
and honor team/friendly-fire rules for harmful player-on-player effects. Source
metadata is keyed by the external effect's registry ID and is cleared lazily when queried after the effect expires;
`clearEffectSource` can clear it explicitly without removing the effect.

Ordinary API applications are not rate-limited. Automatic combat integrations that need the configured PvP proc
cooldown should use `applyProcEffect` with either an `AilmentApplication` descriptor or explicit type/duration values.

Fracture stacks up to three times. Each stack removes 2 armor and 1 armor toughness; reapplication adds a stack and
refreshes its duration. Arcane scales that refreshed duration but never changes the fixed attribute penalties.

These methods reject invalid targets, enforce PvP and immunity rules, scale duration once at application,
and persist source/potency data. Query methods expose stacks, loaded sources, Bleed immunity, and mental-control
resistance/immunity. Source queries return `null` when the UUID cannot currently be resolved.

Soul Rot's optional stack-duration mode follows the explicit balance table: amplifier 0 through 4 receives
160/180/200/220/240 base ticks (or the configured base plus 20 ticks per amplifier). This resolves the prose
formula/table mismatch in favor of the listed gameplay results.

At two or more Bleed stacks, every successful Bleed damage tick builds the victim's Hemorrhage meter. Fill uses the
Bleed source's application-time Arcane potency snapshot. At the configured threshold, the meter resets to zero and
deals Bleed damage equal to 20% of the victim's maximum health by default. Integrations and HUD implementations can
read `AilmentApi.getHemorrhageProgress(target)` or the normalized `getHemorrhageFraction(target)`. Minimum stacks,
threshold, fill per tick, burst health fraction, and the synchronized player-victim HUD meter are configurable in the
`bleed` section.

## Damage ordering

The single `LivingHurtEvent` pipeline applies modifiers in this order:

1. Soul Rot victim vulnerability
2. Charm victim/attacker rules
3. Taunt victim/attacker rules
4. Overcharm's pre-hit Charm snapshot and direct-player-melee bonus

Each applicable multiplier is applied once. The Overcharm snapshot happens before Charm is refreshed, so its first
hit has no pre-existing-Charm bonus. For simultaneous control effects, active Fear takes precedence over Taunt, and
Taunt takes precedence over Charm's prohibition against attacking its owner.

## Optional Ascend integration

`compat.AscendCompat` checks for mod ID `ascend` and reflection-isolates its player-stats capability. Missing classes,
missing capabilities, non-player sources, and absent/unloaded source data all fall back to 1.0x. An incompatible API
is logged and cached instead of retrying reflection during every application. DOT potency is snapshotted on
application so source unloading does not alter an active effect.

## Datapack extension

Entity type tags:

- `ascend_ailments:bleed_immune`
- `ascend_ailments:soulless` (immune to Soul Rot)
- `ascend_ailments:sturdy` (immune to Fracture)
- `ascend_ailments:mental_control_resistant`
- `ascend_ailments:mental_control_immune`

Vanilla undead are included in `soulless`, and any vanilla or modded entity classified with `MobType.UNDEAD` is
also rejected independently of the tag. `sturdy` includes iron golems by default. Non-player undead remain naturally
Bleed-immune; `bleed_immune` takes precedence even for players and also prevents and clears Hemorrhage buildup.
Integrations can query `AilmentApi.canSoulRot`, `canFracture`, and `canBleed` before presenting or applying abilities.

Item tags:

- `ascend_ailments:bleed_on_hit`
- `ascend_ailments:soul_rot_on_hit`
- `ascend_ailments:fracture_on_hit`
- `ascend_ailments:fear_on_hit`
- `ascend_ailments:charm_on_hit`
- `ascend_ailments:taunt_on_hit`
- `ascend_ailments:overcharm_on_hit`
- `ascend_ailments:winded_on_hit`

Direct melee hits with items in these tags apply their corresponding ailment. Soul Rot, Bleed, and Fracture use
their configured stack/duration behavior; the other tags use `taggedWeaponEffectDurationTicks`. Bleed also applies to non-vanilla item
registry paths containing a configured katana-family keyword. Automatic application, proc chance, matching keywords,
and the PvP proc cooldown are configurable under `integration` in `ascend-ailments-server.toml`. PvP cooldowns are
tracked per attacking player and ailment; their configured minimum is 60 ticks (3 seconds).

## Admin commands

`/ascendailments apply ...` supports all seven effects and `/ascendailments clear <target>` removes the library's
effects and metadata. Permission level 2 is required. Durations are ticks; amplifier arguments use vanilla's
zero-based convention.
