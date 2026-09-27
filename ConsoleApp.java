package alife;

public class ConsoleApp {
    private static final int MAX_STEPS = 50000;

    public static void run() {
        long configuredSeed = Long.getLong("seed", Long.MIN_VALUE);
        Simulation sim = configuredSeed == Long.MIN_VALUE ? new Simulation() : new Simulation(configuredSeed);

        System.out.println("============================================================");
        System.out.println(" Лабораторная работа 1 — Искусственная жизнь");
        System.out.println(" Консольная версия");
        System.out.println(" Движение: только вверх / вниз / влево / вправо");
        System.out.println(" Зрение животных: область 5x5, агент в центре");
        System.out.println(" Seed текущего запуска: " + sim.seed());
        System.out.println("============================================================");

        // Первый реальный такт модели.
        sim.step();
        printSnapshot(sim.environment(), "ПЕРВЫЙ ШАГ");

        if (Boolean.parseBoolean(System.getProperty("pauseAfterFirst", "true"))) {
            System.out.println();
            System.out.println("Нажмите Enter, чтобы продолжить расчет до 10000-го шага...");
            try {
                new java.io.BufferedReader(new java.io.InputStreamReader(System.in, java.nio.charset.StandardCharsets.UTF_8)).readLine();
            } catch (java.io.IOException ignored) {
                // Если ввод недоступен, расчет продолжается автоматически.
            }
            clearConsole();
            System.out.println("Расчет продолжается до достижения горизонта или вымирания вида...");
        }

        int finalStep = sim.environment().getStep();
        String reason = "Достигнут горизонт проверки в " + MAX_STEPS + " шагов";

        while (sim.environment().getStep() < MAX_STEPS
                && sim.environment().countHerbivores() > 0
                && sim.environment().countPredators() > 0) {
            sim.step();
            finalStep = sim.environment().getStep();
        }

        Environment env = sim.environment();
        if (env.countHerbivores() == 0) {
            reason = "Исчезли все травоядные";
        } else if (env.countPredators() == 0) {
            reason = "Исчезли все хищники";
        }

        System.out.println();
        System.out.println("============================================================");
        System.out.println(" ОСТАНОВКА СИМУЛЯЦИИ");
        System.out.println(" Причина: " + reason);
        System.out.println("============================================================");
        printSnapshot(env, "ПОСЛЕДНИЙ ШАГ");

        System.out.println();
        System.out.println("-------------------- СТАТИСТИКА ----------------------------");
        System.out.printf("Всего выполнено шагов:    %d%n", finalStep);
        System.out.printf("Растений:                 %d%n", env.countPlants());
        System.out.printf("Травоядных:               %d%n", env.countHerbivores());
        System.out.printf("Хищников:                 %d%n", env.countPredators());
        System.out.printf("Причина остановки:        %s%n", reason);
        System.out.println("------------------------------------------------------------");
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception ignored) {
            for (int i = 0; i < 45; i++) System.out.println();
        }
    }

    private static void printSnapshot(Environment env, String title) {
        System.out.println();
        System.out.println("********** " + title + " **********");
        System.out.printf("Шаг: %d | Растения: %d | Травоядные: %d | Хищники: %d%n",
                env.getStep(), env.countPlants(), env.countHerbivores(), env.countPredators());
        for (int r = 0; r < env.getRows(); r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < env.getCols(); c++) {
                Agent a = env.getCells()[r][c].getOccupant();
                if (a instanceof Herbivore) row.append('H');
                else if (a instanceof Predator) row.append('P');
                else if (a instanceof Plant) row.append('*');
                else row.append('.');
            }
            System.out.println(row);
        }
    }
}
