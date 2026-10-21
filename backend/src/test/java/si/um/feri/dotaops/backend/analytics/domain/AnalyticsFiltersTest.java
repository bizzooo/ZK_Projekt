package si.um.feri.dotaops.backend.analytics.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import si.um.feri.dotaops.backend.common.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalyticsFiltersTest {

    @Test
    void limitDefaultsToTenAndCapsAtOneHundred() {
        assertThat(new AnalyticsFilters(null, null, null, null, 0).limit()).isEqualTo(10);
        assertThat(new AnalyticsFilters(null, null, null, null, -7).limit()).isEqualTo(10);
        assertThat(new AnalyticsFilters(null, null, null, null, 250).limit()).isEqualTo(100);
        assertThat(new AnalyticsFilters(null, null, null, null, 1).limit()).isEqualTo(1);
        assertThat(new AnalyticsFilters(null, null, null, null, 100).limit()).isEqualTo(100);
    }

    @Test
    void allowsInclusiveEqualDateBounds() {
        OffsetDateTime instant = OffsetDateTime.parse("2026-06-01T00:00:00Z");

        var filters = new AnalyticsFilters(null, null, null, null, instant, instant, 10);

        assertThat(filters.from()).isEqualTo(instant);
        assertThat(filters.to()).isEqualTo(instant);
    }

    @Test
    void withMethodsPreserveUnchangedFiltersAndDateBounds() {
        UUID tournamentId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        UUID teamId = UUID.fromString("22222222-2222-4222-8222-222222222222");
        UUID profileId = UUID.fromString("33333333-3333-4333-8333-333333333333");
        UUID heroId = UUID.fromString("44444444-4444-4444-8444-444444444444");
        OffsetDateTime from = OffsetDateTime.parse("2026-05-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        var original = new AnalyticsFilters(tournamentId, teamId, profileId, heroId, from, to, 25);

        assertThat(original.withTournamentId(null))
                .isEqualTo(new AnalyticsFilters(null, teamId, profileId, heroId, from, to, 25));
        assertThat(original.withTeamId(null))
                .isEqualTo(new AnalyticsFilters(tournamentId, null, profileId, heroId, from, to, 25));
        assertThat(original.withProfileId(null))
                .isEqualTo(new AnalyticsFilters(tournamentId, teamId, null, heroId, from, to, 25));
        assertThat(original.withHeroId(null))
                .isEqualTo(new AnalyticsFilters(tournamentId, teamId, profileId, null, from, to, 25));
        assertThat(original.withLimit(50))
                .isEqualTo(new AnalyticsFilters(tournamentId, teamId, profileId, heroId, from, to, 50));
    }

    @Test
    void rejectsTimeRangeWhereFromIsAfterTo() {
        OffsetDateTime from = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-05-01T00:00:00Z");

        assertThatThrownBy(() -> new AnalyticsFilters(null, null, null, null, from, to, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Analytics time range is invalid.");
    }

    @Test
    void allowsOpenEndedTimeRanges() {
        OffsetDateTime instant = OffsetDateTime.parse("2026-06-01T00:00:00Z");

        assertThat(new AnalyticsFilters(null, null, null, null, instant, null, 10).from())
                .isEqualTo(instant);
        assertThat(new AnalyticsFilters(null, null, null, null, null, instant, 10).to())
                .isEqualTo(instant);
    }
}
