package team.projectpulse.security.authorizationmanagers;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import team.projectpulse.student.StudentSecurityService;

import java.util.function.Supplier;

/**
 * Guards changing a student's lifecycle state (UC-STU-deactivate-student), admitting an instructor of that student's
 * course section or the course admin of that student's course (BR-student-lifecycle, BR-section-scoped-access).
 *
 * <p>Unlike {@link UserOwnershipAuthorizationManager} it never admits the student herself, and it admits a course
 * admin who is not assigned to the student's course section. The caller must carry {@code ROLE_instructor}, which
 * every course admin also carries. Reads {@code studentId}; an id it cannot read, or one that names no student, is
 * denied.
 */
@Component
public class StudentLifecycleAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final StudentSecurityService studentSecurityService;


    public StudentLifecycleAuthorizationManager(StudentSecurityService studentSecurityService) {
        this.studentSecurityService = studentSecurityService;
    }

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
        Authentication caller = authentication.get();
        boolean isInstructor = caller != null && caller.getAuthorities().stream()
                .anyMatch(grantedAuthority -> "ROLE_instructor".equals(grantedAuthority.getAuthority()));
        if (!isInstructor) {
            return new AuthorizationDecision(false);
        }
        Integer studentId = PathVariables.readId(context, "studentId");
        if (studentId == null) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(
                this.studentSecurityService.isCurrentUserInstructorOfStudentSection(studentId)
                        || this.studentSecurityService.isCurrentUserAdminOfStudentCourse(studentId));
    }

}
