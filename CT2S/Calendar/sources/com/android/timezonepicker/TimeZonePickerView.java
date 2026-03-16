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

    public TimeZonePickerView(Context context, AttributeSet attrs, String timeZone, long timeMillis, OnTimeZoneSetListener l, boolean hideFilterSearch) {
        super(context, attrs);
        this.mHideFilterSearchOnStart = false;
        this.mFirstTime = true;
        this.mContext = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        inflater.inflate(R.layout.timezonepickerview, (ViewGroup) this, true);
        this.mHideFilterSearchOnStart = hideFilterSearch;
        TimeZoneData tzd = new TimeZoneData(this.mContext, timeZone, timeMillis);
        this.mResultAdapter = new TimeZoneResultAdapter(this.mContext, tzd, l);
        ListView timeZoneList = (ListView) findViewById(R.id.timezonelist);
        timeZoneList.setAdapter((ListAdapter) this.mResultAdapter);
        timeZoneList.setOnItemClickListener(this.mResultAdapter);
        this.mFilterAdapter = new TimeZoneFilterTypeAdapter(this.mContext, tzd, this.mResultAdapter);
        this.mAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.searchBox);
        this.mAutoCompleteTextView.addTextChangedListener(this);
        this.mAutoCompleteTextView.setOnItemClickListener(this);
        this.mAutoCompleteTextView.setOnClickListener(this);
        updateHint(R.string.hint_time_zone_search, R.drawable.ic_search_holo_light);
        this.mClearButton = (ImageButton) findViewById(R.id.clear_search);
        this.mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimeZonePickerView.this.mAutoCompleteTextView.getEditableText().clear();
            }
        });
    }

    public void showFilterResults(int type, String string, int time) {
        if (this.mResultAdapter != null) {
            this.mResultAdapter.onSetFilter(type, string, time);
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
            return this.mResultAdapter.getLastFilterType();
        }
        return -1;
    }

    public boolean getHideFilterSearchOnStart() {
        return this.mHideFilterSearchOnStart;
    }

    private void updateHint(int hintTextId, int imageDrawableId) {
        String hintText = getResources().getString(hintTextId);
        Drawable searchIcon = getResources().getDrawable(imageDrawableId);
        SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
        ssb.append((CharSequence) hintText);
        int textSize = (int) (((double) this.mAutoCompleteTextView.getTextSize()) * 1.25d);
        searchIcon.setBounds(0, 0, textSize, textSize);
        ssb.setSpan(new ImageSpan(searchIcon), 1, 2, 33);
        this.mAutoCompleteTextView.setHint(ssb);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (this.mFirstTime && this.mHideFilterSearchOnStart) {
            this.mFirstTime = false;
        } else {
            filterOnString(s.toString());
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (this.mClearButton != null) {
            this.mClearButton.setVisibility(s.length() > 0 ? 0 : 8);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        InputMethodManager manager = (InputMethodManager) getContext().getSystemService("input_method");
        manager.hideSoftInputFromWindow(this.mAutoCompleteTextView.getWindowToken(), 0);
        this.mHideFilterSearchOnStart = true;
        this.mFilterAdapter.onClick(view);
    }

    @Override
    public void onClick(View v) {
        if (this.mAutoCompleteTextView != null && !this.mAutoCompleteTextView.isPopupShowing()) {
            filterOnString(this.mAutoCompleteTextView.getText().toString());
        }
    }

    private void filterOnString(String string) {
        if (this.mAutoCompleteTextView.getAdapter() == null) {
            this.mAutoCompleteTextView.setAdapter(this.mFilterAdapter);
        }
        this.mHideFilterSearchOnStart = false;
        this.mFilterAdapter.getFilter().filter(string);
    }
}
