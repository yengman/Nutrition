package ca.wescook.nutrition.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import ca.wescook.nutrition.Tags;
import ca.wescook.nutrition.nutrients.Nutrient;
import ca.wescook.nutrition.nutrients.NutrientUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import squeek.applecore.api.AppleCoreAPI;

public class EventTooltip {

    @SubscribeEvent
    public void tooltipEvent(ItemTooltipEvent event) {
        ItemStack itemStack = event.itemStack;
        String tooltip = null;

        int value = 0;
        if (AppleCoreAPI.accessor.isFood(itemStack)) {
            value = AppleCoreAPI.accessor.getFoodValuesForPlayer(itemStack, event.entityPlayer).hunger;
        } else if (NutrientUtils.isSpecialFood(itemStack)) {
            Item item = itemStack.getItem();
            if (item instanceof ItemBucketMilk) {
                value = 4;
            }
        } else {
            // Get out if not a food item
            return;
        }

        // Create readable list of nutrients
        StringJoiner stringJoiner = new StringJoiner(", ");
        Map<Nutrient, Float> foundNutrients = NutrientUtils.getFoodNutrients(itemStack);
        if (foundNutrients.containsValue(0F)) {
            float defaultValue = NutrientUtils.calculateNutrition(value, foundNutrients);
            for (Nutrient nutrient : foundNutrients.keySet()) {
                if (foundNutrients.get(nutrient) == 0F) {
                    foundNutrients.put(nutrient, defaultValue);
                }
            }
        }
        Map<Float, List<Nutrient>> sortedNutrients = new HashMap<>();
        for (Nutrient nutrient : foundNutrients.keySet()) {// Loop through nutrients from food
            if (nutrient.visible) {
                // Get nutrition value
                float nutritionValue = foundNutrients.get(nutrient);
                if (sortedNutrients.get(nutritionValue) == null) {
                    List<Nutrient> nutrients = new ArrayList<>();
                    nutrients.add(nutrient);
                    sortedNutrients.put(nutritionValue, nutrients);
                } else {
                    sortedNutrients.get(nutritionValue)
                        .add(nutrient);
                }
            }
        }

        for (float nutritionValue : sortedNutrients.keySet()) {
            StringBuilder sortedNutrient = new StringBuilder();
            for (Nutrient nutrient : sortedNutrients.get(nutritionValue)) {
                sortedNutrient.append(I18n.format("nutrient." + Tags.MODID + ":" + nutrient.name) + " ");
            }
            stringJoiner.add(
                EnumChatFormatting.DARK_GREEN + sortedNutrient
                    .toString() + EnumChatFormatting.DARK_AQUA + "(" + String.format("%.1f", nutritionValue) + "%)");
        }
        String nutrientString = stringJoiner.toString();

        // Build tooltip
        if (!nutrientString.equals("")) {
            tooltip = I18n.format("tooltip." + Tags.MODID + ":nutrients") + " " + nutrientString;
        }

        // Add to item tooltip
        if (tooltip != null) event.toolTip.add(tooltip);
    }
}
