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
    private static final int SIMULATION_MINUTES = 180; // 7:00 to 10:00 AM
    private static final double ARRIVAL_LAMBDA = 1.4; // customers per minute
    private static final int NUM_BARISTAS = 3;
    private static final int MAX_WAIT_MINUTES = 10;
    private static final int EMERGENCY_THRESHOLD = 8;
    private static final int MAX_SKIPS_TOLERATED = 3;
    private final List<Barista> baristas = new ArrayList<>();
    private final Queue<Order> priorityQueue;
    private final List<Order> completedOrders = new ArrayList<>();
    private final Random random = new Random();
    private final PoissonDistribution poisson = new PoissonDistribution(ARRIVAL_LAMBDA);
    private Thread processingThread;
    private boolean running = true; 
    public SimulationService() {
        this.priorityQueue = new PriorityBlockingQueue<>(500, Comparator.comparingDouble(Order::getPriorityScore).reversed());
        initializeBaristas();
        startProcessingThread();
    }

    private void initializeBaristas() {
        for (long i = 1; i <= NUM_BARISTAS; i++) {
            Barista b = new Barista();
            b.setId(i);
            baristas.add(b);
        }
    }

    public SimulationResult runSimulation() {
        completedOrders.clear();
        priorityQueue.clear();
        baristas.forEach(b -> {
            b.setCurrentWorkload(0.0);
            b.setAvailableTime(LocalDateTime.now().withHour(7).withMinute(0).withSecond(0).withNano(0));
        });

        LocalDateTime currentTime = LocalDateTime.now().withHour(7).withMinute(0).withSecond(0).withNano(0);
        long orderId = 1;

        for (int minute = 0; minute < SIMULATION_MINUTES; minute++) {
            // Generate arrivals this minute
            int arrivals = poisson.sample();
            for (int i = 0; i < arrivals; i++) {
                Order order = createRandomOrder(orderId++, currentTime);
                priorityQueue.add(order);
            }

            // Recalculate priorities (every minute for simplicity; could be every 30s)
            recalculatePriorities(currentTime);

            // Assign to available baristas
            assignOrders(currentTime);

            currentTime = currentTime.plusMinutes(1);
        }

        return calculateMetrics();
    }

    private Order createRandomOrder(long id, LocalDateTime arrival) {
        Order order = new Order();
        order.setId(id);
        order.setArrivalTime(arrival);
        order.setLoyaltyGold(random.nextDouble() < 0.3); // ~30% gold members
        // 1-3 drinks
        int numDrinks = random.nextInt(3) + 1;
        List<DrinkType> drinks = new ArrayList<>();
        for (int j = 0; j < numDrinks; j++) {
            drinks.add(pickDrinkByFrequency());
        }
        order.setDrinks(drinks);
        updatePriorityScore(order, arrival);
        return order;
    }

    private DrinkType pickDrinkByFrequency() {
        double r = random.nextDouble();
        double sum = 0.0;
        for (DrinkType dt : DrinkType.values()) {
            sum += dt.getFrequency();
            if (r <= sum) return dt;
        }
        return DrinkType.COLD_BREW; // fallback
    }

    private void recalculatePriorities(LocalDateTime now) {
        List<Order> temp = new ArrayList<>(priorityQueue);
        priorityQueue.clear();
        temp.forEach(o -> {
            updatePriorityScore(o, now);
            priorityQueue.add(o);
        });
    }

    private void updatePriorityScore(Order order, LocalDateTime now) {
        long waitMin = ChronoUnit.MINUTES.between(order.getArrivalTime(), now);
        int totalPrep = order.getDrinks().stream().mapToInt(DrinkType::getPrepTimeMinutes).sum();
        double score = 0.0;
        score += 0.40 * waitMin;                          // wait time
        score += 0.25 * (20.0 / (totalPrep + 1));         // shorter = better (bonus)
        score += order.isLoyaltyGold() ? 10.0 : 0.0;    // loyalty boost
        score += 0.25 * waitMin;                          // urgency base
        if (waitMin > EMERGENCY_THRESHOLD) {
            score += 50.0;
        }
        order.setPriorityScore(score);
    }

    private void assignOrders(LocalDateTime now) {
        // Sort baristas by earliest available
        baristas.sort(Comparator.comparing(Barista::getAvailableTime));
        for (Barista barista : baristas) {
            if (!priorityQueue.isEmpty() && !barista.getAvailableTime().isAfter(now)) {
                Order selected = selectBestOrderForBarista(barista, now);
                if (selected != null) {
                    assign(selected, barista, now);
                }
            }
        }
    }

    private Order selectBestOrderForBarista(Barista barista, LocalDateTime now) {
        double avgWorkload = baristas.stream().mapToDouble(Barista::getCurrentWorkload).average().orElse(0.0);
        boolean preferQuick = barista.getCurrentWorkload() > 1.2 * avgWorkload;
        Iterator<Order> it = priorityQueue.iterator();
        while (it.hasNext()) {
            Order o = it.next();
            long wait = ChronoUnit.MINUTES.between(o.getArrivalTime(), now);
            if (wait > MAX_WAIT_MINUTES) {
                it.remove(); // hard constraint - abandon (for sim)
                continue;
            }
            int prep = o.getDrinks().stream().mapToInt(DrinkType::getPrepTimeMinutes).sum();
            if (preferQuick && prep > 4) continue; // skip complex if overloaded
            // Fairness: count approximate skips (position in queue)
            int position = new ArrayList<>(priorityQueue).indexOf(o);
            if (position > MAX_SKIPS_TOLERATED) {
                o.setSkippedCount(o.getSkippedCount() + 1);
                if (o.getSkippedCount() > MAX_SKIPS_TOLERATED) {
                    return o; // force serve
                }
            }
            return o;
        }
        return null;
    }

    private void assign(Order order, Barista barista, LocalDateTime now) {
        priorityQueue.remove(order);
        int prepTotal = order.getDrinks().stream().mapToInt(DrinkType::getPrepTimeMinutes).sum();
        order.setAssignedTime(now);
        order.setCompletionTime(now.plusMinutes(prepTotal));
        completedOrders.add(order);
        barista.setCurrentWorkload(barista.getCurrentWorkload() + prepTotal);
        barista.setAvailableTime(now.plusMinutes(prepTotal));
        order.setStatus("ASSIGNED");
    }

    private SimulationResult calculateMetrics() {
        SimulationResult res = new SimulationResult();
        if (completedOrders.isEmpty()) return res;
        double totalWait = completedOrders.stream()
                .mapToLong(o -> ChronoUnit.MINUTES.between(o.getArrivalTime(), o.getCompletionTime()))
                .sum();
        res.setAverageWaitTimeMinutes(totalWait / (double) completedOrders.size());
        long timeouts = completedOrders.stream()
                .filter(o -> ChronoUnit.MINUTES.between(o.getArrivalTime(), o.getCompletionTime()) > MAX_WAIT_MINUTES)
                .count();
        res.setTimeoutRatePercent((timeouts * 100.0) / completedOrders.size());
        double avgLoad = baristas.stream().mapToDouble(Barista::getCurrentWorkload).average().orElse(0);
        double var = baristas.stream().mapToDouble(b -> Math.pow(b.getCurrentWorkload() - avgLoad, 2)).average().orElse(0);
        res.setWorkloadBalanceStdDev(Math.sqrt(var));
        long violations = completedOrders.stream().filter(o -> o.getSkippedCount() > MAX_SKIPS_TOLERATED).count();
        res.setFairnessViolationRatePercent((violations * 100.0) / completedOrders.size());
        return res;
    }
    
    public List<Order> getCompletedOrders() {
        return new ArrayList<>(completedOrders);
    }

    private void startProcessingThread() {
        processingThread = new Thread(() -> {
            LocalDateTime now = LocalDateTime.now();
            while (running) {
                recalculatePriorities(now);
                assignOrders(now);
                now = now.plusSeconds(30); // Simulate time progression every 30s
                try {
                    Thread.sleep(30000); // Real-time delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        processingThread.start();
    }

    public void placeOrder(List<DrinkType> drinks, boolean isLoyaltyGold) {
        Order order = new Order();
        order.setId((long) (completedOrders.size() + priorityQueue.size() + 1));
        order.setArrivalTime(LocalDateTime.now());
        order.setDrinks(drinks);
        order.setLoyaltyGold(isLoyaltyGold);
        updatePriorityScore(order, order.getArrivalTime());
        priorityQueue.add(order);
    }

    public List<Order> getWaitingOrders() {
        return new ArrayList<>(priorityQueue);
    }

    public List<Barista> getBaristas() {
        return new ArrayList<>(baristas);
    }

    public void stop() {
        running = false;
    }

    public int getEstimatedWait(Order order, LocalDateTime now) {
        long waitSoFar = ChronoUnit.MINUTES.between(order.getArrivalTime(), now);
        int prep = order.getDrinks().stream().mapToInt(DrinkType::getPrepTimeMinutes).sum();
        return (int) waitSoFar + prep; // Simple estimate; enhance with queue size
    }

    public String getReason(Order order) {
        if (order.getSkippedCount() > 0) return "Skipped for fairness (" + order.getSkippedCount() + ")";
        if (order.getPriorityScore() > 50) return "High priority (urgency boost)";
        return "Standard";
    }
}