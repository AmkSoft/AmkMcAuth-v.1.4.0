package com.mooo.amksoft.amkmcauth.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Config;
import com.mooo.amksoft.amkmcauth.Hasher;
import com.mooo.amksoft.amkmcauth.Language;
import com.mooo.amksoft.amkmcauth.PConfManager;
import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.AmkAUtils;

import java.security.NoSuchAlgorithmException;

public class CmdLogin implements CommandExecutor {

    private final AmkMcAuth plugin;

    public CmdLogin(AmkMcAuth instance) {
        this.plugin = instance;
    }

    // Suppressing Console Commands. Is this working, i hope so :D ???
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {     
        if (event.getMessage().split(" ")[0].equalsIgnoreCase("/login")) {
        	event.getPlayer().getLocation().getWorld().setGameRuleValue("logAdminCommands", "false");
        }
    }    
    private void enableLogAdminCommand(CommandSender cs){
        if (cs instanceof Player) 
        	((Player) cs).getLocation().getWorld().setGameRuleValue("logAdminCommands", "true");
    }
    
    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("login")) {
            if (!cs.hasPermission("amkauth.login")) {
                AmkAUtils.dispNoPerms(cs);
                enableLogAdminCommand(cs);
                return true;
            }
            if (args.length < 1) {
                cs.sendMessage(cmd.getDescription());
                enableLogAdminCommand(cs);
                return false;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
                enableLogAdminCommand(cs);
                return true;
            }
            Player p = (Player) cs;
            final AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (ap.isLoggedIn()) {
                cs.sendMessage(ChatColor.RED + Language.ALREADY_LOGGED_IN.toString());
                enableLogAdminCommand(cs);
                return true;
            }
            String rawPassword = AmkAUtils.getFinalArg(args, 0); // support spaces
            for (String disallowed : Config.disallowedPasswords) {
                if (!rawPassword.equalsIgnoreCase(disallowed)) continue;
                cs.sendMessage(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
            }
            String hashType = (!ap.getHashType().equalsIgnoreCase(Config.passwordHashType)) ? ap.getHashType() : Config.passwordHashType;
            try {
                rawPassword = Hasher.encrypt(rawPassword, hashType);
            } catch (NoSuchAlgorithmException e) {
                cs.sendMessage(ChatColor.RED + Language.COULD_NOT_LOG_IN.toString());
                cs.sendMessage(ChatColor.RED + Language.ADMIN_SET_UP_INCORRECTLY.toString());
                cs.sendMessage(ChatColor.RED + Language.CONTACT_ADMIN.toString());
                enableLogAdminCommand(cs);
                return true;
            }
            String realPassword = ap.getPasswordHash();
            if (rawPassword.equals(realPassword)) {
                ap.login();
                this.plugin.getLogger().info(p.getName() + " " + Language.HAS_LOGGED_IN);
                cs.sendMessage(ChatColor.BLUE + Language.LOGGED_IN_SUCCESSFULLY.toString());
                if(!ap.getCurrentIPAddress().equals(ap.getLastIPAddress())) {
                	// Spawn off a asynchronously process to Update Ip-Address-Count the player is comming from 
        			new BukkitRunnable() {
        				@Override
        				public void run() {        		    	
        					PConfManager.removePlayerFromIp(ap.getLastIPAddress()); // "192.168.1.7"
        					PConfManager.addPlayerToIp(ap.getCurrentIPAddress()); // "192.168.1.7"
        				}
        			}.runTaskAsynchronously(AmkMcAuth.getInstance());
            	}
                // NEW !!  ASK THE PLAYER to set the EMAIL-Address !!!!
                // The '#' means, the player was unable to Confirm the Email-Address (could it be wrong?)
                if((ap.getEmailAddress().equals("") | ap.getEmailAddress().contains("#")) & Config.emailForceSet) {
    				ap.createSetEmailReminder(this.plugin);
            	}
            } else {
                this.plugin.getLogger().warning(p.getName() + " " + Language.USED_INCORRECT_PASSWORD);
                if(Config.KickOnPasswordFail) {
                	//p.kickPlayer(Language.INCORRECT_PASSWORD.toString());
                	//return false;
                	final Player pl=p;
                	Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
                		public void run() {
                			pl.kickPlayer(Language.INCORRECT_PASSWORD.toString());
                    	}
                	}, 10L);                		

                }
                cs.sendMessage(ChatColor.RED + Language.INCORRECT_PASSWORD.toString());
            }
            enableLogAdminCommand(cs);
            return true;
        }
        enableLogAdminCommand(cs);
        return false;
    }

}
