/*******************************************************************************
 * 
 * Autor: agarciab
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revplan.llm.domain.enums;

/**
 * Specific error codes for the Application
 * 
 * @author agarciab
 * 
 */

public enum ErrorCode {
	NOT_EXIST("There are no elements"),
	UNDETERMINED_PRICE("There are more than one price list applicable: Price can't be returned"),
	UNKNOWN("Unknown error");

	/**
	 * Exception code
	 */
	private final String code;

	/**
	 * Message with additional info.
	 */
	private final String message;

	/**
	 * Constructor
	 * <p>
	 * 
	 * @param message Message with additional info.
	 */
	private ErrorCode(final String message) {
		this.code = this.name();
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	/**
	 * ToString() method
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		return code;
	}
}
