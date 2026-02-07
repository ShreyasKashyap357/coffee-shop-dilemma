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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class SimulationController {

    @Autowired
    private SimulationService simulationService;

    @GetMapping("/")
    public String home(Model model) {
        simulationService.advanceLiveSystem();
        model.addAttribute("waitingOrders", simulationService.getWaitingOrders());
        model.addAttribute("baristas", simulationService.getBaristas());
        model.addAttribute("drinkTypes", DrinkType.values());
        List<SimulationResult> testResults = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            testResults.add(simulationService.runSimulation());
        }
        model.addAttribute("testResults", testResults);
        model.addAttribute("drinkBreakdown100", simulationService.getDrinkBreakdown(testResults));
        return "index";
    }

    @PostMapping("/placeOrder")
    public String placeOrder(
            @RequestParam(value = "drinks", required = false) List<String> drinksStr,
            @RequestParam(value = "loyalty", defaultValue = "false") boolean loyalty
    ) {
        if (drinksStr == null || drinksStr.isEmpty()) {
            return "redirect:/?error=nodrink";
        }

        List<DrinkType> drinks = new ArrayList<>();
        for (String d : drinksStr) {
            drinks.add(DrinkType.valueOf(d));
        }

        simulationService.placeOrder(drinks, loyalty);
        return "redirect:/";
    }

    @GetMapping("/simulate")
    public String simulate(Model model) {

        SimulationResult result = simulationService.runSimulation();

        model.addAttribute("result", result);
        model.addAttribute("completedOrdersCount", result.getCompletedOrders().size());

        model.addAttribute(
            "drinkStats",
            simulationService.getDrinkStatsForSimulation(result)
        );

        return "result";
    }

    @GetMapping("/queue")
    public String viewQueue(Model model) {
        List<Order> orders = simulationService.getWaitingOrders();
        orders.sort(Comparator.comparing(Order::getId));
        model.addAttribute("orders", orders);
        return "queue";
    }
}