package alife;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class Animal extends Agent {
    protected Animal(int id, Position position, int energy) {
        super(id, position, energy);
    }

    protected Position chooseNearest(Environment env, Class<? extends Agent> targetType) {
        return env.visibleAgents(position, Config.VISION).stream()
                .filter(a -> a != this && targetType.isInstance(a) && a.isAlive())
                .min(Comparator.comparingInt(a -> distance(position, a.getPosition())))
                .map(Agent::getPosition)
                .orElse(null);
    }


    protected Position chooseNearestSafePlant(Environment env) {
        return env.findNearestSafePlant((Herbivore) this);
    }

    protected List<Position> visibleTargets(Environment env, Class<? extends Agent> targetType) {
        List<Position> result = new ArrayList<>();
        for (Agent agent : env.visibleAgents(position, Config.VISION)) {
            if (agent != this && targetType.isInstance(agent) && agent.isAlive()) {
                result.add(agent.getPosition());
            }
        }
        return result;
    }

    protected List<Position> visibleThreats(Environment env, Class<? extends Agent> targetType) {
        List<Position> result = new ArrayList<>();
        if (!targetType.isAssignableFrom(Predator.class) && this instanceof Herbivore) {
            return result;
        }

        int radius = Config.VISION / 2;
        int minRow = Math.max(0, position.row() - radius);
        int maxRow = Math.min(env.getRows() - 1, position.row() + radius);
        int minCol = Math.max(0, position.col() - radius);
        int maxCol = Math.min(env.getCols() - 1, position.col() + radius);

        int[][] distance = new int[maxRow - minRow + 1][maxCol - minCol + 1];
        for (int r = 0; r < distance.length; r++) {
            java.util.Arrays.fill(distance[r], -1);
        }

        java.util.ArrayDeque<Position> queue = new java.util.ArrayDeque<>();
        queue.add(position);
        distance[position.row() - minRow][position.col() - minCol] = 0;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            for (int[] direction : directions) {
                Position next = current.move(direction[0], direction[1]);
                if (next.row() < minRow || next.row() > maxRow ||
                        next.col() < minCol || next.col() > maxCol) continue;

                int rr = next.row() - minRow;
                int cc = next.col() - minCol;
                if (distance[rr][cc] != -1) continue;

                Agent occupant = env.getOccupant(next);
                if (occupant != null) {
                    if (targetType.isInstance(occupant) && occupant.isAlive()) {
                        result.add(next);
                    }
                    // Любая занятая клетка является препятствием; через неё нельзя
                    // пройти, но сама клетка хищника может быть достигнутой целью.
                    continue;
                }

                distance[rr][cc] = distance[current.row() - minRow][current.col() - minCol] + 1;
                queue.addLast(next);
            }
        }
        return result;
    }

    protected void moveToward(Position target, Environment env) {
        int dr = Integer.compare(target.row(), position.row());
        int dc = Integer.compare(target.col(), position.col());

        // Движение только по вертикали или горизонтали.
        // Приоритет отдаем направлению, которое уменьшает расстояние до цели сильнее.
        List<Position> preferredDirections = new ArrayList<>();
        if (Math.abs(target.row() - position.row()) >= Math.abs(target.col() - position.col()) && dr != 0) {
            preferredDirections.add(position.move(dr, 0));
            if (dc != 0) preferredDirections.add(position.move(0, dc));
        } else if (dc != 0) {
            preferredDirections.add(position.move(0, dc));
            if (dr != 0) preferredDirections.add(position.move(dr, 0));
        }

        for (Position preferred : preferredDirections) {
            if (this instanceof Herbivore herbivore) {
                if (env.tryHerbivoreMove(herbivore, preferred)) return;
            } else if (env.canAnimalEnter(preferred, this)) {
                moveTo(preferred, env);
                return;
            }
        }

        List<Position> alternatives = env.animalNeighbors(position, this);
        if (this instanceof Herbivore) {
            for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                Position p = position.move(d[0], d[1]);
                Agent occupant = env.getOccupant(p);
                if (occupant instanceof Plant) alternatives.add(p);
            }
        }
        alternatives.sort(Comparator.comparingInt(p -> distance(p, target)));
        if (!alternatives.isEmpty()) {
            Position next = alternatives.get(0);
            if (this instanceof Herbivore herbivore) {
                env.tryHerbivoreMove(herbivore, next);
            } else {
                moveTo(next, env);
            }
        }
    }

    protected void randomMove(Environment env) {
        List<Position> choices = env.animalNeighbors(position, this);
        if (!choices.isEmpty()) {
            Position next = choices.get(env.random().nextInt(choices.size()));
            if (this instanceof Herbivore herbivore) env.tryHerbivoreMove(herbivore, next);
            else moveTo(next, env);
            return;
        }
        if (this instanceof Herbivore herbivore) {
            List<Position> plantChoices = new ArrayList<>();
            for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                Position p = position.move(d[0], d[1]);
                if (env.getOccupant(p) instanceof Plant) plantChoices.add(p);
            }
            if (!plantChoices.isEmpty()) {
                env.tryHerbivoreMove(herbivore, plantChoices.get(env.random().nextInt(plantChoices.size())));
            }
        }
    }

    protected void fleeFrom(List<Position> threats, Environment env) {
        if (threats.isEmpty()) {
            randomMove(env);
            return;
        }

        // Выбираем клетку, максимально увеличивающую расстояние до ВСЕХ видимых
        // угроз, а не только до ближайшего хищника. Это не позволяет убегать от
        // одного хищника прямо в сторону другого. При одинаковой безопасности
        // травоядное предпочитает клетку с растением и получает пищу по пути.
        List<Position> alternatives = env.animalNeighbors(position, this);
        if (this instanceof Herbivore) {
            for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                Position p = position.move(d[0], d[1]);
                if (env.getOccupant(p) instanceof Plant
                        && env.canHerbivoreEatPlantSafely((Herbivore) this, p)) {
                    alternatives.add(p);
                }
            }
        }

        alternatives.sort((a, b) -> {
            int safetyCompare = Integer.compare(minDistance(b, threats), minDistance(a, threats));
            if (safetyCompare != 0) return safetyCompare;
            if (this instanceof Herbivore) {
                boolean aPlant = env.getOccupant(a) instanceof Plant
                        && env.canHerbivoreEatPlantSafely((Herbivore) this, a);
                boolean bPlant = env.getOccupant(b) instanceof Plant
                        && env.canHerbivoreEatPlantSafely((Herbivore) this, b);
                if (aPlant != bPlant) return Boolean.compare(bPlant, aPlant);
            }
            return Integer.compare(distance(a, position), distance(b, position));
        });

        if (!alternatives.isEmpty()) {
            Position next = alternatives.get(0);
            if (this instanceof Herbivore herbivore) {
                env.tryHerbivoreMove(herbivore, next);
            } else {
                moveTo(next, env);
            }
        }
    }

    private static int minDistance(Position p, List<Position> points) {
        return points.stream().mapToInt(q -> distance(p, q)).min().orElse(Integer.MAX_VALUE);
    }

    protected static int distance(Position a, Position b) {
        return Math.abs(a.row() - b.row()) + Math.abs(a.col() - b.col());
    }
}
