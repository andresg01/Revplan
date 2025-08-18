/*******************************************************************************
 * 
 * Autor: Andres Garcia
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revapp.planengine.domain.exceptions;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * Clase para mostrar mensaje de error en la llamada de servicios REST
 * 
 * @author Andres Garcia
 *
 */
@Getter
@Builder
public class ErrorView implements Serializable {

	private static final long serialVersionUID = -8643686987756944916L;

	/**
	 * Mensajes de error
	 */
	@Singular
	private List<ErrorMessage> messages;

}
