package FoodManagement;

import java.util.Map;

public class OfferDiscount {
    // $1.5 off per combo (Burger+Fries+Coke)
    public static double calculateDiscount(Map<String, Integer> cart) {
        int burgers = cart.getOrDefault("Burger", 0);
        int fries = cart.getOrDefault("Fries", 0);
        int cokes = cart.getOrDefault("Coke", 0);

        int combos = Math.min(burgers, Math.min(fries, cokes));
        return combos * 1.5;
    }
}
