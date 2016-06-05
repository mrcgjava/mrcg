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

import mrcg.domain.JavaField;

public class PostgreSqlScriptifier extends AbstractDBScripter {
	protected void start(StringBuilder b, String name) {
		b.append("\\c template1;\n");
		b.append("DROP DATABASE IF EXISTS " + name + ";\n");
		b.append("CREATE DATABASE " + name + ";\n");
		b.append("\\c " + name + ";\n");
		b.append("START TRANSACTION;\n");
	}
	
	protected void end(StringBuilder b) {
		b.append("COMMIT;\n");		
	}
	
	protected String getType(JavaField f) {
		if (f.getName().equals("id")) {
			return "bigserial";
		}
		switch(f.getDbType()) {
			case Boolean:			return "boolean";
			case Date:				return "timestamp";
			case JustDate:			return "date";
			case Time:				return "time";
			case Double:			return "numeric (" + f.getMinLength() + ", " + f.getMaxLength() + ")";
			case DoublePrecision:	return "double precision";
			case Integer:			return "int";
			case Long:				return "bigint";
			case Text:				return "text";
			case Varchar:			return "varchar(" + f.getMaxLength() + ")";
			case Binary:			return "bytea";
			default:				throw new IllegalArgumentException("Unsupported type [" + f.getDbType() + "].");
		}
	}
}