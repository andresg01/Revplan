/*******************************************************************************
 * 
 * Autor: agarciab
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revplan.llm.domain.exceptions;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Clase para detalles de un mensaje de error
 * 
 * @author agarciab
 *
 */
@Data
@AllArgsConstructor
@Builder
public class ErrorMessage implements Serializable {

	/** Serial Id */
	private static final long serialVersionUID = 4533482361338877837L;

	/**
	 * This field must be readable (human readable), therefore it should not be a
	 * numeric code, but an alphanumeric one. Obligatory field.
	 */
	private String code;

	/**
	 * Error description
	 */
	private String message;

	/** Error message severity. */
	private TypeErrorEnum type;

	/**
	 * Description whit the error in the field.
	 */
	private String description;

}
