import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {

    Dish findDishById(int id) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = null;
        try {
            String dishQuery = "SELECT id, name, dish_type FROM dish WHERE id = ?";
            connection = dbConnection.getDBConnection();
            PreparedStatement dishStatement = connection.prepareStatement(dishQuery);
            dishStatement.setInt(1, id);
            ResultSet dishRs = dishStatement.executeQuery();

            if (!dishRs.next()) {
                throw new RuntimeException("Dish not found");
            }

            Dish dish = new Dish();
            dish.setId(dishRs.getInt("id"));
            dish.setName(dishRs.getString("name"));
            dish.setDishType(DishTypeEnum.valueOf(dishRs.getString("dish_type")));

            // Récupérer les ingrédients
            String ingredientQuery = "SELECT id, name, price, category FROM ingredient WHERE id_dish = ?";
            PreparedStatement ingredientStatement = connection.prepareStatement(ingredientQuery);
            ingredientStatement.setInt(1, id);
            ResultSet ingredientRs = ingredientStatement.executeQuery();

            List<Ingredient> ingredients = new ArrayList<>();
            while (ingredientRs.next()) {
                Ingredient ingredient = new Ingredient();
                ingredient.setId(ingredientRs.getInt("id"));
                ingredient.setName(ingredientRs.getString("name"));
                ingredient.setPrice(ingredientRs.getDouble("price"));
                String categoryStr = ingredientRs.getString("category");
                if (categoryStr != null) {
                    ingredient.setCategory(CategoryEnum.valueOf(categoryStr));
                }
                ingredients.add(ingredient);
            }
            dish.setIngredients(ingredients);

            return dish;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Méthodes existantes (inchangées)
    List<Ingredient> findIngredients(int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM Ingredient ORDER BY id LIMIT ? OFFSET ?";

        try (Connection conn = DBConnection.getDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, size);
            pstmt.setInt(2, page * size);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Ingredient ing = new Ingredient();
                ing.setId(rs.getInt("id"));
                ing.setName(rs.getString("name"));
                ing.setPrice(rs.getDouble("price"));

                String categoryStr = rs.getString("category");
                if (categoryStr != null) {
                    ing.setCategory(CategoryEnum.valueOf(categoryStr));
                }

                ingredients.add(ing);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ingredients;
    }

    List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        Connection conn = null;
        List<Ingredient> createdIngredients = new ArrayList<>();

        try {
            conn = DBConnection.getDBConnection();
            conn.setAutoCommit(false);

            String checkSql = "SELECT count(*) FROM Ingredient WHERE name = ?";
            String insertSql = "INSERT INTO Ingredient (name, price, category) VALUES (?, ?, ?::category_enum) RETURNING id";

            for (Ingredient ing : newIngredients) {
                // Vérifier si l'ingrédient existe déjà
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, ing.getName());
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new RuntimeException("L'ingrédient " + ing.getName() + " existe déjà.");
                    }
                }

                // Insérer le nouvel ingrédient
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, ing.getName());
                    insertStmt.setDouble(2, ing.getPrice());
                    insertStmt.setString(3, ing.getCategory().name());

                    ResultSet rs = insertStmt.executeQuery();
                    if (rs.next()) {
                        ing.setId(rs.getInt("id"));
                        createdIngredients.add(ing);
                    }
                }
            }
            conn.commit();
            return createdIngredients;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new RuntimeException("Opération annulée : " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public List<Dish> findDishsByIngredientName(String ingredientName) {
        List<Dish> dishes = new ArrayList<>();

        String sql = "SELECT d.* FROM Dish d " +
                "JOIN Ingredient i ON d.id = i.id_dish " +
                "WHERE i.name ILIKE ?";

        try (Connection conn = DBConnection.getDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + ingredientName + "%");

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Dish dish = new Dish();
                dish.setId(rs.getInt("id"));
                dish.setName(rs.getString("name"));

                String typeStr = rs.getString("dish_type");
                if (typeStr != null) {
                    dish.setDishType(DishTypeEnum.valueOf(typeStr.toUpperCase()));
                }

                dishes.add(dish);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dishes;
    }

    List<Ingredient> findIngredientsByCriteria(String ingredientName, CategoryEnum category,
                                               String dishName, int page, int size) {
        List<Ingredient> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT i.* FROM Ingredient i LEFT JOIN Dish d ON i.id_dish = d.id WHERE 1=1");

        if (ingredientName != null) sql.append(" AND i.name ILIKE ?");
        if (category != null) sql.append(" AND i.category = ?::category_enum");
        if (dishName != null) sql.append(" AND d.name ILIKE ?");

        sql.append(" ORDER BY i.id LIMIT ? OFFSET ?");

        try (Connection conn = DBConnection.getDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (ingredientName != null) pstmt.setString(paramIndex++, "%" + ingredientName + "%");
            if (category != null) pstmt.setString(paramIndex++, category.name());
            if (dishName != null) pstmt.setString(paramIndex++, "%" + dishName + "%");

            pstmt.setInt(paramIndex++, size);
            pstmt.setInt(paramIndex++, page * size);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Ingredient ing = new Ingredient();
                ing.setId(rs.getInt("id"));
                ing.setName(rs.getString("name"));
                ing.setPrice(rs.getDouble("price"));
                ing.setCategory(CategoryEnum.valueOf(rs.getString("category")));
                results.add(ing);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return results;
    }

    Dish saveDish(Dish dish) {
        Connection conn = null;

        try {
            conn = DBConnection.getDBConnection();
            conn.setAutoCommit(false);

            if (dish.getId() == 0) {
                String insertDishSql = "INSERT INTO Dish (name, dish_type) VALUES (?, ?::dish_type_enum) RETURNING id";
                try (PreparedStatement pstmt = conn.prepareStatement(insertDishSql)) {
                    pstmt.setString(1, dish.getName());
                    pstmt.setString(2, dish.getDishType().name());

                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        dish.setId(rs.getInt("id"));
                    }
                }
            } else {
                String updateDishSql = "UPDATE Dish SET name = ?, dish_type = ?::dish_type_enum WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateDishSql)) {
                    pstmt.setString(1, dish.getName());
                    pstmt.setString(2, dish.getDishType().name());
                    pstmt.setInt(3, dish.getId());
                    pstmt.executeUpdate();
                }

                String deleteAssociationsSql = "UPDATE Ingredient SET id_dish = NULL WHERE id_dish = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteAssociationsSql)) {
                    pstmt.setInt(1, dish.getId());
                    pstmt.executeUpdate();
                }
            }

            if (dish.getIngredient() != null && !dish.getIngredient().isEmpty()) {
                String updateIngredientSql = "UPDATE Ingredient SET id_dish = ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateIngredientSql)) {
                    for (Ingredient ingredient : dish.getIngredients()) {
                        pstmt.setInt(1, dish.getId());
                        pstmt.setInt(2, ingredient.getId());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            }

            conn.commit();
            return dish;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new RuntimeException("Erreur lors de la sauvegarde du plat: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}