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
            public Site createFromParcel(Parcel in) {
                return new Site(in, null);
            }

            @Override
            public Site[] newArray(int size) {
                return new Site[size];
            }
        };
        private int mFeatures;
        private Bitmap mIcon;
        private String mOrigin;
        private String mTitle;

        Site(Parcel in, Site site) {
            this(in);
        }

        public Site(String origin) {
            this.mOrigin = origin;
            this.mTitle = null;
            this.mIcon = null;
            this.mFeatures = 0;
        }

        public void addFeature(int feature) {
            this.mFeatures |= 1 << feature;
        }

        public void removeFeature(int feature) {
            this.mFeatures &= ~(1 << feature);
        }

        public boolean hasFeature(int feature) {
            return (this.mFeatures & (1 << feature)) != 0;
        }

        public int getFeatureCount() {
            int count = 0;
            for (int i = 0; i < 2; i++) {
                count += hasFeature(i) ? 1 : 0;
            }
            return count;
        }

        public int getFeatureByIndex(int n) {
            int j = -1;
            for (int i = 0; i < 2; i++) {
                j += hasFeature(i) ? 1 : 0;
                if (j == n) {
                    return i;
                }
            }
            return -1;
        }

        public String getOrigin() {
            return this.mOrigin;
        }

        public void setTitle(String title) {
            this.mTitle = title;
        }

        public void setIcon(Bitmap icon) {
            this.mIcon = icon;
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
            Uri uri = Uri.parse(str);
            return "http".equals(uri.getScheme()) ? str.substring(7) : str;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.mOrigin);
            dest.writeString(this.mTitle);
            dest.writeInt(this.mFeatures);
            dest.writeParcelable(this.mIcon, flags);
        }

        private Site(Parcel in) {
            this.mOrigin = in.readString();
            this.mTitle = in.readString();
            this.mFeatures = in.readInt();
            this.mIcon = (Bitmap) in.readParcelable(null);
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

        public SiteAdapter(WebsiteSettingsFragment this$0, Context context, int rsc) {
            this(context, rsc, null);
        }

        public SiteAdapter(Context context, int rsc, Site site) {
            super(context, rsc);
            this.mResource = rsc;
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mDefaultIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.app_web_browser_sm);
            this.mUsageEmptyIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.ic_list_data_off);
            this.mUsageLowIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.ic_list_data_small);
            this.mUsageHighIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.ic_list_data_large);
            this.mLocationAllowedIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.ic_gps_on_holo_dark);
            this.mLocationDisallowedIcon = BitmapFactory.decodeResource(WebsiteSettingsFragment.this.getResources(), R.drawable.ic_gps_denied_holo_dark);
            this.mCurrentSite = site;
            if (this.mCurrentSite != null) {
                return;
            }
            askForOrigins();
        }

        public void addFeatureToSite(Map<String, Site> sites, String origin, int feature) {
            Site site;
            if (sites.containsKey(origin)) {
                site = sites.get(origin);
            } else {
                site = new Site(origin);
                sites.put(origin, site);
            }
            site.addFeature(feature);
        }

        public void askForOrigins() {
            WebStorage.getInstance().getOrigins(new ValueCallback<Map>() {
                @Override
                public void onReceiveValue(Map origins) {
                    Map<String, Site> sites = new HashMap<>();
                    if (origins != null) {
                        Iterator<String> iter = origins.keySet().iterator();
                        while (iter.hasNext()) {
                            SiteAdapter.this.addFeatureToSite(sites, iter.next(), 0);
                        }
                    }
                    SiteAdapter.this.askForGeolocation(sites);
                }
            });
        }

        public void askForGeolocation(final Map<String, Site> sites) {
            GeolocationPermissions.getInstance().getOrigins(new ValueCallback<Set<String>>() {
                @Override
                public void onReceiveValue(Set<String> origins) {
                    if (origins != null) {
                        Iterator<String> iter = origins.iterator();
                        while (iter.hasNext()) {
                            SiteAdapter.this.addFeatureToSite(sites, iter.next(), 1);
                        }
                    }
                    SiteAdapter.this.populateIcons(sites);
                    SiteAdapter.this.populateOrigins(sites);
                }
            });
        }

        public void populateIcons(Map<String, Site> sites) {
            new UpdateFromBookmarksDbTask(getContext(), sites).execute(new Void[0]);
        }

        private class UpdateFromBookmarksDbTask extends AsyncTask<Void, Void, Void> {
            private Context mContext;
            private boolean mDataSetChanged;
            private Map<String, Site> mSites;

            public UpdateFromBookmarksDbTask(Context ctx, Map<String, Site> sites) {
                this.mContext = ctx.getApplicationContext();
                this.mSites = sites;
            }

            @Override
            public Void doInBackground(Void... unused) {
                Set<Site> hostSites;
                HashMap<String, Set<Site>> hosts = new HashMap<>();
                Set<Map.Entry<String, Site>> elements = this.mSites.entrySet();
                for (Map.Entry<String, Site> entry : elements) {
                    Site site = entry.getValue();
                    String host = Uri.parse(entry.getKey()).getHost();
                    if (hosts.containsKey(host)) {
                        hostSites = hosts.get(host);
                    } else {
                        hostSites = new HashSet<>();
                        hosts.put(host, hostSites);
                    }
                    hostSites.add(site);
                }
                Cursor c = this.mContext.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"url", "title", "favicon"}, "folder == 0", null, null);
                if (c != null) {
                    if (c.moveToFirst()) {
                        int urlIndex = c.getColumnIndex("url");
                        int titleIndex = c.getColumnIndex("title");
                        int faviconIndex = c.getColumnIndex("favicon");
                        do {
                            String url = c.getString(urlIndex);
                            String host2 = Uri.parse(url).getHost();
                            if (hosts.containsKey(host2)) {
                                String title = c.getString(titleIndex);
                                Bitmap bmp = null;
                                byte[] data = c.getBlob(faviconIndex);
                                if (data != null) {
                                    bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                }
                                for (Site site2 : hosts.get(host2)) {
                                    if (url.equals(site2.getOrigin()) || new String(site2.getOrigin() + "/").equals(url)) {
                                        this.mDataSetChanged = true;
                                        site2.setTitle(title);
                                    }
                                    if (bmp != null) {
                                        this.mDataSetChanged = true;
                                        site2.setIcon(bmp);
                                    }
                                }
                            }
                        } while (c.moveToNext());
                    }
                    c.close();
                    return null;
                }
                return null;
            }

            @Override
            public void onPostExecute(Void unused) {
                if (!this.mDataSetChanged) {
                    return;
                }
                SiteAdapter.this.notifyDataSetChanged();
            }
        }

        public void populateOrigins(Map<String, Site> sites) {
            clear();
            Set<Map.Entry<String, Site>> elements = sites.entrySet();
            for (Map.Entry<String, Site> entry : elements) {
                Site site = entry.getValue();
                add(site);
            }
            notifyDataSetChanged();
            if (getCount() != 0) {
                return;
            }
            WebsiteSettingsFragment.this.finish();
        }

        @Override
        public int getCount() {
            if (this.mCurrentSite == null) {
                return super.getCount();
            }
            return this.mCurrentSite.getFeatureCount();
        }

        public String sizeValueToString(long bytes) {
            if (bytes <= 0) {
                Log.e(WebsiteSettingsFragment.this.LOGTAG, "sizeValueToString called with non-positive value: " + bytes);
                return "0";
            }
            float megabytes = bytes / 1048576.0f;
            int truncated = (int) Math.ceil(megabytes * 10.0f);
            float result = truncated / 10.0f;
            return String.valueOf(result);
        }

        public void setIconForUsage(ImageView usageIcon, long usageInBytes) {
            float usageInMegabytes = usageInBytes / 1048576.0f;
            if (usageInMegabytes <= 0.1d) {
                usageIcon.setImageBitmap(this.mUsageEmptyIcon);
                return;
            }
            if (usageInMegabytes > 0.1d && usageInMegabytes <= 5.0f) {
                usageIcon.setImageBitmap(this.mUsageLowIcon);
            } else {
                if (usageInMegabytes <= 5.0f) {
                    return;
                }
                usageIcon.setImageBitmap(this.mUsageHighIcon);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = this.mInflater.inflate(this.mResource, parent, false);
            } else {
                view = convertView;
            }
            final TextView title = (TextView) view.findViewById(R.id.title);
            final TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            final ImageView featureIcon = (ImageView) view.findViewById(R.id.feature_icon);
            final ImageView usageIcon = (ImageView) view.findViewById(R.id.usage_icon);
            final ImageView locationIcon = (ImageView) view.findViewById(R.id.location_icon);
            usageIcon.setVisibility(8);
            locationIcon.setVisibility(8);
            if (this.mCurrentSite == null) {
                Site site = getItem(position);
                title.setText(site.getPrettyTitle());
                String subtitleText = site.getPrettyOrigin();
                if (subtitleText != null) {
                    title.setMaxLines(1);
                    title.setSingleLine(true);
                    subtitle.setVisibility(0);
                    subtitle.setText(subtitleText);
                } else {
                    subtitle.setVisibility(8);
                    title.setMaxLines(2);
                    title.setSingleLine(false);
                }
                icon.setVisibility(0);
                usageIcon.setVisibility(4);
                locationIcon.setVisibility(4);
                featureIcon.setVisibility(8);
                Bitmap bmp = site.getIcon();
                if (bmp == null) {
                    bmp = this.mDefaultIcon;
                }
                icon.setImageBitmap(bmp);
                view.setTag(site);
                String origin = site.getOrigin();
                if (site.hasFeature(0)) {
                    WebStorage.getInstance().getUsageForOrigin(origin, new ValueCallback<Long>() {
                        @Override
                        public void onReceiveValue(Long value) {
                            if (value == null) {
                                return;
                            }
                            SiteAdapter.this.setIconForUsage(usageIcon, value.longValue());
                            usageIcon.setVisibility(0);
                        }
                    });
                }
                if (site.hasFeature(1)) {
                    locationIcon.setVisibility(0);
                    GeolocationPermissions.getInstance().getAllowed(origin, new ValueCallback<Boolean>() {
                        @Override
                        public void onReceiveValue(Boolean allowed) {
                            if (allowed == null) {
                                return;
                            }
                            if (allowed.booleanValue()) {
                                locationIcon.setImageBitmap(SiteAdapter.this.mLocationAllowedIcon);
                            } else {
                                locationIcon.setImageBitmap(SiteAdapter.this.mLocationDisallowedIcon);
                            }
                        }
                    });
                }
            } else {
                icon.setVisibility(8);
                locationIcon.setVisibility(8);
                usageIcon.setVisibility(8);
                featureIcon.setVisibility(0);
                String origin2 = this.mCurrentSite.getOrigin();
                switch (this.mCurrentSite.getFeatureByIndex(position)) {
                    case 0:
                        WebStorage.getInstance().getUsageForOrigin(origin2, new ValueCallback<Long>() {
                            @Override
                            public void onReceiveValue(Long value) {
                                if (value == null) {
                                    return;
                                }
                                String usage = SiteAdapter.this.sizeValueToString(value.longValue()) + " " + WebsiteSettingsFragment.sMBStored;
                                title.setText(R.string.webstorage_clear_data_title);
                                subtitle.setText(usage);
                                subtitle.setVisibility(0);
                                SiteAdapter.this.setIconForUsage(featureIcon, value.longValue());
                            }
                        });
                        break;
                    case 1:
                        title.setText(R.string.geolocation_settings_page_title);
                        GeolocationPermissions.getInstance().getAllowed(origin2, new ValueCallback<Boolean>() {
                            @Override
                            public void onReceiveValue(Boolean allowed) {
                                if (allowed == null) {
                                    return;
                                }
                                if (allowed.booleanValue()) {
                                    subtitle.setText(R.string.geolocation_settings_page_summary_allowed);
                                    featureIcon.setImageBitmap(SiteAdapter.this.mLocationAllowedIcon);
                                } else {
                                    subtitle.setText(R.string.geolocation_settings_page_summary_not_allowed);
                                    featureIcon.setImageBitmap(SiteAdapter.this.mLocationDisallowedIcon);
                                }
                                subtitle.setVisibility(0);
                            }
                        });
                        break;
                }
            }
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (this.mCurrentSite != null) {
                switch (this.mCurrentSite.getFeatureByIndex(position)) {
                    case 0:
                        new AlertDialog.Builder(getContext()).setMessage(R.string.webstorage_clear_data_dialog_message).setPositiveButton(R.string.webstorage_clear_data_dialog_ok_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dlg, int which) {
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
                            public void onClick(DialogInterface dlg, int which) {
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
                return;
            }
            Site site = (Site) view.getTag();
            PreferenceActivity activity = (PreferenceActivity) WebsiteSettingsFragment.this.getActivity();
            if (activity != null) {
                Bundle args = new Bundle();
                args.putParcelable("site", site);
                activity.startPreferencePanel(WebsiteSettingsFragment.class.getName(), args, 0, site.getPrettyTitle(), null, 0);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.website_settings, container, false);
        Bundle args = getArguments();
        if (args != null) {
            this.mSite = (Site) args.getParcelable("site");
        }
        if (this.mSite == null) {
            View clear = view.findViewById(R.id.clear_all_button);
            clear.setVisibility(0);
            clear.setOnClickListener(this);
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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

    public void finish() {
        PreferenceActivity activity = (PreferenceActivity) getActivity();
        if (activity == null) {
            return;
        }
        activity.finishPreferencePanel(this, 0, null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clear_all_button:
                new AlertDialog.Builder(getActivity()).setMessage(R.string.website_settings_clear_all_dialog_message).setPositiveButton(R.string.website_settings_clear_all_dialog_ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dlg, int which) {
                        WebStorage.getInstance().deleteAllData();
                        GeolocationPermissions.getInstance().clearAll();
                        WebStorageSizeManager.resetLastOutOfSpaceNotificationTime();
                        WebsiteSettingsFragment.this.mAdapter.askForOrigins();
                        WebsiteSettingsFragment.this.finish();
                    }
                }).setNegativeButton(R.string.website_settings_clear_all_dialog_cancel_button, (DialogInterface.OnClickListener) null).setIconAttribute(android.R.attr.alertDialogIcon).show();
                break;
        }
    }
}
