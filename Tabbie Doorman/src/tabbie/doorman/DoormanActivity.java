package tabbie.doorman;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;

public class DoormanActivity extends FragmentActivity
{
	protected static final String SERVER_THREAD = "callServer";
	protected static final String PATRON_LIST_FRAGMENT = "patronList", SELECT_LIST_FRAGMENT = "lists";
	protected static final String PATRON_LIST_INFO = "guest_list", SELECT_LIST_INFO = "list"/*, LOGIN_INFO = "key"*/;
	private final ServerHandlerThread serverCaller = new ServerHandlerThread(SERVER_THREAD);
	private final ConnectionChangeReceiver connectivityReceiver = new ConnectionChangeReceiver();
	private Handler serverHandler;
	private FragmentTransaction transaction;
	
	// Called when the activity is created
	
	@Override
    protected void onCreate(final Bundle savedInstanceState)
    {
		Log.v("Activity.onCreate", "Called");
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
    	
    	// Listen for connectivity changes on a dedicated Receiver
    	registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    	
    	// Start the worker thread to send and receive messages from the server
    	serverCaller.start();
    	
    	// Now that the worker thread has started, get a handler for it
    	serverHandler = serverCaller.getHandler();
    	
    	/*
    	 * If there is a List Name available,
    	 * attempt to download the list
    	 * 
    	 * If the download attempt is unsuccessful
    	 * or if the List Name was null, attempt
    	 * to download the list of lists
    	 * 
    	 * If the download attempt is unsuccessful
    	 * or if the Encryption Key is null, begin a new
    	 * LoginFragment with the stored UserName and Password
    	 */
    	
    	if(savedInstanceState==null)
    	{
    		// Restore content and then destroy the local save cache
        	final JSONObject patronListInfo = restorePatronListInfo();
        	final JSONObject selectListInfo = restoreListSelectInfo();
        	deleteSaveCache();
        	
        	if(patronListInfo!=null)
        	{
        		// Build Guest List Fragment
        	}
        	else if(selectListInfo!=null)
        	{
        		// Recreate ListListFragment from memory
        		final SelectListFragment lazarusList = SelectListFragment.recreate(selectListInfo);
        		
	    		if(transaction==null)
	    		{
	    			transaction = getSupportFragmentManager().beginTransaction();
	    		}
	    		transaction.replace(R.id.main_view, lazarusList);
	    		transaction.commit();
	    		transaction = null;
        	}
        	else
        	{
        		// Fuck it, we'll just make him log in
	    		if(transaction==null)
	    		{
	    			transaction = getSupportFragmentManager().beginTransaction();
	    		}
	    		transaction.replace(R.id.main_view, new LoginFragment());
	    		transaction.commit();
	    		transaction = null;
        	}
    	}
    }
	
	@Override
	protected void onDestroy()
	{
		// Clear all pending commands
		serverHandler.removeCallbacksAndMessages(null);
		
		// Get rid of our HandlerThread
		serverCaller.quit();
		
		// Unregister our connectivity broadcast receiver
		unregisterReceiver(connectivityReceiver);
		
		Log.v("Activity.onDestroy", "Called");
		super.onDestroy();
		
	}
	
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent keyEvent)
    {
    	Log.v("DoormanActivity.onKeyDown()", "Called");
    	/*
    	 * We need to intercept the Search button at
    	 * various points during the activity's lifecycle
    	 */
        switch(keyCode)
        {
        case KeyEvent.KEYCODE_SEARCH:
        	Log.v("DoormanActivity.onKeyDown()", "KEYCODE_SEARCH");
        	final PatronListFragment list = (PatronListFragment) getSupportFragmentManager().findFragmentByTag(PATRON_LIST_FRAGMENT);
        	/*
        	 * We only care about the search button if there
        	 * is a GuestListFragment and it is currently visible
        	 */
        	if(list!=null&&list.isVisible())
        	{
        		list.toggleSearch();
        	}
        	return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }
    
    private String restoreFileContent(final String filename)
    {
    	String content = "";
    	try
    	{
    		final FileInputStream fis = openFileInput(filename);
    		byte[] input = new byte[fis.available()];
    		while(fis.read(input)!=-1)
    		{
    			content += new String(input);
    		}
    	}
    	catch(FileNotFoundException e)
    	{
    		e.printStackTrace();
    	}
    	catch (IOException e)
    	{
			e.printStackTrace();
		}
    	
    	return content;
    }
    
    protected void sendCommand(final Command command)
    {
    	final Message message = serverHandler.obtainMessage();
    	message.obj = command;
    	serverHandler.sendMessage(message);
    }
    
    protected void sendCommand(final Command command, final long uptimeMillis)
    {
    	final Message message = serverHandler.obtainMessage();
    	message.obj = command;
    	serverHandler.sendMessageAtTime(message, uptimeMillis);
    }
    
    protected JSONObject restorePatronListInfo()
    {
    	final JSONObject patronListInfo = null;
    	return patronListInfo;
    }
    
    protected JSONObject restoreListSelectInfo()
    {
    	JSONObject listInfo = null;
    	
    	if(hasFileInfo(SELECT_LIST_INFO))
    	{
    		final String savedData = restoreFileContent(SELECT_LIST_INFO);
    		try
    		{
				listInfo = (JSONObject) new JSONTokener(savedData).nextValue();
    		}
    		catch (JSONException e)
    		{
				e.printStackTrace();
			}
    	}
    	return listInfo;
    }
    
    private boolean hasFileInfo(final String fileName)
    {
    	final String[] files = fileList();
    	for(final String f : files)
    	{
    		if(f.contentEquals(fileName))
    		{
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * Cleanup method to ensure all locally saved content is deleted
     */
    protected void deleteSaveCache()
    {
		// this.deleteFile(DoormanActivity.LOGIN_INFO);
		this.deleteFile(DoormanActivity.SELECT_LIST_INFO);
		this.deleteFile(DoormanActivity.PATRON_LIST_INFO);
    }
    
    private class ConnectionChangeReceiver extends BroadcastReceiver
    {
    	@Override
    	public void onReceive(final Context context, final Intent intent)
    	{
    		Log.v("ConnectionChangeReceiver", "Connectivity Changed");
    		
    		final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    		final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
    		
    		if(activeNetInfo!=null && activeNetInfo.isConnected())
    		{
				if(serverCaller.isPaused())
				{
					synchronized(serverCaller)
					{
						serverCaller.unPause();
					}
				}
    		}
    		else
    		{
    			serverCaller.pause();
    		}
    	}
    }
}