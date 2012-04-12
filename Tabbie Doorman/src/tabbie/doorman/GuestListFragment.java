package tabbie.doorman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;

public class GuestListFragment extends Fragment
{ 
	private final ArrayList<Guest> displayGuestList = new ArrayList<Guest>();
	private final Timer updater = new Timer();
	private final Handler listHandler = new Handler();
	private String name;
	private ServerPoller poller;
	private LinearLayout searchElement;
	private InputMethodManager imm;
	private EditText searchBar;
	private GuestListAdapter guestListAdapter;
	
	private ArrayList<Guest> masterGuestList;
	private ArrayList<Promoter> promoters;

	private byte checked = Guest.ALL;
	private boolean reversed = false;
	private Promoter promoter = Guest.ALLPROMOTERS;
	
	protected boolean searchVisible = false;

	
	private OnKeyListener backKeyListener = new OnKeyListener()
	{
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event)
		{
			if(keyCode == KeyEvent.KEYCODE_BACK && searchElement.getVisibility()==View.VISIBLE)
			{
				Log.v("GuestListFragment", "Back key detected");
				toggleSearch();
				return true;
			}
			
			else
			{
				return false;
			}
		}
		
	};
	
	private final OnItemClickListener guestCheckedListener = new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> guestList, View guestListElement, int position, long id)
		{
			final Guest checkedGuest = displayGuestList.get(position);
			/*
			 * Updating displayGuestList automatically will
			 * update masterGuestList because it is a CHILD
			 */
			checkedGuest.onClick();
			poller.postCheckedUpdate(checkedGuest.getGuestId(), checkedGuest.isCheckedIn());
			guestListAdapter.notifyDataSetChanged();
		}
	};
	
	/*
	 * Public empty constructor used for when the
	 * fragment is recreated from memory
	 */
	
	public GuestListFragment(){}
	
	/*
	 * Public parameterized constructor used
	 * for when the fragment is called from
	 * a user login
	 */
	
	public GuestListFragment(final String mName, final ArrayList<Guest> mGuests, final ArrayList<Promoter> mPromoters)
	{
		this.name = mName;
		this.masterGuestList = mGuests;
		this.promoters = mPromoters;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		if(poller == null)
		{
			poller = new ServerPoller(this, null);
		}
		updater.schedule(poller, 20000, 20000);
		/*
		 * If this fragment has previously been saved
		 * (in other words, being created from memory)
		 * we need to recover the guest list
		 */
		
		if(savedInstanceState!=null)
		{
			name = savedInstanceState.getString("name");
			masterGuestList = savedInstanceState.getParcelableArrayList("guestList");
			promoters = savedInstanceState.getParcelableArrayList("promoterList");
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{	
		final View v = inflater.inflate(R.layout.guest_list, null);
		final GridView guestList = (GridView) v.findViewById(R.id.guest_list);
		
		final Button clearSearchButton = (Button) v.findViewById(R.id.search_clear_button);
		
		clearSearchButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				searchBar.setText("");
				
			}
		});
		
		searchElement = (LinearLayout) v.findViewById(R.id.search_bar_container);
		searchElement.setOnKeyListener(backKeyListener);
		
		searchBar = (EditText) v.findViewById(R.id.search_bar);
		searchBar.setOnKeyListener(backKeyListener);
		
		searchBar.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){}
			
			@Override
			public void afterTextChanged(Editable s)
			{
				Log.v("GuestListFragment.TextWatcher()", "Text is " + searchBar.getEditableText().toString());
				sortList();
				guestListAdapter.notifyDataSetChanged();
			}
		});
		
		sortList();
		
		guestListAdapter = new GuestListAdapter(getActivity(),
				displayGuestList,
				R.layout.guest_list_element,
				new String[] {"name", "promoter", "guests"},
				new int[] {R.id.guest_list_element_name, R.id.guest_list_element_promoter, R.id.guest_list_element_guests});
		guestList.setAdapter(guestListAdapter);
		guestList.setOnItemClickListener(guestCheckedListener);
		return v;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// updater.cancel();
		// poller.run();
		if(masterGuestList!=null)
		{
			outState.putString("name", name);
			outState.putParcelableArrayList("guestList", masterGuestList);
			outState.putParcelableArrayList("promoterList", promoters);
		}
		
		super.onSaveInstanceState(outState);
	}
	
	protected void sortList()
	{
		displayGuestList.clear();
		displayGuestList.addAll(masterGuestList);
		
		if(checked!=Guest.ALL)
		{
			final boolean checkedIn = (checked!=Guest.UNCHECKED);
			for(final Iterator<Guest> itr = displayGuestList.iterator(); itr.hasNext();)
			{
				if(itr.next().isCheckedIn()!=checkedIn)
				{
					itr.remove();
				}
			}
		}
		
		if(promoter!=Guest.ALLPROMOTERS)
		{
			for(final Iterator<Guest> itr = displayGuestList.iterator(); itr.hasNext();)
			{
				/*
				 * If we have the same promoter here, remove the
				 * guest from the list
				 */
				if(itr.next().getPromoter().compareTo(promoter)!=0)
				{
					itr.remove();
				}
			}
		}
		
		Collections.sort(displayGuestList);
		
		if(reversed)
		{
			Collections.reverse(displayGuestList);
		}
		
		filter();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.list_menu, menu);
		
		switch(checked)
		{
		case(Guest.CHECKED):
			menu.findItem(R.id.checked_in_checked).setChecked(true);
			break;
		
		case(Guest.UNCHECKED):
			menu.findItem(R.id.checked_in_unchecked).setChecked(true);
			break;
		
		default:
			menu.findItem(R.id.checked_in_all).setChecked(true);
		}
		
		final SubMenu partySortMenu = menu.getItem(1).getSubMenu();
		partySortMenu.add(R.id.party_sort, -1, SubMenu.NONE, "All");
		for(Promoter p : promoters)
		{
			partySortMenu.add(R.id.party_sort, p.getId(), SubMenu.NONE, p.toString());
		}
		partySortMenu.setGroupCheckable(R.id.party_sort, true, true);
		partySortMenu.findItem(promoter.getId()).setChecked(true);
		
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getGroupId()==R.id.party_sort)
		{
			item.setChecked(true);
			promoter = findPromoterById(item.getItemId());
			sortList();
		}
		else
		{
			switch(item.getItemId())
			{
				case(R.id.a_z_sort):
					if(reversed)
					{
						item.setIcon(R.drawable.az_sort);
						item.setTitle("A-Z Sort");
					}	
					else
					{
						item.setIcon(R.drawable.za_sort);
						item.setTitle("Z-A Sort");
					}
					reversed = !reversed;
					break;
					
				case(R.id.checked_in_all):
					checked = Guest.ALL;
					item.setChecked(true);
					break;
					
				case(R.id.checked_in_checked):
					checked = Guest.CHECKED;
					item.setChecked(true);
					break;
					
				case(R.id.checked_in_unchecked):
					checked = Guest.UNCHECKED;
					item.setChecked(true);
					break;
				
				case(R.id.search):
					toggleSearch();
					break;
				
				case(R.id.log_out):
					AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
					dialog.setMessage("Are you sure you want to log out?");
					dialog.setPositiveButton("Yes", new OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							if(poller.hasPendingCommands())
							{
								poller.forceExecute();
							}
							final LoginFragment userLogin = new LoginFragment();
							final FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
							transaction.replace(R.id.main_view, userLogin);
							transaction.commit();
						}
					});
					dialog.setNegativeButton("no", new OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.dismiss();
						}
					});
					dialog.show();
					break;
				
				case(R.id.metrics):
					final int expected = masterGuestList.size();
					final int guests = getNumGuests();
					final int total = (expected + guests);
					final int numChecked = getNumCheckedIn();
					final int numNotChecked = (expected - numChecked);
					final int numCheckedWithGuests = getAttendance();
					final int numKoreans = kimLeePark();
					
					String[] items = new String[]{"Patrons Expected: " + expected,
							"Guests Expected: " + guests,
							"Total Expected: " + total,
							"Patrons Present: " + numChecked,
							"Patrons Not Present " + numNotChecked,
							"Total Present: " + numCheckedWithGuests
					};
					
					if(searchBar.getEditableText().toString().toLowerCase().contains("knutson"))
					{
						String[] temp = new String[items.length+1];
						System.arraycopy(items, 0, temp, 0, items.length);
						temp[items.length] = "Total Koreans: " + numKoreans;
						items = temp;
					}
					
					AlertDialog.Builder metrics = new AlertDialog.Builder(getActivity());
					metrics.setItems(items, null);
					metrics.show();
					break;
					
				default:
					return super.onOptionsItemSelected(item);
			}
		}
		sortList();
		guestListAdapter.notifyDataSetChanged();
		return true;
	}
	
	protected void toggleSearch()
	{
		if(imm==null)
		{
			imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);;
		}
		
		if(searchVisible)
		{
			
			/*
			 * Hide the soft keyboard so the user doesn't have to deal with it
			 */
			imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
			/*
			 * Hide the search bar from the screen
			 * so the gridview can take up as much room
			 * as possible
			 */
			searchElement.setVisibility(View.GONE);
			/*
			 * Change the variable for this method
			 */
			searchVisible = false;
			/*
			 * Clear the search field so the list can repopulate
			 */
			searchBar.setText("");
			/*
			 * Re-sort the list
			 */
			sortList();
			/*
			 * Notify the adapter that the list has been sorted
			 */
			guestListAdapter.notifyDataSetChanged();
		}
		
		else
		{
			searchElement.setVisibility(View.VISIBLE);
			searchVisible = true;
			searchElement.requestFocus();
		}
	}
	
	private void filter()
	{
		final String query = searchBar.getEditableText().toString();
		
		if(query != null && query.length() != 0)
		{
			final String formattedQuery = query.toLowerCase();
			for(final Iterator<Guest> itr = displayGuestList.iterator(); itr.hasNext();)
			{
				final Guest guest = itr.next();
				
				searchRoutine:
				if(guest!=null)
				{
					final String str = (String) guest.get("name");
					final String[] words = str.split(" ");
					for(final String word : words)
					{
						if(word.toLowerCase().contains(formattedQuery))
						{
							break searchRoutine;
						}
					}
					itr.remove();
				}
			}
		}
	}
	
	private Promoter findPromoterById(int id)
	{
		for(Promoter p : promoters)
		{
			if(p.getId()==id)
			{
				return p;
			}
		}
		return Guest.ALLPROMOTERS;
	}
	
	
	protected String getName()
	{
		return name;
	}
	
	protected Handler getHandler()
	{
		return listHandler;
	}
	
	protected ArrayList<Guest> getMasterList()
	{
		return masterGuestList;
	}
	
	protected ArrayList<Promoter> getPromoters()
	{
		return promoters;
	}
	
	protected JSONObject toJsonObject()
	{
		final JSONObject saveData = new JSONObject();
		final JSONArray savePromoters = new JSONArray();
		final JSONArray saveGuests = new JSONArray();
		
		for(Promoter p : promoters)
		{
			savePromoters.put(p.toJsonObject());
		}
		Log.v("savePromoters", savePromoters.toString());
		
		for(Guest g : masterGuestList)
		{
			saveGuests.put(g.toJsonObject());
		}
		Log.v("saveGuests", saveGuests.toString());
		
		poller.cancel();
		try
		{
			saveData.put("promoters", savePromoters);
			saveData.put("data", saveGuests);
			saveData.put("pending", poller.parcelPendingCommands());
			saveData.put("name", name);
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
		return saveData;
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
	}
	
	@Override
	public void onDestroy()
	{
		Log.v("Canceling", "Updater");
		updater.cancel();
		super.onDestroy();
	}
	
	protected void setServerPoller(ArrayList<List<NameValuePair>> pendingCommands)
	{
		poller = new ServerPoller(this, pendingCommands);
	}
	
	private int getNumCheckedIn()
	{
		int count = 0;
		for(Guest g : masterGuestList)
		{
			if(g.isCheckedIn())
			{
				count++;
			}
		}
		return count;
	}
	
	private int getNumGuests()
	{
		int count = 0;
		for(Guest g : masterGuestList)
		{
			count += g.getNumGuests();
		}
		return count;
	}
	
	private int getAttendance()
	{
		int count = 0;
		for(Guest g : masterGuestList)
		{
			if(g.isCheckedIn())
			{
				count+=(g.getNumGuests() + 1);
			}
		}
		return count;
	}
	
	private int kimLeePark()
	{
		int count = 0;
		for(Guest g : masterGuestList)
		{
			final String lastName = g.getLastName();
			if(lastName.contentEquals("Lee") || lastName.contentEquals("Kim") || lastName.contentEquals("Park"))
			{
				count++;
			}
		}
		return count;
	}
}