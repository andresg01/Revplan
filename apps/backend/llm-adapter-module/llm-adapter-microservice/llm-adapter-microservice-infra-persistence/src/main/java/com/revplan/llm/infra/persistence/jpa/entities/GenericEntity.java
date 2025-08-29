/*******************************************************************************
 *
 * Autor: agarciab
 *
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 *
 ******************************************************************************/

package com.revplan.llm.infra.persistence.jpa.entities;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class GenericEntity<T> implements BaseEntity<T> {

	private static final long serialVersionUID = 1L;
	
	// Default structure of a generic entity: To define

}