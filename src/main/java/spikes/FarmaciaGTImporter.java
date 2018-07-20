package spikes;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class FarmaciaGTImporter {
	public static void main(String[] args) throws Exception {

		Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5433/niledb", "postgres", "postgres");
		connection.setAutoCommit(true);
		
		Statement statement = connection.createStatement();
		
		Path path = FileSystems.getDefault().getPath("/home/paco/projects/com.niledb.market/flows/in/", "productfeed.json");
		byte[] encoded = Files.readAllBytes(path);
		JsonArray products = new JsonObject(new String(encoded, "utf-8")).getJsonArray("products");
		
		Set<String> fieldNames = new HashSet<String>();
		
		for (int i = 0; i < products.size(); i++) {
			JsonObject product = products.getJsonObject(i);
			Set<String> fieldNames2 = product.fieldNames();
			fieldNames.addAll(fieldNames2);
		}
		
		for (String fieldName : fieldNames) {
			System.out.println(fieldName);
		}
		
		int maxDepth = 0;

		System.out.println("Size: " + products.size());
		
		for (int i = 0; i < products.size(); i++) {
			JsonObject product = products.getJsonObject(i);
			String sellerId = product.getString("ID");
			String name = product.getString("name").replaceAll("'", "''");
			String description = product.getString("description");
			if (description != null) {
				description = description.replaceAll("'", "''");
			}
			String url = product.getString("URL");
			double salePrice = product.getJsonObject("price").getDouble("amount");
			String imageUrl = product.getJsonArray("images").getString(0);
			JsonObject properties = product.getJsonObject("properties");
			String condition = properties.getJsonArray("condition").getString(0);
			String stock = properties.getJsonArray("stock").getString(0);
			String gtin = properties.getJsonArray("GTIN") != null ? (properties.getJsonArray("GTIN").size() > 0 ? properties.getJsonArray("GTIN").getString(0) : null) : null;
			String brand = properties.getJsonArray("brand") != null ? (properties.getJsonArray("brand").size() > 0 ? properties.getJsonArray("brand").getString(0) : null) : null;
			String mpn = properties.getJsonArray("MPN").getString(0);
			
			Integer hierarchyId = null;
			
			String hierarchyString = properties.getJsonArray("extraInfo") != null ? (properties.getJsonArray("extraInfo").size() > 0 ? properties.getJsonArray("extraInfo").getString(0) : null) : null;

			Integer collectionId = null;
			
			if (hierarchyString != null) {
				ResultSet rs = statement.executeQuery("SELECT \"id\", \"name\" FROM \"Products\".\"Collection\" WHERE \"name\" = '" + hierarchyString + "'");
				if (rs.next()) {
					collectionId = rs.getInt("id");
				}
				else {
					ResultSet rs2 = statement.executeQuery("INSERT INTO \"Products\".\"Collection\" (\"name\") VALUES ('" + hierarchyString + "') RETURNING \"id\"");
					if (rs2.next()) {
						collectionId = rs2.getInt("id");
					}
					rs2.close();
				}
				rs.close();
				
				Integer parent = null;
				String[] categories = hierarchyString.split(" > ");
				for (int j = 0; j < categories.length; j++) {
					rs = statement.executeQuery("SELECT \"id\", \"name\", \"parent\" FROM \"Products\".\"Hierarchy\" WHERE \"parent\" " + (parent == null ? "IS" : "=") + " " + parent + " AND \"name\" = '" + categories[j] + "'");
					if (rs.next()) {
						parent = rs.getInt("id");
					}
					else {
						ResultSet rs2 = statement.executeQuery("INSERT INTO \"Products\".\"Hierarchy\" (\"name\", \"parent\") VALUES ('" + categories[j] + "', " + parent + ") RETURNING \"id\"");
						if (rs2.next()) {
							parent = rs2.getInt("id");
						}
						rs2.close();
					}
					rs.close();
				}
				hierarchyId = parent;
				
				rs = statement.executeQuery("SELECT \"id\" FROM \"Products\".\"HierarchyCollection\" WHERE \"hierarchy\" = " + hierarchyId + " AND \"collection\" = " + collectionId);
				if (!rs.next()) {
					statement.execute("INSERT INTO \"Products\".\"HierarchyCollection\" (\"hierarchy\", \"collection\") VALUES (" + hierarchyId + ", " + collectionId + ")");
				}
				rs.close();
			}
			
			ResultSet rs = statement.executeQuery("SELECT \"id\" FROM \"Products\".\"Item\" WHERE \"name\" = '" + name + "' AND \"brand\" = '" + brand + "'");
			if (!rs.next()) {
				ResultSet rs2 = statement.executeQuery("INSERT INTO \"Products\".\"Item\" (\"name\", \"description\", \"brand\", \"salePrice\", \"originalPrice\", \"stock\", \"seller\", \"url\", \"imageUrl\") VALUES ('" + name + "', '" + description + "', '" + brand + "', " + salePrice + ", " + salePrice + ", " + (stock.equals("in stock") ? 10 : 0) + ", 'FarmaciaGT', '" + url + "', '" + imageUrl + "') RETURNING \"id\"");
				if (rs2.next()) {
					Integer itemId = rs2.getInt("id");
					statement.execute("INSERT INTO \"Products\".\"Variant\" (\"item\", \"gtin13\", \"mpn\", \"spn\") VALUES (" + itemId + ", '" + gtin + "', '" + mpn + "', '" + sellerId + "')");
					if (collectionId != null) {
						statement.execute("INSERT INTO \"Products\".\"CollectionItem\" (\"collection\", \"item\") VALUES (" + collectionId + ", " + itemId + ")");
					}
				}
				rs2.close();
			}
			rs.close();
		}
		connection.close();
	}
}
