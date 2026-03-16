package com.android.browser;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import com.android.browser.UrlInputView;

public class NavigationBarPhone extends NavigationBarBase implements PopupMenu.OnDismissListener, PopupMenu.OnMenuItemClickListener, UrlInputView.StateListener {
    private ImageView mClearButton;
    private View mComboIcon;
    private View mIncognitoIcon;
    private ImageView mMagnify;
    private View mMore;
    private boolean mNeedsMenu;
    private boolean mOverflowMenuShowing;
    private PopupMenu mPopupMenu;
    private String mRefreshDescription;
    private Drawable mRefreshDrawable;
    private ImageView mStopButton;
    private String mStopDescription;
    private Drawable mStopDrawable;
    private View mTabSwitcher;
    private Drawable mTextfieldBgDrawable;
    private View mTitleContainer;
    private ImageView mVoiceButton;

    public NavigationBarPhone(Context context) {
        super(context);
    }

    public NavigationBarPhone(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigationBarPhone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mStopButton = (ImageView) findViewById(R.id.stop);
        this.mStopButton.setOnClickListener(this);
        this.mClearButton = (ImageView) findViewById(R.id.clear);
        this.mClearButton.setOnClickListener(this);
        this.mVoiceButton = (ImageView) findViewById(R.id.voice);
        this.mVoiceButton.setOnClickListener(this);
        this.mMagnify = (ImageView) findViewById(R.id.magnify);
        this.mTabSwitcher = findViewById(R.id.tab_switcher);
        this.mTabSwitcher.setOnClickListener(this);
        this.mMore = findViewById(R.id.more);
        this.mMore.setOnClickListener(this);
        this.mComboIcon = findViewById(R.id.iconcombo);
        this.mComboIcon.setOnClickListener(this);
        this.mTitleContainer = findViewById(R.id.title_bg);
        setFocusState(false);
        Resources res = getContext().getResources();
        this.mStopDrawable = res.getDrawable(R.drawable.ic_stop_holo_dark);
        this.mRefreshDrawable = res.getDrawable(R.drawable.ic_refresh_holo_dark);
        this.mStopDescription = res.getString(R.string.accessibility_button_stop);
        this.mRefreshDescription = res.getString(R.string.accessibility_button_refresh);
        this.mTextfieldBgDrawable = res.getDrawable(R.drawable.textfield_active_holo_dark);
        this.mUrlInput.setContainer(this);
        this.mUrlInput.setStateListener(this);
        this.mNeedsMenu = !ViewConfiguration.get(getContext()).hasPermanentMenuKey();
        this.mIncognitoIcon = findViewById(R.id.incognito_icon);
    }

    @Override
    public void onProgressStarted() {
        super.onProgressStarted();
        if (this.mStopButton.getDrawable() != this.mStopDrawable) {
            this.mStopButton.setImageDrawable(this.mStopDrawable);
            this.mStopButton.setContentDescription(this.mStopDescription);
            if (this.mStopButton.getVisibility() != 0) {
                this.mComboIcon.setVisibility(8);
                this.mStopButton.setVisibility(0);
            }
        }
    }

    @Override
    public void onProgressStopped() {
        super.onProgressStopped();
        this.mStopButton.setImageDrawable(this.mRefreshDrawable);
        this.mStopButton.setContentDescription(this.mRefreshDescription);
        if (!isEditingUrl()) {
            this.mComboIcon.setVisibility(0);
        }
        onStateChanged(this.mUrlInput.getState());
    }

    @Override
    void setDisplayTitle(String title) {
        this.mUrlInput.setTag(title);
        if (!isEditingUrl()) {
            if (title == null) {
                this.mUrlInput.setText(R.string.new_tab);
            } else {
                this.mUrlInput.setText((CharSequence) UrlUtils.stripUrl(title), false);
            }
            this.mUrlInput.setSelection(0);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == this.mStopButton) {
            if (this.mTitleBar.isInLoad()) {
                this.mUiController.stopLoading();
                return;
            }
            WebView web = this.mBaseUi.getWebView();
            if (web != null) {
                stopEditingUrl();
                web.reload();
                return;
            }
            return;
        }
        if (v == this.mTabSwitcher) {
            ((PhoneUi) this.mBaseUi).toggleNavScreen();
            return;
        }
        if (this.mMore == v) {
            showMenu(this.mMore);
            return;
        }
        if (this.mClearButton == v) {
            this.mUrlInput.setText("");
            return;
        }
        if (this.mComboIcon == v) {
            this.mUiController.showPageInfo();
        } else if (this.mVoiceButton == v) {
            this.mUiController.startVoiceRecognizer();
        } else {
            super.onClick(v);
        }
    }

    @Override
    public boolean isMenuShowing() {
        return super.isMenuShowing() || this.mOverflowMenuShowing;
    }

    void showMenu(View anchor) {
        Activity activity = this.mUiController.getActivity();
        if (this.mPopupMenu == null) {
            this.mPopupMenu = new PopupMenu(this.mContext, anchor);
            this.mPopupMenu.setOnMenuItemClickListener(this);
            this.mPopupMenu.setOnDismissListener(this);
            if (!activity.onCreateOptionsMenu(this.mPopupMenu.getMenu())) {
                this.mPopupMenu = null;
                return;
            }
        }
        Menu menu = this.mPopupMenu.getMenu();
        if (activity.onPrepareOptionsMenu(menu)) {
            this.mOverflowMenuShowing = true;
            this.mPopupMenu.show();
        }
    }

    @Override
    public void onDismiss(PopupMenu menu) {
        if (menu == this.mPopupMenu) {
            onMenuHidden();
        }
    }

    private void onMenuHidden() {
        this.mOverflowMenuShowing = false;
        this.mBaseUi.showTitleBarForDuration();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == this.mUrlInput) {
            if (hasFocus && !this.mUrlInput.getText().toString().equals(this.mUrlInput.getTag())) {
                this.mUrlInput.setText((CharSequence) this.mUrlInput.getTag(), false);
                this.mUrlInput.selectAll();
                ((InputMethodManager) this.mContext.getSystemService("input_method")).updateSelection(this.mUrlInput, 0, ((String) this.mUrlInput.getTag()).length(), 0, 0);
            } else {
                setDisplayTitle(this.mUrlInput.getText().toString());
            }
        }
        super.onFocusChange(view, hasFocus);
    }

    @Override
    public void onStateChanged(int state) {
        this.mVoiceButton.setVisibility(8);
        switch (state) {
            case 0:
                this.mComboIcon.setVisibility(0);
                this.mStopButton.setVisibility(8);
                this.mClearButton.setVisibility(8);
                this.mMagnify.setVisibility(8);
                this.mTabSwitcher.setVisibility(0);
                this.mTitleContainer.setBackgroundDrawable(null);
                this.mMore.setVisibility(this.mNeedsMenu ? 0 : 8);
                break;
            case 1:
                this.mComboIcon.setVisibility(8);
                this.mStopButton.setVisibility(0);
                this.mClearButton.setVisibility(8);
                if (this.mUiController != null && this.mUiController.supportsVoice()) {
                    this.mVoiceButton.setVisibility(0);
                }
                this.mMagnify.setVisibility(8);
                this.mTabSwitcher.setVisibility(8);
                this.mMore.setVisibility(8);
                this.mTitleContainer.setBackgroundDrawable(this.mTextfieldBgDrawable);
                break;
            case 2:
                this.mComboIcon.setVisibility(8);
                this.mStopButton.setVisibility(8);
                this.mClearButton.setVisibility(0);
                this.mMagnify.setVisibility(0);
                this.mTabSwitcher.setVisibility(8);
                this.mMore.setVisibility(8);
                this.mTitleContainer.setBackgroundDrawable(this.mTextfieldBgDrawable);
                break;
        }
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        super.onTabDataChanged(tab);
        this.mIncognitoIcon.setVisibility(tab.isPrivateBrowsingEnabled() ? 0 : 8);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return this.mUiController.onOptionsItemSelected(item);
    }
}
