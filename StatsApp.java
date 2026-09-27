package alife;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/** Пакетная проверка устойчивости модели на независимых случайных seed. */
public class StatsApp {
    private record TrialResult(
            long seed, int steps, boolean survived,
            long minPlants, long minHerbivores, long minPredators,
            long plantBirths, long herbivoreBirths, long predatorBirths,
            long plantsEaten, long herbivoresEaten,
            long plantDeaths, long herbivoreDeaths, long predatorDeaths) {}

    public static void main(String[] args) throws Exception {
        int trials = Integer.getInteger("trials", 10);
        int horizon = Integer.getInteger("steps", 1000000);
        int threads = Math.max(1, Math.min(
                Integer.getInteger("threads", Runtime.getRuntime().availableProcessors()), trials));

        System.out.println("Проверка устойчивости: " + trials
                + " независимых запусков, горизонт " + horizon + " шагов.");
        System.out.println("Для каждого испытания используется новый случайный seed.");
        System.out.println("Потоки проверки: " + threads);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<TrialResult>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < trials; i++) {
                long seed = ThreadLocalRandom.current().nextLong();
                futures.add(executor.submit((Callable<TrialResult>) () -> runTrial(seed, horizon)));
            }

            int passed = 0;
            long minP = Long.MAX_VALUE, minH = Long.MAX_VALUE, minX = Long.MAX_VALUE;
            long totalPlantBirths = 0, totalHerbBirths = 0, totalPredBirths = 0;
            long totalPlantsEaten = 0, totalHerbEaten = 0;
            long totalPlantDeaths = 0, totalHerbDeaths = 0, totalPredDeaths = 0;
            List<Long> failedSeeds = new ArrayList<>();
            int shortest = Integer.MAX_VALUE;
            long shortestSeed = 0;
            String shortestCause = "нет";
            int longest = -1;
            long longestSeed = 0;
            String longestCause = "нет";

            for (Future<TrialResult> future : futures) {
                TrialResult r = future.get();
                minP = Math.min(minP, r.minPlants());
                minH = Math.min(minH, r.minHerbivores());
                minX = Math.min(minX, r.minPredators());
                totalPlantBirths += r.plantBirths();
                totalHerbBirths += r.herbivoreBirths();
                totalPredBirths += r.predatorBirths();
                totalPlantsEaten += r.plantsEaten();
                totalHerbEaten += r.herbivoresEaten();
                totalPlantDeaths += r.plantDeaths();
                totalHerbDeaths += r.herbivoreDeaths();
                totalPredDeaths += r.predatorDeaths();

                if (r.survived()) {
                    passed++;
                } else {
                    failedSeeds.add(r.seed());
                    String cause = r.minHerbivores() == 0 ? "травоядные"
                            : r.minPredators() == 0 ? "хищники" : "растения";
                    if (r.steps() < shortest) {
                        shortest = r.steps();
                        shortestSeed = r.seed();
                        shortestCause = cause;
                    }
                    if (r.steps() > longest) {
                        longest = r.steps();
                        longestSeed = r.seed();
                        longestCause = cause;
                    }
                }
            }

            System.out.printf("Успешно: %d/%d (%.1f%%)%n", passed, trials, passed * 100.0 / trials);
            System.out.printf("Минимум за весь горизонт: растения=%d, травоядные=%d, хищники=%d%n",
                    minP, minH, minX);
            System.out.printf("События за серию: рождений растений=%d, травоядных=%d, хищников=%d%n",
                    totalPlantBirths, totalHerbBirths, totalPredBirths);
            System.out.printf("Поеданий: растений=%d, травоядных=%d%n", totalPlantsEaten, totalHerbEaten);
            System.out.printf("Удалений/смертей: растений=%d, травоядных=%d, хищников=%d%n",
                    totalPlantDeaths, totalHerbDeaths, totalPredDeaths);
            System.out.println("Критерий успеха: все три популяции сохраняются на протяжении всего горизонта.");

            if (failedSeeds.isEmpty()) {
                System.out.println("Неудачных запусков нет.");
            } else {
                System.out.println("Неудачных запусков: " + failedSeeds.size());
                System.out.println("Seed неудачных запусков: " + failedSeeds);
                System.out.printf("Самая ранняя остановка: шаг %d, seed=%d, исчезли %s%n",
                        shortest, shortestSeed, shortestCause);
                System.out.printf("Самая ранняя остановка: шаг %d, seed=%d, исчезли %s%n",
                        longest, longestSeed, longestCause);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static TrialResult runTrial(long seed, int horizon) {
        Simulation simulation = new Simulation(seed);
        Environment env = simulation.environment();
        long minP = env.countPlants(), minH = env.countHerbivores(), minX = env.countPredators();

        for (int i = 0; i < horizon; i++) {
            simulation.step();
            minP = Math.min(minP, env.countPlants());
            minH = Math.min(minH, env.countHerbivores());
            minX = Math.min(minX, env.countPredators());
            if (env.countHerbivores() == 0 || env.countPredators() == 0) break;
        }

        boolean survived = env.getStep() >= horizon
                && minP > 0
                && minH > 0
                && minX > 0;

        return new TrialResult(seed, env.getStep(), survived,
                minP, minH, minX,
                env.getPlantBirths(), env.getHerbivoreBirths(), env.getPredatorBirths(),
                env.getPlantsEaten(), env.getHerbivoresEaten(),
                env.getPlantDeaths(), env.getHerbivoreDeaths(), env.getPredatorDeaths());
    }
}
