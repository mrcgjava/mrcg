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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mrcg.utils.Utils;


public class JavaType implements ImportsRequired, Writable {
	public static final JavaType VOID = new JavaType("void");
	public static final JavaType OBJECT = new JavaType("java.lang.Object");
	public static final JavaType DATE_TIME = new JavaType("java.time.LocalDateTime");
	public static final JavaType EMAIL = new JavaType("java.lang.String");
	public static final JavaType STRING = new JavaType("java.lang.String");
	public static final JavaType LONG = new JavaType("java.lang.Long");
	public static final JavaType INTEGER = new JavaType("java.lang.Integer");
	public static final JavaType DOUBLE = new JavaType("java.lang.Double");
	public static final JavaType BOOLEAN = new JavaType("java.lang.Boolean");
	public static final JavaType PBOOLEAN = new JavaType("boolean");
	public static final JavaType PLONG = new JavaType("long");
	
	public static JavaType LOGIC_FACADE;
	
	public static final JavaType SERIALIZABLE = new JavaType("java.io.Serializable");
	
	public static final JavaType LIST = new JavaType("java.util.List");
	public static final JavaType RESULT_SET = new JavaType("java.sql.ResultSet");
	
	public static final JavaType BINARY = new JavaType("byte[]");
	
	private String name;
	private boolean importIgnore;
	private List<JavaType> types = new ArrayList<JavaType>();
	
	public JavaType() {}

	public JavaType(String name) {
		this.name = name;
	}

	public JavaType(String name, boolean importIgnore) {
		this.name = name;
		this.importIgnore = importIgnore;
	}
	
	public JavaType(String name, JavaType...types) {
		this.name = name;
		this.types.addAll(Arrays.asList(types));
	}
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<JavaType> getTypes() {
		return types;
	}
	public void setTypes(List<JavaType> types) {
		this.types = types;
	}
	public void addType(JavaType type) {
		types.add(type);
	}
	
	public String getJustClassName() {
		return Utils.getJustClassName(name);
	}
	
	public boolean isBoolean() {
		return Utils.isBooleanType(name);
	}
	
	public Set<String> getRequiredImports() {
		Set<String> set = new HashSet<String>();
		if (!importIgnore) {
			set.add(name);
			for(JavaType type : types) {
				set.addAll(type.getRequiredImports());
			}
		}
		return set;
	}
	
	public void out(PrintWriter out) throws IOException {
		out.print(getJustClassName());
		if (types.size() > 0) {
			out.print('<');
			for(int i = 0; i < types.size(); i++) {
				JavaType type = types.get(i);
				type.out(out);
				if (i < (types.size() -1)) {
					out.print(", ");
				}
			}
			out.print('>');
		}
	}
}