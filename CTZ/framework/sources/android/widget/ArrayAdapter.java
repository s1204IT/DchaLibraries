package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ArrayAdapter<T> extends BaseAdapter implements Filterable, ThemedSpinnerAdapter {
    private final Context mContext;
    private LayoutInflater mDropDownInflater;
    private int mDropDownResource;
    private int mFieldId;
    private ArrayAdapter<T>.ArrayFilter mFilter;
    private final LayoutInflater mInflater;
    private final Object mLock;
    private boolean mNotifyOnChange;
    private List<T> mObjects;
    private boolean mObjectsFromResources;
    private ArrayList<T> mOriginalValues;
    private final int mResource;

    public ArrayAdapter(Context context, int i) {
        this(context, i, 0, new ArrayList());
    }

    public ArrayAdapter(Context context, int i, int i2) {
        this(context, i, i2, new ArrayList());
    }

    public ArrayAdapter(Context context, int i, T[] tArr) {
        this(context, i, 0, Arrays.asList(tArr));
    }

    public ArrayAdapter(Context context, int i, int i2, T[] tArr) {
        this(context, i, i2, Arrays.asList(tArr));
    }

    public ArrayAdapter(Context context, int i, List<T> list) {
        this(context, i, 0, list);
    }

    public ArrayAdapter(Context context, int i, int i2, List<T> list) {
        this(context, i, i2, list, false);
    }

    private ArrayAdapter(Context context, int i, int i2, List<T> list, boolean z) {
        this.mLock = new Object();
        this.mFieldId = 0;
        this.mNotifyOnChange = true;
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mDropDownResource = i;
        this.mResource = i;
        this.mObjects = list;
        this.mObjectsFromResources = z;
        this.mFieldId = i2;
    }

    public void add(T t) {
        synchronized (this.mLock) {
            if (this.mOriginalValues != null) {
                this.mOriginalValues.add(t);
            } else {
                this.mObjects.add(t);
            }
            this.mObjectsFromResources = false;
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void addAll(Collection<? extends T> collection) {
        synchronized (this.mLock) {
            if (this.mOriginalValues != null) {
                this.mOriginalValues.addAll(collection);
            } else {
                this.mObjects.addAll(collection);
            }
            this.mObjectsFromResources = false;
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void addAll(T... tArr) {
        synchronized (this.mLock) {
            if (this.mOriginalValues != null) {
                Collections.addAll(this.mOriginalValues, tArr);
            } else {
                Collections.addAll(this.mObjects, tArr);
            }
            this.mObjectsFromResources = false;
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void insert(T t, int i) {
        synchronized (this.mLock) {
            if (this.mOriginalValues != null) {
                this.mOriginalValues.add(i, t);
            } else {
                this.mObjects.add(i, t);
            }
            this.mObjectsFromResources = false;
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void remove(T t) {
        synchronized (this.mLock) {
            if (this.mOriginalValues != null) {
                this.mOriginalValues.remove(t);
            } else {
                this.mObjects.remove(t);
            }
            this.mObjectsFromResources = false;
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void clear() {
        synchronized (this.mLock) {
            if (this.mOriginalValues != null) {
                this.mOriginalValues.clear();
            } else {
                this.mObjects.clear();
            }
            this.mObjectsFromResources = false;
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void sort(Comparator<? super T> comparator) {
        synchronized (this.mLock) {
            if (this.mOriginalValues != null) {
                Collections.sort(this.mOriginalValues, comparator);
            } else {
                Collections.sort(this.mObjects, comparator);
            }
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        this.mNotifyOnChange = true;
    }

    public void setNotifyOnChange(boolean z) {
        this.mNotifyOnChange = z;
    }

    public Context getContext() {
        return this.mContext;
    }

    @Override
    public int getCount() {
        return this.mObjects.size();
    }

    @Override
    public T getItem(int i) {
        return this.mObjects.get(i);
    }

    public int getPosition(T t) {
        return this.mObjects.indexOf(t);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return createViewFromResource(this.mInflater, i, view, viewGroup, this.mResource);
    }

    private View createViewFromResource(LayoutInflater layoutInflater, int i, View view, ViewGroup viewGroup, int i2) {
        TextView textView;
        if (view == null) {
            view = layoutInflater.inflate(i2, viewGroup, false);
        }
        try {
            if (this.mFieldId == 0) {
                textView = (TextView) view;
            } else {
                textView = (TextView) view.findViewById(this.mFieldId);
                if (textView == null) {
                    throw new RuntimeException("Failed to find view with ID " + this.mContext.getResources().getResourceName(this.mFieldId) + " in item layout");
                }
            }
            T item = getItem(i);
            if (item instanceof CharSequence) {
                textView.setText((CharSequence) item);
            } else {
                textView.setText(item.toString());
            }
            return view;
        } catch (ClassCastException e) {
            Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException("ArrayAdapter requires the resource ID to be a TextView", e);
        }
    }

    public void setDropDownViewResource(int i) {
        this.mDropDownResource = i;
    }

    @Override
    public void setDropDownViewTheme(Resources.Theme theme) {
        if (theme == null) {
            this.mDropDownInflater = null;
        } else if (theme == this.mInflater.getContext().getTheme()) {
            this.mDropDownInflater = this.mInflater;
        } else {
            this.mDropDownInflater = LayoutInflater.from(new ContextThemeWrapper(this.mContext, theme));
        }
    }

    @Override
    public Resources.Theme getDropDownViewTheme() {
        if (this.mDropDownInflater == null) {
            return null;
        }
        return this.mDropDownInflater.getContext().getTheme();
    }

    @Override
    public View getDropDownView(int i, View view, ViewGroup viewGroup) {
        return createViewFromResource(this.mDropDownInflater == null ? this.mInflater : this.mDropDownInflater, i, view, viewGroup, this.mDropDownResource);
    }

    public static ArrayAdapter<CharSequence> createFromResource(Context context, int i, int i2) {
        return new ArrayAdapter<>(context, i2, 0, Arrays.asList(context.getResources().getTextArray(i)), true);
    }

    @Override
    public Filter getFilter() {
        if (this.mFilter == null) {
            this.mFilter = new ArrayFilter();
        }
        return this.mFilter;
    }

    @Override
    public CharSequence[] getAutofillOptions() {
        CharSequence[] autofillOptions = super.getAutofillOptions();
        if (autofillOptions != null) {
            return autofillOptions;
        }
        if (!this.mObjectsFromResources || this.mObjects == null || this.mObjects.isEmpty()) {
            return null;
        }
        CharSequence[] charSequenceArr = new CharSequence[this.mObjects.size()];
        this.mObjects.toArray(charSequenceArr);
        return charSequenceArr;
    }

    private class ArrayFilter extends Filter {
        private ArrayFilter() {
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            ArrayList arrayList;
            ArrayList arrayList2;
            Filter.FilterResults filterResults = new Filter.FilterResults();
            if (ArrayAdapter.this.mOriginalValues == null) {
                synchronized (ArrayAdapter.this.mLock) {
                    ArrayAdapter.this.mOriginalValues = new ArrayList(ArrayAdapter.this.mObjects);
                }
            }
            if (charSequence == null || charSequence.length() == 0) {
                synchronized (ArrayAdapter.this.mLock) {
                    arrayList = new ArrayList(ArrayAdapter.this.mOriginalValues);
                }
                filterResults.values = arrayList;
                filterResults.count = arrayList.size();
            } else {
                String lowerCase = charSequence.toString().toLowerCase();
                synchronized (ArrayAdapter.this.mLock) {
                    arrayList2 = new ArrayList(ArrayAdapter.this.mOriginalValues);
                }
                int size = arrayList2.size();
                ArrayList arrayList3 = new ArrayList();
                for (int i = 0; i < size; i++) {
                    Object obj = arrayList2.get(i);
                    String lowerCase2 = obj.toString().toLowerCase();
                    if (lowerCase2.startsWith(lowerCase)) {
                        arrayList3.add(obj);
                    } else {
                        String[] strArrSplit = lowerCase2.split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        int length = strArrSplit.length;
                        int i2 = 0;
                        while (true) {
                            if (i2 >= length) {
                                break;
                            }
                            if (!strArrSplit[i2].startsWith(lowerCase)) {
                                i2++;
                            } else {
                                arrayList3.add(obj);
                                break;
                            }
                        }
                    }
                }
                filterResults.values = arrayList3;
                filterResults.count = arrayList3.size();
            }
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            ArrayAdapter.this.mObjects = (List) filterResults.values;
            if (filterResults.count > 0) {
                ArrayAdapter.this.notifyDataSetChanged();
            } else {
                ArrayAdapter.this.notifyDataSetInvalidated();
            }
        }
    }
}
