public class Ingredient {
    private int id;
    private String name;
    private double price;
    private CategoryEnum category;
    Dish dish;

    public Ingredient(int id, String name, double price, CategoryEnum category,  Dish dish) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.dish = dish;
    }

    public String getDishName() {
        if (dish != null) {
            return dish.getName();
        }
        return null;
    }

    public double getPrice() {
        return price;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setCategory(CategoryEnum category) {
        this.category = category;
    }
}
