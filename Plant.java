package alife;

public class Plant extends Agent {
    public Plant(int id, Position position, int energy) {
        super(id, position, energy);
    }

    @Override
    public void act(Environment env) {
        energy += Config.PLANT_GROWTH_PER_TICK;
        if (energy >= Config.PLANT_BIRTH_THRESHOLD) {
            env.reproducePlant(this);
        }
    }

    @Override public char symbol() { return '*'; }
    @Override public String typeName() { return "Растение"; }
}
