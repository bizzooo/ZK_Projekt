package si.um.feri.dotaops.backend.opendota.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMatchImportRequest(
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[0-9]+$")
        String dotaMatchId
) {
}
