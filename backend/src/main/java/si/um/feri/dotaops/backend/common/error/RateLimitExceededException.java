package si.um.feri.dotaops.backend.common.error;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ApiException {

    public RateLimitExceededException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, ApiErrorCode.RATE_LIMITED, message);
    }
}
