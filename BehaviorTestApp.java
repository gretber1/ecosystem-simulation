package alife;

/** Небольшая проверка нового правила поведения травоядного. */
public class BehaviorTestApp {
    public static void main(String[] args) {
        testBlockedPredator();
        testPlantMustNotOpenPredator();
        testOpenPredator();
        System.out.println("BEHAVIOR_TESTS_OK");
    }

    private static void testBlockedPredator() {
        Environment env = new Environment(9, 9, 1L);
        Herbivore herb = new Herbivore(1, new Position(4, 4), 30);
        Predator predator = new Predator(2, new Position(4, 7), 50);

        Plant food = new Plant(3, new Position(4, 5), 4);
        // Дополнительная преграда сохраняется между новой позицией травоядного
        // и хищником, поэтому поедание food не открывает путь.
        Plant blocker = new Plant(4, new Position(4, 6), 4);

        env.addAgent(herb);
        env.addAgent(predator);
        env.addAgent(food);
        env.addAgent(blocker);

        herb.act(env);

        if (herb.getPosition().row() != 4 || herb.getPosition().col() != 5) {
            throw new AssertionError("Травоядное не съело растение перед закрытым хищником");
        }
        if (env.countPlants() != 1) {
            throw new AssertionError("Растение, доступное травоядному, не было съедено");
        }
    }

    private static void testPlantMustNotOpenPredator() {
        Environment env = new Environment(9, 9, 3L);
        Herbivore herb = new Herbivore(1, new Position(4, 4), 30);
        Predator predator = new Predator(2, new Position(4, 6), 50);
        // Растение находится между травоядным и хищником и само является единственной преградой.
        Plant food = new Plant(3, new Position(4, 5), 4);

        env.addAgent(herb);
        env.addAgent(predator);
        env.addAgent(food);

        herb.act(env);

        // Главное условие теста: растение-преграда осталось на месте.
        // Само травоядное может после этого сделать безопасный случайный шаг,
        // но удалять преграду ему нельзя.
        if (env.getOccupant(new Position(4, 5)) != food) {
            throw new AssertionError("Опасное растение было удалено, хотя оно закрывало хищника");
        }
    }

    private static void testOpenPredator() {
        Environment env = new Environment(9, 9, 2L);
        Herbivore herb = new Herbivore(1, new Position(4, 4), 30);
        Predator predator = new Predator(2, new Position(4, 6), 50);
        Plant food = new Plant(3, new Position(4, 3), 4);

        env.addAgent(herb);
        env.addAgent(predator);
        env.addAgent(food);

        herb.act(env);

        int distanceAfter = Math.abs(herb.getPosition().row() - predator.getPosition().row())
                + Math.abs(herb.getPosition().col() - predator.getPosition().col());
        if (distanceAfter <= 2) {
            throw new AssertionError("Травоядное не увеличило расстояние до открытого хищника");
        }
    }
}
