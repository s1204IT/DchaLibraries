package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.Toast;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.common.list.PhoneNumberPickerFragment;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.EmailAddressPickerFragment;
import com.android.contacts.list.JoinContactListFragment;
import com.android.contacts.list.LegacyPhoneNumberPickerFragment;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.list.OnEmailAddressPickerActionListener;
import com.android.contacts.list.OnPostalAddressPickerActionListener;
import com.android.contacts.list.PostalAddressPickerFragment;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Set;

public class ContactSelectionActivity extends ContactsActivity implements View.OnClickListener, View.OnCreateContextMenuListener, View.OnFocusChangeListener, SearchView.OnCloseListener, SearchView.OnQueryTextListener {
    private int mActionCode = -1;
    private ContactsIntentResolver mIntentResolver = new ContactsIntentResolver(this);
    private boolean mIsSearchMode;
    private boolean mIsSearchSupported;
    protected ContactEntryListFragment<?> mListFragment;
    private ContactsRequest mRequest;
    private SearchView mSearchView;
    private View mSearchViewContainer;

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactEntryListFragment) {
            this.mListFragment = (ContactEntryListFragment) fragment;
            setupActionListener();
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState != null) {
            this.mActionCode = savedState.getInt("actionCode");
            this.mIsSearchMode = savedState.getBoolean("searchMode");
        }
        this.mRequest = this.mIntentResolver.resolveIntent(getIntent());
        if (!this.mRequest.isValid()) {
            setResult(0);
            finish();
            return;
        }
        Intent redirect = this.mRequest.getRedirectIntent();
        if (redirect != null) {
            startActivity(redirect);
            finish();
            return;
        }
        configureActivityTitle();
        setContentView(R.layout.contact_picker);
        if (this.mActionCode != this.mRequest.getActionCode()) {
            this.mActionCode = this.mRequest.getActionCode();
            configureListFragment();
        }
        prepareSearchViewAndActionBar();
    }

    private void prepareSearchViewAndActionBar() {
        ActionBar actionBar = getActionBar();
        this.mSearchViewContainer = LayoutInflater.from(actionBar.getThemedContext()).inflate(R.layout.custom_action_bar, (ViewGroup) null);
        this.mSearchView = (SearchView) this.mSearchViewContainer.findViewById(R.id.search_view);
        if (this.mRequest.getActionCode() == 100 || this.mRequest.isLegacyCompatibilityMode()) {
            this.mSearchView.setVisibility(8);
            if (actionBar != null) {
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
            }
            this.mIsSearchSupported = false;
            configureSearchMode();
            return;
        }
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        this.mSearchView.setIconifiedByDefault(true);
        this.mSearchView.setQueryHint(getString(R.string.hint_findContacts));
        this.mSearchView.setIconified(false);
        this.mSearchView.setFocusable(true);
        this.mSearchView.setOnQueryTextListener(this);
        this.mSearchView.setOnCloseListener(this);
        this.mSearchView.setOnQueryTextFocusChangeListener(this);
        actionBar.setCustomView(this.mSearchViewContainer, new ActionBar.LayoutParams(-1, -2));
        actionBar.setDisplayShowCustomEnabled(true);
        this.mIsSearchSupported = true;
        configureSearchMode();
    }

    private void configureSearchMode() {
        ActionBar actionBar = getActionBar();
        if (this.mIsSearchMode) {
            actionBar.setDisplayShowTitleEnabled(false);
            this.mSearchViewContainer.setVisibility(0);
            this.mSearchView.requestFocus();
        } else {
            actionBar.setDisplayShowTitleEnabled(true);
            this.mSearchViewContainer.setVisibility(8);
            this.mSearchView.setQuery(null, true);
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(0);
                onBackPressed();
                return true;
            case R.id.menu_search:
                this.mIsSearchMode = this.mIsSearchMode ? false : true;
                configureSearchMode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("actionCode", this.mActionCode);
        outState.putBoolean("searchMode", this.mIsSearchMode);
    }

    private void configureActivityTitle() {
        if (!TextUtils.isEmpty(this.mRequest.getActivityTitle())) {
            setTitle(this.mRequest.getActivityTitle());
        }
        int actionCode = this.mRequest.getActionCode();
        switch (actionCode) {
            case 60:
                setTitle(R.string.contactPickerActivityTitle);
                break;
            case 70:
                setTitle(R.string.contactPickerActivityTitle);
                break;
            case 80:
                setTitle(R.string.contactInsertOrEditActivityTitle);
                break;
            case 90:
                setTitle(R.string.contactPickerActivityTitle);
                break;
            case 100:
                setTitle(R.string.contactPickerActivityTitle);
                break;
            case 105:
                setTitle(R.string.contactPickerActivityTitle);
                break;
            case 110:
                setTitle(R.string.shortcutActivityTitle);
                break;
            case 120:
                setTitle(R.string.callShortcutActivityTitle);
                break;
            case 130:
                setTitle(R.string.messageShortcutActivityTitle);
                break;
            case 150:
                setTitle(R.string.titleJoinContactDataWith);
                break;
        }
    }

    public void configureListFragment() {
        ContactPickerFragment fragment;
        switch (this.mActionCode) {
            case 10:
            case 60:
                ArrayList<ContactListFilter> accountFilters = getIntent().getParcelableArrayListExtra("CONTACT_LIST_FILTER");
                if (accountFilters == null || accountFilters.isEmpty()) {
                    fragment = new ContactPickerFragment();
                } else {
                    fragment = new ContactPickerFragment(accountFilters);
                }
                fragment.setIncludeProfile(this.mRequest.shouldIncludeProfile());
                this.mListFragment = fragment;
                break;
            case 70:
                ContactPickerFragment fragment2 = new ContactPickerFragment();
                fragment2.setCreateContactEnabled(this.mRequest.isSearchMode() ? false : true);
                this.mListFragment = fragment2;
                break;
            case 80:
                ContactPickerFragment fragment3 = new ContactPickerFragment();
                fragment3.setEditMode(true);
                fragment3.setDirectorySearchMode(0);
                fragment3.setCreateContactEnabled(this.mRequest.isSearchMode() ? false : true);
                this.mListFragment = fragment3;
                break;
            case 90:
                this.mListFragment = getPhoneNumberPickerFragment(this.mRequest);
                break;
            case 100:
                this.mListFragment = new PostalAddressPickerFragment();
                break;
            case 105:
                this.mListFragment = new EmailAddressPickerFragment();
                break;
            case 110:
                ContactPickerFragment fragment4 = new ContactPickerFragment();
                fragment4.setShortcutRequested(true);
                this.mListFragment = fragment4;
                break;
            case 120:
                PhoneNumberPickerFragment fragment5 = getPhoneNumberPickerFragment(this.mRequest);
                fragment5.setShortcutAction("android.intent.action.CALL");
                this.mListFragment = fragment5;
                break;
            case 130:
                PhoneNumberPickerFragment fragment6 = getPhoneNumberPickerFragment(this.mRequest);
                fragment6.setShortcutAction("android.intent.action.SENDTO");
                this.mListFragment = fragment6;
                break;
            case 150:
                JoinContactListFragment joinFragment = new JoinContactListFragment();
                joinFragment.setTargetContactId(getTargetContactId());
                this.mListFragment = joinFragment;
                break;
            default:
                throw new IllegalStateException("Invalid action code: " + this.mActionCode);
        }
        this.mListFragment.setLegacyCompatibilityMode(this.mRequest.isLegacyCompatibilityMode());
        this.mListFragment.setDirectoryResultLimit(20);
        getFragmentManager().beginTransaction().replace(R.id.list_container, this.mListFragment).commitAllowingStateLoss();
    }

    private PhoneNumberPickerFragment getPhoneNumberPickerFragment(ContactsRequest request) {
        return this.mRequest.isLegacyCompatibilityMode() ? new LegacyPhoneNumberPickerFragment() : new PhoneNumberPickerFragment();
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
        } else if (this.mListFragment instanceof EmailAddressPickerFragment) {
            ((EmailAddressPickerFragment) this.mListFragment).setOnEmailAddressPickerActionListener(new EmailAddressPickerActionListener());
        } else {
            if (this.mListFragment instanceof JoinContactListFragment) {
                ((JoinContactListFragment) this.mListFragment).setOnContactPickerActionListener(new JoinContactActionListener());
                return;
            }
            throw new IllegalStateException("Unsupported list fragment type: " + this.mListFragment);
        }
    }

    private final class ContactPickerActionListener implements OnContactPickerActionListener {
        private ContactPickerActionListener() {
        }

        @Override
        public void onCreateNewContactAction() {
            ContactSelectionActivity.this.startCreateNewContactActivity();
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
            Bundle extras = ContactSelectionActivity.this.getIntent().getExtras();
            if (launchAddToContactDialog(extras)) {
                Intent intent = new Intent(ContactSelectionActivity.this, (Class<?>) ConfirmAddDetailActivity.class);
                intent.setData(contactLookupUri);
                if (extras != null) {
                    extras.remove("name");
                    intent.putExtras(extras);
                }
                ContactSelectionActivity.this.startActivityForResult(intent, 0);
                return;
            }
            ContactSelectionActivity.this.startActivityAndForwardResult(new Intent("android.intent.action.EDIT", contactLookupUri));
        }

        @Override
        public void onPickContactAction(Uri contactUri) {
            ContactSelectionActivity.this.returnPickerResult(contactUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            ContactSelectionActivity.this.returnPickerResult(intent);
        }

        private boolean launchAddToContactDialog(Bundle extras) {
            if (extras == null) {
                return false;
            }
            Set<String> intentExtraKeys = Sets.newHashSet();
            intentExtraKeys.addAll(extras.keySet());
            if (intentExtraKeys.contains("name")) {
                intentExtraKeys.remove("name");
            }
            int numIntentExtraKeys = intentExtraKeys.size();
            if (numIntentExtraKeys == 2) {
                boolean hasPhone = intentExtraKeys.contains("phone") && intentExtraKeys.contains("phone_type");
                boolean hasEmail = intentExtraKeys.contains("email") && intentExtraKeys.contains("email_type");
                return hasPhone || hasEmail;
            }
            if (numIntentExtraKeys == 1) {
                return intentExtraKeys.contains("phone") || intentExtraKeys.contains("email");
            }
            return false;
        }
    }

    private final class PhoneNumberPickerActionListener implements OnPhoneNumberPickerActionListener {
        private PhoneNumberPickerActionListener() {
        }

        @Override
        public void onPickPhoneNumberAction(Uri dataUri) {
            ContactSelectionActivity.this.returnPickerResult(dataUri);
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            Log.w("ContactSelectionActivity", "Unsupported call.");
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
        public void onPickContactAction(Uri contactUri) {
            Intent intent = new Intent((String) null, contactUri);
            ContactSelectionActivity.this.setResult(-1, intent);
            ContactSelectionActivity.this.finish();
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
        }

        @Override
        public void onCreateNewContactAction() {
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
        }
    }

    private final class PostalAddressPickerActionListener implements OnPostalAddressPickerActionListener {
        private PostalAddressPickerActionListener() {
        }

        @Override
        public void onPickPostalAddressAction(Uri dataUri) {
            ContactSelectionActivity.this.returnPickerResult(dataUri);
        }
    }

    private final class EmailAddressPickerActionListener implements OnEmailAddressPickerActionListener {
        private EmailAddressPickerActionListener() {
        }

        @Override
        public void onPickEmailAddressAction(Uri dataUri) {
            ContactSelectionActivity.this.returnPickerResult(dataUri);
        }
    }

    public void startActivityAndForwardResult(Intent intent) {
        intent.setFlags(33554432);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("ContactSelectionActivity", "startActivity() failed: " + e);
            Toast.makeText(this, R.string.missing_app, 0).show();
        }
        finish();
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        this.mListFragment.setQueryString(newText, true);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onClose() {
        if (!TextUtils.isEmpty(this.mSearchView.getQuery())) {
            this.mSearchView.setQuery(null, true);
        }
        return true;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        switch (view.getId()) {
            case R.id.search_view:
                if (hasFocus) {
                    showInputMethod(this.mSearchView.findFocus());
                }
                break;
        }
    }

    public void returnPickerResult(Uri data) {
        Intent intent = new Intent();
        intent.setData(data);
        returnPickerResult(intent);
    }

    public void returnPickerResult(Intent intent) {
        intent.setFlags(1);
        setResult(-1, intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.floating_action_button:
                startCreateNewContactActivity();
                break;
        }
    }

    private long getTargetContactId() {
        Intent intent = getIntent();
        long targetContactId = intent.getLongExtra("com.android.contacts.action.CONTACT_ID", -1L);
        if (targetContactId == -1) {
            Log.e("ContactSelectionActivity", "Intent " + intent.getAction() + " is missing required extra: com.android.contacts.action.CONTACT_ID");
            setResult(0);
            finish();
            return -1L;
        }
        return targetContactId;
    }

    private void startCreateNewContactActivity() {
        Intent intent = new Intent("android.intent.action.INSERT", ContactsContract.Contacts.CONTENT_URI);
        intent.putExtra("finishActivityOnSaveCompleted", true);
        startActivityAndForwardResult(intent);
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        if (imm != null && !imm.showSoftInput(view, 0)) {
            Log.w("ContactSelectionActivity", "Failed to show soft input method.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == -1) {
            if (data != null) {
                startActivity(data);
            }
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchItem.setVisible(!this.mIsSearchMode && this.mIsSearchSupported);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (this.mIsSearchMode) {
            this.mIsSearchMode = false;
            configureSearchMode();
        } else {
            super.onBackPressed();
        }
    }
}
