package com.bluestaq.demo.config;

import com.bluestaq.demo.service.DemoDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds demo users, a team and notes at startup when the database has no users.
 *
 * <p>Only active when {@code app.demo-data.enabled=true}. Fail safe: if the property is
 * missing the seeder does not run. It is enabled in {@code application.properties} and
 * disabled for tests in {@code src/test/resources/config/application.properties}.
 */
@Component
// Security: demo accounts have a published password, so they must never be created unless
// explicitly enabled; matchIfMissing = false keeps the seeder off by default.
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private final DemoDataService demoDataService;

    /**
     * Seeds the demo data if the database is empty.
     *
     * @param args application arguments (unused)
     */
    @Override
    public void run(String... args) {
        if (demoDataService.seedIfEmpty()) {
            log.info("Seeded demo data: users alice/bob/carol@demo.com (see README.md for credentials)");
        } else {
            log.info("Users already exist; skipping demo data");
        }
    }
}
