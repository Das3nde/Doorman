package tabbie.doorman;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class SelectListFragment extends Fragment implements Handler.Callback, OnItemClickListener
{
	private static final String TAG = "SelectListFragment";
	private static final String LOADING = "Loading, please wait...";
	private final ArrayList<SelectableList> lists = new ArrayList<SelectableList>();
	private final Handler handler = new Handler(this);
	private AlertDialog logoutDialog;
	private String key;
	private PatronListFragment currentGuestList;
	private String requestedListName;
	private Toast error;
	private ProgressDialog loadingDialog;
	private boolean hasPendingCommands = false;
	
	// Default public constructor
	public SelectListFragment(){};
	
	
	protected SelectListFragment(final ArrayList<SelectableList> mLists, final String key)
	{
		// Completely new reference to the list of lists passed here
		this.lists.addAll(mLists);
		this.key = key;
	}
	
	/**
	 * Convenient static method that takes care of instantiating a new
	 * SelectListFragment from JSON data
	 * 
	 * @param data - the data recovered from a save file
	 * @return - a new SelectListFragment created from JSON data
	 */
	protected static SelectListFragment recreate(final JSONObject data)
	{
		ArrayList<SelectableList> lists = new ArrayList<SelectableList>();
		String key = null;
		JSONArray entities = null;
		
		try
		{
			key = data.getString("key");
			entities = data.getJSONArray("data");
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
		finally
		{
			int length = entities.length();
			for(int i = 0; i < length; i++)
			{
				try
				{
					final JSONObject list = (JSONObject) entities.get(i);
					lists.add(new SelectableList(list.getString("name"), list.getInt("id"), list.getString("display")));
				}
				catch(JSONException e)
				{
					e.printStackTrace();
				}
			}
		}
		return new SelectListFragment(lists, key);
	}
	
	@Override
	public void onResume()
	{
		// During a configuration change, we may hold off on
		// finishing a command. Check here to make sure we finish
		// processing any pending commands
		finishProcessingCommand();
		super.onResume();
	}
	
	@Override
	public void onDestroy()
	{
		// Stop
		try
		{
			final JSONObject storable = new JSONObject();
			final JSONArray savedData = new JSONArray(); 
			for(final SelectableList e : lists)
			{
				savedData.put(e.toJson());
			}
			storable.put("data", savedData);
			storable.put("key", key);
			
			final String writable = storable.toString();
			final FileOutputStream fos = getActivity().openFileOutput(DoormanActivity.SELECT_LIST_INFO, Context.MODE_PRIVATE);
			fos.write(writable.getBytes());
			fos.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		// Hammertime
		Log.v(TAG, "OnDestroy");
		super.onDestroy();
	}
	
	@Override
	public void onDetach()
	{
		Log.v(TAG, "OnDetach");
		dismissLoader();
		if(logoutDialog!=null)
		{
			logoutDialog.dismiss();
		}
		super.onDetach();
	}
	
	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		if(logoutDialog!=null)
		{
			logoutDialog.show();
		}
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		this.setRetainInstance(true);
		this.setHasOptionsMenu(true);
		
		// Make sure we delete all our saved data when this fragment is created 
		((DoormanActivity) getActivity()).deleteSaveCache();
		
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		if(hasPendingCommands)
		{
			displayLoader();
		}
		final View fragmentDisplay = inflater.inflate(R.layout.select_list, null);
		final ListView listListView = (ListView) fragmentDisplay.findViewById(R.id.list_list_view);
		final SimpleAdapter listListAdapter = new SimpleAdapter(getActivity(),
				lists,
				R.layout.select_list_element,
				new String[] {"display"},
				new int[] {R.id.list_list_display});
		listListView.setAdapter(listListAdapter);
		listListView.setOnItemClickListener(this);
		return fragmentDisplay;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.list_list_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		if(item.getItemId()==R.id.list_log_out)
		{
			// We'll build this dialog every time we want to display it
			// to make sure it's attached to a valid Activity
			if(logoutDialog==null)
			{
				final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
				dialogBuilder.setMessage("Are you sure you want to log out?");
				dialogBuilder.setPositiveButton("Yes", new OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						final FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
						transaction.replace(R.id.main_view, new LoginFragment());
						transaction.commit();
						logoutDialog = null;
					}
				});
				dialogBuilder.setNegativeButton("no", new OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
						logoutDialog = null;
					}
				});
				logoutDialog = dialogBuilder.create();
			}
			logoutDialog.show();
			return true;
		}
		return false;
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		hasPendingCommands = false;
		if(msg.obj instanceof java.lang.String)
		{
			final String response = (String) msg.obj;
			Log.v("ListListFragment", "Response is: " + response);
			ArrayList<Promoter> promoters;
			ArrayList<Patron> patrons;
			ArrayList<Guest> guests;
			
			try
			{
				final JSONObject listsObject = (JSONObject) new JSONTokener(response).nextValue();
				if(listsObject.has("error"))
				{
					error = Toast.makeText(getActivity(), "Invalid name/password", Toast.LENGTH_LONG);
				}
				else
				{
					promoters = buildPromoterList(listsObject.getJSONArray("promoters"));
					
					patrons = buildPatronList(listsObject.getJSONArray("data"), promoters);
					
					guests = buildGuestList(listsObject.getJSONArray("data"));
					
					currentGuestList = new PatronListFragment(requestedListName, guests, patrons, promoters);
				}
			}
			catch(JSONException e)
			{
				error = Toast.makeText(getActivity(), "Error formatting server response", Toast.LENGTH_LONG);
			}
		}
		else if(msg.obj instanceof Command)
		{
			error = Toast.makeText(getActivity(), "Unable to connect to server", Toast.LENGTH_LONG);
		}
		
		dismissLoader();
		
		if(isResumed())
		{
			finishProcessingCommand();
		}
		return true;
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
	{
		final ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo status = (NetworkInfo) cm.getActiveNetworkInfo();
		if(status!=null && status.isConnected())
		{
			// Later on we need to pass this information to the PatronListFragment so it can request refresh updates
			requestedListName = lists.get(position).getName();
			try
			{
				final Command loadList = Command.loadList(requestedListName,
						key,
						handler);
				((DoormanActivity) getActivity()).sendCommand(loadList);
				hasPendingCommands = true;
				displayLoader();
			}
			catch(UnsupportedEncodingException e)
			{
				error = Toast.makeText(getActivity(), "Error, unable to create Command.loadList", Toast.LENGTH_SHORT);
				error.show();
				error = null;
			}
		}
		else
		{
			error = Toast.makeText(getActivity(), "No Internet Connection", Toast.LENGTH_SHORT);
			error.show();
			error = null;
		}
	}
	
	private void finishProcessingCommand()
	{
		if(error!=null)
		{
			error.show();
			error = null;
		}
		// This occasionally crashes the program if a configuration change happens
		// as the transaction is being committed
		else if(currentGuestList!=null)
		{
			final FragmentTransaction transaction = ((DoormanActivity) getActivity()).getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.main_view, currentGuestList, DoormanActivity.PATRON_LIST_FRAGMENT);
			transaction.commit();
		}
	}
	
	private void displayLoader()
	{
		loadingDialog = ProgressDialog.show(getActivity(), null, LOADING);
	}
	
	private void dismissLoader()
	{
		if(loadingDialog!=null && loadingDialog.isShowing())
		{
			loadingDialog.dismiss();
		}
	}
	
	/**
	 * Convenience method for building an ArrayList of Promoter objects
	 * from a suitable JSON String
	 * @param data - the JSON String to be processed
	 * @return a new ArrayList of Promoter objects
	 */
	protected static ArrayList<Promoter> buildPromoterList(final JSONArray data)
	{
    	final ArrayList<Promoter> promoterList = new ArrayList<Promoter>();
    	try
    	{
			for(int j = 0; j < data.length(); j++)
			{
				final JSONObject promoter = data.getJSONObject(j);
				promoterList.add(new Promoter(promoter.getString("p_display"),
						promoter.getString("p_code"),
						promoter.getInt("p_id"),
						promoter.getInt("p_nguests"),
						promoter.getInt("p_nguests_checked")));
			}
    	}
    	catch(JSONException e)
    	{
    		e.printStackTrace();
    	}
    	return promoterList;
	}
	
	/**
	 * Convenience method for building an ArrayList of Patron objects
	 * from JSON Data and a suitable list of Promoters
	 * @param data - the JSON String to be processed
	 * @param promoterList - the Promoter ArrayList corresponding to these data
	 * @return a new ArrayList of Patron objects
	 */
	protected static ArrayList<Patron> buildPatronList(final JSONArray data, final ArrayList<Promoter> promoterList)
	{
		final ArrayList<Patron> patronList = new ArrayList<Patron>();
		try
		{
			for(int i = 0; i < data.length(); i++)
			{
				final JSONObject patron = data.getJSONObject(i);
				
				// If the "patron" has a parent, then it is not a Patron but a regular old Guest!
				if(patron.getInt("v_parent")==-1)
				{
					final String promoterId = patron.getString("p_code");
					
					// Find out which Promoter is associated with this Patron
					Promoter promoter = null;
					for(final Promoter p : promoterList)
					{
						if(p.getTag().contentEquals(promoterId))
						{
							promoter = p;
							break;
						}
					}
					
					final Patron tempPatron = new Patron(patron.getString("v_first"),
							patron.getString("v_last"),
							patron.getInt("v_nguests"),
							patron.getInt("v_nguests_checked"),
							patron.getInt("v_id"),
							(byte) patron.getInt("v_checked"),
							promoter);
					
					if(patron.getInt("v_deleted")==1)
					{
						Log.v("deleted is true", "Patron");
						tempPatron.isDeleted = true;
					}
					
					patronList.add(tempPatron);
				}
			}
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
    	return patronList;
	}
	
	/**
	 * Convenience method for building an ArrayList of Guest objects
	 * as distinguished from Patron objects
	 * @param data - generic JSON String of various derived Guest objects
	 * @return a new ArrayList of Guest objects
	 */
	protected static ArrayList<Guest> buildGuestList(final JSONArray data)
	{
		final ArrayList<Guest> guestList = new ArrayList<Guest>();
		try
		{
			for(int i = 0; i < data.length(); i++)
			{
				final JSONObject guest = data.getJSONObject(i);
				
				// Now we need to make sure our Guests DO have parents (Patrons)
				if(guest.getInt("v_parent")!=-1)
				{	
					final Guest tempGuest = new Guest(guest.getString("v_first"),
							guest.getString("v_last"),
							guest.getInt("v_id"),
							guest.getInt("v_parent"),
							(byte) guest.getInt("v_checked"));
					
					if(guest.getInt("v_deleted")==1)
					{
						Log.v("deleted is true", "Guest");
						tempGuest.isDeleted = true;
					}
					
					guestList.add(tempGuest);
				}
			}
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
    	return guestList;
	}
}