package com.mooo.amksoft.amkmcauth.tools;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.PConfManager;
import com.mooo.amksoft.amkmcauth.commands.CmdAmkAuth;

// AmkSoft QueueClass to Implement String Queue
// Use to synchronize Actions if Thread save is a problem
// Just run the "QueueManager" as an asynchronous task.
//
// First create Queue by issuing this command:
// QueueClass MyQueue = new QueueClass(int MaxDepth);        
//
// 'Put' Messages on Queue and check for IsRunning() "QueueManager".
// If there is not a Running "QueueManager", start it as asynchronous task.
// The "QueueManager" marks itself as SetRunning(true) and 
// (loop) Get the (first/next) message from the Queue 
//   and performs the required action on it.
//   The QueueManager then checks on 'isEmpty()'
//   and if not, do (loop)
// Queue is empty, do SetRunning(false) and "QueueManager" ends.

public final class QueueClass
{
    protected ConcurrentLinkedDeque<String> AmkQueue = new ConcurrentLinkedDeque<String>();
    protected long ManagerIsRunning;
    protected int maxdepth;

    private static boolean DebugQueue=false;

    /* Constructor */
    public QueueClass(int n) 
    {
        maxdepth = n; // Maximum QueueDepth
        ManagerIsRunning = 0; // On initial, there is no Running "QueueManager"
    }

    /*  Function to check if queue is empty */
    public boolean isEmpty() 
    {
        return AmkQueue.isEmpty();
    }
    
    /*  Function to check if queue is full */
    public boolean isFull() 
    {
        return getSize() >= maxdepth;
    }    
    
    /*  Function to get the size of the queue */
    public int getSize()
    {
        return AmkQueue.size() ;
    }
    
    /*  Function to check the front element of the queue */
    public String peek() 
    {
        if (isEmpty())
           throw new NoSuchElementException("Underflow Exception");
        return AmkQueue.peekFirst();
    }
    
    /*  Function to insert an element to the queue */
    public void Put(String s) 
    {
        // First/Next Element on Queue
        if (!isFull()) {
        	if(DebugQueue) {
            	Logger log = AmkMcAuth.getInstance().getLogger();
        		log.info("Debug: QueDepth: " + Integer.toString(getSize()) + ", MsgPut: " + s);
        	}        		
            AmkQueue.add(s);
        	RunQueueManager();
        }
        else 
            throw new IndexOutOfBoundsException("Overflow Exception");
    }
    
    /*  Function to remove front element from the queue */
    public String Get() 
    {
        if (isEmpty())
           throw new NoSuchElementException("Underflow Exception");
        else 
        {
        	if(DebugQueue) {
            	Logger log = AmkMcAuth.getInstance().getLogger();
        		log.info("Debug: QueDepth: " + Integer.toString(getSize()) + ", MsgGet: " + peek());
        	}        		
            return AmkQueue.pollFirst();
        }        
    }

    /*  Function to set the QueueManager Running/NotRunning status */
    public void SetRunning(boolean status ) {
    	if (status)
    		ManagerIsRunning = System.currentTimeMillis();
    	else
    		ManagerIsRunning = 0;
    }
    /*  Function to get the QueueManager Running/NotRunning status*/
    public boolean IsRunning() { // one message should not longer take then 10 seconds..
    	if(DebugQueue) {
    		if(ManagerIsRunning>0 & System.currentTimeMillis()-ManagerIsRunning>=10000) {
    			Logger log = AmkMcAuth.getInstance().getLogger();
    			log.info("Debug: QueueManager running more then 10 seconds");
    		}
    	}        		
    	return (System.currentTimeMillis()-ManagerIsRunning<10000); // 10Sec=10.000 milliseconds
    	// FailSave: more then 10 seconds: "QueueManager" abended/got killed???
    	// meaning the 'IsRunning()' is then returning false if running for more then 10sec.
    }
    
    // https://www.tutorialspoint.com/compile_java_online.php	
    // https://www.tutorialspoint.com/java/java_using_iterator.htm
    public void DisplayQueue(final CommandSender cs) {
		new BukkitRunnable() {
		    @Override
		    public void run() {
		    	cs.sendMessage("Entries in AmkMcQueue: ");
		        Iterator<String> itr = AmkQueue.iterator();
		        
		        while(itr.hasNext()) {
		           Object element = itr.next();
		           cs.sendMessage((String) element);
		        }

		    }
		}.runTaskAsynchronously(AmkMcAuth.getInstance());    		
    }
    
    private void RunQueueManager() {

    	if(!IsRunning()) {
    		SetRunning(true);
        	if(DebugQueue) {
            	Logger log = AmkMcAuth.getInstance().getLogger();
        		log.info("Debug: QueueManager started processing actions");
        	}        		
    		
    		new Thread(new Runnable() {
				@Override
				public void run() {
					// Do everything to clear the Queue
					while(!isEmpty()){
			    		SetRunning(true); // Update Running Status (1 command max. 10 seconds)..
			    		
			    		// Message= SQL:~Select * from xxx where a = ? ~String:Hallo~Type:Etc... 
			    		String Msg[] = Get().split("~");
			    		//Msg[0]="SQL:"
			    		// ------------------------------------------------------------------------------
			    		if(Msg[0].equals("QueueCallBack:")) { // Run QueueCallBack You Name it....
			    			CmdAmkAuth.QueueCallBack(Msg[1]); // Just for Debug/testing
			    		}
			    		// ------------------------------------------------------------------------------
			    		if(Msg[0].equals("SQL:")) { // Run  SQL Command.
				    		//Msg[1]="Select * from xxx where a = ? " 
				    		//Msg[2]="String:Hallo"
				    		//Msg[++]="Type:Etc..."
	   						PreparedStatement ps;
	   						try {
	   							//"SELECT count(*) as Aanwezig FROM Players WHERE  UUID = ?"
								ps = MySQL.getConnection().prepareStatement(Msg[1]);
								for(int i=2; i<Msg.length; i++) {
									String Params[]=Msg[i].split(":");
									if(Params[0].equals("String")) ps.setString(i-1, Params[1]);
									if(Params[0].equals("int"))    ps.setInt(i-1, Integer.parseInt(Params[1]));
									if(Params[0].equals("long"))   ps.setLong(i-1, Long.parseLong(Params[1]));
								}
								ResultSet res = ps.executeQuery();   				  			    			
								res.close();
								ps.close();	   						
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			    		}
			    		// ------------------------------------------------------------------------------			    		
			    		if(Msg[0].equals("addAllPlayer:")) { // Run addAllPlayer You Name it....
			    			PConfManager.addAllPlayerSet(Msg[1]);
			    		}
			    		// ------------------------------------------------------------------------------
			    		if(Msg[0].equals("addPlayerToEm:")) { // Run addPlayerToEm You Name it....
			    			PConfManager.addPlayerToEmSet(Msg[1]);
			    		}
			    		// ------------------------------------------------------------------------------			    		
			    		if(Msg[0].equals("addPlayerToIp:")) { // Run addPlayerToIp You Name it....
			    			PConfManager.addPlayerToIpSet(Msg[1]);
			    		}
			    		// ------------------------------------------------------------------------------			    		
			    		if(Msg[0].equals("removePlayerFromEm:")) { // Run removePlayerFromEm You Name it....
			    			PConfManager.removePlayerFromEmSet(Msg[1]);
			    		}
			    		// ------------------------------------------------------------------------------			    		
			    		if(Msg[0].equals("removePlayerFromIp:")) { // Run removePlayerFromIp You Name it....
			    			PConfManager.removePlayerFromIpSet(Msg[1]);
			    		}
			    		// ------------------------------------------------------------------------------			    		
					}
		    		SetRunning(false);
		        	if(DebugQueue) {
		            	Logger log = AmkMcAuth.getInstance().getLogger();
		        		log.info("Debug: QueueManager ready and waiting for new actions");
		        	}        		
				}
			}).start();
    	}
    }
    
    // Turn SAVE-Debug on or off 
    public String SetDebugQueue(String OnOff) {
    	if(OnOff.equals("on")) 	DebugQueue=true;
    	if(OnOff.equals("off")) DebugQueue=false;
    	
    	if(DebugQueue)
    		return "on";
    	else
    		return "off";
    }
}
