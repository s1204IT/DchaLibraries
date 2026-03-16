package com.android.deskclock.timer;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.deskclock.CircleButtonsLayout;
import com.android.deskclock.DeskClock;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.R;

public class TimerItemFragment extends Fragment {
    private TimerObj mTimerObj;

    public static TimerItemFragment newInstance(TimerObj timerObj) {
        TimerItemFragment fragment = new TimerItemFragment();
        Bundle args = new Bundle();
        args.putParcelable("TimerItemFragment_tag", timerObj);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            this.mTimerObj = (TimerObj) bundle.getParcelable("TimerItemFragment_tag");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TimerListItem v = (TimerListItem) inflater.inflate(R.layout.timer_list_item, (ViewGroup) null);
        this.mTimerObj.mView = v;
        long timeLeft = this.mTimerObj.updateTimeLeft(false);
        boolean drawWithColor = this.mTimerObj.mState != 5;
        v.set(this.mTimerObj.mOriginalLength, timeLeft, drawWithColor);
        v.setTime(timeLeft, true);
        v.setResetAddButton(this.mTimerObj.mState == 1 || this.mTimerObj.mState == 3, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment parent = TimerItemFragment.this.getParentFragment();
                if (parent instanceof TimerFragment) {
                    ((TimerFragment) parent).onPlusOneButtonPressed(TimerItemFragment.this.mTimerObj);
                }
            }
        });
        switch (this.mTimerObj.mState) {
            case 1:
                v.start();
                break;
            case 3:
                v.timesUp();
                break;
            case 4:
                v.done();
                break;
        }
        CircleButtonsLayout circleLayout = (CircleButtonsLayout) v.findViewById(R.id.timer_circle);
        circleLayout.setCircleTimerViewIds(R.id.timer_time, R.id.reset_add, R.id.timer_label, R.id.timer_label_text);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View v = this.mTimerObj.mView;
        if (v != null) {
            FrameLayout labelLayout = (FrameLayout) v.findViewById(R.id.timer_label);
            TextView labelPlaceholder = (TextView) v.findViewById(R.id.timer_label_placeholder);
            TextView labelText = (TextView) v.findViewById(R.id.timer_label_text);
            if (TextUtils.isEmpty(this.mTimerObj.mLabel)) {
                labelText.setVisibility(8);
                labelPlaceholder.setVisibility(0);
            } else {
                labelText.setText(this.mTimerObj.mLabel);
                labelText.setVisibility(0);
                labelPlaceholder.setVisibility(8);
            }
            Activity activity = getActivity();
            if (activity instanceof DeskClock) {
                labelLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TimerItemFragment.this.onLabelPressed(TimerItemFragment.this.mTimerObj);
                    }
                });
            } else {
                labelPlaceholder.setVisibility(4);
            }
        }
    }

    private void onLabelPressed(TimerObj t) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("label_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        LabelDialogFragment newFragment = LabelDialogFragment.newInstance(t, t.mLabel, getParentFragment().getTag());
        newFragment.show(ft, "label_dialog");
    }
}
