package si.um.feri.dotaops.backend.profile.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(min = 2, max = 40)
    private String nickname;

    @Size(max = 80)
    private String displayName;

    @Size(max = 512)
    private String avatarUrl;

    @Size(max = 500)
    private String bio;

    @Pattern(regexp = "^[A-Za-z]{2}$")
    private String countryCode;

    @JsonIgnore
    private boolean nicknamePresent;

    @JsonIgnore
    private boolean displayNamePresent;

    @JsonIgnore
    private boolean avatarUrlPresent;

    @JsonIgnore
    private boolean bioPresent;

    @JsonIgnore
    private boolean countryCodePresent;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(
            String nickname,
            String displayName,
            String avatarUrl,
            String bio,
            String countryCode
    ) {
        this.nickname = nickname;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.countryCode = countryCode;
        this.nicknamePresent = nickname != null;
        this.displayNamePresent = displayName != null;
        this.avatarUrlPresent = avatarUrl != null;
        this.bioPresent = bio != null;
        this.countryCodePresent = countryCode != null;
    }

    public String nickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.nicknamePresent = true;
    }

    public String displayName() {
        return displayName;
    }

    @JsonAlias("display_name")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.displayNamePresent = true;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    @JsonAlias("avatar_url")
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.avatarUrlPresent = true;
    }

    public String bio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
        this.bioPresent = true;
    }

    public String countryCode() {
        return countryCode;
    }

    @JsonAlias("country_code")
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        this.countryCodePresent = true;
    }

    @JsonIgnore
    public boolean hasNickname() {
        return nicknamePresent;
    }

    @JsonIgnore
    public boolean hasDisplayName() {
        return displayNamePresent;
    }

    @JsonIgnore
    public boolean hasAvatarUrl() {
        return avatarUrlPresent;
    }

    @JsonIgnore
    public boolean hasBio() {
        return bioPresent;
    }

    @JsonIgnore
    public boolean hasCountryCode() {
        return countryCodePresent;
    }

    @JsonIgnore
    public boolean hasChanges() {
        return nicknamePresent
                || displayNamePresent
                || avatarUrlPresent
                || bioPresent
                || countryCodePresent;
    }
}
