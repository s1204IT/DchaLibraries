package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;

public class AppListPreference extends ListPreference {
    private Drawable[] mEntryDrawables;

    public class AppArrayAdapter extends ArrayAdapter<CharSequence> {
        private Drawable[] mImageDrawables;
        private int mSelectedIndex;

        public AppArrayAdapter(Context context, int textViewResourceId, CharSequence[] objects, Drawable[] imageDrawables, int selectedIndex) {
            super(context, textViewResourceId, objects);
            this.mImageDrawables = null;
            this.mSelectedIndex = 0;
            this.mSelectedIndex = selectedIndex;
            this.mImageDrawables = imageDrawables;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            View view = inflater.inflate(R.layout.app_preference_item, parent, false);
            CheckedTextView checkedTextView = (CheckedTextView) view.findViewById(R.id.app_label);
            checkedTextView.setText(getItem(position));
            if (position == this.mSelectedIndex) {
                checkedTextView.setChecked(true);
            }
            ImageView imageView = (ImageView) view.findViewById(R.id.app_image);
            imageView.setImageDrawable(this.mImageDrawables[position]);
            return view;
        }
    }

    public AppListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AppListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPackageNames(String[] packageNames, String defaultPackageName) {
        int foundPackages = 0;
        PackageManager pm = getContext().getPackageManager();
        ApplicationInfo[] appInfos = new ApplicationInfo[packageNames.length];
        for (int i = 0; i < packageNames.length; i++) {
            try {
                appInfos[i] = pm.getApplicationInfo(packageNames[i], 0);
                foundPackages++;
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        CharSequence[] applicationNames = new CharSequence[foundPackages];
        this.mEntryDrawables = new Drawable[foundPackages];
        int index = 0;
        int selectedIndex = -1;
        for (ApplicationInfo appInfo : appInfos) {
            if (appInfo != null) {
                applicationNames[index] = appInfo.loadLabel(pm);
                this.mEntryDrawables[index] = appInfo.loadIcon(pm);
                if (defaultPackageName != null && appInfo.packageName.contentEquals(defaultPackageName)) {
                    selectedIndex = index;
                }
                index++;
            }
        }
        setEntries(applicationNames);
        setEntryValues(packageNames);
        if (selectedIndex != -1) {
            setValueIndex(selectedIndex);
        } else {
            setValue(null);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        int selectedIndex = findIndexOfValue(getValue());
        ListAdapter adapter = new AppArrayAdapter(getContext(), R.layout.app_preference_item, getEntries(), this.mEntryDrawables, selectedIndex);
        builder.setAdapter(adapter, this);
        super.onPrepareDialogBuilder(builder);
    }
}
