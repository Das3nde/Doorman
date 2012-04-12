package tabbie.doorman;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Guest extends HashMap<String, String> implements Comparable<Guest>, Parcelable
{
	public static final byte ALL = 2;
	public static final byte UNCHECKED = 0;
	public static final byte CHECKED = 1;
	public static final Promoter ALLPROMOTERS = new Promoter("Default", "default", -1);
	private static final long serialVersionUID = 1L;
	private final Promoter promoter;
	private final String firstName, lastName;
	private final int numGuests, guestId;
	private final HashMap<String, String> guestInfo = new HashMap<String, String>();
	private boolean checkedIn;
	
	public Guest(final String mFirstName, final String mLastName, final int mNumGuests, final int mGuestId, final boolean mCheckedIn, final Promoter mPromoter)
	{
		this.lastName = mLastName;
		this.firstName = mFirstName;
		this.guestId = mGuestId;
		this.numGuests = mNumGuests;
		this.checkedIn = mCheckedIn;
		this.promoter = mPromoter;
		
		guestInfo.put("name", (lastName + ", " + firstName));
		try
		{
			guestInfo.put("guests", "Guests: " + (numGuests));
		}
		catch(NullPointerException noGuestFeature)
		{
			guestInfo.put("guests", "");
		}
		guestInfo.put("promoter", promoter.toString());
	}
	
	protected void onClick()
	{
		checkedIn = !checkedIn;
		Log.v("Guest onClick()", "Checked in currently " + checkedIn);
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
	
	protected int getGuestId()
	{
		return guestId;
	}
	
	protected boolean isCheckedIn()
	{
		return checkedIn;
	}
	
	protected HashMap<String, String> getHashMap()
	{
		return guestInfo;
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
		out.writeIntArray(new int[] {numGuests, guestId});
		out.writeParcelable(promoter, 0);
		out.writeByte((byte) (checkedIn ? 1 : 0));
	}
	
	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public Guest createFromParcel(Parcel in) { return new Guest(in);}
		public Guest[] newArray(int size) { return new Guest[size];}
	};
	
	private Guest(final Parcel in)
	{
		final String[] stringData = new String[2];
		final int[] intData = new int[2];
		in.readStringArray(stringData);
		in.readIntArray(intData);
		
		this.lastName = stringData[0];
		this.firstName = stringData[1];
		this.numGuests = intData[0];
		this.guestId = intData[1];
		this.promoter = in.readParcelable(null);
		this.checkedIn = (in.readByte()!=0);
		
		this.guestInfo.put("name", (lastName + ", " + firstName));
		this.guestInfo.put("guests", "Guests: " + numGuests);
		this.guestInfo.put("promoter", promoter.toString());
	}
	
	@Override
	public String get(Object key)
	{
		return guestInfo.get(key);
	}

	@Override
	public int compareTo(Guest g2)
	{
		int result = this.getLastName().compareToIgnoreCase(g2.getLastName());
		return ((result==0) ? this.getFirstName().compareToIgnoreCase(g2.getFirstName()) : result);
	}
	
	protected JSONObject toJsonObject()
	{
		final JSONObject saveGuest = new JSONObject();
		try
		{
			saveGuest.put("v_first", firstName);
			saveGuest.put("v_last", lastName);
			saveGuest.put("v_id", guestId);
			saveGuest.put("v_nguests", numGuests);
			saveGuest.put("v_checked", checkedIn);
			saveGuest.put("p_id", promoter.getId());
			saveGuest.put("p_code", promoter.getTag());
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
		return saveGuest;
	}
}