package team.projectpulse.system.exception;

/**
 * Thrown when a deactivation names a student who is already deactivated (UC-STU-deactivate-student, extension 3a),
 * so the caller learns it had already happened instead of seeing a second success.
 */
public class StudentAlreadyDeactivatedException extends RuntimeException {

    public StudentAlreadyDeactivatedException() {
        super("This student is already deactivated.");
    }

}
