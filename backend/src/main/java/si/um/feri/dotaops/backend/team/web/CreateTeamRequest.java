package si.um.feri.dotaops.backend.team.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank
        @Size(min = 2, max = 80)
        String name,

        @Size(max = 16)
        String tag,

        @Size(max = 80)
        @Pattern(regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$")
        String slug,

        @Size(max = 80)
        String region,

        @Size(max = 512)
        String logoUrl,

        @Size(max = 500)
        String description
) {
}
