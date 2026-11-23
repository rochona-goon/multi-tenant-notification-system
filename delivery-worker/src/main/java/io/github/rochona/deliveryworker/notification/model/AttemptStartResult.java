package io.github.rochona.deliveryworker.notification.model;

import io.github.rochona.deliveryworker.notification.enums.AttemptStartStatus;

public record AttemptStartResult(AttemptStartStatus status,
                                 Long attemptId) {
}
