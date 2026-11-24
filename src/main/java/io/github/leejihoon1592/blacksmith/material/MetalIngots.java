package io.github.leejihoon1592.blacksmith.material;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public final class MetalIngots {
    private static final List<RegistryObject<? extends Item>> OTHER_MATERIALS = new ArrayList<>();
    private static final List<RegistryObject<MetalIngotItem>> ALL = List.of(
            ingot("duracilium", 8, 7, 5, 3.5, 1.5),
            ingot("enduri", 9, 1, 7, 4.5, 3.5),
            ingot("tenaloct", 8, 3.5, 1.5, 7, 5),
            ingot("stevalite", 9, 4.5, 3, 1.5, 7),
            ingot("accutyrone", 6.5, 9, 4.5, 3.5, 1.5),
            ingot("charvin", 1.5, 9, 6.5, 4.5, 3.5),
            ingot("clinsor", 3, 9, 1.5, 7, 4.5),
            ingot("edgastium", 5, 8, 3.5, 1.5, 7),
            ingot("criminaltalloi", 7, 4.5, 8.5, 3.5, 1.5),
            ingot("brackstal", 1, 6.5, 9, 5, 3.5),
            ingot("durathorn", 3.5, 1.5, 8.5, 6.5, 5),
            ingot("granteron", 4, 3.5, 9, 1.5, 7),
            ingot("flexarite", 6, 5, 3.5, 9, 1.5),
            ingot("sprinal", 1, 7, 5, 8.5, 3.5),
            ingot("liselborn", 3.5, 1, 7, 8.5, 5),
            ingot("biklis", 4.5, 3.5, 1.5, 8.5, 7),
            ingot("conductite", 7, 5, 2.5, 1.5, 9),
            ingot("voltarite", 1.5, 7, 4.5, 3, 9),
            ingot("lumesor", 3.5, 1.5, 6.5, 4.5, 9),
            ingot("ginspa", 5, 3.5, 1, 7, 8.5)
    );

    private static RegistryObject<MetalIngotItem> ingot(String id, double durability, double polish,
            double hardness, double elasticity, double conductivity) {
        OTHER_MATERIALS.addAll(MetalForms.register(id));
        var stats = new MetalIngotItem.Stats(durability, polish, hardness, elasticity, conductivity);
        return BlacksmithMod.ITEMS.register(id + "_ingot", () -> new MetalIngotItem(stats));
    }

    public static void registerCreativeTab() {
        BlacksmithMod.CREATIVE_MODE_TABS.register("materials", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.blacksmith.materials"))
                .icon(() -> ALL.get(0).get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(BlacksmithMod.GUIDEBOOK.get());
                    output.accept(BlacksmithMod.MATERIAL_JOURNAL.get());
                    output.accept(BlacksmithMod.WEAPON_LEDGER.get());
                    io.github.leejihoon1592.blacksmith.research.ResearchRegistry.creative(output);
                    ALL.forEach(ingot -> output.accept(ingot.get()));
                    OTHER_MATERIALS.forEach(item -> output.accept(item.get()));
                })
                .build());
    }

    private MetalIngots() {}
}
