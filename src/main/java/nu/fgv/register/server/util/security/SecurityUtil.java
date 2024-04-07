package nu.fgv.register.server.util.security;

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SecurityUtil {

    public static final String ROLE_ADMIN = "ROLE_spexregister_ADMIN";
    public static final String ROLE_EDITOR = "ROLE_spexregister_EDITOR";
    public static final String ROLE_USER = "ROLE_spexregister_USER";
    public static final List<String> ROLES = List.of(ROLE_ADMIN, ROLE_EDITOR, ROLE_USER);
    public static final GrantedAuthoritySid ROLE_ADMIN_SID = new GrantedAuthoritySid(ROLE_ADMIN);
    public static final GrantedAuthoritySid ROLE_EDITOR_SID = new GrantedAuthoritySid(ROLE_EDITOR);
    public static final GrantedAuthoritySid ROLE_USER_SID = new GrantedAuthoritySid(ROLE_USER);
    public static final List<GrantedAuthoritySid> ROLE_SIDS = List.of(ROLE_ADMIN_SID, ROLE_EDITOR_SID, ROLE_USER_SID);

    private SecurityUtil() {
    }

    public static String getCurrentUserSubClaim() {
        return getCurrentUserClaim(JwtClaimNames.SUB)
                .map(String.class::cast)
                .orElse(null);
    }

    public static String getCurrentUserEmailClaim() {
        return getCurrentUserClaim("email")
                .map(String.class::cast)
                .orElse(null);
    }

    public static ObjectIdentity toObjectIdentity(final Class<?> clazz, final Serializable id) {
        return new ObjectIdentityImpl(clazz, id);
    }

    private static Optional<Object> getCurrentUserClaim(final String claim) {
        return getJwtToken()
                .filter(token -> token.getClaims() != null)
                .map(token -> token.getClaims()
                        .entrySet()
                        .stream()
                        .filter(c -> claim.equals(c.getKey()))
                        .findFirst()
                        .map(Map.Entry::getValue)
                )
                .filter(Optional::isPresent)
                .flatMap(o -> o);
    }

    private static Optional<Jwt> getJwtToken() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast);
    }

}
