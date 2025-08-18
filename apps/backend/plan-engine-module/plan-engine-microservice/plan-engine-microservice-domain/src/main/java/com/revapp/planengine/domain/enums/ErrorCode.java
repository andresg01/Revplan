/*******************************************************************************
 *
 * Autor: Andres Garcia
 *
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 *
 ******************************************************************************/

package com.revapp.planengine.domain.enums;

/**
 * Códigos de error del dominio / API, alineados con las respuestas del contrato
 * OpenAPI (400, 401, 403, 404, 409, 422, 429, 500, 502, 503, 504).
 *
 * <p>Cada código lleva un mensaje por defecto legible (puedes internacionalizarlo
 * en tu {@code @ControllerAdvice} si lo deseas).</p>
 */
public enum ErrorCode {

	// -------- 400 Bad Request --------
	BAD_REQUEST("Bad request"),
	MISSING_PARAMETER("Required parameter is missing"),
	MALFORMED_JSON("Malformed JSON payload"),
	VALIDATION_ERROR("Validation error"),
	ILLEGAL_ARGUMENT("Illegal argument"),
	PARAMS_INVALID("Invalid request parameters"),

	// -------- 401 Unauthorized --------
	UNAUTHORIZED("Missing or invalid credentials"),

	// -------- 403 Forbidden --------
	FORBIDDEN("Forbidden operation"),

	// -------- 404 Not Found --------
	NOT_FOUND("Resource not found"),
	TEMPLATE_NOT_FOUND("Template not found"),
	PLAN_NOT_FOUND("Plan not found"),
	PLAN_VERSION_NOT_FOUND("Plan version not found"),

	// -------- 409 Conflict --------
	CONFLICT("State conflict"),
	DUPLICATE_RESOURCE("Resource already exists"),
	VERSION_CONFLICT("Version conflict"),
	STATE_CONFLICT("Invalid state for operation"),
	INTEGRITY_VIOLATION("Data integrity violation"),

	// -------- 422 Unprocessable Entity --------
	UNPROCESSABLE_ENTITY("Unprocessable entity"),
	BUSINESS_RULE_VIOLATED("Business rule violated"),
	ENVELOPE_SUM_INVALID("Envelopes must sum ~1 (±0.02)"),
	KPIS_INVALID("KPIs payload is invalid"),
	SOURCE_INVALID("Source must be one of the allowed values"),
	SCHEMA_VIOLATION("Template schema violation"),

	// -------- 429 Too Many Requests --------
	RATE_LIMIT("Too many requests"),

	// -------- 500 Internal Server Error --------
	INTERNAL_SERVER_ERROR("Internal server error"),
	DATABASE_ERROR("Database error"),
	SERIALIZATION_ERROR("Serialization error"),
	DESERIALIZATION_ERROR("Deserialization error"),
	MAPPING_ERROR("Object mapping error"),
	ILLEGAL_STATE("Illegal state"),

	// -------- 502 Bad Gateway --------
	BAD_GATEWAY("Bad gateway"),

	// -------- 503 Service Unavailable --------
	SERVICE_UNAVAILABLE("Service unavailable"),

	// -------- 504 Gateway Timeout --------
	GATEWAY_TIMEOUT("Gateway timeout"),

	// -------- Compatibilidad con códigos previos --------
	NOT_EXIST("There are no elements"),
	UNDETERMINED_PRICE("There are more than one price list applicable: Price can't be returned"),
	UNKNOWN("Unknown error");

	/** Código (estable) del error. */
	private final String code;
	/** Mensaje legible por defecto. */
	private final String message;

	ErrorCode(final String message) {
		this.code = this.name();
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return code;
	}
}
