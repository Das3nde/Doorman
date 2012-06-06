package tabbie.doorman;

public class Person
{
	private final int id;
	private final String lastName, firstName;
	
	protected Person(final int id, final String lastName, final String firstName)
	{
		this.id = id;
		this.lastName = lastName;
		this.firstName = firstName;
	}
	
	protected String getFirstName()
	{
		return firstName;
	}

	protected String getLastName()
	{
		return lastName;
	}
	
	protected int getId()
	{
		return id;
	}
	
	@Override
	public String toString()
	{
		return lastName + ", " + firstName;
	}
}
