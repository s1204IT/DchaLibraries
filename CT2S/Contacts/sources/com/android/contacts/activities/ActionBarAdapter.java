package com.android.contacts.activities;

import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toolbar;
import com.android.contacts.R;
import com.android.contacts.list.ContactsRequest;

public class ActionBarAdapter implements SearchView.OnCloseListener {
    private final ActionBar mActionBar;
    private final Context mContext;
    private int mCurrentTab = 1;
    private View mLandscapeTabs;
    private Listener mListener;
    private int mMaxPortraitTabHeight;
    private int mMaxToolbarContentInsetStart;
    private View mPortraitTabs;
    private final SharedPreferences mPrefs;
    private String mQueryString;
    private View mSearchContainer;
    private boolean mSearchMode;
    private EditText mSearchView;
    private boolean mShowHomeIcon;
    private final Toolbar mToolbar;

    public interface Listener {
        void onAction(int i);

        void onSelectedTabChanged();

        void onUpButtonPressed();
    }

    public ActionBarAdapter(Context context, Listener listener, ActionBar actionBar, View portraitTabs, View landscapeTabs, Toolbar toolbar) {
        this.mContext = context;
        this.mListener = listener;
        this.mActionBar = actionBar;
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        this.mPortraitTabs = portraitTabs;
        this.mLandscapeTabs = landscapeTabs;
        this.mToolbar = toolbar;
        this.mMaxToolbarContentInsetStart = this.mToolbar.getContentInsetStart();
        this.mShowHomeIcon = this.mContext.getResources().getBoolean(R.bool.show_home_icon);
        setupSearchView();
        setupTabs(context);
    }

    private void setupTabs(Context context) {
        TypedArray attributeArray = context.obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        this.mMaxPortraitTabHeight = attributeArray.getDimensionPixelSize(0, 0);
        setPortraitTabHeight(0);
    }

    private void setupSearchView() {
        LayoutInflater inflater = (LayoutInflater) this.mToolbar.getContext().getSystemService("layout_inflater");
        this.mSearchContainer = inflater.inflate(R.layout.search_bar_expanded, (ViewGroup) this.mToolbar, false);
        this.mSearchContainer.setVisibility(0);
        this.mToolbar.addView(this.mSearchContainer);
        this.mSearchContainer.setBackgroundColor(this.mContext.getResources().getColor(R.color.searchbox_background_color));
        this.mSearchView = (EditText) this.mSearchContainer.findViewById(R.id.search_view);
        this.mSearchView.setHint(this.mContext.getString(R.string.hint_findContacts));
        this.mSearchView.addTextChangedListener(new SearchTextWatcher());
        this.mSearchContainer.findViewById(R.id.search_close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionBarAdapter.this.setQueryString(null);
            }
        });
        this.mSearchContainer.findViewById(R.id.search_back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActionBarAdapter.this.mListener != null) {
                    ActionBarAdapter.this.mListener.onUpButtonPressed();
                }
            }
        });
    }

    public void initialize(Bundle savedState, ContactsRequest request) {
        if (savedState == null) {
            this.mSearchMode = request.isSearchMode();
            this.mQueryString = request.getQueryString();
            this.mCurrentTab = loadLastTabPreference();
        } else {
            this.mSearchMode = savedState.getBoolean("navBar.searchMode");
            this.mQueryString = savedState.getString("navBar.query");
            this.mCurrentTab = savedState.getInt("navBar.selectedTab");
        }
        if (this.mCurrentTab >= 2 || this.mCurrentTab < 0) {
            this.mCurrentTab = 1;
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
        public void onTextChanged(CharSequence queryString, int start, int before, int count) {
            if (!queryString.equals(ActionBarAdapter.this.mQueryString)) {
                ActionBarAdapter.this.mQueryString = queryString.toString();
                if (ActionBarAdapter.this.mSearchMode) {
                    if (ActionBarAdapter.this.mListener != null) {
                        ActionBarAdapter.this.mListener.onAction(0);
                    }
                } else if (!TextUtils.isEmpty(queryString)) {
                    ActionBarAdapter.this.setSearchMode(true);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
    }

    public void setCurrentTab(int tab) {
        setCurrentTab(tab, true);
    }

    public void setCurrentTab(int tab, boolean notifyListener) {
        if (tab != this.mCurrentTab) {
            this.mCurrentTab = tab;
            if (notifyListener && this.mListener != null) {
                this.mListener.onSelectedTabChanged();
            }
            saveLastTabPreference(this.mCurrentTab);
        }
    }

    public int getCurrentTab() {
        return this.mCurrentTab;
    }

    public boolean isSearchMode() {
        return this.mSearchMode;
    }

    public void setSearchMode(boolean flag) {
        if (this.mSearchMode != flag) {
            this.mSearchMode = flag;
            update(false);
            if (this.mSearchView != null) {
                if (this.mSearchMode) {
                    this.mSearchView.setEnabled(true);
                    setFocusOnSearchView();
                } else {
                    this.mSearchView.setEnabled(false);
                }
                setQueryString(null);
                return;
            }
            return;
        }
        if (!flag || this.mSearchView == null) {
            return;
        }
        setFocusOnSearchView();
    }

    public String getQueryString() {
        if (this.mSearchMode) {
            return this.mQueryString;
        }
        return null;
    }

    public void setQueryString(String query) {
        this.mQueryString = query;
        if (this.mSearchView != null) {
            this.mSearchView.setText(query);
            this.mSearchView.setSelection(this.mSearchView.getText() == null ? 0 : this.mSearchView.getText().length());
        }
    }

    public boolean isUpShowing() {
        return this.mSearchMode;
    }

    private void updateDisplayOptionsInner() {
        int newFlags;
        int current = this.mActionBar.getDisplayOptions() & 30;
        int newFlags2 = 0;
        if (this.mShowHomeIcon && !this.mSearchMode) {
            newFlags2 = 0 | 2;
        }
        if (this.mSearchMode) {
            newFlags = newFlags2 | 16;
            this.mToolbar.setContentInsetsRelative(0, this.mToolbar.getContentInsetEnd());
        } else {
            newFlags = newFlags2 | 8;
            this.mToolbar.setContentInsetsRelative(this.mMaxToolbarContentInsetStart, this.mToolbar.getContentInsetEnd());
        }
        if (current != newFlags) {
            this.mActionBar.setDisplayOptions(newFlags, 30);
        }
    }

    private void update(boolean skipAnimation) {
        final boolean isIconifiedChanging = (this.mSearchContainer.getParent() == null) == this.mSearchMode;
        if (isIconifiedChanging && !skipAnimation) {
            this.mToolbar.removeView(this.mLandscapeTabs);
            if (this.mSearchMode) {
                addSearchContainer();
                this.mSearchContainer.setAlpha(0.0f);
                this.mSearchContainer.animate().alpha(1.0f);
                animateTabHeightChange(this.mMaxPortraitTabHeight, 0);
                updateDisplayOptions(isIconifiedChanging);
                return;
            }
            this.mSearchContainer.setAlpha(1.0f);
            animateTabHeightChange(0, this.mMaxPortraitTabHeight);
            this.mSearchContainer.animate().alpha(0.0f).withEndAction(new Runnable() {
                @Override
                public void run() {
                    ActionBarAdapter.this.updateDisplayOptionsInner();
                    ActionBarAdapter.this.updateDisplayOptions(isIconifiedChanging);
                    ActionBarAdapter.this.addLandscapeViewPagerTabs();
                    ActionBarAdapter.this.mToolbar.removeView(ActionBarAdapter.this.mSearchContainer);
                }
            });
            return;
        }
        if (isIconifiedChanging && skipAnimation) {
            this.mToolbar.removeView(this.mLandscapeTabs);
            if (this.mSearchMode) {
                setPortraitTabHeight(0);
                addSearchContainer();
            } else {
                setPortraitTabHeight(this.mMaxPortraitTabHeight);
                this.mToolbar.removeView(this.mSearchContainer);
                addLandscapeViewPagerTabs();
            }
        }
        updateDisplayOptions(isIconifiedChanging);
    }

    private void addLandscapeViewPagerTabs() {
        if (this.mLandscapeTabs != null) {
            this.mToolbar.removeView(this.mLandscapeTabs);
            this.mToolbar.addView(this.mLandscapeTabs);
        }
    }

    private void addSearchContainer() {
        this.mToolbar.removeView(this.mSearchContainer);
        this.mToolbar.addView(this.mSearchContainer);
    }

    private void updateDisplayOptions(boolean isIconifiedChanging) {
        if (this.mSearchMode) {
            setFocusOnSearchView();
            if (isIconifiedChanging) {
                CharSequence queryText = this.mSearchView.getText();
                if (!TextUtils.isEmpty(queryText)) {
                    this.mSearchView.setText(queryText);
                }
            }
            if (this.mListener != null) {
                this.mListener.onAction(1);
            }
        } else if (this.mListener != null) {
            this.mListener.onAction(2);
            this.mListener.onSelectedTabChanged();
        }
        updateDisplayOptionsInner();
    }

    @Override
    public boolean onClose() {
        setSearchMode(false);
        return false;
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("navBar.searchMode", this.mSearchMode);
        outState.putString("navBar.query", this.mQueryString);
        outState.putInt("navBar.selectedTab", this.mCurrentTab);
    }

    public void setFocusOnSearchView() {
        this.mSearchView.requestFocus();
        showInputMethod(this.mSearchView);
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService("input_method");
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    private void saveLastTabPreference(int tab) {
        this.mPrefs.edit().putInt("actionBarAdapter.lastTab", tab).apply();
    }

    private int loadLastTabPreference() {
        try {
            return this.mPrefs.getInt("actionBarAdapter.lastTab", 1);
        } catch (IllegalArgumentException e) {
            return 1;
        }
    }

    private void animateTabHeightChange(int start, int end) {
        if (this.mPortraitTabs != null) {
            ValueAnimator animator = ValueAnimator.ofInt(start, end);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int value = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                    ActionBarAdapter.this.setPortraitTabHeight(value);
                }
            });
            animator.setDuration(100L).start();
        }
    }

    private void setPortraitTabHeight(int height) {
        if (this.mPortraitTabs != null) {
            ViewGroup.LayoutParams layoutParams = this.mPortraitTabs.getLayoutParams();
            layoutParams.height = height;
            this.mPortraitTabs.setLayoutParams(layoutParams);
        }
    }
}
