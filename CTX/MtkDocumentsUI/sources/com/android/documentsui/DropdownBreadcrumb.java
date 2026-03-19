package com.android.documentsui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.documentsui.NavigationViewManager;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.State;
import java.util.function.IntConsumer;

public final class DropdownBreadcrumb extends Spinner implements NavigationViewManager.Breadcrumb {
    private DropdownAdapter mAdapter;

    public DropdownBreadcrumb(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public DropdownBreadcrumb(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public DropdownBreadcrumb(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public DropdownBreadcrumb(Context context) {
        super(context);
    }

    @Override
    public void setup(NavigationViewManager.Environment environment, State state, final IntConsumer intConsumer) {
        this.mAdapter = new DropdownAdapter(state, environment);
        setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                intConsumer.accept(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    @Override
    public void show(boolean z) {
        if (z) {
            setVisibility(0);
            setAdapter((SpinnerAdapter) this.mAdapter);
        } else {
            setVisibility(8);
            setAdapter((SpinnerAdapter) null);
        }
    }

    @Override
    public void postUpdate() {
        setSelection(this.mAdapter.getCount() - 1, false);
    }

    private static final class DropdownAdapter extends BaseAdapter {
        private NavigationViewManager.Environment mEnv;
        private State mState;

        public DropdownAdapter(State state, NavigationViewManager.Environment environment) {
            this.mState = state;
            this.mEnv = environment;
        }

        @Override
        public int getCount() {
            return this.mState.stack.size();
        }

        @Override
        public DocumentInfo getItem(int i) {
            return this.mState.stack.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_subdir_title, viewGroup, false);
            }
            TextView textView = (TextView) view.findViewById(android.R.id.title);
            DocumentInfo item = getItem(i);
            if (i == 0) {
                textView.setText(this.mEnv.getCurrentRoot().title);
            } else if (item != null) {
                textView.setText(item.displayName);
            }
            return view;
        }

        @Override
        public View getDropDownView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_subdir, viewGroup, false);
            }
            TextView textView = (TextView) view.findViewById(android.R.id.title);
            DocumentInfo item = getItem(i);
            if (i == 0) {
                textView.setText(this.mEnv.getCurrentRoot().title);
            } else if (item != null) {
                textView.setText(item.displayName);
            }
            return view;
        }
    }
}
