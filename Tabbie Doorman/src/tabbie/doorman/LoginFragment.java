package tabbie.doorman;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ProgressDialog;
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
	private static final String LOADING = "Loading, please wait...";
	private final Handler loginHandler = new Handler(this);
	private EditText editName, editPassword;
	private Button loginButton;
	private Command loginCommand;
	private ProgressDialog loadingDialog;
	private boolean hasPendingCommands = false;
	
	protected LoginFragment(){};
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		if(hasPendingCommands)
		{
			displayLoader();
		}
		final View fragmentDisplay = inflater.inflate(R.layout.login, null);
		
		editName = (EditText) fragmentDisplay.findViewById(R.id.login_user_name);
		editPassword = (EditText) fragmentDisplay.findViewById(R.id.login_password);
		
		loginButton = (Button) fragmentDisplay.findViewById(R.id.login_button);
		loginButton.setOnClickListener(this);
		
		return fragmentDisplay;
	}

	@Override
	public void onClick(final View lButton)
	{
		try
		{
			loginCommand = Command.login(editName.getEditableText().toString(),
					editPassword.getEditableText().toString(),
					loginHandler);
			((DoormanActivity) getActivity()).sendCommand(loginCommand);
			hasPendingCommands = true;
			displayLoader();
		}
		catch(UnsupportedEncodingException e)
		{
			final Toast toast = Toast.makeText(getActivity(), "Error Logging In", Toast.LENGTH_SHORT);
			toast.show();
		}
	}
	
	/*
	private class DownloadGuestList extends AsyncTask<Void, String, Boolean>
	{
		private final String operation, name, password;
		private ProgressDialog loadingDialog;
		private String errorMessage = "Default Error";
		private ArrayList<Guest> guestList;
		private ArrayList<Promoter> promoterList;
		
		protected DownloadGuestList(final String mOperation, final String mName, final String mPassword)
		{
			this.operation = mOperation;
			this.name = mName;
			this.password = mPassword;
		}
		
		protected void onPreExecute()
		{
			loadingDialog = ProgressDialog.show(getActivity(), "", "Loading. Please wait...", true);
		}

		@Override
		protected Boolean doInBackground(final Void... params)
		{*/
			/*
			 * Obtain information from the server and store it
			 * in "Guest" objects
			 */
			/*
			final List<NameValuePair> listAccessPairs = new ArrayList<NameValuePair>();
			listAccessPairs.add(new BasicNameValuePair("op", operation));
			listAccessPairs.add(new BasicNameValuePair("name", name));
			listAccessPairs.add(new BasicNameValuePair("password", SHA1.hash(password)));
			
			final HttpClient tabbieClient = new DefaultHttpClient();
			final HttpPost accessPost = new HttpPost("http://tabbie.co/cgi-bin/neo.py");
			final ResponseHandler <String> res = new BasicResponseHandler();

			String response = "";
			*/
			/*
			 * Try to create a complete ArrayList of Guest(s)
			 * and if any exceptions are caught, be sure NOT
			 * to instantiate a GuestList object
			 */
			/*
			try
			{
				accessPost.setEntity(new UrlEncodedFormEntity(listAccessPairs));
				response = tabbieClient.execute(accessPost, res);
				final JSONObject listsObject = (JSONObject) new JSONTokener(response).nextValue();
				
				Log.v("Login", "Object returned: " + response);
				errorMessage = response;
				
				if(listsObject.has("error"))
				{
					errorMessage = (String) listsObject.get("error");
					return false;
				}
				else
				{
					final JSONArray listsArray = (JSONArray) listsObject.get("data");
					final String key = (String) listsObject.getString("key");
					
					final short length = (short) listsArray.length();
					for(short i = 0; i < length; i++)
					{
						final JSONObject list = (JSONObject) listsArray.getJSONObject(i);
					}
				}
			}
			catch(JSONException jsonError)
			{
				errorMessage = "Error retrieving data from server";
			}
			catch(NullPointerException nullData)
			{
				nullData.printStackTrace();
				errorMessage = "Guest List is Null";
			}
			catch (UnsupportedEncodingException badEncoding)
			{
				errorMessage = "Unsupported Encoding";
			}
			catch (ClientProtocolException badClientProtocol)
			{
				errorMessage = "Bad Client Protocol";
			}
			catch (IOException badIoStream)
			{
				errorMessage = "Bad IO Stream";
			}
			return false;
		}
		
		protected void dismiss()
		{
			loadingDialog.dismiss();
			this.cancel(true);
		}
		
		protected void onPostExecute(Boolean params)
		{
			loadingDialog.dismiss();
			if(params)
			{
				final GuestListFragment masterList = new GuestListFragment(loginQuery, guestList, promoterList);
				final FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
				transaction.replace(R.id.main_view, masterList, DoormanActivity.LIST_TAG);
				transaction.commit();
			}
			else
			{
				final Toast toast = Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	}*/
	
	@Override
	public void onStop()
	{
		if(loadingDialog!=null && loadingDialog.isShowing())
		{
			loadingDialog.dismiss();
		}
		if(loginCommand!=null)
		{
			loginCommand.cancel();
			loginCommand = null;
		}
		super.onStop();
	}

	@Override
	public boolean handleMessage(final Message msg)
	{
		hasPendingCommands = false;
		if(msg.obj instanceof java.lang.String)
		{
			final String response = (String) msg.obj;
			Log.v("LoginFragment", "Response is: " + response);
			ArrayList<ListEntity> list = null;
			try
			{
				final JSONObject listsObject = (JSONObject) new JSONTokener(response).nextValue();
				if(listsObject.has("error"))
				{
					displayError(listsObject.getString("error"));
				}
				else
				{
					((DoormanActivity) getActivity()).setEncryptionKey(listsObject.getString("key"));
					list = buildList(listsObject.getJSONArray("data"));
				}
			}
			catch(JSONException e)
			{
				displayError("Error formatting server response");
			}
			finally
			{
				if(loadingDialog!=null && loadingDialog.isShowing())
				{
					loadingDialog.dismiss();
				}
				
				if(list!=null)
				{
					final ListListFragment lists = new ListListFragment(list);
					final FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
					transaction.replace(R.id.main_view, lists, DoormanActivity.LISTS_FRAGMENT);
					transaction.commit();
				}
			}
		}
		else if(msg.obj instanceof Command)
		{
			if(loadingDialog!=null && loadingDialog.isShowing())
			{
				loadingDialog.dismiss();
			}
			displayError("Unable to connect to server");
		}
		return true;
	}
	
	private ArrayList<ListEntity> buildList(final JSONArray data)
	{
		final ArrayList<ListEntity> list = new ArrayList<ListEntity>();
		final short length = (short) data.length();
		for(short i = 0; i < length; i++)
		{
			try
			{
				final JSONObject temp = data.getJSONObject(i);
				final ListEntity tempEntity = new ListEntity(temp.getString("e_str_id"),
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
	
	private void displayError(final String errorMessage)
	{
		final Toast toast = Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG);
		toast.show();
	}
	
	private void displayLoader()
	{
		loadingDialog = ProgressDialog.show(getActivity(), null, LOADING);
	}
}