package alife;

public final class Config {
    public static final int ROWS = 50;
    public static final int COLS = 50;

    // Животные видят квадрат 5×5, в центре которого находится сам агент.
    public static final int VISION = 5;

    // Начальная случайная расстановка. Ограничителей роста популяции нет.
    public static final int INITIAL_PLANTS = 1000;
    public static final int INITIAL_HERBIVORES = 80;
    public static final int INITIAL_PREDATORS = 12;

    // Растения: фиксированный рост и фиксированный порог размножения.
    // После старта новое растение может появиться только рядом с родителем.
    public static final int PLANT_START_ENERGY = 6;
    public static final int PLANT_GROWTH_PER_TICK = 6;
    public static final int PLANT_BIRTH_THRESHOLD = 80;
    public static final int PLANT_BIRTH_COST = 40;

    // Травоядные: фиксированный расход энергии и фиксированная награда за пищу.
    public static final int HERB_START_ENERGY = 90;
    public static final int HERB_FOOD_ENERGY = 40;
    public static final int HERB_LOSS_PER_STEP = 1;
    public static final int HERB_BIRTH_THRESHOLD = 4000;
    public static final int HERB_BIRTH_COST = 2000;

    // Хищники: фиксированные параметры охоты и размножения.
    public static final int PREDATOR_START_ENERGY = 3000;
    public static final int PREDATOR_FOOD_ENERGY = 4000;
    public static final int PREDATOR_LOSS_PER_STEP = 1;
    public static final int PREDATOR_BIRTH_THRESHOLD = 150000;
    public static final int PREDATOR_BIRTH_COST = 50000;

    private Config() {}
}
