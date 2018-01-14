package com.mooo.amksoft.amkmcauth.commands;

import java.util.logging.Logger;

import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Config;
import com.mooo.amksoft.amkmcauth.Language;
import com.mooo.amksoft.amkmcauth.PConfManager;
import com.mooo.amksoft.amkmcauth.tools.SMTP;
import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.AmkAUtils;

public class CmdRegister implements CommandExecutor {

    private final AmkMcAuth plugin;

    public CmdRegister(AmkMcAuth instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(final CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("register")) {
            if (!cs.hasPermission("amkauth.register")) {
                AmkAUtils.dispNoPerms(cs);
                return true;
            }
            if (args.length < 1) {
                cs.sendMessage(cmd.getDescription());
                return false;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
                return true;
            }
            final Player p = (Player) cs;
            final AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (ap.isLoggedIn() || ap.isRegistered()) {
                cs.sendMessage(ChatColor.RED + Language.ALREADY_REGISTERED.toString());
                return true;
            }

            int PlayerCount = 0;
            if(Config.maxUsersPerIpaddress>0) {
            	//PlayerCount = PConfManager.countPlayersFromIp(ap.getCurrentIPAddress()); // "192.168.1.7","Userid"
            	PlayerCount = ap.getPlayerIpCount(); // Should be around by now (asynchronously), otherwise 0
            	//This getPlayerIpCount() is set in: 
            	this.plugin.getLogger().info("Login Ip-Address " + ap.getCurrentIPAddress() + " used by " + PlayerCount + " player(s) ");
            	this.plugin.getLogger().info("Configured maximum allowed players from one Ip-Address is: " + Config.maxUsersPerIpaddress);
            	if (PlayerCount >= Config.maxUsersPerIpaddress) {
            		cs.sendMessage(ChatColor.RED + Language.PLAYER_EXCEEDS_MAXREGS_IP.toString());
            		return true;
            	}
        	}

            boolean RegisteredOk = false;
    		if(Config.registrationType.equalsIgnoreCase("email")) {
    			final String EmailAddress = args[0]; // no space support!!

    			for (String disallowed : Config.disallowedEmlAdresses) {
    				if (!EmailAddress.toLowerCase().contains(disallowed.toLowerCase())) continue;
    				cs.sendMessage(ChatColor.RED + Language.DISALLOWED_EMLADDRESS.toString());
    				return true;
    			}

        		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        		final String Password = RandomStringUtils.random( 7, characters );
        		
                if (!EmailAddress.contains("@")) {
                    cs.sendMessage(ChatColor.RED + String.format(Language.PLAYER_INVALID_EMAILADDRESS.toString() + ".", args[1], EmailAddress));
                }
                else
                	{
                	if (ap.setEmailAddress(EmailAddress)) {
                		if (!ap.setPassword(Password, Config.passwordHashType)) {
                			this.plugin.getLogger().info(p.getName() + " !!!! Could not set new PassWord !!!!");
                		} else {
                			RegisteredOk = true;        				
                		}
                	}
                }
                if(RegisteredOk) {
                	//new BukkitRunnable() {
                	//    @Override
                	//    public void run() {
                	//    	// Do Something..                        	
                	//    }
                	//}.runTaskAsynchronously(AmkMcAuth.getInstance());            	
        			
                	//new BukkitRunnable() {
                	new Thread(new Runnable() {
                		@Override
                		public void run() {        		    	
                			String Player = ap.getUserName();
            				Logger log = AmkMcAuth.getInstance().getLogger();
            				
                			int PlayerCount = PConfManager.countPlayersFromEm(EmailAddress); // "email@address.org","Userid"

                			if(Config.maxUsersPerEmaddress>0) {
                				log.info("Login Email-Address " + ap.getEmailAddress() + " used by " + PlayerCount + " player(s) ");
               					log.info("Configured maximum allowed players from one Email-Address is: " + Config.maxUsersPerEmaddress);
               					if (PlayerCount >= Config.maxUsersPerEmaddress) {
               						cs.sendMessage(ChatColor.RED + Language.PLAYER_EXCEEDS_MAXREGS_EM.toString());
               						return; // NOT !!! sending New Password to Email Address !!!
               					}
               				}
    	    		    
                        	log.info(p.getName() + " Email-Address! " + Language.HAS_REGISTERED);

                			// SetUp Email Session
                			SMTP.Email email = SMTP.createEmptyEmail();
                			//email.add("Content-Type", "text/html"); //Headers for the email (useful for html) you do not have to set them
                			//email.add("Content-Type", "text/plain");  //Default Header ("text/plain") for the email.you do not have to set
                			email.from(Config.emlFromNicer, Config.emlFromEmail); //The sender of the email.
                			email.to(Player, EmailAddress); //The recipient of the email.
                			email.subject(Config.emailsubject); //Subject of the email
                			email.body(String.format(Config.emailbodytxt, Player, Password));
                			// All the email stuff here
                			//SMTP.sendEmail(smtpServer, email, password, mail, debug);    			        
                			SMTP.sendEmail(Config.emlSmtpServr,
               								Config.emlLoginName, 
               								Config.emlLoginPswd, 
               								email, false);
                				
               				//SMTP.sendEmail(Config.emlSmtpServr, 
               				//  		Config.emlLoginName, 
               				//  		Config.emlLoginPswd, 
                			//  		Config.emlFromNicer, 
                			//  		Player,	EmailAddress, 
                			//  		Config.emailsubject, 
                			//  		String.format(Config.emailbodytxt,Player,Password),
                			//  		false);

                			cs.sendMessage(ChatColor.BLUE + Language.EMAIL_SET_AND_REGISTERED.toString());
                		}
    				}).start();
    			} else cs.sendMessage(ChatColor.RED + String.format(Language.COULD_NOT_REGISTER.toString(),"Email-Address"));
    		} else {
    			String rawPassword = args[0]; // no space support!!
    			for (String disallowed : Config.disallowedPasswords) {
    				if (!rawPassword.equalsIgnoreCase(disallowed)) continue;
    				cs.sendMessage(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
    				return true;
    			}
    			if (ap.setPassword(rawPassword, Config.passwordHashType)) {
    				RegisteredOk = true;
    				this.plugin.getLogger().info(p.getName() + " !!!! " + Language.HAS_REGISTERED);
    				cs.sendMessage(ChatColor.BLUE + Language.PASSWORD_SET_AND_REGISTERED.toString());
    			} else cs.sendMessage(ChatColor.RED + Language.PASSWORD_COULD_NOT_BE_SET.toString());
    		}
    		if (RegisteredOk) {    		
    			ap.setUserName(p.getName()); 
    			// rest Join-TimeStamp, so Kill-Timeout is more accurate.
    			ap.setLastJoinTimestamp(System.currentTimeMillis());    			
				PConfManager.addAllPlayer(p.getName());
				new BukkitRunnable() {
					@Override
					public void run() {        		    	
						PConfManager.addPlayerToIp(ap.getCurrentIPAddress()); // "192.168.1.7"
					}
				}.runTaskAsynchronously(AmkMcAuth.getInstance());
        
				BukkitTask reminder = ap.getCurrentReminderTask();
				if (reminder != null) reminder.cancel();
				
				if(!Config.registrationType.equalsIgnoreCase("email"))
					ap.createLoginReminder(this.plugin);
				else
					ap.createLoginEmailReminder(this.plugin);
    		}
    		return true;
        }
        return false;
    }
}
