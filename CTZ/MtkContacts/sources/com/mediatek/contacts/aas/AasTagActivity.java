package com.mediatek.contacts.aas;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.mediatek.contacts.aas.AlertDialogFragment;
import com.mediatek.contacts.aas.MessageAlertDialogFragment;
import com.mediatek.contacts.aassne.SimAasSneUtils;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.internal.telephony.phb.AlphaTag;

public class AasTagActivity extends Activity {
    private AasTagInfoAdapter mAasAdapter = null;
    private int mSubId = -1;
    private View mActionBarEdit = null;
    private TextView mSelectedView = null;
    private ToastHelper mToastHelper = null;
    private AlphaTag mCurrentEditAlphaTag = null;
    private BroadcastReceiver mPhbStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getAction();
            int intExtra = intent.getIntExtra("subscription", -1);
            boolean booleanExtra = intent.getBooleanExtra("ready", true);
            Log.d("AasTagActivity", "[onReceive] mPhbStateListener subId:" + intExtra + ",phbReady:" + booleanExtra);
            if (intExtra == AasTagActivity.this.mSubId && !booleanExtra) {
                Log.d("AasTagActivity", "[onReceive] subId: " + intExtra);
                AasTagActivity.this.finish();
            }
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        Log.d("AasTagActivity", "[onSaveInstanceState] mode() = " + this.mAasAdapter.getMode());
        bundle.putInt("adapter_mode", this.mAasAdapter.getMode());
        bundle.putIntArray("adapter_checked_array", this.mAasAdapter.getCheckedIndexArray());
        bundle.putParcelable("current_edit_alphaTag", this.mCurrentEditAlphaTag);
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d("AasTagActivity", "[onCreate]");
        registerReceiver(this.mPhbStateListener, new IntentFilter("mediatek.intent.action.PHB_STATE_CHANGED"));
        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            Log.d("AasTagActivity", "[onCreate] return directly due to no permissions !");
            return;
        }
        PhbInfoUtils.getActiveUsimPhbInfoMap();
        setContentView(R.layout.custom_aas);
        Intent intent = getIntent();
        if (intent != null) {
            this.mSubId = intent.getIntExtra("subId", -1);
        }
        if (this.mSubId == -1) {
            Log.e("AasTagActivity", "[onCreate] Eorror slotId=-1, finish the AasTagActivity");
            finish();
        }
        ListView listView = (ListView) findViewById(R.id.custom_aas);
        this.mAasAdapter = new AasTagInfoAdapter(this, this.mSubId);
        this.mAasAdapter.updateAlphaTags();
        listView.setAdapter((ListAdapter) this.mAasAdapter);
        if (bundle != null) {
            this.mAasAdapter.setMode(bundle.getInt("adapter_mode"));
            this.mAasAdapter.setCheckedByIndexArray(bundle.getIntArray("adapter_checked_array"));
            this.mCurrentEditAlphaTag = bundle.getParcelable("current_edit_alphaTag");
        }
        this.mToastHelper = new ToastHelper(this);
        listView.setOnItemClickListener(new ListItemClickListener());
        initActionBar();
    }

    public void initActionBar() {
        ActionBar actionBar = getActionBar();
        View viewInflate = getLayoutInflater().inflate(R.layout.custom_aas_action_bar, (ViewGroup) null);
        actionBar.setDisplayOptions(16, 28);
        this.mActionBarEdit = viewInflate.findViewById(R.id.action_bar_edit);
        this.mSelectedView = (TextView) viewInflate.findViewById(R.id.selected);
        ((ImageView) viewInflate.findViewById(R.id.selected_icon)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AasTagActivity.this.setMode(0);
                AasTagActivity.this.updateActionBar();
            }
        });
        actionBar.setCustomView(viewInflate);
        updateActionBar();
    }

    public void updateActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if (this.mAasAdapter.isMode(0)) {
                actionBar.setDisplayOptions(4, 12);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle(R.string.aas_custom_title);
                this.mActionBarEdit.setVisibility(8);
                return;
            }
            actionBar.setDisplayOptions(16, 16);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            this.mActionBarEdit.setVisibility(0);
            this.mSelectedView.setText(getResources().getString(R.string.selected_item_count, Integer.valueOf(this.mAasAdapter.getCheckedItemCount())));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d("AasTagActivity", "[onPrepareOptionsMenu]");
        MenuInflater menuInflater = getMenuInflater();
        menu.clear();
        if (this.mAasAdapter.isMode(0)) {
            menuInflater.inflate(R.menu.custom_normal_menu, menu);
            return true;
        }
        menuInflater.inflate(R.menu.custom_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        super.onOptionsItemSelected(menuItem);
        Log.d("AasTagActivity", "[onOptionsItemSelected]");
        if (this.mAasAdapter.isMode(0)) {
            int itemId = menuItem.getItemId();
            if (itemId == 16908332) {
                finish();
            } else if (itemId != R.id.menu_add_new) {
                if (itemId == R.id.menu_deletion) {
                    setMode(1);
                }
            } else if (!this.mAasAdapter.isFull()) {
                showNewAasDialog();
            } else {
                this.mToastHelper.showToast(R.string.aas_usim_full);
            }
        } else {
            int itemId2 = menuItem.getItemId();
            if (itemId2 != R.id.menu_delete) {
                if (itemId2 == R.id.menu_disselect_all) {
                    this.mAasAdapter.setAllChecked(false);
                    updateActionBar();
                } else if (itemId2 == R.id.menu_select_all) {
                    this.mAasAdapter.setAllChecked(true);
                    updateActionBar();
                }
            } else if (this.mAasAdapter.getCheckedItemCount() == 0) {
                this.mToastHelper.showToast(R.string.multichoice_no_select_alert);
            } else {
                showDeleteAlertDialog();
            }
        }
        return true;
    }

    public void setMode(int i) {
        this.mAasAdapter.setMode(i);
        updateActionBar();
        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        if (this.mAasAdapter.isMode(1)) {
            setMode(0);
        } else {
            super.onBackPressed();
        }
    }

    public AlertDialogFragment.EditTextDialogFragment.EditTextDoneListener getEditTextDoneListener(String str) {
        if (str.equals("create_aas_dialog")) {
            return new NewAlpahTagListener();
        }
        if (str.equals("edit_aas_dialog")) {
            return new EditAlpahTagListener(this.mCurrentEditAlphaTag);
        }
        throw new IllegalArgumentException("unknown dialogTag!");
    }

    protected void showNewAasDialog() {
        AlertDialogFragment.EditTextDialogFragment.newInstance(R.string.aas_new_dialog_title, android.R.string.cancel, android.R.string.ok, "").show(getFragmentManager(), "create_aas_dialog");
    }

    private final class NewAlpahTagListener implements AlertDialogFragment.EditTextDialogFragment.EditTextDoneListener {
        private NewAlpahTagListener() {
        }

        @Override
        public void onEditTextDone(String str) {
            Log.d("NewAlpahTagListener", "[onEditTextDone] text = " + Log.anonymize(str));
            if (AasTagActivity.this.mAasAdapter.isExist(str).booleanValue()) {
                AasTagActivity.this.mToastHelper.showToast(R.string.aas_name_exist);
                return;
            }
            if (!SimAasSneUtils.isAasTextValid(str, AasTagActivity.this.mSubId)) {
                AasTagActivity.this.mToastHelper.showToast(R.string.aas_name_invalid);
                return;
            }
            int iInsertUSIMAAS = SimAasSneUtils.insertUSIMAAS(AasTagActivity.this.mSubId, str);
            Log.d("NewAlpahTagListener", "[onEditTextDone] aasIndex = " + iInsertUSIMAAS);
            if (iInsertUSIMAAS > 0) {
                AasTagActivity.this.mAasAdapter.updateAlphaTags();
            } else {
                AasTagActivity.this.mToastHelper.showToast(R.string.aas_new_fail);
            }
        }
    }

    protected void showEditAasDialog(AlphaTag alphaTag) {
        this.mCurrentEditAlphaTag = alphaTag;
        if (alphaTag == null) {
            Log.e("AasTagActivity", "[showEditAasDialog] alphaTag is null,");
        } else {
            AlertDialogFragment.EditTextDialogFragment.newInstance(R.string.ass_rename_dialog_title, android.R.string.cancel, android.R.string.ok, alphaTag.getAlphaTag()).show(getFragmentManager(), "edit_aas_dialog");
        }
    }

    private final class EditAlpahTagListener implements AlertDialogFragment.EditTextDialogFragment.EditTextDoneListener {
        private AlphaTag mAlphaTag;

        public EditAlpahTagListener(AlphaTag alphaTag) {
            this.mAlphaTag = alphaTag;
        }

        @Override
        public void onEditTextDone(String str) {
            Log.e("EditAlpahTagListener", "[onEditTextDone] text=" + Log.anonymize(str) + ", mAlphaTag.getAlphaTag()=" + Log.anonymize(this.mAlphaTag.getAlphaTag()));
            if (!this.mAlphaTag.getAlphaTag().equals(str)) {
                if (AasTagActivity.this.mAasAdapter.isExist(str).booleanValue()) {
                    AasTagActivity.this.mToastHelper.showToast(R.string.aas_name_exist);
                } else if (!SimAasSneUtils.isAasTextValid(str, AasTagActivity.this.mSubId)) {
                    AasTagActivity.this.mToastHelper.showToast(R.string.aas_name_invalid);
                } else {
                    AasTagActivity.this.showEditAssertDialog(this.mAlphaTag, str);
                }
            }
        }
    }

    public MessageAlertDialogFragment.AlertConfirmedListener getAlertConfirmedListener(String str) {
        Log.d("AasTagActivity", "[getAlertConfirmedListener] dialogTag = " + str);
        if (str.equals("edit_confirm_dialog")) {
            return new EditAssertListener(this.mCurrentEditAlphaTag);
        }
        if (str.equals("delete_confirm_dialog")) {
            return new DeletionListener();
        }
        throw new IllegalArgumentException("unknown dialogTag!");
    }

    private void showEditAssertDialog(AlphaTag alphaTag, String str) {
        MessageAlertDialogFragment.newInstance(android.R.string.dialog_alert_title, R.string.ass_edit_assert_message, true, str).show(getFragmentManager(), "edit_confirm_dialog");
    }

    private final class EditAssertListener implements MessageAlertDialogFragment.AlertConfirmedListener {
        private AlphaTag mAlphaTag;

        public EditAssertListener(AlphaTag alphaTag) {
            this.mAlphaTag = null;
            this.mAlphaTag = alphaTag;
        }

        @Override
        public void onMessageAlertConfirmed(String str) {
            Log.d("EditAssertListener", "[onMessageAlertConfirmed] text = " + Log.anonymize(str));
            if (SimAasSneUtils.updateUSIMAAS(AasTagActivity.this.mSubId, this.mAlphaTag.getRecordIndex(), this.mAlphaTag.getPbrIndex(), str)) {
                AasTagActivity.this.mAasAdapter.updateAlphaTags();
            } else {
                AasTagActivity.this.mToastHelper.showToast(AasTagActivity.this.getResources().getString(R.string.aas_edit_fail, this.mAlphaTag.getAlphaTag()));
            }
        }
    }

    protected void showDeleteAlertDialog() {
        MessageAlertDialogFragment.newInstance(android.R.string.dialog_alert_title, R.string.aas_delele_dialog_message, true, "").show(getFragmentManager(), "delete_confirm_dialog");
    }

    private final class DeletionListener implements MessageAlertDialogFragment.AlertConfirmedListener {
        private DeletionListener() {
        }

        @Override
        public void onMessageAlertConfirmed(String str) {
            Log.d("DeletionListener", "[onMessageAlertConfirmed]");
            AasTagActivity.this.mAasAdapter.deleteCheckedAasTag();
            AasTagActivity.this.setMode(0);
        }
    }

    public class ListItemClickListener implements AdapterView.OnItemClickListener {
        public ListItemClickListener() {
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            if (AasTagActivity.this.mAasAdapter.isMode(0)) {
                AasTagActivity.this.showEditAasDialog(AasTagActivity.this.mAasAdapter.getItem(i).mAlphaTag);
                return;
            }
            AasTagActivity.this.mAasAdapter.updateChecked(i);
            AasTagActivity.this.invalidateOptionsMenu();
            AasTagActivity.this.updateActionBar();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mPhbStateListener);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }
}
