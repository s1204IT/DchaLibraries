package com.android.contacts.activities;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import com.android.contacts.AppCompatContactsActivity;
import com.android.contacts.ContactSaveService;
import com.android.contacts.DynamicShortcuts;
import com.android.contacts.R;
import com.android.contacts.detail.PhotoSelectionHandler;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.editor.EditorIntents;
import com.android.contacts.editor.PhotoSourceDialogFragment;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.mediatek.contacts.ContactsApplicationEx;
import com.mediatek.contacts.activities.ActivitiesUtils;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simservice.SimEditProcessor;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ContactEditorActivity extends AppCompatContactsActivity implements PhotoSourceDialogFragment.Listener, DialogManager.DialogShowingViewActivity, SimEditProcessor.Listener {
    private int mActionBarTitleResId;
    private boolean mFinishActivityOnSaveCompleted;
    private ContactEditor mFragment;
    private int mPhotoMode;
    private EditorPhotoSelectionHandler mPhotoSelectionHandler;
    private Uri mPhotoUri;
    private Toolbar mToolbar;
    private DialogManager mDialogManager = new DialogManager(this);
    private final ContactEditorFragment.Listener mFragmentListener = new ContactEditorFragment.Listener() {
        @Override
        public void onDeleteRequested(Uri uri) {
            Log.d("ContactEditorActivity", "[onDeleteRequested]uri = " + uri);
            ContactDeletionInteraction.start(ContactEditorActivity.this, uri, true);
        }

        @Override
        public void onReverted() {
            Log.d("ContactEditorActivity", "[onReverted]finish.");
            ContactEditorActivity.this.finish();
        }

        @Override
        public void onSaveFinished(Intent intent) {
            if (ContactEditorActivity.this.mFinishActivityOnSaveCompleted) {
                ContactEditorActivity.this.setResult(intent == null ? 0 : -1, intent);
            } else if (intent != null) {
                ImplicitIntentsUtil.startActivityInApp(ContactEditorActivity.this, intent);
            }
            ContactEditorActivity.this.finish();
        }

        @Override
        public void onContactSplit(Uri uri) {
            ContactEditorActivity.this.setResult(2, null);
            ContactEditorActivity.this.finish();
        }

        @Override
        public void onContactNotFound() {
            Log.d("ContactEditorActivity", "[onContactNotFound]finish.");
            ContactEditorActivity.this.finish();
        }

        @Override
        public void onEditOtherRawContactRequested(Uri uri, long j, ArrayList<ContentValues> arrayList) {
            Intent intentCreateEditOtherRawContactIntent = EditorIntents.createEditOtherRawContactIntent(ContactEditorActivity.this, uri, j, arrayList);
            Log.d("ContactEditorActivity", "[onEditOtherRawContactRequested]intent = " + intentCreateEditOtherRawContactIntent);
            ImplicitIntentsUtil.startActivityInApp(ContactEditorActivity.this, intentCreateEditOtherRawContactIntent);
            ContactEditorActivity.this.finish();
        }
    };
    private Handler mHandler = null;

    public interface ContactEditor {
        void load(String str, Uri uri, Bundle bundle);

        void onJoinCompleted(Uri uri);

        void onSaveCompleted(boolean z, int i, boolean z2, Uri uri, Long l);

        void onSaveSIMContactCompleted(boolean z, Intent intent);

        boolean revert();

        void setIntentExtras(Bundle bundle);

        void setListener(ContactEditorFragment.Listener listener);
    }

    private final class EditorPhotoSelectionHandler extends PhotoSelectionHandler {
        private final EditorPhotoActionListener mPhotoActionListener;

        private final class EditorPhotoActionListener extends PhotoSelectionHandler.PhotoActionListener {
            private EditorPhotoActionListener() {
                super();
            }

            @Override
            public void onRemovePictureChosen() {
                ContactEditorActivity.this.getEditorFragment().removePhoto();
            }

            @Override
            public void onPhotoSelected(Uri uri) throws FileNotFoundException {
                ContactEditorActivity.this.mPhotoUri = uri;
                ContactEditorActivity.this.getEditorFragment().updatePhoto(uri);
                ContactEditorActivity.this.mPhotoSelectionHandler = null;
            }

            @Override
            public Uri getCurrentPhotoUri() {
                return ContactEditorActivity.this.mPhotoUri;
            }

            @Override
            public void onPhotoSelectionDismissed() {
            }
        }

        public EditorPhotoSelectionHandler(int i) {
            super(ContactEditorActivity.this, null, i, false, new RawContactDeltaList());
            this.mPhotoActionListener = new EditorPhotoActionListener();
        }

        @Override
        public PhotoSelectionHandler.PhotoActionListener getListener() {
            return this.mPhotoActionListener;
        }

        @Override
        protected void startPhotoActivity(Intent intent, int i, Uri uri) {
            ContactEditorActivity.this.mPhotoUri = uri;
            ContactEditorActivity.this.startActivityForResult(intent, i);
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.w("ContactEditorActivity", "[onCreate]");
        RequestPermissionsActivity.startPermissionActivityIfNeeded(this);
        Intent intent = getIntent();
        String action = intent.getAction();
        intent.setComponent(new ComponentName(this, (Class<?>) ContactEditorActivity.class));
        Log.i("ContactEditorActivity", "[onCreate] SIMEditProcessor.registerListener,action = " + action);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            this.mHandler = ActivitiesUtils.initHandler(this);
        }
        this.mFinishActivityOnSaveCompleted = intent.getBooleanExtra("finishActivityOnSaveCompleted", false);
        if ("joinCompleted".equals(action)) {
            finish();
            return;
        }
        if ("saveCompleted".equals(action)) {
            finish();
            return;
        }
        if (ContactsApplicationEx.isContactsApplicationBusy()) {
            Toast.makeText(this, R.string.phone_book_busy, 0).show();
            finish();
            return;
        }
        setContentView(R.layout.contact_editor_activity);
        this.mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(this.mToolbar);
        if ("android.intent.action.EDIT".equals(action)) {
            this.mActionBarTitleResId = R.string.contact_editor_title_existing_contact;
        } else {
            this.mActionBarTitleResId = R.string.contact_editor_title_new_contact;
        }
        this.mToolbar.setTitle(this.mActionBarTitleResId);
        setTitle(this.mActionBarTitleResId);
        if (bundle == null) {
            this.mFragment = new ContactEditorFragment();
            getFragmentManager().beginTransaction().add(R.id.fragment_container, getEditorFragment(), "editor_fragment").commit();
        } else {
            this.mPhotoMode = bundle.getInt("photo_mode");
            this.mActionBarTitleResId = bundle.getInt("action_bar_title");
            this.mPhotoUri = Uri.parse(bundle.getString("photo_uri"));
            this.mFragment = (ContactEditorFragment) getFragmentManager().findFragmentByTag("editor_fragment");
            getFragmentManager().beginTransaction().show(getEditorFragment()).commit();
            this.mToolbar.setTitle(this.mActionBarTitleResId);
        }
        this.mFragment.setListener(this.mFragmentListener);
        this.mFragment.load(action, "android.intent.action.EDIT".equals(action) ? getIntent().getData() : null, getIntent().getExtras());
        if ("android.intent.action.INSERT".equals(action)) {
            DynamicShortcuts.reportShortcutUsed(this, DynamicShortcuts.SHORTCUT_ADD_CONTACT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w("ContactEditorActivity", "[onPause]");
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
        View currentFocus = getCurrentFocus();
        if (inputMethodManager != null && currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        Log.w("ContactEditorActivity", "[onDestroy]");
        SimEditProcessor.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (this.mFragment == null) {
            Log.w("ContactEditorActivity", "[onNewIntent]fragment is null,return!");
            return;
        }
        String action = intent.getAction();
        Log.w("ContactEditorActivity", "[onNewIntent]action = " + action);
        if ("android.intent.action.EDIT".equals(action)) {
            this.mFragment.setIntentExtras(intent.getExtras());
            return;
        }
        if ("saveCompleted".equals(action)) {
            this.mFragment.onSaveCompleted(true, intent.getIntExtra(ContactSaveService.EXTRA_SAVE_MODE, 0), intent.getBooleanExtra(ContactSaveService.EXTRA_SAVE_SUCCEEDED, false), intent.getData(), Long.valueOf(intent.getLongExtra("joinContactId", -1L)));
        } else if ("joinCompleted".equals(action)) {
            this.mFragment.onJoinCompleted(intent.getData());
        } else if ("com.mediatek.contacts.simservice.EDIT_SIM".equals(action)) {
            this.mFragment.onSaveSIMContactCompleted(true, intent);
        }
    }

    @Override
    protected Dialog onCreateDialog(int i, Bundle bundle) {
        if (DialogManager.isManagedId(i)) {
            return this.mDialogManager.onCreateDialog(i, bundle);
        }
        Log.w("ContactEditorActivity", "Unknown dialog requested, id: " + i + ", args: " + bundle);
        return null;
    }

    @Override
    public DialogManager getDialogManager() {
        return this.mDialogManager;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("photo_mode", this.mPhotoMode);
        bundle.putInt("action_bar_title", this.mActionBarTitleResId);
        bundle.putString("photo_uri", (this.mPhotoUri != null ? this.mPhotoUri : Uri.EMPTY).toString());
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (this.mPhotoSelectionHandler == null) {
            this.mPhotoSelectionHandler = (EditorPhotoSelectionHandler) getPhotoSelectionHandler();
        }
        if (this.mPhotoSelectionHandler.handlePhotoActivityResult(i, i2, intent)) {
            return;
        }
        super.onActivityResult(i, i2, intent);
    }

    @Override
    public void onBackPressed() {
        if (this.mFragment != null) {
            this.mFragment.revert();
        }
    }

    public void changePhoto(int i) {
        this.mPhotoMode = i;
        if (isSafeToCommitTransactions()) {
            PhotoSourceDialogFragment.show(this, this.mPhotoMode);
        }
    }

    public Toolbar getToolbar() {
        return this.mToolbar;
    }

    @Override
    public void onRemovePictureChosen() {
        getPhotoSelectionHandler().getListener().onRemovePictureChosen();
    }

    @Override
    public void onTakePhotoChosen() {
        getPhotoSelectionHandler().getListener().onTakePhotoChosen();
    }

    @Override
    public void onPickFromGalleryChosen() {
        getPhotoSelectionHandler().getListener().onPickFromGalleryChosen();
    }

    private PhotoSelectionHandler getPhotoSelectionHandler() {
        if (this.mPhotoSelectionHandler == null) {
            this.mPhotoSelectionHandler = new EditorPhotoSelectionHandler(this.mPhotoMode);
        }
        return this.mPhotoSelectionHandler;
    }

    private ContactEditorFragment getEditorFragment() {
        return (ContactEditorFragment) this.mFragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.w("ContactEditorActivity", "[onResume]");
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            SimCardUtils.ShowSimCardStorageInfoTask.showSimCardStorageInfo(this);
            if (SimEditProcessor.isNeedRegisterHandlerAgain(this.mHandler)) {
                Log.d("ContactEditorActivity", " [onResume] register a handler again! Handler: " + this.mHandler);
                SimEditProcessor.registerListener(this, this.mHandler);
            }
        }
    }

    @Override
    public void onSIMEditCompleted(Intent intent) {
        Log.d("ContactEditorActivity", "[onSIMEditCompleted]callbackIntent = " + intent);
        onNewIntent(intent);
    }
}
