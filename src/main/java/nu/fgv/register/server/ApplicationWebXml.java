package nu.fgv.register.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.util.HashMap;
import java.util.Map;

public class ApplicationWebXml extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        addDefaultProfile(application.application());
        return application.sources(Application.class);
    }

    private void addDefaultProfile(SpringApplication app) {
        final Map<String, Object> defaultProperties = new HashMap<>();
        defaultProperties.put("spring.profiles.default", "dev");
        app.setDefaultProperties(defaultProperties);
    }
}

