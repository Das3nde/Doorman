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
	private boolean cancelled = false;
	
	protected enum Operation
	{
		ADD_GUEST("addGuest"),
		LOAD_LIST("loadVIPList"),
		LOGIN("login"),
		SET_CHECKED("setChecked");
		
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
	
	protected static Command addGuest(final String firstName, final String lastName, final String pTag, final short nGuests, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.ADD_GUEST,
				handler,
				new BasicNameValuePair("first", firstName),
				new BasicNameValuePair("last", lastName),
				new BasicNameValuePair("code", pTag),
				new BasicNameValuePair("nguests", String.valueOf(nGuests)));
	}
	
	protected static Command toggleChecked(final short id, final boolean isChecked, final Handler handler) throws UnsupportedEncodingException
	{
		return new Command(Operation.SET_CHECKED,
				handler,
				new BasicNameValuePair("v_id", String.valueOf(id)),
				new BasicNameValuePair("v_checked", ((isChecked) ? "1" : "0")));
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
	
	protected NameValuePair getNameValuePair(final short index)
	{
		return serverQuery.get(index);
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
}
