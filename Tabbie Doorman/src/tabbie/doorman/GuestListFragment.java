package tabbie.doorman;

import java.io.UnsupportedEncodingException;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

public class GuestListFragment extends Fragment
{
	private final Patron patron;
	private TextView header, guestCounter;
	private Button incrementGuest, decrementGuest;
	
	public GuestListFragment(final Patron mPatron)
	{
		this.patron = mPatron;
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		final View fragmentDisplay = inflater.inflate(R.layout.guest_list, null);
		
		guestCounter = (TextView) fragmentDisplay.findViewById(R.id.guest_toolbar_nguests_counter);
		guestCounter.setText(String.valueOf(patron.getNumGuestsChecked()));

		header = (TextView) fragmentDisplay.findViewById(R.id.sub_guest_list_patron);
		header.setBackgroundResource(android.R.drawable.dark_header);
		header.setPadding(5, 10, 5, 10);
		header.setText(patron.getFirstName() + " " + patron.getLastName());
		
		incrementGuest = (Button) fragmentDisplay.findViewById(R.id.guest_toolbar_increment_guests);
		decrementGuest = (Button) fragmentDisplay.findViewById(R.id.guest_toolbar_decrement_guests);
		
		incrementGuest.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				patron.incrementGuests();
				guestCounter.setText(String.valueOf(patron.getNumGuestsChecked()));
				try
				{
					final Command increment = Command.incrementGuests(patron.getPatronId(), 1, null);
					((DoormanActivity) getActivity()).sendCommand(increment);
				}
				catch(UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
			}
		});
		
		decrementGuest.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				final int counter = Integer.parseInt((String) guestCounter.getText());
				if(counter > 0)
				{
					patron.decrementGuests();
					guestCounter.setText(String.valueOf(patron.getNumGuestsChecked()));
					try
					{
						final Command decrement = Command.incrementGuests(patron.getPatronId(), -1, null);
						((DoormanActivity) getActivity()).sendCommand(decrement);
					}
					catch(UnsupportedEncodingException e)
					{
						e.printStackTrace();
					}
				}
			}
		});
		
		final GridView list = (GridView) fragmentDisplay.findViewById(R.id.sub_guest_list);
		list.setEmptyView(fragmentDisplay.findViewById(R.id.empty_sub_list_text_view));
		return fragmentDisplay;
	}
}