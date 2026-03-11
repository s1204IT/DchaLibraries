package androidx.car.widget;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.car.R;
import androidx.car.utils.CarUxRestrictionsUtils;
import androidx.car.widget.ListItem;
import androidx.car.widget.SubheaderListItem;
import java.util.Iterator;
import java.util.List;

public class SubheaderListItem extends ListItem<ViewHolder> {
    private final List<ListItem.ViewBinder<ViewHolder>> mBinders;
    private final Context mContext;
    private boolean mIsEnabled;
    private int mListItemSubheaderTextAppearance;
    private String mText;
    private int mTextStartMarginType;

    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getViewType() {
        return 3;
    }

    @Override
    protected void resolveDirtyState() {
        this.mBinders.clear();
        setItemLayoutHeight();
        setText();
    }

    @Override
    public void onBind(ViewHolder viewHolder) {
        Iterator<ListItem.ViewBinder<ViewHolder>> it = this.mBinders.iterator();
        while (it.hasNext()) {
            it.next().bind(viewHolder);
        }
        viewHolder.getText().setEnabled(this.mIsEnabled);
    }

    private void setItemLayoutHeight() {
        final int height = this.mContext.getResources().getDimensionPixelSize(R.dimen.car_sub_header_height);
        this.mBinders.add(new ListItem.ViewBinder() {
            @Override
            public final void bind(Object obj) {
                SubheaderListItem.lambda$setItemLayoutHeight$34(height, (SubheaderListItem.ViewHolder) obj);
            }
        });
    }

    static void lambda$setItemLayoutHeight$34(int i, ViewHolder vh) {
        vh.itemView.getLayoutParams().height = i;
        vh.itemView.requestLayout();
    }

    private void setText() {
        final int textStartMarginDimen;
        switch (this.mTextStartMarginType) {
            case 0:
                textStartMarginDimen = R.dimen.car_keyline_1;
                break;
            case 1:
                textStartMarginDimen = R.dimen.car_keyline_3;
                break;
            case 2:
                textStartMarginDimen = R.dimen.car_keyline_4;
                break;
            default:
                throw new IllegalStateException("Unknown text start margin type.");
        }
        this.mBinders.add(new ListItem.ViewBinder() {
            @Override
            public final void bind(Object obj) {
                SubheaderListItem.lambda$setText$35(this.f$0, textStartMarginDimen, (SubheaderListItem.ViewHolder) obj);
            }
        });
    }

    public static void lambda$setText$35(SubheaderListItem subheaderListItem, int i, ViewHolder vh) {
        vh.getText().setText(subheaderListItem.mText);
        vh.getText().setTextAppearance(subheaderListItem.mListItemSubheaderTextAppearance);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) vh.getText().getLayoutParams();
        layoutParams.setMarginStart(subheaderListItem.mContext.getResources().getDimensionPixelSize(i));
        vh.getText().requestLayout();
    }

    public static class ViewHolder extends ListItem.ViewHolder {
        private TextView mText;

        public ViewHolder(View itemView) {
            super(itemView);
            this.mText = (TextView) itemView.findViewById(R.id.text);
        }

        @Override
        protected void applyUxRestrictions(CarUxRestrictions restrictions) {
            CarUxRestrictionsUtils.apply(this.itemView.getContext(), restrictions, getText());
        }

        public TextView getText() {
            return this.mText;
        }
    }
}
