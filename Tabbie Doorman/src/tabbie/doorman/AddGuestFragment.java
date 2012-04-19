package tabbie.doorman;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class AddGuestFragment extends Fragment
{

	private final ArrayList<Promoter> promoterList = new ArrayList<Promoter>();
	private EditText editLastName, editFirstName, editNguests;
	private Spinner promoterSpinner;
	private Button incrementButton, decrementButton, addGuestButton;
	private ArrayAdapter<Promoter> spinnerAdapter;
	private short nGuests = 0;
	
	private final OnClickListener nGuestsListener = new OnClickListener()
	{
		@Override
		public void onClick(final View v)
		{
			switch(v.getId())
			{
			case(R.id.add_name_increment_guests):
				editNguests.setText(String.valueOf(++nGuests));
				break;
			case(R.id.add_name_decrement_guests):
				if(nGuests<=0)
					break;
				editNguests.setText(String.valueOf(--nGuests));
				break;
			}
		}
	};
	
	private final OnClickListener addGuestListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			try
			{
				final Command addGuest = Command.addGuest(editFirstName.getEditableText().toString(),
						editLastName.getEditableText().toString(),
						((Promoter) promoterSpinner.getSelectedItem()).getTag(),
						nGuests,
						null);
			}
			catch(UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	
	public AddGuestFragment(final ArrayList<Promoter> mPromoterList)
	{
		promoterList.addAll(mPromoterList);
	}
	
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		this.setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{	
		final View display = inflater.inflate(R.layout.add_guest, null);
		
		spinnerAdapter = new ArrayAdapter<Promoter>(getActivity(), android.R.layout.simple_spinner_item, promoterList);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		promoterSpinner = (Spinner) display.findViewById(R.id.add_name_promoter_spinner);
		promoterSpinner.setAdapter(spinnerAdapter);
		
		incrementButton = (Button) display.findViewById(R.id.add_name_increment_guests);
		decrementButton = (Button) display.findViewById(R.id.add_name_decrement_guests);
		addGuestButton = (Button) display.findViewById(R.id.add_name_add_guest_button);
		
		editLastName = (EditText) display.findViewById(R.id.add_name_last_edit);
		editFirstName = (EditText) display.findViewById(R.id.add_name_first_edit);
		editNguests = (EditText) display.findViewById(R.id.add_name_nguests_counter);
		editNguests.setText(String.valueOf(nGuests));
		
		incrementButton.setOnClickListener(nGuestsListener);
		decrementButton.setOnClickListener(nGuestsListener);
		addGuestButton.setOnClickListener(addGuestListener);
		
		return display;
	}
}
