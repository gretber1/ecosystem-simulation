package alife;

public class Cell {
    private Agent occupant;

    public boolean isEmpty() { return occupant == null; }
    public Agent getOccupant() { return occupant; }
    public void setOccupant(Agent occupant) { this.occupant = occupant; }

    public Plant getPlant() {
        return occupant instanceof Plant ? (Plant) occupant : null;
    }

    public Animal getAnimal() {
        return occupant instanceof Animal ? (Animal) occupant : null;
    }
}
