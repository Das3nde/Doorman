package tabbie.doorman;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class ServerHandlerThread extends HandlerThread implements Handler.Callback
{
	private final HttpPost accessPost = new HttpPost("http://tabbie.co/cgi-bin/neo.py");
	private final ResponseHandler<String> res = new BasicResponseHandler();
	private final HttpParams callParams = new BasicHttpParams();
	private final HttpClient tabbieClient;
	private Handler serverCallHandler;
	
	public ServerHandlerThread(final String name)
	{
		super(name);
		HttpConnectionParams.setConnectionTimeout(callParams, 5000);
		HttpConnectionParams.setSoTimeout(callParams, 5000);
		tabbieClient = new DefaultHttpClient(callParams);
	}
	
	@Override
	public boolean handleMessage(final Message msg)
	{
		if(msg.obj instanceof Command)
		{
			final Command command = (Command) msg.obj;
			accessPost.setEntity(command.getEntity());
			Object response = null;
			try
			{
				response = tabbieClient.execute(accessPost, res);
			}
			catch(IOException e)
			{
				response = command;
			}
			finally
			{
				if(command.getHandler()!=null)
				{
					final Handler responseHandler = command.getHandler();
					if(command.isCancelled())
					{
						Log.v("SeverHandlerThread", "Command has been cancelled");
					}
					else
					{
						synchronized(responseHandler)
						{
							final Message reply = responseHandler.obtainMessage();
							reply.obj = response;
							responseHandler.sendMessage(reply);
						}
					}
				}
			}
		}
		else
		{
			throw new RuntimeException();
		}
		return true;
	}
	
	protected Handler getHandler()
	{
		if(!isAlive())
		{
			this.start();
		}
		if(serverCallHandler==null)
		{
			serverCallHandler = new Handler(getLooper(), this);
		}
		return serverCallHandler;
	}
}
