package ca.wescook.nutrition.utility;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import ca.wescook.nutrition.Tags;
import ca.wescook.nutrition.data.PlayerDataHandler;
import ca.wescook.nutrition.effects.EffectsList;
import ca.wescook.nutrition.effects.JsonEffect;
import ca.wescook.nutrition.nutrients.JsonNutrient;
import ca.wescook.nutrition.nutrients.NutrientList;
import ca.wescook.nutrition.nutrients.NutrientUtils;

// Handles JSON and API data loading
public class DataImporter {

    private static final Gson gson = new GsonBuilder().enableComplexMapKeySerialization()
        .setPrettyPrinting()
        .create();

    // Loads nutrients from JSONs and API
    // Runs initially during Post-Init, or from /reload command
    // Always call updatePlayerCapabilitiesOnServer() afterward if world is loaded
    public static void reload() {
        NutrientList.register(DataParser.parseNutrients(loadJsonNutrients()));
        EffectsList.register(DataParser.parseEffects(loadJsonEffects()));

        // List all foods registered in-game without nutrients
        if (Config.logMissingNutrients) NutrientUtils.findRegisteredFoods();
    }

    // Updates player capabilities on server so object IDs match those in NutrientList
    public static void updatePlayerCapabilitiesOnServer(MinecraftServer server) {
        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            if (!server.worldServerForDimension(0).isRemote) {
                PlayerDataHandler.getForPlayer(player)
                    .update();
            }
        }
    }

    //////////////////////////////////////////////////

    // Creates and parses nutrient json files into objects, returned as list
    private static List<JsonNutrient> loadJsonNutrients() {
        List<String> nutrientFiles = Lists
            .newArrayList("dairy.json", "example.json", "fruit.json", "grain.json", "protein.json", "vegetable.json");
        File nutrientDirectory = new File(Config.configDirectory, Tags.MODID + "/nutrients/");
        createConfigurationDirectory("assets/nutrition/configs/nutrients", nutrientDirectory, nutrientFiles);
        return readConfigurationDirectory(JsonNutrient.class, nutrientDirectory);
    }

    // Creates and parses effect json files into objects, returned as list
    private static List<JsonEffect> loadJsonEffects() {
        List<String> effectsFiles = Lists.newArrayList(
            "example.json",
            "mining_fatigue.json",
            "resistance.json",
            "strength.json",
            "toughness.json",
            "weakness.json");
        File effectsDirectory = new File(Config.configDirectory, Tags.MODID + "/effects");
        createConfigurationDirectory("assets/nutrition/configs/effects", effectsDirectory, effectsFiles);
        return readConfigurationDirectory(JsonEffect.class, effectsDirectory);
    }

    // Copies files from internal resources to external files. Accepts an input resource path, output directory, and
    // list of files
    private static void createConfigurationDirectory(String inputDirectory, File outputDirectory, List<String> files) {
        // Make no changes if directory already exists
        if (outputDirectory.exists()) return;

        // Create config directory
        outputDirectory.mkdirs();

        // Copy each file over
        ClassLoader loader = Thread.currentThread()
            .getContextClassLoader(); // Can access resources via class loader
        for (String file : files) {
            try (InputStream inputStream = loader.getResourceAsStream(inputDirectory + "/" + file)) { // Get input
                                                                                                      // stream of
                                                                                                      // resource
                Files.copy(inputStream, new File(outputDirectory + "/" + file).toPath()); // Create files from stream
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Reads in JSON as objects. Accepts object to serialize into, and directory to read json files. Returns array of
    // JSON objects.
    private static <T> List<T> readConfigurationDirectory(Class<T> classImport, File configDirectory) {
        File[] files = configDirectory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File file, String name) {
                return (name.toLowerCase()
                    .endsWith(".json"));
            }

        }); // List json files
        List<T> jsonObjectList = new ArrayList<>(); // List json objects

        for (File file : files) {
            try {
                JsonReader jsonReader = new JsonReader(new FileReader(file)); // Read in JSON
                jsonObjectList.add(gson.fromJson(jsonReader, classImport)); // Deserialize with GSON and store for later
                                                                            // processing
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                Log.fatal("The file " + file.getName() + " has invalid JSON and could not be loaded.");
                throw new IllegalArgumentException("Unable to load " + file.getName() + ".  Is the JSON valid?", e);
            }
        }

        return jsonObjectList;
    }

}
