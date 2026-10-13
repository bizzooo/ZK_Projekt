package si.um.feri.dotaops.backend.auth.steam.service;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import si.um.feri.dotaops.backend.config.properties.SteamAuthProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

class SteamOpenIdClientTest {

    private static final String STEAM_ID = "76561198000000001";

    @Test
    void verifyAuthenticationPostsCheckAuthenticationToSteam() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SteamOpenIdClient client = new SteamOpenIdClient(properties("api-key"), builder);

        server.expect(requestTo("https://steamcommunity.com/openid/login"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("openid.mode=check_authentication")))
                .andExpect(content().string(containsString("openid.claimed_id=https%3A%2F%2Fsteamcommunity.com%2Fopenid%2Fid%2F" + STEAM_ID)))
                .andRespond(withSuccess("""
                        ns:http://specs.openid.net/auth/2.0
                        is_valid:true
                        """, MediaType.TEXT_PLAIN));

        assertThat(client.verifyAuthentication(callbackParams())).isTrue();
        server.verify();
    }

    @Test
    void verifyAuthenticationReturnsFalseForInvalidSteamResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SteamOpenIdClient client = new SteamOpenIdClient(properties("api-key"), builder);

        server.expect(requestTo("https://steamcommunity.com/openid/login"))
                .andExpect(method(POST))
                .andRespond(withSuccess("is_valid:false", MediaType.TEXT_PLAIN));

        assertThat(client.verifyAuthentication(callbackParams())).isFalse();
        server.verify();
    }

    @Test
    void fetchPlayerSummaryParsesSteamWebApiResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SteamOpenIdClient client = new SteamOpenIdClient(properties("api-key"), builder);

        server.expect(requestTo("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=api-key&steamids=" + STEAM_ID + "&format=json"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "players": [
                              {
                                "steamid": "76561198000000001",
                                "personaname": "Dota Player",
                                "avatarfull": "https://cdn.example.test/avatar.png",
                                "profileurl": "https://steamcommunity.com/profiles/76561198000000001/"
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var summary = client.fetchPlayerSummary(STEAM_ID);

        assertThat(summary).isPresent();
        assertThat(summary.orElseThrow().personaName()).isEqualTo("Dota Player");
        assertThat(summary.orElseThrow().avatarUrl()).isEqualTo("https://cdn.example.test/avatar.png");
        assertThat(summary.orElseThrow().profileUrl()).isEqualTo("https://steamcommunity.com/profiles/76561198000000001/");
        server.verify();
    }

    @Test
    void fetchPlayerSummarySkipsCallWithoutSteamApiKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SteamOpenIdClient client = new SteamOpenIdClient(properties(""), builder);

        assertThat(client.fetchPlayerSummary(STEAM_ID)).isEmpty();
        server.verify();
    }

    private static MultiValueMap<String, String> callbackParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("openid.mode", "id_res");
        params.add("openid.claimed_id", "https://steamcommunity.com/openid/id/" + STEAM_ID);
        return params;
    }

    private static SteamAuthProperties properties(String apiKey) {
        return new SteamAuthProperties(
                "https://steamcommunity.com/openid/login",
                "http://localhost:8080",
                "http://localhost:8080/api/auth/steam/callback",
                apiKey,
                "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/",
                "",
                Duration.ofMinutes(10));
    }
}
