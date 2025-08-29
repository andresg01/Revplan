package com.revplan.llm.infra.persistence.jpa.mapper;

import java.util.List;

/**
 * Generic interface of mappers between domain and mysql infra
 * 
 * @author agarciab
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
