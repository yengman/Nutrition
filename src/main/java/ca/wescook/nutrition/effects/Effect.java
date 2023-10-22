package ca.wescook.nutrition.effects;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.potion.Potion;

import ca.wescook.nutrition.nutrients.Nutrient;

// todo clean up unnecessary parts of this class and better comment things, as well as value bounds checking
public class Effect {

    public String name;
    public Potion potion;
    public int amplifier;
    public int minimum;
    public int maximum;
    public EnumDetectionType detectionType;
    public List<Nutrient> nutrients = new ArrayList<>();
    public int cumulativeModifier;

    public enum EnumDetectionType {
        ANY, // Any nutrient may be in the threshold.
        AVERAGE, // The average of all nutrients must be in the threshold.
        ALL, // All nutrients must be in the threshold.
        CUMULATIVE // For each nutrient within the threshold, the amplifier increases by one.
    }

}
