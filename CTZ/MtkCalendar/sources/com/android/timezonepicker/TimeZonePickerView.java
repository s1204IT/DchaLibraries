package com.android.timezonepicker;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

public class TimeZonePickerView extends LinearLayout implements TextWatcher, View.OnClickListener, AdapterView.OnItemClickListener {
    private AutoCompleteTextView mAutoCompleteTextView;
    private ImageButton mClearButton;
    private Context mContext;
    private TimeZoneFilterTypeAdapter mFilterAdapter;
    private boolean mFirstTime;
    private boolean mHideFilterSearchOnStart;
    TimeZoneResultAdapter mResultAdapter;

    public interface OnTimeZoneSetListener {
        void onTimeZoneSet(TimeZoneInfo timeZoneInfo);
    }

    public TimeZonePickerView(Context context, AttributeSet attributeSet, String str, long j, OnTimeZoneSetListener onTimeZoneSetListener, boolean z) {
        super(context, attributeSet);
        this.mHideFilterSearchOnStart = false;
        this.mFirstTime = true;
        this.mContext = context;
        ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.timezonepickerview, (ViewGroup) this, true);
        this.mHideFilterSearchOnStart = z;
        TimeZoneData timeZoneData = new TimeZoneData(this.mContext, str, j);
        this.mResultAdapter = new TimeZoneResultAdapter(this.mContext, timeZoneData, onTimeZoneSetListener);
        ListView listView = (ListView) findViewById(R.id.timezonelist);
        listView.setAdapter((ListAdapter) this.mResultAdapter);
        listView.setOnItemClickListener(this.mResultAdapter);
        this.mFilterAdapter = new TimeZoneFilterTypeAdapter(this.mContext, timeZoneData, this.mResultAdapter);
        this.mAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.searchBox);
        this.mAutoCompleteTextView.addTextChangedListener(this);
        this.mAutoCompleteTextView.setOnItemClickListener(this);
        this.mAutoCompleteTextView.setOnClickListener(this);
        updateHint(R.string.hint_time_zone_search, R.drawable.ic_search_holo_light);
        this.mClearButton = (ImageButton) findViewById(R.id.clear_search);
        this.mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimeZonePickerView.this.mAutoCompleteTextView.getEditableText().clear();
            }
        });
    }

    public void showFilterResults(int i, String str, int i2) {
        if (this.mResultAdapter != null) {
            this.mResultAdapter.onSetFilter(i, str, i2);
        }
    }

    public boolean hasResults() {
        return this.mResultAdapter != null && this.mResultAdapter.hasResults();
    }

    public int getLastFilterType() {
        if (this.mResultAdapter != null) {
            return this.mResultAdapter.getLastFilterType();
        }
        return -1;
    }

    public String getLastFilterString() {
        if (this.mResultAdapter != null) {
            return this.mResultAdapter.getLastFilterString();
        }
        return null;
    }

    public int getLastFilterTime() {
        if (this.mResultAdapter != null) {
            return this.mResultAdapter.getLastFilterTime();
        }
        return -1;
    }

    public boolean getHideFilterSearchOnStart() {
        return this.mHideFilterSearchOnStart;
    }

    private void updateHint(int i, int i2) {
        String string = getResources().getString(i);
        Drawable drawable = getResources().getDrawable(i2);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder("   ");
        spannableStringBuilder.append((CharSequence) string);
        int textSize = (int) (((double) this.mAutoCompleteTextView.getTextSize()) * 1.25d);
        drawable.setBounds(0, 0, textSize, textSize);
        spannableStringBuilder.setSpan(new ImageSpan(drawable), 1, 2, 33);
        this.mAutoCompleteTextView.setHint(spannableStringBuilder);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (this.mFirstTime && this.mHideFilterSearchOnStart) {
            this.mFirstTime = false;
        } else {
            filterOnString(charSequence.toString());
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (this.mClearButton != null) {
            this.mClearButton.setVisibility(editable.length() > 0 ? 0 : 8);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        ((InputMethodManager) getContext().getSystemService("input_method")).hideSoftInputFromWindow(this.mAutoCompleteTextView.getWindowToken(), 0);
        this.mHideFilterSearchOnStart = true;
        this.mFilterAdapter.onClick(view);
    }

    @Override
    public void onClick(View view) {
        if (this.mAutoCompleteTextView != null && !this.mAutoCompleteTextView.isPopupShowing()) {
            filterOnString(this.mAutoCompleteTextView.getText().toString());
        }
    }

    private void filterOnString(String str) {
        if (this.mAutoCompleteTextView.getAdapter() == null) {
            this.mAutoCompleteTextView.setAdapter(this.mFilterAdapter);
        }
        this.mHideFilterSearchOnStart = false;
        this.mFilterAdapter.getFilter().filter(str);
    }
}
