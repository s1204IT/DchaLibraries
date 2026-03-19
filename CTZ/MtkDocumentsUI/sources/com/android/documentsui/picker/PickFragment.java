package com.android.documentsui.picker;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.documentsui.BaseActivity;
import com.android.documentsui.Injector;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;

public class PickFragment extends Fragment {
    static final boolean $assertionsDisabled = false;
    private int mAction;
    private TextView mCancel;
    private View mContainer;
    private Injector<ActionHandler<PickActivity>> mInjector;
    private TextView mPick;
    private DocumentInfo mPickTarget;
    private final View.OnClickListener mPickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ((ActionHandler) PickFragment.this.mInjector.actions).pickDocument(PickFragment.this.mPickTarget);
        }
    };
    private final View.OnClickListener mCancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            BaseActivity baseActivity = BaseActivity.get(PickFragment.this);
            baseActivity.setResult(0);
            baseActivity.finish();
        }
    };
    private int mCopyOperationSubType = -1;

    public static void show(FragmentManager fragmentManager) {
        if (get(fragmentManager) != null) {
            return;
        }
        PickFragment pickFragment = new PickFragment();
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        fragmentTransactionBeginTransaction.replace(R.id.container_save, pickFragment, "PickFragment");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }

    public static PickFragment get(FragmentManager fragmentManager) {
        return (PickFragment) fragmentManager.findFragmentByTag("PickFragment");
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mContainer = layoutInflater.inflate(R.layout.fragment_pick, viewGroup, false);
        this.mPick = (TextView) this.mContainer.findViewById(android.R.id.button1);
        this.mPick.setOnClickListener(this.mPickListener);
        this.mCancel = (TextView) this.mContainer.findViewById(android.R.id.button2);
        this.mCancel.setOnClickListener(this.mCancelListener);
        updateView();
        return this.mContainer;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if (bundle != null) {
            this.mAction = bundle.getInt("action");
            this.mCopyOperationSubType = bundle.getInt("copyOperationSubType");
            this.mPickTarget = (DocumentInfo) bundle.getParcelable("pickTarget");
            updateView();
        }
        this.mInjector = ((PickActivity) getActivity()).getInjector();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("action", this.mAction);
        bundle.putInt("copyOperationSubType", this.mCopyOperationSubType);
        bundle.putParcelable("pickTarget", this.mPickTarget);
    }

    public void setPickTarget(int i, int i2, DocumentInfo documentInfo) {
        this.mAction = i;
        this.mCopyOperationSubType = i2;
        this.mPickTarget = documentInfo;
        if (this.mContainer != null) {
            updateView();
        }
    }

    private void updateView() {
        int i;
        int i2 = this.mAction;
        if (i2 == 2) {
            switch (this.mCopyOperationSubType) {
                case 1:
                    i = R.string.button_copy;
                    break;
                case 2:
                    i = R.string.button_extract;
                    break;
                case 3:
                    i = R.string.button_compress;
                    break;
                case 4:
                    i = R.string.button_move;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            this.mPick.setText(i);
            this.mCancel.setVisibility(0);
        } else if (i2 == 6) {
            this.mPick.setText(R.string.button_select);
            this.mCancel.setVisibility(8);
        } else {
            this.mContainer.setVisibility(8);
            return;
        }
        if (this.mPickTarget != null && (this.mAction == 6 || this.mPickTarget.isCreateSupported())) {
            this.mContainer.setVisibility(0);
        } else {
            this.mContainer.setVisibility(8);
        }
    }
}
