package si.um.feri.dotaops.backend.audit.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditJsonSanitizerTest {

    private final AuditJsonSanitizer sanitizer = new AuditJsonSanitizer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void recursivelyRedactsSensitiveFieldsAndDropsFieldsOutsideProjection() {
        var sanitized = objectMapper.valueToTree(sanitizer.projectSanitizedObject(
                """
                {
                  "description": {
                    "label": "safe",
                    "password": "hidden",
                    "nested": {
                      "provider_token": "hidden",
                      "accessToken": "hidden",
                      "metadata": {
                        "steam_api_key": "hidden",
                        "public": "visible"
                      }
                    },
                    "entries": [
                      {"webhook_url": "hidden"},
                      {"name": "visible"}
                    ]
                  },
                  "raw_response": {
                    "access_token": "must-not-be-returned"
                  }
                }
                """,
                Set.of("description")));

        assertThat(sanitized.path("description").path("label").asText()).isEqualTo("safe");
        assertThat(sanitized.path("description").path("password").asText()).isEqualTo("[REDACTED]");
        assertThat(sanitized.path("description").path("nested").path("provider_token").asText())
                .isEqualTo("[REDACTED]");
        assertThat(sanitized.path("description").path("nested").path("accessToken").asText())
                .isEqualTo("[REDACTED]");
        assertThat(sanitized.path("description").path("nested").path("metadata").path("steam_api_key").asText())
                .isEqualTo("[REDACTED]");
        assertThat(sanitized.path("description").path("nested").path("metadata").path("public").asText())
                .isEqualTo("visible");
        assertThat(sanitized.path("description").path("entries").path(0).path("webhook_url").asText())
                .isEqualTo("[REDACTED]");
        assertThat(sanitized.has("raw_response")).isFalse();
    }

    @Test
    void projectsOnlyAllowlistedTopLevelFieldsAndNormalizesSensitiveNames() {
        var projected = sanitizer.projectSanitizedObject(
                """
                {
                  "safe": "kept",
                  "authorization": "top-level secret is omitted unless allowed",
                  "details": {
                    "APIKey": "hidden",
                    "x-Refresh-Token": "hidden",
                    "accountPassword": "hidden",
                    "accountSecret": "hidden",
                    "accountSession": "hidden",
                    "accountWebhook": "hidden",
                    "accountApiKey": "hidden",
                    "note": "public"
                  }
                }
                """,
                Set.of("safe", "details"));

        assertThat(projected).containsOnlyKeys("safe", "details");
        assertThat(projected.get("safe")).isEqualTo("kept");
        assertThat(projected.get("details")).isEqualTo(Map.of(
                "APIKey", "[REDACTED]",
                "x-Refresh-Token", "[REDACTED]",
                "accountPassword", "[REDACTED]",
                "accountSecret", "[REDACTED]",
                "accountSession", "[REDACTED]",
                "accountWebhook", "[REDACTED]",
                "accountApiKey", "[REDACTED]",
                "note", "public"));
    }

    @Test
    void malformedOrNonObjectInputsReturnEmptyProjectionWhileBlankAndNullReturnNull() {
        assertThat(sanitizer.projectSanitizedObject(null, Set.of("value"))).isNull();
        assertThat(sanitizer.projectSanitizedObject("  \n", Set.of("value"))).isNull();
        assertThat(sanitizer.projectSanitizedObject("{broken", Set.of("value"))).isEmpty();
        assertThat(sanitizer.projectSanitizedObject("null", Set.of("value"))).isEmpty();
        assertThat(sanitizer.projectSanitizedObject("[1,2]", Set.of("value"))).isEmpty();
        assertThat(sanitizer.projectSanitizedObject("42", Set.of("value"))).isEmpty();
    }

    @Test
    void nestedArraysPreserveScalarAndNullShapesWhileRedactingObjectMembers() {
        var projected = sanitizer.projectSanitizedObject(
                """
                {"payload": ["text", 7, true, null, {"token": "hidden", "ok": false}, [3, null]]}
                """,
                Set.of("payload"));

        assertThat(projected.get("payload")).isEqualTo(java.util.Arrays.asList(
                "text",
                7,
                true,
                null,
                Map.of("token", "[REDACTED]", "ok", false),
                java.util.Arrays.asList(3, null)));
    }

    @Test
    void returnedProjectionAndNestedObjectsAreImmutable() {
        var projected = sanitizer.projectSanitizedObject(
                "{\"details\":{\"name\":\"visible\"}}",
                Set.of("details"));

        assertThatThrownBy(() -> projected.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ((Map<String, Object>) projected.get("details")).put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
