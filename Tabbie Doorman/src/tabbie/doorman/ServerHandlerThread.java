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
	private boolean paused = false;
	
	public ServerHandlerThread(final String name)
	{
		super(name);
		
		// Socket and Connection will time out after 5 seconds
		HttpConnectionParams.setConnectionTimeout(callParams, 5000);
		HttpConnectionParams.setSoTimeout(callParams, 5000);
		tabbieClient = new DefaultHttpClient(callParams);
	}
	
	@Override
	public boolean handleMessage(final Message msg)
	{
		// Don't run a Command through here unless our BroadcastReceiver has set paused to FALSE
		if(paused)
		{
			try
			{
				synchronized(this)
				{
					wait();
				}
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		// Make sure the message we are processing is a command
		if(msg.obj instanceof Command)
		{
			final Command command = (Command) msg.obj;
			
			// Check here to see if our Command is still viable. Canceled commands will be cleaned up elsewhere
			if(command.isCancelled())
			{
				Log.v("ServerHandlerThread", "Command is canceled");
			}
			else
			{
				accessPost.setEntity(command.getEntity());
				Object response = null;
				try
				{
					synchronized(command)
					{
						// Ensure that the command will not be modified after this point
						// We do not want to synchronize over tabbieClient's execution because the UI thread will pause waiting for the results
						command.lockProcess();
					}
					response = tabbieClient.execute(accessPost, res);
				}
				catch(IOException e)
				{
					e.printStackTrace();
					
					// Return the command as a message to the handler so if need be the command can be reissued
					response = command;
				}
				finally
				{
					// Only send a message if there is a handler available
					if(command.getHandler()!=null)
					{
						final Handler responseHandler = command.getHandler();
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
	
	protected void pause()
	{
		Log.v("ServerHandlerThread", "Paused");
		paused = true;
	}
	
	protected void unPause()
	{
		synchronized(this)
		{
			notifyAll();
		}
		paused = false;
	}
	
	protected boolean isPaused()
	{
		return paused;
	}
}
