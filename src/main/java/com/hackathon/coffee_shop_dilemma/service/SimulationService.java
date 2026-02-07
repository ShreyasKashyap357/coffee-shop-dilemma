package com.hackathon.coffee_shop_dilemma.service;

import com.hackathon.coffee_shop_dilemma.model.*;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

@Service
public class SimulationService {

    private static final int SIMULATION_MINUTES = 180;
    private static final double ARRIVAL_LAMBDA = 1.4;
    private static final int NUM_BARISTAS = 3;
    private static final int MAX_WAIT_MINUTES = 10;
    private static final int EMERGENCY_THRESHOLD = 8;
    private static final int MAX_SKIPS_TOLERATED = 3;

    private final Queue<Order> priorityQueue =
            new PriorityBlockingQueue<>(500,
                    Comparator.comparingDouble(Order::getPriorityScore).reversed());

    private final List<Order> completedOrders = new ArrayList<>();
    private final List<Order> abandonedOrders = new ArrayList<>();
    private final List<Barista> baristas = new ArrayList<>();

    private final Random random = new Random();
    private final PoissonDistribution poisson =
            new PoissonDistribution(ARRIVAL_LAMBDA);

    private LocalDateTime liveTime = LocalDateTime.of(2026, 2, 7, 7, 0);

    public SimulationService() {
        for (long i = 1; i <= NUM_BARISTAS; i++) {
            Barista b = new Barista();
            b.setId(i);
            baristas.add(b);
        }
    }

    // =======================
    // MAIN SIMULATION
    // =======================
    public SimulationResult runSimulation() {

        completedOrders.clear();
        abandonedOrders.clear();
        priorityQueue.clear();

        LocalDateTime now = LocalDateTime.of(2026, 2, 7, 7, 0);

        for (Barista b : baristas) {
            b.setCurrentWorkload(0);
            b.setAvailableTime(now);
        }

        long orderId = 1;

        for (int minute = 0; minute < SIMULATION_MINUTES; minute++) {

            int arrivals = poisson.sample();
            for (int i = 0; i < arrivals; i++) {
                priorityQueue.add(createRandomOrder(orderId++, now));
            }

            recalculatePriorities(now);
            handleAbandonment(now);
            assignOrders(now);

            now = now.plusMinutes(1);
        }

        priorityQueue.clear();
        return calculateMetrics();
    }

    // =======================
    // ORDER CREATION
    // =======================
    private Order createRandomOrder(long id, LocalDateTime arrival) {
        Order o = new Order();
        o.setId(id);
        o.setArrivalTime(arrival);
        o.setLoyaltyGold(random.nextDouble() < 0.3);

        int n = random.nextInt(3) + 1;
        List<DrinkType> drinks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            drinks.add(pickDrinkByFrequency());
        }
        o.setDrinks(drinks);

        updatePriorityScore(o, arrival);
        return o;
    }

    private DrinkType pickDrinkByFrequency() {
        double r = random.nextDouble();
        double sum = 0;
        for (DrinkType d : DrinkType.values()) {
            sum += d.getFrequency();
            if (r <= sum) return d;
        }
        return DrinkType.COLD_BREW;
    }

    // =======================
    // PRIORITY
    // =======================
    private void recalculatePriorities(LocalDateTime now) {
        List<Order> temp = new ArrayList<>(priorityQueue);
        priorityQueue.clear();
        for (Order o : temp) {
            updatePriorityScore(o, now);
            priorityQueue.add(o);
        }
    }

    private void updatePriorityScore(Order o, LocalDateTime now) {
        long wait = ChronoUnit.MINUTES.between(o.getArrivalTime(), now);

        int score = 0;
        score += Math.min(wait * 5, 50);
        if (o.isLoyaltyGold()) score += 20;
        if (wait >= EMERGENCY_THRESHOLD) score += 30;

        o.setPriorityScore(Math.min(score, 100));
    }

    // =======================
    // ASSIGNMENT
    // =======================
    private void assignOrders(LocalDateTime now) {
        for (Barista b : baristas) {
            if (!b.isAvailable(now)) continue;

            Order emergency = findHardTimeoutOrder(now);
            if (emergency != null) {
                priorityQueue.remove(emergency);
                assign(emergency, b, now);
                continue;
            }

            Order normal = selectBestOrder(now);
            if (normal == null) continue;

            priorityQueue.remove(normal);
            assign(normal, b, now);
        }
    }

    private void assign(Order o, Barista b, LocalDateTime now) {
        int prep = o.totalPrepTime();

        o.setAssignedTime(now);
        o.setCompletionTime(now.plusMinutes(prep));
        o.setWaitTimeMinutes(
                ChronoUnit.MINUTES.between(o.getArrivalTime(), now)
        );
        o.setBaristaId(b.getId());
        o.setStatus("COMPLETED");
        o.setCompleted(true);

        completedOrders.add(o);

        b.setCurrentWorkload(b.getCurrentWorkload() + prep);
        b.setAvailableTime(now.plusMinutes(prep));
    }

    private Order selectBestOrder(LocalDateTime now) {
        List<Order> candidates = new ArrayList<>(priorityQueue);

        candidates.sort(Comparator
                .comparingDouble(Order::getPriorityScore).reversed()
                .thenComparing(Order::getArrivalTime));

        for (Order o : candidates) {
            long wait = ChronoUnit.MINUTES.between(o.getArrivalTime(), now);
            if (wait >= MAX_WAIT_MINUTES) return o;

            int allowedSkips =
                    o.totalPrepTime() <= 2 ? 5 : MAX_SKIPS_TOLERATED;

            int pos = candidates.indexOf(o);
            if (pos > allowedSkips) {
                o.setSkippedCount(o.getSkippedCount() + 1);
                if (o.getSkippedCount() > allowedSkips) return o;
                continue;
            }
            return o;
        }
        return null;
    }

    private Order findHardTimeoutOrder(LocalDateTime now) {
        for (Order o : priorityQueue) {
            long wait = ChronoUnit.MINUTES.between(o.getArrivalTime(), now);
            if (wait >= MAX_WAIT_MINUTES) {
                o.setReason("Emergency: waited 10 minutes");
                return o;
            }
        }
        return null;
    }

    private void handleAbandonment(LocalDateTime now) {
        Iterator<Order> it = priorityQueue.iterator();
        while (it.hasNext()) {
            Order o = it.next();
            long wait = ChronoUnit.MINUTES.between(o.getArrivalTime(), now);

            if (!o.isLoyaltyGold() && wait >= 8) {
                o.setStatus("ABANDONED");
                o.setWaitTimeMinutes(wait);
                abandonedOrders.add(o);
                it.remove();
            }
        }
    }

    // =======================
    // METRICS
    // =======================
    private SimulationResult calculateMetrics() {

        SimulationResult res = new SimulationResult();

        int n = completedOrders.size();
        long totalWait = 0;
        int timeouts = 0;
        int fairness = 0;

        for (Order o : completedOrders) {
            long w = o.getWaitTimeMinutes();
            totalWait += w;
            if (w > MAX_WAIT_MINUTES) timeouts++;
            if (o.getSkippedCount() > MAX_SKIPS_TOLERATED) fairness++;
        }

        res.setAverageWaitTimeMinutes(n == 0 ? 0 : (double) totalWait / n);
        res.setTimeoutRatePercent(n == 0 ? 0 : (timeouts * 100.0) / n);
        res.setFairnessViolationRatePercent(n == 0 ? 0 : (fairness * 100.0) / n);
        res.setNumComplaints(timeouts + fairness);

        double avgLoad = baristas.stream()
                .mapToDouble(Barista::getCurrentWorkload)
                .average().orElse(0);

        double variance = baristas.stream()
                .mapToDouble(b -> Math.pow(b.getCurrentWorkload() - avgLoad, 2))
                .average().orElse(0);

        res.setWorkloadBalanceStdDev(Math.sqrt(variance));
        res.setCompletedOrders(new ArrayList<>(completedOrders));

        Map<Long, Integer> workloads = new HashMap<>();
        for (Barista b : baristas) {
            workloads.put(b.getId(), b.getCurrentWorkload());
        }
        res.setBaristaWorkloads(workloads);

        return res;
    }

    // =======================
    // UI HELPERS
    // =======================
    public List<Order> getWaitingOrders() {
        return new ArrayList<>(priorityQueue);
    }

    public List<Barista> getBaristas() {
        return new ArrayList<>(baristas);
    }

    public void placeOrder(List<DrinkType> drinks, boolean loyalty) {
        Order o = new Order();
        o.setId((long) (completedOrders.size() + priorityQueue.size() + 1));
        o.setArrivalTime(liveTime);
        o.setDrinks(drinks);
        o.setLoyaltyGold(loyalty);
        o.setStatus("PENDING");

        updatePriorityScore(o, liveTime);
        priorityQueue.add(o);
    }

    public void advanceLiveSystem() {
        liveTime = liveTime.plusMinutes(1);
        recalculatePriorities(liveTime);
        assignOrders(liveTime);
        priorityQueue.removeIf(o -> "COMPLETED".equals(o.getStatus()));
    }

    public Map<DrinkType, Long> getDrinkBreakdown(List<SimulationResult> results) {
        Map<DrinkType, Long> breakdown = new EnumMap<>(DrinkType.class);

        for (SimulationResult result : results) {
            for (Order order : result.getCompletedOrders()) {
                for (DrinkType drink : order.getDrinks()) {
                    breakdown.put(
                        drink,
                        breakdown.getOrDefault(drink, 0L) + 1
                    );
                }
            }
        }
        return breakdown;
    }

    public Map<DrinkType, DrinkStats> getDrinkStatsForSimulation(SimulationResult result) {

        Map<DrinkType, List<Long>> waits = new EnumMap<>(DrinkType.class);

        for (Order order : result.getCompletedOrders()) {
            for (DrinkType drink : order.getDrinks()) {
                waits
                    .computeIfAbsent(drink, d -> new ArrayList<>())
                    .add(order.getWaitTimeMinutes()); // ✅ correct getter
            }
        }
        Map<DrinkType, DrinkStats> stats = new EnumMap<>(DrinkType.class);
        for (Map.Entry<DrinkType, List<Long>> entry : waits.entrySet()) {
            double avg = entry.getValue()
                            .stream()
                            .mapToLong(Long::longValue)
                            .average()
                            .orElse(0);

            stats.put(
                entry.getKey(),
                new DrinkStats(entry.getValue().size(), avg)
            );
        }
        return stats;
    }
}
