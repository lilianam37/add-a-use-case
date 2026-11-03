package team.projectpulse.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

/**
 * Refuses a bearer token whose account no longer exists or has been deactivated, so a deactivation takes effect on
 * the account's next request rather than when its token expires (FR-SEC-active-account).
 *
 * <p>The signature and expiry are already verified by the time this runs. It then loads the account named by the
 * token's {@code userId} claim and, if that account is present and enabled, hands the token to the existing
 * {@link JwtAuthenticationConverter}, so authorities are mapped exactly as before.
 *
 * <p><strong>It fails closed with an {@code AuthenticationException} only.</strong> A missing claim, a claim that is
 * not a number, a missing account and a disabled account all throw {@link DisabledException}, which the resource
 * server routes to {@code CustomBearerTokenAuthenticationEntryPoint} as a 401. Any other exception thrown on those
 * inputs would escape the filter chain as a 500. A failing database is not one of those inputs and is left to
 * surface as the server error it is.
 */
@Component
public class ActiveAccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String USER_ID_CLAIM = "userId";

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final UserRepository userRepository;


    public ActiveAccountJwtAuthenticationConverter(JwtAuthenticationConverter jwtAuthenticationConverter, UserRepository userRepository) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer userId = readUserId(jwt);
        if (userId == null) {
            throw new DisabledException("The access token does not name an account.");
        }
        boolean active = this.userRepository.findById(userId)
                .map(PeerEvaluationUser::isEnabled)
                .orElse(false);
        if (!active) {
            throw new DisabledException("The account is deactivated or no longer exists.");
        }
        return this.jwtAuthenticationConverter.convert(jwt);
    }

    private static Integer readUserId(Jwt jwt) {
        Object claim = jwt.getClaims().get(USER_ID_CLAIM);
        if (claim instanceof Integer i) {
            return i;
        }
        if (claim instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return l.intValue();
        }
        if (claim instanceof String s) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

}
