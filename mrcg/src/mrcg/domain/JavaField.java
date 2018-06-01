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
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import mrcg.utils.Utils;

public class JavaField implements ImportsRequired, Writable {
	private Visibility visibility = Visibility.Private;
	private boolean isStatic = false;
	private JavaType type;
	private String name;
	private String value;
	
	protected List<String> annotations = new ArrayList<String>();
	
	
	private boolean required;
	private boolean unique;
	private String defaultValue;
	private int minLength;
	private int maxLength;
	private boolean onList;
	private boolean onView;
	private boolean identifier;
	private boolean order;
	private boolean encrypted;
	
	private boolean excludedFromJavaOutput = false;
	
	private JavaClass references;
	private String label;
	
	private DBType dbType;
	
	private boolean autoHandledField = false;
	private boolean databaseField = true;
	
	private boolean indexed = false;
	
	private String dbField = null;
	
	public JavaField() {}
	public JavaField(JavaType type, String name) {
		this.type = type;
		this.name = name;
	}
		
	public String getName() {return name;}
	public void setName(String name) {this.name = name;}
	public JavaType getType() {return type;}
	public void setType(JavaType type) {this.type = type;}
	public Set<String> getRequiredImports() {
		Set<String> set = Utils.toSet(type.getName());
		return set;
	}
	public boolean isIdentifier() {return identifier;}
	public void setIdentifier(boolean identifier) {this.identifier = identifier;}
	public boolean isRequired() {return required;}
	public void setRequired(boolean required) {this.required = required;}
	public int getMinLength() {return minLength;}
	public void setMinLength(int minLength) {this.minLength = minLength;}
	public int getMaxLength() {return maxLength;}
	public void setMaxLength(int maxLength) {this.maxLength = maxLength;}
	public boolean isOnList() {return onList;}
	public void setOnList(boolean onList) {this.onList = onList;}
	public boolean isOnView() {return onView;}
	public void setOnView(boolean onView) {this.onView = onView;}
	public JavaClass getReferences() {return references;}
	public void setReferences(JavaClass references) {this.references = references;}
	public String getLabel() {return label;}
	public void setLabel(String label) {this.label = label;}
	public Visibility getVisibility() {return visibility;}
	public void setVisibility(Visibility visibility) {this.visibility = visibility;}
	public boolean isStatic() {return isStatic;}
	public void setStatic(boolean isStatic) {this.isStatic = isStatic;}
	public String getValue() {return value;}
	public void setValue(String value) {this.value = value;}
	public DBType getDbType() {return dbType;}
	public void setDbType(DBType dbType) {this.dbType = dbType;}
	public boolean isUnique() {return unique;}
	public void setUnique(boolean unique) {this.unique = unique;}
	public boolean isOrder() {return order;}
	public void setOrder(boolean order) {this.order = order;}
	public boolean isEncrypted() {return encrypted;}
	public void setEncrypted(boolean encrypted) {this.encrypted = encrypted;}
	public boolean isExcludedFromJavaOutput() {
		return excludedFromJavaOutput;
	}
	public void setExcludedFromJavaOutput(boolean excludedFromJavaOutput) {
		this.excludedFromJavaOutput = excludedFromJavaOutput;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	public String getLabelOrSynthetic() {
		if (label == null) {
			return Utils.toSpacedCamelCase(StringUtils.capitalize(name));
		} else {
			return label;
		}
	}
	
	public String getListValue() {
		if (isReferenceField()) {
			if (references.isEnum()) {
				return name;
			} else {
				if (references.getIdentifierField() == null) {
					return name.substring(0, name.length()-2) + "." + references.getIdentifierField().getName();
				} else {
					return name;
				}
			}
		} else {
			return name;
		}
	}
	
	public void setAutoHandledField(boolean autoHandledField) {
		this.autoHandledField = autoHandledField;
	}
	
	public boolean isAutoHandledField() {
		return autoHandledField || Utils.toSet("id", "createdAt", "updatedAt").contains(name);
	}
	
	public boolean isTextField() {
		return Utils.toSet(DBType.Double, DBType.DoublePrecision, DBType.Integer, DBType.Long, DBType.Varchar).contains(dbType);
	}

	public boolean isTextArea() {
		return (dbType != null) && Utils.toSet(DBType.Text).contains(dbType);
	}
	
	public boolean isSelectList() {
		return (references != null);
	}
	
	public boolean isCheckBox() {
		return Utils.toSet(DBType.Boolean).contains(dbType);
	}
	
	public boolean isDateField() {
		return DBType.Date.equals(dbType) || DBType.JustDate.equals(dbType);
	}

	public boolean isEmailField() {
		return JavaType.EMAIL.equals(type);
	}
	
	public void addAnnotation(String annotation) {
		annotations.add(annotation);
	}
	
	public boolean isDatabaseField() {
		return databaseField;
	}
	public void setDatabaseField(boolean databaseField) {
		this.databaseField = databaseField;
	}
	
	public boolean isIndexed() {
		return indexed;
	}
	public void setIndexed(boolean indexed) {
		this.indexed = indexed;
	}
	
	public boolean isReferenceField() {
		return references != null;
	}
	
	public String getNameAsLabel() {
		String label = StringUtils.capitalize(getName());
		label = Utils.toSpacedCamelCase(label);
		return label;
	}
	
	public String getDBField() {
		if (dbField == null) {
			return Utils.toDatabaseFormat(getName());	
		} else {
			return dbField;
		}
	}
	
	public void setDBField(String dbField) {
		this.dbField = dbField;
	}
	
	public String getEncryptionFieldName() {
		return name.toUpperCase() + "_KEY";
	}
	
	public void out(PrintWriter out) throws IOException {
		if (isExcludedFromJavaOutput()) return;
		for(String annotation : annotations) {
			out.println("\t" + annotation);
		}

		out.print("\t" + visibility.getOutput());
		if (isStatic) {
			out.print("static ");
		}
		type.out(out);
		out.print(" " + getName());
		if (value == null) {
			out.println(";");
		} else {
			out.println(" = " + value);
		}
	}	
	
}