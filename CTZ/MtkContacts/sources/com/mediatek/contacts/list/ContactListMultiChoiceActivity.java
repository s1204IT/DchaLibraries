package com.mediatek.contacts.list;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.SearchView;
import com.android.contacts.AppCompatContactsActivity;
import com.android.contacts.R;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.list.ContactsRequest;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.list.DropMenu;
import com.mediatek.contacts.util.Log;

public class ContactListMultiChoiceActivity extends AppCompatContactsActivity implements View.OnClickListener, View.OnCreateContextMenuListener, View.OnFocusChangeListener, SearchView.OnCloseListener, SearchView.OnQueryTextListener {
    private ContactsIntentResolverEx mIntentResolverEx;
    protected AbstractPickerFragment mListFragment;
    private ContactsRequest mRequest;
    private SearchView mSearchView;
    private DropMenu.DropDownMenu mSelectionMenu;
    private int mActionCode = -1;
    private boolean mIsSelectedAll = true;
    private boolean mIsSelectedNone = true;
    private boolean mIsSearchMode = false;
    private int mNumberBalance = 100;

    private enum SelectionMode {
        SearchMode,
        ListMode
    }

    public ContactListMultiChoiceActivity() {
        Log.i("ContactListMultiChoiceActivity", "[ContactListMultiChoiceActivity]new.");
        this.mIntentResolverEx = new ContactsIntentResolverEx(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof AbstractPickerFragment) {
            this.mListFragment = (AbstractPickerFragment) fragment;
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            Log.i("ContactListMultiChoiceActivity", "[onCreate]startPermissionActivity,return.");
            return;
        }
        Intent intent = getIntent();
        Log.i("ContactListMultiChoiceActivity", "[onCreate]...");
        if (intent != null) {
            this.mNumberBalance = intent.getIntExtra("NUMBER_BALANCE", 100);
            Log.i("ContactListMultiChoiceActivity", "[onCreate]mNumberBalance from intent = " + this.mNumberBalance);
        }
        if (bundle != null) {
            this.mActionCode = bundle.getInt("actionCode");
            this.mNumberBalance = bundle.getInt("NUMBER_BALANCE");
            Log.i("ContactListMultiChoiceActivity", "[onCreate]mNumberBalance from savedState = " + this.mNumberBalance);
        }
        this.mRequest = this.mIntentResolverEx.resolveIntent(getIntent());
        if (!this.mRequest.isValid()) {
            Log.w("ContactListMultiChoiceActivity", "[onCreate]Request is invalid!");
            setResult(0);
            finish();
        } else {
            setContentView(R.layout.contact_picker);
            findViewById(R.id.toolbar_parent).setVisibility(8);
            configureListFragment();
            if (this.mSearchView != null) {
                this.mSearchView.setVisibility(8);
            }
            showActionBar(SelectionMode.ListMode);
        }
    }

    @Override
    protected void onDestroy() {
        if (this.mSelectionMenu != null && this.mSelectionMenu.isShown()) {
            this.mSelectionMenu.dismiss();
        }
        super.onDestroy();
        Log.i("ContactListMultiChoiceActivity", "[onDestroy]");
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        Log.i("ContactListMultiChoiceActivity", "[onSaveInstanceState]mActionCode = " + this.mActionCode + ",mNumberBalance = " + this.mNumberBalance);
        bundle.putInt("actionCode", this.mActionCode);
        bundle.putInt("NUMBER_BALANCE", this.mNumberBalance);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        ExtensionManager.getInstance();
        ExtensionManager.getContactsPickerExtension().addSearchMenu(this, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        Log.i("ContactListMultiChoiceActivity", "[onClick]v= " + view);
        int id = view.getId();
        if (id == R.id.menu_option) {
            Log.i("ContactListMultiChoiceActivity", "[onClick]resId = menu_option. Fragment=" + this.mListFragment);
            if (this.mListFragment instanceof MultiDuplicationPickerFragment) {
                Log.d("ContactListMultiChoiceActivity", "[onClick]Send result for copy action");
                setResult(11112);
            }
            if (this.mListFragment instanceof PhoneAndEmailsPickerFragment) {
                PhoneAndEmailsPickerFragment phoneAndEmailsPickerFragment = (PhoneAndEmailsPickerFragment) this.mListFragment;
                phoneAndEmailsPickerFragment.setNumberBalance(this.mNumberBalance);
                phoneAndEmailsPickerFragment.onOptionAction();
                return;
            }
            this.mListFragment.onOptionAction();
            return;
        }
        if (id == R.id.search_menu_item) {
            Log.i("ContactListMultiChoiceActivity", "[onClick]resId = search_menu_item ");
            this.mListFragment.updateSelectedItemsView();
            showActionBar(SelectionMode.SearchMode);
            closeOptionsMenu();
            return;
        }
        if (id == R.id.select_items) {
            Log.i("ContactListMultiChoiceActivity", "[onClick]resId = select_items ");
            if (getWindow() == null) {
                Log.w("ContactListMultiChoiceActivity", "[onClick]current Activity dinsow is null");
            } else if (this.mSelectionMenu == null || !this.mSelectionMenu.isShown()) {
                this.mSelectionMenu = updateSelectionMenu((View) view.getParent());
                this.mSelectionMenu.show();
            } else {
                Log.w("ContactListMultiChoiceActivity", "[onClick]mSelectionMenu is already showing, ignore this click");
            }
        }
    }

    private void configureListFragment() {
        if (this.mActionCode == this.mRequest.getActionCode()) {
            Log.w("ContactListMultiChoiceActivity", "[configureListFragment]return ,mActionCode = " + this.mActionCode);
            return;
        }
        Bundle bundle = new Bundle();
        this.mActionCode = this.mRequest.getActionCode();
        Log.i("ContactListMultiChoiceActivity", "[configureListFragment] action code is " + this.mActionCode);
        switch (this.mActionCode) {
            case 300:
                this.mListFragment = new MultiBasePickerFragment();
                break;
            case 305:
                this.mListFragment = new PhoneNumbersPickerFragment();
                ExtensionManager.getInstance();
                ExtensionManager.getRcsExtension().getIntentData(getIntent(), this.mListFragment);
                break;
            case 306:
                this.mListFragment = new PhoneAndEmailsPickerFragment();
                break;
            case 307:
                this.mListFragment = new DataItemsPickerFragment();
                bundle.putParcelable("intent", getIntent());
                this.mListFragment.setArguments(bundle);
                break;
            case 309:
                this.mListFragment = new EmailsPickerFragment();
                break;
            case 310:
                this.mListFragment = new ConferenceCallsPickerFragment();
                bundle.putParcelable("intent", getIntent());
                this.mListFragment.setArguments(bundle);
                break;
            case 16777516:
                this.mListFragment = new MultiVCardPickerFragment();
                break;
            case 33554732:
                this.mListFragment = new MultiDuplicationPickerFragment();
                bundle.putParcelable("intent", getIntent());
                this.mListFragment.setArguments(bundle);
                break;
            default:
                throw new IllegalStateException("Invalid action code: " + this.mActionCode);
        }
        this.mListFragment.setLegacyCompatibilityMode(this.mRequest.isLegacyCompatibilityMode());
        this.mListFragment.setQueryString(this.mRequest.getQueryString(), false);
        this.mListFragment.setDirectoryResultLimit(20);
        this.mListFragment.setVisibleScrollbarEnabled(true);
        getFragmentManager().beginTransaction().replace(R.id.list_container, this.mListFragment).commitAllowingStateLoss();
    }

    @Override
    public boolean onQueryTextChange(String str) {
        this.mListFragment.startSearch(str);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String str) {
        return false;
    }

    @Override
    public boolean onClose() {
        if (this.mSearchView == null) {
            return false;
        }
        if (!TextUtils.isEmpty(this.mSearchView.getQuery())) {
            this.mSearchView.setQuery(null, true);
        }
        showActionBar(SelectionMode.ListMode);
        this.mListFragment.updateSelectedItemsView();
        return true;
    }

    @Override
    public void onFocusChange(View view, boolean z) {
        if (view.getId() == R.id.search_view && z) {
            showInputMethod(this.mSearchView.findFocus());
        }
    }

    private void showInputMethod(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
        if (inputMethodManager != null && !inputMethodManager.showSoftInput(view, 0)) {
            Log.w("ContactListMultiChoiceActivity", "Failed to show soft input method.");
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        Log.i("ContactListMultiChoiceActivity", "[onActivityResult]requestCode = " + i + ",resultCode = " + i2);
        super.onActivityResult(i, i2, intent);
        if (i == 0 && i2 == -1) {
            if (intent != null) {
                startActivity(intent);
            }
            finish();
        }
        if (i2 == 11112) {
            setResult(i2);
            finish();
        }
        if (i2 == 1) {
            long[] longArrayExtra = intent.getLongArrayExtra("checkedids");
            if (this.mListFragment instanceof PhoneAndEmailsPickerFragment) {
                ((PhoneAndEmailsPickerFragment) this.mListFragment).markItemsAsSelectedForCheckedGroups(longArrayExtra);
            }
            ExtensionManager.getInstance();
            ExtensionManager.getRcsExtension().getGroupListResult(this.mListFragment, longArrayExtra);
        }
    }

    @Override
    public void onBackPressed() {
        Log.i("ContactListMultiChoiceActivity", "[onBackPressed]");
        if (this.mSearchView != null && !this.mSearchView.isFocused()) {
            if (!TextUtils.isEmpty(this.mSearchView.getQuery())) {
                this.mSearchView.setQuery(null, true);
            }
            showActionBar(SelectionMode.ListMode);
            this.mListFragment.updateSelectedItemsView();
            return;
        }
        setResult(0);
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        Log.i("ContactListMultiChoiceActivity", "[onConfigurationChanged] newConfig:" + configuration);
        super.onConfigurationChanged(configuration);
    }

    private void showActionBar(SelectionMode selectionMode) {
        Log.d("ContactListMultiChoiceActivity", "[showActionBar]mode = " + selectionMode);
        ActionBar supportActionBar = getSupportActionBar();
        switch (selectionMode) {
            case SearchMode:
                this.mIsSearchMode = true;
                invalidateOptionsMenu();
                View viewInflate = LayoutInflater.from(supportActionBar.getThemedContext()).inflate(R.layout.mtk_multichoice_custom_action_bar, (ViewGroup) null);
                ((Button) viewInflate.findViewById(R.id.select_items)).setVisibility(8);
                this.mSearchView = (SearchView) viewInflate.findViewById(R.id.search_view);
                this.mSearchView.setVisibility(0);
                this.mSearchView.setIconifiedByDefault(true);
                this.mSearchView.setQueryHint(getString(R.string.hint_findContacts));
                this.mSearchView.setIconified(false);
                this.mSearchView.setOnQueryTextListener(this);
                this.mSearchView.setOnCloseListener(this);
                this.mSearchView.setOnQueryTextFocusChangeListener(this);
                this.mSearchView.onActionViewExpanded();
                supportActionBar.setCustomView(viewInflate, new ActionBar.LayoutParams(-1, -2));
                supportActionBar.setDisplayShowCustomEnabled(true);
                supportActionBar.setDisplayShowHomeEnabled(true);
                supportActionBar.setDisplayHomeAsUpEnabled(true);
                Button button = (Button) viewInflate.findViewById(R.id.menu_option);
                button.setTypeface(Typeface.DEFAULT_BOLD);
                if (this.mIsSelectedNone) {
                    button.setEnabled(false);
                    button.setTextColor(-3355444);
                } else {
                    button.setEnabled(true);
                    button.setTextColor(-1);
                }
                button.setOnClickListener(this);
                break;
            case ListMode:
                this.mIsSearchMode = false;
                invalidateOptionsMenu();
                View viewInflate2 = ((LayoutInflater) getSystemService("layout_inflater")).inflate(R.layout.mtk_multichoice_custom_action_bar, (ViewGroup) null);
                this.mSearchView = (SearchView) viewInflate2.findViewById(R.id.search_view);
                this.mSearchView.setVisibility(8);
                ((Button) viewInflate2.findViewById(R.id.select_items)).setOnClickListener(this);
                Button button2 = (Button) viewInflate2.findViewById(R.id.menu_option);
                button2.setTypeface(Typeface.DEFAULT_BOLD);
                button2.getText().toString();
                button2.setOnClickListener(this);
                supportActionBar.setDisplayOptions(20, 30);
                supportActionBar.setCustomView(viewInflate2);
                this.mSearchView = null;
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Log.i("ContactListMultiChoiceActivity", "[onOptionsItemSelected] id=" + menuItem.getItemId());
        if (menuItem.getItemId() == 16908332) {
            hideSoftKeyboard(this.mSearchView);
            if (isResumed()) {
                onBackPressed();
            }
            return true;
        }
        if (menuItem.getItemId() == R.id.groups) {
            startActivityForResult(new Intent(this, (Class<?>) ContactGroupListActivity.class), 1);
            return true;
        }
        if (menuItem.getItemId() == R.id.search_menu_item) {
            this.mListFragment.updateSelectedItemsView();
            this.mIsSelectedNone = this.mListFragment.isSelectedNone();
            showActionBar(SelectionMode.SearchMode);
            menuItem.setVisible(false);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private DropMenu.DropDownMenu updateSelectionMenu(View view) {
        DropMenu dropMenu = new DropMenu(this);
        DropMenu.DropDownMenu dropDownMenuAddDropDownMenu = dropMenu.addDropDownMenu((Button) view.findViewById(R.id.select_items), R.menu.mtk_selection);
        ((Button) view.findViewById(R.id.select_items)).setOnClickListener(this);
        MenuItem menuItemFindItem = dropDownMenuAddDropDownMenu.findItem(R.id.action_select_all);
        this.mListFragment.updateSelectedItemsView();
        this.mIsSelectedAll = this.mListFragment.isSelectedAll();
        if (this.mIsSelectedAll) {
            menuItemFindItem.setTitle(R.string.menu_select_none);
            dropMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    ContactListMultiChoiceActivity.this.showActionBar(SelectionMode.ListMode);
                    ContactListMultiChoiceActivity.this.mListFragment.onClearSelect();
                    return false;
                }
            });
        } else {
            menuItemFindItem.setTitle(R.string.menu_select_all);
            dropMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    ContactListMultiChoiceActivity.this.showActionBar(SelectionMode.ListMode);
                    ContactListMultiChoiceActivity.this.mListFragment.onSelectAll();
                    return false;
                }
            });
        }
        return dropDownMenuAddDropDownMenu;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().addListMenuOptions(this, menu, null, this.mListFragment);
        ExtensionManager.getInstance();
        return ExtensionManager.getContactsPickerExtension().enableDisableSearchMenu(this.mIsSearchMode, menu);
    }

    private void hideSoftKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
        if (inputMethodManager != null && view != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
