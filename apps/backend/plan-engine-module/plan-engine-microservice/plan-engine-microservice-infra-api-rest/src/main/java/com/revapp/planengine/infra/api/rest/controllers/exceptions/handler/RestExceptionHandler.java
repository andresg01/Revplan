/*******************************************************************************
 *
 * Autor: Andres Garcia
 *
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 *
 ******************************************************************************/

package com.revapp.planengine.infra.api.rest.controllers.exceptions.handler;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.exceptions.NotFoundException;
import com.revapp.planengine.infra.api.dto.ErrorDTO;
import com.revapp.planengine.infra.api.dto.ErrorMessagesInnerDTO;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global Exception Handler para la API REST.
 * Devuelve siempre ErrorDTO con lista de ErrorMessagesInnerDTO.
 */
@ControllerAdvice(basePackages = "com.revapp.planengine.infra.api.rest.controllers")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    /* ========= HANDLERS SPRING / VALIDACIÓN ========= */

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        StringBuilder description = new StringBuilder("Validation failed for fields: ");
        errors.forEach((field, message) -> description.append(field).append(": ").append(message).append("; "));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                description.toString()
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        log.debug("HttpMessageNotReadableException: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MALFORMED_JSON,
                "The request body is invalid or cannot be parsed. Ensure JSON format and data types are correct."
        );
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MISSING_PARAMETER,
                "Missing required parameter: " + ex.getParameterName()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                ex.getMessage() != null ? ex.getMessage() : "Constraint violation occurred."
        );
    }

    /* ========= HANDLERS DE TU DOMINIO ========= */

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        HttpStatus status = mapErrorCodeToHttpStatus(code);
        String description = ex.getDescription() != null ? ex.getDescription() : code.getMessage();

        log.warn("BusinessException -> status={}, code={}, desc={}", status, code, description);
        return buildErrorResponse(status, code, description);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
        ErrorCode code = ex.getErrorCode() != null ? ex.getErrorCode() : ErrorCode.NOT_FOUND;
        String description = ex.getMessage() != null ? ex.getMessage() : code.getMessage();

        log.warn("NotFoundException -> code={}, desc={}", code, description);
        return buildErrorResponse(HttpStatus.NOT_FOUND, code, description);
    }

    /* ========= OTROS ERRORES COMUNES ========= */

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("DataIntegrityViolationException", ex);
        // Suele indicar conflictos de unicidad/foreign keys → 409
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ErrorCode.INTEGRITY_VIOLATION,
                "Data integrity violation: " + safeMessage(ex.getMostSpecificCause())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("IllegalArgumentException: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.ILLEGAL_ARGUMENT,
                ex.getMessage() != null ? ex.getMessage() : ErrorCode.ILLEGAL_ARGUMENT.getMessage()
        );
    }

    /* ========= CATCH-ALL ========= */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred."
        );
    }

    /* ========= HELPERS ========= */

    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, ErrorCode errorCode, String description) {
        ErrorMessagesInnerDTO.TypeEnum severity = severityFor(status);
        ErrorMessagesInnerDTO message = ErrorMessagesInnerDTO.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .type(severity)
                .description(description)
                .build();

        ErrorDTO body = ErrorDTO.builder()
                .messages(new ArrayList<>(java.util.List.of(message)))
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private static ErrorMessagesInnerDTO.TypeEnum severityFor(HttpStatus status) {
        int s = status.value();
        if (s >= 500) return ErrorMessagesInnerDTO.TypeEnum.FATAL;
        if (s == 429) return ErrorMessagesInnerDTO.TypeEnum.WARNING;
        // Para 4xx usamos ERROR por defecto
        return ErrorMessagesInnerDTO.TypeEnum.ERROR;
    }

    private static String safeMessage(Throwable t) {
        return t != null && t.getMessage() != null ? t.getMessage() : "N/A";
    }

    /**
     * Mapeo determinista de ErrorCode → HttpStatus (contrato API).
     */
    private static HttpStatus mapErrorCodeToHttpStatus(ErrorCode code) {
        if (code == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        switch (code) {
            // 400
            case BAD_REQUEST:
            case MISSING_PARAMETER:
            case MALFORMED_JSON:
            case VALIDATION_ERROR:
            case ILLEGAL_ARGUMENT:
            case PARAMS_INVALID:
                return HttpStatus.BAD_REQUEST;
            // 401
            case UNAUTHORIZED:
                return HttpStatus.UNAUTHORIZED;
            // 403
            case FORBIDDEN:
                return HttpStatus.FORBIDDEN;
            // 404
            case NOT_FOUND:
            case TEMPLATE_NOT_FOUND:
            case PLAN_NOT_FOUND:
            case PLAN_VERSION_NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            // 409
            case CONFLICT:
            case DUPLICATE_RESOURCE:
            case VERSION_CONFLICT:
            case STATE_CONFLICT:
            case INTEGRITY_VIOLATION:
                return HttpStatus.CONFLICT;
            // 422
            case UNPROCESSABLE_ENTITY:
            case BUSINESS_RULE_VIOLATED:
            case ENVELOPE_SUM_INVALID:
            case KPIS_INVALID:
            case SOURCE_INVALID:
            case SCHEMA_VIOLATION:
                return HttpStatus.UNPROCESSABLE_ENTITY;
            // 429
            case RATE_LIMIT:
                return HttpStatus.TOO_MANY_REQUESTS;
            // 500
            case INTERNAL_SERVER_ERROR:
            case DATABASE_ERROR:
            case SERIALIZATION_ERROR:
            case DESERIALIZATION_ERROR:
            case MAPPING_ERROR:
            case ILLEGAL_STATE:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            // 502, 503, 504
            case BAD_GATEWAY:
                return HttpStatus.BAD_GATEWAY;
            case SERVICE_UNAVAILABLE:
                return HttpStatus.SERVICE_UNAVAILABLE;
            case GATEWAY_TIMEOUT:
                return HttpStatus.GATEWAY_TIMEOUT;
            // Compatibilidad
            case NOT_EXIST:
            case UNDETERMINED_PRICE:
            case UNKNOWN:
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
