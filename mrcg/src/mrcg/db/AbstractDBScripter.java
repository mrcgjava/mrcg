package mrcg.db;

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

import java.util.Map;

import mrcg.domain.Index;
import mrcg.domain.JavaClass;
import mrcg.domain.JavaEnum;
import mrcg.domain.JavaEnumValue;
import mrcg.domain.JavaField;
import mrcg.utils.Utils;

public abstract class AbstractDBScripter  implements DBScripter {
	protected abstract void start(StringBuilder b, String name);
	protected abstract void end(StringBuilder b);
	protected abstract String getType(JavaField f);
	
	public String getColumnDelimiter() {
		return "";
	}
	
	public String getTableClosing() {
		return ");\n";
	}
	
	public String buildScript(String name, Map<String, JavaClass> types) {
		StringBuilder b = new StringBuilder();
		start(b, name);

		// Tables
		for(JavaClass jc : types.values()) {
			if (jc.isEnum() && !jc.isPreExisting()) {
				JavaEnum je = (JavaEnum)jc;
				b.append("\n");
				b.append("CREATE TABLE " + jc.getTableName() + " (\n");
				b.append("    code VARCHAR(" + je.getMaxCodeLength() + ") NOT NULL,\n");
				b.append("    name VARCHAR(" + je.getField("name").getMaxLength() + ") NOT NULL,\n");
				b.append("    PRIMARY KEY (code)\n");				
				b.append(");\n");
			} else {
				b.append("\n");
				b.append("CREATE TABLE " + jc.getTableName() + " (\n");
				for(JavaField jf : jc.getInstanceFields()) {
					if (!jf.isDatabaseField()) continue;
					b.append("    " + getColumnDelimiter() + Utils.toDatabaseFormat(jf.getName()) +
							getColumnDelimiter() + " " + getType(jf) + " " +
						(jf.isRequired()?"not null":"null") + 
					",\n");
				}
				b.append("    PRIMARY KEY (id)\n");
				b.append(getTableClosing());
			}
		}
		
		
		b.append("\n");
		
		// Foreign Keys
		for(JavaClass jc : types.values()) {
			StringBuilder ab = new StringBuilder();
			for(JavaField jf : jc.getInstanceFields()) {
				if (jf.getReferences() != null) {
					if (jf.getReferences().isEnum()) {
						ab.append("    ADD FOREIGN KEY (" + Utils.toDatabaseFormat(jf.getName()) + 
							") REFERENCES " + jf.getReferences().getTableName() + " (code),\n");
					} else {
						ab.append("    ADD FOREIGN KEY (" + Utils.toDatabaseFormat(jf.getName()) + 
							") REFERENCES " + jf.getReferences().getTableName() + " (id),\n");
					}
				}
			}
			if (ab.length() > 0) {
				ab.insert(0, "ALTER TABLE " + jc.getTableName() + " \n");
				ab.setLength(ab.length()-2);
				ab.append(";");
				b.append(ab).append("\n\n");
			}
		}

		b.append("\n");
		
		// Unique Indexes
		for(JavaClass jc : types.values()) {
			for(JavaField jf : jc.getInstanceFields()) {
				if (jf.isUnique()) {
					String column = Utils.toDatabaseFormat(jf.getName());
					b.append(
						"CREATE UNIQUE INDEX " + jc.getTableName() + "_" + column + 
						"_unique_index ON " + jc.getTableName() + " (" + column + ");\n\n"
					);
				}
			}
		}
		
		b.append("\n");
		
		// User Defined Field Indexes		
		for(JavaClass jc : types.values()) {
			for(JavaField jf : jc.getInstanceFields()) {
				if (jf.isIndexed()) {
					String column = Utils.toDatabaseFormat(jf.getName());
					b.append(
						"CREATE INDEX " + jc.getTableName() + "_" + column + 
						"_index ON " + jc.getTableName() + " (" + column + ");\n\n"
					);
				}
			}
		}
		
		// User Defined Compound Indexes		
		for(JavaClass jc : types.values()) {
			for(Index index : jc.getIndexes()) {
				b.append(
					"CREATE INDEX " + jc.getTableName() + "_" + index.getName() + 
					"_index ON " + jc.getTableName() + " (" + index.getColumnList() + ");\n\n"
				);
			}
		}

		
		
		b.append("\n");
		
		// Enum Inserts
		for(JavaClass jc : types.values()) {
			if (jc.isEnum()) {
				for(JavaEnumValue jev : ((JavaEnum)jc).getValues()) {
					b.append("INSERT INTO " + jc.getTableName() + " (code, name) VALUES ('" + jev.getName() + "', '" + jev.getValue() + "');\n");
				}
				b.append("\n");
			}
		}
		
		end(b);
		return b.toString();
	}
	
}