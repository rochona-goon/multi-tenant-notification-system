package io.github.rochona.notificationsystem.preference;

public class PreferenceAlreadyExistsException extends RuntimeException {
    public PreferenceAlreadyExistsException(String message) {
        super(message);
    }
}
