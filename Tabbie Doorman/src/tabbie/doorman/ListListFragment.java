package tabbie.doorman;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class ListListFragment extends Fragment implements Handler.Callback, OnItemClickListener
{
	private static final String LOADING = "Loading, please wait...";
	private final ArrayList<ListEntity> lists = new ArrayList<ListEntity>();
	private final Handler handler = new Handler(this);
	private ProgressDialog loadingDialog;
	private boolean hasPendingCommands = false;
	
	public ListListFragment(){};
	
	protected ListListFragment(final ArrayList<ListEntity> mLists)
	{
		this.lists.addAll(mLists);
	}
	
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		this.setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		final View fragmentDisplay = inflater.inflate(R.layout.list_list, null);
		final ListView listListView = (ListView) fragmentDisplay.findViewById(R.id.list_list_view);
		final SimpleAdapter listListAdapter = new SimpleAdapter(getActivity(),
				lists,
				R.layout.list_list_element,
				new String[] {"display"},
				new int[] {R.id.list_list_display});
		listListView.setAdapter(listListAdapter);
		listListView.setOnItemClickListener(this);
		return fragmentDisplay;
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		loadingDialog.dismiss();
		Log.v("ListListFragment", "Response is: " + msg.obj.toString());
		return true;
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
	{
		final ListEntity requestedList = (ListEntity) lists.get(position);
		final String encryptionKey = ((DoormanActivity) getActivity()).getEncryptionKey();
		try
		{
			final Command loadList = Command.loadList(requestedList.getName(),
					encryptionKey,
					handler);
			((DoormanActivity) getActivity()).sendCommand(loadList);
			hasPendingCommands = true;
			loadingDialog = ProgressDialog.show(getActivity(), null, LOADING);
		}
		catch(final UnsupportedEncodingException e)
		{
			final Toast toast = Toast.makeText(getActivity(), "Error, unable to create Command.loadList", Toast.LENGTH_SHORT);
			toast.show();
		}
	}
	
	@Override
	public void onStop()
	{
		if(loadingDialog!=null && loadingDialog.isShowing())
		{
			loadingDialog.dismiss();
		}
		super.onStop();
	}
}
