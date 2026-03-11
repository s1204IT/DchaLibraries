package com.android.settings;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.internal.telephony.SmsApplication;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IAppListExt;
import com.mediatek.settings.ext.IRCSSettings;
import com.mediatek.settings.ext.ISmsDialogExt;
import java.util.ArrayList;
import java.util.List;

public final class SmsDefaultDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private IRCSSettings mExt;
    private SmsApplication.SmsApplicationData mNewSmsApplicationData;
    private ISmsDialogExt mSmsDialogExt;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String packageName = intent.getStringExtra("package");
        this.mSmsDialogExt = UtilsExt.getSMSApDialogPlugin(this);
        setResult(0);
        if (!buildDialog(packageName)) {
            finish();
        }
        this.mExt = UtilsExt.getRcsSettingsPlugin(this);
    }

    protected void onStart() {
        super.onStart();
        getWindow().addPrivateFlags(524288);
        EventLog.writeEvent(1397638484, "120484087", -1, "");
    }

    protected void onStop() {
        super.onStop();
        Window window = getWindow();
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.privateFlags &= -524289;
        window.setAttributes(attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                this.mSmsDialogExt.onClick(this.mNewSmsApplicationData != null ? this.mNewSmsApplicationData.mPackageName : null, this, getApplicationContext(), which);
                break;
            case -1:
                SmsApplication.setDefaultApplication(this.mNewSmsApplicationData.mPackageName, this);
                this.mSmsDialogExt.onClick(this.mNewSmsApplicationData != null ? this.mNewSmsApplicationData.mPackageName : null, this, getApplicationContext(), which);
                setResult(-1);
                break;
            default:
                if (which >= 0) {
                    AppListAdapter adapter = (AppListAdapter) this.mAlertParams.mAdapter;
                    if (!adapter.isSelected(which)) {
                        String packageName = adapter.getPackageName(which);
                        if (!TextUtils.isEmpty(packageName)) {
                            if (this.mSmsDialogExt.onClick(packageName, this, getApplicationContext(), which)) {
                                this.mExt.setDefaultSmsApplication(packageName, this);
                            }
                            setResult(-1);
                        }
                    }
                }
                break;
        }
    }

    private boolean buildDialog(String packageName) {
        TelephonyManager tm = (TelephonyManager) getSystemService("phone");
        if (!tm.isSmsCapable()) {
            return false;
        }
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.sms_change_default_dialog_title);
        this.mNewSmsApplicationData = SmsApplication.getSmsApplicationData(packageName, this);
        if (this.mNewSmsApplicationData != null) {
            SmsApplication.SmsApplicationData oldSmsApplicationData = null;
            ComponentName oldSmsComponent = SmsApplication.getDefaultSmsApplication(this, true);
            if (oldSmsComponent != null) {
                oldSmsApplicationData = SmsApplication.getSmsApplicationData(oldSmsComponent.getPackageName(), this);
                if (oldSmsApplicationData.mPackageName.equals(this.mNewSmsApplicationData.mPackageName)) {
                    return false;
                }
            }
            if (oldSmsApplicationData != null) {
                p.mMessage = getString(R.string.sms_change_default_dialog_text, new Object[]{this.mNewSmsApplicationData.mApplicationName, oldSmsApplicationData.mApplicationName});
                this.mSmsDialogExt.buildMessage(p, packageName, getIntent(), this.mNewSmsApplicationData.mApplicationName, oldSmsApplicationData.mApplicationName);
            } else {
                p.mMessage = getString(R.string.sms_change_default_no_previous_dialog_text, new Object[]{this.mNewSmsApplicationData.mApplicationName});
            }
            p.mPositiveButtonText = getString(R.string.yes);
            p.mNegativeButtonText = getString(R.string.no);
            p.mPositiveButtonListener = this;
            p.mNegativeButtonListener = this;
        } else {
            p.mAdapter = new AppListAdapter();
            p.mOnClickListener = this;
            p.mNegativeButtonText = getString(R.string.cancel);
            p.mNegativeButtonListener = this;
        }
        setupAlert();
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        this.mSmsDialogExt.onKeyDown(keyCode, event, this);
        return super.onKeyDown(keyCode, event);
    }

    private class AppListAdapter extends BaseAdapter {
        IAppListExt mAppListExt;
        private final List<Item> mItems = getItems();
        private final int mSelectedIndex;

        private class Item {
            final Drawable icon;
            final String label;
            final String packgeName;

            public Item(String label, Drawable icon, String packageName) {
                this.label = label;
                this.icon = icon;
                this.packgeName = packageName;
            }
        }

        public AppListAdapter() {
            int selected = getSelectedIndex();
            if (selected > 0) {
                Item item = this.mItems.remove(selected);
                this.mItems.add(0, item);
                selected = 0;
            }
            this.mSelectedIndex = selected;
            this.mAppListExt = UtilsExt.getAppListPlugin(SmsDefaultDialog.this);
        }

        @Override
        public int getCount() {
            if (this.mItems != null) {
                return this.mItems.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (this.mItems == null || position >= this.mItems.size()) {
                return null;
            }
            return this.mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Item item = (Item) getItem(position);
            LayoutInflater inflater = SmsDefaultDialog.this.getLayoutInflater();
            View view = inflater.inflate(R.layout.app_preference_item, parent, false);
            TextView textView = (TextView) view.findViewById(android.R.id.title);
            textView.setText(item.label);
            if (position == this.mSelectedIndex) {
                view.findViewById(R.id.default_label).setVisibility(0);
            } else {
                view.findViewById(R.id.default_label).setVisibility(8);
            }
            ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
            imageView.setImageDrawable(item.icon);
            this.mAppListExt.setAppListItem(this.mItems.get(position).packgeName, position);
            return this.mAppListExt.addLayoutAppView(view, textView, (TextView) view.findViewById(R.id.default_label), position, item.icon, parent);
        }

        public String getPackageName(int position) {
            Item item = (Item) getItem(position);
            if (item != null) {
                return item.packgeName;
            }
            return null;
        }

        public boolean isSelected(int position) {
            return position == this.mSelectedIndex;
        }

        private List<Item> getItems() {
            PackageManager pm = SmsDefaultDialog.this.getPackageManager();
            List<Item> items = new ArrayList<>();
            for (SmsApplication.SmsApplicationData app : SmsApplication.getApplicationCollection(SmsDefaultDialog.this)) {
                try {
                    String packageName = app.mPackageName;
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    if (appInfo != null) {
                        items.add(new Item(appInfo.loadLabel(pm).toString(), appInfo.loadIcon(pm), packageName));
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            return items;
        }

        private int getSelectedIndex() {
            ComponentName appName = SmsApplication.getDefaultSmsApplication(SmsDefaultDialog.this, true);
            if (appName != null) {
                String defaultSmsAppPackageName = appName.getPackageName();
                if (!TextUtils.isEmpty(defaultSmsAppPackageName)) {
                    for (int i = 0; i < this.mItems.size(); i++) {
                        if (TextUtils.equals(this.mItems.get(i).packgeName, defaultSmsAppPackageName)) {
                            return i;
                        }
                    }
                    return -1;
                }
                return -1;
            }
            return -1;
        }
    }
}
