package io.github.rochona.notificationsystem.preference;

public class PreferenceDoesNotExistException extends RuntimeException {
    public PreferenceDoesNotExistException(String message) {
        super(message);
    }
}
