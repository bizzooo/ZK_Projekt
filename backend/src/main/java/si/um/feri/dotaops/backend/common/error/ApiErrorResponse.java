package si.um.feri.dotaops.backend.common.error;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<ApiFieldError> errors
) {

    public ApiErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
