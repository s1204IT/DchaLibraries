package androidx.slice.widget;

import android.R;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v4.widget.ImageViewCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import java.util.Iterator;
import java.util.List;

public class ActionRow extends FrameLayout {
    private final LinearLayout mActionsGroup;
    private int mColor;
    private final boolean mFullActions;
    private final int mIconPadding;
    private final int mSize;

    public ActionRow(Context context, boolean fullActions) {
        super(context);
        this.mColor = -16777216;
        this.mFullActions = fullActions;
        this.mSize = (int) TypedValue.applyDimension(1, 48.0f, context.getResources().getDisplayMetrics());
        this.mIconPadding = (int) TypedValue.applyDimension(1, 12.0f, context.getResources().getDisplayMetrics());
        this.mActionsGroup = new LinearLayout(context);
        this.mActionsGroup.setOrientation(0);
        this.mActionsGroup.setLayoutParams(new FrameLayout.LayoutParams(-1, -2));
        addView(this.mActionsGroup);
    }

    private void setColor(int color) {
        this.mColor = color;
        for (int i = 0; i < this.mActionsGroup.getChildCount(); i++) {
            View view = this.mActionsGroup.getChildAt(i);
            int mode = ((Integer) view.getTag()).intValue();
            boolean tint = mode == 0;
            if (tint) {
                ImageViewCompat.setImageTintList((ImageView) view, ColorStateList.valueOf(this.mColor));
            }
        }
    }

    private ImageView addAction(IconCompat icon, boolean allowTint) {
        ImageView imageView = new ImageView(getContext());
        imageView.setPadding(this.mIconPadding, this.mIconPadding, this.mIconPadding, this.mIconPadding);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageDrawable(icon.loadDrawable(getContext()));
        if (allowTint) {
            ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(this.mColor));
        }
        imageView.setBackground(SliceViewUtil.getDrawable(getContext(), R.attr.selectableItemBackground));
        imageView.setTag(Boolean.valueOf(allowTint));
        addAction(imageView);
        return imageView;
    }

    public void setActions(List<SliceAction> list, int i) {
        IconCompat icon;
        removeAllViews();
        this.mActionsGroup.removeAllViews();
        addView(this.mActionsGroup);
        if (i != -1) {
            setColor(i);
        }
        Iterator<SliceAction> it = list.iterator();
        while (true) {
            if (it.hasNext()) {
                SliceAction next = it.next();
                if (this.mActionsGroup.getChildCount() >= 5) {
                    return;
                }
                SliceItem sliceItem = ((SliceActionImpl) next).getSliceItem();
                final SliceItem actionItem = ((SliceActionImpl) next).getActionItem();
                SliceItem sliceItemFind = SliceQuery.find(sliceItem, "input");
                SliceItem sliceItemFind2 = SliceQuery.find(sliceItem, "image");
                if (sliceItemFind != null && sliceItemFind2 != null) {
                    if (Build.VERSION.SDK_INT >= 21) {
                        handleSetRemoteInputActions(sliceItemFind, sliceItemFind2, actionItem);
                    } else {
                        Log.w("ActionRow", "Received RemoteInput on API <20 " + sliceItemFind);
                    }
                } else if (next.getIcon() != null && (icon = next.getIcon()) != null && actionItem != null) {
                    addAction(icon, next.getImageMode() == 0).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                actionItem.fireAction(null, null);
                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            } else {
                setVisibility(getChildCount() == 0 ? 8 : 0);
                return;
            }
        }
    }

    private void addAction(View child) {
        this.mActionsGroup.addView(child, new LinearLayout.LayoutParams(this.mSize, this.mSize, 1.0f));
    }

    private void handleSetRemoteInputActions(final SliceItem input, SliceItem image, final SliceItem action) {
        if (input.getRemoteInput().getAllowFreeFormInput()) {
            boolean tint = !image.hasHint("no_tint");
            addAction(image.getIcon(), tint).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActionRow.this.handleRemoteInputClick(v, action, input.getRemoteInput());
                }
            });
            createRemoteInputView(this.mColor, getContext());
        }
    }

    private void createRemoteInputView(int color, Context context) {
        View riv = RemoteInputView.inflate(context, this);
        riv.setVisibility(4);
        addView(riv, new FrameLayout.LayoutParams(-1, -1));
        riv.setBackgroundColor(color);
    }

    public boolean handleRemoteInputClick(View view, SliceItem action, RemoteInput input) {
        if (input == null) {
            return false;
        }
        RemoteInputView riv = null;
        for (ViewParent p = view.getParent().getParent(); p != null; p = p.getParent()) {
            if (p instanceof View) {
                View pv = (View) p;
                riv = findRemoteInputView(pv);
                if (riv != null) {
                    break;
                }
            }
        }
        if (riv == null) {
            return false;
        }
        int width = view.getWidth();
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            if (tv.getLayout() != null) {
                int innerWidth = (int) tv.getLayout().getLineWidth(0);
                width = Math.min(width, innerWidth + tv.getCompoundPaddingLeft() + tv.getCompoundPaddingRight());
            }
        }
        int cx = view.getLeft() + (width / 2);
        int cy = view.getTop() + (view.getHeight() / 2);
        int w = riv.getWidth();
        int h = riv.getHeight();
        int r = Math.max(Math.max(cx + cy, (h - cy) + cx), Math.max((w - cx) + cy, (w - cx) + (h - cy)));
        riv.setRevealParameters(cx, cy, r);
        riv.setAction(action);
        riv.setRemoteInput(new RemoteInput[]{input}, input);
        riv.focusAnimated();
        return true;
    }

    private RemoteInputView findRemoteInputView(View v) {
        if (v == null) {
            return null;
        }
        return (RemoteInputView) v.findViewWithTag(RemoteInputView.VIEW_TAG);
    }
}
