package io.github.rochona.notificationsystem.preference;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreferenceRequestDto {
    @NotNull
    private Boolean emailEnabled;

    @NotNull
    private Boolean smsEnabled;

    @NotNull
    private Boolean pushEnabled;
}
