package com.android.launcher3.allapps;

import android.content.Intent;
import android.net.Uri;
import android.os.BenesseExtension;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.ComponentKey;
import java.util.ArrayList;

public abstract class AllAppsSearchBarController implements TextWatcher, TextView.OnEditorActionListener, ExtendedEditText.OnBackKeyListener {
    protected AlphabeticalAppsList mApps;
    protected Callbacks mCb;
    protected ExtendedEditText mInput;
    protected InputMethodManager mInputMethodManager;
    protected Launcher mLauncher;
    protected DefaultAppSearchAlgorithm mSearchAlgorithm;

    public interface Callbacks {
        void clearSearchResult();

        void onSearchResult(String str, ArrayList<ComponentKey> arrayList);
    }

    protected abstract DefaultAppSearchAlgorithm onInitializeSearch();

    public final void initialize(AlphabeticalAppsList apps, ExtendedEditText input, Launcher launcher, Callbacks cb) {
        this.mApps = apps;
        this.mCb = cb;
        this.mLauncher = launcher;
        this.mInput = input;
        this.mInput.addTextChangedListener(this);
        this.mInput.setOnEditorActionListener(this);
        this.mInput.setOnBackKeyListener(this);
        this.mInputMethodManager = (InputMethodManager) this.mInput.getContext().getSystemService("input_method");
        this.mSearchAlgorithm = onInitializeSearch();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        String query = s.toString();
        if (query.isEmpty()) {
            this.mSearchAlgorithm.cancel(true);
            this.mCb.clearSearchResult();
        } else {
            this.mSearchAlgorithm.cancel(false);
            this.mSearchAlgorithm.doSearch(query, this.mCb);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId != 3) {
            return false;
        }
        String query = v.getText().toString();
        if (query.isEmpty()) {
            return false;
        }
        int dcha_state = BenesseExtension.getDchaState();
        if (dcha_state == 0) {
            return this.mLauncher.startActivitySafely(v, createMarketSearchIntent(query), null);
        }
        return true;
    }

    @Override
    public boolean onBackKey() {
        String query = Utilities.trim(this.mInput.getEditableText().toString());
        if (query.isEmpty() || this.mApps.hasNoFilteredResults()) {
            reset();
            return true;
        }
        return false;
    }

    public void reset() {
        unfocusSearchField();
        this.mCb.clearSearchResult();
        this.mInput.setText("");
        this.mInputMethodManager.hideSoftInputFromWindow(this.mInput.getWindowToken(), 0);
    }

    protected void unfocusSearchField() {
        View nextFocus = this.mInput.focusSearch(130);
        if (nextFocus == null) {
            return;
        }
        nextFocus.requestFocus();
    }

    public void focusSearchField() {
        this.mInput.requestFocus();
        this.mInputMethodManager.showSoftInput(this.mInput, 1);
    }

    public boolean isSearchFieldFocused() {
        return this.mInput.isFocused();
    }

    public Intent createMarketSearchIntent(String query) {
        Uri marketSearchUri = Uri.parse("market://search").buildUpon().appendQueryParameter("c", "apps").appendQueryParameter("q", query).build();
        return new Intent("android.intent.action.VIEW").setData(marketSearchUri);
    }
}
