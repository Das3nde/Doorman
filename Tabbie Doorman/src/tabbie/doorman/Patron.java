package tabbie.doorman;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Patron extends HashMap<String, String> implements Comparable<Patron>, Parcelable
{
	private static final long serialVersionUID = -4606260121276310252L;
	public static final byte ALL = 2, UNCHECKED = 0, CHECKED = 1, BANNED = -1;
	public static final Promoter ALLPROMOTERS = new Promoter("All Promoters", "default", -2, 0, 0);
	private final Promoter promoter;
	private final String firstName, lastName;
	private final int patronId;
	private final HashMap<String, String> patronInfo = new HashMap<String, String>();
	private int numGuests, numGuestsChecked;
	private byte checkedIn;
	protected boolean isDeleted = false, isAdding = false;
	
	public Patron(final String mFirstName, final String mLastName, final int mNumGuests, final int mNumGuestsChecked, final int mPatronId, final byte mCheckedIn, final Promoter mPromoter)
	{
		this.lastName = mLastName;
		this.firstName = mFirstName;
		this.patronId = mPatronId;
		this.numGuests = mNumGuests;
		this.numGuestsChecked = mNumGuestsChecked;
		this.checkedIn = mCheckedIn;
		this.promoter = mPromoter;
		
		patronInfo.put("name", (lastName + ", " + firstName));
		try
		{
			patronInfo.put("guests", "Guests: " + (numGuests));
		}
		catch(NullPointerException noGuestFeature)
		{
			patronInfo.put("guests", "");
		}
		patronInfo.put("promoter", promoter.toString());
	}
	
	protected void onClick()
	{
		switch(checkedIn)
		{
		case 1:
			checkedIn = 0;
			break;
		case 0:
			checkedIn = 1;
			break;
		}
		Log.v("Patron onClick()", "Checked in currently " + checkedIn);
	}
	
	protected String getFirstName()
	{
		return firstName;
	}
	
	protected String getLastName()
	{
		return lastName;
	}
	
	protected Promoter getPromoter()
	{
		return promoter;
	}
	
	protected int getNumGuests()
	{
		return numGuests;
	}
	
	protected int getNumGuestsChecked()
	{
		return numGuestsChecked;
	}
	
	protected int getPatronId()
	{
		return patronId;
	}
	
	protected byte checkedInStatus()
	{
		return checkedIn;
	}
	
	protected HashMap<String, String> getHashMap()
	{
		return patronInfo;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel out, final int flags)
	{
		out.writeStringArray(new String[] {this.lastName, this.firstName});
		out.writeIntArray(new int[] {numGuests, patronId});
		out.writeParcelable(promoter, 0);
		out.writeByte(checkedIn);
	}
	
	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public Patron createFromParcel(Parcel in) { return new Patron(in);}
		public Patron[] newArray(int size) { return new Patron[size];}
	};
	
	private Patron(final Parcel in)
	{
		final String[] stringData = new String[2];
		final int[] intData = new int[2];
		in.readStringArray(stringData);
		in.readIntArray(intData);
		
		this.lastName = stringData[0];
		this.firstName = stringData[1];
		this.numGuests = intData[0];
		this.patronId = intData[1];
		this.promoter = in.readParcelable(null);
		this.checkedIn = in.readByte();
		
		this.patronInfo.put("name", (lastName + ", " + firstName));
		this.patronInfo.put("guests", "Guests: " + numGuests);
		this.patronInfo.put("promoter", promoter.toString());
	}
	
	@Override
	public String get(Object key)
	{
		return patronInfo.get(key);
	}

	@Override
	public int compareTo(Patron g2)
	{
		int result = this.getLastName().compareToIgnoreCase(g2.getLastName());
		return ((result==0) ? this.getFirstName().compareToIgnoreCase(g2.getFirstName()) : result);
	}
	
	protected JSONObject toJsonObject()
	{
		final JSONObject savePatron = new JSONObject();
		try
		{
			savePatron.put("v_first", firstName);
			savePatron.put("v_last", lastName);
			savePatron.put("v_id", patronId);
			savePatron.put("v_nguests", numGuests);
			savePatron.put("v_nguests_checked", numGuestsChecked);
			savePatron.put("v_checked", checkedIn);
			savePatron.put("p_id", promoter.getId());
			savePatron.put("p_code", promoter.getTag());
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
		return savePatron;
	}
	
	protected void toggleBlacklist()
	{
		switch(checkedIn)
		{
		case -1:
			checkedIn = 0;
			break;
		default:
			checkedIn = -1;
			break;
		}
	}
	
	protected void incrementGuests()
	{
		numGuestsChecked++;
	}
	
	protected void decrementGuests()
	{
		numGuestsChecked--;
	}
}