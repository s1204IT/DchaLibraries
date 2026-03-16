package com.android.printspooler.ui;

import android.app.Activity;
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

    public static PrintErrorFragment newInstance(CharSequence message, int action) {
        Bundle arguments = new Bundle();
        arguments.putCharSequence("EXTRA_MESSAGE", message);
        arguments.putInt("EXTRA_ACTION", action);
        PrintErrorFragment fragment = new PrintErrorFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.print_error_fragment, root, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CharSequence message = getArguments().getCharSequence("EXTRA_MESSAGE");
        if (!TextUtils.isEmpty(message)) {
            TextView messageView = (TextView) view.findViewById(R.id.message);
            messageView.setText(message);
        }
        Button actionButton = (Button) view.findViewById(R.id.action_button);
        int action = getArguments().getInt("EXTRA_ACTION");
        switch (action) {
            case 0:
                actionButton.setVisibility(8);
                break;
            case 1:
                actionButton.setVisibility(0);
                actionButton.setText(R.string.print_error_retry);
                break;
        }
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                Activity activity = PrintErrorFragment.this.getActivity();
                if (activity instanceof OnActionListener) {
                    ((OnActionListener) PrintErrorFragment.this.getActivity()).onActionPerformed();
                }
            }
        });
    }
}
