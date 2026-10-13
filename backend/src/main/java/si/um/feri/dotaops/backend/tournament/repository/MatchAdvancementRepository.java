package si.um.feri.dotaops.backend.tournament.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import si.um.feri.dotaops.backend.tournament.domain.MatchAdvancementSlot;
import si.um.feri.dotaops.backend.tournament.domain.MatchSlotName;
import si.um.feri.dotaops.backend.tournament.domain.MatchSlotSourceType;
import si.um.feri.dotaops.backend.tournament.domain.MatchStatus;

@Repository
public class MatchAdvancementRepository {

    private final JdbcTemplate jdbcTemplate;

    public MatchAdvancementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MatchAdvancementSlot> findDependentSlots(UUID sourceMatchId) {
        return jdbcTemplate.query(
                """
                select
                  ms.id as slot_id,
                  ms.match_id as target_match_id,
                  target.tournament_id,
                  ms.slot::text as slot,
                  ms.source_type::text as source_type,
                  ms.team_id as slot_team_id,
                  ms.is_locked,
                  target.team_a_id,
                  target.team_b_id,
                  target.status::text as target_status,
                  target.score_a,
                  target.score_b,
                  target.winner_team_id,
                  target.started_at,
                  target.finished_at,
                  target.cancelled_at
                from public.match_slots ms
                join public.matches target on target.id = ms.match_id
                where ms.source_match_id = ?
                order by target.round_number asc, target.bracket_position asc, ms.slot asc
                """,
                this::mapSlot,
                sourceMatchId);
    }

    public void updateSlotTeam(UUID slotId, UUID teamId) {
        jdbcTemplate.update(
                """
                update public.match_slots
                set team_id = ?,
                    updated_at = now()
                where id = ?
                """,
                teamId,
                slotId);
    }

    public void updateTargetMatchTeam(UUID matchId, MatchSlotName slot, UUID teamId) {
        String column = slot == MatchSlotName.TEAM_A ? "team_a_id" : "team_b_id";
        jdbcTemplate.update(
                """
                update public.matches
                set %s = ?,
                    updated_at = now()
                where id = ?
                """.formatted(column),
                teamId,
                matchId);
    }

    public void insertAudit(CreateMatchAdvancementAuditCommand command) {
        jdbcTemplate.update(
                """
                insert into public.match_advancement_audit_logs (
                  tournament_id,
                  source_match_id,
                  target_match_id,
                  target_slot,
                  source_type,
                  advanced_team_id,
                  previous_team_id,
                  reason,
                  message,
                  created_by
                )
                values (?, ?, ?, ?::public.dotaops_match_slot, ?::public.dotaops_match_slot_source, ?, ?, ?, ?, ?)
                """,
                command.tournamentId(),
                command.sourceMatchId(),
                command.targetMatchId(),
                command.targetSlot().databaseValue(),
                command.sourceType().databaseValue(),
                command.advancedTeamId(),
                command.previousTeamId(),
                command.reason(),
                command.message(),
                command.createdBy());
    }

    private MatchAdvancementSlot mapSlot(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MatchAdvancementSlot(
                resultSet.getObject("slot_id", UUID.class),
                resultSet.getObject("target_match_id", UUID.class),
                resultSet.getObject("tournament_id", UUID.class),
                MatchSlotName.fromDatabaseValue(resultSet.getString("slot")),
                MatchSlotSourceType.fromDatabaseValue(resultSet.getString("source_type")),
                resultSet.getObject("slot_team_id", UUID.class),
                resultSet.getBoolean("is_locked"),
                resultSet.getObject("team_a_id", UUID.class),
                resultSet.getObject("team_b_id", UUID.class),
                MatchStatus.fromDatabaseValue(resultSet.getString("target_status")),
                resultSet.getInt("score_a"),
                resultSet.getInt("score_b"),
                resultSet.getObject("winner_team_id", UUID.class),
                resultSet.getObject("started_at", OffsetDateTime.class),
                resultSet.getObject("finished_at", OffsetDateTime.class),
                resultSet.getObject("cancelled_at", OffsetDateTime.class));
    }
}
