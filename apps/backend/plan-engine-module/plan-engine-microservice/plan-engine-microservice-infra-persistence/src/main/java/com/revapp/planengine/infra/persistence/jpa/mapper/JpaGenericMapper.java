/*******************************************************************************
 * 
 * Autor: Andres Garcia
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revapp.planengine.infra.persistence.jpa.mapper;

import java.util.List;

/**
 * Generic interface of mappers between domain and mysql infra
 * 
 * @author Andres Garcia
 *
 * @param <S>
 * @param <T>
 */
public interface JpaGenericMapper<S, T> {

	T toDomainModel(S s);

	S toEntity(T t);

	List<S> toEntities(List<T> tList);

	List<T> toDomainModelList(List<S> sList);

}
