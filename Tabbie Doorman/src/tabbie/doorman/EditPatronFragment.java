package tabbie.doorman;

import java.io.UnsupportedEncodingException;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class EditPatronFragment extends Fragment implements OnClickListener
{
	private final Guest editableGuest;
	private EditText editFirst, editLast;
	
	protected EditPatronFragment(final Guest guest)
	{
		editableGuest = guest;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.edit_patron, null);
		editFirst = (EditText) v.findViewById(R.id.edit_name_first_edit);
		editLast = (EditText) v.findViewById(R.id.edit_name_last_edit);
		editFirst.setText(editableGuest.getFirstName());
		editLast.setText(editableGuest.getLastName());
		final Button confirm = (Button) v.findViewById(R.id.edit_name_edit_guest_button);
		confirm.setOnClickListener(this);
		
		return v;
	}

	@Override
	public void onClick(View v)
	{
		final PatronListFragment current = (PatronListFragment) getActivity().getSupportFragmentManager().findFragmentByTag(DoormanActivity.PATRON_LIST_FRAGMENT);
		try
		{
			final Command edit = Command.editGuest(editFirst.getEditableText().toString(),
					editLast.getEditableText().toString(), 
					editableGuest.getId(),
					current.serverResponseHandler);
			current.addCommand(edit);
			((DoormanActivity) getActivity()).sendCommand(edit);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		current.nullifyDialogs();
		getActivity().getSupportFragmentManager().popBackStack();
	}
	
}

