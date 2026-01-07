import java.util.List;

public class Dish{
    private int id;
    private String name;
    private DishTypeEnum dishType;
    private List<Ingredient> ingredient;

    public Dish() {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.ingredient = ingredient;
    }

    public Double getDishPrice() {
        double total = 0.0;

        for (Ingredient ingredient : ingredient) {
            total += ingredient.getPrice();
        }

        return total;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DishTypeEnum getDishType() {
        return dishType;
    }

    public List<Ingredient> getIngredient() {
        return ingredient;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDishType(DishTypeEnum dishType) {
        this.dishType = dishType;
    }

    public void setIngredient(List<Ingredient> ingredient) {
        this.ingredient = ingredient;
    }
}
