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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

public class EventLocationAdapter extends ArrayAdapter<Result> implements Filterable {
    private final LayoutInflater mInflater;
    private final Map<Uri, Bitmap> mPhotoCache;
    private final ContentResolver mResolver;
    private final ArrayList<Result> mResultList;
    private static ArrayList<Result> EMPTY_LIST = new ArrayList<>();
    private static final String[] CONTACTS_PROJECTION = {"_id", "display_name", "data1", "contact_id", "photo_id"};
    private static final String CONTACTS_WHERE = "(data1 LIKE ? OR data1 LIKE ? OR display_name LIKE ? OR display_name LIKE ? )";
    private static final String[] EVENT_PROJECTION = {"_id", "eventLocation", "visible"};

    public static class Result {
        private final String mAddress;
        private final Uri mContactPhotoUri;
        private final Integer mDefaultIcon;
        private final String mName;

        public Result(String displayName, String address, Integer defaultIcon, Uri contactPhotoUri) {
            this.mName = displayName;
            this.mAddress = address;
            this.mDefaultIcon = defaultIcon;
            this.mContactPhotoUri = contactPhotoUri;
        }

        public String toString() {
            return this.mAddress;
        }
    }

    public EventLocationAdapter(Context context) {
        super(context, R.layout.location_dropdown_item, EMPTY_LIST);
        this.mResultList = new ArrayList<>();
        this.mPhotoCache = new HashMap();
        this.mResolver = context.getContentResolver();
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
    }

    @Override
    public int getCount() {
        return this.mResultList.size();
    }

    @Override
    public Result getItem(int index) {
        if (index < this.mResultList.size()) {
            return this.mResultList.get(index);
        }
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = this.mInflater.inflate(R.layout.location_dropdown_item, parent, false);
        }
        Result result = getItem(position);
        if (result != null) {
            TextView nameView = (TextView) view.findViewById(R.id.location_name);
            if (nameView != null) {
                if (result.mName == null) {
                    nameView.setVisibility(8);
                } else {
                    nameView.setVisibility(0);
                    nameView.setText(result.mName);
                }
            }
            TextView addressView = (TextView) view.findViewById(R.id.location_address);
            if (addressView != null) {
                addressView.setText(result.mAddress);
            }
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            if (imageView != null) {
                if (result.mDefaultIcon == null) {
                    imageView.setVisibility(4);
                } else {
                    imageView.setVisibility(0);
                    imageView.setImageResource(result.mDefaultIcon.intValue());
                    imageView.setTag(result.mContactPhotoUri);
                    if (result.mContactPhotoUri != null) {
                        Bitmap cachedPhoto = this.mPhotoCache.get(result.mContactPhotoUri);
                        if (cachedPhoto == null) {
                            asyncLoadPhotoAndUpdateView(result.mContactPhotoUri, imageView);
                        } else {
                            imageView.setImageBitmap(cachedPhoto);
                        }
                    }
                }
            }
        }
        return view;
    }

    private void asyncLoadPhotoAndUpdateView(final Uri contactPhotoUri, final ImageView imageView) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                InputStream imageStream = ContactsContract.Contacts.openContactPhotoInputStream(EventLocationAdapter.this.mResolver, contactPhotoUri);
                if (imageStream == null) {
                    return null;
                }
                Bitmap photo = BitmapFactory.decodeStream(imageStream);
                EventLocationAdapter.this.mPhotoCache.put(contactPhotoUri, photo);
                return photo;
            }

            @Override
            public void onPostExecute(Bitmap photo) {
                if (photo != null && imageView.getTag() == contactPhotoUri) {
                    imageView.setImageBitmap(photo);
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
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            long startTime = System.currentTimeMillis();
            final String filter = constraint == null ? "" : constraint.toString();
            if (filter.isEmpty()) {
                return null;
            }
            AsyncTask<Void, Void, List<Result>> locationsQueryTask = new AsyncTask<Void, Void, List<Result>>() {
                @Override
                protected List<Result> doInBackground(Void... params) {
                    return EventLocationAdapter.queryRecentLocations(EventLocationAdapter.this.mResolver, filter);
                }
            }.execute(new Void[0]);
            HashSet<String> contactsAddresses = new HashSet<>();
            Collection<? extends Result> contacts = EventLocationAdapter.queryContacts(EventLocationAdapter.this.mResolver, filter, contactsAddresses);
            ArrayList<Result> resultList = new ArrayList<>();
            try {
                List<Result> recentLocations = locationsQueryTask.get();
                for (Result recentLocation : recentLocations) {
                    if (recentLocation.mAddress != null && !contactsAddresses.contains(recentLocation.mAddress)) {
                        resultList.add(recentLocation);
                    }
                }
            } catch (InterruptedException e) {
                Log.e("EventLocationAdapter", "Failed waiting for locations query results.", e);
            } catch (ExecutionException e2) {
                Log.e("EventLocationAdapter", "Failed waiting for locations query results.", e2);
            }
            if (contacts != null) {
                resultList.addAll(contacts);
            }
            if (Log.isLoggable("EventLocationAdapter", 3)) {
                long duration = System.currentTimeMillis() - startTime;
                StringBuilder msg = new StringBuilder();
                msg.append("Autocomplete of ").append(constraint);
                msg.append(": location query match took ").append(duration).append("ms ");
                msg.append("(").append(resultList.size()).append(" results)");
                Log.d("EventLocationAdapter", msg.toString());
            }
            Filter.FilterResults filterResults = new Filter.FilterResults();
            filterResults.values = resultList;
            filterResults.count = resultList.size();
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            EventLocationAdapter.this.mResultList.clear();
            if (results != null && results.count > 0) {
                EventLocationAdapter.this.mResultList.addAll((ArrayList) results.values);
                EventLocationAdapter.this.notifyDataSetChanged();
            } else {
                EventLocationAdapter.this.notifyDataSetInvalidated();
            }
        }
    }

    private static List<Result> queryContacts(ContentResolver resolver, String input, HashSet<String> addressesRetVal) {
        Result result;
        String where = null;
        String[] whereArgs = null;
        if (!TextUtils.isEmpty(input)) {
            where = CONTACTS_WHERE;
            String param1 = input + "%";
            String param2 = "% " + input + "%";
            whereArgs = new String[]{param1, param2, param1, param2};
        }
        Cursor c = resolver.query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, CONTACTS_PROJECTION, where, whereArgs, "display_name ASC");
        try {
            Map<String, List<Result>> nameToAddresses = new HashMap<>();
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                String name = c.getString(1);
                String address = c.getString(2);
                if (name != null) {
                    List<Result> addressesForName = nameToAddresses.get(name);
                    if (addressesForName == null) {
                        Uri contactPhotoUri = null;
                        if (c.getLong(4) > 0) {
                            contactPhotoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.getLong(3));
                        }
                        addressesForName = new ArrayList<>();
                        nameToAddresses.put(name, addressesForName);
                        result = new Result(name, address, Integer.valueOf(R.drawable.ic_contact_picture), contactPhotoUri);
                    } else {
                        result = new Result(null, address, null, null);
                    }
                    addressesForName.add(result);
                    addressesRetVal.add(address);
                }
            }
            List<Result> allResults = new ArrayList<>();
            for (List<Result> result2 : nameToAddresses.values()) {
                allResults.addAll(result2);
            }
            return allResults;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private static List<Result> queryRecentLocations(ContentResolver resolver, String input) {
        String filter = input == null ? "" : input + "%";
        if (filter.isEmpty()) {
            return null;
        }
        Cursor c = resolver.query(CalendarContract.Events.CONTENT_URI, EVENT_PROJECTION, "visible=? AND eventLocation LIKE ?", new String[]{"1", filter}, "_id DESC");
        List<Result> recentLocations = null;
        if (c != null) {
            try {
                recentLocations = processLocationsQueryResults(c);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
    }

    private static List<Result> processLocationsQueryResults(Cursor cursor) {
        TreeSet<String> locations = new TreeSet<>((Comparator<? super String>) String.CASE_INSENSITIVE_ORDER);
        cursor.moveToPosition(-1);
        while (locations.size() < 4 && cursor.moveToNext()) {
            String location = cursor.getString(1).trim();
            locations.add(location);
        }
        List<Result> results = new ArrayList<>();
        for (String location2 : locations) {
            results.add(new Result(null, location2, Integer.valueOf(R.drawable.ic_history_holo_light), null));
        }
        return results;
    }
}
