package com.android.documentsui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.documentsui.model.DocumentInfo;
import java.util.Locale;

public class PickFragment extends Fragment {
    private View mContainer;
    private Button mPick;
    private View.OnClickListener mPickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            DocumentsActivity activity = DocumentsActivity.get(PickFragment.this);
            activity.onPickRequested(PickFragment.this.mPickTarget);
        }
    };
    private DocumentInfo mPickTarget;

    public static void show(FragmentManager fm) {
        PickFragment fragment = new PickFragment();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_save, fragment, "PickFragment");
        ft.commitAllowingStateLoss();
    }

    public static PickFragment get(FragmentManager fm) {
        return (PickFragment) fm.findFragmentByTag("PickFragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mContainer = inflater.inflate(R.layout.fragment_pick, container, false);
        this.mPick = (Button) this.mContainer.findViewById(android.R.id.button1);
        this.mPick.setOnClickListener(this.mPickListener);
        setPickTarget(null, null);
        return this.mContainer;
    }

    public void setPickTarget(DocumentInfo pickTarget, CharSequence displayName) {
        this.mPickTarget = pickTarget;
        if (this.mContainer != null) {
            if (this.mPickTarget != null) {
                this.mContainer.setVisibility(0);
                Locale locale = getResources().getConfiguration().locale;
                String raw = getString(R.string.menu_select).toUpperCase(locale);
                this.mPick.setText(TextUtils.expandTemplate(raw, displayName));
                return;
            }
            this.mContainer.setVisibility(8);
        }
    }
}
