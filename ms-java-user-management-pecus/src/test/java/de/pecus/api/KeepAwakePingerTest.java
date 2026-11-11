package de.pecus.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.pecus.api.config.KeepAwakePinger;

/**
 * The API pings its own public URL during the day so the free Render instance
 * does not spin down; at night it stays quiet so it can sleep.
 */
class KeepAwakePingerTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private static KeepAwakePinger pinger(String url, String utcInstant, List<String> pinged) {
        Clock clock = Clock.fixed(Instant.parse(utcInstant), ZoneId.of("UTC"));
        return new KeepAwakePinger(url, BERLIN, 6, 23, clock, pinged::add);
    }

    @Test
    void pingsDuringTheDayInTheConfiguredZone() {
        List<String> pinged = new ArrayList<>();
        // 07:30 UTC in November = 08:30 in Berlin
        assertTrue(pinger("https://api.test/health", "2026-11-11T07:30:00Z", pinged).pingIfDaytime());
        assertEquals(List.of("https://api.test/health"), pinged);
    }

    @Test
    void staysQuietAtNightSoTheInstanceCanSleep() {
        List<String> pinged = new ArrayList<>();
        // 23:30 UTC = 00:30 in Berlin; 04:30 UTC = 05:30 in Berlin
        assertFalse(pinger("https://api.test/health", "2026-11-11T23:30:00Z", pinged).pingIfDaytime());
        assertFalse(pinger("https://api.test/health", "2026-11-11T04:30:00Z", pinged).pingIfDaytime());
        assertTrue(pinged.isEmpty());
    }

    @Test
    void windowFollowsSummerTime() {
        List<String> pinged = new ArrayList<>();
        // 21:30 UTC in July = 23:30 in Berlin (CEST): outside; 20:30 UTC = 22:30: inside
        assertFalse(pinger("https://api.test/health", "2026-07-10T21:30:00Z", pinged).pingIfDaytime());
        assertTrue(pinger("https://api.test/health", "2026-07-10T20:30:00Z", pinged).pingIfDaytime());
    }

    @Test
    void windowZeroToTwentyFourKeepsItAwakeAroundTheClock() {
        List<String> pinged = new ArrayList<>();
        for (String instant : new String[] {"2026-11-11T23:30:00Z", "2026-11-11T02:00:00Z", "2026-11-11T12:00:00Z"}) {
            Clock clock = Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"));
            assertTrue(new KeepAwakePinger("https://api.test/health", BERLIN, 0, 24, clock, pinged::add).pingIfDaytime());
        }
        assertEquals(3, pinged.size());
    }

    @Test
    void disabledWithoutUrl() {
        List<String> pinged = new ArrayList<>();
        assertFalse(pinger("", "2026-11-11T10:00:00Z", pinged).pingIfDaytime());
        assertFalse(pinger(null, "2026-11-11T10:00:00Z", pinged).pingIfDaytime());
        assertTrue(pinged.isEmpty());
    }

    @Test
    void aFailedPingNeverBreaksTheScheduler() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneId.of("UTC"));
        KeepAwakePinger failing = new KeepAwakePinger("https://api.test/health", BERLIN, 6, 23, clock, url -> {
            throw new IllegalStateException("network down");
        });
        assertFalse(failing.pingIfDaytime());
    }
}
