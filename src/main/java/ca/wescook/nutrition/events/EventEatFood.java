package ca.wescook.nutrition.events;

import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucketMilk;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;

import ca.wescook.nutrition.Nutrition;
import ca.wescook.nutrition.data.NutrientManager;
import ca.wescook.nutrition.data.PlayerDataHandler;
import ca.wescook.nutrition.effects.EffectsManager;
import ca.wescook.nutrition.nutrients.Nutrient;
import ca.wescook.nutrition.nutrients.NutrientUtils;
import ca.wescook.nutrition.proxy.ClientProxy;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import squeek.applecore.api.food.FoodEvent;

/**
 * Class has a complex hierarchy of calls to these events, as depending on how stats are changed, different things need
 * to be done.
 * "Normal food" follows the event order:
 * - FoodStatsAddition -> FoodEaten -> UseItem.Finish
 * <br>
 * "Stat modifying" items such as healing axe, IC2 food cans, etc. follows event order:
 * - FoodStatsAddition -> UseItem.Finish (SOMETIMES, depending on the specific item)
 * <br>
 * However, FoodStatsAddition, the only common event here, does not provide the Food ItemStack, so there is no way to
 * gather nutrients,
 * nor discern if this is a Food or some other direct modification method.
 * <br>
 * As a result, we need to know if stats were modified directly without eating an actual food, so that
 * nutrition values are modified somehow to a "neutral state" by direct-modification methods.
 * <br>
 * This is achieved with a Stack, held in {@link ClientProxy}.
 * Hunger value stat changes are pushed to the stack, then popped when food is eaten. This results in
 * a "normal" food pushing the value, then popping it immediately after in the next event.
 * However, something which directly modifies hunger stat will never pop the change.
 * Those changes will be popped by {@link EventWorldTick#clientTickEvent(TickEvent.ClientTickEvent)}
 * at the end of each client game tick.
 */
public class EventEatFood {

    @SubscribeEvent
    public void onFoodStatsChanged(FoodEvent.FoodStatsAddition event) {
        if (Nutrition.proxy.isClient()) {
            // only run if hunger value increases, also ignoring saturation
            int hungerValue = event.foodValuesToBeAdded.hunger;
            if (hungerValue <= 0) return;

            // set that stats have been changed, but food has not yet been eaten
            ClientProxy.pushHungerChange(hungerValue);
        }
    }

    @SubscribeEvent
    public void onFoodEaten(FoodEvent.FoodEaten event) {
        // Calculate nutrition
        Map<Nutrient, Float> foundNutrients = NutrientUtils.getFoodNutrients(event.food);

        if (foundNutrients.containsValue(0F)) {
            float defaultValue = NutrientUtils.calculateNutrition(event.foodValues.hunger, foundNutrients);
            for (Nutrient nutrient : foundNutrients.keySet()) {
                if (foundNutrients.get(nutrient) == 0F) {
                    foundNutrients.put(nutrient, defaultValue);
                }
            }
        }

        // Add to each nutrient
        if (!event.player.getEntityWorld().isRemote) { // Server
            NutrientManager nutrientManager = PlayerDataHandler.getForPlayer(event.player);
            for (Nutrient nutrient : foundNutrients.keySet()) {
                nutrientManager.add(nutrient, foundNutrients.get(nutrient));
            }
        } else { // Client
            // set that food has now been eaten
            for (Nutrient nutrient : foundNutrients.keySet()) {
                ClientProxy.localNutrition.add(nutrient, foundNutrients.get(nutrient));
            }
            ClientProxy.popHungerChange();
        }
    }

    // Handle drinking milk
    @SubscribeEvent
    public void finishUsingItem(PlayerUseItemEvent.Finish event) {
        // Only check against players
        if (!(event.entity instanceof EntityPlayer player)) {
            return;
        }

        if (NutrientUtils.isSpecialFood(event.item)) {
            Map<Nutrient, Float> foundNutrients = NutrientUtils.getFoodNutrients(event.item);

            if (foundNutrients.containsValue(0F)) {
                Item item = event.item.getItem();
                int value = 0;
                if (item instanceof ItemBucketMilk) {
                    value = 4;
                    if (!player.getEntityWorld().isRemote) {
                        EffectsManager.reapplyEffects(player);
                    }
                }
                float defaultValue = NutrientUtils.calculateNutrition(value, foundNutrients);
                for (Nutrient nutrient : foundNutrients.keySet()) {
                    if (foundNutrients.get(nutrient) == 0F) {
                        foundNutrients.put(nutrient, defaultValue);
                    }
                }
            }

            // Add to each nutrient
            if (!player.getEntityWorld().isRemote) { // Server
                NutrientManager nutrientManager = PlayerDataHandler.getForPlayer(player);
                for (Nutrient nutrient : foundNutrients.keySet()) {
                    nutrientManager.add(nutrient, foundNutrients.get(nutrient));
                }
            } else { // Client
                // set that food has now been eaten
                for (Nutrient nutrient : foundNutrients.keySet()) {
                    ClientProxy.localNutrition.add(nutrient, foundNutrients.get(nutrient));
                }
            }
        }
    }

}
