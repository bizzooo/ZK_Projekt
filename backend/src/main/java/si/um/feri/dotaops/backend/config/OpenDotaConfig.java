package si.um.feri.dotaops.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import si.um.feri.dotaops.backend.config.properties.OpenDotaProperties;

@Configuration
@EnableConfigurationProperties(OpenDotaProperties.class)
public class OpenDotaConfig {
}
