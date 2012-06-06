package tabbie.doorman;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

final class LoginFragment extends Fragment implements OnClickListener, Handler.Callback
{	
	private static final String TAG = "LoginFragment";
	private static final String LOADING_MESSAGE = "Loading, please wait...";
	private final Handler loginHandler = new Handler(this);
	private Toast error;
	private SelectListFragment selectListFragment;
	private EditText editName, editPassword;
	private Button loginButton;
	private Command loginCommand;
	private ProgressDialog loadingDialog;
	private boolean hasPendingCommands = false;
	
	public LoginFragment(){};
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.v(TAG, "OnCreate");
		
		// Retain variables over configuration changes
		setRetainInstance(true);
		
		// Make sure no save files exist still
		((DoormanActivity) getActivity()).deleteSaveCache();
		
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		Log.v(TAG, "OnCreateView");
		
		// Since the view is redrawn on configuration changes, we have to check if there are any outstanding login Commands
		// and determine whether or not a loading message should be displayed in the context
		if(hasPendingCommands)
		{
			displayLoader(LOADING_MESSAGE);
		}
		final View fragmentDisplay = inflater.inflate(R.layout.login, null);
		
		editName = (EditText) fragmentDisplay.findViewById(R.id.login_user_name);
		editPassword = (EditText) fragmentDisplay.findViewById(R.id.login_password);
		
		loginButton = (Button) fragmentDisplay.findViewById(R.id.login_button);
		loginButton.setOnClickListener(this);
		
		return fragmentDisplay;
	}
	
	@Override
	public void onResume()
	{
		finishProcessingCommand();
		super.onResume();
	}
	
	@Override
	public void onDetach()
	{
		// Fragments are detached during configuration changes; make sure we don't leak a loading message here!
		dismissLoader();
		
		Log.v(TAG, "OnDetach");
		super.onDetach();
	}
	
	@Override
	public void onDestroy()
	{
		// We need to save information when the fragment is finally going to be destroyed
		/*
		try
		{
			final JSONObject storable = new JSONObject();
			storable.put("name", editName.getEditableText().toString());
			storable.put("password", editPassword.getEditableText().toString());
			final String writable = storable.toString();
			final FileOutputStream fos = getActivity().openFileOutput(DoormanActivity.LOGIN_INFO, Context.MODE_PRIVATE);
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
		*/
		Log.v(TAG, "OnDestroy");
		super.onDestroy();
	}

	@Override
	public void onClick(final View lButton)
	{
		final ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo status = (NetworkInfo) cm.getActiveNetworkInfo();
		
		// Make sure we can send a command before we actually send a command
		if(status!=null && status.isConnected())
		{
			try
			{
				loginCommand = Command.login(editName.getEditableText().toString(),
						editPassword.getEditableText().toString(),
						loginHandler);
				((DoormanActivity) getActivity()).sendCommand(loginCommand);
				hasPendingCommands = true;
				displayLoader(LOADING_MESSAGE);
			}
			catch(UnsupportedEncodingException e)
			{
				error = Toast.makeText(getActivity(), "Error Logging In", Toast.LENGTH_SHORT);
				e.printStackTrace();
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

	@Override
	public boolean handleMessage(final Message msg)
	{
		String key = null;

		// String = good; this should be a JSONString
		if(msg.obj instanceof java.lang.String)
		{
			final String response = (String) msg.obj;
			Log.v("LoginFragment", "Response is: " + response);
			ArrayList<SelectableList> list = null;
			try
			{
				final JSONObject listsObject = (JSONObject) new JSONTokener(response).nextValue();
				if(listsObject.has("error"))
				{
					error = Toast.makeText(getActivity(), "Invalid name/password", Toast.LENGTH_LONG);
				}
				else
				{
					// Retrieve our String, build our list (SelectList)
					key = listsObject.getString("key");
					list = buildList(listsObject.getJSONArray("data"));
				}
			}
			catch(JSONException e)
			{
				error = Toast.makeText(getActivity(), "Error formatting server response", Toast.LENGTH_LONG);
			}
			finally
			{
				if(list!=null)
				{
					selectListFragment = new SelectListFragment(list, key);
				}
			}
		}
		else if(msg.obj instanceof Command)
		{
			error = Toast.makeText(getActivity(), "Unable to connect to server", Toast.LENGTH_LONG);
		}
		
		// Regardless of the outcome, we no longer have any pending commands
		hasPendingCommands = false;		
		dismissLoader();
		
		if(isResumed())
		{
			// Assuming there is still an activity, finish processing the command now
			finishProcessingCommand();
		}
		
		return true;
	}
	
	/**
	 * Method used to ensure that any commands parsed before this fragment was detached
	 * from an activity are finished when the fragment is resumed. If the command terminated
	 * in an error, the error will be displayed as a Toast
	 */
	private void finishProcessingCommand()
	{
		if(error!=null)
		{
			error.show();
			error = null;
		}
		else if(selectListFragment!=null)
		{
			final FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.main_view, selectListFragment, DoormanActivity.SELECT_LIST_FRAGMENT);
			transaction.commit();
		}
	}
	
	/**
	 * Attempt to build a list of SelectableList using JSONData
	 * that was returned by the server and processed by this object. 
	 * This will always return a list, even if it is empty.
	 */
	protected static ArrayList<SelectableList> buildList(final JSONArray data)
	{
		final ArrayList<SelectableList> list = new ArrayList<SelectableList>();
		final int length = data.length();
		for(int i = 0; i < length; i++)
		{
			try
			{
				final JSONObject temp = data.getJSONObject(i);
				final SelectableList tempEntity = new SelectableList(temp.getString("e_str_id"),
					(short) temp.getInt("e_id"),
					temp.getString("e_name"));
				list.add(tempEntity);
			}
			catch(JSONException e)
			{
				Log.v("Error", "Unable to create list entity " + i + " from JSON");
			}
		}
		return list;
	}
	
	/**
	 * Convenience method to show a ProgressDialog with loadingMessage
	 * @param loadingMessage - the message to display
	 */
	private void displayLoader(final String loadingMessage)
	{
		loadingDialog = ProgressDialog.show(getActivity(), null, loadingMessage);
	}
	
	/**
	 * Convenience method to dismiss a ProgressDialog created with displayLoader()
	 */
	private void dismissLoader()
	{
		if(loadingDialog!=null && loadingDialog.isShowing())
		{
			loadingDialog.dismiss();
		}
	}
}