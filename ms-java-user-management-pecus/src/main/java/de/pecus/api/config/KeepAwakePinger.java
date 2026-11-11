package de.pecus.api.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Keeps the free Render instance awake during the day.
 *
 * Render's free plan stops a web service after 15 minutes without inbound
 * traffic, and the next request then waits about a minute for the JVM to
 * start. Every {@code app.keep-awake.interval} (10 min) this component calls
 * the service's own public URL ({@code KEEP_AWAKE_URL}); the request goes
 * through Render's edge, so it counts as traffic.
 *
 * It only pings between {@code from-hour} and {@code until-hour} in
 * {@code zone} (06:00-23:00 Europe/Berlin by default). At night it stays quiet
 * and the instance sleeps, which keeps the usage at about 17 hours a day,
 * inside Render's 750 free instance hours per month.
 *
 * Disabled when {@code KEEP_AWAKE_URL} is empty (local runs, tests).
 */
@Configuration
@EnableScheduling
public class KeepAwakePinger {

    private static final Logger log = LoggerFactory.getLogger(KeepAwakePinger.class);

    private final String url;
    private final ZoneId zone;
    private final int fromHour;
    private final int untilHour;
    private final Clock clock;
    private final Consumer<String> ping;

    @Autowired
    public KeepAwakePinger(@Value("${app.keep-awake.url:}") String url,
                           @Value("${app.keep-awake.zone:Europe/Berlin}") String zone,
                           @Value("${app.keep-awake.from-hour:6}") int fromHour,
                           @Value("${app.keep-awake.until-hour:23}") int untilHour) {
        this(url, ZoneId.of(zone), fromHour, untilHour, Clock.systemUTC(), KeepAwakePinger::httpGet);
    }

    public KeepAwakePinger(String url, ZoneId zone, int fromHour, int untilHour, Clock clock, Consumer<String> ping) {
        this.url = url == null ? "" : url.trim();
        this.zone = zone;
        this.fromHour = fromHour;
        this.untilHour = untilHour;
        this.clock = clock;
        this.ping = ping;
    }

    @Scheduled(initialDelayString = "${app.keep-awake.initial-delay:PT2M}",
               fixedDelayString = "${app.keep-awake.interval:PT10M}")
    public void scheduledPing() {
        pingIfDaytime();
    }

    /** Pings the configured URL if it is set and the local time is inside the window. */
    public boolean pingIfDaytime() {
        if (url.isEmpty()) {
            return false;
        }
        int hour = ZonedDateTime.now(clock).withZoneSameInstant(zone).getHour();
        if (hour < fromHour || hour >= untilHour) {
            return false;
        }
        try {
            ping.accept(url);
            return true;
        } catch (Exception e) {
            log.debug("Keep-awake ping failed: {}", e.getMessage());
            return false;
        }
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private static void httpGet(String target) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(target))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "hackthesnackdb-keep-awake")
                    .GET()
                    .build();
            HTTP.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
