package com.android.phone.settings.fdn;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import com.android.phone.ADNList;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;

public class FdnList extends ADNList {
    private static final Uri FDN_CONTENT_URI = Uri.parse("content://icc/fdn");
    private SubscriptionInfoHelper mSubscriptionInfoHelper;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.fdn_list_with_label);
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
        Resources r = getResources();
        menu.add(0, 1, 0, r.getString(R.string.menu_add)).setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, 2, 0, r.getString(R.string.menu_edit)).setIcon(android.R.drawable.ic_menu_edit);
        menu.add(0, 3, 0, r.getString(R.string.menu_delete)).setIcon(android.R.drawable.ic_menu_delete);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean hasSelection = getSelectedItemPosition() >= 0;
        menu.findItem(1).setVisible(true);
        menu.findItem(2).setVisible(hasSelection);
        menu.findItem(3).setVisible(hasSelection);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                addContact();
                return true;
            case 2:
                editSelected();
                return true;
            case 3:
                deleteSelected();
                return true;
            case android.R.id.home:
                Intent intent = this.mSubscriptionInfoHelper.getIntent(FdnSetting.class);
                intent.setAction("android.intent.action.MAIN");
                intent.addFlags(67108864);
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        editSelected(position);
    }

    private void addContact() {
        Intent intent = this.mSubscriptionInfoHelper.getIntent(EditFdnContactScreen.class);
        startActivity(intent);
    }

    private void editSelected() {
        editSelected(getSelectedItemPosition());
    }

    private void editSelected(int position) {
        if (this.mCursor.moveToPosition(position)) {
            String name = this.mCursor.getString(0);
            String number = this.mCursor.getString(1);
            Intent intent = this.mSubscriptionInfoHelper.getIntent(EditFdnContactScreen.class);
            intent.putExtra("name", name);
            intent.putExtra("number", number);
            startActivity(intent);
        }
    }

    private void deleteSelected() {
        if (this.mCursor.moveToPosition(getSelectedItemPosition())) {
            String name = this.mCursor.getString(0);
            String number = this.mCursor.getString(1);
            Intent intent = this.mSubscriptionInfoHelper.getIntent(DeleteFdnContactScreen.class);
            intent.putExtra("name", name);
            intent.putExtra("number", number);
            startActivity(intent);
        }
    }

    public static Uri getContentUri(SubscriptionInfoHelper subscriptionInfoHelper) {
        return subscriptionInfoHelper.hasSubId() ? Uri.parse("content://icc/fdn/subId/" + subscriptionInfoHelper.getSubId()) : FDN_CONTENT_URI;
    }
}
