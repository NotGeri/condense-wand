package dev.geri.condensewand;

import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public final class CondenseWand extends JavaPlugin implements Listener, TabExecutor {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        // Register command & tab completion
        getCommand("condensewand").setExecutor(this);
        getCommand("condensewand").setTabCompleter(this);

        // Save & load configuration file
        this.getConfig().options().copyDefaults();
        this.saveDefaultConfig();
        config = this.getConfig();

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        // Verify that the block being clicked is a chest, make sure the player is sneaking as well as that the item being used to click is a wand
        if (e.getClickedBlock() == null) return;
        if (!e.getClickedBlock().getType().equals(Material.CHEST)) return;
        if (!player.isSneaking()) return;
        if (e.getItem() == null) return;
        if (!e.getItem().getType().equals(Material.valueOf(config.getString("item.material")))) return;
        if (e.getItem().getItemMeta() == null) return;
        if (e.getItem().getItemMeta().getPersistentDataContainer().getKeys().size() == 0) return;
        if (!e.getItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this, "wand-data"), PersistentDataType.STRING).equalsIgnoreCase("wand"))
            return;

        e.setCancelled(true); // Cancel the interact event so the chest doesn't open

        // Get the chest and its inventory.
        Chest chest = (Chest) e.getClickedBlock().getWorld().getBlockAt(e.getClickedBlock().getLocation()).getState();
        Inventory inventory = chest.getInventory();

        // Get the delete and convert values from the config file
        Set<String> convert = config.getConfigurationSection("settings.convert").getKeys(false);
        List<String> delete = config.getStringList("settings.delete");

        // Loop through the inventory's items
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i); // Store the current item in a variable

            if (item == null) {
                continue;
            } // If the item doesn't exist (slot is empty), ignore

            // Check if the item is something that should be deleted
            if (delete.contains(item.getType().name())) {

                for (String configuredMaterial : delete) { // Loop through all the items that should be deleted and check if the current item is one
                    if (item.getType().name().equalsIgnoreCase(configuredMaterial)) {
                        inventory.remove(item); // If it is, remove it from the inventory
                        playEffects(chest); // Summon the particles and play the sound
                    }
                }

            }

            // Check if the item is something that should be converted
            if (convert.contains(item.getType().name())) {

                for (String configuredMaterial : convert) { // Loop through all the items that should be converted and check if the current item is one

                    if (item.getType().name().equalsIgnoreCase(configuredMaterial)) {

                        int have = item.getAmount(); // Store the amount of item in the chest
                        int needed = config.getInt("settings.convert." + configuredMaterial + ".required"); // Get and store the amount of needed items from the config
                        int total = have / needed; // Divide the two values
                        int remainder = have % needed; // Get the remainder of the division

                        if (total >= 1) { // Check if the total is 1 or above

                            // If so, create a new item which is the item we want to convert into
                            ItemStack newItem = new ItemStack(Material.valueOf(config.getString("settings.convert." + configuredMaterial + ".to")));
                            newItem.setAmount(total * config.getInt("settings.convert." + configuredMaterial + ".amount")); // Get how many of the new items there should be

                            // Set the new item instead of the old one
                            inventory.setItem(i, newItem);
                            playEffects(chest); // Summon the particles and play the sound

                            // Check if there is any remainder left
                            if (remainder > 0) {
                                ItemStack remainderItem = new ItemStack(item.getType()); // If so, create a new item from the old item and set it as the remainder
                                remainderItem.setAmount(remainder);
                                HashMap<Integer, ItemStack> hashMap = inventory.addItem(remainderItem); // Add it to the chest, store the return value in a HashMap

                                // If the HashMap isn't empty, the item couldn't be added, try to add it to the player's inventory instead
                                if (!hashMap.isEmpty()) {
                                    HashMap<Integer, ItemStack> hashmap2 = player.getInventory().addItem(remainderItem);

                                    // If the new HashMap isn't empty, the item couldn't be added to the player's inventory, drop it on the ground at the player instead
                                    if (!hashmap2.isEmpty()) {
                                        player.getWorld().dropItem(player.getLocation().add(0, 1, 0), remainderItem);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void playEffects(Chest chest) {
        // Summon the particles from the configuration file if enabled
        if (config.getBoolean("settings.particle.enabled")) {
            chest.getWorld().spawnParticle(Particle.valueOf(config.getString("settings.particle.name")), chest.getLocation().add(0.5, 1, 0.5), 1, 0.1, 0.1, 0.1, 0.1);
        }

        // Play the sound from the configuration file if enabled
        if (config.getBoolean("settings.sound.enabled")) {
            chest.getWorld().playSound(chest.getLocation(), Sound.valueOf(config.getString("settings.sound.name")), config.getInt("settings.sound.volume"), config.getInt("settings.sound.volume"));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player, if not send them a message
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("messages.console"));
            return true;
        }

        Player player = (Player) sender; // Store the player

        if (args.length == 0) { // Check if there aren't any args - If player has permission, give wand, if not, send error message
            if (!player.hasPermission("condensewand.give")) {
                player.sendMessage(getMessage("messages.permission"));
                return true;
            }
            giveWand(player);
            player.sendMessage(getMessage("messages.success"));

        } else {

            /* Switch through the first argument.
            "reload" - if they have permission, reload the configuration values from the file
            "give" - if they have permission, give the player specified in the 2nd argument
            if there isn't any or the specified player isn't online, send an error message.*/
            switch (args[0]) {
                case "reload": {
                    if (!player.hasPermission("condensewand.reload")) {
                        player.sendMessage(getMessage("messages.permission"));
                        return true;
                    }
                    reloadConfig();
                    config = this.getConfig();
                    player.sendMessage(getMessage("messages.reload"));
                }
                break;

                case "give": {
                    if (args.length > 1 && Bukkit.getPlayer(args[1]) != null) {
                        Player otherplayer = Bukkit.getPlayer(args[1]);
                        giveWand(otherplayer);
                        player.sendMessage(getMessage("messages.success-other").replaceAll("%player%", otherplayer.getName()));
                    } else {
                        player.sendMessage(getMessage("messages.player").replaceAll("%player%", (args.length < 2) ? "" : args[1])); // (Check if there's a 2nd arg, if not just return "")
                        return true;
                    }
                }
                break;

                default: {
                    if (!player.hasPermission("condensewand.give")) {
                        player.sendMessage(getMessage("messages.permission"));
                        return true;
                    }
                    giveWand(player);
                    player.sendMessage(getMessage("messages.success"));

                }
                break;
            }

        }
        return true;
    }

    public void giveWand(Player player) {
        // Create a new item
        ItemStack item = new ItemStack(Material.valueOf(config.getString("item.material")));

        // Check if glowing is enabled, apply enchant
        if (config.getBoolean("item.glowing")) {
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        // Get item meta, make it unbreakable, apply item flags to hide enchants/unbreakable, name the item
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.setDisplayName(getMessage("item.name" + ""));

        // Set the custom data to make sure the item is an actual wand
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "wand-data"), PersistentDataType.STRING, "wand");

        // Apply the lore
        ArrayList<String> lore = new ArrayList<>();
        for (String loreLine : config.getStringList("item.lore")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
        }
        meta.setLore(lore);

        // Set the meta
        item.setItemMeta(meta);

        // Give the player the item - if their inventory is full, drop it on the ground
        HashMap<Integer, ItemStack> hashMap = player.getInventory().addItem(item);
        if (!hashMap.isEmpty()) {
            player.getWorld().dropItem(player.getLocation().add(0, 1, 0), item);
        }

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<String>();
        List<String> arguments = new ArrayList<String>();
        // Check if the player has permission for reload or give to other, add tab completion if so
        if (sender.hasPermission("condensewand.reload")) arguments.add("reload");
        if (sender.hasPermission("condensewand.give.other")) arguments.add("give");
        if (args.length == 1) {
            for (String a : arguments) {
                if (a.toLowerCase().startsWith(args[0].toLowerCase())) result.add(a);
            }
            return result;
        }
        return null;
    }

    // Util method to colour messages from config
    public String getMessage(String messageKey) {
        String message = config.getString(messageKey);
        if (message == null) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "[" + this.getDescription().getPrefix() + ChatColor.DARK_RED + "]" + ChatColor.RED + " There was an invalid message! " + ChatColor.DARK_GRAY + "(null)");
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

}