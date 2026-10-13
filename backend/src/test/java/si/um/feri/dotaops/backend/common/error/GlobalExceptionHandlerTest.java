package si.um.feri.dotaops.backend.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.dotaops.backend.common.validation.ValidationMessages;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ContractTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void validationErrorUsesSharedErrorContract() throws Exception {
        mockMvc.perform(post("/contract-test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "size": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.path").value("/contract-test/validation"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").exists())
                .andExpect(jsonPath("$.errors[0].message").exists())
                .andExpect(jsonPath("$.errors[0].code").exists());
    }

    @Test
    void authenticationErrorUsesSharedErrorContract() throws Exception {
        mockMvc.perform(get("/contract-test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required or invalid."))
                .andExpect(jsonPath("$.path").value("/contract-test/unauthorized"));
    }

    @Test
    void authorizationErrorUsesSharedErrorContract() throws Exception {
        mockMvc.perform(get("/contract-test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource."))
                .andExpect(jsonPath("$.path").value("/contract-test/forbidden"));
    }

    @Test
    void notFoundErrorUsesSharedErrorContract() throws Exception {
        mockMvc.perform(get("/contract-test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Tournament not found for slug 'missing'."))
                .andExpect(jsonPath("$.path").value("/contract-test/not-found"));
    }

    @Test
    void rateLimitErrorUsesSharedErrorContract() throws Exception {
        mockMvc.perform(get("/contract-test/rate-limited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("Too many requests. Try again later."))
                .andExpect(jsonPath("$.path").value("/contract-test/rate-limited"));
    }

    @RestController
    @RequestMapping("/contract-test")
    static class ContractTestController {

        @PostMapping("/validation")
        void validateRequest(@Valid @RequestBody ContractRequest request) {
        }

        @GetMapping("/unauthorized")
        void unauthorized() {
            throw new BadCredentialsException("invalid token");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new AccessDeniedException("missing role");
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Tournament", "slug", "missing");
        }

        @GetMapping("/rate-limited")
        void rateLimited() {
            throw new RateLimitExceededException("Too many requests. Try again later.");
        }
    }

    record ContractRequest(
            @NotBlank(message = ValidationMessages.REQUIRED)
            String name,

            @Min(value = 1, message = ValidationMessages.POSITIVE)
            int size
    ) {
    }
}
