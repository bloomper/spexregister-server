package nu.fgv.register.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "spexregister")
@Getter
@Setter
public class SpexregisterConfig {

    private String defaultLanguage;
    private List<String> languages;

    SpexregisterConfig() {
        this.languages = new ArrayList<>();
    }

}
