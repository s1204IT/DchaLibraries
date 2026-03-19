package com.android.contacts.activities;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.util.MaterialColorMapUtils;
import com.mediatek.contacts.list.DropMenu;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class ActionBarAdapter implements SearchView.OnCloseListener {
    private final ActionBar mActionBar;
    private int mActionBarAnimationDuration;
    private final Activity mActivity;
    private View mClearSearchView;
    private DropMenu mDropMenu;
    private MultiSelectContactsListFragment mFragment;
    private boolean mInitState;
    private MenuItem mItem;
    private Listener mListener;
    private int mMaxToolbarContentInsetStart;
    private String mQueryString;
    private View mSearchContainer;
    private int mSearchHintResId;
    private boolean mSearchMode;
    private EditText mSearchView;
    private View mSelectionContainer;
    private DropMenu.DropDownMenu mSelectionMenu;
    private boolean mSelectionMode;
    private boolean mShowHomeAsUp;
    private boolean mShowHomeIcon;
    private ValueAnimator mStatusBarAnimator;
    private final FrameLayout mToolBarFrame;
    private final Toolbar mToolbar;

    public interface Listener {
        void onAction(int i);

        void onUpButtonPressed();
    }

    public ActionBarAdapter(Activity activity, Listener listener, ActionBar actionBar, Toolbar toolbar, int i) {
        this.mInitState = true;
        this.mActivity = activity;
        this.mListener = listener;
        this.mActionBar = actionBar;
        this.mToolbar = toolbar;
        this.mToolBarFrame = (FrameLayout) this.mToolbar.getParent();
        this.mMaxToolbarContentInsetStart = this.mToolbar.getContentInsetStart();
        this.mSearchHintResId = i;
        this.mActionBarAnimationDuration = this.mActivity.getResources().getInteger(R.integer.action_bar_animation_duration);
        setupSearchAndSelectionViews();
    }

    public ActionBarAdapter(Activity activity, Listener listener, ActionBar actionBar, Toolbar toolbar, int i, Fragment fragment) {
        this(activity, listener, actionBar, toolbar, i);
        if (fragment instanceof MultiSelectContactsListFragment) {
            this.mFragment = (MultiSelectContactsListFragment) fragment;
            setupMultiSelectMenu();
        }
    }

    public void setShowHomeIcon(boolean z) {
        this.mShowHomeIcon = z;
    }

    public void setShowHomeAsUp(boolean z) {
        this.mShowHomeAsUp = z;
    }

    public View getSelectionContainer() {
        return this.mSelectionContainer;
    }

    private void setupSearchAndSelectionViews() {
        LayoutInflater layoutInflater = (LayoutInflater) this.mToolbar.getContext().getSystemService("layout_inflater");
        this.mSearchContainer = layoutInflater.inflate(R.layout.search_bar_expanded, (ViewGroup) this.mToolbar, false);
        this.mSearchContainer.setVisibility(0);
        this.mToolbar.addView(this.mSearchContainer);
        this.mSearchContainer.setBackgroundColor(this.mActivity.getResources().getColor(R.color.searchbox_background_color));
        this.mSearchView = (EditText) this.mSearchContainer.findViewById(R.id.search_view);
        this.mSearchView.setHint(this.mActivity.getString(this.mSearchHintResId));
        this.mSearchView.setInputType(33);
        this.mSearchView.addTextChangedListener(new SearchTextWatcher());
        ImageButton imageButton = (ImageButton) this.mSearchContainer.findViewById(R.id.search_back_button);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActionBarAdapter.this.mListener != null) {
                    ActionBarAdapter.this.mListener.onUpButtonPressed();
                }
            }
        });
        imageButton.getDrawable().setAutoMirrored(true);
        this.mClearSearchView = this.mSearchContainer.findViewById(R.id.search_close_button);
        this.mClearSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActionBarAdapter.this.setQueryString(null);
            }
        });
        View viewFindViewById = this.mToolBarFrame.findViewById(R.id.selection_bar);
        if (viewFindViewById != null) {
            Log.d("ActionBarAdapter", "[setupSearchAndSelectionViews] remove lagecy selection_bar");
            this.mToolBarFrame.removeView(viewFindViewById);
        }
        this.mSelectionContainer = layoutInflater.inflate(R.layout.selection_bar, (ViewGroup) this.mToolbar, false);
        this.mToolBarFrame.addView(this.mSelectionContainer, 0);
        this.mSelectionContainer.findViewById(R.id.selection_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActionBarAdapter.this.mListener != null) {
                    ActionBarAdapter.this.mListener.onUpButtonPressed();
                }
            }
        });
    }

    private void setupMultiSelectMenu() {
        if (this.mFragment == null) {
            Log.w("ActionBarAdapter", "[setupMultiSelectMenu]mFragment == null");
            return;
        }
        Log.w("ActionBarAdapter", "[setupMultiSelectMenu]");
        this.mDropMenu = new DropMenu(this.mActivity);
        Button button = (Button) this.mSelectionContainer.findViewById(R.id.selection_count_button);
        this.mSelectionMenu = this.mDropMenu.addDropDownMenu(button, R.menu.mtk_selection);
        this.mItem = this.mSelectionMenu.findItem(R.id.action_select_all);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActionBarAdapter.this.mActivity.getWindow() == null) {
                    Log.w("ActionBarAdapter", "[onClick]current Activity window is null");
                    return;
                }
                if (ActionBarAdapter.this.isSelectionMode()) {
                    if (!ActionBarAdapter.this.mSelectionMenu.isShown()) {
                        if (ActionBarAdapter.this.mFragment.updateSelectedItemsView()) {
                            ActionBarAdapter.this.updateSelectionMenu();
                            ActionBarAdapter.this.mSelectionMenu.show();
                            return;
                        } else {
                            Log.w("ActionBarAdapter", "[onClick]ignore due to list state not ready");
                            return;
                        }
                    }
                    return;
                }
                Log.w("ActionBarAdapter", "[onClick]ignore due to not select mode");
            }
        });
    }

    private void updateSelectionMenu() {
        Log.d("ActionBarAdapter", "[updateSelectionMenu]");
        if (this.mFragment.isSelectedAll()) {
            this.mItem.setTitle(R.string.menu_select_none);
            this.mDropMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if ((ActionBarAdapter.this.mActivity instanceof PeopleActivity) && !((PeopleActivity) ActionBarAdapter.this.mActivity).getResumed()) {
                        Log.w("ActionBarAdapter", "Deselect all is responsed in non-resumed state, ignore");
                        return true;
                    }
                    if (ActionBarAdapter.this.isSelectionMode()) {
                        ActionBarAdapter.this.mFragment.updateCheckBoxState(false);
                        ActionBarAdapter.this.mFragment.displayCheckBoxes(false);
                        ActionBarAdapter.this.setSelectionMode(false);
                        return true;
                    }
                    Log.w("ActionBarAdapter", "[onMenuItemClick]ignore due to not select mode");
                    return true;
                }
            });
        } else {
            this.mItem.setTitle(R.string.menu_select_all);
            this.mDropMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (ActionBarAdapter.this.isSelectionMode()) {
                        ActionBarAdapter.this.mFragment.updateCheckBoxState(true);
                        ActionBarAdapter.this.mFragment.displayCheckBoxes(true);
                        return true;
                    }
                    Log.w("ActionBarAdapter", "[onMenuItemClick]ignore due to not select mode");
                    return true;
                }
            });
        }
    }

    public void closeSelectMenu() {
        if (this.mSelectionMenu != null && this.mSelectionMenu.isShown()) {
            this.mSelectionMenu.dismiss();
        }
    }

    public boolean isSelectMenuShown() {
        return this.mSelectionMenu != null && this.mSelectionMenu.isShown();
    }

    public void initialize(Bundle bundle, ContactsRequest contactsRequest) {
        if (bundle == null) {
            this.mSearchMode = contactsRequest.isSearchMode();
            this.mQueryString = contactsRequest.getQueryString();
            this.mSelectionMode = false;
        } else {
            this.mSearchMode = bundle.getBoolean("navBar.searchMode");
            this.mSelectionMode = bundle.getBoolean("navBar.selectionMode");
            this.mQueryString = bundle.getString("navBar.query");
        }
        update(true);
        if (this.mSearchMode && !TextUtils.isEmpty(this.mQueryString)) {
            setQueryString(this.mQueryString);
        }
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    private class SearchTextWatcher implements TextWatcher {
        private SearchTextWatcher() {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            if (!charSequence.equals(ActionBarAdapter.this.mQueryString)) {
                ActionBarAdapter.this.mQueryString = charSequence.toString();
                if (ActionBarAdapter.this.mSearchMode) {
                    if (ActionBarAdapter.this.mListener != null) {
                        ActionBarAdapter.this.mListener.onAction(0);
                    }
                } else if (!TextUtils.isEmpty(charSequence)) {
                    ActionBarAdapter.this.setSearchMode(true);
                }
                ActionBarAdapter.this.mClearSearchView.setVisibility(TextUtils.isEmpty(charSequence) ? 8 : 0);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }
    }

    public boolean isSearchMode() {
        return this.mSearchMode;
    }

    public boolean isSelectionMode() {
        return this.mSelectionMode;
    }

    public void setSearchMode(boolean z) {
        if (this.mSearchMode != z) {
            this.mSearchMode = z;
            update(false);
            if (this.mSearchView == null) {
                return;
            }
            if (!this.mSearchMode) {
                this.mSearchView.setEnabled(false);
            } else {
                this.mSearchView.setEnabled(true);
                setFocusOnSearchView();
            }
            setQueryString(null);
            return;
        }
        if (!z || this.mSearchView == null) {
            return;
        }
        setFocusOnSearchView();
    }

    public void setSelectionMode(boolean z) {
        if (this.mSelectionMode != z) {
            this.mSelectionMode = z;
            update(false);
        }
    }

    public String getQueryString() {
        if (this.mSearchMode) {
            return this.mQueryString;
        }
        return null;
    }

    public void setQueryString(String str) {
        this.mQueryString = str;
        if (this.mSearchView != null) {
            this.mSearchView.setText(str);
            this.mSearchView.setSelection(this.mSearchView.getText() == null ? 0 : this.mSearchView.getText().length());
        }
    }

    public boolean isUpShowing() {
        return this.mSearchMode;
    }

    private void updateDisplayOptionsInner() {
        boolean z;
        int i;
        int displayOptions = this.mActionBar.getDisplayOptions() & 14;
        if (this.mSearchMode || this.mSelectionMode) {
            z = true;
        } else {
            z = false;
        }
        if (this.mShowHomeIcon && !z) {
            i = 2;
            if (this.mShowHomeAsUp) {
                i = 6;
            }
        } else {
            i = 0;
        }
        if (this.mSearchMode && !this.mSelectionMode) {
            this.mToolbar.setContentInsetsRelative(0, this.mToolbar.getContentInsetEnd());
        }
        if (!z) {
            i |= 8;
            this.mToolbar.setContentInsetsRelative(this.mMaxToolbarContentInsetStart, this.mToolbar.getContentInsetEnd());
            this.mToolbar.setNavigationIcon(R.drawable.quantum_ic_menu_vd_theme_24);
        } else {
            this.mToolbar.setNavigationIcon((Drawable) null);
        }
        if (this.mSelectionMode) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mToolbar.getLayoutParams();
            layoutParams.width = -2;
            layoutParams.gravity = 8388613;
            this.mToolbar.setLayoutParams(layoutParams);
        } else {
            FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) this.mToolbar.getLayoutParams();
            layoutParams2.width = -1;
            layoutParams2.gravity = 8388613;
            this.mToolbar.setLayoutParams(layoutParams2);
        }
        if (displayOptions != i) {
            this.mActionBar.setDisplayOptions(i, 14);
        }
    }

    private void update(boolean z) {
        updateOverflowButtonColor();
        boolean z2 = false;
        boolean z3 = (this.mSelectionContainer.getParent() == null) == this.mSelectionMode;
        boolean z4 = (this.mSearchMode && z3) || (this.mSearchMode && this.mSelectionMode);
        final boolean z5 = (this.mSearchContainer.getParent() == null) == this.mSearchMode;
        boolean z6 = z5 || z3;
        this.mToolBarFrame.setBackgroundColor(MaterialColorMapUtils.getToolBarColor(this.mActivity));
        if (z3 && !z5) {
            z2 = true;
        }
        updateStatusBarColor(z2);
        if (z || z4) {
            if (z6 || z4) {
                this.mToolbar.removeView(this.mSearchContainer);
                this.mToolBarFrame.removeView(this.mSelectionContainer);
                if (this.mSelectionMode) {
                    addSelectionContainer();
                } else if (this.mSearchMode) {
                    addSearchContainer();
                }
                updateDisplayOptions(z5);
                return;
            }
            return;
        }
        if (z3) {
            if (this.mSelectionMode) {
                addSelectionContainer();
                this.mSelectionContainer.setAlpha(ContactPhotoManager.OFFSET_DEFAULT);
                this.mSelectionContainer.animate().alpha(1.0f).setDuration(this.mActionBarAnimationDuration);
                updateDisplayOptions(z5);
            } else {
                if (this.mListener != null) {
                    this.mListener.onAction(4);
                }
                this.mSelectionContainer.setAlpha(1.0f);
                this.mSelectionContainer.animate().alpha(ContactPhotoManager.OFFSET_DEFAULT).setDuration(this.mActionBarAnimationDuration).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        ActionBarAdapter.this.updateDisplayOptions(z5);
                        ActionBarAdapter.this.mToolBarFrame.removeView(ActionBarAdapter.this.mSelectionContainer);
                    }
                });
            }
        }
        if (z5) {
            if (!this.mSearchMode) {
                this.mSearchContainer.setAlpha(1.0f);
                this.mSearchContainer.animate().alpha(ContactPhotoManager.OFFSET_DEFAULT).setDuration(this.mActionBarAnimationDuration).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        ActionBarAdapter.this.updateDisplayOptions(z5);
                        ActionBarAdapter.this.mToolbar.removeView(ActionBarAdapter.this.mSearchContainer);
                    }
                });
            } else {
                addSearchContainer();
                this.mSearchContainer.setAlpha(ContactPhotoManager.OFFSET_DEFAULT);
                this.mSearchContainer.animate().alpha(1.0f).setDuration(this.mActionBarAnimationDuration);
                updateDisplayOptions(z5);
            }
        }
    }

    public void updateOverflowButtonColor() {
        final String string = this.mActivity.getResources().getString(R.string.action_menu_overflow_description);
        final ViewGroup viewGroup = (ViewGroup) this.mActivity.getWindow().getDecorView();
        viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ArrayList<View> arrayList = new ArrayList<>();
                viewGroup.findViewsWithText(arrayList, string, 2);
                for (View view : arrayList) {
                    if (view instanceof ImageView) {
                        ((ImageView) view).setImageTintList(ColorStateList.valueOf(ActionBarAdapter.this.mSelectionMode ? ActionBarAdapter.this.mActivity.getResources().getColor(R.color.actionbar_color_grey_solid) : ActionBarAdapter.this.mActivity.getResources().getColor(R.color.actionbar_text_color)));
                    }
                }
                viewGroup.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        this.mActivity.invalidateOptionsMenu();
    }

    public void setSelectionCount(int i) {
        Button button = (Button) this.mSelectionContainer.findViewById(R.id.selection_count_button);
        TextView textView = (TextView) this.mSelectionContainer.findViewById(R.id.selection_count_text);
        button.setText(String.format(this.mActivity.getString(R.string.menu_actionbar_selected_items), String.valueOf(i)));
        if (i == 0) {
            if (!this.mInitState) {
                button.setVisibility(8);
                this.mInitState = true;
                return;
            } else {
                button.setVisibility(0);
                return;
            }
        }
        textView.setVisibility(8);
        button.setVisibility(0);
        this.mInitState = false;
    }

    public void setActionBarTitle(String str) {
        TextView textView = (TextView) this.mSelectionContainer.findViewById(R.id.selection_count_text);
        textView.setVisibility(0);
        textView.setText(str);
        ((Button) this.mSelectionContainer.findViewById(R.id.selection_count_button)).setVisibility(8);
    }

    private void updateStatusBarColor(boolean z) {
        if (!CompatUtils.isLollipopCompatible()) {
            return;
        }
        if (this.mSelectionMode) {
            runStatusBarAnimation(ContextCompat.getColor(this.mActivity, R.color.contextual_selection_bar_status_bar_color));
        } else if (z) {
            runStatusBarAnimation(MaterialColorMapUtils.getStatusBarColor(this.mActivity));
        } else if (this.mActivity instanceof PeopleActivity) {
            ((PeopleActivity) this.mActivity).updateStatusBarBackground();
        }
    }

    private void runStatusBarAnimation(int i) {
        final Window window = this.mActivity.getWindow();
        if (window.getStatusBarColor() != i) {
            if (this.mStatusBarAnimator != null && this.mStatusBarAnimator.isRunning()) {
                this.mStatusBarAnimator.cancel();
            }
            this.mStatusBarAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), Integer.valueOf(window.getStatusBarColor()), Integer.valueOf(i));
            this.mStatusBarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    window.setStatusBarColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
                }
            });
            this.mStatusBarAnimator.setDuration(this.mActionBarAnimationDuration);
            this.mStatusBarAnimator.setStartDelay(0L);
            this.mStatusBarAnimator.start();
        }
    }

    private void addSearchContainer() {
        this.mToolbar.removeView(this.mSearchContainer);
        this.mToolbar.addView(this.mSearchContainer);
        this.mSearchContainer.setAlpha(1.0f);
    }

    private void addSelectionContainer() {
        this.mToolBarFrame.removeView(this.mSelectionContainer);
        this.mToolBarFrame.addView(this.mSelectionContainer, 0);
        this.mSelectionContainer.setAlpha(1.0f);
    }

    private void updateDisplayOptions(boolean z) {
        if (this.mSearchMode && !this.mSelectionMode) {
            setFocusOnSearchView();
            if (z) {
                Editable text = this.mSearchView.getText();
                if (!TextUtils.isEmpty(text)) {
                    this.mSearchView.setText(text);
                }
            }
        }
        if (this.mListener != null) {
            if (this.mSearchMode) {
                this.mListener.onAction(1);
            }
            if (this.mSelectionMode) {
                this.mListener.onAction(2);
            }
            if (!this.mSearchMode && !this.mSelectionMode) {
                this.mListener.onAction(3);
            }
        }
        updateDisplayOptionsInner();
    }

    @Override
    public boolean onClose() {
        setSearchMode(false);
        return false;
    }

    public void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean("navBar.searchMode", this.mSearchMode);
        bundle.putBoolean("navBar.selectionMode", this.mSelectionMode);
        bundle.putString("navBar.query", this.mQueryString);
    }

    public void setFocusOnSearchView() {
        this.mSearchView.requestFocus();
        showInputMethod(this.mSearchView);
    }

    private void showInputMethod(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) this.mActivity.getSystemService("input_method");
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(view, 0);
        }
    }
}
