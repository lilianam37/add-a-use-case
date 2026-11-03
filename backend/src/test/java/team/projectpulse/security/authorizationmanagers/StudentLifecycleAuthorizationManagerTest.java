package team.projectpulse.security.authorizationmanagers;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import team.projectpulse.student.StudentSecurityService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * UC-STU-deactivate-student's route guard: an instructor caller passes when she teaches the student's course section
 * or administers the student's course; a student caller and an unreadable id are refused before any lookup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentLifecycleAuthorizationManager")
class StudentLifecycleAuthorizationManagerTest {

    @Mock
    StudentSecurityService studentSecurityService;

    @InjectMocks
    StudentLifecycleAuthorizationManager studentLifecycleAuthorizationManager;


    private static RequestAuthorizationContext contextWith(Map<String, String> variables) {
        return new RequestAuthorizationContext(mock(HttpServletRequest.class), variables);
    }

    private static Authentication withAuthorities(String... authorities) {
        return new TestingAuthenticationToken("caller", null, authorities);
    }

    private boolean decide(Authentication caller, Map<String, String> variables) {
        return this.studentLifecycleAuthorizationManager.authorize(() -> caller, contextWith(variables)).isGranted();
    }

    @Test
    void anInstructorOfTheStudentsCourseSectionIsAdmitted() {
        given(this.studentSecurityService.isCurrentUserInstructorOfStudentSection(4)).willReturn(true);

        assertThat(decide(withAuthorities("ROLE_instructor"), Map.of("studentId", "4"))).isTrue();
    }

    @Test
    void theCourseAdminOfTheStudentsCourseIsAdmitted() {
        given(this.studentSecurityService.isCurrentUserInstructorOfStudentSection(4)).willReturn(false);
        given(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).willReturn(true);

        assertThat(decide(withAuthorities("ROLE_admin", "ROLE_instructor"), Map.of("studentId", "4"))).isTrue();
    }

    @Test
    void anInstructorWithNeitherScopeIsRefused() {
        given(this.studentSecurityService.isCurrentUserInstructorOfStudentSection(4)).willReturn(false);
        given(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).willReturn(false);

        assertThat(decide(withAuthorities("ROLE_instructor"), Map.of("studentId", "4"))).isFalse();
    }

    @Test
    void aStudentIsRefusedBeforeAnyLookup() {
        assertThat(decide(withAuthorities("ROLE_student"), Map.of("studentId", "4"))).isFalse();
        verifyNoInteractions(this.studentSecurityService);
    }

    @Test
    void anUnreadableIdIsRefusedBeforeAnyLookup() {
        assertThat(decide(withAuthorities("ROLE_instructor"), Map.of("studentId", "four"))).isFalse();
        verifyNoInteractions(this.studentSecurityService);
    }

}
