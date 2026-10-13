package si.um.feri.dotaops.backend.tournament.dto;

import jakarta.validation.constraints.Min;

import si.um.feri.dotaops.backend.tournament.domain.TournamentFormat;
import si.um.feri.dotaops.backend.tournament.domain.TournamentSettings;

public record TournamentSettingsDto(
        @Min(1)
        Integer maxTeams,

        @Min(1)
        Integer minTeams,

        @Min(1)
        Integer teamSize,

        @Min(1)
        Integer bestOf,

        TournamentFormat format,

        Boolean checkInEnabled,

        Boolean allowSubstitutes
) {

    public static TournamentSettingsDto from(TournamentSettings settings) {
        return new TournamentSettingsDto(
                settings.maxTeams(),
                settings.minTeams(),
                settings.teamSize(),
                settings.bestOf(),
                settings.format(),
                settings.checkInEnabled(),
                settings.allowSubstitutes());
    }
}
