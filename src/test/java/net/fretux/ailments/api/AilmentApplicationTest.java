package net.fretux.ailments.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AilmentApplicationTest {
    @Test void timedApplicationRetainsPublicParameters() {
        AilmentApplication application = AilmentApplication.timed(AilmentType.TAUNT, 100, 2);
        assertEquals(AilmentType.TAUNT, application.type());
        assertEquals(100, application.durationTicks());
        assertEquals(2, application.amplifier());
        assertFalse(application.isStacking());
    }

    @Test void stackingIsOnlyAvailableForStackableAilments() {
        assertTrue(AilmentApplication.stack(AilmentType.BLEED).isStacking());
        assertTrue(AilmentApplication.stack(AilmentType.SOUL_ROT).isStacking());
        assertTrue(AilmentApplication.stack(AilmentType.FRACTURE).isStacking());
        assertThrows(IllegalArgumentException.class, () -> AilmentApplication.stack(AilmentType.FEAR));
    }

    @Test void timedApplicationRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class,
                () -> AilmentApplication.timed(AilmentType.CHARM, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> AilmentApplication.timed(AilmentType.CHARM, 20, -1));
    }
}
