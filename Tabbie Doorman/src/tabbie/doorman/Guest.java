package tabbie.doorman;

public class Guest
{
	private final String firstName, lastName;
	private final int guestId, parent;
	private byte checkedIn;
	protected boolean isDeleted = false;
	
	protected Guest(final String mFirstName, final String mLastName, final int mGuestId, final int mParent, final byte mCheckedIn)
	{
		firstName = mFirstName;
		lastName = mLastName;
		guestId = mGuestId;
		checkedIn = mCheckedIn;
		parent = mParent;
	}
	
	protected int getParentId()
	{
		return parent;
	}
	
	protected String getFullName()
	{
		return firstName + " " + lastName;
	}
	
	protected boolean getCheckedStatus() throws Exception
	{
		switch(checkedIn)
		{
		case 1:
			return true;
		case 0:
			return false;
		default:
			throw new Exception();
		}
	}
	
	protected void setCheckedStatus(final byte checked)
	{
		checkedIn = checked;
	}
	
	protected int getId()
	{
		return guestId;
	}
	
	protected String getFirstName()
	{
		return firstName;
	}
	
	protected String getLastName()
	{
		return lastName;
	}
}
