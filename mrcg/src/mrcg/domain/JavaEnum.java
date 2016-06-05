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
import java.util.List;

import mrcg.utils.ImportUtils;


public class JavaEnum extends JavaClass implements ImportsRequired, Writable {
	private List<JavaEnumValue> values = new ArrayList<JavaEnumValue>();
	private int maxCodeLength = 0;

	public JavaEnum(String classname) {
		super(classname);
	}
	
	public JavaEnum(String pkg, String name) {
		super(pkg, name);
	}
	
	public int getMaxCodeLength() {
		return maxCodeLength;
	}

	public void setMaxCodeLength(int maxCodeLength) {
		this.maxCodeLength = maxCodeLength;
	}

	public void addValue(JavaEnumValue value) {
		values.add(value);
	}

	public List<JavaEnumValue> getValues() {
		return values;
	}
	
	public boolean isEnum() {
		return true;
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
		
		out.print("public enum " + getName());
		
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
		
		for(int i = 0; i < values.size(); i++) {
			out.print("\t");
			values.get(i).out(out);
			if (i == (values.size()-1)) {
				out.println(";");
			} else {
				out.println(",");
			}
		}
		out.println();
		
		
		for(JavaField field : fields) {
			field.out(out);
		}	
		
		for(JavaConstructor constructor : constructors) {
			constructor.out(out);
		}
		
		for(JavaMethod method : methods) {
			method.out(out);
		}
		
		out.print("}");
	}
}