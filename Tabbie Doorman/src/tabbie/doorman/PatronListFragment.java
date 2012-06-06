package tabbie.doorman;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PatronListFragment extends Fragment
{
	private final ArrayList<Patron> masterPatronList = new ArrayList<Patron>();
	private final ArrayList<Patron> displayPatronList = new ArrayList<Patron>();
	private final ArrayList<Promoter> promoters = new ArrayList<Promoter>();
	private final ArrayList<Guest> guests = new ArrayList<Guest>();
	private final ArrayList<Command> pendingCommands = new ArrayList<Command>();
	private Button plusButton, minusButton, addPatronButton;
	private TextView guestCounter, promoterHeader;
	private Command updateCommand;
	private AlertDialog logoutDialog, longClickDialog, guestDialog;
	private String name;
	private LinearLayout searchElement;
	private InputMethodManager imm;
	private EditText searchBar;
	private PatronListAdapter guestListAdapter;
	private int timeSinceLastUpdate = 20;
	private byte checked = Patron.ALL;
	private boolean reversed = false, searchVisible = false;
	private Promoter promoter = Patron.ALLPROMOTERS;
	
	// Empty default public constructor
	public PatronListFragment(){}
	
	public PatronListFragment(final String mName, final ArrayList<Guest> mGuests, final ArrayList<Patron> mPatrons, final ArrayList<Promoter> mPromoters)
	{
		this.name = mName;
		masterPatronList.addAll(mPatrons);
		promoters.addAll(mPromoters);
		guests.addAll(mGuests);
	}
	
	/**
	 * Handler for update commands
	 */
	private final Handler updateHandler = new Handler()
	{
		@Override
		public void handleMessage(final Message msg)
		{
			// Even empty updates will return as a String value
			if(msg.obj instanceof java.lang.String)
			{
				try
				{
					Log.v("Received an update string", "Handling string: " +(String) msg.obj);
					final JSONObject listsObject = (JSONObject) new JSONTokener((String) msg.obj).nextValue();
					if(listsObject.has("error"))
					{
						// Display error message
					}
					else
					{
						final JSONArray data = listsObject.getJSONArray("data");
						final JSONArray promoterData = listsObject.getJSONArray("promoters");
						
						final ArrayList<Promoter> tempPromoters = new ArrayList<Promoter>(SelectListFragment.buildPromoterList(promoterData));
						final ArrayList<Patron> tempPatrons = new ArrayList<Patron>(SelectListFragment.buildPatronList(data, promoters));
						final ArrayList<Guest> tempGuests = new ArrayList<Guest>(SelectListFragment.buildGuestList(data));
						
						promoterLoop:
						for(final Promoter p : tempPromoters)
						{
							int promoterListLength = promoters.size();
							for(int i = 0; i < promoterListLength; i++)
							{
								if(p.getId()==promoters.get(i).getId())
								{
									Log.v("Setting a new promoter", p.getName());
									promoters.set(i, p);
									continue promoterLoop;
								}
							}
						}

						patronLoop:
						for(final Patron g : tempPatrons)
						{
							int patronListLength = masterPatronList.size();
							for(int i = 0; i < patronListLength; i++)
							{
								Log.v("Now iterating through the master list", "At entry: " + i);
								Log.v("Comparing Ids", "New Patron: " + g.getPatronId() + ", Old Patron: " + masterPatronList.get(i).getPatronId());
								if(g.getPatronId()==masterPatronList.get(i).getPatronId())
								{
									Log.v("Found this patron in the master list!", g.getFirstName() + " " + g.getLastName());
									if(g.isDeleted)
									{
										Log.v("Now removing a Patron", g.getFirstName() + " " + g.getLastName());
										masterPatronList.remove(i);
									}
									else
									{
										Log.v("Now editing a Patron", g.getFirstName() + " " + g.getLastName());
										masterPatronList.set(i, g);
									}
									continue patronLoop;
								}
							}
							Log.v("Now adding a Patron", g.getFirstName() + " " + g.getLastName());
							masterPatronList.add(g);
						}
						
						
						guestLoop:
						for(Guest g : tempGuests)
						{
							int guestListLength = guests.size();
							for(int i = 0; i < guestListLength; i++)
							{
								if(g.getId()==guests.get(i).getId())
								{
									if(g.isDeleted)
									{
										guests.remove(i);
									}
									else
									{
										guests.set(i, g);
									}
									continue guestLoop;
								}
							}
							guests.add(g);
						}
						
						sortList();
						setPromoterHUD();
						guestListAdapter.notifyDataSetChanged();
					}
				}
				catch(JSONException e)
				{
					Toast.makeText(getActivity(), "JSONException in update", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
				timeSinceLastUpdate = 20;
			}
			else
			{
				timeSinceLastUpdate+=20;
				Toast.makeText(getActivity(), "Updating timer: " + timeSinceLastUpdate, Toast.LENGTH_LONG);
			}
			
			try
			{
				updateCommand = Command.update(name, timeSinceLastUpdate, updateHandler);
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			
			if(getActivity()!=null)
			{
				((DoormanActivity) getActivity()).sendCommand(updateCommand, SystemClock.uptimeMillis() + 20000);
			}
		}
	};
	

	/**
	 * Generic response handler for when a command is sent
	 * to the server for processing
	 */
	protected final Handler serverResponseHandler = new Handler()
	{
		@Override
		public void handleMessage(final Message msg)
		{
			pendingCommands.remove(0);
			Log.v("Pending Commands", "Is now length: " + pendingCommands.size());
			if(msg.obj instanceof java.lang.String)
			{
				Log.v("Checked in returned", (String) msg.obj);
			}
			else if(msg.obj instanceof Command)
			{
				Log.v("Checked in returned", "BAD");
			}
		}
	};
	
	/**
	 * Generic OnKeyListener for when a dialog is showing
	 * and the user presses the "back" button
	 */
	private final DialogInterface.OnKeyListener dismissDialogListener = new DialogInterface.OnKeyListener()
	{
		
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
		{
			if(keyCode == KeyEvent.KEYCODE_BACK)
			{
				logoutDialog = null;
				longClickDialog = null;
				guestDialog = null;
			}
			return false;
		}
	};
	
	/**
	 * Listener for a Long Click on a patron's name
	 * Options: View Patron, BlackList Patron, Remove Patron
	 */
	private final OnItemLongClickListener holdGuestListener = new OnItemLongClickListener()
	{
		@Override
		public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id)
		{
			final Patron tempPatron = displayPatronList.get(position);
			Log.v("Selected Guest", "DisplayListId = " + tempPatron.getPatronId());
			Log.v("Selected Guest", "MasterListId " + masterPatronList.get(position).getPatronId());
			String displayName = tempPatron.getFirstName() + " " + tempPatron.getLastName();
			
			final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
			
			if(displayName.length() < 15)
			{
				final String temp = new String(new char[15-displayName.length()]);
				displayName = displayName.concat(temp);
				Log.v("Display Name", displayName);
			}
			
			dialogBuilder.setTitle(displayName);
			dialogBuilder.setPositiveButton("Guests", new OnClickListener()
			{
				// ALL THIS SHIT NEEDS TO BE PUT IN A NEW FRAGMENT
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// Get rid of our dialog in case the user hits "back"
					longClickDialog.dismiss();
					longClickDialog = null;
					
					final GuestListFragment myGuestFragment = new GuestListFragment(tempPatron);
					final FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
					transaction.replace(R.id.main_view, myGuestFragment);
					transaction.addToBackStack(null);
					transaction.commit();
				}
				
			});
			dialogBuilder.setNeutralButton("Blacklist", new OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					tempPatron.toggleBlacklist();
					try
					{
						final Command checkGuest = Command.toggleChecked(tempPatron.getPatronId(),
								tempPatron.checkedInStatus(),
								name,
								serverResponseHandler);
						addCommand(checkGuest);
						((DoormanActivity) getActivity()).sendCommand(checkGuest);
					}
					catch(UnsupportedEncodingException e)
					{
						e.printStackTrace();
					}
					guestListAdapter.notifyDataSetChanged();
				}
			});
			dialogBuilder.setNegativeButton("Remove", new OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					try
					{
						final Command removePatron = Command.removeGuest(tempPatron.getPatronId(), serverResponseHandler);
						((DoormanActivity) getActivity()).sendCommand(removePatron);
						addCommand(removePatron);
					}
					catch (UnsupportedEncodingException e)
					{
						e.printStackTrace();
					}
					int length = masterPatronList.size();
					for(int i = 0; i < length; i++)
					{
						if(masterPatronList.get(i).getPatronId()==tempPatron.getPatronId())
						{
							Log.v("Removing Guest from master list", tempPatron.getFirstName() + " " + tempPatron.getLastName());
							masterPatronList.remove(i);
							sortList();
							guestListAdapter.notifyDataSetChanged();
							break;
						}
					}
				}
				
			});
			
			longClickDialog = dialogBuilder.create();
			longClickDialog.setOnKeyListener(dismissDialogListener);
			longClickDialog.show();
			return true;
		}
	};
	
	private OnKeyListener backKeyListener = new OnKeyListener()
	{
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event)
		{
			if(keyCode==KeyEvent.KEYCODE_BACK)
			{

				Log.v("GuestListFragment", "Back key detected");
				if(searchElement.getVisibility()==View.VISIBLE)
				{
					toggleSearch();
					return true;
				}
				else
				{
					return false;
				}
			}
			return false;
		}
	};
	
	private final OnItemClickListener guestCheckedListener = new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> patronList, View patronListElement, int position, long id)
		{
			final Patron checkedPatron = displayPatronList.get(position);
			// Make sure we can't check in a banned/blacklisted Patron
			if(checkedPatron.checkedInStatus()==-1)
			{
				Toast.makeText(getActivity(), "Patron is banned", Toast.LENGTH_SHORT).show();
			}
			else
			{
				// Do our local changes
				checkedPatron.onClick();
				try
				{
					final Command checkGuest = Command.toggleChecked(checkedPatron.getPatronId(),
							checkedPatron.checkedInStatus(),
							name,
							serverResponseHandler);
					
					// Check and see if we already have a pending command for this
					boolean commit = true;
					for(final Iterator<Command> itr = pendingCommands.iterator(); itr.hasNext();)
					{
						final Command c = itr.next();
						synchronized(c)
						{
							byte test = checkGuest.compare(c);
							if(test==0 || c.isProcessed())
							{
								continue;
							}
							else if(test==1)
							{
								Log.v("GuestListFragment", "Two commands are the same");
								commit = false;
								break;
							}
							else
							{
								Log.v("GuestListFragment", "Two commands are the opposite");
								commit = false;
								c.cancel();
								itr.remove();
								break;
							}
						}
					}
					if(commit)
					{
						pendingCommands.add(checkGuest);
						((DoormanActivity) getActivity()).sendCommand(checkGuest);
					}
				}
				catch(UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
				guestListAdapter.notifyDataSetChanged();
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}
	
	@Override
	public void onResume()
	{
		Log.v("GuestListFragment", "Resumed");
		try
		{
			updateCommand = Command.update(name, timeSinceLastUpdate, updateHandler);
			((DoormanActivity) getActivity()).sendCommand(updateCommand, SystemClock.uptimeMillis() + 20000);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		super.onResume();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{	
		final View v = inflater.inflate(R.layout.patron_list, null);
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
		
		plusButton = (Button) v.findViewById(R.id.promoter_toolbar_increment_guests);
		plusButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				int id = promoter.getId();
				if(id==Patron.ALLPROMOTERS.getId())
				{
					id = -1;
					findPromoterById(id).incrementPatrons();
				}
				else
				{
					promoter.incrementPatrons();
				}
				setPromoterHUD();
				try
				{
					final Command increment = Command.incrementPatrons(id, 1, serverResponseHandler);
					((DoormanActivity) getActivity()).sendCommand(increment);
					addCommand(increment);
				}
				catch(UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
			}
		});
		
		minusButton = (Button) v.findViewById(R.id.promoter_toolbar_decrement_guests);
		minusButton.setOnClickListener(new View.OnClickListener()
		{	
			@Override
			public void onClick(final View v)
			{
				int counter = Integer.parseInt((String) guestCounter.getText());
				int id = promoter.getId();
				if(counter>0)
				{
					if(id==Patron.ALLPROMOTERS.getId())
					{
						id = -1;
						findPromoterById(id).decrementPatrons();
					}
					else
					{
						promoter.decrementPatrons();
					}
					setPromoterHUD();
					try
					{
						final Command decrement = Command.incrementPatrons(id, -1, serverResponseHandler);
						((DoormanActivity) getActivity()).sendCommand(decrement);
						addCommand(decrement);
					}
					catch(UnsupportedEncodingException e)
					{
						e.printStackTrace();
					}
				}
			}
		});
		
		addPatronButton = (Button) v.findViewById(R.id.add_patron_button);
		addPatronButton.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v) 
			{
				final AddPersonFragment add = new AddPersonFragment(promoters);
				final FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
				transaction.replace(R.id.main_view, add);
				transaction.addToBackStack(null);
				transaction.commit();
			}
		});
		
		guestCounter = (TextView) v.findViewById(R.id.promoter_toolbar_nguests_counter);
		promoterHeader = (TextView) v.findViewById(R.id.promoter_name_header);
		
		sortList();
		
		setPromoterHUD();
		
		guestListAdapter = new PatronListAdapter(getActivity(),
				displayPatronList,
				R.layout.patron_list_element,
				new String[] {"name", "promoter", "guests"},
				new int[] {R.id.guest_list_element_name, R.id.guest_list_element_promoter, R.id.guest_list_element_guests});
		guestList.setAdapter(guestListAdapter);
		guestList.setEmptyView(v.findViewById(R.id.empty_list_text_view));
		guestList.setOnItemClickListener(guestCheckedListener);
		guestList.setOnItemLongClickListener(holdGuestListener);
		return v;
	}
	
	/**
	 * Convenience method for updating the anonymous patron counter
	 * and simultaneously changing the header to display the proper
	 * Promoter name
	 */
	private void setPromoterHUD()
	{
		if(promoter == Patron.ALLPROMOTERS)
		{
			guestCounter.setText(String.valueOf(this.calculateAnonymousPatrons()));
		}
		else
		{
			guestCounter.setText(String.valueOf(promoter.getNPatrons()));
		}
		promoterHeader.setText(promoter.getName());
	}

	/**
	 * Helper method to determine the total number of
	 * anonymous patrons
	 * @return The number of anonymous Patron
	 */
	private int calculateAnonymousPatrons()
	{
		// Keep track of how many Patrons we have
		int count = 0;
		for(final Promoter p : promoters)
		{
			Log.v("Adding patrons", "" + p.getNPatrons());
			// Add up the total number of Patrons, named and unnamed
			count+=p.getNPatrons();
		}
		return count;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onDetach()
	{
		if(logoutDialog!=null)
		{
			logoutDialog.dismiss();
		}
		
		if(longClickDialog!=null)
		{
			longClickDialog.dismiss();
		}
		
		if(guestDialog!=null)
		{
			guestDialog.dismiss();
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
		if(longClickDialog!=null)
		{
			longClickDialog.show();
		}
		if(guestDialog!=null)
		{
			guestDialog.show();
		}
		super.onActivityCreated(savedInstanceState);
	}
	
	protected void sortList()
	{
		displayPatronList.clear();
		displayPatronList.addAll(masterPatronList);
		Log.v("GuestListFragment", "checked: " + checked);
		if(checked!=Patron.ALL)
		{
			Log.v("GuestListFragment", "Iterating out certain checked/unchecked guests");
			for(final Iterator<Patron> itr = displayPatronList.iterator(); itr.hasNext();)
			{
				if(itr.next().checkedInStatus()!=checked)
				{
					itr.remove();
				}
			}
		}
		
		if(promoter!=Patron.ALLPROMOTERS)
		{
			for(final Iterator<Patron> itr = displayPatronList.iterator(); itr.hasNext();)
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
		
		Collections.sort(displayPatronList);
		
		if(reversed)
		{
			Collections.reverse(displayPatronList);
		}
		
		filter();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.list_menu, menu);
		
		switch(checked)
		{
		case(Patron.CHECKED):
			menu.findItem(R.id.checked_in_checked).setChecked(true);
			break;
		case(Patron.UNCHECKED):
			menu.findItem(R.id.checked_in_unchecked).setChecked(true);
			break;
		case(Patron.BANNED):
			menu.findItem(R.id.checked_in_banned).setChecked(true);
			break;
		default:
			menu.findItem(R.id.checked_in_all).setChecked(true);
		}
		
		final SubMenu partySortMenu = menu.getItem(1).getSubMenu();
		partySortMenu.add(R.id.party_sort, -2, SubMenu.NONE, "All");
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
			Log.v("GuestListFragment", "User clicked on a party sort item");
			item.setChecked(true);
			promoter = findPromoterById(item.getItemId());
			setPromoterHUD();
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
					checked = Patron.ALL;
					item.setChecked(true);
					break;
					
				case(R.id.checked_in_checked):
					checked = Patron.CHECKED;
					item.setChecked(true);
					break;
					
				case(R.id.checked_in_unchecked):
					checked = Patron.UNCHECKED;
					item.setChecked(true);
					break;
				case(R.id.checked_in_banned):
					checked = Patron.BANNED;
					item.setChecked(true);
					break;
				case(R.id.search):
					toggleSearch();
					break;
				
				case(R.id.log_out):
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
					if(pendingCommands.size()>0)
					{
						dialogBuilder.setMessage("Wait! There are pending commands! Are you sure you want to log out?");
					}
					else
					{
						dialogBuilder.setMessage("Are you sure you want to log out?");
					}
					dialogBuilder.setPositiveButton("Yes", new OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							final FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
							transaction.replace(R.id.main_view, 
									SelectListFragment.recreate(((DoormanActivity) getActivity()).restoreListSelectInfo()));
							transaction.commit();
							logoutDialog = null;
						}
					});
					dialogBuilder.setNegativeButton("no", new OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							logoutDialog.dismiss();
							logoutDialog = null;
						}
					});
					logoutDialog = dialogBuilder.create();
					logoutDialog.setOnKeyListener(dismissDialogListener);
					logoutDialog.show();
					break;
				
				case(R.id.metrics):
					int[] metrics = getMetrics();
					/*
					final int patronsExpected = masterPatronList.size();
					final int guests = getNumGuests();
					final int total = (patronsExpected + guests);
					final int numChecked = getNumCheckedIn();
					final int numNotChecked = (patronsExpected - numChecked);
					final int numCheckedWithGuests = getAttendance();
					final int numKoreans = kimLeePark();
					*/
					String[] items = new String[]{"Patrons Expected: " + metrics[0],
							"Guests Expected: " + metrics[1],
							"Total Expected: " + metrics[2],
							"Patrons Present: " + metrics[3],
							"Guests Present: " + metrics[4],
							"Total Present: " + metrics[5]};
					
					if(searchBar.getEditableText().toString().toLowerCase().contains("knutson"))
					{
						String[] temp = new String[items.length+1];
						System.arraycopy(items, 0, temp, 0, items.length);
						temp[items.length] = "Total Koreans: " + metrics[6];
						items = temp;
					}
					
					AlertDialog.Builder metricsDialog = new AlertDialog.Builder(getActivity());
					metricsDialog.setItems(items, null);
					metricsDialog.show();
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
			for(final Iterator<Patron> itr = displayPatronList.iterator(); itr.hasNext();)
			{
				final Patron guest = itr.next();
				
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
		return Patron.ALLPROMOTERS;
	}
	
	
	protected String getName()
	{
		return name;
	}
	
	protected ArrayList<Patron> getMasterList()
	{
		return masterPatronList;
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
		
		for(Patron g : masterPatronList)
		{
			saveGuests.put(g.toJsonObject());
		}
		Log.v("saveGuests", saveGuests.toString());
		
		try
		{
			saveData.put("promoters", savePromoters);
			saveData.put("data", saveGuests);
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
		updateCommand.cancel();
		updateCommand = null;
		Log.v("GuestListFragment", "onDestroy");
		super.onDestroy();
	}
	
	protected void addCommand(final Command c)
	{
		pendingCommands.add(c);
	}
	
	protected void nullifyDialogs()
	{
		logoutDialog = null;
		guestDialog = null;
		longClickDialog = null;
	}
	
	private int[] getMetrics()
	{
		int patronsExpected = 0, guestsExpected = 0, totalExpected = 0, patronsPresent = 0, guestsPresent = 0, totalPresent = 0, koreansPresent = 0;

		for(final Patron p : masterPatronList)
		{
			if(promoter==Patron.ALLPROMOTERS || p.getPromoter().getId()==promoter.getId())
			{
				patronsExpected++;
				guestsExpected+=p.getNumGuests();
				if(p.checkedInStatus()==1)
				{
					patronsPresent++;
					guestsPresent+=p.getNumGuestsChecked();
					
					final String lastName = p.getLastName();
					if(lastName.contentEquals("Lee") || lastName.contentEquals("Kim") || lastName.contentEquals("Park"))
					{
						koreansPresent++;
					}
				}
			}
		}
		totalExpected = patronsExpected + guestsExpected;
		totalPresent = patronsPresent + guestsPresent;
		return new int[] {patronsExpected, guestsExpected, totalExpected, patronsPresent, guestsPresent, totalPresent, koreansPresent};
	}
}