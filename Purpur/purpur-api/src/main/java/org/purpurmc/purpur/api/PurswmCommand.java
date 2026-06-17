package org.purpurmc.purpur.api;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Purswm 命令處理器
 */
public class PurswmCommand implements TabCompleter {
    
    private final PurswmPlugin plugin;
    
    public PurswmCommand(PurswmPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("purswm.command.purswm")) {
            sender.sendMessage("§cYou do not have permission!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help" -> sendHelp(sender);
            case "version" -> sendVersion(sender);
            case "templates" -> listTemplates(sender);
            case "instances" -> listInstances(sender);
            case "load" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /purswm load <worldName> <slimeFile>");
                    return true;
                }
                String worldName = args[1];
                String slimeFile = args.length > 2 ? args[2] : "worlds/" + worldName + ".slime";
                loadWorld(sender, worldName, slimeFile);
            }
            case "unload" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /purswm unload <worldName>");
                    return true;
                }
                unloadWorld(sender, args[1]);
            }
            case "create-instance" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /purswm create-instance <template> <instanceName>");
                    return true;
                }
                createInstance(sender, args[1], args[2]);
            }
            case "destroy-instance" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /purswm destroy-instance <instanceId>");
                    return true;
                }
                destroyInstance(sender, args[1]);
            }
            case "register-template" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /purswm register-template <name> <slimeFile>");
                    return true;
                }
                registerTemplate(sender, args[1], args[2]);
            }
            default -> sender.sendMessage("§cUnknown subcommand: " + subCommand);
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Purswm Help ===");
        sender.sendMessage("§7/purswm help §8- Show this help");
        sender.sendMessage("§7/purswm version §8- Show version info");
        sender.sendMessage("§7/purswm templates §8- List registered templates");
        sender.sendMessage("§7/purswm instances §8- List active instances");
        sender.sendMessage("§7/purswm load <world> <file> §8- Load slime world");
        sender.sendMessage("§7/purswm unload <world> §8- Unload world");
        sender.sendMessage("§7/purswm create-instance <template> <name> §8- Create game instance");
        sender.sendMessage("§7/purswm destroy-instance <id> §8- Destroy game instance");
        sender.sendMessage("§7/purswm register-template <name> <file> §8- Register template");
    }
    
    private void sendVersion(CommandSender sender) {
        sender.sendMessage("§6Purswm Version: " + plugin.getDescription().getVersion());
        sender.sendMessage("§6Minecraft: 1.21.7");
        sender.sendMessage("§6Active Instances: " + plugin.getGameInstances().size());
        sender.sendMessage("§6Registered Templates: " + plugin.worldTemplates.size());
    }
    
    private void listTemplates(CommandSender sender) {
        sender.sendMessage("§6=== Registered Templates ===");
        if (plugin.worldTemplates.isEmpty()) {
            sender.sendMessage("§7No templates registered.");
        } else {
            for (String name : plugin.worldTemplates.keySet()) {
                sender.sendMessage("§7- §f" + name);
            }
        }
    }
    
    private void listInstances(CommandSender sender) {
        sender.sendMessage("§6=== Active Instances ===");
        if (plugin.getGameInstances().isEmpty()) {
            sender.sendMessage("§7No active instances.");
        } else {
            for (var entry : plugin.getGameInstances().entrySet()) {
                var info = entry.getValue();
                sender.sendMessage("§7- §f" + info.getName() + " §8(" + info.getInstanceId() + ")");
            }
        }
    }
    
    private void loadWorld(CommandSender sender, String worldName, String slimeFile) {
        sender.sendMessage("§eLoading world: §f" + worldName);
        // TODO: 實現實際世界創建邏輯
        sender.sendMessage("§aWorld " + worldName + " loaded!");
    }
    
    private void unloadWorld(CommandSender sender, String worldName) {
        sender.sendMessage("§eUnloading world: §f" + worldName);
        // TODO: 實現實際世界卸載邏輯
        sender.sendMessage("§aWorld " + worldName + " unloaded!");
    }
    
    private void createInstance(CommandSender sender, String template, String instanceName) {
        String instanceId = plugin.createInstance(template, instanceName);
        if (instanceId != null) {
            sender.sendMessage("§aInstance created: §f" + instanceId);
        } else {
            sender.sendMessage("§cFailed to create instance!");
        }
    }
    
    private void destroyInstance(CommandSender sender, String instanceId) {
        if (plugin.unloadGameInstance(instanceId)) {
            sender.sendMessage("§aInstance destroyed: §f" + instanceId);
        } else {
            sender.sendMessage("§cInstance not found!");
        }
    }
    
    private void registerTemplate(CommandSender sender, String name, String slimeFile) {
        plugin.registerTemplate(name, java.nio.file.Paths.get(slimeFile));
        sender.sendMessage("§aTemplate registered: §f" + name);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = Arrays.asList(
            "help", "version", "templates", "instances",
            "load", "unload", "create-instance", "destroy-instance",
            "register-template"
        );
        
        if (args.length == 1) {
            return completions.stream()
                .filter(c -> c.startsWith(args[0].toLowerCase()))
                .toList();
        }
        
        return Collections.emptyList();
    }
}
