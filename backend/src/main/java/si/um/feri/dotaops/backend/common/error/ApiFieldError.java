package si.um.feri.dotaops.backend.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiFieldError(
        String field,
        String message,
        String code
) {
}
