package com.mooo.amksoft.amkmcauth;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {

    public static boolean disableIfOnlineMode;
    public static boolean requireLogin;
    public static boolean kickIfAlreadyOnline;
    public static boolean KickOnPasswordFail;
    public static boolean useLoginPermission;
    public static boolean emailForceSet;
    public static long emailRemindInterval;
    public static String loginPermission;
    public static String registrationType;
    public static String emlFromNicer;
    public static String emlFromEmail;
    public static String emlSmtpServr;
    public static String emlLoginName;
    public static String emlLoginPswd;    
    public static String emailsubject;
    public static String emailbodytxt;    
    public static String confirmbodytxt;    
    public static long emailWaitKick;    
    public static boolean allowChat;
    public static String chatPrefix;
    public static boolean allowCommands;
    public static List<String> allowedCommands;
    public static boolean allowMovementWalk;
    public static boolean allowMovementLook;
    public static long allowMovementTime;
    public static boolean godMode;
    public static boolean godModeAfterLogin;
    public static long godModeLength;
    public static boolean remindEnabled;
    public static long remindInterval;
    public static long saveUserdataInterval;
    public static boolean sessionsEnabled;
    public static boolean sessionsCheckIP;
    public static long sessionLength;
    public static long removeAfterDays;
    public static String sessionType;
    public static List<String> disallowedPasswords;
    public static List<String> disallowedEmlAdresses;
    public static String passwordHashType;
    public static boolean validateUsernames;
    public static String usernameRegex;
    public static boolean adventureMode;
    public static boolean teleportToSpawn;
    public static boolean kickPlayers;
    public static long kickAfter;
    public static boolean checkOldUserdata;
    public static long maxUsersPerIpaddress;
    public static long maxUsersPerEmaddress;
    public static String MySqlDbFile;
    public static String MySqlDbHost;
    public static String MySqlDbPort;
    public static String MySqlDbDbse;
    public static String MySqlOption; 
    public static String MySqlDbUser;
    public static String MySqlDbPswd;
    
    private final AmkMcAuth plugin;

    public Config(AmkMcAuth instance) {
        this.plugin = instance;
        final File config = new File(plugin.getDataFolder(), "config.yml");
        if (!config.exists()) {
            if (!config.getParentFile().mkdirs()) 
            	this.plugin.getLogger().warning("Could not create config.yml directory.");
            this.plugin.saveDefaultConfig();            
        }
        this.reloadConfiguration();
    }

    public void reloadConfiguration() {
        this.plugin.reloadConfig(); 
        final FileConfiguration c = this.plugin.getConfig();

        disableIfOnlineMode = c.getBoolean("login.disable_if_online_mode");
        requireLogin = c.getBoolean("login.require");
        kickIfAlreadyOnline = c.getBoolean("login.kick_if_already_online");

        if(c.isSet("login.kick_on_password_fail")) {        	
        	KickOnPasswordFail = c.getBoolean("login.kick_on_password_fail");
    	} else {
            c.set("login.kick_on_password_fail",false);
            this.plugin.saveConfig(); // Save new Configuration
            KickOnPasswordFail=false;
        }

        useLoginPermission = c.getBoolean("login.permission.enabled");
        loginPermission = c.getString("login.permission.permission");

        emailWaitKick=120L;            
        emailForceSet=false;
        emailRemindInterval=120L;
        registrationType="password";  // Default Registration = password
        if(c.isSet("login.registration")) {        	
        	registrationType= c.getString("login.registration");
        	emailForceSet= c.getBoolean("login.forcesetmail");
        	emailRemindInterval=c.getLong("login.emailremindtime");
        	emlFromNicer= c.getString("login.regemlfromnice");
        	emlFromEmail= c.getString("login.regemlfromemail");
        	emlSmtpServr= c.getString("login.regemlsmtpservr");
        	emlLoginName= c.getString("login.regemlloginname");
        	emlLoginPswd= c.getString("login.regemlloginpswd");
        	emailsubject= c.getString("login.emailsubject");
        	emailbodytxt= c.getString("login.emailbodytxt");
        	confirmbodytxt= c.getString("login.confirmbodytxt");
        	emailWaitKick= c.getLong("login.email_wait_kick");        	
            disallowedEmlAdresses=c.getStringList("emailaddresses.disallowed");        
    	} else {
            c.set("login.registration","password");
        	c.set("login.forcesetmail",false);
        	c.set("login.emailremindtime",120L);
            c.set("general.users_per_emailaddress", 0L);
            c.set("login.regemlfromnice", "Server Nice Name");
            c.set("login.regemlfromemail", "Sender-Email-Address");
            c.set("login.regemlsmtpservr", "Smtp-Mail-Server");
            c.set("login.regemlloginname", "Smtp-Login-name");
            c.set("login.regemlloginpswd", "Smtp-Login-Password");
            c.set("login.emailsubject", "MineCraft Player Registration Information");
            c.set("login.emailbodytxt", "Login Password for Player: %1$s is set to %2$s\nYou can change this after login using the changepassword command.\nHappy Mining on our MineCraft Server.");
            c.set("login.confirmbodytxt", "Confirm your new email-address by issuing\nthis player command: %1$s");
        	c.set("login.email_wait_kick", 120L);
        	List<String> disAllowed = Arrays.asList("@spamgourmet.", "@guerillamail");
        	c.set("emailaddresses.disallowed", disAllowed);
        	
            this.plugin.saveConfig(); // Save new Configuration
        }
        
        allowChat = c.getBoolean("login.restrictions.chat.allowed");
        chatPrefix = c.getString("login.restrictions.chat.prefix");

        allowCommands = c.getBoolean("login.restrictions.commands.allowed");
        allowedCommands = c.getStringList("login.restrictions.commands.exempt");

        allowMovementWalk = c.getBoolean("login.restrictions.movement.walk");
        allowMovementLook = c.getBoolean("login.restrictions.movement.look_around");
        allowMovementTime = c.getLong("login.restrictions.movement.allowmovetime");

        godMode = c.getBoolean("login.godmode.enabled");
        godModeAfterLogin = c.getBoolean("login.godmode.after_login.enabled");
        godModeLength = c.getLong("login.godmode.after_login.length");

        remindEnabled = c.getBoolean("login.remind.enabled");
        remindInterval = c.getLong("login.remind.interval");

        saveUserdataInterval = c.getLong("saving.interval");

        sessionsEnabled = c.getBoolean("sessions.enabled");
        sessionsCheckIP = c.getBoolean("sessions.check_ip");
        sessionLength = c.getLong("sessions.length");
        sessionType = c.getString("sessions.LoginCommandsMessage");
        sessionType = "Commands";  // OverRuled!!, Not working!!!

        disallowedPasswords = c.getStringList("passwords.disallowed");
        passwordHashType = c.getString("passwords.hash_type");

        validateUsernames = c.getBoolean("usernames.verify");
        usernameRegex = c.getString("usernames.regex");

        adventureMode = c.getBoolean("login.adventure_mode");
        teleportToSpawn = c.getBoolean("login.teleport_to_spawn");

        kickPlayers = c.getBoolean("login.remind.kick.enabled");
        kickAfter = c.getLong("login.remind.kick.wait");

        checkOldUserdata = c.getBoolean("saving.check_old_userdata");

        MySqlDbFile="Save"; // Default Use ProfileFiles.  
        MySqlDbHost=""; // Default NO MySQL
        if(c.isSet("saving.mysql_hostname")) {
        	MySqlDbFile = c.getString("saving.mysql_filemode");
        	MySqlDbHost = c.getString("saving.mysql_hostname");
        	MySqlDbPort = c.getString("saving.mysql_portnmbr");
        	MySqlDbDbse = c.getString("saving.mysql_database");
        	MySqlOption = c.getString("saving.mysql_dboption");
        	MySqlDbUser = c.getString("saving.mysql_username");
        	MySqlDbPswd = c.getString("saving.mysql_password");
        	if(MySqlDbHost.equals("") && MySqlDbFile.equals("")) MySqlDbFile = "Save"; 
        }
        else
        	{
        	c.set("saving.mysql_filemode","Save");
        	c.set("saving.mysql_hostname","");
        	c.set("saving.mysql_portnmbr","3306");
        	c.set("saving.mysql_database","AmkMcData");
        	c.set("saving.mysql_dboption","useSSL=false&autoReconnect=true");
        	c.set("saving.mysql_username","AmkMcUser");
        	c.set("saving.mysql_password","AmkMcPswd");
            this.plugin.saveConfig(); // Save new Configuration
        }
        
        if(c.isSet("saving.remove_inactive_after")) {        	
            removeAfterDays = c.getInt("saving.remove_inactive_after");
    	} else {
            c.set("saving.remove_inactive_after",99);
            this.plugin.saveConfig(); // Save new Configuration
            removeAfterDays=99L;
        }

        maxUsersPerIpaddress = c.getLong("general.users_per_ipaddress");
        maxUsersPerEmaddress = c.getLong("general.users_per_emailaddress");

        //-- Check for invalid inputs and set to default if invalid --//

        if (remindInterval < 1L)       remindInterval = 30L;
        if (emailRemindInterval < 1L)  emailRemindInterval = 120L;
        if (saveUserdataInterval < 1L) saveUserdataInterval = 5L;
        if (sessionLength < 1L)        sessionLength = 15L;
        if (kickAfter < 0L)            kickAfter = 0L;
        if (godModeLength <= 0L)       godModeLength = 10L;
        if (maxUsersPerIpaddress < 0L) maxUsersPerIpaddress = 0L;
        if (maxUsersPerEmaddress < 0L) maxUsersPerEmaddress = 0L;

    	this.plugin.getLogger().info("AmkMcAuth configuration loaded/reset from config.yml.");
    }
}
