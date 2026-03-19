package com.android.browser.preferences;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebStorage;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.android.browser.R;
import com.android.browser.WebStorageSizeManager;
import com.android.browser.provider.BrowserContract;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class WebsiteSettingsFragment extends ListFragment implements View.OnClickListener {
    private static String sMBStored = null;
    private String LOGTAG = "WebsiteSettingsActivity";
    private SiteAdapter mAdapter = null;
    private Site mSite = null;

    static class Site implements Parcelable {
        public static final Parcelable.Creator<Site> CREATOR = new Parcelable.Creator<Site>() {
            @Override
            public Site createFromParcel(Parcel parcel) {
                return new Site(parcel);
            }

            @Override
            public Site[] newArray(int i) {
                return new Site[i];
            }
        };
        private int mFeatures;
        private Bitmap mIcon;
        private String mOrigin;
        private String mTitle;

        public Site(String str) {
            this.mOrigin = str;
            this.mTitle = null;
            this.mIcon = null;
            this.mFeatures = 0;
        }

        public void addFeature(int i) {
            this.mFeatures = (1 << i) | this.mFeatures;
        }

        public void removeFeature(int i) {
            this.mFeatures = (~(1 << i)) & this.mFeatures;
        }

        public boolean hasFeature(int i) {
            return ((1 << i) & this.mFeatures) != 0;
        }

        public int getFeatureCount() {
            int i = 0;
            for (int i2 = 0; i2 < 2; i2++) {
                i += hasFeature(i2) ? 1 : 0;
            }
            return i;
        }

        public int getFeatureByIndex(int i) {
            int i2 = -1;
            for (int i3 = 0; i3 < 2; i3++) {
                i2 += hasFeature(i3) ? 1 : 0;
                if (i2 == i) {
                    return i3;
                }
            }
            return -1;
        }

        public String getOrigin() {
            return this.mOrigin;
        }

        public void setTitle(String str) {
            this.mTitle = str;
        }

        public void setIcon(Bitmap bitmap) {
            this.mIcon = bitmap;
        }

        public Bitmap getIcon() {
            return this.mIcon;
        }

        public String getPrettyOrigin() {
            if (this.mTitle == null) {
                return null;
            }
            return hideHttp(this.mOrigin);
        }

        public String getPrettyTitle() {
            return this.mTitle == null ? hideHttp(this.mOrigin) : this.mTitle;
        }

        private String hideHttp(String str) {
            return "http".equals(Uri.parse(str).getScheme()) ? str.substring(7) : str;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.mOrigin);
            parcel.writeString(this.mTitle);
            parcel.writeInt(this.mFeatures);
            parcel.writeParcelable(this.mIcon, i);
        }

        private Site(Parcel parcel) {
            this.mOrigin = parcel.readString();
            this.mTitle = parcel.readString();
            this.mFeatures = parcel.readInt();
            this.mIcon = (Bitmap) parcel.readParcelable(null);
        }
    }

    class SiteAdapter extends ArrayAdapter<Site> implements AdapterView.OnItemClickListener {
        private Site mCurrentSite;
        private Bitmap mDefaultIcon;
        private LayoutInflater mInflater;
        private Bitmap mLocationAllowedIcon;
        private Bitmap mLocationDisallowedIcon;
        private int mResource;
        private Bitmap mUsageEmptyIcon;
        private Bitmap mUsageHighIcon;
        private Bitmap mUsageLowIcon;

        public SiteAdapter(WebsiteSettingsFragment websiteSettingsFragment, Context context, int i) {
            this(context, i, null);
        }

        public SiteAdapter(Context context, int i, Site site) {
            super(context, i);
            this.mResource = i;
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mDefaultIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.app_web_browser_sm);
            this.mUsageEmptyIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.ic_list_data_off);
            this.mUsageLowIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.ic_list_data_small);
            this.mUsageHighIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.ic_list_data_large);
            this.mLocationAllowedIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.ic_gps_on_holo_dark);
            this.mLocationDisallowedIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.ic_gps_denied_holo_dark);
            this.mCurrentSite = site;
            if (this.mCurrentSite == null) {
                askForOrigins();
            }
        }

        private void addFeatureToSite(Map<String, Site> map, String str, int i) {
            Site site;
            if (map.containsKey(str)) {
                site = map.get(str);
            } else {
                Site site2 = new Site(str);
                map.put(str, site2);
                site = site2;
            }
            site.addFeature(i);
        }

        public void askForOrigins() {
            WebStorage.getInstance().getOrigins(new ValueCallback<Map>() {
                @Override
                public void onReceiveValue(Map map) {
                    HashMap map2 = new HashMap();
                    if (map != null) {
                        Iterator it = map.keySet().iterator();
                        while (it.hasNext()) {
                            SiteAdapter.this.addFeatureToSite(map2, (String) it.next(), 0);
                        }
                    }
                    SiteAdapter.this.askForGeolocation(map2);
                }
            });
        }

        public void askForGeolocation(final Map<String, Site> map) {
            GeolocationPermissions.getInstance().getOrigins(new ValueCallback<Set<String>>() {
                @Override
                public void onReceiveValue(Set<String> set) {
                    if (set != null) {
                        Iterator<String> it = set.iterator();
                        while (it.hasNext()) {
                            SiteAdapter.this.addFeatureToSite(map, it.next(), 1);
                        }
                    }
                    SiteAdapter.this.populateIcons(map);
                    SiteAdapter.this.populateOrigins(map);
                }
            });
        }

        public void populateIcons(Map<String, Site> map) {
            new UpdateFromBookmarksDbTask(getContext(), map).execute(new Void[0]);
        }

        private class UpdateFromBookmarksDbTask extends AsyncTask<Void, Void, Void> {
            private Context mContext;
            private boolean mDataSetChanged;
            private Map<String, Site> mSites;

            public UpdateFromBookmarksDbTask(Context context, Map<String, Site> map) {
                this.mContext = context.getApplicationContext();
                this.mSites = map;
            }

            @Override
            protected Void doInBackground(Void... voidArr) {
                Bitmap bitmapDecodeByteArray;
                Set set;
                HashMap map = new HashMap();
                for (Map.Entry<String, Site> entry : this.mSites.entrySet()) {
                    Site value = entry.getValue();
                    String host = Uri.parse(entry.getKey()).getHost();
                    if (map.containsKey(host)) {
                        set = (Set) map.get(host);
                    } else {
                        HashSet hashSet = new HashSet();
                        map.put(host, hashSet);
                        set = hashSet;
                    }
                    set.add(value);
                }
                Cursor cursorQuery = this.mContext.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"url", "title", "favicon"}, "folder == 0", null, null);
                if (cursorQuery != null) {
                    if (cursorQuery.moveToFirst()) {
                        int columnIndex = cursorQuery.getColumnIndex("url");
                        int columnIndex2 = cursorQuery.getColumnIndex("title");
                        int columnIndex3 = cursorQuery.getColumnIndex("favicon");
                        do {
                            String string = cursorQuery.getString(columnIndex);
                            String host2 = Uri.parse(string).getHost();
                            if (map.containsKey(host2)) {
                                String string2 = cursorQuery.getString(columnIndex2);
                                byte[] blob = cursorQuery.getBlob(columnIndex3);
                                if (blob != null) {
                                    bitmapDecodeByteArray = BitmapFactory.decodeByteArray(blob, 0, blob.length);
                                } else {
                                    bitmapDecodeByteArray = null;
                                }
                                for (Site site : (Set) map.get(host2)) {
                                    if (!string.equals(site.getOrigin())) {
                                        if (new String(site.getOrigin() + "/").equals(string)) {
                                            this.mDataSetChanged = true;
                                            site.setTitle(string2);
                                        }
                                    }
                                    if (bitmapDecodeByteArray != null) {
                                        this.mDataSetChanged = true;
                                        site.setIcon(bitmapDecodeByteArray);
                                    }
                                }
                            }
                        } while (cursorQuery.moveToNext());
                    }
                    cursorQuery.close();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void r1) {
                if (this.mDataSetChanged) {
                    SiteAdapter.this.notifyDataSetChanged();
                }
            }
        }

        public void populateOrigins(Map<String, Site> map) {
            clear();
            Iterator<Map.Entry<String, Site>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                add(it.next().getValue());
            }
            notifyDataSetChanged();
            if (getCount() == 0) {
                WebsiteSettingsFragment.this.finish();
            }
        }

        @Override
        public int getCount() {
            if (this.mCurrentSite == null) {
                return super.getCount();
            }
            return this.mCurrentSite.getFeatureCount();
        }

        public String sizeValueToString(long j) {
            if (j <= 0) {
                Log.e(WebsiteSettingsFragment.this.LOGTAG, "sizeValueToString called with non-positive value: " + j);
                return "0";
            }
            return String.valueOf(((int) Math.ceil((j / 1048576.0f) * 10.0f)) / 10.0f);
        }

        public void setIconForUsage(ImageView imageView, long j) {
            float f = j / 1048576.0f;
            double d = f;
            if (d <= 0.1d) {
                imageView.setImageBitmap(this.mUsageEmptyIcon);
                return;
            }
            if (d > 0.1d && f <= 5.0f) {
                imageView.setImageBitmap(this.mUsageLowIcon);
            } else if (f > 5.0f) {
                imageView.setImageBitmap(this.mUsageHighIcon);
            }
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = this.mInflater.inflate(this.mResource, viewGroup, false);
            }
            final TextView textView = (TextView) view.findViewById(R.id.title);
            final TextView textView2 = (TextView) view.findViewById(R.id.subtitle);
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            final ImageView imageView2 = (ImageView) view.findViewById(R.id.feature_icon);
            final ImageView imageView3 = (ImageView) view.findViewById(R.id.usage_icon);
            final ImageView imageView4 = (ImageView) view.findViewById(R.id.location_icon);
            imageView3.setVisibility(8);
            imageView4.setVisibility(8);
            if (this.mCurrentSite == null) {
                Site item = getItem(i);
                textView.setText(item.getPrettyTitle());
                String prettyOrigin = item.getPrettyOrigin();
                if (prettyOrigin != null) {
                    textView.setMaxLines(1);
                    textView.setSingleLine(true);
                    textView2.setVisibility(0);
                    textView2.setText(prettyOrigin);
                } else {
                    textView2.setVisibility(8);
                    textView.setMaxLines(2);
                    textView.setSingleLine(false);
                }
                imageView.setVisibility(0);
                imageView3.setVisibility(4);
                imageView4.setVisibility(4);
                imageView2.setVisibility(8);
                Bitmap icon = item.getIcon();
                if (icon == null) {
                    icon = this.mDefaultIcon;
                }
                imageView.setImageBitmap(icon);
                view.setTag(item);
                String origin = item.getOrigin();
                if (item.hasFeature(0)) {
                    WebStorage.getInstance().getUsageForOrigin(origin, new ValueCallback<Long>() {
                        @Override
                        public void onReceiveValue(Long l) {
                            if (l != null) {
                                SiteAdapter.this.setIconForUsage(imageView3, l.longValue());
                                imageView3.setVisibility(0);
                            }
                        }
                    });
                }
                if (item.hasFeature(1)) {
                    imageView4.setVisibility(0);
                    GeolocationPermissions.getInstance().getAllowed(origin, new ValueCallback<Boolean>() {
                        @Override
                        public void onReceiveValue(Boolean bool) {
                            if (bool != null) {
                                if (bool.booleanValue()) {
                                    imageView4.setImageBitmap(SiteAdapter.this.mLocationAllowedIcon);
                                } else {
                                    imageView4.setImageBitmap(SiteAdapter.this.mLocationDisallowedIcon);
                                }
                            }
                        }
                    });
                }
            } else {
                imageView.setVisibility(8);
                imageView4.setVisibility(8);
                imageView3.setVisibility(8);
                imageView2.setVisibility(0);
                String origin2 = this.mCurrentSite.getOrigin();
                switch (this.mCurrentSite.getFeatureByIndex(i)) {
                    case 0:
                        WebStorage.getInstance().getUsageForOrigin(origin2, new ValueCallback<Long>() {
                            @Override
                            public void onReceiveValue(Long l) {
                                if (l != null) {
                                    String str = SiteAdapter.this.sizeValueToString(l.longValue()) + " " + WebsiteSettingsFragment.sMBStored;
                                    textView.setText(R.string.webstorage_clear_data_title);
                                    textView2.setText(str);
                                    textView2.setVisibility(0);
                                    SiteAdapter.this.setIconForUsage(imageView2, l.longValue());
                                }
                            }
                        });
                        break;
                    case 1:
                        textView.setText(R.string.geolocation_settings_page_title);
                        GeolocationPermissions.getInstance().getAllowed(origin2, new ValueCallback<Boolean>() {
                            @Override
                            public void onReceiveValue(Boolean bool) {
                                if (bool != null) {
                                    if (bool.booleanValue()) {
                                        textView2.setText(R.string.geolocation_settings_page_summary_allowed);
                                        imageView2.setImageBitmap(SiteAdapter.this.mLocationAllowedIcon);
                                    } else {
                                        textView2.setText(R.string.geolocation_settings_page_summary_not_allowed);
                                        imageView2.setImageBitmap(SiteAdapter.this.mLocationDisallowedIcon);
                                    }
                                    textView2.setVisibility(0);
                                }
                            }
                        });
                        break;
                }
            }
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            if (this.mCurrentSite != null) {
                switch (this.mCurrentSite.getFeatureByIndex(i)) {
                    case 0:
                        new AlertDialog.Builder(getContext()).setMessage(R.string.webstorage_clear_data_dialog_message).setPositiveButton(R.string.webstorage_clear_data_dialog_ok_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i2) {
                                WebStorage.getInstance().deleteOrigin(SiteAdapter.this.mCurrentSite.getOrigin());
                                SiteAdapter.this.mCurrentSite.removeFeature(0);
                                if (SiteAdapter.this.mCurrentSite.getFeatureCount() == 0) {
                                    WebsiteSettingsFragment.this.finish();
                                }
                                SiteAdapter.this.askForOrigins();
                                SiteAdapter.this.notifyDataSetChanged();
                            }
                        }).setNegativeButton(R.string.webstorage_clear_data_dialog_cancel_button, (DialogInterface.OnClickListener) null).setIconAttribute(android.R.attr.alertDialogIcon).show();
                        break;
                    case 1:
                        new AlertDialog.Builder(getContext()).setMessage(R.string.geolocation_settings_page_dialog_message).setPositiveButton(R.string.geolocation_settings_page_dialog_ok_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i2) {
                                GeolocationPermissions.getInstance().clear(SiteAdapter.this.mCurrentSite.getOrigin());
                                SiteAdapter.this.mCurrentSite.removeFeature(1);
                                if (SiteAdapter.this.mCurrentSite.getFeatureCount() == 0) {
                                    WebsiteSettingsFragment.this.finish();
                                }
                                SiteAdapter.this.askForOrigins();
                                SiteAdapter.this.notifyDataSetChanged();
                            }
                        }).setNegativeButton(R.string.geolocation_settings_page_dialog_cancel_button, (DialogInterface.OnClickListener) null).setIconAttribute(android.R.attr.alertDialogIcon).show();
                        break;
                }
            }
            Site site = (Site) view.getTag();
            PreferenceActivity preferenceActivity = (PreferenceActivity) WebsiteSettingsFragment.this.getActivity();
            if (preferenceActivity != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("site", site);
                preferenceActivity.startPreferencePanel(WebsiteSettingsFragment.class.getName(), bundle, 0, site.getPrettyTitle(), null, 0);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.website_settings, viewGroup, false);
        Bundle arguments = getArguments();
        if (arguments != null) {
            this.mSite = (Site) arguments.getParcelable("site");
        }
        if (this.mSite == null) {
            View viewFindViewById = viewInflate.findViewById(R.id.clear_all_button);
            viewFindViewById.setVisibility(0);
            viewFindViewById.setOnClickListener(this);
        }
        return viewInflate;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if (sMBStored == null) {
            sMBStored = getString(R.string.webstorage_origin_summary_mb_stored);
        }
        this.mAdapter = new SiteAdapter(this, getActivity(), R.layout.website_settings_row);
        if (this.mSite != null) {
            this.mAdapter.mCurrentSite = this.mSite;
        }
        getListView().setAdapter((ListAdapter) this.mAdapter);
        getListView().setOnItemClickListener(this.mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mAdapter.askForOrigins();
    }

    private void finish() {
        PreferenceActivity preferenceActivity = (PreferenceActivity) getActivity();
        if (preferenceActivity != null) {
            preferenceActivity.finishPreferencePanel(this, 0, null);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.clear_all_button) {
            new AlertDialog.Builder(getActivity()).setMessage(R.string.website_settings_clear_all_dialog_message).setPositiveButton(R.string.website_settings_clear_all_dialog_ok_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    WebStorage.getInstance().deleteAllData();
                    GeolocationPermissions.getInstance().clearAll();
                    WebStorageSizeManager.resetLastOutOfSpaceNotificationTime();
                    WebsiteSettingsFragment.this.mAdapter.askForOrigins();
                    WebsiteSettingsFragment.this.finish();
                }
            }).setNegativeButton(R.string.website_settings_clear_all_dialog_cancel_button, (DialogInterface.OnClickListener) null).setIconAttribute(android.R.attr.alertDialogIcon).show();
        }
    }
}
