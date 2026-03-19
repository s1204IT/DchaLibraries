package com.android.calendar.event;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.calendar.R;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

public class EventLocationAdapter extends ArrayAdapter<Result> implements Filterable {
    private Context mContext;
    private final LayoutInflater mInflater;
    private final Map<Uri, Bitmap> mPhotoCache;
    private final ContentResolver mResolver;
    private final ArrayList<Result> mResultList;
    private static ArrayList<Result> EMPTY_LIST = new ArrayList<>();
    private static final String[] CONTACTS_PROJECTION = {"_id", "display_name", "data1", "contact_id", "photo_id"};
    private static final String CONTACTS_WHERE = "(data1 LIKE ? OR data1 LIKE ? OR display_name LIKE ? OR display_name LIKE ? )";
    private static final String[] EVENT_PROJECTION = {"_id", "eventLocation", "visible"};
    private static final String[] STORAGE_PERMISSION = {"android.permission.READ_EXTERNAL_STORAGE"};
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};
    private static final String[] CONTACTS_PERMISSION = {"android.permission.READ_CONTACTS"};

    public static class Result {
        private final String mAddress;
        private final Uri mContactPhotoUri;
        private final Integer mDefaultIcon;
        private final String mName;

        public Result(String str, String str2, Integer num, Uri uri) {
            this.mName = str;
            this.mAddress = str2;
            this.mDefaultIcon = num;
            this.mContactPhotoUri = uri;
        }

        public String toString() {
            return this.mAddress;
        }
    }

    public EventLocationAdapter(Context context) {
        super(context, R.layout.location_dropdown_item, EMPTY_LIST);
        this.mResultList = new ArrayList<>();
        this.mPhotoCache = new HashMap();
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
    }

    @Override
    public int getCount() {
        return this.mResultList.size();
    }

    @Override
    public Result getItem(int i) {
        if (i < this.mResultList.size()) {
            return this.mResultList.get(i);
        }
        return null;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(R.layout.location_dropdown_item, viewGroup, false);
        }
        Result item = getItem(i);
        if (item == null) {
            return view;
        }
        TextView textView = (TextView) view.findViewById(R.id.location_name);
        if (textView != null) {
            if (item.mName == null) {
                textView.setVisibility(8);
            } else {
                textView.setVisibility(0);
                textView.setText(item.mName);
            }
        }
        TextView textView2 = (TextView) view.findViewById(R.id.location_address);
        if (textView2 != null) {
            textView2.setText(item.mAddress);
        }
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        if (imageView != null) {
            if (item.mDefaultIcon == null) {
                imageView.setVisibility(4);
            } else {
                imageView.setVisibility(0);
                imageView.setImageResource(item.mDefaultIcon.intValue());
                imageView.setTag(item.mContactPhotoUri);
                if (item.mContactPhotoUri != null) {
                    Bitmap bitmap = this.mPhotoCache.get(item.mContactPhotoUri);
                    if (bitmap == null) {
                        asyncLoadPhotoAndUpdateView(item.mContactPhotoUri, imageView);
                    } else {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }
        }
        return view;
    }

    private void asyncLoadPhotoAndUpdateView(final Uri uri, final ImageView imageView) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voidArr) {
                InputStream inputStreamOpenContactPhotoInputStream = ContactsContract.Contacts.openContactPhotoInputStream(EventLocationAdapter.this.mResolver, uri);
                if (inputStreamOpenContactPhotoInputStream != null) {
                    Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenContactPhotoInputStream);
                    EventLocationAdapter.this.mPhotoCache.put(uri, bitmapDecodeStream);
                    return bitmapDecodeStream;
                }
                return null;
            }

            @Override
            public void onPostExecute(Bitmap bitmap) {
                if (bitmap != null && imageView.getTag() == uri) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }.execute(new Void[0]);
    }

    @Override
    public Filter getFilter() {
        return new LocationFilter();
    }

    public class LocationFilter extends Filter {
        public LocationFilter() {
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            final String string = charSequence == null ? "" : charSequence.toString();
            if (string.isEmpty() || !EventLocationAdapter.this.hasRequiredPermission(EventLocationAdapter.CALENDAR_PERMISSION) || !EventLocationAdapter.this.hasRequiredPermission(EventLocationAdapter.STORAGE_PERMISSION) || !EventLocationAdapter.this.hasRequiredPermission(EventLocationAdapter.CONTACTS_PERMISSION)) {
                return null;
            }
            AsyncTask<Void, Void, List<Result>> asyncTaskExecute = new AsyncTask<Void, Void, List<Result>>() {
                @Override
                protected List<Result> doInBackground(Void... voidArr) {
                    return EventLocationAdapter.queryRecentLocations(EventLocationAdapter.this.mResolver, string);
                }
            }.execute(new Void[0]);
            HashSet hashSet = new HashSet();
            List listQueryContacts = EventLocationAdapter.queryContacts(EventLocationAdapter.this.mResolver, string, hashSet);
            ArrayList arrayList = new ArrayList();
            try {
                for (Result result : asyncTaskExecute.get()) {
                    if (result.mAddress != null && !hashSet.contains(result.mAddress)) {
                        arrayList.add(result);
                    }
                }
            } catch (InterruptedException e) {
                Log.e("EventLocationAdapter", "Failed waiting for locations query results.", e);
            } catch (ExecutionException e2) {
                Log.e("EventLocationAdapter", "Failed waiting for locations query results.", e2);
            }
            if (listQueryContacts != null) {
                arrayList.addAll(listQueryContacts);
            }
            if (Log.isLoggable("EventLocationAdapter", 3)) {
                Log.d("EventLocationAdapter", "Autocomplete of " + charSequence + ": location query match took " + (System.currentTimeMillis() - jCurrentTimeMillis) + "ms (" + arrayList.size() + " results)");
            }
            Filter.FilterResults filterResults = new Filter.FilterResults();
            filterResults.values = arrayList;
            filterResults.count = arrayList.size();
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            EventLocationAdapter.this.mResultList.clear();
            if (filterResults != null && filterResults.count > 0) {
                EventLocationAdapter.this.mResultList.addAll((ArrayList) filterResults.values);
                EventLocationAdapter.this.notifyDataSetChanged();
            } else {
                EventLocationAdapter.this.notifyDataSetInvalidated();
            }
        }
    }

    private static List<Result> queryContacts(ContentResolver contentResolver, String str, HashSet<String> hashSet) {
        String str2;
        String[] strArr;
        Result result;
        Uri uriWithAppendedId;
        if (TextUtils.isEmpty(str)) {
            str2 = null;
            strArr = null;
        } else {
            String str3 = str + "%";
            String str4 = "% " + str + "%";
            str2 = CONTACTS_WHERE;
            strArr = new String[]{str3, str4, str3, str4};
        }
        Cursor cursorQuery = contentResolver.query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, CONTACTS_PROJECTION, str2, strArr, "display_name ASC");
        try {
            HashMap map = new HashMap();
            cursorQuery.moveToPosition(-1);
            while (cursorQuery.moveToNext()) {
                String string = cursorQuery.getString(1);
                String string2 = cursorQuery.getString(2);
                if (string != null) {
                    List list = (List) map.get(string);
                    if (list == null) {
                        if (cursorQuery.getLong(4) > 0) {
                            uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, cursorQuery.getLong(3));
                        } else {
                            uriWithAppendedId = null;
                        }
                        ArrayList arrayList = new ArrayList();
                        map.put(string, arrayList);
                        result = new Result(string, string2, Integer.valueOf(R.drawable.ic_contact_picture), uriWithAppendedId);
                        list = arrayList;
                    } else {
                        result = new Result(null, string2, null, null);
                    }
                    list.add(result);
                    hashSet.add(string2);
                }
            }
            ArrayList arrayList2 = new ArrayList();
            Iterator it = map.values().iterator();
            while (it.hasNext()) {
                arrayList2.addAll((List) it.next());
            }
            return arrayList2;
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    private static List<Result> queryRecentLocations(ContentResolver contentResolver, String str) {
        String str2;
        if (str == null) {
            str2 = "";
        } else {
            str2 = str + "%";
        }
        List<Result> listProcessLocationsQueryResults = null;
        if (str2.isEmpty()) {
            return null;
        }
        Cursor cursorQuery = contentResolver.query(CalendarContract.Events.CONTENT_URI, EVENT_PROJECTION, "visible=? AND eventLocation LIKE ?", new String[]{"1", str2}, "_id DESC");
        if (cursorQuery != null) {
            try {
                listProcessLocationsQueryResults = processLocationsQueryResults(cursorQuery);
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        return listProcessLocationsQueryResults;
    }

    private static List<Result> processLocationsQueryResults(Cursor cursor) {
        TreeSet treeSet = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        cursor.moveToPosition(-1);
        while (treeSet.size() < 4 && cursor.moveToNext()) {
            treeSet.add(cursor.getString(1).trim());
        }
        ArrayList arrayList = new ArrayList();
        Iterator it = treeSet.iterator();
        while (it.hasNext()) {
            arrayList.add(new Result(null, (String) it.next(), Integer.valueOf(R.drawable.ic_history_holo_light), null));
        }
        return arrayList;
    }

    private boolean hasRequiredPermission(String[] strArr) {
        for (String str : strArr) {
            if (this.mContext.checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }
}
