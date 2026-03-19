package com.android.contacts.editor;

import android.accounts.Account;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.Toast;
import com.android.contacts.ContactSaveService;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.activities.ContactEditorAccountsChangedActivity;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.activities.ContactSelectionActivity;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.editor.AggregationSuggestionEngine;
import com.android.contacts.editor.AggregationSuggestionView;
import com.android.contacts.editor.CancelEditDialogFragment;
import com.android.contacts.editor.JoinContactConfirmationDialogFragment;
import com.android.contacts.editor.PhotoEditorView;
import com.android.contacts.editor.RawContactEditorView;
import com.android.contacts.editor.SplitContactConfirmationDialogFragment;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.Contact;
import com.android.contacts.model.ContactLoader;
import com.android.contacts.model.RawContact;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.AccountsLoader;
import com.android.contacts.preference.ContactsPreferences;
import com.android.contacts.quickcontact.InvisibleContactUtil;
import com.android.contacts.util.ContactDisplayUtils;
import com.android.contacts.util.ContactPhotoUtils;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.MaterialColorMapUtils;
import com.android.contacts.util.UiClosables;
import com.android.contactsbind.HelpUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.editor.ContactEditorUtilsEx;
import com.mediatek.contacts.editor.SubscriberAccount;
import com.mediatek.contacts.eventhandler.BaseEventHandlerFragment;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SimProcessorService;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ContactEditorFragment extends BaseEventHandlerFragment implements ContactEditorActivity.ContactEditor, AggregationSuggestionEngine.Listener, AggregationSuggestionView.Listener, CancelEditDialogFragment.Listener, JoinContactConfirmationDialogFragment.Listener, PhotoEditorView.Listener, RawContactEditorView.Listener, SplitContactConfirmationDialogFragment.Listener, AccountsLoader.AccountsListener {
    private static final List<String> VALID_INTENT_ACTIONS = new ArrayList<String>() {
        {
            add("android.intent.action.EDIT");
            add("android.intent.action.INSERT");
            add("saveCompleted");
        }
    };
    protected AccountWithDataSet mAccountWithDataSet;
    protected String mAction;
    private AggregationSuggestionEngine mAggregationSuggestionEngine;
    protected ListPopupWindow mAggregationSuggestionPopup;
    private long mAggregationSuggestionsRawContactId;
    protected boolean mAutoAddToDefaultGroup;
    protected RawContactDeltaComparator mComparator;
    protected Contact mContact;
    protected long mContactIdForJoin;
    protected LinearLayout mContent;
    protected Context mContext;
    protected boolean mCopyReadOnlyName;
    protected boolean mDisableDeleteMenuOption;
    protected ContactEditorUtils mEditorUtils;
    protected boolean mExistingContactDataReady;
    protected Cursor mGroupMetaData;
    protected boolean mHasNewContact;
    protected Bundle mIntentExtras;
    protected boolean mIsEdit;
    protected boolean mIsUserProfile;
    protected Listener mListener;
    protected Uri mLookupUri;
    protected MaterialColorMapUtils.MaterialPalette mMaterialPalette;
    protected boolean mNewContactAccountChanged;
    protected boolean mNewContactDataReady;
    protected boolean mNewLocalProfile;
    private long mPhotoRawContactId;
    protected ImmutableList<RawContact> mRawContacts;
    protected long mReadOnlyDisplayNameId;
    protected RawContactDeltaList mState;
    protected int mStatus;
    protected ViewIdGenerator mViewIdGenerator;
    protected SubscriberAccount mSubsciberAccount = new SubscriberAccount();
    protected long mRawContactIdToDisplayAlone = -1;
    protected List<AccountInfo> mWritableAccounts = Collections.emptyList();
    private boolean mEnabled = true;
    protected final LoaderManager.LoaderCallbacks<Contact> mContactLoaderListener = new LoaderManager.LoaderCallbacks<Contact>() {
        protected long mLoaderStartTime;

        @Override
        public Loader<Contact> onCreateLoader(int i, Bundle bundle) {
            Log.d("ContactEditor", "[onCreateLoader]mLookupUri = " + ContactEditorFragment.this.mLookupUri + ",id = " + i);
            this.mLoaderStartTime = SystemClock.elapsedRealtime();
            return new ContactLoader(ContactEditorFragment.this.mContext, ContactEditorFragment.this.mLookupUri, true, true);
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact contact) {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (Log.isLoggable("ContactEditor", 2)) {
                Log.v("ContactEditor", "Time needed for loading: " + (jElapsedRealtime - this.mLoaderStartTime));
            }
            if (!contact.isLoaded()) {
                Log.i("ContactEditor", "No contact found. Closing activity");
                ContactEditorFragment.this.mStatus = 3;
                if (ContactEditorFragment.this.mListener != null) {
                    ContactEditorFragment.this.mListener.onContactNotFound();
                    return;
                }
                return;
            }
            Log.i("ContactEditor", "[onLoadFinished]change state is  Status.EDITING");
            ContactEditorFragment.this.mStatus = 1;
            ContactEditorFragment.this.mLookupUri = contact.getLookupUri();
            ContactEditorFragment.this.mSubsciberAccount.setIndicatePhoneOrSimContact(contact.getIndicate());
            ContactEditorFragment.this.mSubsciberAccount.setSimIndex(contact.getSimIndex());
            long jElapsedRealtime2 = SystemClock.elapsedRealtime();
            ContactEditorFragment.this.setState(contact);
            long jElapsedRealtime3 = SystemClock.elapsedRealtime();
            if (Log.isLoggable("ContactEditor", 2)) {
                Log.v("ContactEditor", "Time needed for setting UI: " + (jElapsedRealtime3 - jElapsedRealtime2));
            }
        }

        @Override
        public void onLoaderReset(Loader<Contact> loader) {
        }
    };
    protected final LoaderManager.LoaderCallbacks<Cursor> mGroupsLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader2(int i, Bundle bundle) {
            return new GroupMetaDataLoader(ContactEditorFragment.this.mContext, ContactsContract.Groups.CONTENT_URI, GroupUtil.ALL_GROUPS_SELECTION);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            ContactEditorFragment.this.mGroupMetaData = cursor;
            ContactEditorFragment.this.setGroupMetaData();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };
    private Bundle mUpdatedPhotos = new Bundle();

    public interface Listener {
        void onContactNotFound();

        void onContactSplit(Uri uri);

        void onDeleteRequested(Uri uri);

        void onEditOtherRawContactRequested(Uri uri, long j, ArrayList<ContentValues> arrayList);

        void onReverted();

        void onSaveFinished(Intent intent);
    }

    private static final class AggregationSuggestionAdapter extends BaseAdapter {
        private final LayoutInflater mLayoutInflater;
        private final AggregationSuggestionView.Listener mListener;
        private final List<AggregationSuggestionEngine.Suggestion> mSuggestions;

        public AggregationSuggestionAdapter(Activity activity, AggregationSuggestionView.Listener listener, List<AggregationSuggestionEngine.Suggestion> list) {
            this.mLayoutInflater = activity.getLayoutInflater();
            this.mListener = listener;
            this.mSuggestions = list;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            AggregationSuggestionEngine.Suggestion suggestion = (AggregationSuggestionEngine.Suggestion) getItem(i);
            AggregationSuggestionView aggregationSuggestionView = (AggregationSuggestionView) this.mLayoutInflater.inflate(R.layout.aggregation_suggestions_item, (ViewGroup) null);
            aggregationSuggestionView.setListener(this.mListener);
            aggregationSuggestionView.bindSuggestion(suggestion);
            return aggregationSuggestionView;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public Object getItem(int i) {
            return this.mSuggestions.get(i);
        }

        @Override
        public int getCount() {
            return this.mSuggestions.size();
        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.i("ContactEditor", "[onAttach]");
        this.mContext = activity;
        this.mEditorUtils = ContactEditorUtils.create(this.mContext);
        this.mComparator = new RawContactDeltaComparator(this.mContext);
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().setEditorFragment(this, getFragmentManager());
    }

    @Override
    public void onCreate(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("[onCreate] (savedState == null) = ");
        sb.append(bundle == null);
        Log.i("ContactEditor", sb.toString());
        if (bundle != null) {
            this.mAction = bundle.getString("action");
            this.mLookupUri = (Uri) bundle.getParcelable("uri");
        }
        super.onCreate(bundle);
        if (bundle == null) {
            this.mViewIdGenerator = new ViewIdGenerator();
            this.mState = new RawContactDeltaList();
            return;
        }
        this.mViewIdGenerator = (ViewIdGenerator) bundle.getParcelable("viewidgenerator");
        this.mAutoAddToDefaultGroup = bundle.getBoolean("autoAddToDefaultGroup");
        this.mDisableDeleteMenuOption = bundle.getBoolean("disableDeleteMenuOption");
        this.mNewLocalProfile = bundle.getBoolean("newLocalProfile");
        this.mMaterialPalette = (MaterialColorMapUtils.MaterialPalette) bundle.getParcelable("materialPalette");
        this.mAccountWithDataSet = (AccountWithDataSet) bundle.getParcelable("saveToAccount");
        this.mRawContacts = ImmutableList.copyOf((Collection) bundle.getParcelableArrayList("rawContacts"));
        this.mState = (RawContactDeltaList) bundle.getParcelable(ContactSaveService.EXTRA_CONTACT_STATE);
        this.mStatus = bundle.getInt("status");
        this.mHasNewContact = bundle.getBoolean("hasNewContact");
        this.mNewContactDataReady = bundle.getBoolean("newContactDataReady");
        this.mIsEdit = bundle.getBoolean("isEdit");
        this.mExistingContactDataReady = bundle.getBoolean("existingContactDataReady");
        this.mIsUserProfile = bundle.getBoolean("isUserProfile");
        this.mEnabled = bundle.getBoolean("enabled");
        this.mAggregationSuggestionsRawContactId = bundle.getLong("aggregationSuggestionsRawContactId");
        this.mContactIdForJoin = bundle.getLong("contactidforjoin");
        this.mReadOnlyDisplayNameId = bundle.getLong("readOnlyDisplayNameId");
        this.mCopyReadOnlyName = bundle.getBoolean("copyReadOnlyDisplayName", false);
        this.mPhotoRawContactId = bundle.getLong("photo_raw_contact_id");
        this.mUpdatedPhotos = (Bundle) bundle.getParcelable("updated_photos");
        this.mSubsciberAccount.restoreSimAndSubId(bundle);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Log.i("ContactEditor", "[onCreateView] get mContent(RawContactEditorView).");
        setHasOptionsMenu(true);
        View viewInflate = layoutInflater.inflate(R.layout.contact_editor_fragment, viewGroup, false);
        this.mContent = (LinearLayout) viewInflate.findViewById(R.id.raw_contacts_editor_view);
        return viewInflate;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        Account account;
        super.onActivityCreated(bundle);
        Log.i("ContactEditor", "[onActivityCreated]mAction = " + this.mAction + ", mState.isEmpty() = " + this.mState.isEmpty());
        validateAction(this.mAction);
        if (this.mState.isEmpty()) {
            if ("android.intent.action.EDIT".equals(this.mAction)) {
                Log.d("ContactEditor", "[onActivityCreated]initLoader data.");
                getLoaderManager().initLoader(1, null, this.mContactLoaderListener);
                getLoaderManager().initLoader(2, null, this.mGroupsLoaderListener);
            }
        } else if (this.mSubsciberAccount.isAccountTypeIccCard(this.mState) && "android.intent.action.EDIT".equals(this.mAction)) {
            Log.d("ContactEditor", "[onActivityCreated] sim contact in EDIT action, initLoader data.");
            getLoaderManager().initLoader(1, null, this.mContactLoaderListener);
            getLoaderManager().initLoader(2, null, this.mGroupsLoaderListener);
        } else {
            bindEditors();
        }
        if (bundle == null) {
            if (this.mIntentExtras != null) {
                if (this.mIntentExtras != null) {
                    account = (Account) this.mIntentExtras.getParcelable("android.provider.extra.ACCOUNT");
                } else {
                    account = null;
                }
                this.mAccountWithDataSet = ContactEditorUtilsEx.getAccountWithDataSet(this.mContext, account, this.mIntentExtras != null ? this.mIntentExtras.getString("android.provider.extra.DATA_SET") : null, this.mIntentExtras);
                this.mSubsciberAccount.setAndCheckSimInfo(this.mContext, this.mAccountWithDataSet);
            }
            Log.d("ContactEditor", "[onActivityCreated] mAccountWithDataSet = " + this.mAccountWithDataSet);
            if ("android.intent.action.EDIT".equals(this.mAction)) {
                this.mIsEdit = true;
            } else if ("android.intent.action.INSERT".equals(this.mAction)) {
                this.mHasNewContact = true;
                if (this.mAccountWithDataSet != null) {
                    createContact(this.mAccountWithDataSet);
                }
            }
        }
        if (this.mHasNewContact) {
            Log.d("ContactEditor", "[onActivityCreated] wait for accounts to be loaded");
            AccountsLoader.loadAccounts(this, 3, AccountTypeManager.writableFilter());
        }
    }

    private static void validateAction(String str) {
        if (VALID_INTENT_ACTIONS.contains(str)) {
            return;
        }
        throw new IllegalArgumentException("Unknown action " + str + "; Supported actions: " + VALID_INTENT_ACTIONS);
    }

    @Override
    public void onStart() {
        super.onStart();
        ContactEditorUtilsEx.updateAasView(this.mContext, this.mState, this.mContent);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        Log.d("ContactEditor", "[onSaveInstanceState]");
        bundle.putString("action", this.mAction);
        bundle.putParcelable("uri", this.mLookupUri);
        bundle.putBoolean("autoAddToDefaultGroup", this.mAutoAddToDefaultGroup);
        bundle.putBoolean("disableDeleteMenuOption", this.mDisableDeleteMenuOption);
        bundle.putBoolean("newLocalProfile", this.mNewLocalProfile);
        if (this.mMaterialPalette != null) {
            bundle.putParcelable("materialPalette", this.mMaterialPalette);
        }
        bundle.putParcelable("viewidgenerator", this.mViewIdGenerator);
        bundle.putParcelableArrayList("rawContacts", this.mRawContacts == null ? Lists.newArrayList() : Lists.newArrayList(this.mRawContacts));
        bundle.putParcelable(ContactSaveService.EXTRA_CONTACT_STATE, this.mState);
        bundle.putInt("status", this.mStatus);
        bundle.putBoolean("hasNewContact", this.mHasNewContact);
        bundle.putBoolean("newContactDataReady", this.mNewContactDataReady);
        bundle.putBoolean("isEdit", this.mIsEdit);
        bundle.putBoolean("existingContactDataReady", this.mExistingContactDataReady);
        bundle.putParcelable("saveToAccount", this.mAccountWithDataSet);
        bundle.putBoolean("isUserProfile", this.mIsUserProfile);
        bundle.putBoolean("enabled", this.mEnabled);
        bundle.putLong("aggregationSuggestionsRawContactId", this.mAggregationSuggestionsRawContactId);
        bundle.putLong("contactidforjoin", this.mContactIdForJoin);
        bundle.putLong("readOnlyDisplayNameId", this.mReadOnlyDisplayNameId);
        bundle.putBoolean("copyReadOnlyDisplayName", this.mCopyReadOnlyName);
        bundle.putLong("photo_raw_contact_id", this.mPhotoRawContactId);
        bundle.putParcelable("updated_photos", this.mUpdatedPhotos);
        this.mSubsciberAccount.onSaveInstanceStateSim(bundle);
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("ContactEditor", "[onStop]");
        UiClosables.closeQuietly(this.mAggregationSuggestionPopup);
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().closeTextChangedListener(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("ContactEditor", "[onDestroy]");
        if (this.mAggregationSuggestionEngine != null) {
            this.mAggregationSuggestionEngine.quit();
        }
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().closeTextChangedListener(true);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        Log.i("ContactEditor", "[onActivityResult]requestCode = " + i + ",resultCode = " + i2);
        switch (i) {
            case 0:
                if (i2 == -1 && intent != null) {
                    long id = ContentUris.parseId(intent.getData());
                    if (hasPendingChanges()) {
                        JoinContactConfirmationDialogFragment.show(this, id);
                    } else {
                        joinAggregate(id);
                    }
                }
                break;
            case 1:
                if (i2 != -1 || intent == null || !intent.hasExtra("android.provider.extra.ACCOUNT")) {
                    if (this.mListener != null) {
                        this.mListener.onReverted();
                    }
                } else {
                    this.mSubsciberAccount.setAccountChangedSim(intent, this.mContext);
                    if (!this.mState.isEmpty()) {
                        Log.i("ContactEditor", "[onActivityResult]mState.size=" + this.mState.size());
                        this.mState = new RawContactDeltaList();
                    }
                    createContact((AccountWithDataSet) intent.getParcelableExtra("android.provider.extra.ACCOUNT"));
                }
                break;
        }
    }

    @Override
    public void onAccountsLoaded(List<AccountInfo> list) {
        AccountWithDataSet accountWithDataSet;
        Log.i("ContactEditor", "[onAccountsLoaded], mAccountWithDataSet" + this.mAccountWithDataSet);
        this.mWritableAccounts = list;
        if (this.mAccountWithDataSet == null && this.mHasNewContact) {
            selectAccountAndCreateContact();
        }
        if (isEditingUserProfile()) {
            Log.d("ContactEditor", "[onAccountsLoaded] current is user profile, just return");
            return;
        }
        RawContactEditorView content = getContent();
        if (content == null) {
            return;
        }
        content.setAccounts(list);
        if (this.mAccountWithDataSet == null && content.getCurrentRawContactDelta() == null) {
            return;
        }
        if (this.mAccountWithDataSet != null) {
            accountWithDataSet = this.mAccountWithDataSet;
        } else {
            accountWithDataSet = content.getCurrentRawContactDelta().getAccountWithDataSet();
        }
        if (!AccountInfo.contains(list, accountWithDataSet) && !list.isEmpty()) {
            if (isReadyToBindEditors()) {
                onRebindEditorsForNewContact(getContent().getCurrentRawContactDelta(), accountWithDataSet, list.get(0).getAccount());
            } else {
                this.mAccountWithDataSet = list.get(0).getAccount();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.edit_contact, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.d("ContactEditor", "[onPrepareOptionsMenu]");
        final MenuItem menuItemFindItem = menu.findItem(R.id.menu_save);
        MenuItem menuItemFindItem2 = menu.findItem(R.id.menu_split);
        MenuItem menuItemFindItem3 = menu.findItem(R.id.menu_join);
        MenuItem menuItemFindItem4 = menu.findItem(R.id.menu_delete);
        menuItemFindItem3.setVisible(false);
        menuItemFindItem2.setVisible(false);
        menuItemFindItem4.setVisible(false);
        menuItemFindItem.setVisible(!isEditingReadOnlyRawContact());
        if (menuItemFindItem.isVisible()) {
            menuItemFindItem.getActionView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ContactEditorFragment.this.onOptionsItemSelected(menuItemFindItem);
                }
            });
        }
        if (!HelpUtils.isHelpAndFeedbackAvailable()) {
            menu.findItem(R.id.menu_help).setVisible(false);
        }
        int size = menu.size();
        for (int i = 0; i < size; i++) {
            menu.getItem(i).setEnabled(this.mEnabled);
        }
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().addEditorMenuOptions(this, menu, "android.intent.action.INSERT".equals(this.mAction));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            return revert();
        }
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return true;
        }
        int itemId = menuItem.getItemId();
        if (itemId == R.id.menu_save) {
            Log.d("ContactEditor", "[onOptionsItemSelected]save");
            return save(0);
        }
        if (itemId == R.id.menu_delete) {
            if (this.mListener != null) {
                this.mListener.onDeleteRequested(this.mLookupUri);
            }
            return true;
        }
        if (itemId == R.id.menu_split) {
            return doSplitContactAction();
        }
        if (itemId == R.id.menu_join) {
            return doJoinContactAction();
        }
        if (itemId != R.id.menu_help) {
            return false;
        }
        HelpUtils.launchHelpAndFeedbackForContactScreen(getActivity());
        return true;
    }

    @Override
    public boolean revert() {
        Log.d("ContactEditor", "[revert]");
        if (this.mState.isEmpty() || !hasPendingChanges()) {
            if (GlobalEnv.getSimAasEditor().isAasNameChangedOnly(this.mState)) {
                onSaveAasNameCompletedOnly();
                return true;
            }
            onCancelEditConfirmed();
            return true;
        }
        if (getEditorActivity().isSafeToCommitTransactions()) {
            CancelEditDialogFragment.show(this);
            return true;
        }
        return true;
    }

    @Override
    public void onCancelEditConfirmed() {
        Log.d("ContactEditor", "[onCancelEditConfirmed,change status as Status.CLOSING");
        this.mStatus = 3;
        if (this.mListener != null) {
            this.mListener.onReverted();
        }
    }

    @Override
    public void onSplitContactConfirmed(boolean z) {
        if (this.mState.isEmpty()) {
            Log.e("ContactEditor", "mState became null during the user's confirming split action. Cannot perform the save action.");
            return;
        }
        if (!z && this.mHasNewContact) {
            Iterator<RawContactDelta> it = this.mState.iterator();
            while (it.hasNext()) {
                if (it.next().getRawContactId().longValue() < 0) {
                    it.remove();
                }
            }
        }
        Log.d("ContactEditor", "[onSplitContactConfirmed],set SaveMode.SPLIT");
        this.mState.markRawContactsForSplitting();
        save(2);
    }

    @Override
    public void onSplitContactCanceled() {
    }

    private boolean doSplitContactAction() {
        Log.d("ContactEditor", "[doSplitContactAction]");
        if (!hasValidState()) {
            return false;
        }
        SplitContactConfirmationDialogFragment.show(this, hasPendingChanges());
        return true;
    }

    private boolean doJoinContactAction() {
        Log.d("ContactEditor", "[doJoinContactAction]");
        if (!hasValidState() || this.mLookupUri == null) {
            Log.w("ContactEditor", "[doJoinContactAction]hasValidState is false, return.");
            return false;
        }
        if (this.mState.size() == 1 && this.mState.get(0).isContactInsert() && !hasPendingChanges()) {
            Toast.makeText(this.mContext, R.string.toast_join_with_empty_contact, 1).show();
            return true;
        }
        showJoinAggregateActivity(this.mLookupUri);
        return true;
    }

    @Override
    public void onJoinContactConfirmed(long j) {
        doSaveAction(3, Long.valueOf(j));
        Log.d("ContactEditor", "[doJoinContactAction],set SaveMode.JOIN");
    }

    public boolean save(int i) {
        Log.i("ContactEditor", "[save]saveMode = " + i);
        if (!hasValidState() || this.mStatus != 1) {
            Log.w("ContactEditor", "[save]return,mStatus = " + this.mStatus);
            return false;
        }
        if (i == 0 || i == 4) {
            this.mSubsciberAccount.getProgressHandler().showDialog(getFragmentManager());
            Log.d("ContactEditor", "[save]saveMode == CLOSE or EDITOR,show ProgressDialog");
        }
        if (i == 0 || i == 4 || i == 2) {
            getLoaderManager().destroyLoader(1);
        }
        Log.i("ContactEditor", "[save]change status as Status.SAVING");
        this.mStatus = 2;
        if (!hasPendingChanges()) {
            if (this.mLookupUri == null && i == 1) {
                this.mStatus = 1;
                Log.i("ContactEditor", "[save]change mStatus as EDITING");
                return true;
            }
            Log.i("ContactEditor", "[save]onSaveCompleted");
            if (this.mSubsciberAccount.isAccountTypeIccCard(this.mState)) {
                Intent intent = new Intent("com.mediatek.contacts.simservice.EDIT_SIM");
                intent.putExtra("result", 1);
                intent.putExtra(ContactSaveService.EXTRA_SAVE_MODE, i);
                intent.setData(this.mLookupUri);
                onSaveSIMContactCompleted(false, intent);
                return true;
            }
            onSaveCompleted(false, i, this.mLookupUri != null, this.mLookupUri, null);
            return true;
        }
        setEnabled(false);
        Log.i("ContactEditor", "[save]doSaveAction");
        if (this.mSubsciberAccount.isAccountTypeIccCard(this.mState)) {
            return doSaveSIMContactAction(i);
        }
        return doSaveAction(i, null);
    }

    private boolean hasValidState() {
        return this.mState.size() > 0;
    }

    private boolean isEditingUserProfile() {
        return this.mNewLocalProfile || this.mIsUserProfile;
    }

    private boolean isEditingReadOnlyRawContactWithNewContact() {
        return this.mHasNewContact && this.mState.size() > 1;
    }

    private boolean isEditingReadOnlyRawContact() {
        if (this.mState.getByRawContactId(Long.valueOf(this.mRawContactIdToDisplayAlone)) != null) {
            return hasValidState() && this.mRawContactIdToDisplayAlone > 0 && !this.mState.getByRawContactId(Long.valueOf(this.mRawContactIdToDisplayAlone)).getAccountType(AccountTypeManager.getInstance(this.mContext)).areContactsWritable();
        }
        Log.w("ContactEditor", "[isEditingReadOnlyRawContact] no display-alone raw contact!");
        return false;
    }

    private boolean hasPendingRawContactChanges(Set<String> set) {
        return RawContactModifier.hasChanges(this.mState, AccountTypeManager.getInstance(this.mContext), set);
    }

    private boolean hasPendingChanges() {
        if (isEditingReadOnlyRawContactWithNewContact()) {
            RawContactDelta byRawContactId = this.mState.getByRawContactId(Long.valueOf(this.mReadOnlyDisplayNameId));
            if (structuredNamesAreEqual(byRawContactId != null ? byRawContactId.getSuperPrimaryEntry("vnd.android.cursor.item/name") : null, this.mState.getSuperPrimaryEntry("vnd.android.cursor.item/name"))) {
                HashSet hashSet = new HashSet();
                hashSet.add("vnd.android.cursor.item/name");
                return hasPendingRawContactChanges(hashSet);
            }
            return true;
        }
        return hasPendingRawContactChanges(null);
    }

    private boolean structuredNamesAreEqual(ValuesDelta valuesDelta, ValuesDelta valuesDelta2) {
        if (valuesDelta == valuesDelta2) {
            return true;
        }
        if (valuesDelta == null || valuesDelta2 == null) {
            return false;
        }
        ContentValues before = valuesDelta.getBefore();
        ContentValues after = valuesDelta2.getAfter();
        if (before == null || after == null || !TextUtils.equals(before.getAsString("data1"), after.getAsString("data1")) || !TextUtils.equals(before.getAsString("data4"), after.getAsString("data4")) || !TextUtils.equals(before.getAsString("data2"), after.getAsString("data2")) || !TextUtils.equals(before.getAsString("data5"), after.getAsString("data5")) || !TextUtils.equals(before.getAsString("data3"), after.getAsString("data3"))) {
            return false;
        }
        return TextUtils.equals(before.getAsString("data6"), after.getAsString("data6"));
    }

    private void selectAccountAndCreateContact() {
        Log.d("ContactEditor", "[selectAccountAndCreateContact]");
        Preconditions.checkNotNull(this.mWritableAccounts, "Accounts must be loaded first");
        if (isEditingUserProfile()) {
            Log.i("ContactEditor", "[selectAccountAndCreateContact]isEditingUserProfile.");
            createContact(null);
            return;
        }
        List<AccountWithDataSet> listExtractAccounts = AccountInfo.extractAccounts(this.mWritableAccounts);
        if (this.mEditorUtils.shouldShowAccountChangedNotification(listExtractAccounts)) {
            Intent intent = new Intent(this.mContext, (Class<?>) ContactEditorAccountsChangedActivity.class);
            intent.setFlags(603979776);
            Log.d("ContactEditor", "[selectAccountAndCreateContact]change status as Status.SUB_ACTIVITY");
            this.mStatus = 4;
            startActivityForResult(intent, 1);
            return;
        }
        this.mEditorUtils.maybeUpdateDefaultAccount(listExtractAccounts);
        AccountWithDataSet onlyOrDefaultAccountEx = this.mEditorUtils.getOnlyOrDefaultAccountEx(listExtractAccounts);
        Log.d("ContactEditor", "[selectAccountAndCreateContact] accounts=" + listExtractAccounts + " ,defaultAccount=" + onlyOrDefaultAccountEx);
        this.mSubsciberAccount.setAndCheckSimInfo(this.mContext, onlyOrDefaultAccountEx);
        createContact(onlyOrDefaultAccountEx);
    }

    private void createContact(AccountWithDataSet accountWithDataSet) {
        AccountType accountTypeForAccount = AccountTypeManager.getInstance(this.mContext).getAccountTypeForAccount(accountWithDataSet);
        Log.sensitive("ContactEditor", "[createContact] accountType: " + accountTypeForAccount.accountType);
        setStateForNewContact(accountWithDataSet, accountTypeForAccount, isEditingUserProfile());
    }

    private void setState(Contact contact) {
        if (!this.mState.isEmpty()) {
            if (contact.getIndicate() >= 0) {
                this.mState = new RawContactDeltaList();
            } else {
                if (Log.isLoggable("ContactEditor", 2)) {
                    Log.v("ContactEditor", "Ignoring background change. This will have to be rebased later");
                    return;
                }
                return;
            }
        }
        this.mContact = contact;
        this.mRawContacts = contact.getRawContacts();
        ExtensionManager.getInstance();
        this.mRawContacts = ExtensionManager.getRcsExtension().rcsConfigureRawContacts(this.mRawContacts, contact.isUserProfile());
        if (!contact.isUserProfile() && !contact.isWritableContact(this.mContext)) {
            this.mReadOnlyDisplayNameId = contact.getNameRawContactId();
        } else {
            this.mHasNewContact = false;
        }
        this.mSubsciberAccount.initIccCard(contact);
        setStateForExistingContact(contact.isUserProfile(), this.mRawContacts);
        if (this.mAutoAddToDefaultGroup && InvisibleContactUtil.isInvisibleAndAddable(contact, getContext())) {
            InvisibleContactUtil.markAddToDefaultGroup(contact, this.mState, getContext());
        }
    }

    private void setStateForNewContact(AccountWithDataSet accountWithDataSet, AccountType accountType, boolean z) {
        setStateForNewContact(accountWithDataSet, accountType, null, null, z);
    }

    private void setStateForNewContact(AccountWithDataSet accountWithDataSet, AccountType accountType, RawContactDelta rawContactDelta, AccountType accountType2, boolean z) {
        Log.sensitive("ContactEditor", "[setStateForNewContact] account=" + accountWithDataSet + " accountType=+" + accountType + " oldState=" + rawContactDelta + " oldAccountType=" + accountType2 + " isUserProfile=" + z);
        this.mStatus = 1;
        this.mAccountWithDataSet = accountWithDataSet;
        if (!this.mState.isEmpty() && !z) {
            Log.w("ContactEditor", "setStateForNewContact: mState not null!! mState=" + this.mState);
            this.mState = new RawContactDeltaList();
        }
        if (!z || this.mState.isEmpty()) {
            this.mState.add(createNewRawContactDelta(accountWithDataSet, accountType, rawContactDelta, accountType2));
        }
        this.mIsUserProfile = z;
        this.mNewContactDataReady = true;
        bindEditors();
    }

    private RawContactDelta createNewRawContactDelta(AccountWithDataSet accountWithDataSet, AccountType accountType, RawContactDelta rawContactDelta, AccountType accountType2) {
        Log.d("ContactEditor", "[createNewRawContactDelta]");
        this.mSubsciberAccount.setSimSaveMode(1);
        RawContact rawContact = new RawContact();
        if (accountWithDataSet != null) {
            rawContact.setAccount(accountWithDataSet);
        } else {
            rawContact.setAccountToLocal();
        }
        RawContactDelta rawContactDelta2 = new RawContactDelta(ValuesDelta.fromAfter(rawContact.getValues()));
        if (rawContactDelta == null) {
            RawContactModifier.parseExtras(this.mContext, accountType, rawContactDelta2, this.mIntentExtras);
        } else {
            RawContactModifier.migrateStateForNewContact(this.mContext, rawContactDelta, rawContactDelta2, accountType2, accountType);
        }
        ContactEditorUtilsEx.setSimDataKindCountMax(accountType, this.mSubsciberAccount.getSubId());
        RawContactModifier.ensureKindExists(rawContactDelta2, accountType, "vnd.android.cursor.item/name");
        RawContactModifier.ensureKindExists(rawContactDelta2, accountType, "vnd.android.cursor.item/phone_v2");
        RawContactModifier.ensureKindExists(rawContactDelta2, accountType, "vnd.android.cursor.item/email_v2");
        RawContactModifier.ensureKindExists(rawContactDelta2, accountType, "vnd.android.cursor.item/organization");
        RawContactModifier.ensureKindExists(rawContactDelta2, accountType, "vnd.android.cursor.item/contact_event");
        RawContactModifier.ensureKindExists(rawContactDelta2, accountType, "vnd.android.cursor.item/postal-address_v2");
        if (isEditingUserProfile()) {
            rawContactDelta2.setProfileQueryUri();
        }
        return rawContactDelta2;
    }

    private void setStateForExistingContact(boolean z, ImmutableList<RawContact> immutableList) {
        Log.d("ContactEditor", "[setStateForExistingContact] isUserProfile = " + z);
        setEnabled(true);
        this.mSubsciberAccount.insertRawDataToSim(immutableList);
        this.mState.addAll(immutableList.iterator());
        setIntentExtras(this.mIntentExtras);
        this.mIntentExtras = null;
        this.mIsUserProfile = z;
        if (this.mIsUserProfile) {
            boolean z2 = false;
            for (RawContactDelta rawContactDelta : this.mState) {
                rawContactDelta.setProfileQueryUri();
                if (rawContactDelta.getValues().getAsString("account_type") == null) {
                    z2 = true;
                }
            }
            if (!z2 && this.mRawContactIdToDisplayAlone <= 0) {
                this.mState.add(createLocalRawContactDelta());
            }
        }
        this.mExistingContactDataReady = true;
        bindEditors();
    }

    private void setEnabled(boolean z) {
        if (this.mEnabled != z) {
            this.mEnabled = z;
            if (this.mContent != null) {
                int childCount = this.mContent.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    this.mContent.getChildAt(i).setEnabled(z);
                }
            }
            Activity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
    }

    private static RawContactDelta createLocalRawContactDelta() {
        RawContact rawContact = new RawContact();
        rawContact.setAccountToLocal();
        RawContactDelta rawContactDelta = new RawContactDelta(ValuesDelta.fromAfter(rawContact.getValues()));
        rawContactDelta.setProfileQueryUri();
        return rawContactDelta;
    }

    private void copyReadOnlyName() {
        if (!isEditingReadOnlyRawContactWithNewContact()) {
            return;
        }
        RawContactDelta rawContactDelta = this.mState.get(this.mState.indexOfFirstWritableRawContact(getContext()));
        RawContactDelta byRawContactId = this.mState.getByRawContactId(Long.valueOf(this.mContact.getNameRawContactId()));
        ValuesDelta superPrimaryEntry = rawContactDelta.getSuperPrimaryEntry("vnd.android.cursor.item/name");
        ValuesDelta superPrimaryEntry2 = byRawContactId.getSuperPrimaryEntry("vnd.android.cursor.item/name");
        this.mCopyReadOnlyName = false;
        if (superPrimaryEntry == null || superPrimaryEntry2 == null) {
            return;
        }
        superPrimaryEntry.copyStructuredNameFieldsFrom(superPrimaryEntry2);
    }

    protected void bindEditors() {
        Toolbar toolbar;
        Log.i("ContactEditor", "[bindEditors] mAccountWithDataSet: " + this.mAccountWithDataSet);
        if (!isReadyToBindEditors()) {
            Log.w("ContactEditor", "[bindEditors]no ready, return.");
            return;
        }
        RawContactEditorView content = getContent();
        content.setListener(this);
        ContactEditorUtilsEx.ensureDataKindsForSim(this.mState, this.mSubsciberAccount.getSubId(), this.mContext);
        if (this.mAccountWithDataSet != null && AccountTypeUtils.isAccountTypeIccCard(this.mAccountWithDataSet.type)) {
            Log.d("ContactEditor", "[bindEditors] sim account remove the photo cache!");
            this.mUpdatedPhotos.remove(String.valueOf(this.mPhotoRawContactId));
        }
        if (this.mCopyReadOnlyName) {
            copyReadOnlyName();
        }
        content.setState(this.mState, this.mMaterialPalette, this.mViewIdGenerator, this.mHasNewContact, this.mIsUserProfile, this.mAccountWithDataSet, this.mRawContactIdToDisplayAlone);
        if (getActivity() == null || getActivity().isFinishing()) {
            Log.w("ContactEditor", "Return for host activity has been requested to finish");
            return;
        }
        if (isEditingReadOnlyRawContact() && (toolbar = getEditorActivity().getToolbar()) != null) {
            toolbar.setTitle(R.string.contact_editor_title_read_only_contact);
            getEditorActivity().setTitle(R.string.contact_editor_title_read_only_contact);
            toolbar.setNavigationIcon(R.drawable.quantum_ic_arrow_back_vd_theme_24);
            toolbar.setNavigationContentDescription(R.string.back_arrow_content_description);
            toolbar.getNavigationIcon().setAutoMirrored(true);
        }
        content.setPhotoListener(this);
        this.mPhotoRawContactId = content.getPhotoRawContactId();
        Uri uri = (Uri) this.mUpdatedPhotos.get(String.valueOf(this.mPhotoRawContactId));
        if (uri != null) {
            content.setFullSizePhoto(uri);
        }
        StructuredNameEditorView nameEditorView = content.getNameEditorView();
        TextFieldsEditorView phoneticEditorView = content.getPhoneticEditorView();
        if (Locale.JAPANESE.getLanguage().equals(Locale.getDefault().getLanguage()) && nameEditorView != null && phoneticEditorView != null) {
            nameEditorView.setPhoneticView(phoneticEditorView);
        }
        content.setEnabled(this.mEnabled);
        content.setVisibility(0);
        invalidateOptionsMenu();
    }

    private void invalidateOptionsMenu() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private boolean isReadyToBindEditors() {
        if (this.mState.isEmpty()) {
            if (Log.isLoggable("ContactEditor", 2)) {
                Log.v("ContactEditor", "No data to bind editors");
            }
            return false;
        }
        if (this.mIsEdit && !this.mExistingContactDataReady) {
            if (Log.isLoggable("ContactEditor", 2)) {
                Log.v("ContactEditor", "Existing contact data is not ready to bind editors.");
            }
            return false;
        }
        if (this.mHasNewContact && !this.mNewContactDataReady) {
            if (Log.isLoggable("ContactEditor", 2)) {
                Log.v("ContactEditor", "New contact data is not ready to bind editors.");
            }
            return false;
        }
        return RequestPermissionsActivity.hasRequiredPermissions(this.mContext);
    }

    private void rebindEditorsForNewContact(RawContactDelta rawContactDelta, AccountWithDataSet accountWithDataSet, AccountWithDataSet accountWithDataSet2) {
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(this.mContext);
        AccountType accountTypeForAccount = accountTypeManager.getAccountTypeForAccount(accountWithDataSet);
        AccountType accountTypeForAccount2 = accountTypeManager.getAccountTypeForAccount(accountWithDataSet2);
        ContactEditorUtilsEx.updateDataKindsForSim(accountTypeForAccount2, this.mSubsciberAccount.getSubId());
        ContactEditorUtilsEx.setSimDataKindCountMax(accountTypeForAccount2, this.mSubsciberAccount.getSubId());
        this.mExistingContactDataReady = false;
        this.mNewContactDataReady = false;
        this.mState = new RawContactDeltaList();
        setStateForNewContact(accountWithDataSet2, accountTypeForAccount2, rawContactDelta, accountTypeForAccount, isEditingUserProfile());
        if (this.mIsEdit) {
            setStateForExistingContact(isEditingUserProfile(), this.mRawContacts);
        }
    }

    @Override
    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    public void load(String str, Uri uri, Bundle bundle) {
        Log.sensitive("ContactEditor", "[load]action = " + str + ",lookupUri = " + uri);
        this.mAction = str;
        this.mLookupUri = uri;
        this.mIntentExtras = bundle;
        Log.sensitive("ContactEditor", "[load] mIntentExtras = " + this.mIntentExtras);
        if (this.mIntentExtras != null) {
            this.mAutoAddToDefaultGroup = this.mIntentExtras.containsKey("addToDefaultDirectory");
            this.mNewLocalProfile = this.mIntentExtras.getBoolean("newLocalProfile");
            this.mDisableDeleteMenuOption = this.mIntentExtras.getBoolean("disableDeleteMenuOption");
            if (this.mIntentExtras.containsKey("material_palette_primary_color") && this.mIntentExtras.containsKey("material_palette_secondary_color")) {
                this.mMaterialPalette = new MaterialColorMapUtils.MaterialPalette(this.mIntentExtras.getInt("material_palette_primary_color"), this.mIntentExtras.getInt("material_palette_secondary_color"));
            }
            this.mRawContactIdToDisplayAlone = this.mIntentExtras.getLong("raw_contact_id_to_display_alone");
        }
    }

    @Override
    public void setIntentExtras(Bundle bundle) {
        getContent().setIntentExtras(bundle);
    }

    @Override
    public void onJoinCompleted(Uri uri) {
        Log.d("ContactEditor", "[onJoinCompleted],uri = " + uri);
        onSaveCompleted(false, 1, uri != null, uri, null);
    }

    private String getNameToDisplay(Uri uri) {
        Cursor cursorQuery;
        if (uri != null && (cursorQuery = this.mContext.getContentResolver().query(uri, new String[]{"display_name", "display_name_alt"}, null, null, null)) != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    String string = cursorQuery.getString(0);
                    String string2 = cursorQuery.getString(1);
                    cursorQuery.close();
                    return ContactDisplayUtils.getPreferredDisplayName(string, string2, new ContactsPreferences(this.mContext));
                }
            } finally {
                cursorQuery.close();
            }
        }
        return null;
    }

    @Override
    public void onSaveCompleted(boolean z, int i, boolean z2, Uri uri, Long l) {
        String string;
        this.mSubsciberAccount.getProgressHandler().dismissDialog(getFragmentManager());
        if (z) {
            if (z2) {
                switch (i) {
                    case 2:
                        Toast.makeText(this.mContext, R.string.contactUnlinkedToast, 0).show();
                        break;
                    case 3:
                        break;
                    default:
                        String nameToDisplay = getNameToDisplay(uri);
                        if (!TextUtils.isEmpty(nameToDisplay)) {
                            string = getResources().getString(R.string.contactSavedNamedToast, nameToDisplay);
                        } else {
                            string = getResources().getString(R.string.contactSavedToast);
                        }
                        Toast.makeText(this.mContext, string, 0).show();
                        break;
                }
            } else {
                Toast.makeText(this.mContext, R.string.contactSavedErrorToast, 1).show();
            }
        }
        Intent intentComposeQuickContactIntent = null;
        switch (i) {
            case 0:
                if (z2 && uri != null) {
                    Uri uriMaybeConvertToLegacyLookupUri = ContactEditorUtils.maybeConvertToLegacyLookupUri(this.mContext, uri, this.mLookupUri);
                    ExtensionManager.getInstance();
                    ExtensionManager.getRcsRichUiExtension().loadRichScrnByContactUri(uriMaybeConvertToLegacyLookupUri, getActivity());
                    intentComposeQuickContactIntent = ImplicitIntentsUtil.composeQuickContactIntent(this.mContext, uriMaybeConvertToLegacyLookupUri, 6);
                    intentComposeQuickContactIntent.putExtra("contact_edited", true);
                }
                Log.d("ContactEditor", "[onSaveCompleted]SaveMode.CLOSE,change status as Status.CLOSING");
                this.mStatus = 3;
                if (this.mListener != null) {
                    this.mListener.onSaveFinished(intentComposeQuickContactIntent);
                }
                break;
            case 1:
                if (z2 && uri != null) {
                    this.mState = new RawContactDeltaList();
                    load("android.intent.action.EDIT", uri, null);
                    this.mStatus = 0;
                    getLoaderManager().restartLoader(1, null, this.mContactLoaderListener);
                    break;
                }
                break;
            case 2:
                Log.d("ContactEditor", "[onSaveCompleted]SaveMode.SPLIT,change status as Status.CLOSING");
                this.mStatus = 3;
                if (this.mListener != null) {
                    this.mListener.onContactSplit(uri);
                } else if (Log.isLoggable("ContactEditor", 3)) {
                    Log.d("ContactEditor", "No listener registered, can not call onSplitFinished");
                }
                break;
            case 3:
                if (z2 && uri != null && l != null) {
                    joinAggregate(l.longValue());
                    break;
                }
                break;
            case CompatUtils.TYPE_ASSERT:
                this.mStatus = 3;
                if (this.mListener != null) {
                    this.mListener.onSaveFinished(null);
                }
                break;
        }
    }

    private void showJoinAggregateActivity(Uri uri) {
        if (uri == null || !isAdded()) {
            Log.w("ContactEditor", "[showJoinAggregateActivity]error,contactLookupUri = " + uri);
            return;
        }
        Log.d("ContactEditor", "[showJoinAggregateActivity]");
        this.mContactIdForJoin = ContentUris.parseId(uri);
        Intent intent = new Intent(this.mContext, (Class<?>) ContactSelectionActivity.class);
        intent.setAction("com.android.contacts.action.JOIN_CONTACT");
        intent.putExtra("com.android.contacts.action.CONTACT_ID", this.mContactIdForJoin);
        startActivityForResult(intent, 0);
    }

    protected void acquireAggregationSuggestions(Context context, long j, ValuesDelta valuesDelta) {
        if (this.mSubsciberAccount.isAccountTypeIccCard(this.mState)) {
            return;
        }
        this.mAggregationSuggestionsRawContactId = j;
        if (this.mAggregationSuggestionEngine == null) {
            this.mAggregationSuggestionEngine = new AggregationSuggestionEngine(context);
            this.mAggregationSuggestionEngine.setListener(this);
            this.mAggregationSuggestionEngine.start();
        }
        this.mAggregationSuggestionEngine.setContactId(getContactId());
        this.mAggregationSuggestionEngine.setAccountFilter(getContent().getCurrentRawContactDelta().getAccountWithDataSet());
        this.mAggregationSuggestionEngine.onNameChange(valuesDelta);
    }

    private long getContactId() {
        Iterator<RawContactDelta> it = this.mState.iterator();
        while (it.hasNext()) {
            Long asLong = it.next().getValues().getAsLong("contact_id");
            if (asLong != null) {
                return asLong.longValue();
            }
        }
        return 0L;
    }

    @Override
    public void onAggregationSuggestionChange() {
        Activity activity = getActivity();
        Log.d("ContactEditor", "[onAggregationSuggestionChange]mStatus = " + this.mStatus);
        if ((activity != null && activity.isFinishing()) || !isVisible() || this.mState.isEmpty() || this.mStatus != 1) {
            Log.w("ContactEditor", "[onAggregationSuggestionChange]invalid status,return. ");
            return;
        }
        UiClosables.closeQuietly(this.mAggregationSuggestionPopup);
        if (this.mAggregationSuggestionEngine.getSuggestedContactCount() == 0) {
            Log.w("ContactEditor", "[onAggregationSuggestionChange]count = 0,return. ");
            return;
        }
        View aggregationAnchorView = getAggregationAnchorView();
        if (aggregationAnchorView == null) {
            Log.w("ContactEditor", "[onAggregationSuggestionChange]anchorView = null,return.");
            return;
        }
        this.mAggregationSuggestionPopup = new ListPopupWindow(this.mContext, null);
        this.mAggregationSuggestionPopup.setAnchorView(aggregationAnchorView);
        this.mAggregationSuggestionPopup.setWidth(aggregationAnchorView.getWidth());
        this.mAggregationSuggestionPopup.setInputMethodMode(2);
        this.mAggregationSuggestionPopup.setAdapter(new AggregationSuggestionAdapter(getActivity(), this, this.mAggregationSuggestionEngine.getSuggestions()));
        this.mAggregationSuggestionPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                ((AggregationSuggestionView) view).handleItemClickEvent();
                UiClosables.closeQuietly(ContactEditorFragment.this.mAggregationSuggestionPopup);
                ContactEditorFragment.this.mAggregationSuggestionPopup = null;
            }
        });
        this.mAggregationSuggestionPopup.show();
    }

    protected View getAggregationAnchorView() {
        return getContent().getAggregationAnchorView();
    }

    @Override
    public void onEditAction(Uri uri, long j) {
        SuggestionEditConfirmationDialogFragment.show(this, uri, j);
    }

    public void doEditSuggestedContact(Uri uri, long j) {
        if (this.mListener != null) {
            Log.d("ContactEditor", "[doEditSuggestedContact]change status is Status.CLOSING");
            this.mStatus = 3;
            this.mListener.onEditOtherRawContactRequested(uri, j, getContent().getCurrentRawContactDelta().getContentValues());
        }
    }

    protected void setGroupMetaData() {
        if (this.mGroupMetaData != null) {
            getContent().setGroupMetaData(this.mGroupMetaData);
        }
    }

    protected boolean doSaveAction(int i, Long l) {
        Log.d("ContactEditor", "[doSaveAction] start ContactSaveService,saveMode = " + i + ",joinContactId = " + l);
        return startSaveService(this.mContext, ContactSaveService.createSaveContactIntent(this.mContext, this.mState, ContactSaveService.EXTRA_SAVE_MODE, i, isEditingUserProfile(), ((Activity) this.mContext).getClass(), "saveCompleted", this.mUpdatedPhotos, "joinContactId", l), i);
    }

    private boolean startSaveService(Context context, Intent intent, int i) {
        boolean zStartService = ContactSaveService.startService(context, intent, i);
        if (!zStartService) {
            onCancelEditConfirmed();
        }
        return zStartService;
    }

    @Override
    public void onSaveSIMContactCompleted(boolean z, Intent intent) {
        if (intent == null) {
            Log.w("ContactEditor", "[onSaveSIMContactCompleted] data is null.");
        }
        this.mSubsciberAccount.getProgressHandler().dismissDialog(getFragmentManager());
        Log.i("ContactEditor", "[onSaveSIMContactCompleted] mStatus = " + this.mStatus);
        if (this.mStatus == 4) {
            Log.d("ContactEditor", "[onSaveSIMContactCompleted]status changed as EDITING,ori is SUB_ACTIVITY");
            this.mStatus = 1;
        }
        int intExtra = intent.getIntExtra("result", -2);
        Log.d("ContactEditor", "[onSaveSIMContactCompleted] result = " + intExtra);
        if (intExtra == 0) {
            this.mStatus = 1;
            Log.d("ContactEditor", "[onSaveSIMContactCompleted]change status is Status.EDITING 2");
            if (intent != null) {
                boolean booleanExtra = intent.getBooleanExtra("isQuitEdit", false);
                Log.d("ContactEditor", "[onSaveSIMContactCompleted] isQuitEdit : " + booleanExtra);
                if (booleanExtra && getActivity() != null) {
                    getActivity().finish();
                    Log.d("ContactEditor", "[onSaveSIMContactCompleted] finish activity.");
                    return;
                }
                ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("simData");
                Log.sensitive("ContactEditor", "[onSaveSIMContactCompleted] simData : " + parcelableArrayListExtra);
                this.mState = (RawContactDeltaList) parcelableArrayListExtra;
                this.mAggregationSuggestionsRawContactId = 0L;
                setEnabled(true);
                Log.d("ContactEditor", "[onSaveSIMContactCompleted] setEnabletrue, and bindEditors");
                bindEditors();
                return;
            }
            return;
        }
        if (intExtra != 1) {
            if (intExtra == 2) {
                this.mStatus = 1;
                if (intent != null) {
                    boolean booleanExtra2 = intent.getBooleanExtra("isQuitEdit", false);
                    Log.d("ContactEditor", "[onSaveSIMContactCompleted] isQuitEdit : " + booleanExtra2);
                    if (booleanExtra2 && getActivity() != null) {
                        getActivity().finish();
                        return;
                    }
                    setEnabled(true);
                    bindEditors();
                    Log.d("ContactEditor", "[onSaveSIMContactCompleted] setEnabletrue, and bindEditors");
                    return;
                }
                return;
            }
            return;
        }
        int intExtra2 = intent.getIntExtra(ContactSaveService.EXTRA_SAVE_MODE, 0);
        Uri data = intent.getData();
        Log.sensitive("ContactEditor", "[onSaveSIMContactCompleted] result: RESULT_OK,mIsEdit = " + this.mIsEdit + ",lookupUri = " + data);
        Intent intentComposeQuickContactIntent = null;
        switch (intExtra2) {
            case 0:
                if (data != null) {
                    Uri uriMaybeConvertToLegacyLookupUri = ContactEditorUtils.maybeConvertToLegacyLookupUri(this.mContext, data, this.mLookupUri);
                    ExtensionManager.getInstance();
                    ExtensionManager.getRcsRichUiExtension().loadRichScrnByContactUri(uriMaybeConvertToLegacyLookupUri, getActivity());
                    intentComposeQuickContactIntent = ImplicitIntentsUtil.composeQuickContactIntent(this.mContext, uriMaybeConvertToLegacyLookupUri, 6);
                    intentComposeQuickContactIntent.putExtra("previous_screen_type", 6);
                }
                this.mStatus = 3;
                Log.d("ContactEditor", "[onSaveSIMContactCompleted]change status is Status.CLOSING");
                if (this.mListener != null) {
                    this.mListener.onSaveFinished(intentComposeQuickContactIntent);
                }
                Log.d("ContactEditor", "Status.CLOSING onSaveFinished");
                break;
            case 1:
                Log.d("ContactEditor", "[onSaveCompleted]SaveMode.RELOAD2, reloadFullEditor");
                if (data != null) {
                    this.mState = new RawContactDeltaList();
                    load("android.intent.action.EDIT", data, null);
                    this.mStatus = 0;
                    getLoaderManager().restartLoader(1, null, this.mContactLoaderListener);
                }
                break;
        }
    }

    protected boolean saveToIccCard(RawContactDeltaList rawContactDeltaList, int i, Class<? extends Activity> cls) {
        Log.d("ContactEditor", "[saveToIccCard]saveMode = " + i);
        if (!preSavetoSim(i)) {
            Log.i("ContactEditor", "[saveToIccCard]fail,saveMode = " + i);
            return false;
        }
        ContactEditorUtilsEx.showLogContactState(rawContactDeltaList);
        setEnabled(false);
        Intent intent = new Intent(this.mContext, (Class<?>) SimProcessorService.class);
        intent.putParcelableArrayListExtra("simData", rawContactDeltaList);
        intent.putParcelableArrayListExtra("oldsimData", this.mSubsciberAccount.getOldState());
        ContactEditorUtilsEx.showLogContactState(this.mSubsciberAccount.getOldState());
        this.mSubsciberAccount.processSaveToSim(intent, this.mLookupUri);
        Log.d("ContactEditor", "[saveToIccCard]set setEnabled false,the mLookupUri is = " + this.mLookupUri);
        ContactEditorUtilsEx.processGroupMetadataToSim(rawContactDeltaList, intent, this.mGroupMetaData);
        intent.putExtra("subscription_key", this.mSubsciberAccount.getSubId());
        intent.putExtra("work_type", 1);
        if (cls != null) {
            Intent intent2 = new Intent(this.mContext, cls);
            intent2.putExtra(ContactSaveService.EXTRA_SAVE_MODE, i);
            intent2.setAction("com.mediatek.contacts.simservice.EDIT_SIM");
            intent.putExtra(ContactSaveService.EXTRA_CALLBACK_INTENT, intent2);
        }
        this.mContext.startService(intent);
        return true;
    }

    protected boolean preSavetoSim(int i) {
        Log.i("ContactEditor", "[preSavetoSim]saveMode = " + i);
        if (!hasValidState() || this.mStatus != 2) {
            Log.i("ContactEditor", "[preSavetoSim]return false,mStatus = " + this.mStatus);
            return false;
        }
        if (hasPendingChanges()) {
            return true;
        }
        Log.i("ContactEditor", "[preSavetoSim] hasPendingChanges is false");
        setEnabled(true);
        onSaveCompleted(false, i, this.mLookupUri != null, this.mLookupUri, null);
        return false;
    }

    @Override
    public void onPause() {
        Log.d("ContactEditor", "[onPause]");
        if (this.mAggregationSuggestionPopup != null) {
            this.mAggregationSuggestionPopup.dismiss();
            this.mAggregationSuggestionPopup = null;
        }
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().closeTextChangedListener(false);
        super.onPause();
    }

    protected void joinAggregate(long j) {
        Log.d("ContactEditor", "[joinAggregate] start ContactSaveService,contactId = " + j);
        this.mContext.startService(ContactSaveService.createJoinContactsIntent(this.mContext, this.mContactIdForJoin, j, ContactEditorActivity.class, "joinCompleted"));
    }

    public void removePhoto() {
        getContent().removePhoto();
        this.mUpdatedPhotos.remove(String.valueOf(this.mPhotoRawContactId));
    }

    public void updatePhoto(Uri uri) throws FileNotFoundException {
        Bitmap bitmapFromUri = ContactPhotoUtils.getBitmapFromUri(getActivity(), uri);
        if (bitmapFromUri == null || bitmapFromUri.getHeight() <= 0 || bitmapFromUri.getWidth() <= 0) {
            Toast.makeText(this.mContext, R.string.contactPhotoSavedErrorToast, 0).show();
        } else {
            this.mUpdatedPhotos.putParcelable(String.valueOf(this.mPhotoRawContactId), uri);
            getContent().updatePhoto(uri);
        }
    }

    @Override
    public void onNameFieldChanged(long j, ValuesDelta valuesDelta) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        acquireAggregationSuggestions(activity, j, valuesDelta);
    }

    @Override
    public void onRebindEditorsForNewContact(RawContactDelta rawContactDelta, AccountWithDataSet accountWithDataSet, AccountWithDataSet accountWithDataSet2) {
        Log.d("ContactEditor", "[onRebindEditorsForNewContact] beg");
        this.mNewContactAccountChanged = true;
        if (this.mSubsciberAccount.setAccountSimInfo(rawContactDelta, accountWithDataSet2, this.mContext)) {
            return;
        }
        Log.sensitive("ContactEditor", "[onRebindEditorsForNewContact] oldState: " + rawContactDelta + "\n,oldAccount:" + accountWithDataSet + ",newAccount: " + accountWithDataSet2);
        rebindEditorsForNewContact(rawContactDelta, accountWithDataSet, accountWithDataSet2);
        Log.d("ContactEditor", "[onRebindEditorsForNewContact] end");
    }

    @Override
    public void onBindEditorsFailed() {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            Toast.makeText(activity, R.string.editor_failed_to_load, 0).show();
            activity.setResult(0);
            activity.finish();
        }
    }

    @Override
    public void onEditorsBound() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        Log.d("ContactEditor", "[onEditorsBound] isAdd(): " + isAdded());
        if (!isAdded()) {
            Log.w("ContactEditor", "[onEditorsBound] ContactEditorFragment not attached,return!");
        } else {
            getLoaderManager().initLoader(2, null, this.mGroupsLoaderListener);
        }
    }

    @Override
    public void onPhotoEditorViewClicked() {
        getEditorActivity().changePhoto(getPhotoMode());
    }

    private int getPhotoMode() {
        return getContent().isWritablePhotoSet() ? 14 : 4;
    }

    private ContactEditorActivity getEditorActivity() {
        return (ContactEditorActivity) getActivity();
    }

    private RawContactEditorView getContent() {
        return (RawContactEditorView) this.mContent;
    }

    protected boolean doSaveSIMContactAction(int i) {
        Log.d("ContactEditor", "[doSaveSIMContactAction] saveMode = " + i);
        saveToIccCard(this.mState, i, ((Activity) this.mContext).getClass());
        return true;
    }

    @Override
    public void onReceiveEvent(String str, Intent intent) {
        Log.d("ContactEditor", "[onReceiveEvent] eventType: " + str + ",mIsUserProfile: " + this.mIsUserProfile);
        if ("PhbChangeEvent".equals(str)) {
            if (this.mIsUserProfile) {
                Log.i("ContactEditor", "[onReceiveEvent] editing profile, ignore sim state changed,return");
                return;
            }
            if (this.mState == null || this.mState.size() <= 0) {
                Log.i("ContactEditor", "[onReceiveEvent] mState data is not available, return");
                return;
            }
            RawContactDelta rawContactDelta = this.mState.get(0);
            if (rawContactDelta == null) {
                Log.i("ContactEditor", "[onReceiveEvent] contactDelta is null, return");
                return;
            }
            String accountType = rawContactDelta.getAccountType();
            Log.d("ContactEditor", "[onReceiveEvent] current accountType: " + accountType);
            if (accountType == null) {
                Log.i("ContactEditor", "[onReceiveEvent] current accountType is null, return");
                return;
            }
            if (AccountTypeUtils.isAccountTypeIccCard(accountType)) {
                int intExtra = intent.getIntExtra("subscription", -1000);
                int subIdBySimAccountName = AccountTypeUtils.getSubIdBySimAccountName(getContext(), rawContactDelta.getAccountName());
                Log.d("ContactEditor", "[onReceiveEvent] stateChangedSubId: " + intExtra + ",currentSubId: " + subIdBySimAccountName);
                if (intExtra == -1000) {
                    Log.e("ContactEditor", "[onReceiveEvent] effor sub id,return");
                    return;
                }
                if (subIdBySimAccountName != SubInfoUtils.getInvalidSubId() && intExtra != subIdBySimAccountName) {
                    Log.d("ContactEditor", "[onReceiveEvent] state changed sub id is not current,ignore it,return");
                    return;
                }
                Activity activity = getActivity();
                if (activity == null) {
                    Log.e("ContactEditor", "[onReceiveEvent] cannot get hostActivity!");
                } else if (!activity.isFinishing()) {
                    Log.i("ContactEditor", "[onReceiveEvent] hostActivity finish!");
                    activity.finish();
                }
            }
        }
    }

    private void onSaveAasNameCompletedOnly() {
        Intent intentComposeQuickContactIntent;
        Log.d("ContactEditor", "[onSaveAasNameCompletedOnly] mLookupUri = " + this.mLookupUri);
        if (this.mLookupUri != null) {
            intentComposeQuickContactIntent = ImplicitIntentsUtil.composeQuickContactIntent(this.mContext, this.mLookupUri, 6);
        } else {
            intentComposeQuickContactIntent = null;
        }
        this.mStatus = 3;
        if (this.mListener != null) {
            this.mListener.onSaveFinished(intentComposeQuickContactIntent);
        }
    }
}
