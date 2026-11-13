package io.github.rochona.notificationsystem.shared;

import io.github.rochona.notificationsystem.notification.exception.NotificationNotDeletableException;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotFoundException;
import io.github.rochona.notificationsystem.preference.PreferenceAlreadyExistsException;
import io.github.rochona.notificationsystem.preference.PreferenceDoesNotExistException;
import io.github.rochona.notificationsystem.template.exception.TemplateAlreadyActiveException;
import io.github.rochona.notificationsystem.template.exception.TemplateAlreadyInactiveException;
import io.github.rochona.notificationsystem.template.exception.TemplateNameAlreadyExistsException;
import io.github.rochona.notificationsystem.template.exception.TemplateNotFoundException;
import io.github.rochona.notificationsystem.tenant.exception.InvalidApiKeyException;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.springframework.web.method.annotation.HandlerMethodValidationException;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ****************** TENANT ************************ **/

    @ExceptionHandler(InvalidApiKeyException.class)
    public ResponseEntity<String> handleInvalidApiKeyException(InvalidApiKeyException exception){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(exception.getMessage());
    }



    /* ****************** NOTIFICATION ************************ **/

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<String> handleNotificationNotFoundException(NotificationNotFoundException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(NotificationNotDeletableException.class)
    public ResponseEntity<String> handleNotificationNotDeletableException(NotificationNotDeletableException exception){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }



    /* ****************** TEMPLATE ************************ **/

    @ExceptionHandler(TemplateAlreadyActiveException.class)
    public ResponseEntity<String> handleTemplateAlreadyActiveException(TemplateAlreadyActiveException exception){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(TemplateAlreadyInactiveException.class)
    public ResponseEntity<String> handleTemplateAlreadyInactiveException(TemplateAlreadyInactiveException exception){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(TemplateNameAlreadyExistsException.class)
    public ResponseEntity<String> handleTemplateNameAlreadyExistsException(TemplateNameAlreadyExistsException exception){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<String> handleTemplateNotFoundException(TemplateNotFoundException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }



    /* ****************** PREFERENCE ********************* **/

    @ExceptionHandler(PreferenceAlreadyExistsException.class)
    public ResponseEntity<String> handlePreferenceAlreadyExistsException(PreferenceAlreadyExistsException exception){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(PreferenceDoesNotExistException.class)
    public ResponseEntity<String> handlePreferenceDoesNotExistException(PreferenceDoesNotExistException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }



    /* ****************** GENERIC ************************ **/

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException exception){
    String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .orElse("Invalid Request");

    return ResponseEntity.badRequest().body(message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpectedException(Exception exception){

        log.error("Unexpected error occurred", exception);

        return ResponseEntity.internalServerError().body("Unexpected error occurred");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<String> handleMethodValidationException(
            HandlerMethodValidationException exception) {
        return ResponseEntity.badRequest().body("Invalid request parameters");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleOptimisticLockingException(
            ObjectOptimisticLockingFailureException exception) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("This resource was modified by another request. " + "Please retrieve the latest version and retry.");
    }
}
