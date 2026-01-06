Create TYPE dish_type AS ENUM ('START', 'MAIN', 'DESSERT');

Create TYPE category_type AS ENUM ('VEGETABLE', 'ANIMAL', 'OTHER', 'DAIRY');

CREATE TABLE Dish(
    id int PRIMARY KEY ,
    name varchar(255)NOT NULL ,
    dish_type dish_type NOT NULL
);


CREATE TABLE Ingredient(
    id int PRIMARY KEY,
    name varchar(255)NOT NULL ,
    price DOUBLE PRECISION,
    category category_type ,
    id_dish int NOT NULL REFERENCES Dish(id)
)