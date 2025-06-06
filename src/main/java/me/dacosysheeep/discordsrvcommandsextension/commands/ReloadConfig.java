package me.dacosysheeep.discordsrvcommandsextension.commands;

import me.dacosysheeep.discordsrvcommandsextension.DiscordsrvCommandsExtension;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class ReloadConfig implements CommandExecutor {
    private DiscordsrvCommandsExtension plugin;
    public ReloadConfig(DiscordsrvCommandsExtension plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("DiscordsrvCommandsExtension.reload")) {
            plugin.reloadConfig();
            FileConfiguration config = plugin.getConfig();
            plugin.serverManagerRoles = config.getStringList("server-manager-roles");
            plugin.whitelistManagerRoles = config.getStringList("whitelist-manager-roles");
            plugin.whitelistViewerRoles = config.getStringList("whitelist-viewer-roles");
            sender.sendMessage(ChatColor.GREEN + "The configuration has been reloaded.");
        } else {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use that command.");
        }
        return true;
    }
}
