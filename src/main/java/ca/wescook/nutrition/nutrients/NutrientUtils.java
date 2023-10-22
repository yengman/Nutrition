package ca.wescook.nutrition.nutrients;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import ca.wescook.nutrition.utility.Config;
import ca.wescook.nutrition.utility.Log;
import squeek.applecore.api.AppleCoreAPI;
import squeek.applecore.api.food.FoodValues;

public class NutrientUtils {

    // Returns list of nutrients that food belongs to
    public static Map<Nutrient, Float> getFoodNutrients(ItemStack eatingFood) {
        Map<Nutrient, Float> nutrientsFound = new HashMap<>();

        // Loop through nutrients to look for food
        foodSearch: for (Nutrient nutrient : NutrientList.get()) { // All nutrients
            // Search foods
            for (ItemStack listedFood : nutrient.foodItems.keySet()) { // All foods in that category
                if (listedFood.isItemEqual(eatingFood)) {
                    nutrientsFound.put(nutrient, nutrient.foodItems.get(listedFood));// Add nutrient
                    continue foodSearch; // Skip rest of search in this nutrient, try others
                }
            }

            // Search ore dictionary
            for (String listedOreDict : nutrient.foodOreDict) { // All ore dicts in that nutrient
                for (ItemStack itemStack : OreDictionary.getOres(listedOreDict)) { // All items that match that oredict
                    // (eg. listAllmilk)
                    if (itemStack.isItemEqual(eatingFood)) { // Our food matches oredict
                        nutrientsFound.put(nutrient, 0F); // Add nutrient
                        continue foodSearch; // Skip rest of search in this nutrient, try others
                    }
                }
            }
        }

        return nutrientsFound;
    }

    // Calculate nutrition value for supplied food
    // Requires nutrient list from that food for performance reasons (see getFoodNutrients)
    public static float calculateNutrition(int foodValues, Map<Nutrient, Float> nutrients) {
        return getNutrientValue(foodValues, nutrients.size());
    }

    public static float getNutrientValue(int hungerValue, int numNutrients) {
        // Apply multipliers
        float adjustedFoodValue = (float) (hungerValue * 0.5); // Halve to start at reasonable starting point
        adjustedFoodValue = adjustedFoodValue * Config.nutritionMultiplier; // Multiply by config value
        float lossPercentage = (float) Config.lossPerNutrient / 100; // Loss percentage from config file
        // Lose 15% (configurable) for each nutrient added after the first nutrient
        float foodLoss = (adjustedFoodValue * lossPercentage * (numNutrients - 1));
        return Math.max(0, adjustedFoodValue - foodLoss);
    }

    // List all foods registered in-game without nutrients
    public static void findRegisteredFoods() {
        for (Object object : Item.itemRegistry) {
            Item item = (Item) object;
            ItemStack itemStack = new ItemStack(item);
            if (AppleCoreAPI.accessor.isFood(itemStack) && getFoodNutrients(itemStack).size() == 0)
                Log.warn("Registered food without nutrients: " + item.getUnlocalizedName());
        }
    }

    public static boolean isSpecialFood(ItemStack itemStack) {
        return itemStack.getItem() instanceof ItemBucketMilk;
    }
}
