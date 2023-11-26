package nu.fgv.register.server.util.security;

import com.nimbusds.jwt.JWTClaimNames;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.Optional;

public class SecurityUtil {

    public static final GrantedAuthoritySid ROLE_ADMIN_SID = new GrantedAuthoritySid("ROLE_ADMIN");
    public static final GrantedAuthoritySid ROLE_EDITOR_SID = new GrantedAuthoritySid("ROLE_EDITOR");
    public static final GrantedAuthoritySid ROLE_USER_SID = new GrantedAuthoritySid("ROLE_USER");

    private SecurityUtil() {
    }

    private static Optional<Jwt> getJwtToken() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(token -> token instanceof Jwt)
                .map(Jwt.class::cast);
    }

    public static String getCurrentUserClaim() {
        return getJwtToken()
                .filter(token -> token.getClaims() != null)
                .map(token -> token.getClaims()
                        .entrySet()
                        .stream()
                        .filter(c -> JWTClaimNames.SUBJECT.equals(c.getKey()))
                        .findFirst()
                        .map(Map.Entry::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(String.class::cast)
                .orElse("system");
    }

    public static ObjectIdentity toObjectIdentity(final Class<?> clazz, final Long id) {
        return new ObjectIdentityImpl(clazz, id);
    }

}
