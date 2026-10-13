package si.um.feri.dotaops.backend.common.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageRequestParams(
        @Min(0)
        int page,

        @Min(1)
        @Max(100)
        int size,

        String sort
) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;

    public PageRequestParams {
        if (size == 0) {
            size = DEFAULT_SIZE;
        }
    }

    public static PageRequestParams defaults() {
        return new PageRequestParams(DEFAULT_PAGE, DEFAULT_SIZE, null);
    }
}
