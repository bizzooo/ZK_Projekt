package si.um.feri.dotaops.backend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import si.um.feri.dotaops.backend.config.properties.IntegrationHttpProperties;

@Configuration
@EnableConfigurationProperties(IntegrationHttpProperties.class)
public class SteamHttpClientConfig {

    @Bean
    @Primary
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Qualifier("steamRestClientBuilder")
    RestClient.Builder steamRestClientBuilder(IntegrationHttpProperties properties) {
        return RestClient.builder()
                .requestFactory(requestFactory(properties.steam()));
    }

    @Bean
    @Qualifier("openDotaRestClientBuilder")
    RestClient.Builder openDotaRestClientBuilder(IntegrationHttpProperties properties) {
        return RestClient.builder()
                .requestFactory(requestFactory(properties.opendota()));
    }

    static SimpleClientHttpRequestFactory requestFactory(IntegrationHttpProperties.Client properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return requestFactory;
    }
}
