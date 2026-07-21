package com.example.roulette;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RouletteCommand implements CommandExecutor {

    //  renderizza la texture di sfondo dalla resource pack
    public static final Component GUI_TITLE = Component.text("")
            .color(TextColor.color(0xFFFFFF));

    public static final int[] RING_SLOTS = {0, 1, 2, 3, 4, 13, 22, 31, 40, 39, 38, 37, 36, 27, 18, 9};
    public static final int CENTER_SLOT = 20;
    public static final int BLACK_BET_SLOT = 16;
    public static final int RED_BET_SLOT = 34;
    public static final int CHIPS_SLOT = 11;
    public static final int LEVER_UP_SLOT = 14;
    public static final int LEVER_DOWN_SLOT = 32;

    // Custom Model Data usato dalla texture pack: si applica SOLO alla lana
    // dell'anello nel menu /roulette, non alla lana nera/rossa "vera" nel mondo.
    public static final int RING_BLACK_CUSTOM_MODEL_DATA = 1001;
    public static final int RING_RED_CUSTOM_MODEL_DATA = 1002;

    private final RoulettePlugin plugin;

    public RouletteCommand(RoulettePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo i giocatori possono eseguire questo comando!");
            return true;
        }

        Inventory gui = Bukkit.createInventory(new RouletteHolder(), 45, GUI_TITLE);

        // Anello 5x5
        Material[][] pattern = {
                {Material.RED_WOOL,   Material.BLACK_WOOL, Material.RED_WOOL,   Material.BLACK_WOOL, Material.RED_WOOL},
                {Material.BLACK_WOOL, null,                null,                null,                Material.BLACK_WOOL},
                {Material.RED_WOOL,   null,                null,                null,                Material.RED_WOOL},
                {Material.BLACK_WOOL, null,                null,                null,                Material.BLACK_WOOL},
                {Material.RED_WOOL,   Material.BLACK_WOOL, Material.RED_WOOL,   Material.BLACK_WOOL, Material.RED_WOOL}
        };

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                Material mat = pattern[row][col];
                if (mat != null) {
                    int slot = row * 9 + col;
                    gui.setItem(slot, createRingItem(mat));
                }
            }
        }

        // Girasole al centro
        ItemStack sunflower = new ItemStack(Material.SUNFLOWER);
        ItemMeta sunflowerMeta = sunflower.getItemMeta();
        if (sunflowerMeta != null) {
            sunflowerMeta.displayName(Component.text("GIRA!").color(TextColor.color(0xFFD700)));
            sunflower.setItemMeta(sunflowerMeta);
        }
        gui.setItem(CENTER_SLOT, sunflower);

        // Leve bet
        updateLevers(gui, player);

        // Blocchi scommessa
        updateBetDisplay(gui, null, 0, player);

        player.openInventory(gui);
        return true;
    }

    static void updateLevers(Inventory gui, Player player) {
        int defaultBet = ((RoulettePlugin) player.getServer().getPluginManager().getPlugin("RoulettePlugin")).getPlayerDefaultBet(player.getUniqueId());
        String formatted = RoulettePlugin.formatBet(defaultBet);

        // Leva su (aumenta)
        ItemStack leverUp = new ItemStack(Material.LEVER);
        ItemMeta upMeta = leverUp.getItemMeta();
        if (upMeta != null) {
            upMeta.displayName(Component.text("AUMENTA BET").color(TextColor.color(0x00FF00)));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Bet attuale: " + formatted).color(TextColor.color(0xFFD700)));
            upMeta.lore(lore);
            leverUp.setItemMeta(upMeta);
        }
        gui.setItem(LEVER_UP_SLOT, leverUp);

        // Leva giu (diminuisce)
        ItemStack leverDown = new ItemStack(Material.LEVER);
        ItemMeta downMeta = leverDown.getItemMeta();
        if (downMeta != null) {
            downMeta.displayName(Component.text("DIMINUISCI BET").color(TextColor.color(0xFF0000)));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Bet attuale: " + formatted).color(TextColor.color(0xFFD700)));
            downMeta.lore(lore);
            leverDown.setItemMeta(downMeta);
        }
        gui.setItem(LEVER_DOWN_SLOT, leverDown);
    }

    static void updateChipsButton(Inventory gui, int betAmount) {
        if (betAmount <= 0) {
            gui.setItem(CHIPS_SLOT, null);
            return;
        }
        ItemStack chipBtn = new ItemStack(Material.WARPED_BUTTON);
        ItemMeta meta = chipBtn.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Chips").color(TextColor.color(0x00AAAA)));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(RoulettePlugin.formatBet(betAmount) + " chips").color(TextColor.color(0xFFD700)));
            meta.lore(lore);
            chipBtn.setItemMeta(meta);
        }
        gui.setItem(CHIPS_SLOT, chipBtn);
    }

    static void removeChipsButton(Inventory gui) {
        gui.setItem(CHIPS_SLOT, null);
    }

    /**
     * Crea un blocco di lana per l'anello della roulette. Se il materiale e'
     * lana nera o rossa, applica un Custom Model Data così che la texture
     * pack possa mostrare una texture diversa SOLO per questi item, senza
     * toccare la lana nera/rossa normale usata altrove nel server.
     */
    static ItemStack createRingItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            if (mat == Material.BLACK_WOOL) {
                meta.setCustomModelData(RING_BLACK_CUSTOM_MODEL_DATA);
            } else if (mat == Material.RED_WOOL) {
                meta.setCustomModelData(RING_RED_CUSTOM_MODEL_DATA);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    static void updateBetDisplay(Inventory gui, Material bet, int betAmount, Player player) {
        // Nero
        ItemStack blackBlock = new ItemStack(Material.BLACK_WOOL);
        ItemMeta blackMeta = blackBlock.getItemMeta();
        if (blackMeta != null) {
            if (bet == Material.BLACK_WOOL) {
                blackMeta.displayName(Component.text("Scommessa: NERO (" + RoulettePlugin.formatBet(betAmount) + " chips)").color(TextColor.color(0x00FF00)));
            } else {
                blackMeta.displayName(Component.text("Scommetti NERO").color(TextColor.color(0x555555)));
            }
            blackBlock.setItemMeta(blackMeta);
        }
        gui.setItem(BLACK_BET_SLOT, blackBlock);

        // Rosso
        ItemStack redBlock = new ItemStack(Material.RED_WOOL);
        ItemMeta redMeta = redBlock.getItemMeta();
        if (redMeta != null) {
            if (bet == Material.RED_WOOL) {
                redMeta.displayName(Component.text("Scommessa: ROSSO (" + RoulettePlugin.formatBet(betAmount) + " chips)").color(TextColor.color(0x00FF00)));
            } else {
                redMeta.displayName(Component.text("Scommetti ROSSO").color(TextColor.color(0xFF0000)));
            }
            redBlock.setItemMeta(redMeta);
        }
        gui.setItem(RED_BET_SLOT, redBlock);
    }

    static void showError(Inventory gui, int slot) {
        Material baseMat = slot == BLACK_BET_SLOT ? Material.BLACK_WOOL : Material.RED_WOOL;
        ItemStack item = new ItemStack(baseMat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Non hai abbastanza chips!").color(TextColor.color(0xFF0000)));
            item.setItemMeta(meta);
        }
        gui.setItem(slot, item);
    }
}
