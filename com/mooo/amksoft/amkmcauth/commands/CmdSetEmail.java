package com.mooo.amksoft.amkmcauth.commands;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Config;
import com.mooo.amksoft.amkmcauth.Language;
import com.mooo.amksoft.amkmcauth.PConfManager;
import com.mooo.amksoft.amkmcauth.tools.SMTP;

public class CmdSetEmail implements CommandExecutor {

    //@SuppressWarnings("unused")
	private final AmkMcAuth plugin;
    
    public CmdSetEmail(AmkMcAuth instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(final CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("setemail")) {        	
        	int EmlCode = 0;

            final Player p = (Player) cs;
            final AuthPlayer ap = AuthPlayer.getAuthPlayer(p);

            //if (!cs.hasPermission("amkauth.setemail")) {
            //    AmkAUtils.dispNoPerms(cs);
            //    return true;
            //}
            if (args.length < 1) {
                cs.sendMessage(cmd.getDescription());
            	if(ap.getEmailAddress().contains("#")) {
            		cs.sendMessage("Email-address: " + ap.getEmailAddress().replace("#", "") + " not confirmed");
            	} else {
            		cs.sendMessage("Current email-address: " + ap.getEmailAddress().replace("#", ""));                    		
            	}
                return false;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
                return true;
            }

        	String EmailAddress = args[0]; // No Space Support

        	if (EmailAddress.contains("-confirm")) {
        		// SetEmailAddress Confirmation, player has received confirmation email.
        		// if Confirmation is correct then kill the SetEmail reminder.
        		// At this time Player still has unusable EmailAddress starting with '#' 
        		final String CurrEml = ap.getEmailAddress().replace("#", "");

        		for(int i=1 ; i<CurrEml.length() && i<15 ; i++){
            		EmlCode = EmlCode + CurrEml.codePointAt(i);
            	}
            	if(EmailAddress.contentEquals(Integer.toString(EmlCode) + "-confirm")) {
            		ap.setEmailAddress(CurrEml); // Confirmation correct, Set-EmailAddress 
            		
        			BukkitTask reminder = ap.getCurrentReminderTask();
        			if (reminder != null) reminder.cancel(); // kill the SetEmail reminder
    				cs.sendMessage(ChatColor.RED + String.format(Language.REGISTERED_SUCCESSFULLY.toString(),CurrEml));
        			
        			// Add the Players email address to the EmailAddress list
    				PConfManager.addPlayerToEm(CurrEml); // "player@nowhere.com"
        			//new BukkitRunnable() {
        			//	@Override
        			//	public void run() {        		    	
        			//		PConfManager.addPlayerToEm(CurrEml); // "player@nowhere.com"
        			//	}
        			//}.runTaskAsynchronously(AmkMcAuth.getInstance());
            	}            	
        		
        		// if NOT correct, Clear out then current Email-Address?? (sets next login reminder)
        		
            	// EmailAddress contains "-confirm", meaning ONLY Confirmation-Checking, so Return!!!
        		return true;
        	}
        	
        	// No "-confirm emailaddress", player is setting his Email-Address, Check Address,
        	// Save it in the player Profile and send Confirmation-email to check trust-worthy

			for (String disallowed : Config.disallowedEmlAdresses) {
				if (!EmailAddress.toLowerCase().contains(disallowed.toLowerCase())) continue;
				cs.sendMessage(ChatColor.RED + Language.DISALLOWED_EMLADDRESS.toString());
				return true;
			}

			if (!EmailAddress.contains("@")) {
                cs.sendMessage(ChatColor.RED + String.format(Language.PLAYER_INVALID_EMAILADDRESS.toString() + ".", args[1], EmailAddress));
                return true;
            }

			// reduce the Players email address from the EmailAddress list
			// If The EmailAddress is not found, nothing is reduced.
			PConfManager.removePlayerFromEmSet(ap.getEmailAddress());
			//new BukkitRunnable() {
			//	@Override
			//	public void run() {        		    	
			//		PConfManager.removePlayerFromEmSet(ap.getEmailAddress());
			//	}
			//}.runTaskAsynchronously(AmkMcAuth.getInstance());
			
    		ap.setEmailAddress("#" + EmailAddress); 

    		// ----------------------------------------------------------------------------------------------
        	//new BukkitRunnable() {
        	new Thread(new Runnable() {
        		@Override
        		public void run() { 
        			
    				int EmlCode = 0;
        			String Player = ap.getUserName();
    				Logger log = AmkMcAuth.getInstance().getLogger();
    				String EmailAddress = ap.getEmailAddress().replace("#", "");
    				
    	        	for(int i=1 ; i<EmailAddress.length() && i<15 ; i++){
    	        		EmlCode = EmlCode + EmailAddress.codePointAt(i);
    	        	}

        			int PlayerCount = PConfManager.countPlayersFromEm(EmailAddress); // "email@address.org","Userid"

        			if(Config.maxUsersPerEmaddress>0) {
        				log.info("Login Email-Address " + EmailAddress + " used by " + PlayerCount + " player(s) ");
       					log.info("Configured maximum allowed players from one Email-Address is: " + Config.maxUsersPerEmaddress);
       					if (PlayerCount >= Config.maxUsersPerEmaddress) {
       						cs.sendMessage(ChatColor.RED + Language.PLAYER_EXCEEDS_MAXREGS_EM.toString());
       						return; // NOT !!! sending New Password to Email Address !!!
       					}
       				}
    		    
        			// SetUp Email Session
        			SMTP.Email email = SMTP.createEmptyEmail();
        			//email.add("Content-Type", "text/html"); //Headers for the email (useful for html) you do not have to set them
        			//email.add("Content-Type", "text/plain");  //Default Header ("text/plain") for the email.you do not have to set
        			email.from(Config.emlFromNicer, Config.emlFromEmail); //The sender of the email.
        			email.to(Player, EmailAddress); //The recipient of the email.
        			email.subject(Config.emailsubject); //Subject of the email
        			email.body(String.format(Config.confirmbodytxt, "/setemail " + Integer.toString(EmlCode) + "-confirm"));
        			// All the email stuff here
        			//SMTP.sendEmail(smtpServer, email, password, mail, debug);    			        
        			SMTP.sendEmail(Config.emlSmtpServr,
       								Config.emlLoginName, 
       								Config.emlLoginPswd, 
       								email, false);
        			cs.sendMessage(ChatColor.BLUE + Language.EMAIL_SET_AND_CONFIRMSEND.toString());
        		}
			}).start();
			// ----------------------------------------------------------------------------------------------
			
            // NEW !!  ASK THE PLAYER to set the EMAIL-Address !!!!
			BukkitTask reminder = ap.getCurrentReminderTask();
			if (reminder != null) reminder.cancel();
			ap.createSetEmailReminder(this.plugin);
			// This reminder-task is killed when player enters the correct confirmation code

    		return true;
        }
        return false;
    }

}
