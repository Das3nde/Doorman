package tabbie.doorman;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;

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

import android.os.Handler;
import android.util.Log;

public class ServerPoller extends TimerTask
{
	private static final String tabbieUrl = "http://tabbie.co/cgi-bin/neo.py";
	
	private final GuestListFragment guestList;
	private final String name;
	private final HttpPost tabbieRequest = new HttpPost(tabbieUrl);
	private final List<List<NameValuePair>> updateRequests = new ArrayList<List<NameValuePair>>();
	private final ResponseHandler<String> receiver = new BasicResponseHandler();
	
	private int seconds;
	
	public ServerPoller(final GuestListFragment gGuestList, final ArrayList<List<NameValuePair>> mPendingCommands)
	{
		this.guestList = gGuestList;
		this.name = guestList.getName();
		if(mPendingCommands!=null)
		{
			this.updateRequests.addAll(mPendingCommands);
		}
	}

	@Override
	public void run()
	{
		seconds+=20;
		Log.v("Serverpoller", "Attempting Update");
		String response = "";
		
		if(!updateRequests.isEmpty())
		{
			for(final Iterator<List<NameValuePair>> itr = updateRequests.iterator(); itr.hasNext();)
			{
				final List<NameValuePair> updateRequest = itr.next();
				Log.v("Sending Info to Server", updateRequest.toString());
				try
				{
					final HttpClient tabbieClient = new DefaultHttpClient();
					tabbieRequest.setEntity(new UrlEncodedFormEntity(updateRequest));
					response = tabbieClient.execute(tabbieRequest, receiver);
				}
				catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
				catch (ClientProtocolException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				Log.v("Server", "returned: " + response);
				itr.remove();
			}
		}

		final Handler updater = guestList.getHandler();
		
		updater.post(new Runnable()
		{
			public void run()
			{
				try
				{
					final HttpClient tabbieClient = new DefaultHttpClient();
					
					final List<NameValuePair> updateRequest = new ArrayList<NameValuePair>();
					
					updateRequest.add(new BasicNameValuePair("op", "updateVIPList"));
					updateRequest.add(new BasicNameValuePair("list_id", name));
					updateRequest.add(new BasicNameValuePair("last_update", String.valueOf(seconds)));
					
					tabbieRequest.setEntity(new UrlEncodedFormEntity(updateRequest));
					String response = tabbieClient.execute(tabbieRequest, receiver);
					Log.v("Response", response);
					if(response!=null)
					{
						final ArrayList<Guest> newList = guestList.getMasterList();
						final JSONObject guestsObject = (JSONObject) new JSONTokener(response).nextValue();
						final JSONArray guestsArray = (JSONArray) guestsObject.get("data");
						
						Promoter promoter = null;
						for(int i = 0; i < guestsArray.length(); i++)
						{
							final JSONObject guestObject = guestsArray.getJSONObject(i);
							for(Promoter p : guestList.getPromoters())
							{
								if(guestObject.getString("p_code").contentEquals(p.getTag()))
								{
									promoter = p;
								}
							}
							
							if(promoter==null)
							{
								promoter = new Promoter("", "", -1);
							}
									
							final Guest newGuest = new Guest(guestObject.getString("v_first"),
									guestObject.getString("v_last"),
									guestObject.getInt("v_nguests"),
									guestObject.getInt("v_id"),
									(guestObject.getInt("v_checked")!=0),
									promoter);
							for(int j = 0; j < newList.size(); j++)
							{
								if(newList.get(j).compareTo(newGuest)==0)
								{
									newList.set(j, newGuest);
									break;
								}
							}
						}
					}
					seconds = 0;
				}
				catch(UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
				catch(JSONException e)
				{
					e.printStackTrace();
				}
			}
		});
	}
	
	protected void postCheckedUpdate(int guestId, boolean checked)
	{
		final ArrayList<NameValuePair> currentPost = new ArrayList<NameValuePair>();
		currentPost.add(new BasicNameValuePair("op", "setChecked"));
		currentPost.add(new BasicNameValuePair("v_id", String.valueOf(guestId)));
		currentPost.add(new BasicNameValuePair("v_checked", String.valueOf(((checked==true) ? 1 : 0))));
		
		updateRequests.add(currentPost);
	}
	
	protected JSONArray parcelPendingCommands()
	{
		final JSONArray pendingCommands = new JSONArray();
		try
		{
			for(List<NameValuePair> command : updateRequests)
			{
				final JSONObject pendingCommand = new JSONObject();
				for(NameValuePair p : command)
				{
					pendingCommand.put(p.getName(), p.getValue());
				}
				pendingCommands.put(pendingCommand);
			}
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
		Log.v("pending commands", pendingCommands.toString());
		return pendingCommands;
	}
	
	protected boolean hasPendingCommands()
	{
		if(this.updateRequests.isEmpty())
		{
			return false;
		}
		else
			return true;
	}
	
	protected void forceExecute()
	{
		seconds+=20;
		Log.v("Serverpoller", "Attempting Forced Update");
		String response = "";
		
		if(!updateRequests.isEmpty())
		{
			for(final Iterator<List<NameValuePair>> itr = updateRequests.iterator(); itr.hasNext();)
			{
				final HttpClient tabbieClient = new DefaultHttpClient();
				final List<NameValuePair> updateRequest = itr.next();
				Log.v("Sending Info to Server", updateRequest.toString());
				try
				{
					tabbieRequest.setEntity(new UrlEncodedFormEntity(updateRequest));
					response = tabbieClient.execute(tabbieRequest, receiver);
				}
				catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
				catch (ClientProtocolException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				Log.v("Server", "returned: " + response);
				itr.remove();
			}
		}
	}
}