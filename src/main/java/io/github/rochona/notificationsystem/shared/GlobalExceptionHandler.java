package io.github.rochona.notificationsystem.shared;

import io.github.rochona.notificationsystem.notification.exception.NotificationNotDeletableException;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotFoundException;
import io.github.rochona.notificationsystem.template.exception.TemplateAlreadyActiveException;
import io.github.rochona.notificationsystem.template.exception.TemplateAlreadyInactiveException;
import io.github.rochona.notificationsystem.template.exception.TemplateNameAlreadyExistsException;
import io.github.rochona.notificationsystem.template.exception.TemplateNotFoundException;
import io.github.rochona.notificationsystem.tenant.exception.InvalidApiKeyException;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ****************** TENANT ************************ **/

    @ExceptionHandler(InvalidApiKeyException.class)
    public ResponseEntity<String> handleInvalidApiKeyException(InvalidApiKeyException exception){
        String message = exception.getMessage();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
    }



    /* ****************** NOTIFICATION ************************ **/

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<String> handleNotificationNotFoundException(NotificationNotFoundException exception){
        String  message = exception.getMessage();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
    }

    @ExceptionHandler(NotificationNotDeletableException.class)
    public ResponseEntity<String> handleNotificationNotDeletableException(NotificationNotDeletableException exception){
        String  message = exception.getMessage();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }



    /* ****************** TEMPLATE ************************ **/

    @ExceptionHandler(TemplateAlreadyActiveException.class)
    public ResponseEntity<String> handleTemplateAlreadyActiveException(TemplateAlreadyActiveException exception){
        String  message = exception.getMessage();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }

    @ExceptionHandler(TemplateAlreadyInactiveException.class)
    public ResponseEntity<String> handleTemplateAlreadyInactiveException(TemplateAlreadyInactiveException exception){
        String  message = exception.getMessage();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }

    @ExceptionHandler(TemplateNameAlreadyExistsException.class)
    public ResponseEntity<String> handleTemplateNameAlreadyExistsException(TemplateNameAlreadyExistsException exception){
        String  message = exception.getMessage();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<String> handleTemplateNotFoundException(TemplateNotFoundException exception){
        String  message = exception.getMessage();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
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
}
