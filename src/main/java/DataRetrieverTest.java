import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import static javax.management.Query.times;
import static jdk.internal.classfile.impl.verifier.VerifierImpl.verify;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class DataRetrieverTest {

    @Mock
    private DBConnection dbConnection;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private DataRetriever dataRetriever;

    @BeforeEach
    void setUp() throws SQLException {
        when(dbConnection.getDBConnection()).thenReturn(connection);
    }

    // Test a) Dish findDishById(Integer id); id = 1;
    @Test
    void testA_FindDishById_WithIngredients() throws SQLException {
        // Arrange
        int dishId = 1;

        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Configurer le ResultSet pour le plat
        when(resultSet.next())
                .thenReturn(true)  // Plat trouvé
                .thenReturn(true)  // Premier ingrédient
                .thenReturn(true)  // Deuxième ingrédient
                .thenReturn(false); // Fin des ingrédients

        // Données du plat
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("name")).thenReturn("Salade Fraîche");
        when(resultSet.getString("dish_type")).thenReturn("START");

        // Données des ingrédients
        when(resultSet.getInt("id")).thenReturn(101, 102);
        when(resultSet.getString("name")).thenReturn("Laitue", "Tomate");
        when(resultSet.getDouble("price")).thenReturn(2.0, 1.5);
        when(resultSet.getString("category")).thenReturn("VEGETABLE", "VEGETABLE");

        // Act
        Dish result = dataRetriever.findDishById(dishId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Salade Fraîche", result.getName());
        assertEquals(DishTypeEnum.START, result.getDishType());
        assertEquals(2, result.getIngredients().size());
        assertEquals("Laitue", result.getIngredients().get(0).getName());
        assertEquals("Tomate", result.getIngredients().get(1).getName());
    }

    // Test b) Dish findDishById(Integer id); id = 999;
    @Test
    void testB_FindDishById_NotFound() throws SQLException {
        // Arrange
        int dishId = 999;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Aucun plat trouvé

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> dataRetriever.findDishById(dishId)
        );

        assertEquals("Dish not found", exception.getMessage());
    }


    @Test
    void testC_FindIngredients_Page2Size2() throws SQLException {
        // Arrange
        int page = 2;
        int size = 2;
        int offset = page * size; // 4

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Simuler 2 ingrédients (Poulet, Chocolat)
        when(resultSet.next()).thenReturn(true, true, false);

        when(resultSet.getInt("id")).thenReturn(5, 6);
        when(resultSet.getString("name")).thenReturn("Poulet", "Chocolat");
        when(resultSet.getDouble("price")).thenReturn(10.0, 5.0);
        when(resultSet.getString("category")).thenReturn("MEAT", "SWEET");

        // Act
        List<Ingredient> result = dataRetriever.findIngredients(page, size);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Poulet", result.get(0).getName());
        assertEquals("Chocolat", result.get(1).getName());

        verify(preparedStatement).setInt(1, size);
        verify(preparedStatement).setInt(2, offset);
    }

    // Test d) List<Ingredient> findIngredients(int page, int size); page=3; size=5;
    @Test
    void testD_FindIngredients_Page3Size5_Empty() throws SQLException {
        // Arrange
        int page = 3;
        int size = 5;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Aucun résultat

        // Act
        List<Ingredient> result = dataRetriever.findIngredients(page, size);

        // Assert
        assertTrue(result.isEmpty());
        verify(preparedStatement).setInt(1, size);
        verify(preparedStatement).setInt(2, page * size);
    }

    // Test e) List<Dish> findDishsByIngredientName(String ingredientName); ingredientName="eur"
    @Test
    void testE_FindDishesByIngredientName_Eur() throws SQLException {
        // Arrange
        String ingredientName = "eur";

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);

        when(resultSet.getInt("id")).thenReturn(3);
        when(resultSet.getString("name")).thenReturn("Gâteau au chocolat");
        when(resultSet.getString("dish_type")).thenReturn("dessert");

        // Act
        List<Dish> result = dataRetriever.findDishsByIngredientName(ingredientName);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Gâteau au chocolat", result.get(0).getName());
        assertEquals(DishTypeEnum.DESSERT, result.get(0).getDishType());

        verify(preparedStatement).setString(1, "%" + ingredientName + "%");
    }

    // Test f) findIngredientsByCriteria avec category=VEGETABLE
    @Test
    void testF_FindIngredientsByCriteria_VegetablesOnly() throws SQLException {
        // Arrange
        CategoryEnum category = CategoryEnum.VEGETABLE;
        int page = 1;
        int size = 10;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Simuler 2 légumes
        when(resultSet.next()).thenReturn(true, true, false);

        when(resultSet.getInt("id")).thenReturn(1, 2);
        when(resultSet.getString("name")).thenReturn("Laitue", "Tomate");
        when(resultSet.getDouble("price")).thenReturn(2.0, 1.5);
        when(resultSet.getString("category")).thenReturn("VEGETABLE", "VEGETABLE");

        // Act
        List<Ingredient> result = dataRetriever.findIngredientsByCriteria(
                null, category, null, page, size
        );

        // Assert
        assertEquals(2, result.size());
        assertEquals("Laitue", result.get(0).getName());
        assertEquals("Tomate", result.get(1).getName());

        // Vérifier que seul le category est set
        verify(preparedStatement).setString(1, category.name());
        verify(preparedStatement).setInt(2, size);
        verify(preparedStatement).setInt(3, page * size);
    }

    // Test g) findIngredientsByCriteria avec ingredientName="cho" et dishName="Sal"
    @Test
    void testG_FindIngredientsByCriteria_ChoAndSal_Empty() throws SQLException {
        // Arrange
        String ingredientName = "cho";
        String dishName = "Sal";
        int page = 1;
        int size = 10;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Aucun résultat

        // Act
        List<Ingredient> result = dataRetriever.findIngredientsByCriteria(
                ingredientName, null, dishName, page, size
        );

        // Assert
        assertTrue(result.isEmpty());

        verify(preparedStatement).setString(1, "%" + ingredientName + "%");
        verify(preparedStatement).setString(2, "%" + dishName + "%");
        verify(preparedStatement).setInt(3, size);
        verify(preparedStatement).setInt(4, page * size);
    }

    // Test h) findIngredientsByCriteria avec ingredientName="cho" et dishName="gâteau"
    @Test
    void testH_FindIngredientsByCriteria_ChoAndGateau() throws SQLException {
        // Arrange
        String ingredientName = "cho";
        String dishName = "gâteau";
        int page = 1;
        int size = 10;

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);

        when(resultSet.getInt("id")).thenReturn(6);
        when(resultSet.getString("name")).thenReturn("Chocolat");
        when(resultSet.getDouble("price")).thenReturn(5.0);
        when(resultSet.getString("category")).thenReturn("SWEET");

        // Act
        List<Ingredient> result = dataRetriever.findIngredientsByCriteria(
                ingredientName, null, dishName, page, size
        );

        // Assert
        assertEquals(1, result.size());
        assertEquals("Chocolat", result.get(0).getName());

        verify(preparedStatement).setString(1, "%" + ingredientName + "%");
        verify(preparedStatement).setString(2, "%" + dishName + "%");
    }

    // Test i) createIngredients avec Fromage et Oignon
    @Test
    void testI_CreateIngredients_NewIngredients() throws SQLException {
        // Arrange
        List<Ingredient> newIngredients = new ArrayList<>();

        Ingredient fromage = new Ingredient();
        fromage.setName("Fromage");
        fromage.setCategory(CategoryEnum.DAIRY);
        fromage.setPrice(1200.0);
        newIngredients.add(fromage);

        Ingredient oignon = new Ingredient();
        oignon.setName("Oignon");
        oignon.setCategory(CategoryEnum.VEGETABLE);
        oignon.setPrice(500.0);
        newIngredients.add(oignon);

        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(preparedStatement)
                .thenReturn(preparedStatement)
                .thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Première vérification - Fromage n'existe pas
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        // Insertion Fromage
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(7);

        // Deuxième vérification - Oignon n'existe pas
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);


        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(8);

        List<Ingredient> result = dataRetriever.createIngredients(newIngredients);

        assertEquals(2, result.size());
        assertEquals("Fromage", result.get(0).getName());
        assertEquals("Oignon", result.get(1).getName());
        assertEquals(7, result.get(0).getId());
        assertEquals(8, result.get(1).getId());

        verify(connection).setAutoCommit(false);
        verify(connection).commit();
    }

    @Test
    void testJ_CreateIngredients_DuplicateIngredient() throws SQLException {
        List<Ingredient> newIngredients = new ArrayList<>();

        Ingredient carotte = new Ingredient();
        carotte.setName("Carotte");
        carotte.setCategory(CategoryEnum.VEGETABLE);
        carotte.setPrice(2000.0);
        newIngredients.add(carotte);

        Ingredient laitue = new Ingredient();
        laitue.setName("Laitue");
        laitue.setCategory(CategoryEnum.VEGETABLE);
        laitue.setPrice(2000.0);
        newIngredients.add(laitue);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);


        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> dataRetriever.createIngredients(newIngredients)
        );

        assertTrue(exception.getMessage().contains("Laitue"));
        assertTrue(exception.getMessage().contains("existe déjà"));

        verify(connection).rollback();
    }

    @Test
    void testK_SaveDish_NewDish() throws SQLException {
        // Arrange
        Dish newDish = new Dish();
        newDish.setName("Soupe de légumes");
        newDish.setDishType(DishTypeEnum.START);

        Ingredient oignon = new Ingredient();
        oignon.setId(8); // ID de l'oignon créé précédemment
        oignon.setName("Oignon");

        List<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(oignon);
        newDish.setIngredients(ingredients);

        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(4);


        Dish result = dataRetriever.saveDish(newDish);

        assertNotNull(result);
        assertEquals(4, result.getId());
        assertEquals("Soupe de légumes", result.getName());

        verify(connection).setAutoCommit(false);
        verify(preparedStatement).setString(1, "Soupe de légumes");
        verify(preparedStatement).setString(2, "START");
        verify(connection).commit();
    }

    @Test
    void testL_SaveDish_UpdateAddIngredients() throws SQLException {

        Dish updatedDish = new Dish();
        updatedDish.setId(1);
        updatedDish.setName("Salade fraîche");
        updatedDish.setDishType(DishTypeEnum.START);

        List<Ingredient> ingredients = new ArrayList<>();

        Ingredient oignon = new Ingredient();
        oignon.setId(8);
        ingredients.add(oignon);

        Ingredient laitue = new Ingredient();
        laitue.setId(1);
        ingredients.add(laitue);

        Ingredient tomate = new Ingredient();
        tomate.setId(2);
        ingredients.add(tomate);

        Ingredient fromage = new Ingredient();
        fromage.setId(7);
        ingredients.add(fromage);

        updatedDish.setIngredients(ingredients);

        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(preparedStatement)
                .thenReturn(preparedStatement);


        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeBatch()).thenReturn(new int[]{1, 1, 1, 1});


        Dish result = dataRetriever.saveDish(updatedDish);


        assertEquals(1, result.getId());
        assertEquals(4, result.getIngredients().size());


        verify(preparedStatement).setInt(1, 1); // Pour DELETE

        verify(preparedStatement, times(4)).setInt(1, 1);
    }

    @Test
    void testM_SaveDish_UpdateRemoveIngredients() throws SQLException {
        // Arrange
        Dish updatedDish = new Dish();
        updatedDish.setId(1);
        updatedDish.setName("Salade de fromage");
        updatedDish.setDishType(DishTypeEnum.START);

        // Seul Fromage reste
        List<Ingredient> ingredients = new ArrayList<>();
        Ingredient fromage = new Ingredient();
        fromage.setId(7);
        ingredients.add(fromage);
        updatedDish.setIngredients(ingredients);

        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(preparedStatement)
                .thenReturn(preparedStatement);

        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeBatch()).thenReturn(new int[]{1});

        // Act
        Dish result = dataRetriever.saveDish(updatedDish);

        // Assert
        assertEquals("Salade de fromage", result.getName());
        assertEquals(1, result.getIngredients().size());

        // Vérifier que les associations sont supprimées
        verify(preparedStatement).setInt(1, 1); // Pour DELETE

        // Vérifier que seule l'association avec Fromage est créée
        verify(preparedStatement, times(1)).setInt(1, 1); // Pour UPDATE d'ingrédient
        verify(preparedStatement, times(1)).setInt(2, 7); // ID Fromage
    }

    @Test
    void testSaveDish_WithTransactionRollback() throws SQLException {
        // Arrange
        Dish dish = new Dish();
        dish.setId(1);
        dish.setName("Test Dish");

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> dataRetriever.saveDish(dish)
        );

        assertTrue(exception.getMessage().contains("Erreur lors de la sauvegarde"));
        verify(connection).rollback();
    }
}