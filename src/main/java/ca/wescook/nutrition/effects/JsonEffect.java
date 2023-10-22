package ca.wescook.nutrition.effects;

import java.util.ArrayList;
import java.util.List;

public class JsonEffect {

    // This class mimics the layout of the nutrient json files
    public String name;
    public String potion;
    public Integer amplifier;
    public int minimum;
    public int maximum;
    public String detect;
    public List<String> nutrients = new ArrayList<>();
    public Integer cumulative_modifier;
    public Boolean enabled;

}
