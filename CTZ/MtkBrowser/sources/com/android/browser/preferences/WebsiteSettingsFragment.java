package com.android.browser.preferences;

import android.R;
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

        private Site(Parcel parcel) {
            this.mOrigin = parcel.readString();
            this.mTitle = parcel.readString();
            this.mFeatures = parcel.readInt();
            this.mIcon = (Bitmap) parcel.readParcelable(null);
        }

        public Site(String str) {
            this.mOrigin = str;
            this.mTitle = null;
            this.mIcon = null;
            this.mFeatures = 0;
        }

        private String hideHttp(String str) {
            return "http".equals(Uri.parse(str).getScheme()) ? str.substring(7) : str;
        }

        public void addFeature(int i) {
            this.mFeatures = (1 << i) | this.mFeatures;
        }

        @Override
        public int describeContents() {
            return 0;
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

        public int getFeatureCount() {
            int i = 0;
            int i2 = 0;
            while (true) {
                int i3 = i;
                if (i3 >= 2) {
                    return i2;
                }
                i2 += hasFeature(i3) ? 1 : 0;
                i = i3 + 1;
            }
        }

        public Bitmap getIcon() {
            return this.mIcon;
        }

        public String getOrigin() {
            return this.mOrigin;
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

        public boolean hasFeature(int i) {
            return (this.mFeatures & (1 << i)) != 0;
        }

        public void removeFeature(int i) {
            this.mFeatures = ((1 << i) ^ (-1)) & this.mFeatures;
        }

        public void setIcon(Bitmap bitmap) {
            this.mIcon = bitmap;
        }

        public void setTitle(String str) {
            this.mTitle = str;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.mOrigin);
            parcel.writeString(this.mTitle);
            parcel.writeInt(this.mFeatures);
            parcel.writeParcelable(this.mIcon, i);
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
        final WebsiteSettingsFragment this$0;

        private class UpdateFromBookmarksDbTask extends AsyncTask<Void, Void, Void> {
            private Context mContext;
            private boolean mDataSetChanged;
            private Map<String, Site> mSites;
            final SiteAdapter this$1;

            public UpdateFromBookmarksDbTask(SiteAdapter siteAdapter, Context context, Map<String, Site> map) {
                this.this$1 = siteAdapter;
                this.mContext = context.getApplicationContext();
                this.mSites = map;
            }

            @Override
            public Void doInBackground(Void... voidArr) {
                Set hashSet;
                HashMap map = new HashMap();
                for (Map.Entry<String, Site> entry : this.mSites.entrySet()) {
                    Site value = entry.getValue();
                    String host = Uri.parse(entry.getKey()).getHost();
                    if (map.containsKey(host)) {
                        hashSet = (Set) map.get(host);
                    } else {
                        hashSet = new HashSet();
                        map.put(host, hashSet);
                    }
                    hashSet.add(value);
                }
                Cursor cursorQuery = this.mContext.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"url", "title", "favicon"}, "folder == 0", null, null);
                if (cursorQuery == null) {
                    return null;
                }
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
                            Bitmap bitmapDecodeByteArray = blob != null ? BitmapFactory.decodeByteArray(blob, 0, blob.length) : null;
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
                return null;
            }

            @Override
            public void onPostExecute(Void r2) {
                if (this.mDataSetChanged) {
                    this.this$1.notifyDataSetChanged();
                }
            }
        }

        public SiteAdapter(WebsiteSettingsFragment websiteSettingsFragment, Context context, int i) {
            this(websiteSettingsFragment, context, i, null);
        }

        public SiteAdapter(WebsiteSettingsFragment websiteSettingsFragment, Context context, int i, Site site) {
            super(context, i);
            this.this$0 = websiteSettingsFragment;
            this.mResource = i;
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mDefaultIcon = BitmapFactory.decodeResource(websiteSettingsFragment.getResources(), 2130837505);
            this.mUsageEmptyIcon = BitmapFactory.decodeResource(websiteSettingsFragment.getResources(), 2130837566);
            this.mUsageLowIcon = BitmapFactory.decodeResource(websiteSettingsFragment.getResources(), 2130837567);
            this.mUsageHighIcon = BitmapFactory.decodeResource(websiteSettingsFragment.getResources(), 2130837565);
            this.mLocationAllowedIcon = BitmapFactory.decodeResource(websiteSettingsFragment.getResources(), 2130837559);
            this.mLocationDisallowedIcon = BitmapFactory.decodeResource(websiteSettingsFragment.getResources(), 2130837558);
            this.mCurrentSite = site;
            if (this.mCurrentSite == null) {
                askForOrigins();
            }
        }

        public void addFeatureToSite(Map<String, Site> map, String str, int i) {
            Site site;
            if (map.containsKey(str)) {
                site = map.get(str);
            } else {
                site = new Site(str);
                map.put(str, site);
            }
            site.addFeature(i);
        }

        public void askForGeolocation(Map<String, Site> map) {
            GeolocationPermissions.getInstance().getOrigins(new ValueCallback<Set<String>>(this, map) {
                final SiteAdapter this$1;
                final Map val$sites;

                {
                    this.this$1 = this;
                    this.val$sites = map;
                }

                @Override
                public void onReceiveValue(Set<String> set) {
                    if (set != null) {
                        Iterator<String> it = set.iterator();
                        while (it.hasNext()) {
                            this.this$1.addFeatureToSite(this.val$sites, it.next(), 1);
                        }
                    }
                    this.this$1.populateIcons(this.val$sites);
                    this.this$1.populateOrigins(this.val$sites);
                }
            });
        }

        public void askForOrigins() {
            WebStorage.getInstance().getOrigins(new ValueCallback<Map>(this) {
                final SiteAdapter this$1;

                {
                    this.this$1 = this;
                }

                @Override
                public void onReceiveValue(Map map) {
                    HashMap map2 = new HashMap();
                    if (map != null) {
                        Iterator it = map.keySet().iterator();
                        while (it.hasNext()) {
                            this.this$1.addFeatureToSite(map2, (String) it.next(), 0);
                        }
                    }
                    this.this$1.askForGeolocation(map2);
                }
            });
        }

        @Override
        public int getCount() {
            return this.mCurrentSite == null ? super.getCount() : this.mCurrentSite.getFeatureCount();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = this.mInflater.inflate(this.mResource, viewGroup, false);
            }
            TextView textView = (TextView) view.findViewById(2131558407);
            TextView textView2 = (TextView) view.findViewById(2131558558);
            ImageView imageView = (ImageView) view.findViewById(2131558509);
            ImageView imageView2 = (ImageView) view.findViewById(2131558557);
            ImageView imageView3 = (ImageView) view.findViewById(2131558556);
            ImageView imageView4 = (ImageView) view.findViewById(2131558555);
            imageView3.setVisibility(8);
            imageView4.setVisibility(8);
            if (this.mCurrentSite != null) {
                imageView.setVisibility(8);
                imageView4.setVisibility(8);
                imageView3.setVisibility(8);
                imageView2.setVisibility(0);
                String origin = this.mCurrentSite.getOrigin();
                switch (this.mCurrentSite.getFeatureByIndex(i)) {
                    case 0:
                        WebStorage.getInstance().getUsageForOrigin(origin, new ValueCallback<Long>(this, textView, textView2, imageView2) {
                            final SiteAdapter this$1;
                            final ImageView val$featureIcon;
                            final TextView val$subtitle;
                            final TextView val$title;

                            {
                                this.this$1 = this;
                                this.val$title = textView;
                                this.val$subtitle = textView2;
                                this.val$featureIcon = imageView2;
                            }

                            @Override
                            public void onReceiveValue(Long l) {
                                if (l != null) {
                                    String str = this.this$1.sizeValueToString(l.longValue()) + " " + WebsiteSettingsFragment.sMBStored;
                                    this.val$title.setText(2131493230);
                                    this.val$subtitle.setText(str);
                                    this.val$subtitle.setVisibility(0);
                                    this.this$1.setIconForUsage(this.val$featureIcon, l.longValue());
                                }
                            }
                        });
                        break;
                    case 1:
                        textView.setText(2131493249);
                        GeolocationPermissions.getInstance().getAllowed(origin, new ValueCallback<Boolean>(this, textView2, imageView2) {
                            final SiteAdapter this$1;
                            final ImageView val$featureIcon;
                            final TextView val$subtitle;

                            {
                                this.this$1 = this;
                                this.val$subtitle = textView2;
                                this.val$featureIcon = imageView2;
                            }

                            @Override
                            public void onReceiveValue(Boolean bool) {
                                if (bool != null) {
                                    if (bool.booleanValue()) {
                                        this.val$subtitle.setText(2131493250);
                                        this.val$featureIcon.setImageBitmap(this.this$1.mLocationAllowedIcon);
                                    } else {
                                        this.val$subtitle.setText(2131493251);
                                        this.val$featureIcon.setImageBitmap(this.this$1.mLocationDisallowedIcon);
                                    }
                                    this.val$subtitle.setVisibility(0);
                                }
                            }
                        });
                        break;
                }
            } else {
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
                String origin2 = item.getOrigin();
                if (item.hasFeature(0)) {
                    WebStorage.getInstance().getUsageForOrigin(origin2, new ValueCallback<Long>(this, imageView3) {
                        final SiteAdapter this$1;
                        final ImageView val$usageIcon;

                        {
                            this.this$1 = this;
                            this.val$usageIcon = imageView3;
                        }

                        @Override
                        public void onReceiveValue(Long l) {
                            if (l != null) {
                                this.this$1.setIconForUsage(this.val$usageIcon, l.longValue());
                                this.val$usageIcon.setVisibility(0);
                            }
                        }
                    });
                }
                if (item.hasFeature(1)) {
                    imageView4.setVisibility(0);
                    GeolocationPermissions.getInstance().getAllowed(origin2, new ValueCallback<Boolean>(this, imageView4) {
                        final SiteAdapter this$1;
                        final ImageView val$locationIcon;

                        {
                            this.this$1 = this;
                            this.val$locationIcon = imageView4;
                        }

                        @Override
                        public void onReceiveValue(Boolean bool) {
                            if (bool != null) {
                                if (bool.booleanValue()) {
                                    this.val$locationIcon.setImageBitmap(this.this$1.mLocationAllowedIcon);
                                } else {
                                    this.val$locationIcon.setImageBitmap(this.this$1.mLocationDisallowedIcon);
                                }
                            }
                        }
                    });
                }
            }
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            if (this.mCurrentSite != null) {
                switch (this.mCurrentSite.getFeatureByIndex(i)) {
                    case 0:
                        new AlertDialog.Builder(getContext()).setMessage(2131493231).setPositiveButton(2131493232, new DialogInterface.OnClickListener(this) {
                            final SiteAdapter this$1;

                            {
                                this.this$1 = this;
                            }

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i2) {
                                WebStorage.getInstance().deleteOrigin(this.this$1.mCurrentSite.getOrigin());
                                this.this$1.mCurrentSite.removeFeature(0);
                                if (this.this$1.mCurrentSite.getFeatureCount() == 0) {
                                    this.this$1.this$0.finish();
                                }
                                this.this$1.askForOrigins();
                                this.this$1.notifyDataSetChanged();
                            }
                        }).setNegativeButton(2131493233, (DialogInterface.OnClickListener) null).setIconAttribute(R.attr.alertDialogIcon).show();
                        break;
                    case 1:
                        new AlertDialog.Builder(getContext()).setMessage(2131493252).setPositiveButton(2131493253, new DialogInterface.OnClickListener(this) {
                            final SiteAdapter this$1;

                            {
                                this.this$1 = this;
                            }

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i2) {
                                GeolocationPermissions.getInstance().clear(this.this$1.mCurrentSite.getOrigin());
                                this.this$1.mCurrentSite.removeFeature(1);
                                if (this.this$1.mCurrentSite.getFeatureCount() == 0) {
                                    this.this$1.this$0.finish();
                                }
                                this.this$1.askForOrigins();
                                this.this$1.notifyDataSetChanged();
                            }
                        }).setNegativeButton(2131493254, (DialogInterface.OnClickListener) null).setIconAttribute(R.attr.alertDialogIcon).show();
                        break;
                }
                return;
            }
            Site site = (Site) view.getTag();
            PreferenceActivity preferenceActivity = (PreferenceActivity) this.this$0.getActivity();
            if (preferenceActivity != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("site", site);
                preferenceActivity.startPreferencePanel(WebsiteSettingsFragment.class.getName(), bundle, 0, site.getPrettyTitle(), null, 0);
            }
        }

        public void populateIcons(Map<String, Site> map) {
            new UpdateFromBookmarksDbTask(this, getContext(), map).execute(new Void[0]);
        }

        public void populateOrigins(Map<String, Site> map) {
            clear();
            Iterator<Map.Entry<String, Site>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                add(it.next().getValue());
            }
            notifyDataSetChanged();
            if (getCount() == 0) {
                this.this$0.finish();
            }
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

        public String sizeValueToString(long j) {
            if (j > 0) {
                return String.valueOf(((int) Math.ceil((j / 1048576.0f) * 10.0f)) / 10.0f);
            }
            Log.e(this.this$0.LOGTAG, "sizeValueToString called with non-positive value: " + j);
            return "0";
        }
    }

    public void finish() {
        PreferenceActivity preferenceActivity = (PreferenceActivity) getActivity();
        if (preferenceActivity != null) {
            preferenceActivity.finishPreferencePanel(this, 0, null);
        }
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if (sMBStored == null) {
            sMBStored = getString(2131493234);
        }
        this.mAdapter = new SiteAdapter(this, getActivity(), 2130968636);
        if (this.mSite != null) {
            this.mAdapter.mCurrentSite = this.mSite;
        }
        getListView().setAdapter((ListAdapter) this.mAdapter);
        getListView().setOnItemClickListener(this.mAdapter);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() != 2131558553) {
            return;
        }
        new AlertDialog.Builder(getActivity()).setMessage(2131493256).setPositiveButton(2131493257, new DialogInterface.OnClickListener(this) {
            final WebsiteSettingsFragment this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                WebStorage.getInstance().deleteAllData();
                GeolocationPermissions.getInstance().clearAll();
                WebStorageSizeManager.resetLastOutOfSpaceNotificationTime();
                this.this$0.mAdapter.askForOrigins();
                this.this$0.finish();
            }
        }).setNegativeButton(2131493258, (DialogInterface.OnClickListener) null).setIconAttribute(R.attr.alertDialogIcon).show();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(2130968635, viewGroup, false);
        Bundle arguments = getArguments();
        if (arguments != null) {
            this.mSite = (Site) arguments.getParcelable("site");
        }
        if (this.mSite == null) {
            View viewFindViewById = viewInflate.findViewById(2131558553);
            viewFindViewById.setVisibility(0);
            viewFindViewById.setOnClickListener(this);
        }
        return viewInflate;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mAdapter.askForOrigins();
    }
}
