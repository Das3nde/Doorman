package tabbie.doorman;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class Promoter implements Parcelable, Comparable<Promoter>
{
	private final String name, tag;
	private final int id;
	private int nPatronsChecked, nPatrons;
	
	public Promoter(final String pName, final String pTag, final int pId, final int pNPatrons, final int pNPatronsChecked)
	{
		this.name = pName;
		this.tag = pTag;
		this.id = pId;
		this.nPatrons = pNPatrons;
		this.nPatronsChecked = pNPatronsChecked;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	protected String getTag()
	{
		return tag;
	}
	
	protected int getId()
	{
		return id;
	}
	
	protected String getName()
	{
		return name;
	}
	
	protected int getNPatronsChecked()
	{
		return nPatronsChecked;
	}
	
	protected int getNPatrons()
	{
		return nPatrons;
	}
	
	protected void incrementPatrons()
	{
		nPatronsChecked++;
		nPatrons++;
	}
	
	protected void decrementPatrons()
	{
		nPatronsChecked--;
		nPatrons--;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel out, final int flags)
	{
		out.writeStringArray(new String[] {this.name, this.tag});
		out.writeInt(this.id);
	}
	
	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public Promoter createFromParcel(Parcel in) { return new Promoter(in);}
		public Promoter[] newArray(int size) { return new Promoter[size];}
	};
	
	private Promoter(final Parcel in)
	{
		final String[] stringData = new String[2];
		in.readStringArray(stringData);
		
		this.name = stringData[0];
		this.tag = stringData[1];
		this.id = in.readInt();
	}

	@Override
	public int compareTo(Promoter p)
	{
		if(this.getId()==p.getId())
		{
			return 0;
		}
		else if(this.getId()>p.getId())
		{
			return 1; 
		}
		else
		{
			return -1;
		}
	}
	
	protected JSONObject toJsonObject()
	{
		final JSONObject promoter = new JSONObject();
		try
		{
			promoter.put("p_display", name);
			promoter.put("p_code", tag);
			promoter.put("p_id", id);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return promoter;
	}
}
