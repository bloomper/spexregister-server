package nu.fgv.register.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {
}
