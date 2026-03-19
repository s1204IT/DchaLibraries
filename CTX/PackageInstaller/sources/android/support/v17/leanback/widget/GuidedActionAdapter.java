package android.support.v17.leanback.widget;

import android.support.v17.leanback.widget.GuidedActionAutofillSupport;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.support.v17.leanback.widget.ImeKeyMonitor;
import android.support.v4.app.DialogFragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class GuidedActionAdapter extends RecyclerView.Adapter {
    private final ActionAutofillListener mActionAutofillListener;
    private final ActionEditListener mActionEditListener;
    private final ActionOnFocusListener mActionOnFocusListener;
    private final ActionOnKeyListener mActionOnKeyListener;
    private final List<GuidedAction> mActions;
    private ClickListener mClickListener;
    DiffCallback<GuidedAction> mDiffCallback;
    GuidedActionAdapterGroup mGroup;
    private final boolean mIsSubAdapter;
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v != null && v.getWindowToken() != null && GuidedActionAdapter.this.getRecyclerView() != null) {
                GuidedActionsStylist.ViewHolder avh = (GuidedActionsStylist.ViewHolder) GuidedActionAdapter.this.getRecyclerView().getChildViewHolder(v);
                GuidedAction action = avh.getAction();
                if (action.hasTextEditable()) {
                    GuidedActionAdapter.this.mGroup.openIme(GuidedActionAdapter.this, avh);
                    return;
                }
                if (action.hasEditableActivatorView()) {
                    GuidedActionAdapter.this.performOnActionClick(avh);
                    return;
                }
                GuidedActionAdapter.this.handleCheckedActions(avh);
                if (action.isEnabled() && !action.infoOnly()) {
                    GuidedActionAdapter.this.performOnActionClick(avh);
                }
            }
        }
    };
    final GuidedActionsStylist mStylist;

    public interface ClickListener {
        void onGuidedActionClicked(GuidedAction guidedAction);
    }

    public interface EditListener {
        void onGuidedActionEditCanceled(GuidedAction guidedAction);

        long onGuidedActionEditedAndProceed(GuidedAction guidedAction);

        void onImeClose();

        void onImeOpen();
    }

    public interface FocusListener {
        void onGuidedActionFocused(GuidedAction guidedAction);
    }

    public GuidedActionAdapter(List<GuidedAction> actions, ClickListener clickListener, FocusListener focusListener, GuidedActionsStylist presenter, boolean isSubAdapter) {
        this.mActions = actions == null ? new ArrayList() : new ArrayList(actions);
        this.mClickListener = clickListener;
        this.mStylist = presenter;
        this.mActionOnKeyListener = new ActionOnKeyListener();
        this.mActionOnFocusListener = new ActionOnFocusListener(focusListener);
        this.mActionEditListener = new ActionEditListener();
        this.mActionAutofillListener = new ActionAutofillListener();
        this.mIsSubAdapter = isSubAdapter;
        if (!isSubAdapter) {
            this.mDiffCallback = GuidedActionDiffCallback.getInstance();
        }
    }

    public void setActions(List<GuidedAction> actions) {
        if (!this.mIsSubAdapter) {
            this.mStylist.collapseAction(false);
        }
        this.mActionOnFocusListener.unFocus();
        if (this.mDiffCallback != null) {
            final List<GuidedAction> oldActions = new ArrayList<>();
            oldActions.addAll(this.mActions);
            this.mActions.clear();
            this.mActions.addAll(actions);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldActions.size();
                }

                @Override
                public int getNewListSize() {
                    return GuidedActionAdapter.this.mActions.size();
                }

                @Override
                public boolean areItemsTheSame(int i, int i2) {
                    return GuidedActionAdapter.this.mDiffCallback.areItemsTheSame((GuidedAction) oldActions.get(i), (GuidedAction) GuidedActionAdapter.this.mActions.get(i2));
                }

                @Override
                public boolean areContentsTheSame(int i, int i2) {
                    return GuidedActionAdapter.this.mDiffCallback.areContentsTheSame((GuidedAction) oldActions.get(i), (GuidedAction) GuidedActionAdapter.this.mActions.get(i2));
                }

                @Override
                public Object getChangePayload(int i, int i2) {
                    return GuidedActionAdapter.this.mDiffCallback.getChangePayload((GuidedAction) oldActions.get(i), (GuidedAction) GuidedActionAdapter.this.mActions.get(i2));
                }
            });
            diffResult.dispatchUpdatesTo(this);
            return;
        }
        this.mActions.clear();
        this.mActions.addAll(actions);
        notifyDataSetChanged();
    }

    public int getCount() {
        return this.mActions.size();
    }

    public GuidedAction getItem(int position) {
        return this.mActions.get(position);
    }

    public int indexOf(GuidedAction action) {
        return this.mActions.indexOf(action);
    }

    public GuidedActionsStylist getGuidedActionsStylist() {
        return this.mStylist;
    }

    @Override
    public int getItemViewType(int position) {
        return this.mStylist.getItemViewType(this.mActions.get(position));
    }

    RecyclerView getRecyclerView() {
        return this.mIsSubAdapter ? this.mStylist.getSubActionsGridView() : this.mStylist.getActionsGridView();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        GuidedActionsStylist.ViewHolder vh = this.mStylist.onCreateViewHolder(parent, viewType);
        View v = vh.itemView;
        v.setOnKeyListener(this.mActionOnKeyListener);
        v.setOnClickListener(this.mOnClickListener);
        v.setOnFocusChangeListener(this.mActionOnFocusListener);
        setupListeners(vh.getEditableTitleView());
        setupListeners(vh.getEditableDescriptionView());
        return vh;
    }

    private void setupListeners(EditText editText) {
        if (editText != 0) {
            editText.setPrivateImeOptions("EscapeNorth=1;");
            editText.setOnEditorActionListener(this.mActionEditListener);
            if (editText instanceof ImeKeyMonitor) {
                ImeKeyMonitor monitor = (ImeKeyMonitor) editText;
                monitor.setImeKeyListener(this.mActionEditListener);
            }
            if (editText instanceof GuidedActionAutofillSupport) {
                ((GuidedActionAutofillSupport) editText).setOnAutofillListener(this.mActionAutofillListener);
            }
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position >= this.mActions.size()) {
            return;
        }
        GuidedActionsStylist.ViewHolder avh = (GuidedActionsStylist.ViewHolder) holder;
        GuidedAction action = this.mActions.get(position);
        this.mStylist.onBindViewHolder(avh, action);
    }

    @Override
    public int getItemCount() {
        return this.mActions.size();
    }

    private class ActionOnFocusListener implements View.OnFocusChangeListener {
        private FocusListener mFocusListener;
        private View mSelectedView;

        ActionOnFocusListener(FocusListener focusListener) {
            this.mFocusListener = focusListener;
        }

        public void unFocus() {
            if (this.mSelectedView != null && GuidedActionAdapter.this.getRecyclerView() != null) {
                RecyclerView.ViewHolder vh = GuidedActionAdapter.this.getRecyclerView().getChildViewHolder(this.mSelectedView);
                if (vh != null) {
                    GuidedActionsStylist.ViewHolder avh = (GuidedActionsStylist.ViewHolder) vh;
                    GuidedActionAdapter.this.mStylist.onAnimateItemFocused(avh, false);
                } else {
                    Log.w("GuidedActionAdapter", "RecyclerView returned null view holder", new Throwable());
                }
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (GuidedActionAdapter.this.getRecyclerView() == null) {
                return;
            }
            GuidedActionsStylist.ViewHolder avh = (GuidedActionsStylist.ViewHolder) GuidedActionAdapter.this.getRecyclerView().getChildViewHolder(v);
            if (hasFocus) {
                this.mSelectedView = v;
                if (this.mFocusListener != null) {
                    this.mFocusListener.onGuidedActionFocused(avh.getAction());
                }
            } else if (this.mSelectedView == v) {
                GuidedActionAdapter.this.mStylist.onAnimateItemPressedCancelled(avh);
                this.mSelectedView = null;
            }
            GuidedActionAdapter.this.mStylist.onAnimateItemFocused(avh, hasFocus);
        }
    }

    public GuidedActionsStylist.ViewHolder findSubChildViewHolder(View v) {
        if (getRecyclerView() == null) {
            return null;
        }
        ViewParent parent = v.getParent();
        while (parent != getRecyclerView() && parent != null && v != null) {
            v = parent;
            parent = parent.getParent();
        }
        if (parent == null || v == null) {
            return null;
        }
        GuidedActionsStylist.ViewHolder result = (GuidedActionsStylist.ViewHolder) getRecyclerView().getChildViewHolder(v);
        return result;
    }

    public void handleCheckedActions(GuidedActionsStylist.ViewHolder avh) {
        GuidedAction action = avh.getAction();
        int actionCheckSetId = action.getCheckSetId();
        if (getRecyclerView() != null && actionCheckSetId != 0) {
            if (actionCheckSetId != -1) {
                int size = this.mActions.size();
                for (int i = 0; i < size; i++) {
                    GuidedAction a = this.mActions.get(i);
                    if (a != action && a.getCheckSetId() == actionCheckSetId && a.isChecked()) {
                        a.setChecked(false);
                        GuidedActionsStylist.ViewHolder vh = (GuidedActionsStylist.ViewHolder) getRecyclerView().findViewHolderForPosition(i);
                        if (vh != null) {
                            this.mStylist.onAnimateItemChecked(vh, false);
                        }
                    }
                }
            }
            if (!action.isChecked()) {
                action.setChecked(true);
                this.mStylist.onAnimateItemChecked(avh, true);
            } else if (actionCheckSetId == -1) {
                action.setChecked(false);
                this.mStylist.onAnimateItemChecked(avh, false);
            }
        }
    }

    public void performOnActionClick(GuidedActionsStylist.ViewHolder avh) {
        if (this.mClickListener != null) {
            this.mClickListener.onGuidedActionClicked(avh.getAction());
        }
    }

    private class ActionOnKeyListener implements View.OnKeyListener {
        private boolean mKeyPressed = false;

        ActionOnKeyListener() {
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (v == null || event == null || GuidedActionAdapter.this.getRecyclerView() == null) {
                return false;
            }
            if (keyCode != 23 && keyCode != 66 && keyCode != 160) {
                switch (keyCode) {
                    case 99:
                    case 100:
                    default:
                        return false;
                }
            } else {
                GuidedActionsStylist.ViewHolder avh = (GuidedActionsStylist.ViewHolder) GuidedActionAdapter.this.getRecyclerView().getChildViewHolder(v);
                GuidedAction action = avh.getAction();
                if (!action.isEnabled() || action.infoOnly()) {
                    event.getAction();
                    return true;
                }
                switch (event.getAction()) {
                    case DialogFragment.STYLE_NORMAL:
                        if (!this.mKeyPressed) {
                            this.mKeyPressed = true;
                            GuidedActionAdapter.this.mStylist.onAnimateItemPressed(avh, this.mKeyPressed);
                        }
                        break;
                    case DialogFragment.STYLE_NO_TITLE:
                        if (this.mKeyPressed) {
                            this.mKeyPressed = false;
                            GuidedActionAdapter.this.mStylist.onAnimateItemPressed(avh, this.mKeyPressed);
                        }
                        break;
                }
            }
            return false;
        }
    }

    private class ActionEditListener implements ImeKeyMonitor.ImeKeyListener, TextView.OnEditorActionListener {
        ActionEditListener() {
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == 5 || actionId == 6) {
                GuidedActionAdapter.this.mGroup.fillAndGoNext(GuidedActionAdapter.this, v);
                return true;
            }
            if (actionId != 1) {
                return false;
            }
            GuidedActionAdapter.this.mGroup.fillAndStay(GuidedActionAdapter.this, v);
            return true;
        }

        @Override
        public boolean onKeyPreIme(EditText editText, int keyCode, KeyEvent event) {
            if (keyCode == 4 && event.getAction() == 1) {
                GuidedActionAdapter.this.mGroup.fillAndStay(GuidedActionAdapter.this, editText);
                return true;
            }
            if (keyCode == 66 && event.getAction() == 1) {
                GuidedActionAdapter.this.mGroup.fillAndGoNext(GuidedActionAdapter.this, editText);
                return true;
            }
            return false;
        }
    }

    private class ActionAutofillListener implements GuidedActionAutofillSupport.OnAutofillListener {
        private ActionAutofillListener() {
        }

        @Override
        public void onAutofill(View view) {
            GuidedActionAdapter.this.mGroup.fillAndGoNext(GuidedActionAdapter.this, (EditText) view);
        }
    }
}
