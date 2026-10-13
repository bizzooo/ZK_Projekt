package si.um.feri.dotaops.backend.common.validation;

public final class ValidationMessages {

    public static final String REQUIRED = "Field is required.";
    public static final String POSITIVE = "Value must be positive.";
    public static final String SLUG = "Slug must contain lowercase letters, numbers, and single hyphens only.";
    public static final String UUID = "Value must be a valid UUID.";

    private ValidationMessages() {
    }
}
