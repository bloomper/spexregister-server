package nu.fgv.register.server.config;

import lombok.Getter;
import lombok.Setter;
import nu.fgv.register.server.config.model.PasswordEncoding;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "spexregister")
@Getter
@Setter
public class SpexregisterConfig {

    private String defaultLanguage;
    private List<String> languages;
    private String defaultPasswordEncoderPrefix;
    private Map<String, PasswordEncoding> passwordEncodings;

    SpexregisterConfig() {
        this.languages = new ArrayList<>();
        this.passwordEncodings = new HashMap<>();
    }

}
