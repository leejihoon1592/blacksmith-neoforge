package io.github.leejihoon1592.blacksmith.item;

import io.github.leejihoon1592.blacksmith.guide.GuideNetwork;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public final class GuidebookItem extends Item {
    public enum Book {
        PRIMER("guidebook"), MATERIALS("material_journal"), WEAPONS("weapon_ledger");

        private final String id;

        Book(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private final Book book;

    public GuidebookItem(Book book) {
        super(new Item.Properties().stacksTo(1));
        this.book = book;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (book == Book.MATERIALS) io.github.leejihoon1592.blacksmith.research.thermal.ResearchMenu.openBook(serverPlayer, hand);
            else GuideNetwork.open(serverPlayer, book);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
