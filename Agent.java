package alife;

public abstract class Agent {
    protected final int id;
    protected Position position;
    protected int energy;

    protected Agent(int id, Position position, int energy) {
        this.id = id;
        this.position = position;
        this.energy = energy;
    }

    public int getId() { return id; }
    public Position getPosition() { return position; }
    public int getEnergy() { return energy; }
    public boolean isAlive() { return energy > 0; }

    public abstract void act(Environment environment);
    public abstract char symbol();
    public abstract String typeName();

    protected void spendEnergy(int amount) { energy -= amount; }
    protected void addEnergy(int amount) { energy += amount; }

    protected void moveTo(Position next, Environment environment) {
        environment.moveAnimal((Animal) this, next);
    }
}
