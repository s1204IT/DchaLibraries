package com.android.printspooler.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.printspooler.R;

public final class PrintErrorFragment extends Fragment {

    public interface OnActionListener {
        void onActionPerformed();
    }

    public static PrintErrorFragment newInstance(CharSequence charSequence, int i) {
        Bundle bundle = new Bundle();
        bundle.putCharSequence("EXTRA_MESSAGE", charSequence);
        bundle.putInt("EXTRA_ACTION", i);
        PrintErrorFragment printErrorFragment = new PrintErrorFragment();
        printErrorFragment.setArguments(bundle);
        return printErrorFragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.print_error_fragment, viewGroup, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        CharSequence charSequence = getArguments().getCharSequence("EXTRA_MESSAGE");
        if (!TextUtils.isEmpty(charSequence)) {
            ((TextView) view.findViewById(R.id.message)).setText(charSequence);
        }
        Button button = (Button) view.findViewById(R.id.action_button);
        switch (getArguments().getInt("EXTRA_ACTION")) {
            case 0:
                button.setVisibility(8);
                break;
            case 1:
                button.setVisibility(0);
                button.setText(R.string.print_error_retry);
                break;
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                if (PrintErrorFragment.this.getActivity() instanceof OnActionListener) {
                    ((OnActionListener) PrintErrorFragment.this.getActivity()).onActionPerformed();
                }
            }
        });
    }
}
