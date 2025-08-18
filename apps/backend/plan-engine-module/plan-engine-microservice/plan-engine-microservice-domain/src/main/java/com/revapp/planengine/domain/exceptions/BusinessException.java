/*******************************************************************************
 * 
 * Autor: Andres Garcia
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revapp.planengine.domain.exceptions;

import com.revapp.planengine.domain.enums.ErrorCode;

import lombok.Getter;
import lombok.ToString;

/**
 * Generic Business exception for the module
 * 
 * @author Andres Garcia
 */
@Getter
@ToString(callSuper = true)
public class BusinessException extends BaseException {

	private static final long serialVersionUID = -2634207060501411224L;

	private String description;

	/**
	 * Constructor for OrangeBankBusinessException.
	 *
	 * @param errorCode a {@link String} object.
	 */
	public BusinessException(ErrorCode errorCode) {
		super(errorCode);
		this.description = errorCode.getMessage();
	}

	/**
	 * Constructor for OrangeBankBusinessException.
	 *
	 * @param errorCode a {@link String} object.
	 */
	public BusinessException(ErrorCode errorCode, String description) {
		super(errorCode);
		this.description = description;
	}

}
