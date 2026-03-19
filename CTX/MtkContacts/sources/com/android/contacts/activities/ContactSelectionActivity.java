package com.android.contacts.activities;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.android.contacts.AppCompatContactsActivity;
import com.android.contacts.R;
import com.android.contacts.activities.ActionBarAdapter;
import com.android.contacts.editor.EditorIntents;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.EmailAddressPickerFragment;
import com.android.contacts.list.GroupMemberPickerFragment;
import com.android.contacts.list.JoinContactListFragment;
import com.android.contacts.list.LegacyPhoneNumberPickerFragment;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.list.MultiSelectEmailAddressesListFragment;
import com.android.contacts.list.MultiSelectPhoneNumbersListFragment;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.list.OnEmailAddressPickerActionListener;
import com.android.contacts.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.OnPostalAddressPickerActionListener;
import com.android.contacts.list.PhoneNumberPickerFragment;
import com.android.contacts.list.PostalAddressPickerFragment;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.ViewUtil;
import com.mediatek.contacts.activities.ActivitiesUtils;
import com.mediatek.contacts.util.Log;

public class ContactSelectionActivity extends AppCompatContactsActivity implements View.OnClickListener, View.OnCreateContextMenuListener, View.OnFocusChangeListener, ActionBarAdapter.Listener, MultiSelectContactsListFragment.OnCheckBoxListActionListener {
    private ActionBarAdapter mActionBarAdapter;
    private int mActionCode = -1;
    private ContactsIntentResolver mIntentResolver = new ContactsIntentResolver(this);
    private boolean mIsSearchMode;
    private boolean mIsSearchSupported;
    protected ContactEntryListFragment<?> mListFragment;
    private ContactsRequest mRequest;
    private Toolbar mToolbar;

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactEntryListFragment) {
            this.mListFragment = (ContactEntryListFragment) fragment;
            setupActionListener();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i("ContactSelection", "[onCreate]");
        RequestPermissionsActivity.startPermissionActivityIfNeeded(this);
        if (bundle != null) {
            this.mActionCode = bundle.getInt("actionCode");
            this.mIsSearchMode = bundle.getBoolean("searchMode");
        }
        this.mRequest = this.mIntentResolver.resolveIntent(getIntent());
        if (!this.mRequest.isValid()) {
            Log.w("ContactSelection", "[onCreate] mRequest is Invalid,finish activity...mRequest:" + this.mRequest);
            setResult(0);
            finish();
            return;
        }
        setContentView(R.layout.contact_picker);
        if (this.mActionCode != this.mRequest.getActionCode()) {
            this.mActionCode = this.mRequest.getActionCode();
            configureListFragment();
        }
        prepareSearchViewAndActionBar(bundle);
        configureActivityTitle();
    }

    @Override
    protected void onPause() {
        Log.i("ContactSelection", "[onPause] mListFragment = " + this.mListFragment + ", mActionBarAdapter=" + this.mActionBarAdapter);
        if (getMultiSelectListFragment() != null && this.mActionBarAdapter != null) {
            this.mActionBarAdapter.closeSelectMenu();
        }
        super.onPause();
    }

    public boolean isSelectionMode() {
        return this.mActionBarAdapter.isSelectionMode();
    }

    public boolean isSearchMode() {
        return this.mActionBarAdapter.isSearchMode();
    }

    private void prepareSearchViewAndActionBar(Bundle bundle) {
        this.mToolbar = (Toolbar) getView(R.id.toolbar);
        setSupportActionBar(this.mToolbar);
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());
        this.mActionBarAdapter = new ActionBarAdapter(this, this, getSupportActionBar(), this.mToolbar, R.string.enter_contact_name, this.mListFragment);
        this.mActionBarAdapter.setShowHomeIcon(true);
        this.mActionBarAdapter.setShowHomeAsUp(true);
        this.mActionBarAdapter.initialize(bundle, this.mRequest);
        this.mIsSearchSupported = (this.mRequest.getActionCode() == 100 || this.mRequest.getActionCode() == 106 || this.mRequest.getActionCode() == 107 || this.mRequest.isLegacyCompatibilityMode()) ? false : true;
        configureSearchMode();
    }

    private void configureSearchMode() {
        this.mActionBarAdapter.setSearchMode(this.mIsSearchMode);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            setResult(0);
            if (isResumed()) {
                onBackPressed();
            }
        } else if (itemId == R.id.menu_search) {
            this.mIsSearchMode = !this.mIsSearchMode;
            configureSearchMode();
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("actionCode", this.mActionCode);
        bundle.putBoolean("searchMode", this.mIsSearchMode);
        if (this.mActionBarAdapter != null) {
            this.mActionBarAdapter.onSaveInstanceState(bundle);
        }
    }

    private void configureActivityTitle() {
        if (!TextUtils.isEmpty(this.mRequest.getActivityTitle())) {
            getSupportActionBar().setTitle(this.mRequest.getActivityTitle());
            return;
        }
        int i = -1;
        int actionCode = this.mRequest.getActionCode();
        int i2 = R.string.contactPickerActivityTitle;
        switch (actionCode) {
            case 21:
                i = R.string.groupMemberPickerActivityTitle;
                i2 = i;
                break;
            case 60:
            case 70:
            case 90:
            case GroupUtil.RESULT_SEND_TO_SELECTION:
            case 105:
                break;
            case 80:
                i = R.string.contactInsertOrEditActivityTitle;
                i2 = i;
                break;
            case 106:
            case 107:
                i2 = R.string.pickerSelectContactsActivityTitle;
                break;
            case 110:
            case BaseAccountType.Weight.EVENT:
            case BaseAccountType.Weight.NOTE:
                i2 = R.string.shortcutActivityTitle;
                break;
            case BaseAccountType.Weight.GROUP_MEMBERSHIP:
                i = R.string.titleJoinContactDataWith;
                i2 = i;
                break;
            default:
                i2 = i;
                break;
        }
        if (i2 > 0) {
            getSupportActionBar().setTitle(i2);
        }
    }

    public void configureListFragment() {
        Log.d("ContactSelection", "[configureListFragment] mActionCode=" + this.mActionCode);
        switch (this.mActionCode) {
            case BaseAccountType.Weight.PHONE:
            case 60:
                ContactPickerFragment contactPickerFragment = new ContactPickerFragment();
                contactPickerFragment.setIncludeFavorites(this.mRequest.shouldIncludeFavorites());
                contactPickerFragment.setListType(10);
                this.mListFragment = contactPickerFragment;
                break;
            case 21:
                this.mListFragment = GroupMemberPickerFragment.newInstance(getIntent().getStringExtra("com.android.contacts.extra.GROUP_ACCOUNT_NAME"), getIntent().getStringExtra("com.android.contacts.extra.GROUP_ACCOUNT_TYPE"), getIntent().getStringExtra("com.android.contacts.extra.GROUP_ACCOUNT_DATA_SET"), getIntent().getStringArrayListExtra("com.android.contacts.extra.GROUP_CONTACT_IDS"), getIntent().getIntExtra("com.android.contacts.extra.GROUP_ACCOUNT_SUBID", -1));
                this.mListFragment.setListType(16);
                break;
            case 70:
                ContactPickerFragment contactPickerFragment2 = new ContactPickerFragment();
                contactPickerFragment2.setCreateContactEnabled(!this.mRequest.isSearchMode());
                contactPickerFragment2.setListType(10);
                this.mListFragment = contactPickerFragment2;
                break;
            case 80:
                ContactPickerFragment contactPickerFragment3 = new ContactPickerFragment();
                contactPickerFragment3.setEditMode(true);
                contactPickerFragment3.setDirectorySearchMode(0);
                contactPickerFragment3.setCreateContactEnabled(!this.mRequest.isSearchMode());
                contactPickerFragment3.setListType(10);
                this.mListFragment = contactPickerFragment3;
                break;
            case 90:
                PhoneNumberPickerFragment phoneNumberPickerFragment = getPhoneNumberPickerFragment(this.mRequest);
                phoneNumberPickerFragment.setListType(12);
                phoneNumberPickerFragment.setUseCallableUri(getIntent().getBooleanExtra("isCallableUri", false));
                this.mListFragment = phoneNumberPickerFragment;
                break;
            case GroupUtil.RESULT_SEND_TO_SELECTION:
                PostalAddressPickerFragment postalAddressPickerFragment = new PostalAddressPickerFragment();
                postalAddressPickerFragment.setListType(14);
                this.mListFragment = postalAddressPickerFragment;
                break;
            case 105:
                this.mListFragment = new EmailAddressPickerFragment();
                this.mListFragment.setListType(13);
                break;
            case 106:
                this.mListFragment = new MultiSelectEmailAddressesListFragment();
                this.mListFragment.setArguments(getIntent().getExtras());
                break;
            case 107:
                this.mListFragment = new MultiSelectPhoneNumbersListFragment();
                this.mListFragment.setArguments(getIntent().getExtras());
                break;
            case 110:
                ContactPickerFragment contactPickerFragment4 = new ContactPickerFragment();
                contactPickerFragment4.setShortcutRequested(true);
                contactPickerFragment4.setListType(11);
                this.mListFragment = contactPickerFragment4;
                break;
            case BaseAccountType.Weight.EVENT:
                PhoneNumberPickerFragment phoneNumberPickerFragment2 = getPhoneNumberPickerFragment(this.mRequest);
                phoneNumberPickerFragment2.setShortcutAction("android.intent.action.CALL");
                phoneNumberPickerFragment2.setListType(11);
                this.mListFragment = phoneNumberPickerFragment2;
                break;
            case BaseAccountType.Weight.NOTE:
                PhoneNumberPickerFragment phoneNumberPickerFragment3 = getPhoneNumberPickerFragment(this.mRequest);
                phoneNumberPickerFragment3.setShortcutAction("android.intent.action.SENDTO");
                phoneNumberPickerFragment3.setListType(11);
                this.mListFragment = phoneNumberPickerFragment3;
                break;
            case BaseAccountType.Weight.GROUP_MEMBERSHIP:
                JoinContactListFragment joinContactListFragment = new JoinContactListFragment();
                joinContactListFragment.setTargetContactId(getTargetContactId());
                joinContactListFragment.setListType(15);
                this.mListFragment = joinContactListFragment;
                break;
            default:
                throw new IllegalStateException("Invalid action code: " + this.mActionCode);
        }
        ActivitiesUtils.setPickerFragmentAccountType(this, this.mListFragment);
        this.mListFragment.setLegacyCompatibilityMode(this.mRequest.isLegacyCompatibilityMode());
        this.mListFragment.setDirectoryResultLimit(20);
        getFragmentManager().beginTransaction().replace(R.id.list_container, this.mListFragment).commitAllowingStateLoss();
    }

    private PhoneNumberPickerFragment getPhoneNumberPickerFragment(ContactsRequest contactsRequest) {
        if (this.mRequest.isLegacyCompatibilityMode()) {
            return new LegacyPhoneNumberPickerFragment();
        }
        return new PhoneNumberPickerFragment();
    }

    public void setupActionListener() {
        if (this.mListFragment instanceof ContactPickerFragment) {
            ((ContactPickerFragment) this.mListFragment).setOnContactPickerActionListener(new ContactPickerActionListener());
            return;
        }
        if (this.mListFragment instanceof PhoneNumberPickerFragment) {
            ((PhoneNumberPickerFragment) this.mListFragment).setOnPhoneNumberPickerActionListener(new PhoneNumberPickerActionListener());
            return;
        }
        if (this.mListFragment instanceof PostalAddressPickerFragment) {
            ((PostalAddressPickerFragment) this.mListFragment).setOnPostalAddressPickerActionListener(new PostalAddressPickerActionListener());
            return;
        }
        if (this.mListFragment instanceof EmailAddressPickerFragment) {
            ((EmailAddressPickerFragment) this.mListFragment).setOnEmailAddressPickerActionListener(new EmailAddressPickerActionListener());
            return;
        }
        if (this.mListFragment instanceof MultiSelectEmailAddressesListFragment) {
            ((MultiSelectEmailAddressesListFragment) this.mListFragment).setCheckBoxListListener(this);
            return;
        }
        if (this.mListFragment instanceof MultiSelectPhoneNumbersListFragment) {
            ((MultiSelectPhoneNumbersListFragment) this.mListFragment).setCheckBoxListListener(this);
            return;
        }
        if (this.mListFragment instanceof JoinContactListFragment) {
            ((JoinContactListFragment) this.mListFragment).setOnContactPickerActionListener(new JoinContactActionListener());
            return;
        }
        if (this.mListFragment instanceof GroupMemberPickerFragment) {
            ((GroupMemberPickerFragment) this.mListFragment).setListener(new GroupMemberPickerListener());
            getMultiSelectListFragment().setCheckBoxListListener(this);
        } else {
            throw new IllegalStateException("Unsupported list fragment type: " + this.mListFragment);
        }
    }

    private MultiSelectContactsListFragment getMultiSelectListFragment() {
        if (this.mListFragment instanceof MultiSelectContactsListFragment) {
            return (MultiSelectContactsListFragment) this.mListFragment;
        }
        return null;
    }

    @Override
    public void onAction(int i) {
        Log.d("ContactSelection", "[onAction]action = " + i);
        switch (i) {
            case 0:
                this.mListFragment.setQueryString(this.mActionBarAdapter.getQueryString(), false);
                break;
            case 1:
                this.mIsSearchMode = true;
                if (getMultiSelectListFragment() != null) {
                    getMultiSelectListFragment().displayCheckBoxes(false);
                }
                configureSearchMode();
                break;
            case 2:
                if (getMultiSelectListFragment() != null) {
                    getMultiSelectListFragment().displayCheckBoxes(true);
                }
                invalidateOptionsMenu();
                break;
            case 3:
                this.mListFragment.setQueryString("", false);
                this.mActionBarAdapter.setSearchMode(false);
                if (getMultiSelectListFragment() != null) {
                    getMultiSelectListFragment().displayCheckBoxes(false);
                }
                invalidateOptionsMenu();
                break;
        }
    }

    @Override
    public void onUpButtonPressed() {
        onBackPressed();
    }

    @Override
    public void onStartDisplayingCheckBoxes() {
        this.mActionBarAdapter.setSelectionMode(true);
    }

    @Override
    public void onSelectedContactIdsChanged() {
        if (this.mListFragment instanceof MultiSelectContactsListFragment) {
            int size = getMultiSelectListFragment().getSelectedContactIds().size();
            this.mActionBarAdapter.setSelectionCount(size);
            updateAddContactsButton(size);
            invalidateOptionsMenu();
        }
    }

    private void updateAddContactsButton(int i) {
        TextView textView = (TextView) this.mActionBarAdapter.getSelectionContainer().findViewById(R.id.add_contacts);
        if (i > 0) {
            textView.setVisibility(0);
            textView.setAllCaps(true);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ContactSelectionActivity.this.returnSelectedContacts(ContactSelectionActivity.this.getMultiSelectListFragment().getSelectedContactIdsArray());
                }
            });
            return;
        }
        textView.setVisibility(8);
    }

    @Override
    public void onStopDisplayingCheckBoxes() {
        this.mActionBarAdapter.setSelectionMode(false);
    }

    private final class ContactPickerActionListener implements OnContactPickerActionListener {
        private ContactPickerActionListener() {
        }

        @Override
        public void onCreateNewContactAction() {
            ContactSelectionActivity.this.startCreateNewContactActivity();
        }

        @Override
        public void onEditContactAction(Uri uri) {
            ContactSelectionActivity.this.startActivityAndForwardResult(EditorIntents.createEditContactIntent(ContactSelectionActivity.this, uri, null, -1L));
        }

        @Override
        public void onPickContactAction(Uri uri) {
            ContactSelectionActivity.this.returnPickerResult(uri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            ContactSelectionActivity.this.returnPickerResult(intent);
        }
    }

    private final class PhoneNumberPickerActionListener implements OnPhoneNumberPickerActionListener {
        private PhoneNumberPickerActionListener() {
        }

        @Override
        public void onPickDataUri(Uri uri, boolean z, int i) {
            ContactSelectionActivity.this.returnPickerResult(uri);
        }

        @Override
        public void onPickPhoneNumber(String str, boolean z, int i) {
            Log.w("ContactSelection", "Unsupported call.");
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            ContactSelectionActivity.this.returnPickerResult(intent);
        }

        @Override
        public void onHomeInActionBarSelected() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class JoinContactActionListener implements OnContactPickerActionListener {
        private JoinContactActionListener() {
        }

        @Override
        public void onPickContactAction(Uri uri) {
            ContactSelectionActivity.this.setResult(-1, new Intent((String) null, uri));
            ContactSelectionActivity.this.finish();
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
        }

        @Override
        public void onCreateNewContactAction() {
        }

        @Override
        public void onEditContactAction(Uri uri) {
        }
    }

    private final class GroupMemberPickerListener implements GroupMemberPickerFragment.Listener {
        private GroupMemberPickerListener() {
        }

        @Override
        public void onGroupMemberClicked(long j) {
            Log.d("ContactSelection", "[onGroupMemberClicked] contactId=" + j);
            Intent intent = new Intent();
            intent.putExtra("com.android.contacts.action.CONTACT_ID", j);
            ContactSelectionActivity.this.returnPickerResult(intent);
        }

        @Override
        public void onSelectGroupMembers() {
            ContactSelectionActivity.this.mActionBarAdapter.setSelectionMode(true);
        }
    }

    private void returnSelectedContacts(long[] jArr) {
        Intent intent = new Intent();
        intent.putExtra("com.android.contacts.action.CONTACT_IDS", jArr);
        returnPickerResult(intent);
    }

    private final class PostalAddressPickerActionListener implements OnPostalAddressPickerActionListener {
        private PostalAddressPickerActionListener() {
        }

        @Override
        public void onPickPostalAddressAction(Uri uri) {
            ContactSelectionActivity.this.returnPickerResult(uri);
        }
    }

    private final class EmailAddressPickerActionListener implements OnEmailAddressPickerActionListener {
        private EmailAddressPickerActionListener() {
        }

        @Override
        public void onPickEmailAddressAction(Uri uri) {
            ContactSelectionActivity.this.returnPickerResult(uri);
        }
    }

    public void startActivityAndForwardResult(Intent intent) {
        intent.setFlags(33554432);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        try {
            ImplicitIntentsUtil.startActivityInApp(this, intent);
        } catch (ActivityNotFoundException e) {
            Log.e("ContactSelection", "startActivity() failed: " + e);
            Toast.makeText(this, R.string.missing_app, 0).show();
        }
        finish();
    }

    @Override
    public void onFocusChange(View view, boolean z) {
        if (view.getId() == R.id.search_view && z) {
            this.mActionBarAdapter.setFocusOnSearchView();
        }
    }

    public void returnPickerResult(Uri uri) {
        Intent intent = new Intent();
        intent.setData(uri);
        returnPickerResult(intent);
    }

    public void returnPickerResult(Intent intent) {
        intent.setFlags(1);
        setResult(-1, intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.floating_action_button) {
            startCreateNewContactActivity();
        }
    }

    private long getTargetContactId() {
        Intent intent = getIntent();
        long longExtra = intent.getLongExtra("com.android.contacts.action.CONTACT_ID", -1L);
        if (longExtra == -1) {
            Log.e("ContactSelection", "Intent " + intent.getAction() + " is missing required extra: com.android.contacts.action.CONTACT_ID");
            setResult(0);
            finish();
            return -1L;
        }
        return longExtra;
    }

    private void startCreateNewContactActivity() {
        Intent intent = new Intent("android.intent.action.INSERT", ContactsContract.Contacts.CONTENT_URI);
        intent.putExtra("finishActivityOnSaveCompleted", true);
        intent.putExtra("account_type", getIntent().getIntExtra("account_type", 0));
        startActivityAndForwardResult(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem menuItemFindItem = menu.findItem(R.id.menu_search);
        menuItemFindItem.setVisible(!this.mIsSearchMode && this.mIsSearchSupported);
        Drawable icon = menuItemFindItem.getIcon();
        if (icon != null) {
            icon.mutate().setColorFilter(ContextCompat.getColor(this, R.color.actionbar_icon_color), PorterDuff.Mode.SRC_ATOP);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!isSafeToCommitTransactions()) {
            return;
        }
        if (isSelectionMode()) {
            this.mActionBarAdapter.setSelectionMode(false);
            if (getMultiSelectListFragment() != null) {
                getMultiSelectListFragment().displayCheckBoxes(false);
                return;
            }
            return;
        }
        if (this.mIsSearchMode) {
            this.mIsSearchMode = false;
            configureSearchMode();
        } else {
            super.onBackPressed();
        }
    }
}
