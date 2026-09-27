package alife;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Environment {
    private final Cell[][] cells;
    private final List<Agent> agents = new ArrayList<>();
    private final Random random;
    private int nextId = 1;
    private int step = 0;
    private int plantCount;
    private int herbivoreCount;
    private int predatorCount;

    private long plantBirths;
    private long herbivoreBirths;
    private long predatorBirths;
    private long plantsEaten;
    private long herbivoresEaten;
    private long plantDeaths;
    private long herbivoreDeaths;
    private long predatorDeaths;

    public Environment(int rows, int cols, long seed) {
        cells = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell();
            }
        }
        random = new Random(seed);
    }

    public void initialize() {
        for (int i = 0; i < Config.INITIAL_PLANTS; i++) {
            Position p = randomFreePosition();
            if (p != null) {
                addAgent(new Plant(nextId++, p, Config.PLANT_START_ENERGY));
            }
        }
        for (int i = 0; i < Config.INITIAL_HERBIVORES; i++) {
            Position p = randomFreePosition();
            if (p != null) {
                addAgent(new Herbivore(nextId++, p, Config.HERB_START_ENERGY));
            }
        }
        for (int i = 0; i < Config.INITIAL_PREDATORS; i++) {
            Position p = randomFreePosition();
            if (p != null) {
                addAgent(new Predator(nextId++, p, Config.PREDATOR_START_ENERGY));
            }
        }
    }

    void addAgent(Agent agent) {
        cells[agent.position.row()][agent.position.col()].setOccupant(agent);
        agents.add(agent);
        incrementCount(agent);
    }

    private void incrementCount(Agent agent) {
        if (agent instanceof Plant) plantCount++;
        else if (agent instanceof Herbivore) herbivoreCount++;
        else if (agent instanceof Predator) predatorCount++;
    }

    private void decrementCount(Agent agent) {
        if (agent instanceof Plant) plantCount--;
        else if (agent instanceof Herbivore) herbivoreCount--;
        else if (agent instanceof Predator) predatorCount--;
    }

    public void tick() {
        step++;
        List<Agent> order = new ArrayList<>(agents);
        Collections.shuffle(order, random);

        // Агент действует не более одного раза за такт.
        // Новые агенты, созданные во время такта, начнут действовать со следующего такта.
        // Погибшие агенты помечаются energy <= 0 и удаляются из списка после завершения
        // всех действий — это одновременно корректно и существенно быстрее.
        for (Agent agent : order) {
            if (agent.isAlive()) {
                agent.act(this);
            }
        }

        removeDeadAgents();
    }

    public void moveAnimal(Animal animal, Position next) {
        if (animal == null || !isInside(next)) return;
        if (!canAnimalEnter(next, animal)) return;

        Position old = animal.position;
        if (isInside(old) && cells[old.row()][old.col()].getOccupant() == animal) {
            cells[old.row()][old.col()].setOccupant(null);
        }
        animal.position = next;
        cells[next.row()][next.col()].setOccupant(animal);
    }

    public boolean canAnimalEnter(Position p, Animal animal) {
        if (!isInside(p)) return false;
        Agent occupant = cells[p.row()][p.col()].getOccupant();
        // Одновременно в клетке находится только один агент.
        return occupant == null;
    }

    /** Перемещение травоядного: свободная клетка или клетка с растением.
     * Если впереди растение, оно сразу съедается. */
    public boolean tryHerbivoreMove(Herbivore herbivore, Position p) {
        if (herbivore == null || !isInside(p)) return false;
        Agent occupant = cells[p.row()][p.col()].getOccupant();
        if (occupant == null) {
            moveAnimal(herbivore, p);
            return true;
        }
        if (occupant instanceof Plant plant && plant.isAlive()) {
            // Травоядное не должно съедать преграждающее растение, если этим
            // действием оно откроет себе путь до видимого хищника.
            if (!canHerbivoreEatPlantSafely(herbivore, p)) return false;
            removeAgent(plant);
            herbivore.addEnergy(Config.HERB_FOOD_ENERGY);
            moveAnimal(herbivore, p);
            return true;
        }
        return false;
    }

    /**
     * Проверяет, не станет ли видимый сейчас хищник достижимым для травоядного
     * после удаления растения и перехода травоядного на его клетку.
     * Текущая позиция травоядного и клетка съедаемого растения считаются
     * свободными при расчёте гипотетического пути.
     */

    /**
     * Возвращает ближайшее растение, которое травоядное может съесть, не открывая
     * путь ранее заблокированному видимому хищнику. Карта достижимости хищников
     * вычисляется один раз на действие травоядного, а не отдельно для каждого растения.
     */
    public Position findNearestSafePlant(Herbivore herbivore) {
        if (herbivore == null) return null;

        List<Position> plants = new ArrayList<>();
        for (Agent agent : visibleAgents(herbivore.getPosition(), Config.VISION)) {
            if (agent != herbivore && agent instanceof Plant plant && plant.isAlive()) {
                plants.add(plant.getPosition());
            }
        }
        plants.sort(java.util.Comparator.comparingInt(p ->
                Math.abs(p.row() - herbivore.getPosition().row())
                        + Math.abs(p.col() - herbivore.getPosition().col())));

        boolean[][] predatorReachable = buildPredatorReachability(herbivore);
        for (Position plant : plants) {
            if (isPlantSafeUsingReachability(herbivore, plant, predatorReachable)) {
                return plant;
            }
        }
        return null;
    }

    private boolean[][] buildPredatorReachability(Herbivore herbivore) {
        Position center = herbivore.getPosition();
        int radius = Config.VISION / 2;
        int minRow = Math.max(0, center.row() - radius);
        int maxRow = Math.min(getRows() - 1, center.row() + radius);
        int minCol = Math.max(0, center.col() - radius);
        int maxCol = Math.min(getCols() - 1, center.col() + radius);

        boolean[][] reachable = new boolean[maxRow - minRow + 1][maxCol - minCol + 1];
        java.util.ArrayDeque<Position> queue = new java.util.ArrayDeque<>();

        // В стартовые вершины входят только видимые живые хищники. Через клетки
        // хищников не проходим, но сама позиция хищника важна при проверке соседней травы.
        for (Agent agent : visibleAgents(center, Config.VISION)) {
            if (agent instanceof Predator predator && predator.isAlive()) {
                Position p = predator.getPosition();
                reachable[p.row() - minRow][p.col() - minCol] = true;
                queue.addLast(p);
            }
        }

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            for (int[] direction : directions) {
                Position next = current.move(direction[0], direction[1]);
                if (next.row() < minRow || next.row() > maxRow
                        || next.col() < minCol || next.col() > maxCol) continue;
                int rr = next.row() - minRow;
                int cc = next.col() - minCol;
                if (reachable[rr][cc]) continue;

                Agent occupant = getOccupant(next);
                boolean passable = occupant == null || next.equals(center);
                if (!passable) continue;

                reachable[rr][cc] = true;
                queue.addLast(next);
            }
        }
        return reachable;
    }

    private boolean isPlantSafeUsingReachability(Herbivore herbivore, Position plant, boolean[][] reachable) {
        Position center = herbivore.getPosition();
        int radius = Config.VISION / 2;
        int minRow = Math.max(0, center.row() - radius);
        int maxRow = Math.min(getRows() - 1, center.row() + radius);
        int minCol = Math.max(0, center.col() - radius);
        int maxCol = Math.min(getCols() - 1, center.col() + radius);

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] direction : directions) {
            Position n = plant.move(direction[0], direction[1]);
            if (n.row() < minRow || n.row() > maxRow
                    || n.col() < minCol || n.col() > maxCol) continue;
            Agent occupant = getOccupant(n);
            if (occupant instanceof Predator && occupant.isAlive()) return false;
            if (reachable[n.row() - minRow][n.col() - minCol]) return false;
        }
        return true;
    }

    public boolean canHerbivoreEatPlantSafely(Herbivore herbivore, Position plantPosition) {
        if (herbivore == null || !isInside(plantPosition)) return false;
        Agent occupant = getOccupant(plantPosition);
        if (!(occupant instanceof Plant plant) || !plant.isAlive()) return false;
        return isPlantSafeUsingReachability(herbivore, plantPosition, buildPredatorReachability(herbivore));
    }

    private int reachableDistanceInVisionAfterHypotheticalMove(
            Position from, Position target, int visionSize,
            Position free1, Position free2) {
        if (!isInside(from) || !isInside(target)) return -1;

        int radius = visionSize / 2;
        int minRow = Math.max(0, target.row() - radius);
        int maxRow = Math.min(getRows() - 1, target.row() + radius);
        int minCol = Math.max(0, target.col() - radius);
        int maxCol = Math.min(getCols() - 1, target.col() + radius);

        if (from.row() < minRow || from.row() > maxRow ||
                from.col() < minCol || from.col() > maxCol) return -1;

        int[][] distance = new int[maxRow - minRow + 1][maxCol - minCol + 1];
        for (int r = 0; r < distance.length; r++) {
            java.util.Arrays.fill(distance[r], -1);
        }

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        java.util.ArrayDeque<Position> queue = new java.util.ArrayDeque<>();
        queue.add(from);
        distance[from.row() - minRow][from.col() - minCol] = 0;

        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            int currentDistance = distance[current.row() - minRow][current.col() - minCol];
            if (current.equals(target)) return currentDistance;

            for (int[] direction : directions) {
                Position next = current.move(direction[0], direction[1]);
                if (next.row() < minRow || next.row() > maxRow ||
                        next.col() < minCol || next.col() > maxCol) continue;

                int rr = next.row() - minRow;
                int cc = next.col() - minCol;
                if (distance[rr][cc] != -1) continue;

                boolean hypotheticalFree = next.equals(free1) || next.equals(free2) || next.equals(target);
                Agent occupant = cells[next.row()][next.col()].getOccupant();
                if (occupant != null && !hypotheticalFree) continue;

                distance[rr][cc] = currentDistance + 1;
                queue.addLast(next);
            }
        }
        return -1;
    }

    public Agent getOccupant(Position p) {
        if (!isInside(p)) return null;
        return cells[p.row()][p.col()].getOccupant();
    }

    public void eatPlant(Herbivore herbivore, Position p) {
        if (!isInside(p)) return;
        Agent target = cells[p.row()][p.col()].getOccupant();
        if (!(target instanceof Plant plant)) return;
        // Дополнительная защита: никакой вызов поедания растения не должен
        // удалить преграду, открывающую путь видимому хищнику.
        if (!canHerbivoreEatPlantSafely(herbivore, p)) return;
        removeAgent(plant);
        plantsEaten++;
        herbivore.addEnergy(Config.HERB_FOOD_ENERGY);
        moveAnimal(herbivore, p);
    }

    public void eatAnimal(Predator predator, Position p) {
        if (!isInside(p)) return;
        Agent target = cells[p.row()][p.col()].getOccupant();
        if (!(target instanceof Herbivore victim) || !victim.isAlive()) return;
        removeAgent(victim);
        herbivoresEaten++;
        predator.addEnergy(Config.PREDATOR_FOOD_ENERGY);
        moveAnimal(predator, p);
    }

    public void reproducePlant(Plant parent) {
        Position spawn = findFreeNeighbor(parent.position);
        if (spawn == null || parent.energy < Config.PLANT_BIRTH_COST * 2) return;
        parent.energy -= Config.PLANT_BIRTH_COST;
        addAgent(new Plant(nextId++, spawn, Config.PLANT_BIRTH_COST));
        plantBirths++;
    }

    public void reproduce(Animal parent) {
        Position spawn = findFreeNeighbor(parent.position);
        int cost = parent instanceof Herbivore ? Config.HERB_BIRTH_COST : Config.PREDATOR_BIRTH_COST;
        if (spawn == null || parent.energy < cost * 2) return;

        parent.energy -= cost;
        Animal child;
        if (parent instanceof Herbivore) {
            child = new Herbivore(nextId++, spawn, cost);
            herbivoreBirths++;
        } else {
            child = new Predator(nextId++, spawn, cost);
            predatorBirths++;
        }
        addAgent(child);
    }

    private void removeAgent(Agent agent) {
        if (agent == null) return;
        Position p = agent.getPosition();
        if (isInside(p) && cells[p.row()][p.col()].getOccupant() == agent) {
            cells[p.row()][p.col()].setOccupant(null);
        }
        if (agents.remove(agent)) {
            decrementCount(agent);
            if (agent instanceof Plant) plantDeaths++;
            else if (agent instanceof Herbivore) herbivoreDeaths++;
            else if (agent instanceof Predator) predatorDeaths++;
        }
        agent.energy = 0;
    }

    public void removeDeadAgents() {
        var iterator = agents.iterator();
        while (iterator.hasNext()) {
            Agent agent = iterator.next();
            if (agent.isAlive()) continue;
            Position p = agent.getPosition();
            if (isInside(p) && cells[p.row()][p.col()].getOccupant() == agent) {
                cells[p.row()][p.col()].setOccupant(null);
            }
            iterator.remove();
            decrementCount(agent);
            if (agent instanceof Plant) plantDeaths++;
            else if (agent instanceof Herbivore) herbivoreDeaths++;
            else if (agent instanceof Predator) predatorDeaths++;
        }
    }

    public List<Agent> visibleAgents(Position center, int size) {
        int radius = size / 2;
        List<Agent> result = new ArrayList<>();
        for (int r = center.row() - radius; r <= center.row() + radius; r++) {
            for (int c = center.col() - radius; c <= center.col() + radius; c++) {
                if (!isInside(new Position(r, c))) continue;
                Agent agent = cells[r][c].getOccupant();
                if (agent != null && agent.isAlive()) result.add(agent);
            }
        }
        return result;
    }

    /**
     * Возвращает длину кратчайшего пути по четырём направлениям внутри 5×5 зоны
     * от указанной клетки до цели. Занятые клетки являются преградами, кроме цели.
     * Если путь недоступен, возвращается -1.
     */
    public int reachableDistanceInVision(Position from, Position target, int visionSize) {
        if (!isInside(from) || !isInside(target)) return -1;

        int radius = visionSize / 2;
        int minRow = Math.max(0, target.row() - radius);
        int maxRow = Math.min(getRows() - 1, target.row() + radius);
        int minCol = Math.max(0, target.col() - radius);
        int maxCol = Math.min(getCols() - 1, target.col() + radius);

        if (from.row() < minRow || from.row() > maxRow ||
                from.col() < minCol || from.col() > maxCol) return -1;

        int[][] distance = new int[maxRow - minRow + 1][maxCol - minCol + 1];
        for (int r = 0; r < distance.length; r++) {
            java.util.Arrays.fill(distance[r], -1);
        }

        int[][] directions = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };
        java.util.ArrayDeque<Position> queue = new java.util.ArrayDeque<>();
        queue.add(from);
        distance[from.row() - minRow][from.col() - minCol] = 0;

        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            int currentDistance = distance[current.row() - minRow][current.col() - minCol];
            if (current.equals(target)) return currentDistance;

            for (int[] direction : directions) {
                Position next = current.move(direction[0], direction[1]);
                if (next.row() < minRow || next.row() > maxRow ||
                        next.col() < minCol || next.col() > maxCol) continue;

                int rr = next.row() - minRow;
                int cc = next.col() - minCol;
                if (distance[rr][cc] != -1) continue;

                Agent occupant = cells[next.row()][next.col()].getOccupant();
                if (occupant != null && !next.equals(target)) continue;

                distance[rr][cc] = currentDistance + 1;
                queue.addLast(next);
            }
        }
        return -1;
    }

    public List<Position> animalNeighbors(Position p, Animal animal) {
        List<Position> result = new ArrayList<>();
        int[][] directions = {
                {-1, 0},  // вверх
                {1, 0},   // вниз
                {0, -1},  // влево
                {0, 1}    // вправо
        };
        for (int[] direction : directions) {
            Position n = p.move(direction[0], direction[1]);
            if (canAnimalEnter(n, animal)) result.add(n);
        }
        return result;
    }

    private Position findFreeNeighbor(Position p) {
        List<Position> free = animalNeighbors(p, null);
        return free.isEmpty() ? null : free.get(random.nextInt(free.size()));
    }

    private Position randomFreePosition() {
        for (int attempt = 0; attempt < 500; attempt++) {
            Position p = new Position(random.nextInt(cells.length), random.nextInt(cells[0].length));
            if (cells[p.row()][p.col()].isEmpty()) return p;
        }
        return null;
    }

    public boolean isInside(Position p) {
        return p != null && p.row() >= 0 && p.row() < cells.length
                && p.col() >= 0 && p.col() < cells[0].length;
    }

    public Cell[][] getCells() { return cells; }
    public int getRows() { return cells.length; }
    public int getCols() { return cells[0].length; }
    public int getStep() { return step; }
    public Random random() { return random; }

    public long countPlants() { return plantCount; }
    public long countHerbivores() { return herbivoreCount; }
    public long countPredators() { return predatorCount; }

    public long getPlantBirths() { return plantBirths; }
    public long getHerbivoreBirths() { return herbivoreBirths; }
    public long getPredatorBirths() { return predatorBirths; }
    public long getPlantsEaten() { return plantsEaten; }
    public long getHerbivoresEaten() { return herbivoresEaten; }
    public long getPlantDeaths() { return plantDeaths; }
    public long getHerbivoreDeaths() { return herbivoreDeaths; }
    public long getPredatorDeaths() { return predatorDeaths; }
}
