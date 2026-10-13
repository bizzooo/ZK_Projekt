package si.um.feri.dotaops.backend.tournament.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import si.um.feri.dotaops.backend.common.validation.ValidationMessages;
import si.um.feri.dotaops.backend.tournament.domain.TournamentFormat;

public class UpdateTournamentRequest {

    @Size(min = 2, max = 120)
    private String title;

    @Size(max = 80)
    @Pattern(regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$", message = ValidationMessages.SLUG)
    private String slug;

    private TournamentFormat format;

    @Size(max = 2000)
    private String description;

    @Size(max = 5000)
    private String rules;

    @Size(max = 120)
    private String prizePool;

    @Min(2)
    @Max(128)
    private Integer maxTeams;

    private OffsetDateTime startsAt;

    private OffsetDateTime endsAt;

    private OffsetDateTime registrationOpensAt;

    private OffsetDateTime registrationClosesAt;

    @Size(min = 1, max = 80)
    private String timezone;

    private OffsetDateTime checkInOpensAt;

    private OffsetDateTime checkInClosesAt;

    @Valid
    private TournamentSettingsDto settings;

    @JsonIgnore
    private boolean titlePresent;
    @JsonIgnore
    private boolean slugPresent;
    @JsonIgnore
    private boolean formatPresent;
    @JsonIgnore
    private boolean descriptionPresent;
    @JsonIgnore
    private boolean rulesPresent;
    @JsonIgnore
    private boolean prizePoolPresent;
    @JsonIgnore
    private boolean maxTeamsPresent;
    @JsonIgnore
    private boolean startsAtPresent;
    @JsonIgnore
    private boolean endsAtPresent;
    @JsonIgnore
    private boolean registrationOpensAtPresent;
    @JsonIgnore
    private boolean registrationClosesAtPresent;
    @JsonIgnore
    private boolean timezonePresent;
    @JsonIgnore
    private boolean checkInOpensAtPresent;
    @JsonIgnore
    private boolean checkInClosesAtPresent;
    @JsonIgnore
    private boolean settingsPresent;

    public UpdateTournamentRequest() {
    }

    public UpdateTournamentRequest(
            String title,
            String slug,
            TournamentFormat format,
            String description,
            String rules,
            String prizePool,
            Integer maxTeams,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            OffsetDateTime registrationOpensAt,
            OffsetDateTime registrationClosesAt,
            String timezone,
            OffsetDateTime checkInOpensAt,
            OffsetDateTime checkInClosesAt,
            TournamentSettingsDto settings
    ) {
        this.title = title;
        this.slug = slug;
        this.format = format;
        this.description = description;
        this.rules = rules;
        this.prizePool = prizePool;
        this.maxTeams = maxTeams;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.registrationOpensAt = registrationOpensAt;
        this.registrationClosesAt = registrationClosesAt;
        this.timezone = timezone;
        this.checkInOpensAt = checkInOpensAt;
        this.checkInClosesAt = checkInClosesAt;
        this.settings = settings;
        this.titlePresent = title != null;
        this.slugPresent = slug != null;
        this.formatPresent = format != null;
        this.descriptionPresent = description != null;
        this.rulesPresent = rules != null;
        this.prizePoolPresent = prizePool != null;
        this.maxTeamsPresent = maxTeams != null;
        this.startsAtPresent = startsAt != null;
        this.endsAtPresent = endsAt != null;
        this.registrationOpensAtPresent = registrationOpensAt != null;
        this.registrationClosesAtPresent = registrationClosesAt != null;
        this.timezonePresent = timezone != null;
        this.checkInOpensAtPresent = checkInOpensAt != null;
        this.checkInClosesAtPresent = checkInClosesAt != null;
        this.settingsPresent = settings != null;
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    public String slug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
        this.slugPresent = true;
    }

    public TournamentFormat format() {
        return format;
    }

    public void setFormat(TournamentFormat format) {
        this.format = format;
        this.formatPresent = true;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    public String rules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
        this.rulesPresent = true;
    }

    public String prizePool() {
        return prizePool;
    }

    public void setPrizePool(String prizePool) {
        this.prizePool = prizePool;
        this.prizePoolPresent = true;
    }

    public Integer maxTeams() {
        return maxTeams;
    }

    public void setMaxTeams(Integer maxTeams) {
        this.maxTeams = maxTeams;
        this.maxTeamsPresent = true;
    }

    public OffsetDateTime startsAt() {
        return startsAt;
    }

    public void setStartsAt(OffsetDateTime startsAt) {
        this.startsAt = startsAt;
        this.startsAtPresent = true;
    }

    public OffsetDateTime endsAt() {
        return endsAt;
    }

    public void setEndsAt(OffsetDateTime endsAt) {
        this.endsAt = endsAt;
        this.endsAtPresent = true;
    }

    public OffsetDateTime registrationOpensAt() {
        return registrationOpensAt;
    }

    public void setRegistrationOpensAt(OffsetDateTime registrationOpensAt) {
        this.registrationOpensAt = registrationOpensAt;
        this.registrationOpensAtPresent = true;
    }

    public OffsetDateTime registrationClosesAt() {
        return registrationClosesAt;
    }

    public void setRegistrationClosesAt(OffsetDateTime registrationClosesAt) {
        this.registrationClosesAt = registrationClosesAt;
        this.registrationClosesAtPresent = true;
    }

    public String timezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
        this.timezonePresent = true;
    }

    public OffsetDateTime checkInOpensAt() {
        return checkInOpensAt;
    }

    public void setCheckInOpensAt(OffsetDateTime checkInOpensAt) {
        this.checkInOpensAt = checkInOpensAt;
        this.checkInOpensAtPresent = true;
    }

    public OffsetDateTime checkInClosesAt() {
        return checkInClosesAt;
    }

    public void setCheckInClosesAt(OffsetDateTime checkInClosesAt) {
        this.checkInClosesAt = checkInClosesAt;
        this.checkInClosesAtPresent = true;
    }

    public TournamentSettingsDto settings() {
        return settings;
    }

    public void setSettings(TournamentSettingsDto settings) {
        this.settings = settings;
        this.settingsPresent = true;
    }

    @JsonIgnore
    public boolean hasTitle() {
        return titlePresent;
    }

    @JsonIgnore
    public boolean hasSlug() {
        return slugPresent;
    }

    @JsonIgnore
    public boolean hasFormat() {
        return formatPresent;
    }

    @JsonIgnore
    public boolean hasDescription() {
        return descriptionPresent;
    }

    @JsonIgnore
    public boolean hasRules() {
        return rulesPresent;
    }

    @JsonIgnore
    public boolean hasPrizePool() {
        return prizePoolPresent;
    }

    @JsonIgnore
    public boolean hasMaxTeams() {
        return maxTeamsPresent;
    }

    @JsonIgnore
    public boolean hasStartsAt() {
        return startsAtPresent;
    }

    @JsonIgnore
    public boolean hasEndsAt() {
        return endsAtPresent;
    }

    @JsonIgnore
    public boolean hasRegistrationOpensAt() {
        return registrationOpensAtPresent;
    }

    @JsonIgnore
    public boolean hasRegistrationClosesAt() {
        return registrationClosesAtPresent;
    }

    @JsonIgnore
    public boolean hasTimezone() {
        return timezonePresent;
    }

    @JsonIgnore
    public boolean hasCheckInOpensAt() {
        return checkInOpensAtPresent;
    }

    @JsonIgnore
    public boolean hasCheckInClosesAt() {
        return checkInClosesAtPresent;
    }

    @JsonIgnore
    public boolean hasSettings() {
        return settingsPresent;
    }

    @JsonIgnore
    public boolean hasChanges() {
        return titlePresent
                || slugPresent
                || formatPresent
                || descriptionPresent
                || rulesPresent
                || prizePoolPresent
                || maxTeamsPresent
                || startsAtPresent
                || endsAtPresent
                || registrationOpensAtPresent
                || registrationClosesAtPresent
                || timezonePresent
                || checkInOpensAtPresent
                || checkInClosesAtPresent
                || settingsPresent;
    }
}
