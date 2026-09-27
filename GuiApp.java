package alife;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

public class GuiApp {
    public static void run() {
        long configuredSeed = Long.getLong("seed", Long.MIN_VALUE);
        Simulation simulation = configuredSeed == Long.MIN_VALUE
                ? new Simulation()
                : new Simulation(configuredSeed);
        showWithSimulation(simulation);
    }

    private static void showWithSimulation(Simulation simulation) {
        SimulationPanel panel = new SimulationPanel(simulation);
        JFrame frame = new JFrame("Искусственная жизнь — Лабораторная 1");
        frame.setLayout(new BorderLayout(8, 8));

        JLabel info = new JLabel();
        JLabel cellInfo = new JLabel("Наведите мышь на клетку");
        JLabel legend = new JLabel("Растение: •");
        JLabel herbLegend = new JLabel("Травоядное: синее");
        JLabel predLegend = new JLabel("Хищник: красный");
        JLabel energyLegend = new JLabel("H/P: энергия внутри");
        JLabel exactLegend = new JLabel("Точно: при наведении");
        Font small = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        for (JLabel label : new JLabel[]{cellInfo, legend, herbLegend, predLegend, energyLegend, exactLegend}) {
            label.setFont(small);
        }

        JButton start = new JButton("Старт");
        JButton pause = new JButton("Пауза");
        JButton step = new JButton("Шаг");
        JButton reset = new JButton("Новая карта");
        JPanel controls = new JPanel();
        controls.add(start);
        controls.add(pause);
        controls.add(step);
        controls.add(reset);
        controls.add(info);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createTitledBorder("Информация"));
        side.setPreferredSize(new Dimension(210, 760));
        side.add(legend);
        side.add(Box.createVerticalStrut(5));
        side.add(herbLegend);
        side.add(predLegend);
        side.add(Box.createVerticalStrut(12));
        side.add(energyLegend);
        side.add(exactLegend);
        side.add(Box.createVerticalStrut(12));
        side.add(cellInfo);
        side.add(Box.createVerticalGlue());

        panel.setHoverConsumer(cellInfo::setText);

        Timer timer = new Timer(120, e -> {
            simulation.step();
            panel.repaint();
            updateInfo(info, simulation);
        });

        start.addActionListener(e -> timer.start());
        pause.addActionListener(e -> timer.stop());
        step.addActionListener(e -> {
            simulation.step();
            panel.repaint();
            updateInfo(info, simulation);
        });
        reset.addActionListener(e -> {
            timer.stop();
            frame.dispose();
            showWithSimulation(new Simulation());
        });

        updateInfo(info, simulation);
        frame.add(panel, BorderLayout.CENTER);
        frame.add(side, BorderLayout.EAST);
        frame.add(controls, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void updateInfo(JLabel label, Simulation sim) {
        Environment e = sim.environment();
        label.setText(String.format("Шаг: %d | Растения: %d | Травоядные: %d | Хищники: %d",
                e.getStep(), e.countPlants(), e.countHerbivores(), e.countPredators()));
    }
}
