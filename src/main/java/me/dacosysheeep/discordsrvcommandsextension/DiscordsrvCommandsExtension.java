package me.dacosysheeep.discordsrvcommandsextension;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData;
import me.dacosysheeep.discordsrvcommandsextension.commands.ReloadConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

import static java.lang.String.valueOf;


@SuppressWarnings({"deprecation", "unused"})
public final class DiscordsrvCommandsExtension extends JavaPlugin implements Listener, SlashCommandProvider {
    FileConfiguration config = this.getConfig();
    public List<String> serverManagerRoles = config.getStringList("server-manager-roles");
    public List<String> whitelistManagerRoles = config.getStringList("whitelist-manager-roles");
    public List<String> whitelistViewerRoles = config.getStringList("whitelist-viewer-roles");
    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("dscext reload").setExecutor(new ReloadConfig(this));
        getLogger().info(this.getName()+" has started");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info(this.getName()+" has stopped");
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Arrays.asList(
                new PluginSlashCommand(this, new CommandData("whitelist", "Manage server whitelist")
                        .addSubcommands(new SubcommandData("add", "Add a player to the Minecraft server whitelist")
                                .addOption(OptionType.STRING, "username", "The minecraft username of the user to whitelist", true)
                                .addOption(OptionType.USER, "user", "Discord user to link minecraft account to", false))


                        .addSubcommands(new SubcommandData("list", "List players on the whitelist"))
                        .addSubcommands(new SubcommandData("remove", "Remove a player from the whitelist")
                                .addOption(OptionType.STRING, "username", "The minecraft username of the user to unwhitelist", true))
                        .setDefaultEnabled(false)
                ),
                new PluginSlashCommand(this, new CommandData("restart", "Restart the server")
                        .setDefaultEnabled(false)),
                new PluginSlashCommand(this, new CommandData("stop", "Stop the server")
                        .setDefaultEnabled(false)),
                new PluginSlashCommand(this, new CommandData("list", "Lists the online players")
                        .setDefaultEnabled(false))
        ));
    }

    @SlashCommand(path="whitelist/add", deferReply = true)
    public void whitelistAdd(SlashCommandEvent e) {
        String username = Objects.requireNonNull(e.getOption("username")).getAsString();
        getLogger().info(String.format("Whitelisting %1$s", username));

        List<Role> roles = e.getMember().getRoles();
        boolean serverManager = whitelistManagerRoles.isEmpty() && roles.stream().anyMatch(role -> serverManagerRoles.contains(valueOf(role.getIdLong())));
        boolean whitelistManager = roles.stream().anyMatch(role -> whitelistManagerRoles.contains(valueOf(role.getIdLong())));

        if (serverManager || whitelistManager) {
            if (getServer().getOfflinePlayer(username).isWhitelisted()) {
                e.getHook().sendMessage("This user is already whitelisted on the server").queue();
            } else {
                try {
                    Bukkit.getScheduler().runTask(this, () -> getServer().getOfflinePlayer(username).setWhitelisted(true));
                    if (e.getOption("user") != null) {
                        User discorduser = e.getOption("user").getAsUser();
                        String discordid = discorduser.getId();
                        getLogger().info(String.format("Linking %1$s to %2$s", username, discordid));
                        UUID uuid = Bukkit.getOfflinePlayer(username).getUniqueId();

                        DiscordSRV.getPlugin().getAccountLinkManager().link(discordid, uuid);
                        e.getHook().sendMessage(String.format("%1$s is now whitelisted and linked to <@%2$s>", username, discordid)).queue();
                        getLogger().info(String.format("%s just added %s to the whitelist and linked them to %s", e.getMember().getNickname(), username, discorduser.getEffectiveName()));
                    } else {
                        e.getHook().sendMessage(String.format("%1$s is now whitelisted", username)).queue();
                        getLogger().info(String.format("%s just added %s to the whitelist", e.getMember().getNickname(), username));
                    }
                } catch (Exception exception) {
                    getLogger().warning(String.format("An error occurred when adding %s to the whitelist:\n%s", username, exception));
                    e.getHook().sendMessage("An error occurred. Check server logs.").queue();
                }
            }
        } else {
            e.getHook().sendMessage("You don't have permission to do that.").queue();
        }

    }
    @SlashCommand(path="whitelist/remove", deferReply = true)
    public void whitelistRemove(SlashCommandEvent e) {
        List<Role> roles = e.getMember().getRoles();
        boolean serverManager = whitelistManagerRoles.isEmpty() && roles.stream().anyMatch(role -> serverManagerRoles.contains(valueOf(role.getIdLong())));
        boolean whitelistManager = roles.stream().anyMatch(role -> whitelistManagerRoles.contains(valueOf(role.getIdLong())));
        if (serverManager || whitelistManager) {
            String username = Objects.requireNonNull(e.getOption("username")).getAsString();
            Bukkit.getScheduler().runTask(this, () -> getServer().getOfflinePlayer(username).setWhitelisted(false));

            e.getHook().sendMessage(String.format("%1$s is no longer whitelisted", username)).queue();
        } else {
            e.getHook().sendMessage("You don't have permission to do that.").queue();
        }
    }
    @SlashCommand(path="whitelist/list", deferReply = true)
    public void whitelistList(SlashCommandEvent e) {
        List<Role> roles = e.getMember().getRoles();
        boolean serverManager = whitelistManagerRoles.isEmpty() && roles.stream().anyMatch(role -> serverManagerRoles.contains(valueOf(role.getIdLong())));
        boolean whitelistManager = roles.stream().anyMatch(role -> whitelistManagerRoles.contains(valueOf(role.getIdLong()))) || whitelistManagerRoles.contains("everyone");
        boolean whitelistViewer = roles.stream().anyMatch(role -> whitelistViewerRoles.contains(valueOf(role.getIdLong()))) || whitelistViewerRoles.contains("everyone");
        if (serverManager || whitelistManager || whitelistViewer) {
            // Get the whitelist
            Set<OfflinePlayer> whitelist = Bukkit.getServer().getWhitelistedPlayers();
            if(whitelist.isEmpty()) {
                if (serverManager || whitelistManager) {
                    e.getHook().sendMessage("The server whitelist is empty. To add users, try using the ```/whitelist add <username>``` command.").queue();
                } else {
                    e.getHook().sendMessage("The server whitelist is empty. Wait for a whitelist manager to add someone.").queue();
                }
            } else {

                // Create a StringBuilder to build the list of usernames
                StringBuilder builder = new StringBuilder("Server whitelist:\n```\n");

                // Loop through the whitelist and add each username to the builder

                for (OfflinePlayer player : whitelist) {
                    builder.append(player.getName()).append("\n");
                }

                // Close the code block
                builder.append("```");

                // Send the list of whitelisted usernames
                e.getHook().sendMessage(builder.toString()).queue();
            }
        } else {
            e.getHook().sendMessage("You don't have permission to do that.").queue();
        }
    }

    @SlashCommand(path="restart", deferReply = true)
    public void serverRestart(SlashCommandEvent e) {
        //Restart the server
        List<Role> roles = e.getMember().getRoles();
        boolean serverManager = roles.stream().anyMatch(role -> serverManagerRoles.contains(valueOf(role.getIdLong())));
        if (serverManager) {
            e.getHook().sendMessage("Restarting the server.").queue();
            Bukkit.getScheduler().runTask(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop"));
        } else {
            e.getHook().sendMessage("You don't have permission to do that.").queue();
        }
    }
    @SlashCommand(path="stop", deferReply = true)
    public void serverStop(SlashCommandEvent e) {
        List<Role> roles = e.getMember().getRoles();
        boolean serverManager = roles.stream().anyMatch(role -> serverManagerRoles.contains(valueOf(role.getIdLong())));
        if (serverManager) {
            e.getHook().sendMessage("Stopping the server.").queue();
            //getServer().dispatchCommand(getServer().getConsoleSender(), "stop");
            Bukkit.getScheduler().runTask(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop"));
        } else {
            e.getHook().sendMessage("You don't have permission to do that.").queue();
        }
    }

    @SlashCommand(path="list", deferReply = true, deferEphemeral = true)
    public void listPlayers(SlashCommandEvent e) {
        Collection<? extends Player> onlinePlayers=getServer().getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            e.getHook().sendMessage("No one is playing at the moment.").queue();
        } else {
            StringBuilder onlinePlayerList=new StringBuilder();
            onlinePlayerList.append(String.format("There are %d player(s) online:\n```\n", onlinePlayers.size()));
            for (Player player : onlinePlayers) {
                onlinePlayerList.append(player.getName()+"\n");
            }
            onlinePlayerList.append("```");
            e.getHook().sendMessage(onlinePlayerList.toString()).queue();
        }
    }

}
