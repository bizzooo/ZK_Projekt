package si.um.feri.dotaops.backend.tournament.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import si.um.feri.dotaops.backend.common.error.BadRequestException;
import si.um.feri.dotaops.backend.tournament.domain.TournamentFormat;
import si.um.feri.dotaops.backend.tournament.domain.TournamentSettings;

@Component
public class TournamentSettingsMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String toJson(TournamentSettings settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Tournament settings could not be serialized.");
        }
    }

    public TournamentSettings fromJson(String json, TournamentFormat fallbackFormat, int fallbackMaxTeams) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return TournamentSettings.defaults(fallbackFormat, fallbackMaxTeams);
        }

        try {
            StoredTournamentSettings storedSettings = objectMapper.readValue(json, StoredTournamentSettings.class);
            return new TournamentSettings(
                    storedSettings.maxTeams() == null ? fallbackMaxTeams : storedSettings.maxTeams(),
                    storedSettings.minTeams() == null ? TournamentSettings.DEFAULT_MIN_TEAMS : storedSettings.minTeams(),
                    storedSettings.teamSize() == null ? TournamentSettings.DOTA_TEAM_SIZE : storedSettings.teamSize(),
                    storedSettings.bestOf() == null ? TournamentSettings.DEFAULT_BEST_OF : storedSettings.bestOf(),
                    storedSettings.format() == null ? fallbackFormat : storedSettings.format(),
                    storedSettings.checkInEnabled() == null
                            ? TournamentSettings.DEFAULT_CHECK_IN_ENABLED
                            : storedSettings.checkInEnabled(),
                    storedSettings.allowSubstitutes() == null
                            ? TournamentSettings.DEFAULT_ALLOW_SUBSTITUTES
                            : storedSettings.allowSubstitutes());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return TournamentSettings.defaults(fallbackFormat, fallbackMaxTeams);
        }
    }

    private record StoredTournamentSettings(
            Integer maxTeams,
            Integer minTeams,
            Integer teamSize,
            Integer bestOf,
            TournamentFormat format,
            Boolean checkInEnabled,
            Boolean allowSubstitutes
    ) {
    }
}
