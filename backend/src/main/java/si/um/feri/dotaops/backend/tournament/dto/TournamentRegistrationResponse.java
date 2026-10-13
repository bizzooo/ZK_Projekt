package si.um.feri.dotaops.backend.tournament.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.TournamentRegistration;
import si.um.feri.dotaops.backend.tournament.domain.TournamentRegistrationMember;

public record TournamentRegistrationResponse(
        UUID id,
        UUID tournamentId,
        String tournamentSlug,
        String tournamentTitle,
        UUID teamId,
        String teamName,
        String teamTag,
        String teamSlug,
        UUID captainProfileId,
        String captainNickname,
        String status,
        String message,
        UUID reviewedBy,
        String reviewedByNickname,
        OffsetDateTime reviewedAt,
        Integer seedNumber,
        OffsetDateTime checkedInAt,
        String contactEmail,
        List<TournamentRegistrationMemberResponse> members,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static TournamentRegistrationResponse from(
            TournamentRegistration registration,
            List<TournamentRegistrationMember> members
    ) {
        return new TournamentRegistrationResponse(
                registration.id(),
                registration.tournamentId(),
                registration.tournamentSlug(),
                registration.tournamentTitle(),
                registration.teamId(),
                registration.teamName(),
                registration.teamTag(),
                registration.teamSlug(),
                registration.captainProfileId(),
                registration.captainNickname(),
                registration.status().databaseValue(),
                registration.message(),
                registration.reviewedBy(),
                registration.reviewedByNickname(),
                registration.reviewedAt(),
                registration.seedNumber(),
                registration.checkedInAt(),
                registration.contactEmail(),
                members.stream()
                        .map(TournamentRegistrationMemberResponse::from)
                        .toList(),
                registration.createdAt(),
                registration.updatedAt());
    }
}
