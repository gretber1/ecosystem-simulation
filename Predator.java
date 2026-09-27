package alife;

import java.util.List;

public class Predator extends Animal {
    public Predator(int id, Position position, int energy) {
        super(id, position, energy);
    }

    private static boolean adjacent(Position a, Position b) {
        return Math.abs(a.row() - b.row()) + Math.abs(a.col() - b.col()) == 1;
    }

    @Override
    public void act(Environment env) {
        if (!isAlive()) return;

        spendEnergy(Config.PREDATOR_LOSS_PER_STEP);
        if (!isAlive()) return;

        // Логическая модель преподавателя:
        // ЕСЛИ видит травоядное -> идти к нему и атаковать.
        Position prey = chooseNearest(env, Herbivore.class);
        if (prey != null) {
            if (adjacent(position, prey)) {
                env.eatAnimal(this, prey);
            } else {
                moveToward(prey, env);
            }
        } else {
            // ИНАЧЕ блуждать случайно.
            randomMove(env);
        }

        if (energy >= Config.PREDATOR_BIRTH_THRESHOLD) {
            env.reproduce(this);
        }
    }

    @Override public char symbol() { return 'P'; }
    @Override public String typeName() { return "Хищник"; }
}
