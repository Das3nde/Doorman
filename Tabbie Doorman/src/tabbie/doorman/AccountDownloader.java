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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

public class AccountDownloader extends AsyncTask<String, Void, Boolean>
{
	public static final String OP_GET_ACCOUNT = "login";
	
	protected ProgressDialog loadingDialog;
	
	private final Context context;
	private final FragmentManager manager;
	private String errorMessage;
	
	
	public AccountDownloader(final Context mContext, final FragmentManager mManager)
	{
		this.context = mContext;
		this.manager = mManager;
	}
	
	protected void onPreExecute()
	{
		loadingDialog = ProgressDialog.show(context, "", "Loading. Please wait...", true);
	}
	
	@Override
	protected void onCancelled()
	{
		/*
		 * Redundant check in case for
		 * whatever reason loadingDialog
		 * is still active
		 */
		
		if(loadingDialog.isShowing())
		{
			loadingDialog.dismiss();
		}
	}

	@Override
	protected Boolean doInBackground(final String... params)
	{
		final String userName = params[0];
		final String userPassword = SHA1.hash(params[1]);
		Log.v("userPassword", userPassword);
		return false;}
		/*
		final List<NameValuePair> listAccessPairs = new ArrayList<NameValuePair>();
		listAccessPairs.add(new BasicNameValuePair("name", userName));
		listAccessPairs.add(new BasicNameValuePair("password", userPassword));
		listAccessPairs.add(new BasicNameValuePair("op", OP_GET_ACCOUNT));
		
		final HttpClient tabbieClient = new DefaultHttpClient();
		final HttpPost accessPost = new HttpPost("http://tabbie.co/cgi-bin/neo.py");
		final ResponseHandler <String> res = new BasicResponseHandler();

		String response = "";
		
		try
		{
			accessPost.setEntity(new UrlEncodedFormEntity(listAccessPairs));
			response = tabbieClient.execute(accessPost, res);
			Log.v("Login", "Object returned: " + response);
			
			final JSONObject guestsObject = (JSONObject) new JSONTokener(response).nextValue();
			
			if(guestsObject.has("error"))
			{
				errorMessage = (String) guestsObject.get("error");
				return false;
			}
			else
			{
				
				return true;
			}
		}
		catch(JSONException jsonError)
		{
			errorMessage = "Error retrieving data from server";
		}
		catch(NullPointerException nullData)
		{
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
	}*/
	
	protected void onPostExecute(Boolean noError)
	{
		loadingDialog.dismiss();
		if(noError)
		{
		}
		else
		{
			final Toast toast = Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT);
			toast.show();
		}
	}

}
