package com.mooo.amksoft.amkmcauth.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Config;
import com.mooo.amksoft.amkmcauth.Language;
import com.mooo.amksoft.amkmcauth.PConfManager;
import com.mooo.amksoft.amkmcauth.tools.MySQL;
import com.mooo.amksoft.amkmcauth.tools.SMTP;
import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.AmkAUtils;

public class CmdAmkAuth implements CommandExecutor {

    private final AmkMcAuth plugin;
    public static File dataFolder; // DirectoryNaam of  DataDirectory
    
    private boolean DebugEmail = false; 

    public CmdAmkAuth(AmkMcAuth instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(final CommandSender cs, Command cmd, String label, String[] args) {
        if ( !cmd.getName().equalsIgnoreCase("")) {
            if (!cs.hasPermission("amkauth.")) {
                AmkAUtils.dispNoPerms(cs);
                return true;
            }
            if (args.length < 1) {
                cs.sendMessage(cmd.getDescription());
                return false;
            }
            if (cs instanceof Player) {
                AuthPlayer ap = AuthPlayer.getAuthPlayer(((Player) cs).getUniqueId());
                if (!ap.isLoggedIn()) {
                    cs.sendMessage(ChatColor.RED + Language.YOU_MUST_LOGIN.toString());
                    return true;
                }
            }
            String subcommand = args[0].toLowerCase();
            switch (subcommand) {
                case "help": {
                    cs.sendMessage(ChatColor.BLUE + Language.ADMIN_HELP.toString());
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " changepassword [player] [newpassword]" + ChatColor.BLUE + " - " + Language.HELP_CHANGEPASSWORD);
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " login [player]" + ChatColor.BLUE + " - " + Language.HELP_LOGIN);
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " logout [player]" + ChatColor.BLUE + " - " + Language.HELP_LOGOUT);
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " register [player] [password]" + ChatColor.BLUE + " - " + Language.HELP_REGISTER);
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " setemail [player] [emailaddress]" + ChatColor.BLUE + " - " + Language.HELP_REGEMAIL);
            		cs.sendMessage(ChatColor.GRAY + "  /" + label + " testemail [player]" + ChatColor.BLUE + " - Test Email-Setup, send test-mail to [Player]");
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " unregister [player]" + ChatColor.BLUE + " - " + Language.HELP_UNREGISTER);
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " getuuid [player]" + ChatColor.BLUE + " - " + Language.HELP_GETUUID);
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " nlplist " + ChatColor.BLUE + " - " + Language.HELP_NLPLIST);
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " nlpadd [player]" + ChatColor.BLUE + " - " + Language.HELP_NLPADD);
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " nlprem [player]" + ChatColor.BLUE + " - " + Language.HELP_NLPREM);
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " ipcount" + ChatColor.BLUE + " - Show Login Ip-Address count");
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " emcount" + ChatColor.BLUE + " - Show Registered Email-Address count");
                    //cs.sendMessage(ChatColor.GRAY + "  /" + label + " iplist" + ChatColor.BLUE + " - (debug) show unique Login Ip-Addresses + playercount");
                	if (!Config.MySqlDbHost.equals("")) {                     
                        cs.sendMessage(ChatColor.GRAY + "  /" + label + " resetdbcon" + ChatColor.BLUE + " - Reset MySQL Connection");
                        //cs.sendMessage(ChatColor.GRAY + "  /" + label + " reloadusers" + ChatColor.BLUE + " - (debug) Load Player profiles into MySQL Updater (may cause lag)");
            		} else {
                        //cs.sendMessage(ChatColor.GRAY + "  /" + label + " profilesave" + ChatColor.BLUE + " - Save all Player Profiles to Disk (may cause lag)");
                        //cs.sendMessage(ChatColor.GRAY + "  /" + label + " reloadusers" + ChatColor.BLUE + " - (debug) Load Player profiles into system (may cause lag)");
                	}   
            		//cs.sendMessage(ChatColor.GRAY + "  /" + label + " listusers" + ChatColor.BLUE + " - (debug) Show Player list (ActiveProfile)");
            		cs.sendMessage(ChatColor.GRAY + "  /" + label + " showstats" + ChatColor.BLUE + " - Show AmkMcAuth Player-base Statistics ");
            		cs.sendMessage(ChatColor.GRAY + "  /" + label + " chkdevmsg" + ChatColor.BLUE + " - Show/check info/version from Plugin ");
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " reload" + ChatColor.BLUE + " - " + Language.HELP_RELOAD);   
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " debug" + ChatColor.BLUE + " - Debug commands, use with caution!!!");   
                    cs.sendMessage(ChatColor.GRAY + "  /" + label + " help" + ChatColor.BLUE + " - " + Language.HELP_HELP);
                    break;
            	}
                case "changepassword": {
                    //if (cs instanceof Player) ((Player) cs).getLocation().getWorld().setGameRuleValue("logAdminCommands", "true");
                    if (args.length < 3) {
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                        return true;
                    }
                    AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                    
                    if (ap == null) {
                        cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                        return true;
                    }
                    if (!ap.isRegistered()) {
                        cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_REGISTERED.toString());
                        return true;
                    }
                    if (ap.setPassword(args[2], Config.passwordHashType)) {
                        cs.sendMessage(ChatColor.BLUE + Language.PASSWORD_CHANGED.toString());
                        ap.setLastJoinTimestamp(System.currentTimeMillis()); // Force ProfileSave
                    }
                    else cs.sendMessage(ChatColor.RED + Language.PASSWORD_COULD_NOT_BE_CHANGED.toString());

                    break;
                }
                case "login": {
                    //if (cs instanceof Player) ((Player) cs).getLocation().getWorld().setGameRuleValue("logAdminCommands", "true");
                    if (args.length < 2) {
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                        return true;
                    }
                    AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                    if (ap == null) {
                        cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                        return true;
                    }
                    Player p = ap.getPlayer();
                    if (p == null) {
                        cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_ONLINE.toString());
                        return true;
                    }
                    ap.login();
                    this.plugin.getLogger().info(p.getName() + " " + Language.HAS_LOGGED_IN);
                    cs.sendMessage(ChatColor.BLUE + Language.PLAYER_LOGGED_IN.toString());
                    break;
                }
                case "logout": {
                    if (args.length < 2) {
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                        return true;
                    }
                    AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                    if (ap == null) {
                        cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                        return true;
                    }
                    Player p = ap.getPlayer();
                    if (p == null) {
                        cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_ONLINE.toString());
                        return true;
                    }
                    if (!ap.isLoggedIn()) {
                        cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_LOGGED_IN.toString());
                        return true;
                    }
                    ap.logout(this.plugin);
                    cs.sendMessage(ChatColor.BLUE + Language.PLAYER_LOGGED_OUT.toString());
                    break;
                }
                case "register": {
                    //if (cs instanceof Player) ((Player) cs).getLocation().getWorld().setGameRuleValue("logAdminCommands", "true");
                    if (args.length < 3) {
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                        return true;
                    }
                    final AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                    if (ap == null) {
                        cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                        return true;
                    }
                    if (ap.isRegistered()) {
                        cs.sendMessage(ChatColor.RED + Language.PLAYER_ALREADY_REGISTERED.toString());
                        return true;
                    }
                    String rawPassword = args[2];
                    for (String disallowed : Config.disallowedPasswords) {
                        if (!rawPassword.equalsIgnoreCase(disallowed)) continue;
                        cs.sendMessage(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
                        return true;
                    }
                    final String name = AmkAUtils.forceGetName(ap.getUniqueId());
                    if (ap.setPassword(rawPassword, Config.passwordHashType)) {
                        //cs.sendMessage(ChatColor.BLUE + String.format(Language.REGISTERED_SUCCESSFULLY.toString(), ChatColor.GRAY + name + ChatColor.BLUE));
                        if(name!=args[1]) ap.setUserName(args[1]); //name not set?, set it!
                    	cs.sendMessage(ChatColor.BLUE + String.format(Language.REGISTERED_SUCCESSFULLY.toString(), ChatColor.GRAY + args[1] + ChatColor.BLUE));
    					PConfManager.addPlayerToIp(ap.getCurrentIPAddress()); // "192.168.1.7"
            			//new BukkitRunnable() {
            			//	@Override
            			//	public void run() {        		    	
            			//		PConfManager.addPlayerToIp(ap.getCurrentIPAddress()); // "192.168.1.7"
            			//	}
            			//}.runTaskAsynchronously(AmkMcAuth.getInstance());                    	
                        //PConfManager.addPlayerToIp(ap.getCurrentIPAddress());
                        PConfManager.addAllPlayer(args[1].toLowerCase());
                        ap.setLastJoinTimestamp(System.currentTimeMillis()); // Force ProfileSave
                    }
                    else
                        //cs.sendMessage(ChatColor.RED + String.format(Language.COULD_NOT_REGISTER.toString(), ChatColor.GRAY + name + ChatColor.RED));
                    	cs.sendMessage(ChatColor.RED + String.format(Language.COULD_NOT_REGISTER.toString(), ChatColor.GRAY + args[1] + ChatColor.RED));
                    break;
                }
                case "setemail": {
                    //if (cs instanceof Player) ((Player) cs).getLocation().getWorld().setGameRuleValue("logAdminCommands", "true");
                    if (args.length < 2) { // No parameters
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                        return true;
                    }
                    
                    final AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                    if (ap == null) { // check on player name 
                        cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                        return true;
                    }
                    if (!ap.isRegistered()) {
                        cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_REGISTERED.toString());
                        return true;
                    }

                    if (args.length < 3) { // No EmailAddres, just playername, give current emailaddress
                    	if(ap.getEmailAddress().contains("#")) {
                    		cs.sendMessage("Email-address: " + ap.getEmailAddress().replace("#", "") + " not confirmed");
                    	} else {
                    		cs.sendMessage("Current email-address: " + ap.getEmailAddress().replace("#", ""));                    		
                    	}
                        return true;
                    }

                    final String EmailAddress = args[2];
                    if (!EmailAddress.contains("@")) {
                        cs.sendMessage(ChatColor.RED + String.format(Language.PLAYER_INVALID_EMAILADDRESS.toString() + ".", args[1], EmailAddress));
                        return true;
                    }
                    
                    if (ap.setEmailAddress(EmailAddress)) {
                    	ap.setLastJoinTimestamp(System.currentTimeMillis()); // Force ProfileSave                    	
                        //cs.sendMessage(ChatColor.BLUE + String.format(Language.REGISTERED_SUCCESSFULLY.toString(), ChatColor.GRAY + name + ChatColor.BLUE));
                        String name = AmkAUtils.forceGetName(ap.getUniqueId());                    
                        if(name!=args[1]) ap.setUserName(args[1]); //name not set?, set it!
                    	cs.sendMessage(ChatColor.BLUE + String.format(Language.REGISTERED_SUCCESSFULLY.toString(), ChatColor.GRAY + args[2] + ChatColor.BLUE));
    					PConfManager.addPlayerToEm(EmailAddress); // "192.168.1.7"
            			//new BukkitRunnable() {
            			//	@Override
            			//	public void run() {        		    	
            			//		PConfManager.addPlayerToEm(EmailAddress); // "192.168.1.7"
            			//	}
            			//}.runTaskAsynchronously(AmkMcAuth.getInstance());                    	
                    }
                    else
                        //cs.sendMessage(ChatColor.RED + String.format(Language.COULD_NOT_REGISTER.toString(), ChatColor.GRAY + name + ChatColor.RED));
                    	cs.sendMessage(ChatColor.RED + String.format(Language.COULD_NOT_REGISTER.toString(), ChatColor.GRAY + args[1] + ChatColor.RED));
                    break;
                }
                case "unregister": {
                    //if (cs instanceof Player) ((Player) cs).getLocation().getWorld().setGameRuleValue("logAdminCommands", "true");
                    if (args.length < 2) {
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                        return true;
                    }
                    final AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                    if (ap == null) {
                        cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                        return true;
                    }
                    if (!ap.isRegistered()) {
                        cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_REGISTERED.toString());
                        return true;
                    }
                    PConfManager.removePlayer(ap.getUniqueId());
                    if (ap.isLoggedIn()) ap.logout(this.plugin);
        			new BukkitRunnable() {
        				@Override
        				public void run() {        		    	
        					PConfManager.removePlayerFromIp(ap.getCurrentIPAddress()); // "192.168.1.7"
        				}
        			}.runTaskAsynchronously(AmkMcAuth.getInstance());                    
                    //PConfManager.removePlayerFromIp(ap.getCurrentIPAddress());
                    
                    PConfManager.removeAllPlayer(args[1].toLowerCase());
                    ap.removeAuthPlayer(ap.getUniqueId());
                	cs.sendMessage(ChatColor.BLUE + Language.PLAYER_REMOVED.toString());
                    break;
                }
                case "getuuid": {
                    if (args.length < 2) {
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                        return true;
                    }

                    UUID u;
                	try {
                		u = AmkAUtils.getUUID(args[1]);
                	} catch (Exception ex) {
                		//ex.printStackTrace();
                        u = null;        		
                    }

                	if (u == null) {
                        cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                        return true;
                    }
                    cs.sendMessage(ChatColor.BLUE + "UUID of Player: " + ChatColor.GRAY + args[1]+ ChatColor.BLUE + " is: " + ChatColor.GRAY + u + ChatColor.BLUE);
                    break;
                }
                case "nlplist": {
                    // https://bukkit.org/threads/get-a-players-minecraft-language.172468/
                    
                    PConfManager.listVipPlayers(cs);                    
                    break;
                }
                case "nlpadd": {
                    if (args.length < 2) {
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                        return true;
                    }
                    AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                    if (ap == null) {
                        cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                        return true;
                    }
                    final String name = AmkAUtils.forceGetName(ap.getUniqueId());
                    if (!ap.isRegistered()) {
                        cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_REGISTERED.toString());
                        return true;
                    }
                    ap.setVIP(true);
                    cs.sendMessage(ChatColor.BLUE + String.format(Language.NLP_SET_UPDATED.toString(), ChatColor.GRAY + name + ChatColor.BLUE));
                    break;
                }
                case "nlprem": {
                    if (args.length < 2) {
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                        return true;
                    }
                    AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                    if (ap == null) {
                        cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                        return true;
                    }
                    final String name = AmkAUtils.forceGetName(ap.getUniqueId());
                    if (!ap.isRegistered()) {
                        cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_REGISTERED.toString());
                        return true;
                    }
                    ap.setVIP(false);
                    cs.sendMessage(ChatColor.BLUE + String.format(Language.NLP_SET_UPDATED.toString(), ChatColor.GRAY + name + ChatColor.BLUE));
                    break;
                }

                case "ipcount": {
                    if (Config.maxUsersPerIpaddress==0) {
                    	this.plugin.getLogger().info("Maximum allowed registered player counting from one Ip-Address is disabled.");            		
                	}
                	else
                	{
                		int Count = PConfManager.getIpaddressCount();
                		cs.sendMessage(ChatColor.BLUE + "A total of: " + Count + " unique registered Ip-Addresses found " );
                    }
            		break;
                }
                case "iplist": {
                    if (Config.maxUsersPerIpaddress==0) {
                    	this.plugin.getLogger().info("Maximum allowed registered player counting from one Ip-Address is disabled.");            		
                	}
                	else
                	{
                        PConfManager.listIpaddressesInfo(cs); // is running as a asynchronously task

                     	//for(int i=0; i<PConfManager.getIpaddressCount();i++) { // CommandSender cs
                        //	cs.sendMessage(ChatColor.BLUE + "Registered player-count from Ip-Address: " + PConfManager.listIpaddressesInfo(i) );
                		//}
                	}
            		break;
                }
                
                case "emcount": {
                    if (Config.maxUsersPerEmaddress==0) {
                    	this.plugin.getLogger().info("Maximum allowed registered player counting from one Email-Address is disabled.");            		
                	}
                	else
                	{
                		int Count = PConfManager.getEmaddressCount();
                		cs.sendMessage(ChatColor.BLUE + "A total of: " + Count + " unique registered Email-Addresses found " );
                    }
            		break;
                }
                case "emlist": {
                    if (Config.maxUsersPerEmaddress==0) {
                    	this.plugin.getLogger().info("Maximum allowed registered player counting from one Email-Address is disabled.");            		
                	}
                	else
                	{
                        PConfManager.listEmaddressesInfo(cs); // is running as a asynchronously task
                	}
            		break;
                }                                

                case "resetdbcon": {
                    MySQL.disconnect();
                	if (Config.MySqlDbHost.equals(""))
                        cs.sendMessage("MySQL Connection disconnected/disabled, no (re)connect, check MySqlDbHost in config.yml.");
                	else 
                		MySQL.connect();
                    cs.sendMessage(ChatColor.BLUE + "MySQL Connection Reset finished.");
                    break;
                }
                case "reloadusers": {
                	if (Config.MySqlDbHost.equals("")) {                		
                    	this.plugin.CommandDoUpdate("HashMap");
                		cs.sendMessage(ChatColor.BLUE + "Player profiles loaded into system finished.");
                	} else {
                    	this.plugin.CommandDoUpdate("MySQL");
                		cs.sendMessage(ChatColor.BLUE + "Player profiles loaded into MySQL finished.");

                		cs.sendMessage("Counting PlayerBase, ActiveProfiles, IP-Addresses and nlp-players.");
                        PConfManager.countPlayersFromIpAndGetVipPlayers();
                	}
            		//cs.sendMessage("PlayerBaseCount: " + String.valueOf(PConfManager.getPlayerCount() ) +
					//				", ActiveProfiles: " + String.valueOf(PConfManager.getProfileCount() ) +
					//				", Ip-Addresses: " + String.valueOf(PConfManager.getIpaddressCount() ) +
            		//				", nlp-Players: " + String.valueOf(PConfManager.getVipPlayerCount() ) + 
            		//	            "." );
                	// Now Showing stats (using case "showstats")..!!because there is no 'break' !!
                }
                //case "profilesave": {
                //	PConfManager.saveAllManagers("Normal");                	
                //	break;
                //}
                case "showstats": {
                	cs.sendMessage("User profile data AutoSave Task running every: " + Config.saveUserdataInterval +" minutes." );
                	if(Config.removeAfterDays>0) 
                		cs.sendMessage("Auto removal of inactive playerdata set to: " + Config.removeAfterDays +" days old.");
                	else
                		cs.sendMessage("Auto removal of inactive playerdata disabled!");
                	
                	if (!Config.MySqlDbHost.equals("")) {
                		if (MySQL.isConnected())
                			cs.sendMessage("Using MySQL for player profile data storage, connection OK.");
                		else
                			cs.sendMessage("Configured to use MySQL, but there is *no* connection!!");
                		if (!Config.MySqlDbFile.equals("")) {
                			cs.sendMessage("Using Filesystem Hash-Maps as Backup for player profiles.");
                		}
                		cs.sendMessage("Counting PlayerBase, ActiveProfiles, IP-Addresses and nlp-players.");
                		PConfManager.countPlayersFromIpAndGetVipPlayers();
                		//cs.sendMessage("PlayerBaseCount: " + String.valueOf(PConfManager.getPlayerCount() ) +
						//				", ActiveProfiles: " + String.valueOf(PConfManager.getProfileCount() ) +
						//				", Ip-Addresses: " + String.valueOf(PConfManager.getIpaddressCount() ) +
                		//				", nlp-Players: " + String.valueOf(PConfManager.getVipPlayerCount() ) + 
                		//	            "." );
                	} else {
            			cs.sendMessage("Using Filesystem Hash-Maps for player profile data storage.");
                	}
                	cs.sendMessage("PlayerBaseCount: " + String.valueOf(PConfManager.getPlayerCount() ) +
                					", ActiveProfiles: " + String.valueOf(PConfManager.getProfileCount() ) +
									", Ip-Addresses: " + String.valueOf(PConfManager.getIpaddressCount() ) +
            						", nlp-Players: " + String.valueOf(PConfManager.getVipPlayerCount() ) + 
            			            "." );                	
                    break;
                }

                case "listusers": {
                	PConfManager.listActivePlayers(cs);
                    break;
                }
                
                case "listqueue": {
                	AmkMcAuth.MyQueue.DisplayQueue(cs);
                    break;
                }
                
                case "testemail": {
                    if (!Config.emlFromEmail.contains("@")) {
                        cs.sendMessage(ChatColor.RED + "Incorrect Config Email Setup.");
                        return true;
                    }
                    if (args.length < 2) {
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                        return true;
                    }
                    final AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                    if (ap == null) {
                        cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                        return true;
                    }
                    final String EmailAddress = ap.getEmailAddress();
                    if (!EmailAddress.contains("@")) {
                        cs.sendMessage(ChatColor.RED + String.format(Language.PLAYER_INVALID_EMAILADDRESS.toString() + ".", args[1], "'"+EmailAddress+"'"));
                        return true;
                    }
                    // Lets try to send the Email to this Player....
        			//new BukkitRunnable() {
        			//	@Override
       				//	public void run() {
       				//	String Player = ap.getUserName();
           	    	//	String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
           	    	//	String Password = RandomStringUtils.random( 10, characters );

            		//	cs.sendMessage("emlSmtpServr = '" + Config.emlSmtpServr + "'");
            		//	cs.sendMessage("emlLoginName = '" + Config.emlLoginName + "'");
            		//	cs.sendMessage("emlLoginPswd = '" + Config.emlLoginPswd + "'");
            		//	cs.sendMessage("emlFromNicer = '" + Config.emlFromNicer + "'");
            		//	cs.sendMessage("emlFromEmail = '" + Config.emlFromEmail + "'");
            		//	cs.sendMessage("emailsubject = '" + Config.emailsubject + "'");
            		//	cs.sendMessage("emailbodytxt = '" + String.format(Config.emailbodytxt,Player,Password) + "'");
           	    		
           	    	//	// SetUp Email Session
        			//	SMTP.Email email = SMTP.createEmptyEmail();
    				//					//email.add("Content-Type", "text/html"); //Headers for the email (useful for html) you do not have to set them
    				//					email.add("Content-Type", "text/plain");  //Default Header ("text/plain") for the email.you do not have to set
       				//					email.from(Config.emlFromNicer, Config.emlFromEmail); //The sender of the email.
       				//					email.to(Player, EmailAddress); //The recipient of the email.
       				//					email.subject("Test-Email, "+ Config.emailsubject); //Subject of the email
       				//					email.body(String.format(Config.emailbodytxt,Player,Password));    									
       		        //   // All the email stuff here
       				//	//SMTP.sendEmail(smtpServer, email, password, mail, debug);    			        
       				//	SMTP.sendEmail(Config.emlSmtpServr,
       				//						Config.emlLoginName, 
       				//						Config.emlLoginPswd, 
       				//						email, DebugEmail);    			        
        			//	}
        			//}.runTaskAsynchronously(AmkMcAuth.getInstance());                    
                    
                    new Thread(new Runnable() {
    					@Override
    					public void run() {
    						String Player = ap.getUserName();
    						String Password = "Not-Set,Only-Demo";

    						cs.sendMessage("emlSmtpServr = '" + Config.emlSmtpServr + "'");
    						cs.sendMessage("emlLoginName = '" + Config.emlLoginName + "'");
    						cs.sendMessage("emlLoginPswd = '" + Config.emlLoginPswd + "'");
    						cs.sendMessage("emlFromNicer = '" + Config.emlFromNicer + "'");
    						cs.sendMessage("emlFromEmail = '" + Config.emlFromEmail + "'");
    						cs.sendMessage("emailsubject = '" + "Test-Email, " + Config.emailsubject + "'");
    						cs.sendMessage("emailbodytxt = '" + String.format(Config.emailbodytxt,Player,Password) + "'");
       	    		
    						// SetUp Email Session
    						SMTP.Email email = SMTP.createEmptyEmail();
    									//email.add("Content-Type", "text/html"); //Headers for the email (useful for html) you do not have to set them
										//email.add("Content-Type", "text/plain");  //Default Header ("text/plain") for the email.you do not have to set
    									email.from(Config.emlFromNicer, Config.emlFromEmail); //The sender of the email.
    									email.to(Player, EmailAddress); //The recipient of the email.
    									email.subject("Test-Email, "+ Config.emailsubject); //Subject of the email
    									email.body(String.format(Config.emailbodytxt,Player,Password));    									
    						// All the email stuff here
    						//SMTP.sendEmail(smtpServer, email, password, mail, debug);    			        
    						SMTP.sendEmail(Config.emlSmtpServr,
    										Config.emlLoginName, 
    										Config.emlLoginPswd, 
    										email, DebugEmail);
    					    
    						//SMTP.sendEmail(Config.emlSmtpServr, 
    			    		//  		Config.emlLoginName, 
    			    		//  		Config.emlLoginPswd, 
    			    		//  		Config.emlFromNicer, 
    			    		//  		Player, EmailAddress, 
    			    		//  		"Test-Email, "+ Config.emailsubject, 
    			    		//  		String.format(Config.emailbodytxt,Player,Password),
    			    		//  		DebugEmail);

    					}
    				}).start();
                    
        			break;
                }

                case "testqueue": {
                	AmkMcAuth.MyQueue.Put("QueueCallBack:~This is the test-data");
                    break;
                }                
                case "chkdevmsg": {
                	CheckDevMessage(cs);
                    break;
                }

                case "reload": {
                    Bukkit.getPluginManager().getPlugin("AmkMcAuth").reloadConfig();
                    this.plugin.c.reloadConfiguration(); // Do both ??
                    cs.sendMessage(ChatColor.BLUE + Language.CONFIGURATION_RELOADED.toString());
                    break;
                }
                
                case "debug": {
                    if (args.length < 2) {
                        cs.sendMessage(ChatColor.GRAY + "  /" + label + " iplist" + ChatColor.BLUE + " - (debug) show unique Login Ip-Addresses + playercount");
                        cs.sendMessage(ChatColor.GRAY + "  /" + label + " emlist" + ChatColor.BLUE + " - (debug) show unique Email-Addresses + playercount");
                    	if (!Config.MySqlDbHost.equals("")) {                     
                            cs.sendMessage(ChatColor.GRAY + "  /" + label + " reloadusers" + ChatColor.BLUE + " - (debug) Load Player profiles into MySQL Updater (may cause lag)");
                		} else {
                            cs.sendMessage(ChatColor.GRAY + "  /" + label + " reloadusers" + ChatColor.BLUE + " - (debug) Load Player profiles into system (may cause lag)");
                    	}   
                		cs.sendMessage(ChatColor.GRAY + "  /" + label + " listusers" + ChatColor.BLUE + " - (debug) Show Player list (ActiveProfile)");
                        cs.sendMessage(ChatColor.GRAY + "  /" + label + " listqueue" + ChatColor.BLUE + " - (debug) show current AmkQueue entries");
                        cs.sendMessage(ChatColor.GRAY + "  /" + label + " testqueue" + ChatColor.BLUE + " - (debug) test working of AmkQueue system");
                    	
                        cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " debug <name> [on|off]" + ChatColor.RED + ".");
                        cs.sendMessage("<name> can be: profilemgr smtpmail queuemgr .");
                        return true;
                    }
                    String CmpParm="";
                    if (args.length == 3) CmpParm=args[2];
                    if (args[1].equals("profilemgr")) cs.sendMessage("debug profilemgr set to: " + PConfManager.SetDebugSave(CmpParm));
                    if (args[1].equals("smtpmail")) {
                    	if (!CmpParm.equals("")) {
                    		if (CmpParm.contentEquals("on")) {
                    			DebugEmail = true;                    		
                    		} else {
                    			DebugEmail = false;
                    		}
                    	}
                    	if (DebugEmail) {                    		
                    		cs.sendMessage("debug smtpmail set to: on");
                    	} else {
                    		cs.sendMessage("debug smtpmail set to: off");
                    	}
                    }
                    if (args[1].equals("queuemgr")) cs.sendMessage("debug queuemgr set to: " + AmkMcAuth.MyQueue.SetDebugQueue(CmpParm));

                    break;
                }

                default: {
                    cs.sendMessage(ChatColor.RED + Language.INVALID_SUBCOMMAND.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                    break;
                }
            }
            return true;
        }
        return false;
    }

    public static void QueueCallBack(String Response) {
    	Logger log = AmkMcAuth.getInstance().getLogger();
		log.info("Debug: QueueCallBack Correct, Data was: " + Response );    	
    }
    
    public static void CheckDevMessage(final CommandSender cs) {
        new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Create a URL for the desired page
					URL url = new URL("https://github.com/AmkSoft/Versions/raw/master/AmkMcAuth");
					String ThisPlugin=AmkMcAuth.getInstance().getDescription().getName();
					String ThisVersion=AmkMcAuth.getInstance().getDescription().getVersion();

					int i;

					Integer CrrntVersion=0;
					String[] Dummy = ThisVersion.split("\\.");					
					for(i=0 ; i< Dummy.length ; i++) {
						try {
							CrrntVersion=(CrrntVersion*10) + Integer.parseInt(Dummy[i]);
						} catch (NumberFormatException e) {
							throw new IllegalArgumentException("Not a number: " + Dummy[i] + " at index " + i, e);
						}
					}
            
					// Read all the text returned by the server
					BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
					String str;
					while ((str = in.readLine()) != null) {
						// str = in.readLine().toString();
						// str is one line of text; readLine() strips the newline character(s)

	    				String[] Msg = str.split(":");

	    				if(Msg[0].equals("Version")) {
	                		Integer CheckVersion=0;
	    					Dummy = Msg[1].split("\\.");
	                		for(i=0 ; i< Dummy.length ; i++) {
	                			try {
	                				CheckVersion=(CheckVersion*10) + Integer.parseInt(Dummy[i]);
	                    		} catch (NumberFormatException e) {
	                    		throw new IllegalArgumentException("Not a number: " + Dummy[i] + " at index " + i, e);
	                    		}
	                		}

	        				if(CrrntVersion==CheckVersion) {
	        					cs.sendMessage(ChatColor.GREEN + "[" + ThisPlugin + "] You have the Current AmkMcAuth Version (" + ThisVersion + ") running.");
	        				} else {
		        				if(CrrntVersion<CheckVersion) {
		        					cs.sendMessage(ChatColor.RED + "[" + ThisPlugin + "] There is a newer Version (" + Msg[1] + ") of the AmkMcAuth plugin available!");
		        					cs.sendMessage(ChatColor.RED + "[" + ThisPlugin + "] Download from: " + ChatColor.BLUE + "https://dev.bukkit.org/projects/amkmcauth");
		        				} else {
		        					cs.sendMessage(ChatColor.BLUE + "[" + ThisPlugin + "] You are running a future/test Version (" + ThisVersion + ") of the AmkMcAuth plugin.");
		        				}
	        				}
	    				}
	    				if(Msg[0].equals("Message") && !str.equals("")) {
	                		cs.sendMessage(ChatColor.BLUE + ThisPlugin + " Author: " + ChatColor.GREEN + Msg[1]);
	    				}
            	}
            		in.close();
        		} catch (MalformedURLException e) {
        		} catch (IOException e) {
        		}
			}
    	}).start();
    }
}
