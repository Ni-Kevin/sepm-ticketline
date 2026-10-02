package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;

public interface PasswordResetService {

    void requestPasswordReset(String email, String frontendOrigin);

    void resetPassword(String token, String newPassword) throws ValidationException;
}
