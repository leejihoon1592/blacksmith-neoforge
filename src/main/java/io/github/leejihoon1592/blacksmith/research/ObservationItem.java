package io.github.leejihoon1592.blacksmith.research;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class ObservationItem extends Item {
    public ObservationItem() { super(new Properties().stacksTo(1)); }
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if(level.isClientSide) net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> ObservationScreen.open(player.getItemInHand(hand)));
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    public static ItemStack create(ThermalExperiment.Observation event, java.util.UUID experiment) {
        ItemStack stack = new ItemStack(ResearchRegistry.NOTE.get());
        var tag = stack.getOrCreateTag();
        tag.putUUID("Experiment", experiment);
        tag.putBoolean("Onset", event.onset());
        tag.putDouble("Temperature", event.temperature());
        tag.putDouble("Seconds", event.elapsedTicks()/20.0);
        tag.putBoolean("Mixed", event.mixed());
        // No material ID or pairing ID: identifying the component is the player's research.
        tag.putString("Quantity", event.mixed() ? "unknown" : event.quantity());
        return stack;
    }
    public static List<Component> lines(ItemStack stack) {
        if (!stack.hasTag()) return List.of(Component.translatable("research.blacksmith.blank_note"));
        var tag = stack.getTag();
        if (tag.getBoolean("Onset")) return List.of(
                Component.translatable("research.blacksmith.onset"),
                Component.translatable("research.blacksmith.temperature", tag.getDouble("Temperature")));
        if (tag.getBoolean("Mixed")) return List.of(Component.translatable("research.blacksmith.mixed"));
        return List.of(Component.translatable("research.blacksmith.discharge"),
                Component.translatable("research.blacksmith.quantity." + tag.getString("Quantity")),
                Component.translatable("research.blacksmith.seconds", tag.getDouble("Seconds")));
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.addAll(lines(stack));
    }
}
