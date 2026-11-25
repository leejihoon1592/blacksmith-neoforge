package io.github.leejihoon1592.blacksmith.research.thermal;

import io.github.leejihoon1592.blacksmith.BlacksmithMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = BlacksmithMod.MODID)
public final class ResearchCommands {
    private ResearchCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("blacksmith")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("research")
                        .then(Commands.literal("unlock_all")
                                .executes(context -> unlock(context.getSource(), context.getSource().getPlayerOrException())))));
    }

    private static int unlock(CommandSourceStack source, ServerPlayer player) {
        if (ResearchData.ids().isEmpty()) {
            source.sendFailure(Component.translatable("commands.blacksmith.research.no_materials"));
            return 0;
        }

        ItemStack book = findJournal(player);
        boolean newBook = book.isEmpty();
        if (newBook) book = new ItemStack(BlacksmithMod.MATERIAL_JOURNAL.get());
        int count = ResearchData.unlockAll(book);
        if (newBook) player.getInventory().placeItemBackInInventory(book);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        source.sendSuccess(() -> Component.translatable("commands.blacksmith.research.unlocked", count), false);
        return count;
    }

    private static ItemStack findJournal(ServerPlayer player) {
        var journal = BlacksmithMod.MATERIAL_JOURNAL.get();
        if (player.getMainHandItem().is(journal)) return player.getMainHandItem();
        if (player.getOffhandItem().is(journal)) return player.getOffhandItem();
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(journal)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
