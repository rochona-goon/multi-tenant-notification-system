package io.github.rochona.notificationsystem.preference;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class PreferenceResponseDto {

    private String userId;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean pushEnabled;
    private Instant createdAt;
    private Instant updatedAt;
}
