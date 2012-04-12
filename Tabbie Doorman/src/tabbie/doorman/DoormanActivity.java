package tabbie.doorman;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;

public class DoormanActivity extends FragmentActivity
{
	protected static final String LIST_TAG = "list";
	protected static final String LIST_SAVE_FILE = "guest_list";
	protected static final String OP_ADD_GUEST = "addGuest";
	private FragmentTransaction transaction;
	
	@Override
    protected void onCreate(final Bundle savedInstanceState)
    {
		Log.v("Activity.onCreate", "Called");
        super.onCreate(savedInstanceState);
    	this.setContentView(R.layout.main);
    	
    	if(transaction == null)
    	{
    		transaction = this.getSupportFragmentManager().beginTransaction();
    	}
    	
    	if(savedInstanceState==null)
    	{
    		ArrayList<Promoter> test = new ArrayList<Promoter>();
    		test.add(new Promoter("Justin Knutson", "JAK", 12));
    		test.add(new Promoter("Valeri Karpov", "VAL", 23));
    		transaction.replace(R.id.main_view, new AddGuestFragment(test));
    		transaction.commit();
    	}
    	
    	/*
    	if(savedInstanceState==null)
    	{
    		final LoginFragment userLogin = new LoginFragment();
    		final FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();
    		transaction.replace(R.id.main_view, userLogin);
    		transaction.commit();
    	}*/
    	
    	/*
    	 * Try to restore the file SAVELISTFILE. If restore is
    	 * unsuccessful, this will return false and the if statement
    	 * will evaluate to true. Therefore the if statement
    	 * will NOT execute unless savedInstanceState IS null
    	 * AND there is no saved file to restore
    	 */
    	/*
    	if(savedInstanceState==null&!this.restoreFile(LIST_SAVE_FILE))
    	{
	        final LoginFragment userLogin = new LoginFragment();
	        final FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();
	        transaction.replace(R.id.main_view, userLogin);
	        transaction.commit();
    	}
    	*/
    }
	
	@Override
	protected void onStart()
	{
		Log.v("Activity.onStart", "Called");
		super.onStart();
	}
	
	@Override
	protected void onRestart()
	{
		Log.v("Activity.onRestart", "Called");
		super.onRestart();
	}
	
	@Override
	protected void onResume()
	{
		Log.v("Activity.onResume", "Called");
		super.onResume();
	}
	
	@Override
	protected void onPause()
	{
		Log.v("Activity.onPause", "Called");
		super.onPause();
	}
	
	@Override
	protected void onStop()
	{
		Log.v("Activity.onStop", "Called");
		
		/*
		 * When the activity is stopped, we need to save
		 * all of the current data, including pending
		 * commands to the server. 
		 */
		
		try
		{
			final GuestListFragment list = (GuestListFragment) getSupportFragmentManager().findFragmentByTag(LIST_TAG);
			/*
			 * If findFragmentByTag evaluated to NULL
			 * be sure we don't throw a nullPointerException
			 * by trying to save a fragment that doesn't exist
			 */
			
			if(list!=null)
			{
				final FileOutputStream fos = openFileOutput(LIST_SAVE_FILE, Context.MODE_PRIVATE);
				final String writable = list.toJsonObject().toString();
				
				Log.v("DoormanActivity.onDestroy()", "Writing to file: " + writable);
				
				try
				{
					fos.write(writable.getBytes());
					fos.close();
				} 
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		catch (FileNotFoundException e)
		{
			/*
			 * This statement should NEVER be executed
			 */
			Log.e("DoormanActivity.onDestroy()", "File not found");
			e.printStackTrace();
		}
		super.onStop();
	}
	
	@Override
	protected void onDestroy()
	{
		Log.v("Activity.onDestroy", "Called");
		super.onDestroy();
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		Log.v("DoormanActivity.onSaveInstanceState()", "Called");
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		Log.v("DoormanActivity.onResumeInstanceState", "Called");
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		Log.v("DoormanActivity.onCreateOptionsMenu", "Called");
		return true;
	}
	
	@Override
	public void onBackPressed()
	{
		Log.v("DoormanActivity.onBackPressed()", "Called");
		super.onBackPressed();
	}
	
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent)
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
        	final GuestListFragment list = (GuestListFragment) getSupportFragmentManager().findFragmentByTag(LIST_TAG);
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
    
    private boolean restoreFile(String filename)
    {
    	Log.v("DoormanActivity.restoreFile()", "Called");
    	String content = "";
    	try
    	{
    		final FileInputStream fis = openFileInput(filename);
    		byte[] input = new byte[fis.available()];
    		while(fis.read(input)!=-1)
    		{
    			content += new String(input);
    		}
    		Log.v("DoormanActivity.restoreFile()", "Content Restored is: " + content);
    		/*
    		 * Once our list is loaded from memory, we can delete the file (hopefully)
    		 * This may be a source of unforeseen errors in the future
    		 */
    		
    		deleteFile(filename);
    		/*
    		 * This will return true if the Guest List instantiated correctly
    		 */
    		return this.instantiateGuestList(content);
    	}
    	catch(FileNotFoundException e)
    	{
    		Log.v("DoormanActivity.restoreFile()", "File does not exist, will return false");
    		/*
    		 * This exception will be thrown every time the app is started from
    		 * a logged out state. Ideally there would be a more robust handler
    		 * for when the file does not exist, but for now this works and
    		 * returns FALSE when there is no file to read from 
    		 */
    		e.printStackTrace();
    	}
    	catch (IOException e)
    	{
			e.printStackTrace();
		}
    	return false;
    }
    
    private boolean instantiateGuestList(String content)
    {
		try
		{
			final JSONObject savedObject = (JSONObject) new JSONTokener(content).nextValue();
			
			final JSONArray promotersArray = (JSONArray) savedObject.get("promoters");
			final JSONArray guestsArray = (JSONArray) savedObject.get("data");
			final String name = (String) savedObject.getString("name");
			
			ArrayList<List<NameValuePair>> pendingCommands = null;
			
			/*
			 * It's possible we had no pending commands, in which case
			 * we will want to skip this piece of code entirely
			 */
			if(savedObject.has("pending"))
			{
				final JSONArray pendingCommandsArray = (JSONArray) savedObject.get("pending");
				
				Log.v("Making", "Pending Commands");
				
				pendingCommands = JSONTOPENDINGCOMMANDS(pendingCommandsArray);
			}
			
			Log.v("Making", "Promoters Array");
			
			final ArrayList<Promoter> promoterList = JSONTOPROMOTERLIST(promotersArray);
			
			Log.v("Making", "Guests Array");
			
			final ArrayList<Guest> guestList = JSONTOGUESTLIST(guestsArray, promoterList);
			
			final GuestListFragment list = new GuestListFragment(name, guestList, promoterList);
			/*
			 * If pendingCommands is null here, the constructor
			 * of ServerPoller will ignore it, no worries
			 */
			list.setServerPoller(pendingCommands);
			final FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();
		    transaction.replace(R.id.main_view, list, LIST_TAG);
		    transaction.commit();
			return true;
		}
		catch (JSONException e)
		{
			Log.v("DoormanActivity.instantiateGuestList()", "There was an error parsing the JSON, returning FALSE");
			e.printStackTrace();
			return false;
		}
    }
    
    /*
     * The following static methods are used to parse JSON
     * anywhere it is needed in the application. It should have
     * robust error and exception handling to deal with JSON
     * errors
     */
    protected static ArrayList<Promoter> JSONTOPROMOTERLIST(final JSONArray data)
    {
    	Log.v("DoormanActivity.JSONTOPROMOTERLIST()", "Called");
    	final ArrayList<Promoter> promoterList = new ArrayList<Promoter>();
    	try
    	{
			for(int j = 0; j < data.length(); j++)
			{
				Log.v("DoormanActivity.JSONTOPROMOTERLIST()", "Instantiating promoter number " + j);
				final JSONObject promoter = data.getJSONObject(j);
				promoterList.add(new Promoter(promoter.getString("p_display"),
						promoter.getString("p_code"),
						promoter.getInt("p_id")));
			}
    	}
    	catch(JSONException e)
    	{
    		e.printStackTrace();
    	}
    	return promoterList;
    }
    
    protected static ArrayList<Guest> JSONTOGUESTLIST(final JSONArray data, final ArrayList<Promoter> promoterList)
    {
    	Log.v("DoormanActivity.JSONTOGUESTLIST()", "Called");
		final ArrayList<Guest> guestList = new ArrayList<Guest>();
		try
		{
			for(int i = 0; i < data.length(); i++)
			{
				Log.v("DoormanActivity.JSONTOGUESTLIST()", "On guest number " + i);
				final JSONObject guest = data.getJSONObject(i);
				final String promoterId = guest.getString("p_code");
				/*
				 * We need to assign a promoter to each Guest.
				 * If the guest doesn't have a promoter, we'll create
				 * a default promoter with no name, no tag, and an identifier
				 * equal to -1 (should be unused)
				 */
				Promoter promoter = null;
				for(Promoter p : promoterList)
				{
					if(p.getTag().contentEquals(promoterId))
					{
						promoter = p;
						break;
					}
				}
				if(promoter==null)
				{
					/*
					 * There may be a better way of doing this
					 * instead of creating a bunch of unused
					 * Promoter objects
					 */
					promoter = new Promoter("", "", -1);
				}
				
				guestList.add(new Guest(guest.getString("v_first"),
						guest.getString("v_last"),
						guest.getInt("v_nguests"),
						guest.getInt("v_id"),
						((guest.get("v_checked") instanceof Boolean) ? guest.getBoolean("v_checked") : (guest.getInt("v_checked")!=0)),
						promoter));
			}
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
    	return guestList;
    }
    
    protected static ArrayList<List<NameValuePair>> JSONTOPENDINGCOMMANDS(JSONArray data)
    {
    	Log.v("DoormanActivity.JSONTOPENDINGCOMMANDS()", "Called");
    	final ArrayList<List<NameValuePair>> pendingCommands = new ArrayList<List<NameValuePair>>();
    	try
    	{
    		for(int i = 0; i < data.length(); i++)
    		{
    			final ArrayList<NameValuePair> command = new ArrayList<NameValuePair>();
    			command.add(new BasicNameValuePair("op", data.getJSONObject(i).getString("op")));
    			command.add(new BasicNameValuePair("v_id", data.getJSONObject(i).getString("v_id")));
    			command.add(new BasicNameValuePair("v_checked", data.getJSONObject(i).getString("v_checked")));
    			pendingCommands.add(command);
    		}
    	}
    	catch (JSONException e)
    	{
			e.printStackTrace();
		}
    	return pendingCommands;
    }
}