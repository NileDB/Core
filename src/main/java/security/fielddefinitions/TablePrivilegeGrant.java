package security.fielddefinitions;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.Value;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLTypeReference;
import helpers.DatabaseHelper;
import helpers.Helper;
import helpers.maps.SchemaMap;

public class TablePrivilegeGrant {
	public static GraphQLFieldDefinition.Builder builder = newFieldDefinition()
			.name("__tablePrivilegeGrant")
			.description("It grants table access privileges to the specified roles.")
			.argument(newArgument()
					.name("privileges")
					.description("The list of privileges to grant.")
					.type(GraphQLNonNull.nonNull(GraphQLList.list(newEnum()
							.name("TablePrivilegeType")
							.value("SELECT")
							.value("INSERT")
							.value("UPDATE")
							.value("DELETE")
							.build()))))
			.argument(newArgument()
					.name("tables")
					.description("The list of tables the privileges are granted on.")
					.type(GraphQLNonNull.nonNull(GraphQLList.list(GraphQLTypeReference.typeRef("EntityEnumType")))))
			.argument(newArgument()
					.name("roles")
					.description("The list of roles the privileges are granted to.")
					.type(GraphQLNonNull.nonNull(GraphQLList.list(GraphQLString))))
			.type(GraphQLString)
			.dataFetcher(new DataFetcher<String>() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public String get(DataFetchingEnvironment environment) {
					List<Field> fields = environment.getFields();
					Connection connection = null;
					try {
						List<String> privileges = null;
						List<String> tables = null;
						List<String> roles = null;
						Field field = fields.get(0);
						for (int i = 0; i < field.getArguments().size(); i++) {
							Argument argument = field.getArguments().get(i);
							if (argument.getName().equals("privileges")) {
								List<Value> values = (List<Value>) Helper.resolveValue(argument.getValue(), environment);
								if (values.size() > 0) {
									privileges = new ArrayList<String>();
									for (int j = 0; j < values.size(); j++) {
										privileges.add((String) Helper.resolveValue(values.get(j), environment));
									}
								}
							}
							else if (argument.getName().equals("tables")) {
								List<Value> values = (List<Value>) Helper.resolveValue(argument.getValue(), environment);
								if (values.size() > 0) {
									tables = new ArrayList<String>();
									for (int j = 0; j < values.size(); j++) {
										String table = (String) Helper.resolveValue(values.get(j), environment);
										if (!table.matches("[_a-zA-Z][_0-9a-zA-Z]*")) {
											throw new Exception("Incorrect table name. Please, use this format: [_a-zA-Z][_0-9a-zA-Z]*");
										}
										tables.add(SchemaMap.entityNameByUnderscoredName.get(table));
									}
								}
							}
							else if (argument.getName().equals("roles")) {
								List<Value> values = (List<Value>) Helper.resolveValue(argument.getValue(), environment);
								if (values.size() > 0) {
									roles = new ArrayList<String>();
									for (int j = 0; j < values.size(); j++) {
										String role = (String) Helper.resolveValue(values.get(j), environment);
										if (!role.matches("[_a-zA-Z][_0-9a-zA-Z]*")) {
											throw new Exception("Incorrect role name. Please, use this format: [_a-zA-Z][_0-9a-zA-Z]*");
										}
										roles.add(role);
									}
								}
							}
						}
						
						// PreparedStatement now allowed, so check to avoid SQL injection
						connection = DatabaseHelper.getConnection((String) ((Map<String, Object>) environment.getContext()).get("authorization"));
						StringBuffer sb = new StringBuffer("GRANT ");
						for (int i = 0; i < privileges.size(); i++) {
							sb.append((i > 0 ? ", " : "") + privileges.get(i));
						}
						sb.append(" ON ");
						for (int i = 0; i < tables.size(); i++) {
							sb.append((i > 0 ? ", " : "") + tables.get(i));
						}
						sb.append(" TO ");
						for (int i = 0; i < roles.size(); i++) {
							sb.append((i > 0 ? ", " : "") + roles.get(i));
						}
						
						PreparedStatement ps = connection.prepareStatement(sb.toString());
						ps.execute();
					}
					catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e.getMessage());
					}
					finally {
						try {
							if (connection != null) {
								connection.close();
							}
						}
						catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e.getMessage());
						}
					}
					return "ok";
				}
			});
}
