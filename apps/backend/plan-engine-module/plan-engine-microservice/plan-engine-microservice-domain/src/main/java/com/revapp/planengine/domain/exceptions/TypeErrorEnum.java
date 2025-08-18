/*******************************************************************************
 * 
 * Autor: Andres Garcia
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revapp.planengine.domain.exceptions;

/**
 * Error message severity.
 * 
 * @author  Andres Garcia
 *
 */
public enum TypeErrorEnum {
	/** error that prevents the operation of the entire application. */
	CRITICAL,
	/** error that prevents the operation of a specific functionality */
	FATAL,
	/** error that does not affect more features */
	ERROR,
	/** application works but a warning has been generated */
	WARNING,
	/** operative info message */
	INFO

}
