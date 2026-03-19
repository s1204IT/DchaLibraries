package com.android.phone.settings.fdn;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.PopupMenu;
import com.android.phone.ADNList;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;

public class FdnList extends ADNList implements PhoneGlobals.SubInfoUpdateListener {
    private static final Uri FDN_CONTENT_URI = Uri.parse("content://icc/fdn");
    private SelectionPopupMenu mPopup;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private boolean mFdnDialDirectlySupported = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                FdnList.this.log("AIRPLANE_MODE_CHANGED, so finish FDN Settings");
                FdnList.this.finish();
            }
        }
    };

    private class SelectionPopupMenu extends PopupMenu {
        private PopupMenu.OnMenuItemClickListener mMenuItemListener;
        private final int position;

        public SelectionPopupMenu(Context context, View view, int i) {
            super(context, view, 5);
            this.mMenuItemListener = new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (menuItem.getItemId() == 2) {
                        FdnList.this.editSelected(SelectionPopupMenu.this.position);
                        return true;
                    }
                    if (menuItem.getItemId() == 3) {
                        FdnList.this.deleteSelected(SelectionPopupMenu.this.position);
                        return true;
                    }
                    if (menuItem.getItemId() == 4) {
                        FdnList.this.dialSelected(SelectionPopupMenu.this.position);
                        return true;
                    }
                    return true;
                }
            };
            this.position = i;
        }

        public void showPopUp() {
            getMenu().add(0, 2, 0, FdnList.this.getString(R.string.menu_edit));
            getMenu().add(0, 3, 0, FdnList.this.getString(R.string.menu_delete));
            if (FdnList.this.mFdnDialDirectlySupported) {
                getMenu().add(0, 4, 0, FdnList.this.getString(R.string.menu_dial));
            }
            setOnMenuItemClickListener(this.mMenuItemListener);
            show();
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.fdn_list_with_label);
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mFdnDialDirectlySupported = getFdnDialDirectlySupported();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mPopup != null) {
            this.mPopup.dismiss();
        }
    }

    @Override
    protected Uri resolveIntent() {
        Intent intent = getIntent();
        intent.setData(getContentUri(this.mSubscriptionInfoHelper));
        return intent.getData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        Resources resources = getResources();
        menu.add(0, 1, 0, resources.getString(R.string.menu_add)).setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, 2, 0, resources.getString(R.string.menu_edit)).setIcon(android.R.drawable.ic_menu_edit);
        menu.add(0, 3, 0, resources.getString(R.string.menu_delete)).setIcon(android.R.drawable.ic_menu_delete);
        menu.add(0, 4, 0, resources.getString(R.string.menu_dial));
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean z = false;
        boolean z2 = getSelectedItemPosition() >= 0;
        menu.findItem(1).setVisible(true);
        menu.findItem(2).setVisible(z2);
        menu.findItem(3).setVisible(z2);
        MenuItem menuItemFindItem = menu.findItem(4);
        if (z2 && this.mFdnDialDirectlySupported) {
            z = true;
        }
        menuItemFindItem.setVisible(z);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            Intent intent = this.mSubscriptionInfoHelper.getIntent(FdnSetting.class);
            intent.setAction("android.intent.action.MAIN");
            intent.addFlags(67108864);
            startActivity(intent);
            finish();
            return true;
        }
        switch (itemId) {
            case 1:
                addContact();
                return true;
            case 2:
                editSelected();
                return true;
            case 3:
                deleteSelected();
                return true;
            case 4:
                dialSelected();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int i, long j) {
        this.mPopup = new SelectionPopupMenu(this, view, i);
        this.mPopup.showPopUp();
    }

    private void addContact() {
        Intent intent = this.mSubscriptionInfoHelper.getIntent(EditFdnContactScreen.class);
        intent.putExtra("addContact", true);
        startActivity(intent);
    }

    private void editSelected() {
        editSelected(getSelectedItemPosition());
    }

    private void editSelected(int i) {
        if (this.mCursor.moveToPosition(i)) {
            String string = this.mCursor.getString(1);
            String string2 = this.mCursor.getString(2);
            Intent intent = this.mSubscriptionInfoHelper.getIntent(EditFdnContactScreen.class);
            intent.putExtra("name", string);
            intent.putExtra("number", string2);
            intent.putExtra("addContact", false);
            startActivity(intent);
        }
    }

    private void deleteSelected() {
        deleteSelected(getSelectedItemPosition());
    }

    private void deleteSelected(int i) {
        if (this.mCursor.moveToPosition(i)) {
            String string = this.mCursor.getString(1);
            String string2 = this.mCursor.getString(2);
            Intent intent = this.mSubscriptionInfoHelper.getIntent(DeleteFdnContactScreen.class);
            intent.putExtra("name", string);
            intent.putExtra("number", string2);
            startActivity(intent);
        }
    }

    private void dialSelected() {
        dialSelected(getSelectedItemPosition());
    }

    private void dialSelected(int i) {
        if (this.mCursor.moveToPosition(i)) {
            String string = this.mCursor.getString(2);
            if (!TextUtils.isEmpty(string)) {
                startActivity(new Intent("android.intent.action.CALL_PRIVILEGED", Uri.fromParts("tel", string, null)));
            }
        }
    }

    public static Uri getContentUri(SubscriptionInfoHelper subscriptionInfoHelper) {
        if (subscriptionInfoHelper.hasSubId()) {
            return Uri.parse("content://icc/fdn/subId/" + subscriptionInfoHelper.getSubId());
        }
        return FDN_CONTENT_URI;
    }

    private boolean getFdnDialDirectlySupported() {
        int defaultSubscriptionId;
        if (this.mSubscriptionInfoHelper.hasSubId()) {
            defaultSubscriptionId = this.mSubscriptionInfoHelper.getSubId();
        } else {
            defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
        }
        return PhoneGlobals.getInstance().getCarrierConfigForSubId(defaultSubscriptionId).getBoolean("support_direct_fdn_dialing_bool");
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(this.mReceiver);
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        super.onDestroy();
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}
