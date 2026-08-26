package net.fretux.ailments.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/** Reflection boundary: no Ascend type appears in a loadable signature or constant-pool class reference. */
public final class AscendCompat {
    private static final String PROVIDER = "net.fretux.ascend.player.PlayerStatsProvider";
    private static boolean resolved;
    private static Capability<?> statsCapability;
    private static Method getAttributeLevel;

    public static boolean isAscendLoaded() { return ModList.get().isLoaded("ascend"); }

    /** Resolves the optional public API without requiring an entity; useful for lifecycle diagnostics. */
    public static boolean validateIntegration() {
        if (!isAscendLoaded()) return false;
        try {
            resolve();
            return statsCapability != null && getAttributeLevel != null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    public static int getArcane(LivingEntity source) {
        if (!(source instanceof Player player) || !isAscendLoaded()) return 0;
        try {
            resolve();
            if (statsCapability == null || getAttributeLevel == null) return 0;
            LazyOptional<?> lazy = player.getCapability(statsCapability);
            Optional<?> stats = lazy.resolve();
            if (stats.isEmpty()) return 0;
            return Math.max(0, ((Number) getAttributeLevel.invoke(stats.get(), "arcane")).intValue());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    private static synchronized void resolve() throws ReflectiveOperationException {
        if (resolved) return;
        Class<?> provider = Class.forName(PROVIDER, false, AscendCompat.class.getClassLoader());
        Field capabilityField = provider.getField("PLAYER_STATS");
        Capability<?> resolvedCapability = (Capability<?>) capabilityField.get(null);
        Class<?> statsClass = Class.forName("net.fretux.ascend.player.PlayerStats", false,
                AscendCompat.class.getClassLoader());
        Method resolvedGetter = statsClass.getMethod("getAttributeLevel", String.class);
        Class<?> returnType = resolvedGetter.getReturnType();
        boolean numericPrimitive = returnType == byte.class || returnType == short.class || returnType == int.class
                || returnType == long.class || returnType == float.class || returnType == double.class;
        if (!Number.class.isAssignableFrom(returnType) && !numericPrimitive)
            throw new NoSuchMethodException("getAttributeLevel(String) does not return a number");
        statsCapability = resolvedCapability;
        getAttributeLevel = resolvedGetter;
        resolved = true;
    }
    private AscendCompat() {}
}
