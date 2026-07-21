package com.example.roulette;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RouletteListener implements Listener {

    private final RoulettePlugin plugin;
    private final Map<UUID, Integer> errorSlot = new HashMap<>();

    public RouletteListener(RoulettePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof RouletteHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (holder.isSpinning()) {
            return;
        }

        int slot = event.getRawSlot();
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        UUID uuid = player.getUniqueId();

        if (errorSlot.containsKey(uuid) && errorSlot.get(uuid) != slot) {
            restoreSlot(top, errorSlot.get(uuid), player);
            errorSlot.remove(uuid);
        }

        if (slot == RouletteCommand.CENTER_SLOT) {
            Material bet = plugin.getPlayerBet(uuid);
            if (bet == null) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            new RouletteGame(plugin, top, player).start();

        } else if (slot == RouletteCommand.BLACK_BET_SLOT) {
            handleBetClick(player, top, Material.BLACK_WOOL, slot);

        } else if (slot == RouletteCommand.RED_BET_SLOT) {
            handleBetClick(player, top, Material.RED_WOOL, slot);

        } else if (slot == RouletteCommand.CHIPS_SLOT) {
            handleChipsButtonClick(player, top);

        } else if (slot == RouletteCommand.LEVER_UP_SLOT) {
            handleLeverUp(player, top);

        } else if (slot == RouletteCommand.LEVER_DOWN_SLOT) {
            handleLeverDown(player, top);

        } else {
            if (errorSlot.containsKey(uuid)) {
                restoreSlot(top, errorSlot.get(uuid), player);
                errorSlot.remove(uuid);
            }
        }
    }

    private void handleBetClick(Player player, Inventory gui, Material color, int slot) {
        UUID uuid = player.getUniqueId();
        int chips = plugin.getChips(uuid);
        int currentBet = plugin.getPlayerBetAmount(uuid);
        int defaultBet = plugin.getPlayerDefaultBet(uuid);
        int newTotalBet = currentBet + defaultBet;

        if (errorSlot.containsKey(uuid) && errorSlot.get(uuid) == slot) {
            restoreSlot(gui, slot, player);
            errorSlot.remove(uuid);
        }

        if (newTotalBet > chips) {
            RouletteCommand.showError(gui, slot);
            errorSlot.put(uuid, slot);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        plugin.setPlayerBet(uuid, color);
        plugin.addPlayerBetAmount(uuid, defaultBet);
        int newBet = plugin.getPlayerBetAmount(uuid);

        RouletteCommand.updateBetDisplay(gui, color, newBet, player);
        RouletteCommand.updateChipsButton(gui, newBet);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private void handleChipsButtonClick(Player player, Inventory gui) {
        UUID uuid = player.getUniqueId();
        int currentBet = plugin.getPlayerBetAmount(uuid);

        if (currentBet <= 0) {
            return;
        }

        int defaultBet = plugin.getPlayerDefaultBet(uuid);
        int newBet = currentBet - defaultBet;
        if (newBet < 0) newBet = 0;

        plugin.setPlayerBetAmount(uuid, newBet);

        if (newBet == 0) {
            plugin.setPlayerBet(uuid, null);
            RouletteCommand.updateBetDisplay(gui, null, 0, player);
            RouletteCommand.removeChipsButton(gui);
        } else {
            Material bet = plugin.getPlayerBet(uuid);
            RouletteCommand.updateBetDisplay(gui, bet, newBet, player);
            RouletteCommand.updateChipsButton(gui, newBet);
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
    }

    private void handleLeverUp(Player player, Inventory gui) {
        UUID uuid = player.getUniqueId();
        int currentBet = plugin.getPlayerDefaultBet(uuid);
        int[] steps = RoulettePlugin.BET_STEPS;

        int currentIndex = -1;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == currentBet) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < steps.length - 1) {
            plugin.setPlayerDefaultBet(uuid, steps[currentIndex + 1]);
            RouletteCommand.updateLevers(gui, player);
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.2f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void handleLeverDown(Player player, Inventory gui) {
        UUID uuid = player.getUniqueId();
        int currentBet = plugin.getPlayerDefaultBet(uuid);
        int[] steps = RoulettePlugin.BET_STEPS;

        int currentIndex = -1;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == currentBet) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex > 0) {
            plugin.setPlayerDefaultBet(uuid, steps[currentIndex - 1]);
            RouletteCommand.updateLevers(gui, player);
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 0.8f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void restoreSlot(Inventory gui, int slot, Player player) {
        Material bet = plugin.getPlayerBet(player.getUniqueId());
        int betAmount = plugin.getPlayerBetAmount(player.getUniqueId());
        if (slot == RouletteCommand.BLACK_BET_SLOT) {
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
            gui.setItem(slot, blackBlock);
        } else if (slot == RouletteCommand.RED_BET_SLOT) {
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
            gui.setItem(slot, redBlock);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RouletteHolder holder) {
            if (holder.isSpinning() && event.getPlayer() instanceof Player player) {
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(event.getView().getTopInventory()));
                return;
            }
            UUID uuid = event.getPlayer().getUniqueId();
            plugin.setPlayerBet(uuid, null);
            plugin.setPlayerBetAmount(uuid, 0);
            errorSlot.remove(uuid);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RouletteHolder) {
            event.setCancelled(true);
        }
    }
}
