package com.midnightsmp.midnightrevivebeacon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class MidnightReviveBeacon extends JavaPlugin implements Listener, CommandExecutor {

    private NamespacedKey beaconKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        beaconKey = new NamespacedKey(this, "revive_beacon");

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("giverevivebeacon").setExecutor(this);
        getCommand("revive").setExecutor(this);

        registerRecipe();
        getLogger().info("MidnightReviveBeacon enabled!");
    }

    private void registerRecipe() {
        ItemStack reviveBeacon = getReviveBeaconItem();
        ShapedRecipe recipe = new ShapedRecipe(beaconKey, reviveBeacon);

        // Recipe Pattern: 4 Nether Stars (Corners), 2 Netherite Ingots (Top/Bottom), 1 Beacon (Center), 2 Redstone Blocks (Sides)
        recipe.shape("NBN", "RCR", "NBN");
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('B', Material.NETHERITE_INGOT);
        recipe.setIngredient('C', Material.BEACON);
        recipe.setIngredient('R', Material.REDSTONE_BLOCK);

        Bukkit.addRecipe(recipe);
    }

    public ItemStack getReviveBeaconItem() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("beacon-item-name", "&c&lRevive Beacon")));
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Hold this item and type /revive <Player>",
                    ChatColor.RED + "Brings back an eliminated teammate!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (item != null && item.getType() == Material.BEACON && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName() && meta.getDisplayName().contains("Revive Beacon")) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.YELLOW + "💡 Usage: Hold the beacon and type " + ChatColor.GREEN + "/revive <PlayerName>");
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("giverevivebeacon")) {
            if (sender instanceof Player p) {
                p.getInventory().addItem(getReviveBeaconItem());
                p.sendMessage(ChatColor.GREEN + "✅ Given 1x Revive Beacon!");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("revive")) {
            if (!(sender instanceof Player player)) return true;

            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "Usage: /revive <PlayerName>");
                return true;
            }

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            boolean isOp = player.isOp();

            if (!isOp) {
                if (mainHand.getType() != Material.BEACON || !mainHand.hasItemMeta() || !mainHand.getItemMeta().getDisplayName().contains("Revive Beacon")) {
                    player.sendMessage(ChatColor.RED + "❌ You must hold a Revive Beacon to revive someone!");
                    return true;
                }
            }

            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);

            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player " + targetName + " is not online or found!");
                return true;
            }

            double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            if (maxHealth > 0) {
                player.sendMessage(ChatColor.YELLOW + target.getName() + " is already alive!");
                return true;
            }

            int startHearts = getConfig().getInt("revive-starting-hearts", 2);
            target.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(startHearts * 2.0);
            target.setHealth(startHearts * 2.0);

            if (!isOp) {
                mainHand.setAmount(mainHand.getAmount() - 1);
            }

            if (getConfig().getBoolean("broadcast-revive-message", true)) {
                Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "✨ " + player.getName() + " has REVIVED " + target.getName() + " back to the SMP!");
            }

            player.sendMessage(ChatColor.GREEN + "✅ Successfully revived " + target.getName() + "!");
            return true;
        }

        return true;
    }
}
