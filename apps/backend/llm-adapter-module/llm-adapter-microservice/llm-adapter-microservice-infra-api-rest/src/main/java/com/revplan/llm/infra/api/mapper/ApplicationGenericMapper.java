/*******************************************************************************
 * 
 * Autor: agarciab
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revplan.llm.infra.api.mapper;

import java.util.List;

/**
 * Generic interface of mappers between domain and DTO Models
 * 
 * @author agarciab
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
