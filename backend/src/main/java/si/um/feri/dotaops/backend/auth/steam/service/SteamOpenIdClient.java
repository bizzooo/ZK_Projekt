package si.um.feri.dotaops.backend.auth.steam.service;

import java.net.URI;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import si.um.feri.dotaops.backend.auth.steam.domain.SteamPlayerSummary;
import si.um.feri.dotaops.backend.config.properties.SteamAuthProperties;

@Component
public class SteamOpenIdClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SteamOpenIdClient.class);

    private final SteamAuthProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SteamOpenIdClient(
            SteamAuthProperties properties,
            @Qualifier("steamRestClientBuilder") RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public boolean verifyAuthentication(MultiValueMap<String, String> callbackParams) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        callbackParams.forEach((key, values) -> {
            if (key.startsWith("openid.") && !values.isEmpty()) {
                form.set(key, values.getFirst());
            }
        });
        form.set("openid.mode", "check_authentication");

        try {
            String body = restClient.post()
                    .uri(properties.openidEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            return body != null && body.lines().anyMatch("is_valid:true"::equals);
        } catch (RestClientException exception) {
            LOGGER.warn("Steam OpenID verification request failed.", exception);
            return false;
        }
    }

    public Optional<SteamPlayerSummary> fetchPlayerSummary(String steamId) {
        if (!StringUtils.hasText(properties.webApiKey())) {
            return Optional.empty();
        }

        URI uri = UriComponentsBuilder.fromUriString(properties.playerSummariesUrl())
                .queryParam("key", properties.webApiKey())
                .queryParam("steamids", steamId)
                .queryParam("format", "json")
                .build()
                .toUri();

        try {
            String body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            JsonNode players = objectMapper.readTree(body).path("response").path("players");
            if (!players.isArray() || players.isEmpty()) {
                return Optional.empty();
            }

            JsonNode player = players.get(0);
            return Optional.of(new SteamPlayerSummary(
                    player.path("steamid").asText(steamId),
                    blankToNull(player.path("personaname").asText(null)),
                    blankToNull(player.path("avatarfull").asText(null)),
                    blankToNull(player.path("profileurl").asText(null))));
        } catch (Exception exception) {
            LOGGER.warn("Steam player summary fetch failed for Steam ID {}.", steamId, exception);
            return Optional.empty();
        }
    }

    private String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
