package tabbie.doorman;

public class NewGuest extends Person
{
	private boolean banned = false, checked = false;
	
	protected NewGuest(final int id, final String lastName, final String firstName, final boolean banned, final boolean checked)
	{
		super(id, lastName, firstName);
		this.banned = banned;
		this.checked = checked;
	}
	
	protected boolean isBanned()
	{
		return banned;
	}
	
	protected boolean isChecked()
	{
		return checked;
	}
	
	protected void ban()
	{
		banned = true;
	}
	
	protected void unban()
	{
		banned = false;
	}
	
	protected void check()
	{
		checked = true;
	}
	
	protected void uncheck()
	{
		checked = false;
	}
}
