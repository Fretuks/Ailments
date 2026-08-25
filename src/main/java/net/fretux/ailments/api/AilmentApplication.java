package net.fretux.ailments.api;

import java.util.Objects;

/**
 * Immutable description of one ailment application.
 *
 * <p>Use {@link #timed(AilmentType, int, int)} when the caller controls the duration and
 * amplifier. Use {@link #stack(AilmentType)} for the library's configured Bleed, Soul Rot, or Fracture
 * stacking behavior.</p>
 */
public final class AilmentApplication {
    private final AilmentType type;
    private final int durationTicks;
    private final int amplifier;
    private final boolean stacking;

    private AilmentApplication(AilmentType type, int durationTicks, int amplifier, boolean stacking) {
        this.type = Objects.requireNonNull(type, "type");
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
        this.stacking = stacking;
    }

    /** Creates an explicit, source-scaled application. Duration is measured in ticks. */
    public static AilmentApplication timed(AilmentType type, int durationTicks, int amplifier) {
        if (durationTicks < 1) throw new IllegalArgumentException("durationTicks must be positive");
        if (amplifier < 0) throw new IllegalArgumentException("amplifier must not be negative");
        return new AilmentApplication(type, durationTicks, amplifier, false);
    }

    /** Creates an explicit amplifier-zero, source-scaled application. */
    public static AilmentApplication timed(AilmentType type, int durationTicks) {
        return timed(type, durationTicks, 0);
    }

    /**
     * Adds one stack using the configured duration and cap.
     *
     * @throws IllegalArgumentException if {@code type} does not support stacking
     */
    public static AilmentApplication stack(AilmentType type) {
        Objects.requireNonNull(type, "type");
        if (type != AilmentType.BLEED && type != AilmentType.SOUL_ROT && type != AilmentType.FRACTURE) {
            throw new IllegalArgumentException("Only BLEED, SOUL_ROT, and FRACTURE support stack applications");
        }
        return new AilmentApplication(type, 0, 0, true);
    }

    public AilmentType type() { return type; }
    public int durationTicks() { return durationTicks; }
    public int amplifier() { return amplifier; }
    public boolean isStacking() { return stacking; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AilmentApplication that)) return false;
        return durationTicks == that.durationTicks && amplifier == that.amplifier
                && stacking == that.stacking && type == that.type;
    }

    @Override public int hashCode() {
        return Objects.hash(type, durationTicks, amplifier, stacking);
    }

    @Override public String toString() {
        return stacking ? "AilmentApplication[" + type + ", stack]"
                : "AilmentApplication[" + type + ", " + durationTicks + " ticks, amplifier=" + amplifier + "]";
    }
}
