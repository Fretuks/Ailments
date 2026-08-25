package net.fretux.ailments.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fretux.ailments.api.AilmentApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class AilmentCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ascendailments").requires(source -> source.hasPermission(2))
                .then(Commands.literal("clear").then(Commands.argument("target", EntityArgument.entity())
                        .executes(ctx -> withTarget(ctx, target -> { AilmentApi.clearAll(target); return true; }))))
                .then(Commands.literal("apply").then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.literal("bleed")
                                .executes(ctx -> withTarget(ctx, target -> AilmentApi.applyBleed(target, null)))
                                .then(explicitDot(false)))
                        .then(Commands.literal("soul_rot")
                                .executes(ctx -> withTarget(ctx, target -> AilmentApi.applySoulRot(target, null)))
                                .then(explicitDot(true)))
                        .then(control("fracture", AilmentApi::applyFracture, true))
                        .then(control("fear", (target, source, duration, amplifier) ->
                                AilmentApi.applyFear(target, source, duration), false))
                        .then(control("charm", (target, source, duration, amplifier) ->
                                AilmentApi.applyCharm(target, source, duration), false))
                        .then(control("taunt", AilmentApi::applyTaunt, true))
                        .then(Commands.literal("overcharm")
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                        .executes(ctx -> withTarget(ctx, target -> AilmentApi.applyOvercharm(target,
                                                null, IntegerArgumentType.getInteger(ctx, "duration"))))))
                        .then(Commands.literal("winded")
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                        .executes(ctx -> withTarget(ctx, target -> AilmentApi.applyWinded(target,
                                                null, IntegerArgumentType.getInteger(ctx, "duration")))))))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> explicitDot(boolean soulRot) {
        return Commands.argument("duration", IntegerArgumentType.integer(1))
                .executes(ctx -> withTarget(ctx, target -> soulRot
                        ? AilmentApi.applySoulRot(target, null, IntegerArgumentType.getInteger(ctx, "duration"), 0)
                        : AilmentApi.applyBleed(target, null, IntegerArgumentType.getInteger(ctx, "duration"), 0)))
                .then(Commands.argument("amplifier", IntegerArgumentType.integer(0, soulRot ? 4 : 2))
                        .executes(ctx -> withTarget(ctx, target -> soulRot
                                ? AilmentApi.applySoulRot(target, null,
                                        IntegerArgumentType.getInteger(ctx, "duration"),
                                        IntegerArgumentType.getInteger(ctx, "amplifier"))
                                : AilmentApi.applyBleed(target, null,
                                        IntegerArgumentType.getInteger(ctx, "duration"),
                                        IntegerArgumentType.getInteger(ctx, "amplifier")))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> control(
            String name, ControlApplication application, boolean amplifier) {
        var duration = Commands.argument("duration", IntegerArgumentType.integer(1))
                .executes(ctx -> applyControl(ctx, application, 0));
        if (amplifier) duration.then(Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                .executes(ctx -> applyControl(ctx, application,
                        IntegerArgumentType.getInteger(ctx, "amplifier"))));
        return Commands.literal(name).then(Commands.argument("source", EntityArgument.entity()).then(duration));
    }

    private static int applyControl(CommandContext<CommandSourceStack> ctx, ControlApplication application,
                                    int amplifier) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Entity targetEntity = EntityArgument.getEntity(ctx, "target");
        Entity sourceEntity = EntityArgument.getEntity(ctx, "source");
        if (!(targetEntity instanceof LivingEntity target) || !(sourceEntity instanceof LivingEntity source)) {
            ctx.getSource().sendFailure(Component.literal("Target and source must be living entities"));
            return 0;
        }
        boolean applied = application.apply(target, source, IntegerArgumentType.getInteger(ctx, "duration"), amplifier);
        return result(ctx, applied);
    }

    private static int withTarget(CommandContext<CommandSourceStack> ctx, TargetApplication application)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(ctx, "target");
        if (!(entity instanceof LivingEntity target)) {
            ctx.getSource().sendFailure(Component.literal("Target must be a living entity"));
            return 0;
        }
        return result(ctx, application.apply(target));
    }

    private static int result(CommandContext<CommandSourceStack> ctx, boolean applied) {
        if (!applied) { ctx.getSource().sendFailure(Component.literal("Ailment was rejected")); return 0; }
        ctx.getSource().sendSuccess(() -> Component.literal("Ailment operation succeeded"), true);
        return 1;
    }
    @FunctionalInterface private interface TargetApplication { boolean apply(LivingEntity target); }
    @FunctionalInterface private interface ControlApplication {
        boolean apply(LivingEntity target, LivingEntity source, int duration, int amplifier);
    }
    private AilmentCommands() {}
}
