package com.hackathon.coffee_shop_dilemma.controller;

import com.hackathon.coffee_shop_dilemma.model.DrinkType;
import com.hackathon.coffee_shop_dilemma.model.Order;
import com.hackathon.coffee_shop_dilemma.model.SimulationResult;
import com.hackathon.coffee_shop_dilemma.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Controller
public class SimulationController {

    @Autowired
    private SimulationService simulationService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/simulate")
    public String simulate(Model model) {
        int runs = 100; // or 500 if fast enough
        double totalAvgWait = 0;
        double totalTimeoutRate = 0;
        double totalBalance = 0;
        double totalFairness = 0;
        int totalOrders = 0;

        for (int i = 0; i < runs; i++) {
            SimulationResult r = simulationService.runSimulation();
            totalAvgWait += r.getAverageWaitTimeMinutes();
            totalTimeoutRate += r.getTimeoutRatePercent();
            totalBalance += r.getWorkloadBalanceStdDev();
            totalFairness += r.getFairnessViolationRatePercent();
            totalOrders += simulationService.getCompletedOrders().size();
        }

        SimulationResult avg = new SimulationResult();
        avg.setAverageWaitTimeMinutes(totalAvgWait / runs);
        avg.setTimeoutRatePercent(totalTimeoutRate / runs);
        avg.setWorkloadBalanceStdDev(totalBalance / runs);
        avg.setFairnessViolationRatePercent(totalFairness / runs);

        model.addAttribute("result", avg);
        model.addAttribute("runs", runs);
        model.addAttribute("avgOrders", totalOrders / runs);
        return "result";
    }

    @PostMapping("/placeOrder")
    public String placeOrder(Model model,
        @RequestParam("drinks") String drinksStr,
        @RequestParam(value = "loyalty", required = false, defaultValue = "false") boolean loyalty
    ) {
        try {
            List<DrinkType> drinks = Arrays.stream(drinksStr.split(","))
                .map(String::trim)
                .map(DrinkType::valueOf)
                .toList();
            simulationService.placeOrder(drinks, loyalty);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Invalid drink type. Use: COLD_BREW, ESPRESSO, etc.");
            return "index";
        }
        return "redirect:/queue";
    }

    @GetMapping("/queue")
    public String viewQueue(Model model) {
        List<Order> waitingOrders = simulationService.getWaitingOrders();
        LocalDateTime now = LocalDateTime.now();
        for (Order order : waitingOrders) {
            order.setEstimatedWait(simulationService.getEstimatedWait(order, now));
            order.setReason(simulationService.getReason(order));
        }
        model.addAttribute("waitingOrders", waitingOrders);
        model.addAttribute("currentTime", LocalDateTime.now());
        model.addAttribute("baristas", simulationService.getBaristas());
        return "queue";
    }
}