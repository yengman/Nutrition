package ca.wescook.nutrition.effects;

import java.util.ArrayList;
import java.util.List;

// Maintains information about effects (name, potion, nutrient conditions)
// Stored client and server-side
public class EffectsList {

    private static List<Effect> EFFECTS = new ArrayList<>();

    // Return all parsed effects
    public static List<Effect> get() {
        return EFFECTS;
    }

    public static void register(List<Effect> effectsIn) {
        EFFECTS.clear();
        EFFECTS.addAll(effectsIn);
    }

}
