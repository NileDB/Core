package security.fielddefinitions;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import graphql.language.Argument;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLTypeReference;
import helpers.DatabaseHelper;
import helpers.Helper;
import helpers.maps.SchemaMap;

public class FilterByCurrentUserDelete {
	public static GraphQLFieldDefinition.Builder builder = newFieldDefinition()
			.name("deleteFilterByCurrentUser")
			.description("It disables the filter by current authenticated user on the specified table.")
			.argument(newArgument()
					.name("table")
					.description("The table name.")
					.type(GraphQLNonNull.nonNull(GraphQLTypeReference.typeRef("EntityEnumType"))))
			.argument(newArgument()
					.name("rolename")
					.description("The role name.")
					.type(GraphQLNonNull.nonNull(GraphQLString)))
			.type(GraphQLString)
			.dataFetcher(new DataFetcher<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public String get(DataFetchingEnvironment environment) {
					List<Field> fields = environment.getFields();
					Connection connection = null;
					try {
						String table = null;
						String rolename = null;
						
						Field field = fields.get(0);
						for (int i = 0; i < field.getArguments().size(); i++) {
							Argument argument = field.getArguments().get(i);
							if (argument.getName().equals("table")) {
								table = (String) Helper.resolveValue(argument.getValue(), environment);
							}
							else if (argument.getName().equals("rolename")) {
								rolename = (String) Helper.resolveValue(argument.getValue(), environment);
							}
						}
						
						// PreparedStatement now allowed, so check to avoid SQL injection
						if (!table.matches("[_a-zA-Z][_0-9a-zA-Z]*")) {
							throw new Exception("Incorrect table name. Please, use this format: [_a-zA-Z][_0-9a-zA-Z]*");
						}
						if (!rolename.matches("[_a-zA-Z][_0-9a-zA-Z]*")) {
							throw new Exception("Incorrect role name. Please, use this format: [_a-zA-Z][_0-9a-zA-Z]*");
						}
						table = SchemaMap.entityNameByUnderscoredName.get(table);
						connection = DatabaseHelper.getConnection((String) ((Map<String, Object>) environment.getContext()).get("authorization"));
						PreparedStatement ps = connection.prepareStatement("DROP POLICY IF EXISTS filter_by_user_" + rolename + " ON " + table);
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
