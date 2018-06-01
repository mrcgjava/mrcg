package mrcg;

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.ho.yaml.Yaml;

import mrcg.db.DBScripter;
import mrcg.db.MySqlScriptifier;
import mrcg.db.PostgreSqlScriptifier;
import mrcg.domain.DBType;
import mrcg.domain.Index;
import mrcg.domain.JavaClass;
import mrcg.domain.JavaConstructor;
import mrcg.domain.JavaEnum;
import mrcg.domain.JavaEnumValue;
import mrcg.domain.JavaField;
import mrcg.domain.JavaGetter;
import mrcg.domain.JavaMethod;
import mrcg.domain.JavaParameter;
import mrcg.domain.JavaType;
import mrcg.domain.ListField;
import mrcg.domain.Visibility;
import mrcg.utils.Utils;

public class MRCGInstance {
	private static final Map<String, DBScripter> DB_SCRIPTER = new HashMap<String, DBScripter>();

	static {
		DB_SCRIPTER.put("mysql", new MySqlScriptifier());
		DB_SCRIPTER.put("postgres", new PostgreSqlScriptifier());
		DB_SCRIPTER.put("postgresql", new PostgreSqlScriptifier());
	}
	
	private String definitionFile;

	private Map<String, Object> root;
	
	
	private String projectPath;
	private String sourcePath;
	private String webPath;
	private String basePackage;
		
	private boolean reverseReferenceEnabled = true;
	
	private String libraryPackage = null;
	private String logicsPackage = null;
	private String digester = null;
	
	private String tagLibPrefix;
	private String tagUtils;
	private String transactionFilter;
	private String exceptionHandler;
	private String tableNameClass;
	
	
	private Map<String, JavaClass> types = new HashMap<String, JavaClass>();
	
	public MRCGInstance(String defintionFile) throws Exception {
		Velocity.init();
		if (defintionFile.startsWith("~")) {
			defintionFile = defintionFile.replace("~", System.getProperty("user.home"));
		} else if (!defintionFile.startsWith("/")) {
			defintionFile = System.getProperty("user.dir") + "/" + defintionFile;
		}		
		this.definitionFile = defintionFile;
	}
	
	private JavaClass getType(String type) {
		return types.get(type);
	}
	
	public void execute() throws Exception {
		String contents = IOUtils.toString(new FileReader(definitionFile));
		contents = contents.replace("\t", "    ");
		
	 	root  = (Map<String, Object>)Yaml.load(contents);
		projectPath = getString("config.projectpath");
		if (StringUtils.isEmpty(projectPath)) {
			projectPath = System.getProperty("user.dir") + "/";
		}		
		sourcePath = projectPath + "src/";
		webPath = projectPath + "WebContent/";
	 	basePackage = getString("config.basepackage");
	 	
 		// load library configuration stuff
 		this.libraryPackage = getString("config.libraryPackage");
 		this.logicsPackage = getString("config.logicsPackage");
 		JavaType.LOGIC_FACADE = new JavaType(getString("config.logicFacade"));
 		this.digester = getString("config.digester");
 		this.tagLibPrefix = getString("config.tagLibPrefix");
 		this.tagUtils = getString("config.tagUtils");
 		this.transactionFilter = getString("config.transactionFilter");
 		this.exceptionHandler = getString("config.exceptionHandler");
 		this.tableNameClass = getString("config.tableNameClass");
 		
 		

	 	reverseReferenceEnabled = getBoolean("config.reversereference", true);
			 	
		establishTypes();
		createEnums();
		createBeans();
		
		outputTypes();
		
		createBaseActions();
		createEditActions();
		
		boolean skipGui = getBoolean("config.skipgui", false);
		if (!skipGui) {
			createListActions();
			createListJSPs();
//			
//			createViewActions();
//			createViewJSPs();
		}
		
		if (!skipGui) {
			createEditJSPs();
			createOtherResources();
		}

		if (getBoolean("config.copyresources", true)) copyResources();
		
		createSQL();		
	}
	
	private void copyResources() {
		File dir = new File(new File(projectPath).getParentFile(), "mrcg-resources/src/to-copy");
		copyResources(dir, new File(projectPath));		
	}
	
	private void copyResources(File src, File dest) {
		for(File fsrc : src.listFiles()) {
			if (Utils.in(fsrc.getName(), ".DS_Store", "CVS")) continue;
			
			File fdest = new File(dest, fsrc.getName());
			if (fsrc.isDirectory()) {
				if (!fdest.exists()) {
					System.out.println("creating: " + fdest.getAbsolutePath());
					fdest.mkdirs();
				}
				copyResources(fsrc, fdest);
			} else {
				if (fdest.getName().endsWith(".vel")) {
					fdest = new File(dest, fsrc.getName().replace(".vel", ""));
				}
				if (!fdest.exists()) {
					System.out.println("creating: " + fdest.getAbsolutePath());
					if (fsrc.getName().endsWith(".vel")) {
						Reader in = null;
						Writer out = null;
						try {
							in = new BufferedReader(new FileReader(fsrc));
							String template = IOUtils.toString(in);
							String content = Utils.execute(template, 
								"basePackage", 			basePackage,
								"projectName", 			getString("config.projectname"),
								"databaseName",			getString("database.name"),
								"transactionFilter", 	transactionFilter,
								"exceptionFilter",		exceptionHandler,
								"tagUtils",				tagUtils
							);
							out = new BufferedWriter(new FileWriter(fdest));
							IOUtils.write(content, out);
						} catch(Exception e) {
							e.printStackTrace();
						} finally {
							IOUtils.closeQuietly(in);
							IOUtils.closeQuietly(out);
						}
					} else {
						InputStream in = null;
						OutputStream out = null;
						try {
							in = new BufferedInputStream(new FileInputStream(fsrc));
							out = new BufferedOutputStream(new FileOutputStream(fdest));
							IOUtils.copy(in, out);
						} catch(Exception e) {
							e.printStackTrace();
						} finally {
							IOUtils.closeQuietly(in);
							IOUtils.closeQuietly(out);
						}
					}
				}
			}
		}
	}
	
	private void createSQL() throws Exception {
		String databaseType = getString("database.type");
		DBScripter dbScripter = DB_SCRIPTER.get(databaseType);
		String script = dbScripter.buildScript(getString("database.name"), types);
		File file = new File(projectPath + "database/" + databaseType + "/01-create-generated.sql");
		boolean changed = content(file, script, true);
		if (isSkipDatabaseEnabled()) return;
		if (isDesignAlwaysModeEnabled() || (changed && isDesignModeEnabled())) {
			System.out.print("Running SQL Script...");

			Process process = Runtime.getRuntime().exec(
				"bash runall.sh", 
				null,
				new File(projectPath + "database/" + databaseType)
			);
			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			System.out.println(process.waitFor());
			while(in.ready() || err.ready()) {
				if (in.ready()) {
					System.out.println(in.readLine());
				} else if (err.ready()) {
					System.err.println(err.readLine());
				}
			}
		}
	}
	
	public boolean isDesignModeEnabled() {
		return "design".equals(getString("config.mode"));
	}

	public boolean isDesignAlwaysModeEnabled() {
		return "design-always".equals(getString("config.mode"));
	}
	
	public boolean isSkipDatabaseEnabled() {
		return "true".equals(getString("config.skipdatabase"));
	}

	private String getResourcePath(String filename) {
		String rbase = 	new File(new File(projectPath).getParentFile(), "mrcg-resources/src/resources").getAbsolutePath();
		return rbase + "/" + filename;
	}
	
//	config.properties
	private void createOtherResources() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
			map.put("basePackage", basePackage);
			map.put("databaseName", getString("database.name"));
			map.put("libraryPackage", libraryPackage);
			map.put("logicsPackage", logicsPackage);
			map.put("logicFacade", JavaType.LOGIC_FACADE.getName());
			map.put("digester", digester);
			map.put("digesterClassName", new JavaType(digester).getJustClassName());
			map.put("tagLibPrefix", tagLibPrefix);
			map.put("tagUtils", tagUtils);
			map.put("transactionFilter", transactionFilter);
			map.put("exceptionHandler", exceptionHandler);

			
		String base = sourcePath + basePackage.replace('.', '/');
		String rbase = 	new File(new File(projectPath).getParentFile(), "mrcg-resources/src/resources").getAbsolutePath();

		velocity(new File(base + "/gui/filter/AuthenticationFilter.java"), rbase + "/AuthenticationFilter.java.vel", map, false);
		velocity(new File(base + "/gui/action/LoginAction.java"), rbase + "/LoginAction.java.vel", map, false);
		velocity(new File(base + "/gui/action/LogoutAction.java"), rbase + "/LogoutAction.java.vel", map, false);
		velocity(new File(base + "/gui/action/ChangePasswordAction.java"), rbase + "/ChangePasswordAction.java.vel", map, false);
		velocity(new File(base + "/gui/action/BaseAction.java"), rbase + "/BaseAction.java.vel", map, false);
		velocity(new File(base + "/gui/admin/action/AbstractListAction.java"), rbase + "/AbstractListAction.java.vel", map, false);
		velocity(new File(base + "/gui/admin/action/IndexAction.java"), rbase + "/IndexAction.java.vel", map, false);
		velocity(new File(base + "/gui/servlet/StartupServlet.java"), rbase + "/StartupServlet.java.vel", map, false);			
		velocity(new File("/srv/" + getString("config.projectname") + "/config.properties"), rbase + "/config.properties.vel", map, false);
	}
	
	
	private void createEditJSPs() throws Exception {
		for(JavaClass jclass : types.values()) {
			if (!(jclass.isEnum() || jclass.isMapping() || skipGui(jclass))) {				
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("basePackage", basePackage);
				map.put("classUpper", StringUtils.capitalize(jclass.getName()));
				map.put("classUpperSpaced", Utils.toSpacedCamelCase(StringUtils.capitalize(jclass.getName())));
				map.put("classLower", jclass.getName().toLowerCase());
				map.put("classLowerCamel", StringUtils.uncapitalize(jclass.getName()));
				map.put("fields", jclass.getNonAutoHandledInstanceFields());
//				map.put("mappings", convertToJspEditCode(jclass));
				map.put("tagLibPrefix", tagLibPrefix);
				File file = new File(webPath + "admin/" + jclass.getName().toLowerCase() + "/edit.jsp");
				velocity(file, getResourcePath("edit-jsp.vel"), map, false);

				file = new File(webPath + "admin/" + jclass.getName().toLowerCase() + "/edit-layout.jsp");
				velocity(file, getResourcePath("edit-layout-jsp.vel"), map, true);
				
			}
		}
	}

	private void createListJSPs() throws Exception {
		for(JavaClass jclass : types.values()) {
			if (!(jclass.isEnum() || jclass.isMapping() || skipGui(jclass))) {
				
				String path = "beans." + jclass.getName() + ".list.";
				
				List<ListField> listFields = new ArrayList<ListField>();
				List<Object> lfs = getList(path + "fields");
				if (lfs != null && !lfs.isEmpty()) {
					for(Object o : lfs) {
						String s = StringUtils.trimToEmpty(o.toString());
						ListField lf = new ListField();
						if (s.contains(":")) {
							String[] ss = s.split(":");
							lf.setName(ss[0]);
							lf.setLabel(ss[1]);
						} else {
							lf.setName(s);
							lf.setLabel(Utils.toSpacedCamelCase(StringUtils.capitaliseAllWords(s.replace('.', ' '))));
						}
						listFields.add(lf);
					}
				} else {
					for(JavaField jf : jclass.getListFields()) {
						ListField lf = new ListField();
						if (jf.isReferenceField()) {
							JavaClass ref = jf.getReferences();
							lf.setLabel(jf.getNameAsLabel().replace(" Id", ""));
							String refField = "identifierLabel";
							if (ref.getIdentifierField() != null) {
								refField = ref.getIdentifierField().getName();
							}
							lf.setName(jf.getName().replace("Id", "") + "." + refField);
						} else {
							lf.setLabel(jf.getNameAsLabel());
							lf.setName(jf.getName());
						}
						listFields.add(lf);
					}
				}
				
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("basePackage", basePackage);
				map.put("classUpper", StringUtils.capitalize(jclass.getName()));
				map.put("classUpperSpaced", Utils.toSpacedCamelCase(StringUtils.capitalize(jclass.getName())));
				map.put("classUpperSpacedPlural", Utils.toSpacedCamelCase(StringUtils.capitalize(Utils.pluralize(jclass.getName()))));
				map.put("classLower", jclass.getName().toLowerCase());
				map.put("classLowerCamel", StringUtils.uncapitalize(jclass.getName()));
				map.put("fields", listFields);
				map.put("tagLibPrefix", tagLibPrefix);
//				map.put("mappings", convertToJspEditCode(jclass));
				map.put("edit", getBoolean(path + "edit", true));
				map.put("view", getBoolean(path + "view", true));
				
				File file = new File(webPath + "admin/" + jclass.getName().toLowerCase() + "/list-generated.jsp");
				velocity(file, getResourcePath("list-jsp.vel"), map, true);
			}
		}
	}

	private void createBaseActions() throws Exception {
		JavaClass genBase = new JavaClass(basePackage + ".generated.gui.admin.action", "GeneratedBaseAction");
		genBase.addImplements(new JavaType("net.sourceforge.stripes.action.ActionBean"));
		genBase.addImport(JavaType.LOGIC_FACADE);
		genBase.addImport(new JavaType("java.util.List"));
		genBase.addImport(new JavaType("java.io.InputStream"));
		Utils.createBeanProperty(genBase, new JavaType("net.sourceforge.stripes.action.ActionBeanContext"), "context", Visibility.Private, true);		
		
		JavaMethod jspe = new JavaMethod(JavaType.BOOLEAN, "doesJspExist");
		jspe.addParameter(new JavaParameter(JavaType.STRING, "jsp"));
		jspe.setBody(
			"\t\tInputStream in = null;\n"+
			"\t\ttry {\n"+
			"\t\t	in = getContext().getServletContext().getResourceAsStream(jsp);\n"+
			"\t\t	return in != null;\n"+
			"\t\t} finally {\n"+
			"\t\t	IOUtils.closeQuietly(in);\n"+
			"\t\t}"
		);
		genBase.addMethod(jspe);
		genBase.addImport(new JavaType("org.apache.commons.io.IOUtils"));
		
		JavaMethod gcids = new JavaMethod(new JavaType("java.util.Set", JavaType.LONG), "getCheckedIds");
		gcids.addParameter(new JavaParameter(new JavaType("java.util.Map", JavaType.LONG, JavaType.BOOLEAN), "map"));
		gcids.setBody(
			"\t\tSet<Long> set = new HashSet<Long>();\n" +
			"\t\tif (map != null) {\n" +
			"\t\t	for(Long id : map.keySet()) {\n" + 
			"\t\t		Boolean value = map.get(id);\n" + 
			"\t\t		if ((value != null) && value) {\n" + 
			"\t\t			set.add(id);\n" + 
			"\t\t		}\n" + 
			"\t\t	}\n" + 
			"\t\t}\n" + 
			"\t\treturn set;"
		);
		genBase.addMethod(gcids);
		genBase.addImport(new JavaType("java.util.HashSet"));
		
		for(JavaClass jclass : types.values()) {
			if (jclass.isEnum()) {
				genBase.addImport(new JavaType("java.util.Arrays"));
				JavaType jt = new JavaType("java.util.List");
				jt.addType(jclass.getJavaType());
				JavaMethod m = new JavaMethod(jt, "get" + Utils.pluralize(jclass.getName()));
				m.setBody("\t\treturn Arrays.asList(" + jclass.getName() + ".values());");
				genBase.addMethod(m);
			} else {
				JavaType jt = new JavaType("java.util.List");
				jt.addType(jclass.getJavaType());
				JavaMethod m = new JavaMethod(jt, "get" + Utils.pluralize(jclass.getName()));

				JavaField orderField = jclass.getOrderField();
				if (orderField == null) {
					orderField = jclass.getIdentifierField();
				}
				
				if (orderField != null) {
					m.setBody("\t\treturn " + JavaType.LOGIC_FACADE.getJustClassName() + ".listBySQL(" + jclass.getName() + ".class, \"ORDER BY " + Utils.toDatabaseFormat(orderField.getName()) + "\");");						
				} else {
					m.setBody("\t\treturn " + JavaType.LOGIC_FACADE.getJustClassName() + ".list(" + jclass.getName() + ".class);");					
				}

				genBase.addMethod(m);
			}
		}
		
		write(genBase);
		
		JavaClass base = new JavaClass(basePackage + ".gui.admin.action", "BaseAction");
		base.setExtendsion(genBase.getJavaType());
		write(base, false);
		
	}
	
	private boolean skipGui(JavaClass jclass) {
		return getBoolean("beans." + jclass.getName() + ".skipgui", false);
	}
	
	private static final Set<String> DONT_VALIDATE = Utils.toSet("id","createdAt","updatedAt");
	private void createEditActions() throws Exception {
		for(JavaClass jclass : types.values()) {
			if (!(jclass.isEnum() || jclass.isMapping() || skipGui(jclass))) {
				Set<String> imports = new HashSet<String>();
				
				String classUpper = StringUtils.capitalize(jclass.getName());
				String classLower = jclass.getName().toLowerCase();
				String pkg = basePackage + ".generated.gui.admin.action." + classLower;
				String classname = "GeneratedEdit" + classUpper + "Action";
				
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("basePackage", basePackage);
				map.put("classUpper", classUpper);
				map.put("classLower", classLower);
				map.put("libraryPackage", libraryPackage);
				map.put("logicsPackage", logicsPackage);
				map.put("logicFacade", JavaType.LOGIC_FACADE.getName());

				String v = "@ValidateNestedProperties({\n";
				for(JavaField jf : jclass.getFields()) {
					if (!jf.isStatic() && !DONT_VALIDATE.contains(jf.getName())) {
						v += "\t\t@Validate(field=\"" + jf.getName() + "\"";
						
						String label = Utils.firstNonNull(jf.getLabel(), jf.getNameAsLabel());
						v += ", label=\"" + label + "\"";
						
						if (jf.isRequired()) {
							v += ", required=true";
						}
						if (jf.isDateField()) {
							imports.add(libraryPackage + ".stripes.LocalDateTimeConverter");
							v += ", converter=LocalDateTimeConverter.class";
						} else if (jf.isEmailField()) {
							imports.add("net.sourceforge.stripes.validation.EmailTypeConverter");
							v += ", converter=EmailTypeConverter.class";
						}
						v += "),\n";
					}
				}
				v += "\t})";
				map.put("validation", v);
				
				map.put("imports", imports);
				
				velocity(classToFile(pkg, classname), getResourcePath("GeneratedEditAction.vel"), map, true);

				// Edit Action
				pkg = basePackage + ".gui.admin.action." + classLower;
				classname = "Edit" + classUpper + "Action";				
				velocity(classToFile(pkg, classname), getResourcePath("EditAction.vel"), map, false);
			}
		}
	}

	
	private void outputTypes() throws Exception {
		for(JavaClass jc : types.values()) {
			if (jc.isEnum()) {
				if (jc.getCompleteName().startsWith(basePackage) && !jc.isPreExisting()) {
					write(jc);
				}
			} else {
				String pkg = jc.getPackage();
				String name = jc.getName();
				jc.setPackage(basePackage + ".generated.bean");
				jc.setName("Generated" + name);
				
				if (jc.hasTableNameOverride()) {
					jc.addAnnotation("@TableName(\"" + jc.getTableName() + "\")");
					jc.addImport(new JavaType(tableNameClass));
				}
				
				jc.addAnnotation("@SuppressWarnings(\"serial\")");
				jc.addImplements(JavaType.SERIALIZABLE);
				write(jc);
				
				JavaClass real = new JavaClass(pkg, name);
				real.setExtendsion(jc.getJavaType());
				real.addImport(jc.getJavaType());
				real.addAnnotation("@SuppressWarnings(\"serial\")");
				write(real, false);
				
				jc.setPackage(pkg);
				jc.setName(name);
			}
		}
	}

	private JavaType getDateType() {
		return JavaType.DATE_TIME;			
	}
	
	private void createBeans() throws Exception {
		for(JavaClass jc : types.values()) {
			if (!jc.isEnum() && !jc.isMapping()) {
				
				jc.setExtendsion(new JavaType(getString("config.beanInterface")));
//				jc.addImplements(new JavaType(getString("config.beanInterface")));
				
				Visibility visibility = Visibility.Private;
				String svisibility = getString("beans." + jc.getName() + ".fields-visibility");
				if (StringUtils.isNotEmpty(svisibility)) {
					visibility = Visibility.valueOf(svisibility);
				}
				
				System.out.println("Creating bean '" + jc.getName() + "'");
				jc.setTableName(getString("beans." + jc.getName() + ".tablename"));
				
//				JavaField fid = new JavaField(JavaType.LONG, "id");
				JavaField fid = Utils.createBeanProperty(jc, JavaType.LONG, "id", visibility, false);
				fid.setDbType(DBType.Long);
				fid.setOnView(true);
				fid.setRequired(true);
				fid.setExcludedFromJavaOutput(true);
				Map<String, Object> map = getMap("beans." + jc.getName() + ".fields");
				if (map == null) {
					System.out.println("Couldn't find fields for beans." + jc.getName());
					map = new HashMap<String, Object>();
				}
				
				JavaField identitifierField = fid;
				for(String key : map.keySet()) {
					String def = map.get(key).toString();
					JavaField field = buildField(jc, key, def, visibility);
					if (field.isIdentifier()) {
						identitifierField = field;
					}
				}
				
//				jc.setAuditFieldsPresent(getBoolean("beans." + jc.getName() + ".audit-fields", true)
				
				if (getBoolean("beans." + jc.getName() + ".audit-fields", true)) {
					JavaField createdAt = Utils.createBeanProperty(jc, getDateType(), "createdAt", visibility, false);
					createdAt.setOnView(true);
					createdAt.setDbType(DBType.Date);
					createdAt.setExcludedFromJavaOutput(true);
					
					JavaField updatedAt = Utils.createBeanProperty(jc, getDateType(), "updatedAt", visibility, false);
					updatedAt.setOnView(true);
					updatedAt.setDbType(DBType.Date);
					updatedAt.setExcludedFromJavaOutput(true);
				}
				
				
				if (identitifierField == fid) {
					if (jc.hasField("name")) {
						identitifierField = jc.getField("name");
					}
				}
				
				JavaMethod jm = new JavaMethod(JavaType.OBJECT, "getIdentifierLabel");
				String returnValue = identitifierField.getName();
				if (identitifierField == fid) {
					returnValue = "getId()";
				}
				jm.setBody("\t\treturn " + returnValue + ";");
				jc.addMethod(jm);
				
//				jm = new JavaMethod(JavaType.PBOOLEAN, "isNew");
//				jm.setBody("\t\treturn id == null;");
//				jc.addMethod(jm);
				
				
				map = getMap("beans." + jc.getName() + ".indexes");
				if (map != null) {
					for(String key : map.keySet()) {
						List<Object> list = getList("beans." + jc.getName() + ".indexes." + key);
						if (!list.isEmpty()) {
							Index index = new Index();
							index.addColumns(list);
							jc.getIndexes().add(index);
						}
					}
				}
			}
		}
	}
	
	private JavaField buildField(JavaClass jclass, String fieldName, String def, Visibility visibility) {
		String[] parts = splitFieldDef(def);
		LinkedHashSet<String> set = new LinkedHashSet<String>(Arrays.asList(parts));
		JavaField field = null;
		if ("references".equals(parts[0])) {
			field = addReferenceProperty(fieldName, jclass, getType(parts[1]), parts);
//			field.setLabel(StringUtils.toSpacedCamelCase(field.getReferences().getName()));
			field.setLabel(Utils.toSpacedCamelCase(StringUtils.capitalize(fieldName)));
		} else {
			String type = parts[0];
			int i = type.indexOf("(");
			Integer[] scale = null;
			if (i > 0) {
				scale = getScale(type.substring(i));
				type = type.substring(0, i);
			}

			field = new JavaField();
			field.setName(fieldName);

			if (scale != null) {
				field.setMinLength(scale[0]);
				field.setMaxLength(scale[1]);
			}
			
			// string, text, numeric, long, date
			if ("string".equals(type)) {
				if (scale == null) {
					throw new IllegalArgumentException("Type [string] requires scale");
				}
				field.setType(JavaType.STRING);
				field.setDbType(DBType.Varchar);
			} else if ("text".equals(type)) {
				field.setType(JavaType.STRING);
				field.setDbType(DBType.Text);
			} else if ("double".equals(type)) {
				field.setType(JavaType.DOUBLE);
				field.setDbType(DBType.DoublePrecision);				
			} else if ("numeric".equals(type)) {
				if (scale == null) {
					throw new IllegalArgumentException("Type [numeric] requires scale");
				}
				field.setType(JavaType.DOUBLE);
				field.setDbType(DBType.Double);
			} else if ("money".equals(type)) {
				scale = new Integer[] {8,2};
				field.setMinLength(8);
				field.setMaxLength(2);
				field.setType(JavaType.DOUBLE);
				field.setDbType(DBType.Double);
			} else if ("int".equals(type) || "integer".equals(type)) {
				field.setType(JavaType.INTEGER);
				field.setDbType(DBType.Integer);
			} else if ("long".equals(type)) {
				field.setType(JavaType.LONG);
				field.setDbType(DBType.Long);
			} else if ("date".equals(type)) {
				field.setType(getDateType());
				field.setDbType(DBType.Date);
			} else if ("justdate".equals(type)) {
				field.setType(getDateType());
				field.setDbType(DBType.JustDate);
			} else if ("boolean".equals(type)) {
				field.setType(JavaType.BOOLEAN);
				field.setDbType(DBType.Boolean);
			} else if ("time".equals(type)) {
				field.setType(getDateType());
				field.setDbType(DBType.Time);
			} else if ("email".equals(type)) {
				field.setType(JavaType.EMAIL);
				field.setDbType(DBType.Varchar);
				if (scale == null) {
					throw new IllegalArgumentException("Type [email] requires scale");
				}
			} else if ("binary".equals(type)) {
				field.setType(JavaType.BINARY);
				field.setDbType(DBType.Binary);
			} else {
				throw new IllegalArgumentException("Type [" + type + 
					"] is not currently supported [" + jclass.getName() + "." + fieldName + ": " + def + "]"
				);
			}
			
			Utils.createBeanProperty(jclass, field, true);
		}
		
		field.setVisibility(visibility);		
		
		if (set.contains("label")) {
			field.setLabel(getItemAfter(set, "label"));
		}
		
		if (set.contains("default")) {
			field.setDefaultValue(getItemAfter(set, "default"));
		}
		
		field.setEncrypted(set.contains("encrypted"));
		
		if (field != null) {
			field.setOnList(set.contains("list"));
			field.setOnView(!set.contains("noview"));
			field.setRequired(!set.contains("null"));
			field.setUnique(set.contains("unique"));
			field.setIdentifier(set.contains("identifier"));
			field.setOrder(set.contains("order"));
		}
		
		if (set.contains("index") || set.contains("indexed")) {
			field.setIndexed(true);
		}
		
		return field;
		
	}
	
	private String getItemAfter(Set<String> set, String itemBefore) {
		if (set.contains(itemBefore)) {
			for(Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
				if (iterator.next().equals(itemBefore)) {
					return iterator.hasNext()?iterator.next():null;
				}
			}
		}
		return null;
	}
	
	private Integer[] getScale(String s) {
		s = s.replace("(", "");
		s = s.replace(")", "");
		String[] parts = s.split("-");
		if (parts.length == 1) {
			return new Integer[] {0, new Integer(parts[0])};
		} else {
			return new Integer[] {new Integer(parts[0]), new Integer(parts[1])};
		}
		
	}
	
	private static String[] splitFieldDef(String def) {
		List<String> list = new ArrayList<String>();
		StringBuilder b = new StringBuilder();
		boolean insideQuotes = false;
		for(char c : def.toCharArray()) {
			if (c == ' ' && !insideQuotes) {
				String s = b.toString().trim();
				if (s.length() > 0) {list.add(s);}
				b.setLength(0);
			} else if (c == '"') {
				String s = b.toString().trim();
				if (s.length() > 0) {list.add(s);}
				b.setLength(0);
				insideQuotes = !insideQuotes;
			} else {
				b.append(c);
			}
		}
		
		String s = b.toString().trim();
		if (s.length() > 0) {list.add(s);}
		
		return list.toArray(new String[list.size()]);
	}
	
	private JavaField addReferenceProperty(String fieldName, JavaClass jclass, JavaClass refClass, String[] parts) {
		JavaField jfield = null;
		if (refClass.isEnum()) {
			fieldName = StringUtils.capitalize(fieldName);
			
			jfield = new JavaField(refClass.getJavaType(), StringUtils.uncapitalize(fieldName) + "Code");
			if (new HashSet<String>(Arrays.asList(parts)).contains("nocode")) {
				jfield = new JavaField(refClass.getJavaType(), StringUtils.uncapitalize(fieldName));
			}
			jfield.setDbType(DBType.Varchar);
			jfield.setMaxLength(((JavaEnum)refClass).getMaxCodeLength());
		} else {
			fieldName = StringUtils.capitalize(fieldName);
			
			// refClass.getName()
			jfield = new JavaField(JavaType.LONG, StringUtils.uncapitalize(fieldName) + "Id");
			jfield.setDbType(DBType.Long);
			jfield.setRequired(true);

			// refClass.getName()
			String field2name = StringUtils.uncapitalize(fieldName);
			JavaField jfield2 = new JavaField(refClass.getJavaType(), field2name);
			jfield2.setAutoHandledField(true);
			jfield2.setDatabaseField(false);
			jclass.addField(jfield2);
			
			// jclass.getReferencedObject()
			jclass.addImport(JavaType.LOGIC_FACADE);
			JavaMethod jmethod = new JavaMethod(refClass.getJavaType(), "get" + fieldName);
			jmethod.setBody(
				"\t\t" + field2name + " = (" + field2name + " == null)?" + JavaType.LOGIC_FACADE.getJustClassName() + ".get(" + refClass.getName() + ".class, " + 
				jfield.getName() + "):" + field2name + ";\n\t\treturn " + field2name + ";"
			);
			jclass.addMethod(jmethod);
			
			// if 'unique' add singular, else plural
			
			// refClass.getRefererObjects()
			String name = jclass.getName();
//			if (fieldName != null) {
//				name = fieldName;
//			}
//			String name = (fieldName == null)?jclass.getName():fieldName;
			
			if (isReverseReferenceEnabled(refClass.getName())) {
				refClass.addImport(JavaType.LOGIC_FACADE);
				JavaType jtl = new JavaType("java.util.List");
				jtl.addType(jclass.getJavaType());
				jmethod = new JavaMethod(jtl, "get" + Utils.pluralize(name));
				jmethod.setBody("\t\treturn " + JavaType.LOGIC_FACADE.getJustClassName() + ".list(" + jclass.getName() + ".class, \"" + Utils.toDatabaseFormat(refClass.getName()) + "_id\", getId());");
				refClass.addMethod(jmethod);
			}
			
		}
		jfield.setReferences(refClass);
		Utils.createBeanProperty(jclass, jfield, true);
		return jfield;

	}
	
	private boolean isReverseReferenceEnabled(String beanName) {
		return !reverseReferenceEnabled?false:(getBoolean("beans." + beanName + ".reversereference", true));
	}
	
	private void createEnums() throws Exception {
		for(JavaClass jc : types.values()) {
			if (jc.isEnum()) {
				JavaEnum je = (JavaEnum)jc;
				jc.setPreExisting(!jc.getCompleteName().startsWith(basePackage));
				if (jc.isPreExisting()) continue;
				jc.setPreExisting(getBoolean("enums." + je.getName() + ".preexisting", false));

		 		JavaField jf = new JavaField(JavaType.STRING, "name");
		 		jf.setType(JavaType.STRING);
		 		jf.setDbType(DBType.Varchar);
		 		je.addField(jf);
		 		je.addMethod(new JavaGetter(jf));
		 		
		 		JavaConstructor jcon = new JavaConstructor(je.getJavaType());
		 		jcon.setVisibility(Visibility.Private);
		 		jcon.addParameter(new JavaParameter(JavaType.STRING, "name"));
		 		jcon.setBody("\t\tthis.name = name;");
		 		je.addConstructor(jcon);
				
		 		JavaMethod getcode = new JavaMethod(JavaType.STRING, "getCode");
		 		getcode.setBody("\t\treturn toString();");
		 		je.addMethod(getcode);
		 		
		 		JavaMethod aci = new JavaMethod(JavaType.OBJECT, "getIdentifierLabel");
		 		aci.setBody("\t\treturn name;");
		 		je.addMethod(aci);
		 		
				int maxCodeLength = 0;
				int maxNameLength = 0;
				Object object = getValue("enums." + jc.getName());
				if (object instanceof Map) {
					Map<String, Object> map = getMap("enums." + jc.getName());
					for(String key : map.keySet()) {
						if ("preexisting".equals(key)) continue;
						maxCodeLength = Math.max(maxCodeLength, key.toString().length());
						maxNameLength = Math.max(maxNameLength, map.get(key).toString().length());
						je.addValue(new JavaEnumValue(key, map.get(key).toString()));
					}					
				} else if (object instanceof List) {
					List<Object> values = getList("enums." + jc.getName());
					for(Object value : values) {
						if (value instanceof String) {
							if ("preexisting".equals(value.toString())) continue;
							maxCodeLength = Math.max(maxCodeLength, value.toString().length());
							maxNameLength = Math.max(maxNameLength, value.toString().length());
							je.addValue(new JavaEnumValue(value.toString(), Utils.toSpacedCamelCase(StringUtils.capitalize(value.toString()))));
						} else if (value instanceof Map) {
							Map<String, Object> map = toMap(value);
							for(String key : map.keySet()) {
								if ("preexisting".equals(key)) continue;								
								maxCodeLength = Math.max(maxCodeLength, key.toString().length());
								maxNameLength = Math.max(maxNameLength, map.get(key).toString().length());
								je.addValue(new JavaEnumValue(key, map.get(key).toString()));
							}
						}
					}
				}
				
				je.setMaxCodeLength(maxCodeLength);
				jf.setMaxLength(maxNameLength);
			}
		}
	}
	
	// First Pass - establish all JavaTypes
	private void establishTypes() {
		// Enums
		Map<String, Object> enums = getMap("enums");
		if (enums != null) {
			for(String key : enums.keySet()) {
				Object object = enums.get(key);
				String classname = basePackage + ".bean." + key;
				if (object instanceof String) {
					classname = object.toString();
				}
				types.put(key, new JavaEnum(classname));
			}
		}

		// Classes
		Map<String, Object> beans = getMap("beans");
		if (beans != null) {
			for(String key : beans.keySet()) {
				JavaClass jc = new JavaClass(basePackage + ".bean", key);
				types.put(key, jc);
			}
		}		
	}

	private void createListActions() throws Exception {
		for(JavaClass jc : types.values()) {
			if(skipGui(jc)) continue;
			if (jc.isEnum()) continue;
			
			String jclower = jc.getName().toLowerCase();
			
			JavaClass lc = new JavaClass(basePackage + ".gui.admin.action." + jclower, "List" + jc.getName() + "Action");
			lc.setMethodSpacer("\n");
			
//			lc.addImport(new JavaType("net.sourceforge.stripes.action.DefaultHandler"));
			lc.addImport(new JavaType("net.sourceforge.stripes.action.ForwardResolution"));
			lc.addImport(new JavaType("net.sourceforge.stripes.action.Resolution"));
			lc.addImport(new JavaType("net.sourceforge.stripes.action.UrlBinding"));
			lc.addImport(jc.getJavaType());
			lc.addImport(JavaType.LOGIC_FACADE);
			
			lc.setExtendsion(new JavaType(basePackage + ".gui.admin.action.AbstractListAction"));
			lc.addAnnotation("@UrlBinding(\"/admin/" + jclower + "/list.action\")");

			JavaMethod jm = new JavaMethod(new JavaType("net.sourceforge.stripes.action.Resolution"), "execute");
				jm.setBody(Utils.createListActionExecuteMethod(jclower));
			lc.addMethod(jm);
		
			jm = new JavaMethod(JavaType.LONG, "_getItemCount");
				jm.setBody("\t\treturn " + JavaType.LOGIC_FACADE.getJustClassName() + ".getCount(" + jc.getName() + ".class);");
			lc.addMethod(jm);
			
			JavaType jt = new JavaType("java.util.List");
			jt.addType(new JavaType("?"));
			jm = new JavaMethod(jt, "_getItems");
				jm.setBody("\t\treturn " + JavaType.LOGIC_FACADE.getJustClassName() + ".list(" + jc.getName() + ".class, getItemOn(), getItemsPerPage());");
			lc.addMethod(jm);
			
			write(lc, false);

		}
	}

	private Object getValue(String path) {
		Map<String, Object> node = root;
		String[] keys = path.split("\\.");
		for(int i = 0; i < keys.length-1; i++) {
			Object onode = node.get(keys[i]);
			if ((node == null) || !(onode instanceof Map)) return null;
			node = (Map<String, Object>)onode;
		}
		if (node == null) return null;
		return node.get(keys[keys.length-1]);
	}

	private String getString(String path) {
		Object o = getValue(path);
		return (o == null)?null:o.toString();
	}

	private boolean getBoolean(String path, boolean defaultValue) {
		String value = getString(path);
		if (StringUtils.isEmpty(value)) {
			return defaultValue;
		} else {
			return Boolean.valueOf(value);
		}
	}
	
	private List<Object> getList(String path) {
		return toList(getValue(path));
	}
	
	private Map<String, Object> getMap(String path) {
		return toMap(getValue(path));
	}
	
	private List<Object> toList(Object value) {
		return (List<Object>)value;
	}
	
	private Map<String, Object> toMap(Object value) {
		return (Map<String, Object>)value;
	}
	
	
	private void write(JavaClass jclass) throws Exception {
		write(jclass, true);
	}

	private void write(JavaClass jclass, boolean overwrite) throws Exception {
		File file = classToFile(jclass);
		if (!overwrite && file.exists()) {return;}

		StringWriter swriter = new StringWriter();
		jclass.out(new PrintWriter(swriter));
	
		String newContent = swriter.toString();
		String currentContent = "";
		if (file.exists()) {
			currentContent = IOUtils.toString(new FileInputStream(file));
		}
				
		if (!newContent.equals(currentContent)) {
			PrintWriter out = openStream(jclass);
			out.print(newContent);
			out.flush();
			out.close();		
		}
	}

	private PrintWriter openStream(JavaClass jclass) throws Exception {
		return openStream(jclass.getPackage(), jclass.getName());
	}

	int fileCount = 0;
	private PrintWriter openStream(String pkg, String className) throws Exception {
		String path = sourcePath  + packageToPath(pkg);
		new File(path).mkdirs();
		path += "/" + className + ".java";
		System.out.println(++fileCount + "\t" + path);
		return new PrintWriter(new FileOutputStream(path));
	}
	
	private String packageToPath(String pkg) {
		return pkg.replace('.', '/');
	}
	
	private File classToFile(JavaClass jclass) {
		return new File(sourcePath  + packageToPath(jclass.getPackage()), jclass.getName() + ".java");
	}
	
	private File classToFile(String pkg, String classname) {
		return new File(sourcePath  + packageToPath(pkg), classname + ".java");		
	}
	
	private void velocity(File file, String template, Map<String, Object> map, boolean overwrite) throws Exception {
		if (!overwrite && file.exists()) return;

		file.getParentFile().mkdirs();
		
		StringWriter swriter = new StringWriter();
//		InputStream in = getClass().getClassLoader().getResourceAsStream(template);
		InputStream in = new FileInputStream(template);
		
		
		VelocityEngine ve = new VelocityEngine();
		VelocityContext context = new VelocityContext(map);
		ve.evaluate(context, new PrintWriter(swriter), "", new InputStreamReader(in));

		String newContent = swriter.toString();
		String currentContent = "";
		if (file.exists()) {
			currentContent = IOUtils.toString(new FileInputStream(file));
		}

		if (!newContent.equals(currentContent)) {
			System.out.println(++fileCount + "\t" + file.toString());
			PrintWriter out = new PrintWriter(new FileOutputStream(file));
			out.print(newContent);
			out.flush();
			out.close();
		}
	}
	
	/** @return whether or not the file's contents have been overwritten */
	private boolean content(File file, String newContent, boolean overwrite) throws Exception {
		if (!overwrite && file.exists()) return false;

		file.getParentFile().mkdirs();
		

		String currentContent = "";
		if (file.exists()) {
			currentContent = IOUtils.toString(new FileInputStream(file));
		}

		if (!newContent.equals(currentContent)) {
			System.out.println(++fileCount + "\t" + file.toString());
			PrintWriter out = new PrintWriter(new FileOutputStream(file));
			out.print(newContent);
			out.flush();
			out.close();
			return true;
		}
		
		return false;
	}	
}