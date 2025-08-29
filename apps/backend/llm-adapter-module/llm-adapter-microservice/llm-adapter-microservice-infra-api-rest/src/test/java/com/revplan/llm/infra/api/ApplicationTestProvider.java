/*******************************************************************************
 * 
 * Autor: agarciab
 * 
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 * 
 ******************************************************************************/

package com.revplan.llm.infra.api;

import java.util.ArrayList;
import java.util.List;

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

/**
 * Utility class to build domain beans
 * 
 * @author agarciab
 *
 */
public class ApplicationTestProvider {

	private static PodamFactory FACTORY = new PodamFactoryImpl();

	/**
	 * Build a pojo pojoClass with random values
	 * 
	 * @param pojoClass
	 * @return
	 */
	public static <T> T buildPojo(Class<T> pojoClass) {
		return FACTORY.manufacturePojo(pojoClass);
	}

	/**
	 * Build a pojo list of pojoClass with random values
	 * 
	 * @param pojoClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> buildPojoList(Class<T> pojoClass) {
		return FACTORY.manufacturePojo(ArrayList.class, pojoClass);
	}

	/**
	 * Build a pojo list of pojoClass with random values of a concrete size
	 * 
	 * @param pojoClass
	 * @param size
	 * @return
	 */
	public static <T> List<T> buildPojoList(Class<T> pojoClass, int size) {
		List<T> list = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			list.add(buildPojo(pojoClass));
		}

		return list;
	}
}