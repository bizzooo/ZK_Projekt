package si.um.feri.dotaops.backend.profile.web;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProfileRequest(
        @NotBlank
        @Size(min = 2, max = 40)
        String nickname,

        @JsonAlias("display_name")
        @Size(max = 80)
        String displayName,

        @JsonAlias("avatar_url")
        @Size(max = 512)
        String avatarUrl,

        @Size(max = 500)
        String bio,

        @JsonAlias("country_code")
        @Pattern(regexp = "^[A-Za-z]{2}$")
        String countryCode,

        @JsonAlias({"desired_role", "account_type", "accountType", "role"})
        String desiredRole
) {

    public CreateProfileRequest(
            String nickname,
            String displayName,
            String avatarUrl,
            String bio,
            String countryCode
    ) {
        this(nickname, displayName, avatarUrl, bio, countryCode, null);
    }
}
