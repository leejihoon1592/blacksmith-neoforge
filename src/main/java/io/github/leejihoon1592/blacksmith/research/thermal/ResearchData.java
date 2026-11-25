package io.github.leejihoon1592.blacksmith.research.thermal;

import com.google.gson.*;
import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import io.github.leejihoon1592.blacksmith.item.MetalIngotItem;
import io.github.leejihoon1592.blacksmith.item.RawFragmentItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

@net.neoforged.fml.common.EventBusSubscriber(modid = BlacksmithMod.MODID)
public final class ResearchData extends SimpleJsonResourceReloadListener {
    public record Material(String id, double point, double seconds) {}
    private static Map<String, Material> materials = Map.of();
    private static Map<String, List<ThermalExperiment.Component>> samples = Map.of();
    public static double closeTemperature = 20;
    public static double closeSeconds = 2;
    public ResearchData() { super(new Gson(), "research"); }

    @SubscribeEvent public static void reload(AddReloadListenerEvent event) { event.addListener(new ResearchData()); }

    @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, Material> nextMaterials = new LinkedHashMap<>();
        Map<String, List<ThermalExperiment.Component>> nextSamples = new HashMap<>();
        JsonObject config = files.get(ResourceLocation.fromNamespaceAndPath("blacksmith", "thermal")).getAsJsonObject();
        JsonObject bases = config.getAsJsonObject("base_seconds");
        for (var entry : config.getAsJsonObject("specializations").entrySet()) {
            String id = entry.getKey();
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("blacksmith", id + "_ingot"));
            if (!(item instanceof MetalIngotItem metal)) throw new JsonParseException("Unknown metal: " + id);
            double seconds = MaterialPhysics.meltingSeconds(metal.baseStats(), bases.get(entry.getValue().getAsString()).getAsDouble());
            if (seconds <= 0) throw new JsonParseException("Nonpositive melting time: " + id);
            nextMaterials.put(id, new Material(id, MaterialPhysics.meltingPoint(metal.baseStats()), seconds));
        }
        JsonObject hosts = config.getAsJsonObject("hosts");
        JsonObject byproducts = config.getAsJsonObject("byproducts");
        for (var entry : byproducts.entrySet()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            nextMaterials.put(entry.getKey(), new Material(entry.getKey(), value.get("point").getAsDouble(), value.get("seconds").getAsDouble()));
        }
        for (var entry : hosts.entrySet()) {
            JsonObject host = entry.getValue().getAsJsonObject();
            nextMaterials.put(entry.getKey(), new Material(entry.getKey(), host.get("point").getAsDouble(), host.get("seconds").getAsDouble()));
        }
        for (String metal : config.getAsJsonObject("specializations").keySet()) {
            for (String host : hosts.keySet()) {
                List<ThermalExperiment.Component> parts = new ArrayList<>();
                parts.add(component(nextMaterials.get(metal), 4, "many"));
                parts.add(component(nextMaterials.get(host), 8, "very_many"));
                JsonElement extra = config.getAsJsonObject("sample_byproducts").get(metal);
                if (extra != null) parts.add(component(Objects.requireNonNull(nextMaterials.get(extra.getAsString())), 1, "little"));
                nextSamples.put(metal + "/" + host, List.copyOf(parts));
            }
        }
        closeTemperature = config.get("close_temperature").getAsDouble();
        closeSeconds = config.get("close_seconds").getAsDouble();
        materials = Collections.unmodifiableMap(nextMaterials);
        samples = Map.copyOf(nextSamples);
    }

    private static ThermalExperiment.Component component(Material m, int amount, String quantity) {
        return new ThermalExperiment.Component(m.id(), m.point(), (int)Math.round(m.seconds()*20), amount, quantity);
    }

    public static boolean canResearchSample(ItemStack stack) {
        return sampleMetalId(stack) != null;
    }

    private static String sampleMetalId(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals(BlacksmithMod.MODID)) return null;
        String name = key.getPath();
        if (name.endsWith("_raw_fragment")) {
            return name.substring(0, name.length() - "_raw_fragment".length());
        }
        if (!name.startsWith("raw_")) return null;
        String metal = name.substring("raw_".length());
        return isMetal(metal) ? metal : null;
    }

    public static List<ThermalExperiment.Component> sample(ItemStack stack) {
        String metal = sampleMetalId(stack);
        if (metal == null) return List.of();
        if (stack.getItem() instanceof RawFragmentItem) {
            return samples.getOrDefault(metal + "/" + RawFragmentItem.researchHost(stack), List.of());
        }
        Material material = materials.get(metal);
        return material == null ? List.of() : List.of(component(material, 4, "many"));
    }
    public static boolean isMetal(String material) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("blacksmith", material + "_ingot"))
                instanceof MetalIngotItem;
    }

    public static Material material(String id) { return materials.get(id); }
    public static List<String> ids() { return List.copyOf(materials.keySet()); }
    private static final String ENTRIES = "ResearchEntries";

    public static boolean isMechanicalProperty(String property) {
        return property.equals("hardness") || property.equals("elasticity") || property.equals("polish");
    }

    public static void record(ItemStack book, String material, String property, double guess) {
        Material definition = materials.get(material);
        if (definition == null || !Double.isFinite(guess) || guess <= 0 || guess > 10000) return;
        double actual;
        double tolerance;
        MetalIngotItem.Stats stats = null;
        switch (property) {
            case "point" -> {
                actual = definition.point();
                tolerance = closeTemperature;
            }
            case "seconds" -> {
                actual = definition.seconds();
                tolerance = closeSeconds;
            }
            case "hardness", "elasticity", "polish" -> {
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("blacksmith", material + "_ingot"));
                if (!(item instanceof MetalIngotItem ingot) || guess * 2 != Math.rint(guess * 2)) return;
                stats = ingot.baseStats();
                actual = switch (property) {
                    case "hardness" -> stats.hardness();
                    case "elasticity" -> stats.elasticity();
                    default -> stats.polish();
                };
                tolerance = 0.5;
            }
            default -> { return; }
        }
        double error = Math.abs(actual - guess);
        var data = book.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var entries = data.getCompound(ENTRIES);
        var entry = entries.getCompound(material);
        entry.putDouble(property, guess);
        entry.putInt(property + "Grade", error < 0.0001 ? 2 : error <= tolerance ? 1 : 0);
        if (stats != null) {
            if (entry.getInt("hardnessGrade") == 2 && entry.getInt("elasticityGrade") == 2
                    && entry.getInt("polishGrade") == 2) {
                entry.putDouble("durability", stats.durability());
                entry.putInt("durabilityGrade", 2);
            } else {
                entry.remove("durability");
                entry.remove("durabilityGrade");
            }
        }
        entries.put(material, entry);
        data.put(ENTRIES, entries);
        book.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
}
