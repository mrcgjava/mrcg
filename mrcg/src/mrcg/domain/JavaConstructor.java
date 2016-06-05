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

public class JavaConstructor implements ImportsRequired, Writable {
	private Visibility visibility = Visibility.Public;
	private JavaType returnType;
	private List<JavaParameter> parameters = new ArrayList<JavaParameter>();
	private String body;
	private List<JavaType> throwers = new ArrayList<JavaType>();
	
	public JavaConstructor() {}

	public JavaConstructor(JavaType returnType) {
		this.returnType = returnType;
	}
	
	public JavaType getReturnType() {
		return returnType;
	}
	public void setReturnType(JavaType returnType) {
		this.returnType = returnType;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}		
	public List<JavaParameter> getParameters() {
		return parameters;
	}
	public void setParameters(List<JavaParameter> parameters) {
		this.parameters = parameters;
	}
	public List<JavaType> getThrowers() {
		return throwers;
	}
	public void setThrowers(List<JavaType> throwers) {
		this.throwers = throwers;
	}
	public Visibility getVisibility() {
		return visibility;
	}
	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}
	public void addParameter(JavaParameter parameter) {
		parameters.add(parameter);
	}

	public Set<String> getRequiredImports() {
		Set<String> set = new HashSet<String>();
		set.addAll(returnType.getRequiredImports());
		for(JavaParameter p : parameters) {
			set.addAll(p.getRequiredImports());
		}
		for(JavaType jt : throwers) {
			set.addAll(jt.getRequiredImports());
		}
		return set;
	}

	public void out(PrintWriter out) throws IOException {
		out(out, true);
	}
	
	public void out(PrintWriter out, boolean includeBody) throws IOException {
		out.print("\t" + visibility.getOutput());
		out.print(returnType.getJustClassName() + "(");
		for(int i = 0; i < parameters.size(); i++) {
			JavaParameter p = parameters.get(i);
			p.out(out);
			if (i < (parameters.size()-1)) {
				out.print(", ");
			}
		}
		
		out.print(")");
		if (!throwers.isEmpty()) {
			out.print(" throws ");
			for(int i = 0; i < throwers.size(); i++) {
				JavaType jt = throwers.get(i);
				jt.out(out);
				if (i < (throwers.size() -1)) {
					out.print(", ");
				}
			}
		}
		
		if (includeBody) {
			out.println(" {");
			if (body != null) {
				out.print(body);
				if (!body.endsWith("\n")) {
					out.println();
				}
			}
			out.println("\t}");
		} else {
			out.println(";");
		}
	}	
}