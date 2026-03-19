package com.android.printspooler.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.printspooler.R;

public final class PrintProgressFragment extends Fragment {
    public static PrintProgressFragment newInstance() {
        return new PrintProgressFragment();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.print_progress_fragment, viewGroup, false);
    }
}
