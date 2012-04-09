package tabbie.doorman;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginFragment extends Fragment implements OnClickListener
{
	public static final String OP_LOAD_LIST = "loadVIPList";
	
	private DownloadGuestList downloader;
	private EditText listId;
	private String loginQuery;
	private boolean resumeTask = false;
	
	public LoginFragment(){};
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		/*
		 * We want this fragment to retain its instance
		 * across screen orientation changes, etc.
		 * Until the object is DESTROYED, savedInstanceState
		 * is always going to be NULL
		 */
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View fragmentDisplay = inflater.inflate(R.layout.login, null);
		final Button loginButton = (Button) fragmentDisplay.findViewById(R.id.login_button);
		loginButton.setOnClickListener(this);

		listId = (EditText) fragmentDisplay.findViewById(R.id.login_list_id);
		
		return fragmentDisplay;
	}

	@Override
	public void onClick(View v)
	{
		/*
		 * loginQuery cannot be strictly local
		 * because if the AsyncTask is interrupted,
		 * we need to remember what the String was
		 * in order to restart it
		 */
		
		loginQuery = (String) listId.getText().toString();
		
		/*
		 * Downloader must also be accessible outside of this
		 * method so that it can be canceled if the screen
		 * orientation changes or the activity is otherwise
		 * destroyed.
		 */
		
		downloader = new DownloadGuestList(getActivity());
		downloader.execute(loginQuery, OP_LOAD_LIST);
	}
	
	private class DownloadGuestList extends AsyncTask<String, String, Boolean>
	{
		final FragmentActivity activity;
		private ArrayList<Guest> guestList;
		private ArrayList<Promoter> promoterList;
		private ProgressDialog loadingDialog;
		private String errorMessage = "Default Error";
		
		public DownloadGuestList(final Activity mActivity)
		{
			this.activity = (FragmentActivity) mActivity;
		}
		
		protected void onPreExecute()
		{
			loadingDialog = ProgressDialog.show(activity, "", "Loading. Please wait...", true);
		}

		@Override
		protected Boolean doInBackground(final String... params)
		{
			/*
			 * Obtain information from the server and store it
			 * in "Guest" objects
			 */
			
			final List<NameValuePair> listAccessPairs = new ArrayList<NameValuePair>();
			listAccessPairs.add(new BasicNameValuePair("list_id", params[0]));
			listAccessPairs.add(new BasicNameValuePair("op", params[1]));
			
			final HttpClient tabbieClient = new DefaultHttpClient();
			final HttpPost accessPost = new HttpPost("http://tabbie.co/cgi-bin/neo.py");
			final ResponseHandler <String> res = new BasicResponseHandler();

			String response = "";
			
			/*
			 * Try to create a complete ArrayList of Guest(s)
			 * and if any exceptions are caught, be sure NOT
			 * to instantiate a GuestList object
			 */
			
			try
			{
				accessPost.setEntity(new UrlEncodedFormEntity(listAccessPairs));
				response = tabbieClient.execute(accessPost, res);
				final JSONObject guestsObject = (JSONObject) new JSONTokener(response).nextValue();
				
				Log.v("Login", "Object returned: " + response);
				
				if(guestsObject.has("error"))
				{
					errorMessage = (String) guestsObject.get("error");
					return false;
				}
				else
				{
					final JSONArray guestsArray = (JSONArray) guestsObject.get("data");
					final JSONArray promotersArray = (JSONArray) guestsObject.get("promoters");

					/*
					 * Iterate through each guest in the JSONArray
					 * and create a new Guest object for each one
					 */
					Log.v("Making", "Promoters Array");
					
					promoterList = DoormanActivity.JSONTOPROMOTERLIST(promotersArray);
					
					guestList = DoormanActivity.JSONTOGUESTLIST(guestsArray, promoterList);

					return true;
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
		
		protected void dismissDialog()
		{
			loadingDialog.dismiss();
		}
		
		protected void onPostExecute(Boolean params)
		{
			loadingDialog.dismiss();
			if(params)
			{
				final GuestListFragment masterList = new GuestListFragment(loginQuery, guestList, promoterList);
				final FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
				transaction.replace(R.id.main_view, masterList, DoormanActivity.LIST_TAG);
				transaction.commit();
			}
			else
			{
				final Toast toast = Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		/*
		 * We made a note of whether or not a downloader
		 * was running when (if) the activity restarted itself.
		 * If that task never completed, let's go ahead
		 * and finish it here.
		 */
		
		if(resumeTask)
		{
			downloader = new DownloadGuestList(getActivity());
			downloader.execute(loginQuery, OP_LOAD_LIST);
			resumeTask = false;
		}
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onStop()
	{
		/*
		 * Check and see if we have a downloader active.
		 * If so, check its status - if it's finished, we
		 * can ignore it; otherwise we need to cancel it so
		 * the application can restart during config change
		 */
		if(downloader!=null && downloader.getStatus()!=AsyncTask.Status.FINISHED)
		{
			downloader.dismissDialog();
			downloader.cancel(true);
			resumeTask = true;
		}
		super.onStop();
	}
	
	
}