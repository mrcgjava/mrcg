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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ImportUtils {
	private static final Set<String> CLEAN_SET = Utils.toSet(
		"long", "int", "short", "byte", "double", "float", "boolean", "void"
	);
	
	public static List<String> cleanImports(Collection<String> imports, String skipPackage) {
		List<String> clean = new ArrayList<String>();
		for(String i : imports) {
			if (needsImportStatement(i)) {
				if (!skipPackage.equals(getPackage(i))) {
					clean.add(i);
				}
			}
		}
		return clean;
	}
	
	public static boolean needsImportStatement(String type) {
		if (CLEAN_SET.contains(type)) return false;
		if (type.startsWith("java.lang")) return false;
		if(type.indexOf(".") == -1)  return false;
		return true;
	}
	
	public static String getPackage(String type) {
		int i = type.lastIndexOf('.');
		if (i == -1) {
			return "";
		} else {
			return type.substring(0, i);
		}
	}
	
	public static final void main(String...args) throws Exception {
		System.out.println(Void.TYPE.toString());
	}
}