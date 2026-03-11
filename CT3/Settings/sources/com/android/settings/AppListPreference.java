package com.android.settings;

import android.app.AlertDialog;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IAppListExt;
import java.util.ArrayList;
import java.util.List;

public class AppListPreference extends CustomListPreference {
    private Drawable[] mEntryDrawables;
    IAppListExt mExt;
    protected final boolean mForWork;
    private boolean mShowItemNone;
    private CharSequence[] mSummaries;
    private int mSystemAppIndex;
    protected final int mUserId;

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
        public boolean isEnabled(int position) {
            return AppListPreference.this.mSummaries == null || AppListPreference.this.mSummaries[position] == null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            boolean enabled = true;
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View view = inflater.inflate(R.layout.app_preference_item, parent, false);
            TextView textView = (TextView) view.findViewById(android.R.id.title);
            textView.setText(getItem(position));
            if (position == this.mSelectedIndex && position == AppListPreference.this.mSystemAppIndex) {
                view.findViewById(R.id.system_default_label).setVisibility(0);
            } else if (position == this.mSelectedIndex) {
                view.findViewById(R.id.default_label).setVisibility(0);
            } else if (position == AppListPreference.this.mSystemAppIndex) {
                view.findViewById(R.id.system_label).setVisibility(0);
            }
            ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
            imageView.setImageDrawable(this.mImageDrawables[position]);
            if (AppListPreference.this.mSummaries != null && AppListPreference.this.mSummaries[position] != null) {
                enabled = false;
            }
            view.setEnabled(enabled);
            if (!enabled) {
                TextView summary = (TextView) view.findViewById(android.R.id.summary);
                summary.setText(AppListPreference.this.mSummaries[position]);
                summary.setVisibility(0);
            }
            if (AppListPreference.this.mExt == null) {
                AppListPreference.this.mExt = UtilsExt.getAppListPlugin(getContext());
            }
            return AppListPreference.this.mExt.addLayoutAppView(view, textView, (TextView) view.findViewById(R.id.default_label), position, this.mImageDrawables[position], parent);
        }
    }

    public AppListPreference(Context context, AttributeSet attrs, int defStyle, int defAttrs) {
        super(context, attrs, defStyle, defAttrs);
        this.mShowItemNone = false;
        this.mSystemAppIndex = -1;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WorkPreference, 0, 0);
        this.mForWork = a.getBoolean(0, false);
        UserHandle managedProfile = Utils.getManagedProfile(UserManager.get(context));
        this.mUserId = (!this.mForWork || managedProfile == null) ? UserHandle.myUserId() : managedProfile.getIdentifier();
    }

    public AppListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShowItemNone = false;
        this.mSystemAppIndex = -1;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WorkPreference, 0, 0);
        this.mForWork = a.getBoolean(0, false);
        UserHandle managedProfile = Utils.getManagedProfile(UserManager.get(context));
        this.mUserId = (!this.mForWork || managedProfile == null) ? UserHandle.myUserId() : managedProfile.getIdentifier();
    }

    public void setShowItemNone(boolean showItemNone) {
        this.mShowItemNone = showItemNone;
    }

    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName) {
        setPackageNames(packageNames, defaultPackageName, null);
    }

    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName, CharSequence systemPackageName) {
        PackageManager pm = getContext().getPackageManager();
        int entryCount = packageNames.length + (this.mShowItemNone ? 1 : 0);
        List<CharSequence> applicationNames = new ArrayList<>(entryCount);
        List<CharSequence> validatedPackageNames = new ArrayList<>(entryCount);
        List<Drawable> entryDrawables = new ArrayList<>(entryCount);
        int selectedIndex = -1;
        this.mSystemAppIndex = -1;
        for (int i = 0; i < packageNames.length; i++) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfoAsUser(packageNames[i].toString(), 0, this.mUserId);
                if (this.mExt == null) {
                    this.mExt = UtilsExt.getAppListPlugin(getContext());
                }
                this.mExt.setAppListItem(appInfo.packageName, i);
                applicationNames.add(appInfo.loadLabel(pm));
                validatedPackageNames.add(appInfo.packageName);
                entryDrawables.add(appInfo.loadIcon(pm));
                if (defaultPackageName != null && appInfo.packageName.contentEquals(defaultPackageName)) {
                    selectedIndex = i;
                }
                if (appInfo.packageName != null && systemPackageName != null && appInfo.packageName.contentEquals(systemPackageName)) {
                    this.mSystemAppIndex = i;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        if (this.mShowItemNone) {
            applicationNames.add(getContext().getResources().getText(R.string.app_list_preference_none));
            validatedPackageNames.add("");
            entryDrawables.add(getContext().getDrawable(R.drawable.ic_remove_circle));
        }
        setEntries((CharSequence[]) applicationNames.toArray(new CharSequence[applicationNames.size()]));
        setEntryValues((CharSequence[]) validatedPackageNames.toArray(new CharSequence[validatedPackageNames.size()]));
        this.mEntryDrawables = (Drawable[]) entryDrawables.toArray(new Drawable[entryDrawables.size()]);
        if (selectedIndex != -1) {
            setValueIndex(selectedIndex);
        } else {
            setValue(null);
        }
    }

    public void setComponentNames(ComponentName[] componentNames, ComponentName defaultCN, CharSequence[] summaries) {
        this.mSummaries = summaries;
        PackageManager pm = getContext().getPackageManager();
        int entryCount = componentNames.length + (this.mShowItemNone ? 1 : 0);
        List<CharSequence> applicationNames = new ArrayList<>(entryCount);
        List<CharSequence> validatedComponentNames = new ArrayList<>(entryCount);
        List<Drawable> entryDrawables = new ArrayList<>(entryCount);
        int selectedIndex = -1;
        for (int i = 0; i < componentNames.length; i++) {
            try {
                ActivityInfo activityInfo = AppGlobals.getPackageManager().getActivityInfo(componentNames[i], 0, this.mUserId);
                if (activityInfo != null) {
                    applicationNames.add(activityInfo.loadLabel(pm));
                    validatedComponentNames.add(componentNames[i].flattenToString());
                    entryDrawables.add(activityInfo.loadIcon(pm));
                    if (defaultCN != null && componentNames[i].equals(defaultCN)) {
                        selectedIndex = i;
                    }
                }
            } catch (RemoteException e) {
            }
        }
        if (this.mShowItemNone) {
            applicationNames.add(getContext().getResources().getText(R.string.app_list_preference_none));
            validatedComponentNames.add("");
            entryDrawables.add(getContext().getDrawable(R.drawable.ic_remove_circle));
        }
        setEntries((CharSequence[]) applicationNames.toArray(new CharSequence[applicationNames.size()]));
        setEntryValues((CharSequence[]) validatedComponentNames.toArray(new CharSequence[validatedComponentNames.size()]));
        this.mEntryDrawables = (Drawable[]) entryDrawables.toArray(new Drawable[entryDrawables.size()]);
        if (selectedIndex != -1) {
            setValueIndex(selectedIndex);
        } else {
            setValue(null);
        }
    }

    protected ListAdapter createListAdapter() {
        boolean selectedNone;
        String selectedValue = getValue();
        if (selectedValue == null) {
            selectedNone = true;
        } else {
            selectedNone = this.mShowItemNone ? selectedValue.contentEquals("") : false;
        }
        int selectedIndex = selectedNone ? -1 : findIndexOfValue(selectedValue);
        return new AppArrayAdapter(getContext(), R.layout.app_preference_item, getEntries(), this.mEntryDrawables, selectedIndex);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
        builder.setAdapter(createListAdapter(), listener);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(getEntryValues(), getValue(), this.mSummaries, this.mShowItemNone, superState);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            this.mShowItemNone = savedState.showItemNone;
            setPackageNames(savedState.entryValues, savedState.value);
            this.mSummaries = savedState.summaries;
            super.onRestoreInstanceState(savedState.superState);
            return;
        }
        super.onRestoreInstanceState(state);
    }

    private static class SavedState implements Parcelable {
        public static Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                CharSequence[] entryValues = source.readCharSequenceArray();
                CharSequence value = source.readCharSequence();
                boolean showItemNone = source.readInt() != 0;
                Parcelable superState = source.readParcelable(getClass().getClassLoader());
                CharSequence[] summaries = source.readCharSequenceArray();
                return new SavedState(entryValues, value, summaries, showItemNone, superState);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public final CharSequence[] entryValues;
        public final boolean showItemNone;
        public final CharSequence[] summaries;
        public final Parcelable superState;
        public final CharSequence value;

        public SavedState(CharSequence[] entryValues, CharSequence value, CharSequence[] summaries, boolean showItemNone, Parcelable superState) {
            this.entryValues = entryValues;
            this.value = value;
            this.showItemNone = showItemNone;
            this.superState = superState;
            this.summaries = summaries;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeCharSequenceArray(this.entryValues);
            dest.writeCharSequence(this.value);
            dest.writeInt(this.showItemNone ? 1 : 0);
            dest.writeParcelable(this.superState, flags);
            dest.writeCharSequenceArray(this.summaries);
        }
    }
}
