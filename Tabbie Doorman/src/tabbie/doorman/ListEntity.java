package tabbie.doorman;

import java.util.HashMap;

public class ListEntity extends HashMap<String, String>
{
	private static final long serialVersionUID = -5710150711760123303L;
	private final String name, display;
	private final short id;
	
	protected ListEntity(final String name, final short id, final String display)
	{
		this.name = name;
		this.id = id;
		this.display = display;
		
		this.put("display", display);
	}
	
	protected String getName()
	{
		return name;
	}

}
