package nu.fgv.register.server.util.security;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class JwtAuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(SecurityUtil.getCurrentUserSubClaim());
    }

}
