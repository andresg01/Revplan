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
 * Excepción de recurso no encontrado (HTTP 404) alineada con el contrato de errores.
 * Usa {@link ErrorCode} igual que {@link BusinessException}.
 *
 * <p>El {@code ErrorCode} recomendado suele ser NOT_FOUND o similar en tu enum.</p>
 */
@Getter
@ToString(callSuper = true)
public class NotFoundException extends BaseException {

    private static final long serialVersionUID = -5912345678901234567L;

    private final String description;

    /**
     * Crea una NotFoundException con el {@link ErrorCode} indicado.
     * La descripción por defecto será {@code errorCode.getMessage()}.
     */
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
        this.description = errorCode.getMessage();
    }

    /**
     * Crea una NotFoundException con el {@link ErrorCode} indicado y descripción personalizada.
     */
    public NotFoundException(ErrorCode errorCode, String description) {
        super(errorCode);
        this.description = description;
    }
}
