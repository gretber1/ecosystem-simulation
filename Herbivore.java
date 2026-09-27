package alife;

import java.util.List;

public class Herbivore extends Animal {
    public Herbivore(int id, Position position, int energy) {
        super(id, position, energy);
    }

    private static boolean adjacent(Position a, Position b) {
        return Math.abs(a.row() - b.row()) + Math.abs(a.col() - b.col()) == 1;
    }

    @Override
    public void act(Environment env) {
        if (!isAlive()) return;

        spendEnergy(Config.HERB_LOSS_PER_STEP);
        if (!isAlive()) return;

        // Уточнённая логическая модель поведения:
        // 1) Если рядом есть растение и нет непосредственной угрозы -> съесть его.
        // 2) ИНАЧЕ если в зоне 5×5 есть доступный хищник на расстоянии не более
        //    двух шагов -> убегать.
        // 3) ИНАЧЕ если растение видно -> двигаться к нему.
        // 4) ИНАЧЕ -> блуждать случайно.
        // Такой порядок не заставляет травоядное стоять рядом с едой из-за дальнего
        // или физически закрытого хищника.
        List<Position> predators = visibleThreats(env, Predator.class);

        if (!predators.isEmpty()) {
            fleeFrom(predators, env);
        } else {
            // Здесь могут оставаться видимые, но физически закрытые хищники.
            // Поэтому растение выбирается с дополнительной проверкой: его удаление
            // не должно открыть путь такому хищнику.
            Position safePlant = chooseNearestSafePlant(env);
            if (safePlant != null && adjacent(position, safePlant)) {
                env.eatPlant(this, safePlant);
            } else if (safePlant != null) {
                moveToward(safePlant, env);
            } else {
                randomMove(env);
            }
        }

        if (energy >= Config.HERB_BIRTH_THRESHOLD) {
            env.reproduce(this);
        }
    }

    @Override public char symbol() { return 'H'; }
    @Override public String typeName() { return "Травоядное"; }
}
