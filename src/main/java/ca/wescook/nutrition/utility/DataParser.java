package ca.wescook.nutrition.utility;

import java.util.ArrayList;
import java.util.List;

import ca.wescook.nutrition.nutrients.NutrientUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

import org.apache.commons.lang3.math.NumberUtils;

import ca.wescook.nutrition.effects.Effect;
import ca.wescook.nutrition.effects.JsonEffect;
import ca.wescook.nutrition.nutrients.JsonNutrient;
import ca.wescook.nutrition.nutrients.Nutrient;
import ca.wescook.nutrition.nutrients.NutrientList;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import squeek.applecore.api.AppleCoreAPI;

public class DataParser {

    // Accepts a list of raw JSON objects, which are returned as cleaned Nutrients
    public static List<Nutrient> parseNutrients(List<JsonNutrient> jsonNutrients) {
        List<Nutrient> nutrients = new ArrayList<>();

        for (JsonNutrient nutrientRaw : jsonNutrients) {
            // Skip if nutrient is not enabled, or if field omitted (null)
            if (nutrientRaw.enabled != null && !nutrientRaw.enabled) continue;

            // Copying and cleaning data
            Nutrient nutrient = new Nutrient();

            // Name, icon color
            try {
                nutrient.name = nutrientRaw.name;
                nutrient.icon = getItemByName(nutrientRaw.name, nutrientRaw.icon).getItemStack(); // Create ItemStack
                                                                                                  // used to represent
                                                                                                  // icon
                nutrient.color = Integer.parseUnsignedInt("ff" + nutrientRaw.color, 16); // Convert hex string to int
            } catch (NullPointerException e) {
                Log.fatal("Missing or invalid JSON.  A name, icon, and color are required.");
                throw e;
            }

            // Starting value
            // Determined either by global rate, or optional override in nutrient file
            if (nutrientRaw.starting == null) nutrient.startingNutrition = Config.startingNutrition; // Set to global
                                                                                                     // value
            else if (nutrientRaw.starting >= 0 && nutrientRaw.starting <= 100)
                nutrient.startingNutrition = nutrientRaw.starting;
            else {
                nutrient.startingNutrition = 0;
                Log.error("Starting value must be between 0 and 100 (" + nutrient.name + ").");
                continue;
            }

            // Minimum value of nutrition to trigger penalty on death
            // Determined either by global rate, or optional override in nutrient file
            if (nutrientRaw.deathmin == null) nutrient.deathPenaltyMin = Config.deathPenaltyMin; // Set to global value
            else if (nutrientRaw.deathmin >= 0 && nutrientRaw.deathmin <= 100)
                nutrient.deathPenaltyMin = nutrientRaw.deathmin;
            else {
                nutrient.deathPenaltyMin = 0;
                Log.error("Death penalty threshold must be between 0 and 100 (" + nutrient.name + ").");
                continue;
            }

            // Value to subtract nutrition upon death if above penalty threshold
            // Determined either by global rate, or optional override in nutrient file
            if (nutrientRaw.deathloss == null) nutrient.deathPenaltyLoss = Config.deathPenaltyLoss; // Set to global
                                                                                                    // value
            else if (nutrientRaw.deathloss >= 0 && nutrientRaw.deathloss <= 100)
                nutrient.deathPenaltyLoss = nutrientRaw.deathloss;
            else {
                nutrient.deathPenaltyLoss = 0;
                Log.error("Death loss value must be between 0 and 100 (" + nutrient.name + ").");
                continue;
            }

            // Decay rate multiplier
            // Determined either by global rate, or optional override in nutrient file
            if (nutrientRaw.decay == null) nutrient.decay = Config.decayMultiplier; // Set to global value
            else if (nutrientRaw.decay >= -100 && nutrientRaw.decay <= 100) nutrient.decay = nutrientRaw.decay;
            else {
                nutrient.decay = 0;
                Log.error("Decay rate must be between -100 and 100 (" + nutrient.name + ").");
                continue;
            }

            // Nutrient Visibility
            nutrient.visible = (nutrientRaw.visible == null || nutrientRaw.visible);

            // Food - Ore Dictionary
            if (nutrientRaw.food.oredict != null) nutrient.foodOreDict = nutrientRaw.food.oredict; // Ore dicts remains
                                                                                                   // as strings

            // Food Items
            if (nutrientRaw.food.items != null) {
                for (String fullName : nutrientRaw.food.items) {
                    ItemData data = getItemByName(nutrient.name, fullName, true);
                    if (data == null) continue;

                    // Get item
                    Item item = data.getItem();

                    // Item ID not found, issue warning and skip adding item
                    if (item == null) {
                        if (Config.logMissingFood && Loader.isModLoaded(data.modid)) {
                            Log.warn("Food with nutrients doesn't exist: " + fullName + " (" + nutrient.name + ")");
                        }
                        continue;
                    }

                    // Add to nutrient, or report error
                    ItemStack itemStack = new ItemStack(item, 1, data.metadata);
                    if (AppleCoreAPI.accessor.isFood(itemStack) || NutrientUtils.isSpecialFood(itemStack)) {
                        nutrient.foodItems.put(itemStack, data.value);
                    } else {
                        Log.warn(data.name + " is not a valid food (" + fullName + ")");
                    }
                }
            }

            // Register nutrient
            nutrients.add(nutrient);
        }

        return nutrients;
    }

    // Accepts a list of raw JSON objects, which are returned as cleaned Effects
    public static List<Effect> parseEffects(List<JsonEffect> jsonEffects) {
        List<Effect> effects = new ArrayList<>();

        for (JsonEffect effectRaw : jsonEffects) {
            // Skip if effect is not enabled, or if field omitted (null)
            if (effectRaw.enabled != null && !effectRaw.enabled) continue;

            Effect effect = new Effect();

            boolean foundPotion = false;
            for (Potion potion : Potion.potionTypes) {
                if (potion != null) {
                    if (effectRaw.potion.equals(potion.getName())) {
                        effect.potion = potion;
                        foundPotion = true;
                        break;
                    }
                }
            }

            if (!foundPotion) {
                Log.error("Potion '" + effectRaw.potion + "' is not valid (" + effectRaw.name + ").");
                continue;
            }

            // Copying and cleaning data
            effect.name = effectRaw.name;
            effect.minimum = effectRaw.minimum;
            effect.maximum = effectRaw.maximum;

            switch (effectRaw.detect.toLowerCase()) {
                case "any" -> {
                    effect.detectionType = Effect.EnumDetectionType.ANY;
                }
                case "average" -> {
                    effect.detectionType = Effect.EnumDetectionType.AVERAGE;
                }
                case "all" -> {
                    effect.detectionType = Effect.EnumDetectionType.ALL;

                }
                case "cumulative" -> {
                    effect.detectionType = Effect.EnumDetectionType.CUMULATIVE;
                }
                default -> {
                    effect.detectionType = Effect.EnumDetectionType.AVERAGE;
                }
            }

            // Amplifier defaults to 0 if undefined
            effect.amplifier = (effectRaw.amplifier != null) ? effectRaw.amplifier : 0;

            // Default the cumulative modifier to 1 if not defined
            effect.cumulativeModifier = (effectRaw.cumulative_modifier != null) ? effectRaw.cumulative_modifier : 1;

            // Build list of applicable nutrients
            // If nutrients are unspecified in file, this defaults to include every nutrient
            if (effectRaw.nutrients.size() == 0) {
                effect.nutrients.addAll(NutrientList.get());
            } else { // Field has been set, so fetch nutrients by name
                for (String nutrientName : effectRaw.nutrients) {
                    Nutrient nutrient = NutrientList.getByName(nutrientName);
                    if (nutrient != null) effect.nutrients.add(nutrient); // Nutrient checks out, add to list
                    else Log.error("Nutrient " + nutrientName + " not found (" + effectRaw.name + ").");
                }
            }

            // Register effect
            effects.add(effect);
        }

        return effects;
    }

    private static ItemData getItemByName(String nutrientName, String fullName, boolean throwException) {
        String modid;
        String name;
        int metadata = 0;
        // Null check input string
        if (fullName == null) {
            Log.fatal(
                "There is a null item in the '" + nutrientName + "' JSON.  Check for a trailing comma in the file.");
            if (throwException) {
                throw new NullPointerException(
                    "There is a null item in the '" + nutrientName
                        + "' JSON.  Check for a trailing comma in the file.");
            }
            return null;
        }
        String[] nameWithValue = fullName.split("/");
        String[] splitName = nameWithValue[0].split(":");
        if (splitName.length <= 1) {
            Log.fatal(
                "There is an item missing a modid in the '" + nutrientName
                    + "' JSON. Ensure names are formatted like 'minecraft:golden_apple'");
            if (throwException) {
                throw new NullPointerException(
                    "There is an item missing a modid in the '" + nutrientName
                        + "' JSON. Ensure names are formatted like 'minecraft:golden_apple'");
            }
            return null;
        }
        modid = splitName[0];
        name = splitName[1];
        if (splitName.length > 2) {
            if (NumberUtils.isNumber(splitName[2])) {
                metadata = Integer.decode(splitName[2]);
            } else {
                Log.warn(fullName + " does not contain valid metadata");
                return null;
            }
        }
        // Custom value configs
        float value = 0;

        if (nameWithValue.length == 2) {
            if (NumberUtils.isNumber(nameWithValue[1])) {
                value = Float.valueOf(nameWithValue[1]);
            } else {
                Log.warn(fullName + " has an invalid custom value");
                return null;
            }
        }

        return new ItemData(modid, name, metadata, value);
    }

    private static ItemData getItemByName(String nutrientName, String fullName) {
        return getItemByName(nutrientName, fullName, false);
    }

    private static class ItemData {

        private final String modid, name;
        private final int metadata;

        private final float value;

        private ItemData(String modid, String name, int metadata, float value) {
            this.modid = modid;
            this.name = name;
            this.metadata = metadata;
            this.value = value;
        }

        public Item getItem() {
            return GameRegistry.findItem(modid, name);
        }

        public ItemStack getItemStack() {
            return new ItemStack(getItem(), 1, metadata);
        }

    }
}
