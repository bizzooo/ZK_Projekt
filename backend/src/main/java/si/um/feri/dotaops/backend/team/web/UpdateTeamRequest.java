package si.um.feri.dotaops.backend.team.web;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateTeamRequest {

    @Size(min = 2, max = 80)
    private String name;

    @Size(max = 16)
    private String tag;

    @Size(max = 80)
    @Pattern(regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$")
    private String slug;

    @Size(max = 80)
    private String region;

    @Size(max = 512)
    private String logoUrl;

    @Size(max = 500)
    private String description;

    @JsonIgnore
    private boolean namePresent;

    @JsonIgnore
    private boolean tagPresent;

    @JsonIgnore
    private boolean slugPresent;

    @JsonIgnore
    private boolean regionPresent;

    @JsonIgnore
    private boolean logoUrlPresent;

    @JsonIgnore
    private boolean descriptionPresent;

    public UpdateTeamRequest() {
    }

    public UpdateTeamRequest(
            String name,
            String tag,
            String slug,
            String region,
            String logoUrl,
            String description
    ) {
        this.name = name;
        this.tag = tag;
        this.slug = slug;
        this.region = region;
        this.logoUrl = logoUrl;
        this.description = description;
        this.namePresent = name != null;
        this.tagPresent = tag != null;
        this.slugPresent = slug != null;
        this.regionPresent = region != null;
        this.logoUrlPresent = logoUrl != null;
        this.descriptionPresent = description != null;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.namePresent = true;
    }

    public String tag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
        this.tagPresent = true;
    }

    public String slug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
        this.slugPresent = true;
    }

    public String region() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
        this.regionPresent = true;
    }

    public String logoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        this.logoUrlPresent = true;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    @JsonIgnore
    public boolean hasName() {
        return namePresent;
    }

    @JsonIgnore
    public boolean hasTag() {
        return tagPresent;
    }

    @JsonIgnore
    public boolean hasSlug() {
        return slugPresent;
    }

    @JsonIgnore
    public boolean hasRegion() {
        return regionPresent;
    }

    @JsonIgnore
    public boolean hasLogoUrl() {
        return logoUrlPresent;
    }

    @JsonIgnore
    public boolean hasDescription() {
        return descriptionPresent;
    }

    @JsonIgnore
    public boolean hasChanges() {
        return namePresent
                || tagPresent
                || slugPresent
                || regionPresent
                || logoUrlPresent
                || descriptionPresent;
    }
}
