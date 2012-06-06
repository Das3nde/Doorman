package tabbie.doorman;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import android.os.Handler;
import android.util.Log;


public class Command
{
	private final ArrayList<NameValuePair> serverQuery = new ArrayList<NameValuePair>();
	private final Operation operation;
	private final UrlEncodedFormEntity urlEntity;
	private final Handler handler;
	private boolean cancelled = false, processed = false; // Flags for thread-safe command processing
	
	protected enum Operation
	{
		ADD_GUEST("addGuest"),
		LOAD_LIST("loadVIPList"),
		LOGIN("login"),
		SET_CHECKED("setChecked"),
		REMOVE("deleteGuest"),
		EDIT("editGuest"),
		UPDATE("updateVIPList"),
		INCREMENT_GUESTS("incrementGuestsCheckedBy"),
		INCREMENT_PATRONS("incrementPatronsCheckedBy");
		
		private final String query;
		
		private Operation(final String ops)
		{
			this.query = ops;
		}
		
		protected String getQuery()
		{
			return query;
		}
	}
	
	protected static Command update(final String list, final int timeSinceLastUpdate, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.UPDATE,
				handler,
				new BasicNameValuePair("list_id", list),
				new BasicNameValuePair("last_update", String.valueOf(timeSinceLastUpdate)));
	}
	
	protected static Command login(final String name, final String password, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.LOGIN,
				handler,
				new BasicNameValuePair("name", name),
				new BasicNameValuePair("password", SHA1.hash(password)));
	}
	
	protected static Command loadList(final String listName, final String encryptionKey, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.LOAD_LIST,
				handler,
				new BasicNameValuePair("list_id", listName),
				new BasicNameValuePair("key", encryptionKey));
	}
	
	protected static Command addGuest(final String firstName, final String lastName, final String pTag, final int parent, final int nGuests, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.ADD_GUEST,
				handler,
				new BasicNameValuePair("first", firstName),
				new BasicNameValuePair("last", lastName),
				new BasicNameValuePair("code", pTag),
				new BasicNameValuePair("nguests", String.valueOf(nGuests)));
	}
	
	protected static Command removeGuest(final int id, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.REMOVE,
				handler,
				new BasicNameValuePair("v_id", String.valueOf(id)));
	}
	
	protected static Command editGuest(final String firstName, final String lastName, final int id, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.EDIT,
				handler,
				new BasicNameValuePair("v_first", firstName),
				new BasicNameValuePair("v_last", lastName),
				new BasicNameValuePair("v_id", String.valueOf(id)));
	}
	
	protected static Command toggleChecked(final int id, final byte checkedStatus, final String list_id, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.SET_CHECKED,
				handler,
				new BasicNameValuePair("v_id", String.valueOf(id)),
				new BasicNameValuePair("v_checked", String.valueOf(checkedStatus)),
				new BasicNameValuePair("list_id", list_id));
	}
	
	protected static Command incrementGuests(final int patronId, final int delta, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.INCREMENT_GUESTS,
				handler,
				new BasicNameValuePair("v_id", String.valueOf(patronId)),
				new BasicNameValuePair("delta", String.valueOf(delta)));
	}
	
	protected static Command incrementPatrons(final int promoterId, final int delta, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.INCREMENT_PATRONS,
				handler,
				new BasicNameValuePair("p_id", String.valueOf(promoterId)),
				new BasicNameValuePair("delta", String.valueOf(delta)));
	}
	
	private Command(final Operation ops, final Handler handler, final NameValuePair... params) throws UnsupportedEncodingException
	{
		this.handler = handler;
		this.operation = ops;
		serverQuery.add(new BasicNameValuePair("op", operation.getQuery()));
		for(NameValuePair entity : params)
		{
			Log.v("Command", "Adding entity: " + entity.toString());
			serverQuery.add(entity);
		}
		urlEntity = new UrlEncodedFormEntity(serverQuery);
	}
	
	protected Handler getHandler()
	{
		return handler;
	}
	
	protected Operation getOperation()
	{
		return operation;
	}
	
	protected NameValuePair getNameValuePair(final int i)
	{
		return serverQuery.get(i);
	}
	
	protected UrlEncodedFormEntity getEntity()
	{
		return urlEntity;
	}
	
	protected void cancel()
	{
		cancelled = true;
	}
	
	protected boolean isCancelled()
	{
		return cancelled;
	}
	
	protected void lockProcess()
	{
		processed = true;
	}
	
	protected boolean isProcessed()
	{
		return processed;
	}
	
	/**
	 * 
	 * @return The number of NameValuePairs present in this object
	 */
	protected int getSize()
	{
		return serverQuery.size();
	}
	
	/**
	 * Compare two Command objects to determine if they are
	 * the same, the opposite, or different
	 * @return -1 if the Commands are opposite, 1 if the Commands are identical, and 0 if the commands are different
	 * 
	 */
	protected byte compare(final Command c)
	{
		Log.v("Command", "Comparing");
		byte comp = 1;
		int length = c.getSize();
		if(serverQuery.size() == length)
		{
			Log.v("Command", "Same Length");
			for(short i = 0; i < length; i++)
			{
				final NameValuePair temp = serverQuery.get(i);
				Log.v("Comand", "NameValuePair name is " + temp.getName());
				if(!temp.getValue().contentEquals(c.getNameValuePair(i).getValue()))
				{
					if(temp.getName().contentEquals("v_checked"))
					{
						Log.v("Command", "Detected v_checked");
						comp = -1;
						continue;
					}
					else
					{
						comp = 0;
						break;
					}
				}
			}
		}
		else
		{
			comp = 0;
		}
		Log.v("Returning", String.valueOf(comp));
		return comp;
	}
}
