package team.projectpulse.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * The course-admin scope check behind UC-STU-deactivate-student's route guard (BR-student-lifecycle): the owning
 * course admin is admitted even when she is not assigned to the student's course section; anyone else, and any id
 * that names no student, is refused.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentSecurityService")
class StudentSecurityServiceTest {

    @Mock
    StudentRepository studentRepository;

    @Mock
    UserUtils userUtils;

    @InjectMocks
    StudentSecurityService studentSecurityService;

    Student jana;


    @BeforeEach
    void setUp() {
        Instructor tim = new Instructor("t.cook@abc.edu", "Tim", "Cook", "t.cook@abc.edu", "123456", true, "admin instructor");
        tim.setId(3);

        Course course = new Course("EE 30323 Capstone", "Capstone project course for EE majors");
        course.setCourseAdmin(tim);

        // Tim administers the course but is not assigned to this course section
        Section section = new Section();
        course.addSection(section);

        this.jana = new Student("j.norton@abc.edu", "Jana", "Norton", "j.norton@abc.edu", "123456", true, "student");
        this.jana.setId(20);
        section.addStudent(this.jana);
    }

    @Test
    void theOwningCourseAdminIsAdmittedWithoutACourseSectionAssignment() {
        given(this.userUtils.getUserId()).willReturn(3);
        given(this.studentRepository.findById(20)).willReturn(Optional.of(this.jana));

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(20)).isTrue();
        assertThat(this.studentSecurityService.isCurrentUserInstructorOfStudentSection(20)).isFalse();
    }

    @Test
    void anotherCoursesAdminIsRefused() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(20)).willReturn(Optional.of(this.jana));

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(20)).isFalse();
    }

    @Test
    void anIdNamingNoStudentIsRefused() {
        given(this.userUtils.getUserId()).willReturn(3);
        given(this.studentRepository.findById(999)).willReturn(Optional.empty());

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(999)).isFalse();
    }

}
