/*******************************************************************************
 * 
 * Autor: Andres Garcia
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revapp.planengine.infra.persistence.jpa.entities;

import java.io.Serializable;

/**
 * Represent a base entity structure
 * 
 * @author Andres Garcia
 * @param <T> Id typ
 */
public interface BaseEntity<T> extends Serializable  {
	/**
	 * Obtiene el ID del objeto.
	 * 
	 * @return ID del objeto
	 */
	T getId();

	/**
	 * Actualiza el ID del objeto.
	 * 
	 * @param id ID del objeto
	 */
	void setId(T id);

	/**
	 * Indica si el objeto ha sido persistido o es nuevo.
	 * 
	 * @return Verdadero si no ha sido persistido
	 */
	default boolean isNew() {
		return getId() == null;
	}

}
