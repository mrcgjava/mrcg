package mrcg.utils;

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

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import mrcg.domain.JavaClass;
import mrcg.domain.JavaField;
import mrcg.domain.JavaGetter;
import mrcg.domain.JavaSetter;
import mrcg.domain.JavaType;
import mrcg.domain.Visibility;

public class Utils {
	public static String getJustClassName(String className) {
		int i = className.lastIndexOf('.');
		if (i == -1) {
			return className;
		} else {
			return className.substring(i+1);
		}
	}

	public static JavaField createBeanProperty(JavaClass jc, JavaField field, boolean addGetterSetter) {
		jc.getFields().add(field);
		if (field.getType().isBoolean()) {
			jc.getMethods().add(new JavaGetter(field, "get"));
		}
		if (addGetterSetter) {
			jc.getMethods().add(new JavaGetter(field));
			jc.getMethods().add(new JavaSetter(field));
		}
		return field;
	}
	
	
	public static JavaField createBeanProperty(JavaClass jc, JavaType type, String name, Visibility visibility, boolean addGetterSetter) {
		JavaField jf = new JavaField();
			jf.setVisibility(visibility);
			jf.setName(name);
			jf.setType(type);
		return createBeanProperty(jc, jf, addGetterSetter);
	}
	
	public static String camelCase(String s) {
		return camelCase(s, true);
	}
	
	private static String camelCase(String s, boolean first) {
		String[] parts = s.split("_");
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < parts.length; i++) {
			String p = parts[i];
			if (i == 0 && !first) {
				b.append(p);
			} else {
				b.append(camelCaseWord(p));	
			}
		}
		return b.toString();
	}
	
	private static String camelCaseWord(String word) {
		return word.substring(0, 1).toUpperCase() + word.substring(1);
	}
	
	public static boolean isBooleanType(String type) {
		if ("boolean".equals(type)) {
			return true;
		} else if ("Boolean".equals(type)) {
			return true;
		} else if ("java.lang.Boolean".equals(type)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String createListActionExecuteMethod(String jclower) {
		Coder coder = new Coder();
			coder.println(2, "String jsp = \"/admin/" + jclower + "/list.jsp\";");
			coder.println(2, "if (!doesJspExist(jsp)) {");
			coder.println(3, "jsp = \"/admin/" + jclower + "/list-generated.jsp\";");
			coder.println(2, "}");
			coder.println(2, "return new ForwardResolution(jsp);");
		return coder.toString();		
	}

	public static String createSimpleListActionExecuteMethod(String jclower) {
		Coder coder = new Coder();
			coder.println(2, "return new ForwardResolution(\"/admin/" + jclower + "/list.jsp\");");
		return coder.toString();		
	}
	
	public static String createViewActionExecuteMethod(String jclower) {
		Coder coder = new Coder();
			coder.println(2, "String jsp = \"/admin/" + jclower + "/view.jsp\";");
			coder.println(2, "if (!doesJspExist(jsp)) {");
			coder.println(3, "jsp = \"/admin/" + jclower + "/view-generated.jsp\";");
			coder.println(2, "}");
			coder.println(2, "return new ForwardResolution(jsp);");
		return coder.toString();
	}

	public static String createSimpleViewActionExecuteMethod(String jclower) {
		Coder coder = new Coder();
			coder.println(2, "return new ForwardResolution(\"/admin/" + jclower + "/view.jsp\");");
		return coder.toString();
	}

	public static String createEditActionExecuteMethodBody(String jclower) {
		return 
			"		if (doesJspExist(\"/admin/" + jclower + "/edit.jsp\")) {\n" +
			"			return new ForwardResolution(\"/admin/" + jclower + "/edit.jsp\");\n" +
			"		} else {\n" +
			"			return new ForwardResolution(\"/admin/" + jclower + "/edit-generated.jsp\");\n" +
			"		}"
		;
				
	}
	
	public static String createSimpleEditActionExecuteMethodBody(String jclower) {
		return "		return new ForwardResolution(\"/admin/" + jclower + "/edit.jsp\");";
	}
	

	public static String toSpacedCamelCase(String s) {
		StringBuilder b = new StringBuilder(s.length() + 5);
		for(char c : s.toCharArray()) {
			if (Character.isUpperCase(c) && b.length() > 0) {
				b.append(' ');
			}
			b.append(c);
		}
		return b.toString();
	}
	
	
	public static String toSpacedCamelCase(String[] parts) {
		StringBuilder b = new StringBuilder();
		for(String a : parts) {
			b.append(StringUtils.capitalize(a)).append(" ");
		}
		return b.toString().trim();
	}
	
	public static <T> T firstNonNull(T...ts) {
		for(T t : ts) {
			if (t != null) return t;
		}
		return null;
	}
	
	public static <T> boolean in(T value, T...values) {
		for(T t : values) {
			if (value == null) {
				if (t == null) return true;
			} else {
				if (value == t || value.equals(t)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static String pluralize(String s) {
		if (s.length() == 0) return s;
		char c = Character.toLowerCase(s.charAt(s.length()-1));
		if (c == 'y') {
			if (StringUtils.endsWithAny(s.toLowerCase(), "day")) {
				return s + "s";
			} else {
				return s.substring(0, s.length()-1) + "ies";
			}
		} else if (c == 's') {
			return s + "es";
		} else {
			return s + "s";
		}
	}

	public static <S> Set<S> toSet(S...values) {
		return new TreeSet<S>(Arrays.asList(values));
	}
	
	public static String toDatabaseFormat(String s) {
		StringBuilder b = new StringBuilder(s.length() + 5);
		for(char c : s.toCharArray()) {
			if (Character.isUpperCase(c) && b.length() > 0) {
				b.append('_');
			}
			b.append(Character.toLowerCase(c));
		}
		return b.toString();
	}

	public static String execute(String template, Map<String, Object> map) throws Exception {
		VelocityContext context = new VelocityContext(map);
		StringWriter writer = new StringWriter();
		if (Velocity.evaluate(context, writer, "", template)) {
			return writer.getBuffer().toString();
		} else {
			throw new RuntimeException("Un unspecified error occured while transforming template");
		}
	}
	
	public static String execute(String template, Object...keysAndValues) throws Exception {
		return execute(template, toMap(keysAndValues));
	}
	
	private static Map<String, Object> toMap(Object...keysAndValues) {
		Map<String, Object> map = new HashMap<String, Object>();
		for(int i = 0; i < keysAndValues.length; i+=2) {
			map.put((String)keysAndValues[i], keysAndValues[i+1]);
		}
		return map;
	}
}