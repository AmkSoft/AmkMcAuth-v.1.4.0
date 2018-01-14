package com.mooo.amksoft.amkmcauth;

import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class AuthPlayer {

    private final static Map<UUID, AuthPlayer> authPlayers = new HashMap<>();
    private final PConfManager pcm;
    private UUID playerUUID;
    private String lastIPAddress;
    private int IpAddressCount=0;
    private long lastJoinTimestamp = 0L;
    private long lastWalkTimestamp = 0L;
    private long lastLoginTimestamp = 0L;
    private long lastQuitTimestamp = 0L;
    private Location lastJoinLocation;
    private BukkitTask reminderTask = null;

    private AuthPlayer(UUID u) {
        this.playerUUID = u;
        this.pcm = PConfManager.getPConfManager(u);
        this.lastJoinTimestamp = this.pcm.getLong("timestamps.join", 0L);
        this.lastLoginTimestamp = this.pcm.getLong("timestamps.login", 0L);
        this.lastQuitTimestamp = this.pcm.getLong("timestamps.quit", 0L);
    }

    private AuthPlayer(Player p) {
        this(p.getUniqueId());
    }

    /**
     * Gets the AuthPlayer for the UUID of a player.
     *
     * @param u UUID to get AuthPlayer of
     * @return AuthPlayer
     */
    public static AuthPlayer getAuthPlayer(UUID u) {
        synchronized (AuthPlayer.authPlayers) {
            if (AuthPlayer.authPlayers.containsKey(u)) return AuthPlayer.authPlayers.get(u);
            final AuthPlayer ap = new AuthPlayer(u);
            AuthPlayer.authPlayers.put(u, ap);
            return ap;
        }
    }

    /**
     * Queries Mojang's API to get a UUID for the name, and then gets the AuthPlayer for that UUID.
     *
     * @param s Name
     * @return AuthPlayer or null if there was an error
     */
    public static AuthPlayer getAuthPlayer(String s) {
        //boolean Online=true;
        //UUID u;
        
    	//if(Bukkit.getOnlineMode()!= Online) 
		//{
    	//	// Server runs 'OffLine' AmkMcAuth calculates the UUID for this player...
    	//    u = UUID.nameUUIDFromBytes(("OfflinePlayer:" + s).getBytes(Charsets.UTF_8));    		
		//}
    	//else
    	//	{
    	//	try {
    	//		u = AmkAUtils.getUUID(s);
    	//	} catch (Exception ex) {
    	//		//ex.printStackTrace();
        //        return null;        		
        //	}
        //}
        UUID u;
    	try {
    		u = AmkAUtils.getUUID(s);
    	} catch (Exception ex) {
    		//ex.printStackTrace();
            return null;        		
        }
        return AuthPlayer.getAuthPlayer(u);
    }

    /**
     * Gets the AuthPlayer that represents a Player.
     *
     * @param p Player to get AuthPlayer of
     * @return AuthPlayer
     */
    public static AuthPlayer getAuthPlayer(Player p) {
        return AuthPlayer.getAuthPlayer(p.getUniqueId());
    }

    /**
     * Remove AmkMcAuth PlayerProfile from configuration by the UUID of a player. 
     *
     * @param u UUID of the AuthPlayer to remove
     */
    public void remAuthPlayer(UUID u){
        synchronized (AuthPlayer.authPlayers) {
            if (AuthPlayer.authPlayers.containsKey(u)) {
            	AuthPlayer.authPlayers.remove(u);
            }
        }
    }
    /**
     * Removes and Deletes an AuthPlayer by the UUID
     *
     * @param u UUID of the AuthPlayer to remove
     */
    public void removeAuthPlayer(UUID u) {
    	remAuthPlayer(u);
    	removeThisPlayer();
    }

    /**
     * Sets AmkMcAuth Player as not logged in and no Username and no password 
     *
     */
    public void removeThisPlayer() {
        //this.pcm.set("login.username", UserName);
    	//set EVERY playerInfo to null to clean it all.
        this.setLoggedIn(false);
    	this.pcm.set("login.password",null);
    	this.pcm.set("login.username",null);
    }

    /**
     * Checks if the AP has a password set.
     *
     * @return true if registered, false if not
     */
    public boolean isRegistered() {
        return this.pcm.isSet("login.password");
    }

    /**
     * Checks if Player is a VIP Player.
     *
     * @return true if VIP, false if not
     */
    public boolean isVIP() {
        return this.pcm.getBoolean("login.vip");
    }

    /**
     * Checks if the AP has logged in.
     *
     * @return true if logged in, false if not
     */
    public boolean isLoggedIn() {
        return this.pcm.getBoolean("login.logged_in");
    }

    /**
     * Sets the AP's logged in status. In most cases, login() or logout() should be used.
     *
     * @param loggedIn true if logged in, false if not
     */
    public void setLoggedIn(final boolean loggedIn) {
        this.pcm.set("login.logged_in", loggedIn);
    }

    /**
     * Sets the AP's Logged/Register-IpAddress count. Only used during Join+Register.
     *
     * @param The Login-IpAddress-Count of the Ip-Address the player is coming from
     */
    public void setPlayerIpCount(final int Count) {
    	this.IpAddressCount=Count;
    }
    
    /**
     * Sets the AP's VIP status. 
     *
     * @param VIP true if it is a VIP, false if not
     */
    public void setVIP(final boolean VIP) {
        this.pcm.set("login.vip", VIP);
        if(VIP)
        	PConfManager.addVipPlayer(getUserName());
        else
        	PConfManager.removeVipPlayer(getUserName());
    }

    /**
     * Sets the AP's logged in UserName. 
     *
     * @param loggedIn true if logged in, false if not
     */
    public void setUserName(final String UserName) {
        this.pcm.set("login.username", UserName);
    }
   
    
    /**
     * Checks if the AP has/had previously a logged in Session. Will return false if not correct logged in.
     *
     * @return true if (still) logged in and on same IP-Address
     */
    public boolean isInSession() {
        if (!this.isLoggedIn()) return false;
        if (this.lastLoginTimestamp <= 0L || this.lastQuitTimestamp <= 0L) return false;
        if (!getCurrentIPAddress().equals(this.lastIPAddress)) return false;
        return true;
    }

    /**
     * Checks if the AP is within a login session. Will return false if sessions are disabled.
     *
     * @return true if in a session, false if not or sessions are off
     * Extra test on NLP (NoLoginPassword) Player (Like VIP's).
     */
    public boolean isWithinSession() {
        if (!Config.sessionsEnabled) return false;
        if (Config.sessionsCheckIP && !isInSession()) return false; // Line replaced next 3 lines
        //if (this.lastLoginTimestamp <= 0L || this.lastQuitTimestamp <= 0L) return false;
        //if (!this.isLoggedIn()) return false;
        //if (Config.sessionsCheckIP && !getCurrentIPAddress().equals(this.lastIPAddress)) return false;
        long validUntil = Config.sessionLength * 60000L + this.lastQuitTimestamp;
        return (validUntil > System.currentTimeMillis() || this.isVIP() );
    }

    /**
     * Changes the player's password. Requires the old password for security verification.
     *
     * @param hashedPassword    The password hash of the new password.
     * @param oldHashedPassword The password hash of the old password.
     * @return true if password changed, false if not
     */
    public boolean setHashedPassword(String hashedPassword, String oldHashedPassword, final String hashType) {
        if (!this.getPasswordHash().equals(oldHashedPassword)) return false;
        this.pcm.set("login.password", hashedPassword);
        this.pcm.set("login.hash", hashType.toUpperCase());
        return true;
    }

    /**
     * Changes the player's password. Requires the old password for security verification.
     *
     * @param rawPassword    Plaintext new password
     * @param rawOldPassword Plaintext old password
     * @param hashType       Hashtypes to be used on these passwords
     * @return true if password changed, false if not
     */
    public boolean setPassword(String rawPassword, String rawOldPassword, final String hashType) {
        final String oldPasswordHash = (!getHashType().equalsIgnoreCase(hashType)) ? getHashType() : hashType;
        try {
            rawPassword = Hasher.encrypt(rawPassword, hashType);
            rawOldPassword = Hasher.encrypt(rawOldPassword, oldPasswordHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        return this.setHashedPassword(rawPassword, rawOldPassword, hashType);
    }

    /**
     * Gets the hashed password currently set for the AP.
     *
     * @return Hashed password
     */
    public String getPasswordHash() {
        return this.pcm.getString("login.password");
    }

    /**
     * Gets the type of hash used to make the AP's password.
     *
     * @return Digest type
     */
    public String getHashType() {
        return this.pcm.getString("login.hash", "AMKAUTH");
    }

    /**
     * Gets the PConfManager of this AP.
     *
     * @return PConfManager
     */
    public PConfManager getConfiguration() {
        return this.pcm;
    }

    /**
     * Turns on the god mode for post-login if enabled in the config. Will auto-expire.
     */
    public void enableAfterLoginGodmode() {
        if (Config.godModeAfterLogin)
            this.pcm.set("godmode_expires", System.currentTimeMillis() + Config.godModeLength * 1000L);
    }

    /**
     * Checks if the player is in godmode from post-login godmode.
     *
     * @return true if in godmode, false if otherwise
     */
    public boolean isInAfterLoginGodmode() {
        if (!Config.godModeAfterLogin) return false;
        final long expires = pcm.getLong("godmode_expires", 0L);
        return expires >= System.currentTimeMillis();
    }

    /**
     * Logs an AP in. Does everything necessary to ensure a full login.
     */
    public void login() {
        final Player p = getPlayer();
        if (p == null) throw new IllegalArgumentException("That player is not online!");
        this.setLoggedIn(true);
        this.setUserName(p.getName());
        this.setLastLoginTimestamp(System.currentTimeMillis());
        final BukkitTask reminder = this.getCurrentReminderTask();
        if (reminder != null) reminder.cancel();
        final PConfManager pcm = getConfiguration();
        if (Config.adventureMode) {
            if (pcm.isSet("login.gamemode")) {
                try {
                    p.setGameMode(GameMode.valueOf(pcm.getString("login.gamemode", "SURVIVAL")));
                } catch (IllegalArgumentException e) {
                    p.setGameMode(GameMode.SURVIVAL);
                }
            }
            pcm.set("login.gamemode", null);
        }
        if (Config.allowMovementTime>0){ //  Last Time to reset to Login Location
            //p.teleport(pcm.getLocation("login.lastlocation"));
        	p.teleport(this.getJoinLocation());
        }
        if (Config.teleportToSpawn) {
            if (pcm.isSet("login.lastlocation")) p.teleport(pcm.getLocation("login.lastlocation"));
            pcm.set("login.lastlocation", null);
        }
        this.enableAfterLoginGodmode();
        this.setLoggedIn(true);
        this.setUserName(p.getName());
    }

    /**
     * Logs a player out. This does the same as forcing a player to rejoin.
     * <p/>
     * This will schedule reminders for the player.
     *
     * @param plugin Plugin to register reminder events under
     */
    public void logout(Plugin plugin) {
        this.logout(plugin, true);
    }

    /**
     * Logs a player out. This does the same as forcing a player to rejoin.
     *
     * @param plugin          Plugin to register reminder events under
     * @param createReminders If reminders should be created for the player
     */
    public void logout(Plugin plugin, boolean createReminders) {
        final Player p = getPlayer();
        //if (p == null) throw new IllegalArgumentException(Language.PLAYER_NOT_ONLINE.toString());
        if (p != null) {
        	this.setLoggedIn(false);
        	if (createReminders) {
        		if (this.isRegistered()) this.createLoginReminder(plugin);
        		else this.createRegisterReminder(plugin);
        	}
        	final PConfManager pcm = getConfiguration();
        	if (Config.adventureMode) {
        		if (!pcm.isSet("login.gamemode")) pcm.set("login.gamemode", p.getGameMode().name());
        		p.setGameMode(GameMode.ADVENTURE);
        	}
        	if (Config.teleportToSpawn) {
        		if (!pcm.isSet("login.lastlocation")) pcm.setLocation("login.lastlocation", p.getLocation());
        		p.teleport(p.getLocation().getWorld().getSpawnLocation());
        	}
            if (Config.allowMovementTime>0){ //  Last Time to reset to Login Location
                //p.teleport(pcm.getLocation("login.lastlocation"));
            	this.setJoinLocation(p.getLocation()); // Logout to Join Location Fix ??
            }
        	this.setLoggedIn(false);
    	}
    }

    /**
     * Sets the last time that an AP logged in.
     *
     * @param timestamp Time in milliseconds from epoch
     */
    public void setLastLoginTimestamp(final long timestamp) {
        this.lastLoginTimestamp = timestamp;
        this.pcm.set("timestamps.login", timestamp);
    }

    /**
     * Sets the last time that an AP quit.
     *
     * @param timestamp Time in milliseconds from epoch
     */
    public void setLastQuitTimestamp(final long timestamp) {
        this.lastQuitTimestamp = timestamp;
        this.pcm.set("timestamps.quit", timestamp);
    }


    /**
     * Gets the AP's Registered EMail address.
     *
     * @return Registered Email Address)
     */
    public String getEmailAddress() {
    	// the ""+ prevents from returing null value.
        return ""+this.pcm.getString("login.emladdress");
    }

    /**
     * Sets the Registered Email Address.
     *
     * @param The EmailAddress to set.
     */
    public boolean setEmailAddress(final String emladdress) {
        this.pcm.set("login.emladdress", emladdress);
        return true;
    }

    /**
     * Sets the AP's password.
     *
     * @param newPasswordHash An already encrypted password
     * @param hashType        What type of hash was used to encrypt the password (Java type)
     * @return true
     */
    public boolean setHashedPassword(String newPasswordHash, final String hashType) {
        this.pcm.set("login.password", newPasswordHash);
        this.pcm.set("login.hash", hashType);
        return true;
    }

    /**
     * Sets the AP's password.
     *
     * @param rawPassword Unencrypted password
     * @param hashType    What type of hash was used to encrypt the password (Java type)
     * @return true if password set, false if otherwise
     */
    public boolean setPassword(String rawPassword, final String hashType) {
        try {
            rawPassword = Hasher.encrypt(rawPassword, hashType);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        this.pcm.set("login.password", rawPassword);
        this.pcm.set("login.hash", hashType);
        return true;
    }

    /**
     * Gets the AP's last/previous used IP address (not the current IP-Address) .
     *
     * @return ip IP Address (after quit it is the Current Ip-Address)
     */
    public String getLastIPAddress() {
        //"login.ipaddress" only used in Register to check on multiple Usernames from Client-IP
        return this.pcm.getString("login.ipaddress");
    }

    /**
     * Sets the AP's last IP address.
     *
     * @param ip IP Address (IPv6 or IPv4 will work, as long as they are consistent)
     */
    public void setLastIPAddress(String ip) {
        this.lastIPAddress = ip.replace("/", "");
        //"login.ipaddress" only used in Register to check on multiple Usernames from Client-IP
        this.pcm.set("login.ipaddress", this.lastIPAddress);
    }

    /**
     * Updates the AP's IP address automatically
     */
    public void updateLastIPAddress() {
        final String ip = getCurrentIPAddress();
        if (ip.isEmpty()) return;
        this.setLastIPAddress(ip);
    }

    /**
     * Gets the AuthPlayer's current IP address.
     *
     * @return IP address in String form or empty string if the player was null
     */
    public String getCurrentIPAddress() {
        final Player p = this.getPlayer();
        if (p == null) return "";
        final InetSocketAddress isa = p.getAddress();
        if (isa == null) return "";
        return isa.getAddress().toString().replace("/", "");
    }

    /**
     * Gets the current task sending reminders to the AP.
     *
     * @return Task or null if no task
     */
    public BukkitTask getCurrentReminderTask() {
        return this.reminderTask;
    }

    /**
     * Creates a task to remind the AP to login.
     *
     * @param p Plugin to register task under
     * @return Task created
     */
    public BukkitTask createLoginReminder(Plugin p) {
        this.reminderTask = AmkAUtils.createLoginReminder(getPlayer(), p);
        return this.getCurrentReminderTask();
    }
    public BukkitTask createLoginEmailReminder(Plugin p) {
        this.reminderTask = AmkAUtils.createLoginEmailReminder(getPlayer(), p);
        return this.getCurrentReminderTask();
    }

    /**
     * Creates a task to remind the AP to register.
     *
     * @param p Plugin to register task under
     * @return Task created
     */
    public BukkitTask createRegisterReminder(Plugin p) {
        this.reminderTask = AmkAUtils.createRegisterReminder(getPlayer(), p);
        return this.getCurrentReminderTask();
    }

    /**
     * Creates a task to remind the AP to register.
     *
     * @param p Plugin to register task under
     * @return Task created
     */
    public BukkitTask createSetEmailReminder(Plugin p) {
        this.reminderTask = AmkAUtils.createSetEmailReminder(getPlayer(), p);
        return this.getCurrentReminderTask();
    }

    /**
     * Gets the Player object represented by this AuthPlayer.
     *
     * @return Player or null if player not online
     */
    public Player getPlayer() {
        return Bukkit.getPlayer(playerUUID);
    }

    /**
     * Gets the Player UserName represented by this AuthPlayer.
     *
     * @return Player or null if player not online
     */
    public String getUserName() {
        return this.pcm.getString("login.username");
     }

    /**
     * Gets the UUID associated with this AuthPlayer.
     *
     * @return UUID
     */
    public UUID getUniqueId() {
        return this.playerUUID;
    }

    /**
     * Gets the last time this AP joined the server. If this is 0, they have never joined.
     *
     * @return Timestamp in milliseconds from epoch
     */
    public long getLastJoinTimestamp() {
        return this.lastJoinTimestamp;
    }
    public long getLastWalkTimestamp() {
        return this.lastWalkTimestamp;
    }

    /**
     * Gets the AP's Logged/Register-IpAddress count. Only used during Join+Register.
     *
     * @return The Login-IpAddress-Count of the Ip-Address the player is coming from
     */
    public int getPlayerIpCount() {
    	return this.IpAddressCount;
    }

    /**
     * Sets the last time that an AP joined the server.
     *
     * @param timestamp Time in milliseconds from epoch
     */
    public void setLastJoinTimestamp(final long timestamp) {
        this.lastJoinTimestamp = timestamp;
        this.lastWalkTimestamp = timestamp;
        this.pcm.set("timestamps.join", timestamp);
    }
    
    public void setLastWalkTimestamp(final long timestamp) {
        this.lastWalkTimestamp = timestamp;
    }

    /**
     * Gets the Location where the Player was when he Joined.
     *
     * @return Location
     */
    public Location getJoinLocation() {
        return this.lastJoinLocation;
    }

    /**
     * Sets the last time that an AP joined the server.
     *
     * @param Location
     */
    public void setJoinLocation(final Location l) {
        this.lastJoinLocation = l;
    }
}
