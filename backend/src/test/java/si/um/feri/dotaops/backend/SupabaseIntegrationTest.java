package si.um.feri.dotaops.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Migration smoke test for PostgreSQL-backed environments.
 * Runs only when SUPABASE_DB_URL is present. CI points this at an ephemeral
 * PostgreSQL service; local/staging runs may still point it at Supabase.
 */
@SpringBootTest
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")
class SupabaseIntegrationTest {

    @Test
    void contextLoadsWithSupabaseConfiguration() {
        // If context loads successfully with integration profile,
        // it means Supabase connection is configured and Flyway ran.
        // If datasource or migrations fail, Spring startup fails before this test runs.
    }
}
