package android.widget;

import android.content.Context;
import android.net.ProxyInfo;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleAdapter extends BaseAdapter implements Filterable {
    private List<? extends Map<String, ?>> mData;
    private int mDropDownResource;
    private SimpleFilter mFilter;
    private String[] mFrom;
    private LayoutInflater mInflater;
    private int mResource;
    private int[] mTo;
    private ArrayList<Map<String, ?>> mUnfilteredData;
    private ViewBinder mViewBinder;

    public interface ViewBinder {
        boolean setViewValue(View view, Object obj, String str);
    }

    public SimpleAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
        this.mData = data;
        this.mDropDownResource = resource;
        this.mResource = resource;
        this.mFrom = from;
        this.mTo = to;
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return this.mData.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, this.mResource);
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
        View v;
        if (convertView == null) {
            v = this.mInflater.inflate(resource, parent, false);
        } else {
            v = convertView;
        }
        bindView(position, v);
        return v;
    }

    public void setDropDownViewResource(int resource) {
        this.mDropDownResource = resource;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, this.mDropDownResource);
    }

    private void bindView(int position, View view) {
        Map<String, ?> map = this.mData.get(position);
        if (map != null) {
            ViewBinder viewBinder = this.mViewBinder;
            String[] from = this.mFrom;
            int[] to = this.mTo;
            int count = to.length;
            for (int i = 0; i < count; i++) {
                View viewFindViewById = view.findViewById(to[i]);
                if (viewFindViewById != 0) {
                    Object data = map.get(from[i]);
                    String text = data == null ? ProxyInfo.LOCAL_EXCL_LIST : data.toString();
                    if (text == null) {
                        text = ProxyInfo.LOCAL_EXCL_LIST;
                    }
                    boolean bound = false;
                    if (viewBinder != null) {
                        bound = viewBinder.setViewValue(viewFindViewById, data, text);
                    }
                    if (bound) {
                        continue;
                    } else if (viewFindViewById instanceof Checkable) {
                        if (data instanceof Boolean) {
                            ((Checkable) viewFindViewById).setChecked(((Boolean) data).booleanValue());
                        } else if (viewFindViewById instanceof TextView) {
                            setViewText((TextView) viewFindViewById, text);
                        } else {
                            throw new IllegalStateException(viewFindViewById.getClass().getName() + " should be bound to a Boolean, not a " + (data == null ? "<unknown type>" : data.getClass()));
                        }
                    } else if (viewFindViewById instanceof TextView) {
                        setViewText((TextView) viewFindViewById, text);
                    } else if (viewFindViewById instanceof ImageView) {
                        if (data instanceof Integer) {
                            setViewImage((ImageView) viewFindViewById, ((Integer) data).intValue());
                        } else {
                            setViewImage((ImageView) viewFindViewById, text);
                        }
                    } else {
                        throw new IllegalStateException(viewFindViewById.getClass().getName() + " is not a  view that can be bounds by this SimpleAdapter");
                    }
                }
            }
        }
    }

    public ViewBinder getViewBinder() {
        return this.mViewBinder;
    }

    public void setViewBinder(ViewBinder viewBinder) {
        this.mViewBinder = viewBinder;
    }

    public void setViewImage(ImageView v, int value) {
        v.setImageResource(value);
    }

    public void setViewImage(ImageView v, String value) {
        try {
            v.setImageResource(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            v.setImageURI(Uri.parse(value));
        }
    }

    public void setViewText(TextView v, String text) {
        v.setText(text);
    }

    @Override
    public Filter getFilter() {
        if (this.mFilter == null) {
            this.mFilter = new SimpleFilter();
        }
        return this.mFilter;
    }

    private class SimpleFilter extends Filter {
        private SimpleFilter() {
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence prefix) {
            Filter.FilterResults results = new Filter.FilterResults();
            if (SimpleAdapter.this.mUnfilteredData == null) {
                SimpleAdapter.this.mUnfilteredData = new ArrayList(SimpleAdapter.this.mData);
            }
            if (prefix == null || prefix.length() == 0) {
                ArrayList<Map<String, ?>> list = SimpleAdapter.this.mUnfilteredData;
                results.values = list;
                results.count = list.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();
                ArrayList<Map<String, ?>> unfilteredValues = SimpleAdapter.this.mUnfilteredData;
                int count = unfilteredValues.size();
                ArrayList<Map<String, ?>> newValues = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    Map<String, ?> h = unfilteredValues.get(i);
                    if (h != null) {
                        int len = SimpleAdapter.this.mTo.length;
                        for (int j = 0; j < len; j++) {
                            String str = (String) h.get(SimpleAdapter.this.mFrom[j]);
                            String[] words = str.split(" ");
                            int wordCount = words.length;
                            int k = 0;
                            while (true) {
                                if (k < wordCount) {
                                    String word = words[k];
                                    if (!word.toLowerCase().startsWith(prefixString)) {
                                        k++;
                                    } else {
                                        newValues.add(h);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                results.values = newValues;
                results.count = newValues.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            SimpleAdapter.this.mData = (List) results.values;
            if (results.count > 0) {
                SimpleAdapter.this.notifyDataSetChanged();
            } else {
                SimpleAdapter.this.notifyDataSetInvalidated();
            }
        }
    }
}
