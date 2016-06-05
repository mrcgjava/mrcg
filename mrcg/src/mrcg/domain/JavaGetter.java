package mrcg.domain;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import mrcg.utils.Utils;

public class JavaGetter extends JavaMethod {

	public JavaGetter(JavaField field) {
		this(field, (field.getType().isBoolean()?"is":"get"));
	}

	public JavaGetter(JavaField field, String beginWith) {
		setReturnType(field.getType());		
		setName(beginWith + Utils.camelCase(field.getName()));
		setBody("\t\treturn " + field.getName() + ";");
	}
	
	public static String getGetterName(JavaField field) {
		return getGetterName(field.getType(), field.getName());	
	}

	public static String getGetterName(JavaType type, String fieldName) {
		return (type.isBoolean()?"is":"get") + Utils.camelCase(fieldName);
	}
}