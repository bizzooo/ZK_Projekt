package si.um.feri.dotaops.backend.integration;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import si.um.feri.dotaops.backend.analytics.domain.AnalyticsFilters;
import si.um.feri.dotaops.backend.analytics.repository.AnalyticsRepository;
import si.um.feri.dotaops.backend.analytics.repository.RoleBasedAnalyticsRepository;
import si.um.feri.dotaops.backend.dashboard.repository.DashboardRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")
class DashboardAnalyticsRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private RoleBasedAnalyticsRepository roleBasedAnalyticsRepository;

    @Test
    void dashboardAndProtectedAnalyticsQueriesExecuteAgainstPostgres() {
        UUID playerAuthUserId = UUID.randomUUID();
        UUID organizerAuthUserId = UUID.randomUUID();
        UUID playerProfileId = upsertProfile(playerAuthUserId, "player");
        UUID organizerProfileId = upsertProfile(organizerAuthUserId, "organizer");
        UUID missingTeamId = UUID.randomUUID();
        UUID missingTournamentId = UUID.randomUUID();

        assertThat(dashboardRepository.countPendingInvitations(playerProfileId, null)).isZero();
        assertThat(dashboardRepository.countTournamentRegistrations(missingTeamId)).isZero();
        assertThat(dashboardRepository.findOrganizerCounts(organizerProfileId, false).tournaments()).isZero();
        assertThat(dashboardRepository.findAdminCounts().profiles()).isGreaterThanOrEqualTo(2);

        AnalyticsFilters playerFilters = new AnalyticsFilters(null, null, playerProfileId, null, 10);
        assertThat(analyticsRepository.findProtectedPlayerMetrics(playerFilters)).isEmpty();
        assertThat(analyticsRepository.findProtectedHeroMetrics(playerFilters)).isEmpty();
        assertThat(analyticsRepository.findProtectedTeamMetrics(
                new AnalyticsFilters(null, missingTeamId, null, null, 10))).isEmpty();
        assertThat(analyticsRepository.findProtectedTournamentMetricsById(missingTournamentId)).isEmpty();

        assertThat(roleBasedAnalyticsRepository.findOrganizerCounts(organizerProfileId, false).tournaments()).isZero();
        assertThat(roleBasedAnalyticsRepository.findTournamentOperationalMetrics(missingTournamentId).gamesProcessed())
                .isZero();
        assertThat(roleBasedAnalyticsRepository.findRecentImports(missingTournamentId, 10)).isEmpty();
    }

    @Test
    void organizerCountsIncludesFromBoundaryAndExcludesToBoundary() {
        UUID organizerProfileId = upsertProfile(UUID.randomUUID(), "organizer");
        OffsetDateTime from = OffsetDateTime.parse("2026-05-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-06-01T00:00:00Z");

        UUID tournamentId = jdbcTemplate.queryForObject(
                """
                insert into public.tournaments (slug, title, organizer_profile_id, starts_at)
                values (?, ?, ?, ?)
                returning id
                """,
                UUID.class,
                "it-" + uniqueSuffix(),
                "Timestamp boundary test",
                organizerProfileId,
                from);
        UUID matchId = jdbcTemplate.queryForObject(
                """
                insert into public.matches (tournament_id, round_name)
                values (?, ?)
                returning id
                """,
                UUID.class,
                tournamentId,
                "Test");

        for (int gameNumber = 1; gameNumber <= 3; gameNumber++) {
            OffsetDateTime timestamp = switch (gameNumber) {
                case 1 -> from.minusSeconds(1);
                case 2 -> from;
                default -> to;
            };
            jdbcTemplate.update(
                    """
                    insert into public.match_games (match_id, game_number, import_status, started_at)
                    values (?, ?, 'ready'::public.dotaops_import_status, ?)
                    """,
                    matchId,
                    gameNumber,
                    timestamp);
        }

        var counts = roleBasedAnalyticsRepository.findOrganizerCounts(
                organizerProfileId,
                false,
                new AnalyticsFilters(tournamentId, null, null, null, from, to, 10));

        assertThat(counts.processedMatchGames()).isEqualTo(1);
    }
}
