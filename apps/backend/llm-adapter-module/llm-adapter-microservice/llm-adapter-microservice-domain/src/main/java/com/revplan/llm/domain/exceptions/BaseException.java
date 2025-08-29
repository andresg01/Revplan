/*******************************************************************************
 * 
 * Autor: agarciab
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revplan.llm.domain.exceptions;

import com.revplan.llm.domain.enums.ErrorCode;

import lombok.ToString;

/**
 * Generic exception for the module
 * 
 * @author agarciab
 */
@ToString
public class BaseException extends RuntimeException {

    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = -6788661443546955762L;

    /**
     * The error code.
     */
    private final ErrorCode errorCode;

    /**
     * <p>
     * Constructor for OrangeBankException.
     * </p>
     *
     * @param errorCode a {@link String} object.
     */
    public BaseException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * <p>
     * Constructor for OrangeBankException (errorCode, message)
     * </p>.
     *
     * @param errorCode a {@link String} string.
     * @param message   a {@link String} message.
     */
    public BaseException(ErrorCode errorCode, String message) {
        super(message, null, false, false);
        this.errorCode = errorCode;
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return a {@link Long} object.
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

}
