package alife;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.Consumer;

public class SimulationPanel extends JPanel {
    private final Simulation simulation;
    private static final int CELL_SIZE = 15;
    private Consumer<String> hoverConsumer = s -> {};

    public SimulationPanel(Simulation simulation) {
        this.simulation = simulation;
        setPreferredSize(new Dimension(Config.COLS * CELL_SIZE, Config.ROWS * CELL_SIZE));
        setBackground(Color.WHITE);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int col = e.getX() / CELL_SIZE;
                int row = e.getY() / CELL_SIZE;
                if (row < 0 || row >= Config.ROWS || col < 0 || col >= Config.COLS) {
                    hoverConsumer.accept("");
                    return;
                }
                Agent agent = simulation.environment().getCells()[row][col].getOccupant();
                if (agent == null) {
                    hoverConsumer.accept(String.format("Клетка (%d, %d): свободна", row + 1, col + 1));
                } else {
                    hoverConsumer.accept(String.format("Клетка (%d, %d): %s | энергия: %d",
                            row + 1, col + 1, agent.typeName(), agent.getEnergy()));
                }
            }
        });
    }

    public void setHoverConsumer(Consumer<String> consumer) {
        hoverConsumer = consumer == null ? s -> {} : consumer;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Cell[][] cells = simulation.environment().getCells();

        for (int r = 0; r < Config.ROWS; r++) {
            for (int c = 0; c < Config.COLS; c++) {
                int x = c * CELL_SIZE;
                int y = r * CELL_SIZE;

                g2.setColor(new Color(232, 232, 232));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);

                Agent occupant = cells[r][c].getOccupant();
                if (occupant instanceof Plant) {
                    g2.setColor(new Color(55, 155, 75));
                    g2.fillOval(x + 4, y + 4, 7, 7);
                } else if (occupant instanceof Herbivore herbivore) {
                    drawAnimal(g2, herbivore, x, y, new Color(45, 110, 205));
                } else if (occupant instanceof Predator predator) {
                    drawAnimal(g2, predator, x, y, new Color(215, 65, 55));
                }
            }
        }
        g2.dispose();
    }

    private void drawAnimal(Graphics2D g2, Animal animal, int x, int y, Color bodyColor) {
        g2.setColor(bodyColor);
        g2.fillOval(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 7));
        String text = formatEnergy(animal.getEnergy());
        int width = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, x + (CELL_SIZE - width) / 2, y + 10);

        int maxEnergy = animal instanceof Predator
                ? Config.PREDATOR_BIRTH_THRESHOLD
                : Config.HERB_BIRTH_THRESHOLD;
        double ratio = Math.min(1.0, Math.max(0.0, animal.getEnergy() / (double) maxEnergy));
        int barWidth = (int) Math.round((CELL_SIZE - 2) * ratio);
        g2.setColor(Color.WHITE);
        g2.fillRect(x + 1, y + CELL_SIZE - 3, CELL_SIZE - 2, 2);
        g2.setColor(Color.BLACK);
        if (barWidth > 0) g2.fillRect(x + 1, y + CELL_SIZE - 3, barWidth, 2);
    }

    private String formatEnergy(int energy) {
        if (energy < 1000) return String.valueOf(energy);
        if (energy < 10000) return String.format("%.1fk", energy / 1000.0);
        return (energy / 1000) + "k";
    }
}
