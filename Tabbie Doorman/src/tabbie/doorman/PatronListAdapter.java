package tabbie.doorman;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * An easy adapter to map static data to views defined in an XML file. You can specify the data
 * backing the list as an ArrayList of Guests. Each entry in the ArrayList corresponds to one row
 * in the list. The Guests contain the data for each row. You also specify an XML file that
 * defines the views used to display the row, and a mapping from keys in the Map to specific
 * views.
 *
 * Binding data to views occurs in two phases. First, if a
 * {@link android.widget.SimpleAdapter.ViewBinder} is available,
 * {@link ViewBinder#setViewValue(android.view.View, Object, String)}
 * is invoked. If the returned value is true, binding has occured. If the
 * returned value is false and the view to bind is a TextView,
 * {@link #setViewText(TextView, String)} is invoked. If the returned value
 * is false and the view to bind is an ImageView,
 * {@link #setViewImage(ImageView, int)} or {@link #setViewImage(ImageView, String)} is
 * invoked. If no appropriate binding can be found, an {@link IllegalStateException} is thrown.
 */

public class PatronListAdapter extends BaseAdapter
{
    private int[] mTo;
    private String[] mFrom;
    // private ViewBinder mViewBinder;
    private ArrayList<Patron> mData;
    
    private int mResource, mDropDownResource;
    private LayoutInflater mInflater;

    /**
     * Constructor
     * 
     * @param context The context where the View associated with this SimpleAdapter is running
     * @param data A List of Maps. Each entry in the List corresponds to one row in the list. The
     *        Maps contain the data for each row, and should include all the entries specified in
     *        "from"
     * @param resource Resource identifier of a view layout that defines the views for this list
     *        item. The layout file should include at least those named views defined in "to"
     * @param from A list of column names that will be added to the Map associated with each
     *        item.
     * @param to The views that should display column in the "from" parameter. These should all be
     *        TextViews. The first N views in this list are given the values of the first N columns
     *        in the from parameter.
     */
    public PatronListAdapter(Context context, ArrayList<Patron> data, int resource, String[] from, int[] to)
    {
        mData = data;
        mResource = mDropDownResource = resource;
        mFrom = from;
        mTo = to;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    
    /**
     * @see android.widget.Adapter#getCount()
     */
    public int getCount()
    {
    	Log.v("GuestListAdapter.getCount()", "Returning size: " + mData.size());
        return mData.size();
    }

    /**
     * @see android.widget.Adapter#getItem(int)
     */
    public Object getItem(int position)
    {
        return mData.get(position);
    }

    /**
     * @see android.widget.Adapter#getItemId(int)
     */
    public long getItemId(int position)
    {
        return mData.get(position).getPatronId();
    }

    /**
     * @see android.widget.Adapter#getView(int, View, ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent)
    {
    	View v = createViewFromResource(position, convertView, parent, mResource);
    	if(((Patron) getItem(position)).checkedInStatus()==1)
		{
    		v.setBackgroundResource(R.drawable.list_select_gradient);
		}
		else if(((Patron) getItem(position)).checkedInStatus()==-1)
		{
			v.setBackgroundResource(R.drawable.blacklist_gradient);
		}
		else
		{
			v.setBackgroundResource(0);
		}
        return v;
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource)
    {
        View v;
        if (convertView == null)
        {
            v = mInflater.inflate(resource, parent, false);
        }
        else
        {
            v = convertView;
        }
        bindView(position, v);
        return v;
    }

    /**
     * <p>Sets the layout resource to create the drop down views.</p>
     *
     * @param resource the layout resource defining the drop down views
     * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
     */
    public void setDropDownViewResource(int resource)
    {
        this.mDropDownResource = resource;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent)
    {
        return createViewFromResource(position, convertView, parent, mDropDownResource);
    }

    private void bindView(int position, View view)
    {
		final HashMap<String, String> dataSet = mData.get(position);
        if (dataSet == null)
        {
            return;
        }

        final String[] from = mFrom;
        final int[] to = mTo;
        final int len = to.length;

        for (int i = 0; i < len; i++)
        {
            final View v = view.findViewById(to[i]);
            if (v != null)
            {
                final Object data = dataSet.get(from[i]);
                String text = data == null ? "" : data.toString();
                if (text == null)
                {
                    text = "";
                }
                
                if (v instanceof TextView)
                {
                    setViewText((TextView) v, text);
                }
                else if (v instanceof ImageView)
                {
                    if (data instanceof Integer)
                    {
                        setViewImage((ImageView) v, (Integer) data);                            
                    }
                    else
                    {
                        setViewImage((ImageView) v, text);
                    }
                }
                else
                {
                    throw new IllegalStateException(v.getClass().getName() + " is not a " +
                            " view that can be bounds by this SimpleAdapter");
                }
            }
        }
    }

    /**
     * Called by bindView() to set the image for an ImageView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an ImageView.
     *
     * This method is called instead of {@link #setViewImage(ImageView, String)}
     * if the supplied data is an int or Integer.
     *
     * @param v ImageView to receive an image
     * @param value the value retrieved from the data set
     *
     * @see #setViewImage(ImageView, String)
     */
    public void setViewImage(ImageView v, int value)
    {
        v.setImageResource(value);
    }

    /**
     * Called by bindView() to set the image for an ImageView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an ImageView.
     *
     * By default, the value will be treated as an image resource. If the
     * value cannot be used as an image resource, the value is used as an
     * image Uri.
     *
     * This method is called instead of {@link #setViewImage(ImageView, int)}
     * if the supplied data is not an int or Integer.
     *
     * @param v ImageView to receive an image
     * @param value the value retrieved from the data set
     *
     * @see #setViewImage(ImageView, int) 
     */
    public void setViewImage(ImageView v, String value)
    {
        try
        {
            v.setImageResource(Integer.parseInt(value));
        }
        catch (NumberFormatException nfe)
        {
            v.setImageURI(Uri.parse(value));
        }
    }

    /**
     * Called by bindView() to set the text for a TextView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an TextView.
     *
     * @param v TextView to receive text
     * @param text the text to be set for the TextView
     */
    public void setViewText(TextView v, String text)
    {
        v.setText(text);
    }
    
    @Override
    public void notifyDataSetChanged()
    {
    	super.notifyDataSetChanged();
    }
}