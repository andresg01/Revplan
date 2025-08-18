/*******************************************************************************
 * 
 * Autor: Andres Garcia
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revapp.planengine.infra.api.mapper;

import java.util.List;

/**
 * Generic interface of mappers between domain and DTO Models
 * 
 * @author Andres Garcia
 *
 * @param <S>
 * @param <T>
 */
public interface ApplicationGenericMapper<S, T> {

	T toDTO(S s);

	S toModel(T t);

	List<T> toDTOList(List<S> sList);

	List<S> toModelList(List<T> tList);

}
