package io.github.leejihoon1592.blacksmith.research;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class ObservationItem extends Item {
    public ObservationItem() { super(new Properties().stacksTo(1)); }
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if(level.isClientSide) ObservationScreen.open(player.getItemInHand(hand));
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    public static ItemStack create(ThermalExperiment.Observation event, java.util.UUID experiment) {
        ItemStack stack = new ItemStack(ResearchRegistry.NOTE.get());
        var tag = new CompoundTag();
        tag.putUUID("Experiment", experiment);
        tag.putBoolean("Onset", event.onset());
        tag.putDouble("Temperature", event.temperature());
        tag.putDouble("Seconds", event.elapsedTicks()/20.0);
        tag.putBoolean("Mixed", event.mixed());
        // No material ID or pairing ID: identifying the component is the player's research.
        tag.putString("Quantity", event.mixed() ? "unknown" : event.quantity());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
    public static ItemStack createScratchNote(int sampleHalfPoints, ScratchResult result) {
        ItemStack stack = new ItemStack(ResearchRegistry.NOTE.get());
        var tag = new CompoundTag();
        tag.putInt("ScratchSample", sampleHalfPoints);
        tag.putString("ScratchResult", result.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static Component scratchLine(int sampleHalfPoints, ScratchResult result) {
        String sample = sampleHalfPoints % 2 == 0 ? Integer.toString(sampleHalfPoints / 2)
                : sampleHalfPoints / 2 + ".5";
        return Component.translatable("research.blacksmith.scratch_note", sample,
                Component.translatable("research.blacksmith.scratch." + result.name().toLowerCase(java.util.Locale.ROOT)));
    }

    public static List<Component> lines(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return List.of(Component.translatable("research.blacksmith.blank_note"));
        var tag = data.copyTag();
        if (tag.contains("ScratchSample")) {
            for (ScratchResult result : ScratchResult.values()) {
                if (result.name().equals(tag.getString("ScratchResult"))) {
                    return List.of(scratchLine(tag.getInt("ScratchSample"), result));
                }
            }
            return List.of(Component.translatable("research.blacksmith.blank_note"));
        }
        if (tag.getBoolean("Onset")) return List.of(
                Component.translatable("research.blacksmith.onset"),
                Component.translatable("research.blacksmith.temperature", tag.getDouble("Temperature")));
        if (tag.getBoolean("Mixed")) return List.of(Component.translatable("research.blacksmith.mixed"));
        return List.of(Component.translatable("research.blacksmith.discharge"),
                Component.translatable("research.blacksmith.quantity." + tag.getString("Quantity")),
                Component.translatable("research.blacksmith.seconds", tag.getDouble("Seconds")));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.addAll(lines(stack));
    }
}
