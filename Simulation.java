package alife;

import java.util.concurrent.ThreadLocalRandom;

public class Simulation {
    private final Environment environment;
    private final long seed;

    /** Новый случайный seed для каждого нового запуска симуляции. */
    public Simulation() {
        this(ThreadLocalRandom.current().nextLong());
    }

    /** Конструктор с seed нужен для воспроизводимых тестов. */
    public Simulation(long seed) {
        this.seed = seed;
        environment = new Environment(Config.ROWS, Config.COLS, seed);
        environment.initialize();
    }

    public Environment environment() { return environment; }
    public void step() { environment.tick(); }
    public long seed() { return seed; }
}
