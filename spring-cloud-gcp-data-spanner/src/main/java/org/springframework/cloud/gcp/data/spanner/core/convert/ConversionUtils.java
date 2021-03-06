/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.lang.reflect.Array;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.ByteArray;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class ConversionUtils {

	public static Class boxIfNeeded(Class propertyType) {
		if (propertyType == null) {
			return null;
		}
		return propertyType.isPrimitive()
				? Array.get(Array.newInstance(propertyType, 1), 0).getClass()
				: propertyType;
	}

	public static boolean isIterableNonByteArrayType(Class propType) {
		return Iterable.class.isAssignableFrom(propType) && !ByteArray.class.isAssignableFrom(propType);
	}

	static Iterable convertIterable(
			Iterable source, Class targetType, AbstractSpannerCustomConverter converter) {
		return (Iterable) StreamSupport.stream(source.spliterator(), false)
				.map(item -> converter.convert(item, targetType))
				.collect(Collectors.toList());
	}
}
