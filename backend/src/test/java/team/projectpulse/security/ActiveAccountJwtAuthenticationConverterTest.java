package team.projectpulse.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * FR-SEC-active-account: a token is honored only while its account exists and is enabled, and every refusal is a
 * {@link DisabledException} so the resource server answers 401 rather than 500.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActiveAccountJwtAuthenticationConverter")
class ActiveAccountJwtAuthenticationConverterTest {

    @Mock
    UserRepository userRepository;


    private ActiveAccountJwtAuthenticationConverter converter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("authorities");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(authorities);
        return new ActiveAccountJwtAuthenticationConverter(delegate, this.userRepository);
    }

    private static Jwt jwtWithUserId(Object userId) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("j.smith@abc.edu")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("authorities", "ROLE_student");
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.build();
    }

    private static Student student(boolean enabled) {
        Student student = new Student("j.smith@abc.edu", "John", "Smith", "j.smith@abc.edu", "123456", enabled, "student");
        student.setId(4);
        return student;
    }

    @Test
    void anEnabledAccountKeepsTheAuthoritiesItsTokenCarries() {
        given(this.userRepository.findById(4)).willReturn(Optional.of(student(true)));

        AbstractAuthenticationToken token = converter().convert(jwtWithUserId(4L));

        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("ROLE_student");
    }

    @Test
    void aDeactivatedAccountIsRefused() {
        given(this.userRepository.findById(4)).willReturn(Optional.of(student(false)));

        assertThatThrownBy(() -> converter().convert(jwtWithUserId(4L))).isInstanceOf(DisabledException.class);
    }

    @Test
    void anAccountThatNoLongerExistsIsRefused() {
        given(this.userRepository.findById(4)).willReturn(Optional.empty());

        assertThatThrownBy(() -> converter().convert(jwtWithUserId(4L))).isInstanceOf(DisabledException.class);
    }

    @Test
    void aTokenWithoutAUserIdIsRefusedWithoutALookup() {
        assertThatThrownBy(() -> converter().convert(jwtWithUserId(null))).isInstanceOf(DisabledException.class);
        verifyNoInteractions(this.userRepository);
    }

    @Test
    void aUserIdThatIsNotANumberIsRefusedWithoutALookup() {
        assertThatThrownBy(() -> converter().convert(jwtWithUserId("four"))).isInstanceOf(DisabledException.class);
        verifyNoInteractions(this.userRepository);
    }

}
