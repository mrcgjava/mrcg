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

public class JavaSetter extends JavaMethod {
	public JavaSetter(JavaField field) {
		setReturnType(JavaType.VOID);
		setName(getSetterName(field));
		
		JavaParameter p = new JavaParameter();
			p.setName(field.getName());
			p.setType(field.getType());
		getParameters().add(p);
		
		setBody("\t\tthis." + field.getName() + " = " + field.getName() + ";");
	}
	
	public static String getSetterName(JavaField field) {
		return getSetterName(field.getType(), field.getName());	
	}

	public static String getSetterName(JavaType type, String fieldName) {
		return "set" + Utils.camelCase(fieldName);
	}
	
}