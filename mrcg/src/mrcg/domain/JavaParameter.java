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
import java.util.Set;

public class JavaParameter implements ImportsRequired, Writable {
	private String name;
	private JavaType type;
	
	public JavaParameter() {}
	public JavaParameter(JavaType type, String name) {
		this.type = type;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public JavaType getType() {
		return type;
	}
	public void setType(JavaType type) {
		this.type = type;
	}
	
	public Set<String> getRequiredImports() {
		return type.getRequiredImports();
	}
	
	public void out(PrintWriter out) throws IOException {
		type.out(out);
		out.print(" " + name);
	}	
}