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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mrcg.utils.ImportUtils;
import mrcg.utils.Utils;

import org.apache.commons.lang.StringUtils;


public class JavaClass implements ImportsRequired, Writable {
	protected String pkg;
	protected String name;
	protected JavaType extendsion = null;
	protected boolean isAbstract = false;
	protected boolean mapping = false;
	protected List<String> annotations = new ArrayList<String>();
	protected List<JavaType> implementsions = new ArrayList<JavaType>();	
	protected List<JavaType> imports = new ArrayList<JavaType>();
	protected List<JavaField> fields = new ArrayList<JavaField>();
	protected List<JavaMethod> methods = new ArrayList<JavaMethod>();
	protected List<JavaConstructor> constructors = new ArrayList<JavaConstructor>();
	
	protected List<Index> indexes = new ArrayList<Index>();
	
	private String tableName;
	
	private boolean preExisting;
	
	private String methodSpacer = "";
	
	public JavaClass() {}
	
	public JavaClass(String classname) {
		int i = classname.lastIndexOf('.');
		this.pkg = classname.substring(0, i);
		this.name = classname.substring(i+1);
	}
	
	
	public JavaClass(String pkg, String name) {
		this.pkg = pkg;
		this.name = name;
	}
	
	public String getLowerCamelPlural() {
		return Utils.pluralize(StringUtils.uncapitalize(name));
	}
	
	public boolean isPreExisting() {
		return preExisting;
	}

	public void setPreExisting(boolean preExisting) {
		this.preExisting = preExisting;
	}
	
	public boolean isAuditable() {
		return !(isMapping() || isEnum());
	}

	public boolean isEnum() {
		return false;
	}

	public boolean isMapping() {
		return mapping;
	}

	public void setMapping(boolean mapping) {
		this.mapping = mapping;
	}

	public String getCompleteName() {
		return pkg + "." + name;
	}

	public JavaType getJavaType() {
		return new JavaType(getCompleteName());
	}
	
	public boolean isAbstract() {
		return isAbstract;
	}
	
	public void setExtendsion(JavaType extendsion) {
		this.extendsion = extendsion;
		addImport(extendsion);
	}
	
	public JavaType getExtendsion() {
		return extendsion;
	}
	
	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}
	
	public String getPackage() {
		return pkg;
	}
	public void setPackage(String pkg) {
		this.pkg = pkg;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<JavaType> getImports() {
		return imports;
	}
	public void setImports(List<JavaType> imports) {
		this.imports = imports;
	}
	public List<JavaField> getFields() {
		return fields;
	}
	
	public boolean hasTableNameOverride() {
		return tableName != null;
	}
	
	public String getTableName() {
		if (tableName == null) {
			return Utils.toDatabaseFormat(name);
		} else {
			return tableName;
		}
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getSpacedCamelCaseName() {
		return Utils.toSpacedCamelCase(name);
	}

	public List<JavaField> getStaticFields() {
		List<JavaField> list = new ArrayList<JavaField>();
		for(JavaField f : fields) {
			if (f.isStatic()) {
				list.add(f);
			}
		}
		return list;
	}
	public List<JavaField> getInstanceFields() {
		List<JavaField> list = new ArrayList<JavaField>();
		for(JavaField f : fields) {
			if (!f.isStatic()) {
				list.add(f);
			}
		}
		return list;
	}

	public List<JavaField> getNonAutoHandledInstanceFields() {
		List<JavaField> list = new ArrayList<JavaField>();
		for(JavaField f : fields) {
			if (!f.isStatic() && !f.isAutoHandledField()) {
				list.add(f);
			}
		}
		return list;
	}
	
	public List<JavaField> getListFields() {
		List<JavaField> list = new ArrayList<JavaField>();
		for(JavaField f : fields) {
			if (!f.isStatic() && f.isOnList()) {
				list.add(f);
			}
		}
		return list;
	}
	
	public void setFields(List<JavaField> fields) {
		this.fields = fields;
	}
	public List<JavaMethod> getMethods() {
		return methods;
	}
	public void setMethods(List<JavaMethod> methods) {
		this.methods = methods;
	}
	public List<JavaType> getImplementsions() {
		return implementsions;
	}
	public void setImplementsions(List<JavaType> implementsions) {
		this.implementsions = implementsions;
	}

	public void addConstructor(JavaConstructor constructor) {
		constructors.add(constructor);
	}
	public void addMethod(JavaMethod method) {
		for(JavaMethod jm : methods) {
			if (jm.definitionsAreEqual(method)) return;
		}
		methods.add(method);
	}
	public void addField(JavaField field) {
		fields.add(field);
	}
	public void addImport(JavaType javaType) {
		imports.add(javaType);
	}
	public void addAnnotation(String annotation) {
		annotations.add(annotation);
	}
	public void addImplements(JavaType implement) {
		implementsions.add(implement);
	}
	
	public String getMethodSpacer() {
		return methodSpacer;
	}

	public void setMethodSpacer(String methodSpacer) {
		this.methodSpacer = methodSpacer;
	}

	public JavaField getField(String fieldName) {
		for(JavaField f : fields) {
			if (fieldName.equals(f.getName())) {
				return f;
			}
		}
		return null;
	}
	public boolean hasField(String fieldName) {
		return getField(fieldName) != null;
	}
	
	public JavaField getIdentifierField() {
		for(JavaField jf : fields) {
			if (jf.isIdentifier()) {
				return jf;
			}
		}
		return null;
	}

	public JavaField getOrderField() {
		for(JavaField jf : fields) {
			if (jf.isOrder()) {
				return jf;
			}
		}
		return null;
	}
	
	public boolean isReferenceFieldPresent() {
		for(JavaField f : getFields()) {
			if (f.getReferences() != null) return true;
		}
		return false;
	}
	
	
	public Set<String> getRequiredImports() {
		Set<String> set = new HashSet<String>();
		for(JavaType jt : imports) {
			set.addAll(jt.getRequiredImports());
		}
		for(JavaField field : fields) {
			set.addAll(field.getRequiredImports());
		}
		for(JavaMethod method : methods) {
			set.addAll(method.getRequiredImports());
		}
		for(JavaType jt : implementsions) {
			set.addAll(jt.getRequiredImports());
		}
		return set;
	}
	
	public List<Index> getIndexes() {
		return indexes;
	}
	
	public void out(PrintWriter out) throws IOException {
		out.println("package " + pkg + ";\n");
		
		List<String> imports = ImportUtils.cleanImports(getRequiredImports(), pkg);
		if (!imports.isEmpty()) {
			for(String i : imports) {
				out.println("import " + i + ";");
			}
			out.println();
		}
		
		for(String annotation : annotations) {
			out.println(annotation);
		}
		
		out.print("public " + (isAbstract?"abstract ":"") + "class " + getName());
		
		if (extendsion != null) {
			out.print(" extends ");
			extendsion.out(out);
		}
		
		if (!implementsions.isEmpty()) {
			out.print(" implements ");
			for(int i = 0; i < implementsions.size(); i++) {
				out.print(implementsions.get(i).getJustClassName());
				if (i < (implementsions.size() -1)) {
					out.print(", ");
				}
			}
		}
		out.println(" {");

		boolean println = false;
		for(JavaField field : fields) {
			if (field.isStatic()) {
				field.out(out);
				println = true;
			}
		}
		if (println) out.println();

		println = false;
		for(JavaField field : fields) {
			if (!field.isStatic()) {
				field.out(out);
				println = true;
			}
		}
		if (println) out.println();

		
		if (!constructors.isEmpty()) {
			for(JavaConstructor constructor : constructors) {
				constructor.out(out);
			}
			out.println(methodSpacer);
		}		
		
		for(JavaMethod method : methods) {
			method.out(out);
			out.print(methodSpacer);
		}
	
		
		out.print("}");
		
	}	
}