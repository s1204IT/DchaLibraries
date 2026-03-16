package com.android.contacts.editor;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
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
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.ContactLoader;
import com.android.contacts.common.model.RawContact;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.account.GoogleAccountType;
import com.android.contacts.common.model.account.SimAccountType;
import com.android.contacts.common.util.AccountsListAdapter;
import com.android.contacts.common.util.SimPhoneBookCommonUtil;
import com.android.contacts.detail.PhotoSelectionHandler;
import com.android.contacts.editor.AggregationSuggestionEngine;
import com.android.contacts.editor.AggregationSuggestionView;
import com.android.contacts.editor.BaseRawContactEditorView;
import com.android.contacts.editor.Editor;
import com.android.contacts.editor.SplitContactConfirmationDialogFragment;
import com.android.contacts.util.ContactPhotoUtils;
import com.android.contacts.util.HelpUtils;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.util.UiClosables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ContactEditorFragment extends Fragment implements AggregationSuggestionEngine.Listener, AggregationSuggestionView.Listener, BaseRawContactEditorView.Listener, SplitContactConfirmationDialogFragment.Listener {
    private static final String TAG = ContactEditorFragment.class.getSimpleName();
    private String mAction;
    private AggregationSuggestionEngine mAggregationSuggestionEngine;
    private ListPopupWindow mAggregationSuggestionPopup;
    private View mAggregationSuggestionView;
    private long mAggregationSuggestionsRawContactId;
    private boolean mArePhoneOptionsChangable;
    private boolean mAutoAddToDefaultGroup;
    private long mContactIdForJoin;
    private boolean mContactWritableForJoin;
    private LinearLayout mContent;
    private Context mContext;
    private PhotoHandler mCurrentPhotoHandler;
    private Uri mCurrentPhotoUri;
    private String mCustomRingtone;
    private String mDefaultDisplayName;
    private ContactEditorUtils mEditorUtils;
    private Cursor mGroupMetaData;
    private Bundle mIntentExtras;
    private Listener mListener;
    private long mLoaderStartTime;
    private Uri mLookupUri;
    private long mRawContactIdRequestingPhoto;
    private ImmutableList<RawContact> mRawContacts;
    private boolean mRequestFocus;
    private boolean mSendToVoicemailState;
    private RawContactDeltaList mState;
    private RawContactDeltaList mStateOld;
    private int mStatus;
    private TelephonyManager mTelephonyManager;
    private ViewIdGenerator mViewIdGenerator;
    private final AccountWithDataSet mMeAccount = new AccountWithDataSet("Me", "Me", null);
    private final AccountWithDataSet mPhoneAccount = new AccountWithDataSet("Phone", "Phone", null);
    private final EntityDeltaComparator mComparator = new EntityDeltaComparator();
    private Bundle mUpdatedPhotos = new Bundle();
    private boolean mHasNewContact = false;
    private boolean mNewContactDataReady = false;
    private boolean mIsEdit = false;
    private boolean mExistingContactDataReady = false;
    private HashMap<Long, Boolean> mExpandedEditors = new HashMap<>();
    private AdapterView.OnItemClickListener mAggregationSuggestionItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            AggregationSuggestionView suggestionView = (AggregationSuggestionView) view;
            suggestionView.handleItemClickEvent();
            UiClosables.closeQuietly(ContactEditorFragment.this.mAggregationSuggestionPopup);
            ContactEditorFragment.this.mAggregationSuggestionPopup = null;
        }
    };
    private boolean mEnabled = true;
    private boolean mNewLocalProfile = false;
    private boolean mIsUserProfile = false;
    private boolean mDisableDeleteMenuOption = false;
    private final LoaderManager.LoaderCallbacks<Contact> mDataLoaderListener = new LoaderManager.LoaderCallbacks<Contact>() {
        @Override
        public Loader<Contact> onCreateLoader(int id, Bundle args) {
            ContactEditorFragment.this.mLoaderStartTime = SystemClock.elapsedRealtime();
            return new ContactLoader(ContactEditorFragment.this.mContext, ContactEditorFragment.this.mLookupUri, true);
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact data) {
            long loaderCurrentTime = SystemClock.elapsedRealtime();
            Log.v(ContactEditorFragment.TAG, "Time needed for loading: " + (loaderCurrentTime - ContactEditorFragment.this.mLoaderStartTime));
            if (!data.isLoaded()) {
                Log.i(ContactEditorFragment.TAG, "No contact found. Closing activity");
                ContactEditorFragment.this.mStatus = 3;
                if (ContactEditorFragment.this.mListener != null) {
                    ContactEditorFragment.this.mListener.onContactNotFound();
                    return;
                }
                return;
            }
            ContactEditorFragment.this.mStatus = 1;
            ContactEditorFragment.this.mLookupUri = data.getLookupUri();
            long setDataStartTime = SystemClock.elapsedRealtime();
            ContactEditorFragment.this.setData(data);
            long setDataEndTime = SystemClock.elapsedRealtime();
            Log.v(ContactEditorFragment.TAG, "Time needed for setting UI: " + (setDataEndTime - setDataStartTime));
        }

        @Override
        public void onLoaderReset(Loader<Contact> loader) {
        }
    };
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader2(int id, Bundle args) {
            return new GroupMetaDataLoader(ContactEditorFragment.this.mContext, ContactsContract.Groups.CONTENT_URI);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            ContactEditorFragment.this.mGroupMetaData = data;
            ContactEditorFragment.this.bindGroupMetaData();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    public interface Listener {
        void onContactNotFound();

        void onContactSplit(Uri uri);

        void onCustomCreateContactActivityRequested(AccountWithDataSet accountWithDataSet, Bundle bundle);

        void onCustomEditContactActivityRequested(AccountWithDataSet accountWithDataSet, Uri uri, Bundle bundle, boolean z);

        void onDeleteRequested(Uri uri);

        void onEditOtherContactRequested(Uri uri, ArrayList<ContentValues> arrayList);

        void onReverted();

        void onSaveFinished(Intent intent);
    }

    private static final class AggregationSuggestionAdapter extends BaseAdapter {
        private final Activity mActivity;
        private final AggregationSuggestionView.Listener mListener;
        private final boolean mSetNewContact;
        private final List<AggregationSuggestionEngine.Suggestion> mSuggestions;

        public AggregationSuggestionAdapter(Activity activity, boolean setNewContact, AggregationSuggestionView.Listener listener, List<AggregationSuggestionEngine.Suggestion> suggestions) {
            this.mActivity = activity;
            this.mSetNewContact = setNewContact;
            this.mListener = listener;
            this.mSuggestions = suggestions;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AggregationSuggestionEngine.Suggestion suggestion = (AggregationSuggestionEngine.Suggestion) getItem(position);
            LayoutInflater inflater = this.mActivity.getLayoutInflater();
            AggregationSuggestionView suggestionView = (AggregationSuggestionView) inflater.inflate(R.layout.aggregation_suggestions_item, (ViewGroup) null);
            suggestionView.setNewContact(this.mSetNewContact);
            suggestionView.setListener(this.mListener);
            suggestionView.bindSuggestion(suggestion);
            return suggestionView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return this.mSuggestions.get(position);
        }

        @Override
        public int getCount() {
            return this.mSuggestions.size();
        }
    }

    public void setEnabled(boolean enabled) {
        if (this.mEnabled != enabled) {
            this.mEnabled = enabled;
            if (this.mContent != null) {
                int count = this.mContent.getChildCount();
                for (int i = 0; i < count; i++) {
                    this.mContent.getChildAt(i).setEnabled(enabled);
                }
            }
            setAggregationSuggestionViewEnabled(enabled);
            Activity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
        this.mEditorUtils = ContactEditorUtils.getInstance(this.mContext);
    }

    @Override
    public void onStop() {
        super.onStop();
        UiClosables.closeQuietly(this.mAggregationSuggestionPopup);
        if (!getActivity().isChangingConfigurations() && this.mStatus == 1) {
            save(1);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mAggregationSuggestionEngine != null) {
            this.mAggregationSuggestionEngine.quit();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.contact_editor_fragment, container, false);
        this.mContent = (LinearLayout) view.findViewById(R.id.editors);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        validateAction(this.mAction);
        if (this.mState.isEmpty()) {
            if ("android.intent.action.EDIT".equals(this.mAction)) {
                getLoaderManager().initLoader(1, null, this.mDataLoaderListener);
            }
        } else {
            bindEditors();
        }
        if (savedInstanceState == null) {
            if ("android.intent.action.EDIT".equals(this.mAction)) {
                this.mIsEdit = true;
                return;
            }
            if ("android.intent.action.INSERT".equals(this.mAction)) {
                this.mHasNewContact = true;
                Account account = this.mIntentExtras == null ? null : (Account) this.mIntentExtras.getParcelable("com.android.contacts.extra.ACCOUNT");
                String dataSet = this.mIntentExtras == null ? null : this.mIntentExtras.getString("com.android.contacts.extra.DATA_SET");
                if (account != null) {
                    createContact(new AccountWithDataSet(account.name, account.type, dataSet));
                } else {
                    selectAccountAndCreateContact();
                }
            }
        }
    }

    private void validateAction(String action) {
        if ("android.intent.action.EDIT".equals(action) || "android.intent.action.INSERT".equals(action) || "saveCompleted".equals(action)) {
        } else {
            throw new IllegalArgumentException("Unknown Action String " + this.mAction + ". Only support android.intent.action.EDIT or android.intent.action.INSERT or saveCompleted");
        }
    }

    @Override
    public void onStart() {
        getLoaderManager().initLoader(2, null, this.mGroupLoaderListener);
        super.onStart();
    }

    public void load(String action, Uri lookupUri, Bundle intentExtras) {
        this.mAction = action;
        this.mLookupUri = lookupUri;
        this.mIntentExtras = intentExtras;
        this.mAutoAddToDefaultGroup = this.mIntentExtras != null && this.mIntentExtras.containsKey("addToDefaultDirectory");
        this.mNewLocalProfile = this.mIntentExtras != null && this.mIntentExtras.getBoolean("newLocalProfile");
        this.mDisableDeleteMenuOption = this.mIntentExtras != null && this.mIntentExtras.getBoolean("disableDeleteMenuOption");
    }

    public void setListener(Listener value) {
        this.mListener = value;
    }

    @Override
    public void onCreate(Bundle savedState) {
        if (savedState != null) {
            this.mLookupUri = (Uri) savedState.getParcelable("uri");
            this.mAction = savedState.getString("action");
        }
        this.mTelephonyManager = TelephonyManager.from(getActivity());
        Log.d(TAG, "oncreate()");
        super.onCreate(savedState);
        if (savedState == null) {
            this.mViewIdGenerator = new ViewIdGenerator();
        } else {
            this.mState = (RawContactDeltaList) savedState.getParcelable("state");
            this.mRawContactIdRequestingPhoto = savedState.getLong("photorequester");
            this.mViewIdGenerator = (ViewIdGenerator) savedState.getParcelable("viewidgenerator");
            this.mCurrentPhotoUri = (Uri) savedState.getParcelable("currentphotouri");
            this.mContactIdForJoin = savedState.getLong("contactidforjoin");
            this.mContactWritableForJoin = savedState.getBoolean("contactwritableforjoin");
            this.mAggregationSuggestionsRawContactId = savedState.getLong("showJoinSuggestions");
            this.mEnabled = savedState.getBoolean("enabled");
            this.mStatus = savedState.getInt("status");
            this.mNewLocalProfile = savedState.getBoolean("newLocalProfile");
            this.mDisableDeleteMenuOption = savedState.getBoolean("disableDeleteMenuOption");
            this.mIsUserProfile = savedState.getBoolean("isUserProfile");
            this.mUpdatedPhotos = (Bundle) savedState.getParcelable("updatedPhotos");
            this.mIsEdit = savedState.getBoolean("isEdit");
            this.mHasNewContact = savedState.getBoolean("hasNewContact");
            this.mNewContactDataReady = savedState.getBoolean("newContactDataReady");
            this.mExistingContactDataReady = savedState.getBoolean("existingContactDataReady");
            this.mRawContacts = ImmutableList.copyOf((Collection) savedState.getParcelableArrayList("rawContacts"));
            this.mSendToVoicemailState = savedState.getBoolean("sendToVoicemailState");
            this.mCustomRingtone = savedState.getString("customRingtone");
            this.mArePhoneOptionsChangable = savedState.getBoolean("arePhoneOptionsChangable");
            this.mExpandedEditors = (HashMap) savedState.getSerializable("expandedEditors");
        }
        if (this.mState == null) {
            this.mState = new RawContactDeltaList();
        }
        if (this.mTelephonyManager.hasIccCard(0) || this.mTelephonyManager.hasIccCard(1)) {
            initSimAccountType();
        }
    }

    private void initSimAccountType() {
        Log.d(TAG, "initSimAccountType()");
        this.mTelephonyManager = TelephonyManager.from(getActivity());
        try {
            if (SimPhoneBookCommonUtil.isSimEnabled(0)) {
                AccountType accountType = AccountTypeManager.getInstance(getActivity().getBaseContext()).getAccountType("com.android.contact.sim", null);
                if (accountType instanceof SimAccountType) {
                    SimAccountType simAccount = (SimAccountType) accountType;
                    simAccount.addDataKindStructuredName(getActivity().getBaseContext());
                    simAccount.addDataKindDisplayName(getActivity().getBaseContext());
                    if (SimPhoneBookCommonUtil.isSneFieldEnable(0)) {
                        simAccount.addDataKindNickname(getActivity().getBaseContext());
                    }
                    simAccount.addDataKindPhone(getActivity().getBaseContext());
                    int[] subs = SubscriptionManager.getSubId(0);
                    if (2 == this.mTelephonyManager.getUiccAppType(subs[0])) {
                        simAccount.addDataKindEmail(getActivity().getBaseContext());
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "simAccount1 already init" + e);
        }
        try {
            if (SimPhoneBookCommonUtil.isSimEnabled(1) && this.mTelephonyManager.isMultiSimEnabled()) {
                AccountType accountType2 = AccountTypeManager.getInstance(getActivity().getBaseContext()).getAccountType("com.android.contact.sim2", null);
                if (accountType2 instanceof SimAccountType) {
                    SimAccountType simAccount2 = (SimAccountType) accountType2;
                    simAccount2.addDataKindStructuredName(getActivity().getBaseContext());
                    simAccount2.addDataKindDisplayName(getActivity().getBaseContext());
                    if (SimPhoneBookCommonUtil.isSneFieldEnable(1)) {
                        simAccount2.addDataKindNickname(getActivity().getBaseContext());
                    }
                    simAccount2.addDataKindPhone(getActivity().getBaseContext());
                    int[] subs2 = SubscriptionManager.getSubId(1);
                    if (2 == this.mTelephonyManager.getUiccAppType(subs2[0])) {
                        simAccount2.addDataKindEmail(getActivity().getBaseContext());
                    }
                }
            }
        } catch (Exception e2) {
            Log.w(TAG, "simAccount2 already init" + e2);
        }
    }

    public void setData(Contact contact) {
        if (!this.mState.isEmpty()) {
            Log.v(TAG, "Ignoring background change. This will have to be rebased later");
            return;
        }
        this.mRawContacts = contact.getRawContacts();
        if (this.mRawContacts.size() == 1) {
            RawContact rawContact = this.mRawContacts.get(0);
            String type = rawContact.getAccountTypeString();
            String dataSet = rawContact.getDataSet();
            AccountType accountType = rawContact.getAccountType(this.mContext);
            if (accountType.getEditContactActivityClassName() != null && !accountType.areContactsWritable()) {
                if (this.mListener != null) {
                    String name = rawContact.getAccountName();
                    long rawContactId = rawContact.getId().longValue();
                    this.mListener.onCustomEditContactActivityRequested(new AccountWithDataSet(name, type, dataSet), ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId), this.mIntentExtras, true);
                    return;
                }
                return;
            }
        }
        String displayName = null;
        if (!contact.isUserProfile() && !contact.isWritableContact(this.mContext)) {
            this.mHasNewContact = true;
            selectAccountAndCreateContact();
            displayName = contact.getDisplayName();
        }
        bindEditorsForExistingContact(displayName, contact.isUserProfile(), this.mRawContacts);
        bindMenuItemsForPhone(contact);
    }

    @Override
    public void onExternalEditorRequest(AccountWithDataSet account, Uri uri) {
        this.mListener.onCustomEditContactActivityRequested(account, uri, null, false);
    }

    @Override
    public void onEditorExpansionChanged() {
        updatedExpandedEditorsMap();
    }

    private void bindEditorsForExistingContact(String displayName, boolean isUserProfile, ImmutableList<RawContact> rawContacts) {
        setEnabled(true);
        this.mDefaultDisplayName = displayName;
        this.mState.addAll(rawContacts.iterator());
        setIntentExtras(this.mIntentExtras);
        this.mIntentExtras = null;
        this.mStateOld = RawContactDeltaList.fromIterator(rawContacts.iterator());
        Log.d(TAG, "bindEditorsForExistingContact() mStateOld = " + this.mStateOld);
        this.mIsUserProfile = isUserProfile;
        boolean localProfileExists = false;
        if (this.mIsUserProfile) {
            for (RawContactDelta state : this.mState) {
                state.setProfileQueryUri();
                if ("Me".equals(state.getValues().getAsString("account_type"))) {
                    localProfileExists = true;
                }
            }
            if (!localProfileExists) {
                RawContact rawContact = new RawContact();
                rawContact.setAccount(this.mMeAccount);
                RawContactDelta insert = new RawContactDelta(ValuesDelta.fromAfter(rawContact.getValues()));
                insert.setProfileQueryUri();
                this.mState.add(insert);
            }
        }
        this.mRequestFocus = true;
        this.mExistingContactDataReady = true;
        bindEditors();
    }

    private void bindMenuItemsForPhone(Contact contact) {
        this.mSendToVoicemailState = contact.isSendToVoicemail();
        this.mCustomRingtone = contact.getCustomRingtone();
        this.mArePhoneOptionsChangable = arePhoneOptionsChangable(contact);
    }

    private boolean arePhoneOptionsChangable(Contact contact) {
        return (contact == null || contact.isDirectoryEntry() || !PhoneCapabilityTester.isPhone(this.mContext)) ? false : true;
    }

    public void setIntentExtras(Bundle extras) {
        if (extras != null && extras.size() != 0) {
            AccountTypeManager accountTypes = AccountTypeManager.getInstance(this.mContext);
            for (RawContactDelta state : this.mState) {
                AccountType type = state.getAccountType(accountTypes);
                if (type.areContactsWritable()) {
                    RawContactModifier.parseExtras(this.mContext, type, state, extras);
                    return;
                }
            }
        }
    }

    private void selectAccountAndCreateContact() {
        if (this.mNewLocalProfile) {
            createContact(null);
            return;
        }
        if (this.mEditorUtils.shouldShowAccountChangedNotification()) {
            Intent intent = new Intent(this.mContext, (Class<?>) ContactEditorAccountsChangedActivity.class);
            this.mStatus = 4;
            startActivityForResult(intent, 1);
        } else {
            AccountWithDataSet defaultAccount = this.mEditorUtils.getDefaultAccount();
            if (defaultAccount == null) {
                createContact(null);
            } else {
                createContact(defaultAccount);
            }
        }
    }

    private void createContact() {
        List<AccountWithDataSet> accounts = AccountTypeManager.getInstance(this.mContext).getAccounts(true);
        if (accounts.isEmpty()) {
            createContact(null);
        } else {
            createContact(accounts.get(0));
        }
    }

    private void createContact(AccountWithDataSet account) {
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(this.mContext);
        AccountType accountType = accountTypes.getAccountType(account != null ? account.type : null, account != null ? account.dataSet : null);
        if (accountType.getCreateContactActivityClassName() != null) {
            if (this.mListener != null) {
                this.mListener.onCustomCreateContactActivityRequested(account, this.mIntentExtras);
                return;
            }
            return;
        }
        bindEditorsForNewContact(account, accountType);
    }

    private void rebindEditorsForNewContact(RawContactDelta oldState, AccountWithDataSet oldAccount, AccountWithDataSet newAccount) {
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(this.mContext);
        AccountType oldAccountType = accountTypes.getAccountType(oldAccount.type, oldAccount.dataSet);
        AccountType newAccountType = accountTypes.getAccountType(newAccount.type, newAccount.dataSet);
        if (newAccountType.getCreateContactActivityClassName() != null) {
            Log.w(TAG, "external activity called in rebind situation");
            if (this.mListener != null) {
                this.mListener.onCustomCreateContactActivityRequested(newAccount, this.mIntentExtras);
                return;
            }
            return;
        }
        this.mExistingContactDataReady = false;
        this.mNewContactDataReady = false;
        this.mState = new RawContactDeltaList();
        bindEditorsForNewContact(newAccount, newAccountType, oldState, oldAccountType);
        if (this.mIsEdit) {
            bindEditorsForExistingContact(this.mDefaultDisplayName, this.mIsUserProfile, this.mRawContacts);
        }
    }

    private void bindEditorsForNewContact(AccountWithDataSet account, AccountType accountType) {
        this.mStateOld = null;
        bindEditorsForNewContact(account, accountType, null, null);
    }

    private void bindEditorsForNewContact(AccountWithDataSet newAccount, AccountType newAccountType, RawContactDelta oldState, AccountType oldAccountType) {
        this.mStatus = 1;
        RawContact rawContact = new RawContact();
        if (newAccount != null) {
            rawContact.setAccount(newAccount);
        } else if (this.mNewLocalProfile) {
            rawContact.setAccount(this.mMeAccount);
        } else {
            rawContact.setAccount(this.mPhoneAccount);
        }
        ValuesDelta valuesDelta = ValuesDelta.fromAfter(rawContact.getValues());
        RawContactDelta insert = new RawContactDelta(valuesDelta);
        if (oldState == null) {
            RawContactModifier.parseExtras(this.mContext, newAccountType, insert, this.mIntentExtras);
        } else {
            RawContactModifier.migrateStateForNewContact(this.mContext, oldState, insert, oldAccountType, newAccountType);
        }
        RawContactModifier.ensureKindExists(insert, newAccountType, "vnd.android.cursor.item/phone_v2");
        RawContactModifier.ensureKindExists(insert, newAccountType, "vnd.android.cursor.item/email_v2");
        RawContactModifier.ensureKindExists(insert, newAccountType, "vnd.android.cursor.item/organization");
        RawContactModifier.ensureKindExists(insert, newAccountType, "vnd.android.cursor.item/contact_event");
        RawContactModifier.ensureKindExists(insert, newAccountType, "vnd.android.cursor.item/postal-address_v2");
        if (this.mNewLocalProfile) {
            insert.setProfileQueryUri();
        }
        this.mState.add(insert);
        this.mRequestFocus = true;
        this.mNewContactDataReady = true;
        bindEditors();
    }

    private void bindEditors() {
        BaseRawContactEditorView editor;
        if (!this.mState.isEmpty()) {
            if (!this.mIsEdit || this.mExistingContactDataReady) {
                if (!this.mHasNewContact || this.mNewContactDataReady) {
                    Collections.sort(this.mState, this.mComparator);
                    Log.d(TAG, "bindEditors()  ");
                    this.mContent.removeAllViews();
                    LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
                    AccountTypeManager accountTypes = AccountTypeManager.getInstance(this.mContext);
                    int numRawContacts = this.mState.size();
                    int i = 0;
                    while (i < numRawContacts) {
                        RawContactDelta rawContactDelta = this.mState.get(i);
                        if (rawContactDelta.isVisible()) {
                            AccountType type = rawContactDelta.getAccountType(accountTypes);
                            long rawContactId = rawContactDelta.getRawContactId().longValue();
                            if (!type.areContactsWritable()) {
                                editor = (BaseRawContactEditorView) inflater.inflate(R.layout.raw_contact_readonly_editor_view, (ViewGroup) this.mContent, false);
                            } else {
                                editor = (RawContactEditorView) inflater.inflate(R.layout.raw_contact_editor_view, (ViewGroup) this.mContent, false);
                            }
                            editor.setListener(this);
                            List<AccountWithDataSet> accounts = AccountTypeManager.getInstance(this.mContext).getAccounts(true);
                            if (this.mHasNewContact && !this.mNewLocalProfile && accounts.size() > 1) {
                                addAccountSwitcher(this.mState.get(0), editor);
                            }
                            editor.setEnabled(this.mEnabled);
                            if (this.mExpandedEditors.containsKey(Long.valueOf(rawContactId))) {
                                editor.setCollapsed(this.mExpandedEditors.get(Long.valueOf(rawContactId)).booleanValue());
                            } else {
                                editor.setCollapsed(i != 0);
                            }
                            this.mContent.addView(editor);
                            editor.setState(rawContactDelta, type, this.mViewIdGenerator, isEditingUserProfile());
                            editor.setCollapsible(numRawContacts > 1);
                            bindPhotoHandler(editor, type, this.mState);
                            Uri photoUri = updatedPhotoUriForRawContact(rawContactId);
                            if (photoUri != null) {
                                editor.setFullSizedPhoto(photoUri);
                            }
                            if (editor instanceof RawContactEditorView) {
                                final Activity activity = getActivity();
                                final RawContactEditorView rawContactEditor = (RawContactEditorView) editor;
                                Editor.EditorListener listener = new Editor.EditorListener() {
                                    @Override
                                    public void onRequest(int request) {
                                        if (!activity.isFinishing()) {
                                            if (request == 2 && !ContactEditorFragment.this.isEditingUserProfile()) {
                                                ContactEditorFragment.this.acquireAggregationSuggestions(activity, rawContactEditor);
                                            } else if (request == 6) {
                                                ContactEditorFragment.this.adjustNameFieldsHintDarkness(rawContactEditor);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onDeleteRequested(Editor removedEditor) {
                                    }
                                };
                                StructuredNameEditorView nameEditor = rawContactEditor.getNameEditor();
                                if (this.mRequestFocus) {
                                    nameEditor.requestFocus();
                                    this.mRequestFocus = false;
                                }
                                nameEditor.setEditorListener(listener);
                                if (!TextUtils.isEmpty(this.mDefaultDisplayName)) {
                                    nameEditor.setDisplayName(this.mDefaultDisplayName);
                                }
                                TextFieldsEditorView phoneticNameEditor = rawContactEditor.getPhoneticNameEditor();
                                phoneticNameEditor.setEditorListener(listener);
                                rawContactEditor.setAutoAddToDefaultGroup(this.mAutoAddToDefaultGroup);
                                TextFieldsEditorView nickNameEditor = rawContactEditor.getNickNameEditor();
                                nickNameEditor.setEditorListener(listener);
                                if (rawContactId == this.mAggregationSuggestionsRawContactId) {
                                    acquireAggregationSuggestions(activity, rawContactEditor);
                                }
                                adjustNameFieldsHintDarkness(rawContactEditor);
                            }
                        }
                        i++;
                    }
                    this.mRequestFocus = false;
                    bindGroupMetaData();
                    this.mContent.setVisibility(0);
                    Activity activity2 = getActivity();
                    if (activity2 != null) {
                        activity2.invalidateOptionsMenu();
                    }
                    updatedExpandedEditorsMap();
                }
            }
        }
    }

    private void adjustNameFieldsHintDarkness(RawContactEditorView rawContactEditor) {
        boolean nameFieldsAreNotFocused = rawContactEditor.getNameEditor().findFocus() == null && rawContactEditor.getPhoneticNameEditor().findFocus() == null && rawContactEditor.getNickNameEditor().findFocus() == null;
        rawContactEditor.getNameEditor().setHintColorDark(!nameFieldsAreNotFocused);
        rawContactEditor.getPhoneticNameEditor().setHintColorDark(!nameFieldsAreNotFocused);
        rawContactEditor.getNickNameEditor().setHintColorDark(nameFieldsAreNotFocused ? false : true);
    }

    private void updatedExpandedEditorsMap() {
        for (int i = 0; i < this.mContent.getChildCount(); i++) {
            View childView = this.mContent.getChildAt(i);
            if (childView instanceof BaseRawContactEditorView) {
                BaseRawContactEditorView childEditor = (BaseRawContactEditorView) childView;
                this.mExpandedEditors.put(Long.valueOf(childEditor.getRawContactId()), Boolean.valueOf(childEditor.isCollapsed()));
            }
        }
    }

    private Uri updatedPhotoUriForRawContact(long rawContactId) {
        return (Uri) this.mUpdatedPhotos.get(String.valueOf(rawContactId));
    }

    private void bindPhotoHandler(BaseRawContactEditorView editor, AccountType type, RawContactDeltaList state) {
        int mode;
        boolean showIsPrimaryOption;
        if (type.areContactsWritable()) {
            if (editor.hasSetPhoto()) {
                mode = 14;
                showIsPrimaryOption = hasMoreThanOnePhoto();
            } else {
                mode = 4;
                showIsPrimaryOption = false;
            }
        } else if (editor.hasSetPhoto() && hasMoreThanOnePhoto()) {
            mode = 0;
            showIsPrimaryOption = true;
        } else {
            editor.getPhotoEditor().setEditorListener(null);
            editor.getPhotoEditor().setShowPrimary(false);
            return;
        }
        PhotoHandler photoHandler = new PhotoHandler(this.mContext, editor, mode, state);
        editor.getPhotoEditor().setEditorListener((PhotoHandler.PhotoEditorListener) photoHandler.getListener());
        editor.getPhotoEditor().setShowPrimary(showIsPrimaryOption);
        if (this.mRawContactIdRequestingPhoto == editor.getRawContactId()) {
            this.mCurrentPhotoHandler = photoHandler;
        }
    }

    private void bindGroupMetaData() {
        if (this.mGroupMetaData != null) {
            int editorCount = this.mContent.getChildCount();
            for (int i = 0; i < editorCount; i++) {
                BaseRawContactEditorView editor = (BaseRawContactEditorView) this.mContent.getChildAt(i);
                editor.setGroupMetaData(this.mGroupMetaData);
            }
        }
    }

    private void saveDefaultAccountIfNecessary() {
        if ("android.intent.action.INSERT".equals(this.mAction) || this.mState.size() != 1 || isEditingUserProfile()) {
            RawContactDelta rawContactDelta = this.mState.get(0);
            String name = rawContactDelta.getAccountName();
            String type = rawContactDelta.getAccountType();
            String dataSet = rawContactDelta.getDataSet();
            AccountWithDataSet account = (name == null || type == null) ? null : new AccountWithDataSet(name, type, dataSet);
            this.mEditorUtils.saveDefaultAndAllAccounts(account);
        }
    }

    private void addAccountSwitcher(final RawContactDelta currentState, BaseRawContactEditorView editor) {
        final AccountWithDataSet currentAccount = new AccountWithDataSet(currentState.getAccountName(), currentState.getAccountType(), currentState.getDataSet());
        View accountView = editor.findViewById(R.id.account);
        final View anchorView = editor.findViewById(R.id.account_selector_container);
        if (accountView != null) {
            anchorView.setVisibility(0);
            accountView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ListPopupWindow popup = new ListPopupWindow(ContactEditorFragment.this.mContext, null);
                    final AccountsListAdapter adapter = new AccountsListAdapter(ContactEditorFragment.this.mContext, AccountsListAdapter.AccountListFilter.ACCOUNTS_CONTACT_WRITABLE, currentAccount);
                    popup.setWidth(anchorView.getWidth());
                    popup.setAnchorView(anchorView);
                    popup.setAdapter(adapter);
                    popup.setModal(true);
                    popup.setInputMethodMode(2);
                    popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            UiClosables.closeQuietly(popup);
                            AccountWithDataSet newAccount = adapter.getItem(position);
                            if (!newAccount.equals(currentAccount)) {
                                ContactEditorFragment.this.rebindEditorsForNewContact(currentState, currentAccount, newAccount);
                            }
                        }
                    });
                    popup.show();
                    ContactEditorFragment.this.initSimAccountType();
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_contact, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem doneMenu = menu.findItem(R.id.menu_done);
        MenuItem splitMenu = menu.findItem(R.id.menu_split);
        MenuItem joinMenu = menu.findItem(R.id.menu_join);
        MenuItem helpMenu = menu.findItem(R.id.menu_help);
        MenuItem discardMenu = menu.findItem(R.id.menu_discard);
        MenuItem sendToVoiceMailMenu = menu.findItem(R.id.menu_send_to_voicemail);
        MenuItem ringToneMenu = menu.findItem(R.id.menu_set_ringtone);
        MenuItem deleteMenu = menu.findItem(R.id.menu_delete);
        deleteMenu.setShowAsAction(2);
        deleteMenu.setIcon(R.drawable.ic_delete_white_24dp);
        doneMenu.setVisible(false);
        discardMenu.setVisible((this.mState == null || this.mState.getFirstWritableRawContact(this.mContext) == null) ? false : true);
        if ("android.intent.action.INSERT".equals(this.mAction)) {
            HelpUtils.prepareHelpMenuItem(this.mContext, helpMenu, R.string.help_url_people_add);
            splitMenu.setVisible(false);
            joinMenu.setVisible(false);
            deleteMenu.setVisible(false);
        } else if ("android.intent.action.EDIT".equals(this.mAction)) {
            HelpUtils.prepareHelpMenuItem(this.mContext, helpMenu, R.string.help_url_people_edit);
            splitMenu.setVisible(this.mState.size() > 1 && !isEditingUserProfile());
            joinMenu.setVisible(!isEditingUserProfile());
            deleteMenu.setVisible(this.mDisableDeleteMenuOption ? false : true);
        } else {
            helpMenu.setVisible(false);
        }
        sendToVoiceMailMenu.setChecked(this.mSendToVoicemailState);
        sendToVoiceMailMenu.setVisible(this.mArePhoneOptionsChangable);
        ringToneMenu.setVisible(this.mArePhoneOptionsChangable);
        int size = menu.size();
        for (int i = 0; i < size; i++) {
            menu.getItem(i).setEnabled(this.mEnabled);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.menu_done:
                return save(0);
            case R.id.menu_split:
                return doSplitContactAction();
            case R.id.menu_join:
                return doJoinContactAction();
            case R.id.menu_discard:
                return revert();
            case R.id.menu_delete:
                if (this.mListener == null) {
                    return true;
                }
                this.mListener.onDeleteRequested(this.mLookupUri);
                return true;
            case R.id.menu_set_ringtone:
                doPickRingtone();
                return true;
            case R.id.menu_send_to_voicemail:
                this.mSendToVoicemailState = this.mSendToVoicemailState ? false : true;
                item.setChecked(this.mSendToVoicemailState);
                Intent intent = ContactSaveService.createSetSendToVoicemail(this.mContext, this.mLookupUri, this.mSendToVoicemailState);
                this.mContext.startService(intent);
                return true;
            default:
                return false;
        }
    }

    private boolean doSplitContactAction() {
        if (!hasValidState()) {
            return false;
        }
        SplitContactConfirmationDialogFragment dialog = new SplitContactConfirmationDialogFragment();
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), "SplitContactConfirmationDialog");
        return true;
    }

    private boolean doJoinContactAction() {
        if (!hasValidState()) {
            return false;
        }
        if (this.mState.size() == 1 && this.mState.get(0).isContactInsert() && !hasPendingChanges()) {
            Toast.makeText(this.mContext, R.string.toast_join_with_empty_contact, 1).show();
            return true;
        }
        return save(3);
    }

    private boolean hasValidState() {
        return this.mState.size() > 0;
    }

    private boolean hasPendingChanges() {
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(this.mContext);
        return RawContactModifier.hasChanges(this.mState, accountTypes);
    }

    public boolean save(int saveMode) {
        Log.d(TAG, " save()");
        if (!hasValidState() || this.mStatus != 1) {
            return false;
        }
        if (saveMode == 0 || saveMode == 2) {
            getLoaderManager().destroyLoader(1);
        }
        this.mStatus = 2;
        if (!hasPendingChanges()) {
            if (this.mLookupUri == null && saveMode == 1) {
                this.mStatus = 1;
                return true;
            }
            onSaveCompleted(false, saveMode, this.mLookupUri != null, this.mLookupUri);
            return true;
        }
        setEnabled(false);
        saveDefaultAccountIfNecessary();
        String accountType = this.mState.get(0).getValues().getAsString("account_type");
        if (accountType != null && accountType.equals("com.android.contact.sim")) {
            Intent intent = ContactSaveService.createSaveContactIntent(0, this.mContext, this.mState, this.mStateOld, "saveMode", saveMode, isEditingUserProfile(), (Class<? extends Activity>) ((Activity) this.mContext).getClass(), "saveCompleted", this.mUpdatedPhotos);
            this.mContext.startService(intent);
        } else if (accountType != null && accountType.equals("com.android.contact.sim2")) {
            Intent intent2 = ContactSaveService.createSaveContactIntent(1, this.mContext, this.mState, this.mStateOld, "saveMode", saveMode, isEditingUserProfile(), (Class<? extends Activity>) ((Activity) this.mContext).getClass(), "saveCompleted", this.mUpdatedPhotos);
            this.mContext.startService(intent2);
        } else {
            Intent intent3 = ContactSaveService.createSaveContactIntent(this.mContext, this.mState, null, "saveMode", saveMode, isEditingUserProfile(), ((Activity) this.mContext).getClass(), "saveCompleted", this.mUpdatedPhotos);
            this.mContext.startService(intent3);
        }
        this.mUpdatedPhotos = new Bundle();
        return true;
    }

    private void doPickRingtone() {
        Uri ringtoneUri;
        Intent intent = new Intent("android.intent.action.RINGTONE_PICKER");
        intent.putExtra("android.intent.extra.ringtone.SHOW_DEFAULT", true);
        intent.putExtra("android.intent.extra.ringtone.TYPE", 1);
        intent.putExtra("android.intent.extra.ringtone.SHOW_SILENT", true);
        if (this.mCustomRingtone != null) {
            ringtoneUri = Uri.parse(this.mCustomRingtone);
        } else {
            ringtoneUri = RingtoneManager.getDefaultUri(1);
        }
        intent.putExtra("android.intent.extra.ringtone.EXISTING_URI", ringtoneUri);
        try {
            startActivityForResult(intent, 2);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this.mContext, R.string.missing_app, 0).show();
        }
    }

    private void handleRingtonePicked(Uri pickedUri) {
        if (pickedUri == null || RingtoneManager.isDefault(pickedUri)) {
            this.mCustomRingtone = null;
        } else {
            this.mCustomRingtone = pickedUri.toString();
        }
        Intent intent = ContactSaveService.createSetRingtone(this.mContext, this.mLookupUri, this.mCustomRingtone);
        this.mContext.startService(intent);
    }

    public static class CancelEditDialogFragment extends DialogFragment {
        public static void show(ContactEditorFragment fragment) {
            CancelEditDialogFragment dialog = new CancelEditDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            dialog.show(fragment.getFragmentManager(), "cancelEditor");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.cancel_confirmation_dialog_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int whichButton) {
                    ((ContactEditorFragment) CancelEditDialogFragment.this.getTargetFragment()).doRevertAction();
                }
            }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
            return dialog;
        }
    }

    private boolean revert() {
        if (this.mState.isEmpty() || !hasPendingChanges()) {
            doRevertAction();
            return true;
        }
        CancelEditDialogFragment.show(this);
        return true;
    }

    private void doRevertAction() {
        this.mStatus = 3;
        if (this.mListener != null) {
            this.mListener.onReverted();
        }
    }

    public void onJoinCompleted(Uri uri) {
        onSaveCompleted(false, 1, uri != null, uri);
    }

    public void onSaveCompleted(boolean hadChanges, int saveMode, boolean saveSucceeded, Uri contactLookupUri) {
        Intent resultIntent;
        Uri lookupUri;
        if (hadChanges) {
            if (saveSucceeded) {
                if (saveMode != 3) {
                    Toast.makeText(this.mContext, R.string.contactSavedToast, 0).show();
                }
            } else {
                Toast.makeText(this.mContext, R.string.contactSavedErrorToast, 1).show();
            }
        }
        switch (saveMode) {
            case 0:
            case 4:
                if (saveSucceeded && contactLookupUri != null) {
                    String requestAuthority = this.mLookupUri == null ? null : this.mLookupUri.getAuthority();
                    if ("contacts".equals(requestAuthority)) {
                        long contactId = ContentUris.parseId(ContactsContract.Contacts.lookupContact(this.mContext.getContentResolver(), contactLookupUri));
                        Uri legacyContentUri = Uri.parse("content://contacts/people");
                        Uri legacyUri = ContentUris.withAppendedId(legacyContentUri, contactId);
                        lookupUri = legacyUri;
                    } else {
                        lookupUri = contactLookupUri;
                    }
                    resultIntent = ContactsContract.QuickContact.composeQuickContactsIntent(getActivity(), (Rect) null, lookupUri, 4, null);
                    resultIntent.setFlags(67108864);
                } else {
                    resultIntent = null;
                }
                this.mStatus = 3;
                if (this.mListener != null) {
                    this.mListener.onSaveFinished(resultIntent);
                }
                break;
            case 1:
            case 3:
                if (saveSucceeded && contactLookupUri != null) {
                    if (saveMode == 3 && hasValidState()) {
                        showJoinAggregateActivity(contactLookupUri);
                    }
                    this.mState = new RawContactDeltaList();
                    load("android.intent.action.EDIT", contactLookupUri, null);
                    this.mStatus = 0;
                    getLoaderManager().restartLoader(1, null, this.mDataLoaderListener);
                } else {
                    this.mStatus = 1;
                    setEnabled(true);
                    bindEditors();
                }
                break;
            case 2:
                this.mStatus = 3;
                if (this.mListener != null) {
                    this.mListener.onContactSplit(contactLookupUri);
                } else {
                    Log.d(TAG, "No listener registered, can not call onSplitFinished");
                }
                break;
        }
    }

    private void showJoinAggregateActivity(Uri contactLookupUri) {
        if (contactLookupUri != null && isAdded()) {
            this.mContactIdForJoin = ContentUris.parseId(contactLookupUri);
            this.mContactWritableForJoin = isContactWritable();
            Intent intent = new Intent("com.android.contacts.action.JOIN_CONTACT");
            intent.putExtra("com.android.contacts.action.CONTACT_ID", this.mContactIdForJoin);
            startActivityForResult(intent, 0);
        }
    }

    private void joinAggregate(long contactId) {
        Intent intent = ContactSaveService.createJoinContactsIntent(this.mContext, this.mContactIdForJoin, contactId, this.mContactWritableForJoin, ContactEditorActivity.class, "joinCompleted");
        this.mContext.startService(intent);
    }

    private boolean isContactWritable() {
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(this.mContext);
        int size = this.mState.size();
        for (int i = 0; i < size; i++) {
            RawContactDelta entity = this.mState.get(i);
            AccountType type = entity.getAccountType(accountTypes);
            if (type.areContactsWritable()) {
                return true;
            }
        }
        return false;
    }

    private boolean isEditingUserProfile() {
        return this.mNewLocalProfile || this.mIsUserProfile;
    }

    private class EntityDeltaComparator implements Comparator<RawContactDelta> {
        private EntityDeltaComparator() {
        }

        @Override
        public int compare(RawContactDelta one, RawContactDelta two) {
            int value;
            int value2;
            if (!one.equals(two)) {
                AccountTypeManager accountTypes = AccountTypeManager.getInstance(ContactEditorFragment.this.mContext);
                String accountType1 = one.getValues().getAsString("account_type");
                String dataSet1 = one.getValues().getAsString("data_set");
                AccountType type1 = accountTypes.getAccountType(accountType1, dataSet1);
                String accountType2 = two.getValues().getAsString("account_type");
                String dataSet2 = two.getValues().getAsString("data_set");
                AccountType type2 = accountTypes.getAccountType(accountType2, dataSet2);
                if (!type1.areContactsWritable() && type2.areContactsWritable()) {
                    return 1;
                }
                if (type1.areContactsWritable() && !type2.areContactsWritable()) {
                    return -1;
                }
                boolean skipAccountTypeCheck = false;
                boolean isGoogleAccount1 = type1 instanceof GoogleAccountType;
                boolean isGoogleAccount2 = type2 instanceof GoogleAccountType;
                if (isGoogleAccount1 && !isGoogleAccount2) {
                    return -1;
                }
                if (!isGoogleAccount1 && isGoogleAccount2) {
                    return 1;
                }
                if (isGoogleAccount1 && isGoogleAccount2) {
                    skipAccountTypeCheck = true;
                }
                if (!skipAccountTypeCheck) {
                    if (type1.accountType != null && type2.accountType == null) {
                        return -1;
                    }
                    if (type1.accountType == null && type2.accountType != null) {
                        return 1;
                    }
                    if (type1.accountType == null || type2.accountType == null || (value2 = type1.accountType.compareTo(type2.accountType)) == 0) {
                        if (type1.dataSet != null && type2.dataSet == null) {
                            return -1;
                        }
                        if (type1.dataSet == null && type2.dataSet != null) {
                            return 1;
                        }
                        if (type1.dataSet != null && type2.dataSet != null && (value = type1.dataSet.compareTo(type2.dataSet)) != 0) {
                            return value;
                        }
                    } else {
                        return value2;
                    }
                }
                String oneAccount = one.getAccountName();
                if (oneAccount == null) {
                    oneAccount = "";
                }
                String twoAccount = two.getAccountName();
                if (twoAccount == null) {
                    twoAccount = "";
                }
                int value3 = oneAccount.compareTo(twoAccount);
                if (value3 == 0) {
                    Long oneId = one.getRawContactId();
                    Long twoId = two.getRawContactId();
                    if (oneId == null) {
                        return -1;
                    }
                    if (twoId == null) {
                        return 1;
                    }
                    return (int) (oneId.longValue() - twoId.longValue());
                }
                return value3;
            }
            return 0;
        }
    }

    protected long getContactId() {
        for (RawContactDelta rawContact : this.mState) {
            Long contactId = rawContact.getValues().getAsLong("contact_id");
            if (contactId != null) {
                return contactId.longValue();
            }
        }
        return 0L;
    }

    private void acquireAggregationSuggestions(Context context, RawContactEditorView rawContactEditor) {
        long rawContactId = rawContactEditor.getRawContactId();
        if (this.mAggregationSuggestionsRawContactId != rawContactId && this.mAggregationSuggestionView != null) {
            this.mAggregationSuggestionView.setVisibility(8);
            this.mAggregationSuggestionView = null;
            this.mAggregationSuggestionEngine.reset();
        }
        this.mAggregationSuggestionsRawContactId = rawContactId;
        if (this.mAggregationSuggestionEngine == null) {
            this.mAggregationSuggestionEngine = new AggregationSuggestionEngine(context);
            this.mAggregationSuggestionEngine.setListener(this);
            this.mAggregationSuggestionEngine.start();
        }
        this.mAggregationSuggestionEngine.setContactId(getContactId());
        LabeledEditorView nameEditor = rawContactEditor.getNameEditor();
        this.mAggregationSuggestionEngine.onNameChange(nameEditor.getValues());
    }

    @Override
    public void onAggregationSuggestionChange() {
        RawContactEditorView rawContactView;
        Activity activity = getActivity();
        if ((activity == null || !activity.isFinishing()) && isVisible() && !this.mState.isEmpty() && this.mStatus == 1) {
            UiClosables.closeQuietly(this.mAggregationSuggestionPopup);
            if (this.mAggregationSuggestionEngine.getSuggestedContactCount() != 0 && (rawContactView = (RawContactEditorView) getRawContactEditorView(this.mAggregationSuggestionsRawContactId)) != null) {
                View anchorView = rawContactView.findViewById(R.id.anchor_view);
                this.mAggregationSuggestionPopup = new ListPopupWindow(this.mContext, null);
                this.mAggregationSuggestionPopup.setAnchorView(anchorView);
                this.mAggregationSuggestionPopup.setWidth(anchorView.getWidth());
                this.mAggregationSuggestionPopup.setInputMethodMode(2);
                this.mAggregationSuggestionPopup.setAdapter(new AggregationSuggestionAdapter(getActivity(), this.mState.size() == 1 && this.mState.get(0).isContactInsert(), this, this.mAggregationSuggestionEngine.getSuggestions()));
                this.mAggregationSuggestionPopup.setOnItemClickListener(this.mAggregationSuggestionItemClickListener);
                this.mAggregationSuggestionPopup.show();
            }
        }
    }

    @Override
    public void onJoinAction(long contactId, List<Long> rawContactIdList) {
        long[] rawContactIds = new long[rawContactIdList.size()];
        for (int i = 0; i < rawContactIds.length; i++) {
            rawContactIds[i] = rawContactIdList.get(i).longValue();
        }
        JoinSuggestedContactDialogFragment dialog = new JoinSuggestedContactDialogFragment();
        Bundle args = new Bundle();
        args.putLongArray("rawContactIds", rawContactIds);
        dialog.setArguments(args);
        dialog.setTargetFragment(this, 0);
        try {
            dialog.show(getFragmentManager(), "join");
        } catch (Exception e) {
        }
    }

    public static class JoinSuggestedContactDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.aggregation_suggestion_join_dialog_message).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    ContactEditorFragment targetFragment = (ContactEditorFragment) JoinSuggestedContactDialogFragment.this.getTargetFragment();
                    long[] rawContactIds = JoinSuggestedContactDialogFragment.this.getArguments().getLongArray("rawContactIds");
                    targetFragment.doJoinSuggestedContact(rawContactIds);
                }
            }).setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null).create();
        }
    }

    protected void doJoinSuggestedContact(long[] rawContactIds) {
        if (hasValidState() && this.mStatus == 1) {
            this.mState.setJoinWithRawContacts(rawContactIds);
            save(1);
        }
    }

    @Override
    public void onEditAction(Uri contactLookupUri) {
        SuggestionEditConfirmationDialogFragment dialog = new SuggestionEditConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable("contactUri", contactLookupUri);
        dialog.setArguments(args);
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), "edit");
    }

    public static class SuggestionEditConfirmationDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.aggregation_suggestion_edit_dialog_message).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    ContactEditorFragment targetFragment = (ContactEditorFragment) SuggestionEditConfirmationDialogFragment.this.getTargetFragment();
                    Uri contactUri = (Uri) SuggestionEditConfirmationDialogFragment.this.getArguments().getParcelable("contactUri");
                    targetFragment.doEditSuggestedContact(contactUri);
                }
            }).setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null).create();
        }
    }

    protected void doEditSuggestedContact(Uri contactUri) {
        if (this.mListener != null) {
            this.mStatus = 3;
            this.mListener.onEditOtherContactRequested(contactUri, this.mState.get(0).getContentValues());
        }
    }

    public void setAggregationSuggestionViewEnabled(boolean enabled) {
        if (this.mAggregationSuggestionView != null) {
            LinearLayout itemList = (LinearLayout) this.mAggregationSuggestionView.findViewById(R.id.aggregation_suggestions);
            int count = itemList.getChildCount();
            for (int i = 0; i < count; i++) {
                itemList.getChildAt(i).setEnabled(enabled);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("uri", this.mLookupUri);
        outState.putString("action", this.mAction);
        if (hasValidState()) {
            outState.putParcelable("state", this.mState);
        }
        outState.putLong("photorequester", this.mRawContactIdRequestingPhoto);
        outState.putParcelable("viewidgenerator", this.mViewIdGenerator);
        outState.putParcelable("currentphotouri", this.mCurrentPhotoUri);
        outState.putLong("contactidforjoin", this.mContactIdForJoin);
        outState.putBoolean("contactwritableforjoin", this.mContactWritableForJoin);
        outState.putLong("showJoinSuggestions", this.mAggregationSuggestionsRawContactId);
        outState.putBoolean("enabled", this.mEnabled);
        outState.putBoolean("newLocalProfile", this.mNewLocalProfile);
        outState.putBoolean("disableDeleteMenuOption", this.mDisableDeleteMenuOption);
        outState.putBoolean("isUserProfile", this.mIsUserProfile);
        outState.putInt("status", this.mStatus);
        outState.putParcelable("updatedPhotos", this.mUpdatedPhotos);
        outState.putBoolean("hasNewContact", this.mHasNewContact);
        outState.putBoolean("isEdit", this.mIsEdit);
        outState.putBoolean("newContactDataReady", this.mNewContactDataReady);
        outState.putBoolean("existingContactDataReady", this.mExistingContactDataReady);
        outState.putParcelableArrayList("rawContacts", this.mRawContacts == null ? Lists.newArrayList() : Lists.newArrayList(this.mRawContacts));
        outState.putBoolean("sendToVoicemailState", this.mSendToVoicemailState);
        outState.putString("customRingtone", this.mCustomRingtone);
        outState.putBoolean("arePhoneOptionsChangable", this.mArePhoneOptionsChangable);
        outState.putSerializable("expandedEditors", this.mExpandedEditors);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        AccountWithDataSet account;
        if (this.mStatus == 4) {
            this.mStatus = 1;
        }
        if (this.mCurrentPhotoHandler == null || !this.mCurrentPhotoHandler.handlePhotoActivityResult(requestCode, resultCode, data)) {
            switch (requestCode) {
                case 0:
                    if (resultCode == -1 && data != null) {
                        long contactId = ContentUris.parseId(data.getData());
                        joinAggregate(contactId);
                        break;
                    }
                    break;
                case 1:
                    if (resultCode != -1) {
                        this.mListener.onReverted();
                    } else if (data != null && (account = (AccountWithDataSet) data.getParcelableExtra("com.android.contacts.extra.ACCOUNT")) != null) {
                        createContact(account);
                    } else {
                        createContact();
                    }
                    break;
                case 2:
                    if (data != null) {
                        Uri pickedUri = (Uri) data.getParcelableExtra("android.intent.extra.ringtone.PICKED_URI");
                        handleRingtonePicked(pickedUri);
                    }
                    break;
            }
        }
    }

    private void setPhoto(long rawContact, Bitmap photo, Uri photoUri) {
        BaseRawContactEditorView requestingEditor = getRawContactEditorView(rawContact);
        if (photo == null || photo.getHeight() < 0 || photo.getWidth() < 0) {
            Log.w(TAG, "Invalid bitmap passed to setPhoto()");
        }
        if (requestingEditor != null) {
            requestingEditor.setPhotoEntry(photo);
            for (int i = 0; i < this.mContent.getChildCount(); i++) {
                View childView = this.mContent.getChildAt(i);
                if ((childView instanceof BaseRawContactEditorView) && childView != requestingEditor) {
                    BaseRawContactEditorView rawContactEditor = (BaseRawContactEditorView) childView;
                    rawContactEditor.getPhotoEditor().setSuperPrimary(false);
                }
            }
        } else {
            Log.w(TAG, "The contact that requested the photo is no longer present.");
        }
        this.mUpdatedPhotos.putParcelable(String.valueOf(rawContact), photoUri);
    }

    public BaseRawContactEditorView getRawContactEditorView(long rawContactId) {
        for (int i = 0; i < this.mContent.getChildCount(); i++) {
            View childView = this.mContent.getChildAt(i);
            if (childView instanceof BaseRawContactEditorView) {
                BaseRawContactEditorView editor = (BaseRawContactEditorView) childView;
                if (editor.getRawContactId() == rawContactId) {
                    return editor;
                }
            }
        }
        return null;
    }

    private boolean hasMoreThanOnePhoto() {
        int countWithPicture = 0;
        int numEntities = this.mState.size();
        for (int i = 0; i < numEntities; i++) {
            RawContactDelta entity = this.mState.get(i);
            if (entity.isVisible()) {
                ValuesDelta primary = entity.getPrimaryEntry("vnd.android.cursor.item/photo");
                if (primary != null && primary.getPhoto() != null) {
                    countWithPicture++;
                } else {
                    long rawContactId = entity.getRawContactId().longValue();
                    Uri uri = (Uri) this.mUpdatedPhotos.getParcelable(String.valueOf(rawContactId));
                    if (uri != null) {
                        try {
                            this.mContext.getContentResolver().openInputStream(uri);
                            countWithPicture++;
                        } catch (FileNotFoundException e) {
                        }
                    }
                }
                if (countWithPicture > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onSplitContactConfirmed() {
        if (this.mState.isEmpty()) {
            Log.e(TAG, "mState became null during the user's confirming split action. Cannot perform the save action.");
        } else {
            this.mState.markRawContactsForSplitting();
            save(2);
        }
    }

    private final class PhotoHandler extends PhotoSelectionHandler {
        private final BaseRawContactEditorView mEditor;
        private final PhotoSelectionHandler.PhotoActionListener mPhotoEditorListener;
        final long mRawContactId;

        public PhotoHandler(Context context, BaseRawContactEditorView editor, int photoMode, RawContactDeltaList state) {
            super(context, editor.getPhotoEditor().getChangeAnchorView(), photoMode, false, state);
            this.mEditor = editor;
            this.mRawContactId = editor.getRawContactId();
            this.mPhotoEditorListener = new PhotoEditorListener();
        }

        @Override
        public PhotoSelectionHandler.PhotoActionListener getListener() {
            return this.mPhotoEditorListener;
        }

        @Override
        public void startPhotoActivity(Intent intent, int requestCode, Uri photoUri) {
            ContactEditorFragment.this.mRawContactIdRequestingPhoto = this.mEditor.getRawContactId();
            ContactEditorFragment.this.mCurrentPhotoHandler = this;
            ContactEditorFragment.this.mStatus = 4;
            ContactEditorFragment.this.mCurrentPhotoUri = photoUri;
            ContactEditorFragment.this.startActivityForResult(intent, requestCode);
        }

        private final class PhotoEditorListener extends PhotoSelectionHandler.PhotoActionListener implements Editor.EditorListener {
            private PhotoEditorListener() {
                super();
            }

            @Override
            public void onRequest(int request) {
                if (ContactEditorFragment.this.hasValidState()) {
                    if (request == 1) {
                        PhotoHandler.this.onClick(PhotoHandler.this.mEditor.getPhotoEditor());
                    }
                    if (request == 0) {
                        useAsPrimaryChosen();
                    }
                }
            }

            @Override
            public void onDeleteRequested(Editor removedEditor) {
            }

            public void useAsPrimaryChosen() {
                int count = ContactEditorFragment.this.mContent.getChildCount();
                for (int i = 0; i < count; i++) {
                    View childView = ContactEditorFragment.this.mContent.getChildAt(i);
                    if (childView instanceof BaseRawContactEditorView) {
                        BaseRawContactEditorView editor = (BaseRawContactEditorView) childView;
                        PhotoEditorView photoEditor = editor.getPhotoEditor();
                        photoEditor.setSuperPrimary(editor == PhotoHandler.this.mEditor);
                    }
                }
                ContactEditorFragment.this.bindEditors();
            }

            @Override
            public void onRemovePictureChosen() {
                PhotoHandler.this.mEditor.setPhotoEntry(null);
                ContactEditorFragment.this.mUpdatedPhotos.remove(String.valueOf(PhotoHandler.this.mRawContactId));
                ContactEditorFragment.this.bindEditors();
            }

            @Override
            public void onPhotoSelected(Uri uri) throws FileNotFoundException {
                Bitmap bitmap = ContactPhotoUtils.getBitmapFromUri(PhotoHandler.this.mContext, uri);
                ContactEditorFragment.this.setPhoto(PhotoHandler.this.mRawContactId, bitmap, uri);
                ContactEditorFragment.this.mCurrentPhotoHandler = null;
                ContactEditorFragment.this.bindEditors();
            }

            @Override
            public Uri getCurrentPhotoUri() {
                return ContactEditorFragment.this.mCurrentPhotoUri;
            }

            @Override
            public void onPhotoSelectionDismissed() {
            }
        }
    }
}
