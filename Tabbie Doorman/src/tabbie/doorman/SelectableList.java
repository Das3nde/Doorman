package tabbie.doorman;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

public class SelectableList extends HashMap<String, String>
{
	private static final long serialVersionUID = -5710150711760123303L;
	private final String name, display;
	private final int id;
	
	protected SelectableList(final String name, final int id, final String display)
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
	
	/**
	 * Collapse this object and save it as JSON data
	 * @return - a JSONObject corresponding to this SelectableList
	 */
	protected JSONObject toJson()
	{
		final JSONObject save = new JSONObject();
		try
		{
			save.put("name", name);
			save.put("id", id);
			save.put("display", display);
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
		return save;
	}
}
