package com.android.launcher3.allapps.search;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.TextView;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.PackageManagerHelper;
import java.util.ArrayList;

public class AllAppsSearchBarController implements TextWatcher, TextView.OnEditorActionListener, ExtendedEditText.OnBackKeyListener {
    protected Callbacks mCb;
    protected ExtendedEditText mInput;
    protected Launcher mLauncher;
    protected String mQuery;
    protected SearchAlgorithm mSearchAlgorithm;

    public interface Callbacks {
        void clearSearchResult();

        void onSearchResult(String str, ArrayList<ComponentKey> arrayList);
    }

    public void setVisibility(int i) {
        this.mInput.setVisibility(i);
    }

    public final void initialize(SearchAlgorithm searchAlgorithm, ExtendedEditText extendedEditText, Launcher launcher, Callbacks callbacks) {
        this.mCb = callbacks;
        this.mLauncher = launcher;
        this.mInput = extendedEditText;
        this.mInput.addTextChangedListener(this);
        this.mInput.setOnEditorActionListener(this);
        this.mInput.setOnBackKeyListener(this);
        this.mSearchAlgorithm = searchAlgorithm;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        this.mQuery = editable.toString();
        if (this.mQuery.isEmpty()) {
            this.mSearchAlgorithm.cancel(true);
            this.mCb.clearSearchResult();
        } else {
            this.mSearchAlgorithm.cancel(false);
            this.mSearchAlgorithm.doSearch(this.mQuery, this.mCb);
        }
    }

    public void refreshSearchResult() {
        if (TextUtils.isEmpty(this.mQuery)) {
            return;
        }
        this.mSearchAlgorithm.cancel(false);
        this.mSearchAlgorithm.doSearch(this.mQuery, this.mCb);
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i != 3) {
            return false;
        }
        String string = textView.getText().toString();
        if (string.isEmpty()) {
            return false;
        }
        return this.mLauncher.startActivitySafely(textView, PackageManagerHelper.getMarketSearchIntent(this.mLauncher, string), null);
    }

    @Override
    public boolean onBackKey() {
        if (Utilities.trim(this.mInput.getEditableText().toString()).isEmpty()) {
            reset();
            return true;
        }
        return false;
    }

    public void reset() {
        this.mCb.clearSearchResult();
        this.mInput.reset();
        this.mQuery = null;
    }

    public void focusSearchField() {
        this.mInput.showKeyboard();
    }

    public boolean isSearchFieldFocused() {
        return this.mInput.isFocused();
    }
}
