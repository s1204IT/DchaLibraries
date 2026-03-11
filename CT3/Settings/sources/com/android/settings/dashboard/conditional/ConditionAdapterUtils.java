package com.android.settings.dashboard.conditional;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardAdapter;

public class ConditionAdapterUtils {
    public static void addDismiss(final RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, 48) {
            @Override
            public boolean onMove(RecyclerView recyclerView2, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView2, RecyclerView.ViewHolder viewHolder) {
                if (viewHolder.getItemViewType() == R.layout.condition_card) {
                    return super.getSwipeDirs(recyclerView2, viewHolder);
                }
                return 0;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                DashboardAdapter adapter = (DashboardAdapter) recyclerView.getAdapter();
                Object item = adapter.getItem(viewHolder.getItemId());
                if (!(item instanceof Condition)) {
                    return;
                }
                ((Condition) item).silence();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public static void bindViews(final Condition condition, DashboardAdapter.DashboardItemHolder view, boolean isExpanded, View.OnClickListener onClickListener, View.OnClickListener onExpandListener) {
        View card = view.itemView.findViewById(R.id.content);
        card.setTag(condition);
        card.setOnClickListener(onClickListener);
        view.icon.setImageIcon(condition.getIcon());
        view.title.setText(condition.getTitle());
        ImageView expand = (ImageView) view.itemView.findViewById(R.id.expand_indicator);
        expand.setTag(condition);
        expand.setImageResource(isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        expand.setContentDescription(expand.getContext().getString(isExpanded ? R.string.condition_expand_hide : R.string.condition_expand_show));
        expand.setOnClickListener(onExpandListener);
        View detailGroup = view.itemView.findViewById(R.id.detail_group);
        CharSequence[] actions = condition.getActions();
        if (isExpanded != (detailGroup.getVisibility() == 0)) {
            animateChange(view.itemView, view.itemView.findViewById(R.id.content), detailGroup, isExpanded, actions.length > 0);
        }
        if (!isExpanded) {
            return;
        }
        view.summary.setText(condition.getSummary());
        int i = 0;
        while (i < 2) {
            Button button = (Button) detailGroup.findViewById(i == 0 ? R.id.first_action : R.id.second_action);
            if (actions.length > i) {
                button.setVisibility(0);
                button.setText(actions[i]);
                final int index = i;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MetricsLogger.action(v.getContext(), 376, condition.getMetricsConstant());
                        condition.onActionClick(index);
                    }
                });
            } else {
                button.setVisibility(8);
            }
            i++;
        }
    }

    private static void animateChange(View view, final View content, final View detailGroup, final boolean visible, boolean hasButtons) {
        setViewVisibility(detailGroup, R.id.divider, hasButtons);
        setViewVisibility(detailGroup, R.id.buttonBar, hasButtons);
        final int beforeBottom = content.getBottom();
        setHeight(detailGroup, visible ? -2 : 0);
        detailGroup.setVisibility(0);
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int afterBottom = content.getBottom();
                v.removeOnLayoutChangeListener(this);
                ObjectAnimator animator = ObjectAnimator.ofInt(content, "bottom", beforeBottom, afterBottom);
                animator.setDuration(250L);
                final boolean z = visible;
                final View view2 = detailGroup;
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (z) {
                            return;
                        }
                        view2.setVisibility(8);
                    }
                });
                animator.start();
            }
        });
    }

    private static void setHeight(View detailGroup, int height) {
        ViewGroup.LayoutParams params = detailGroup.getLayoutParams();
        params.height = height;
        detailGroup.setLayoutParams(params);
    }

    private static void setViewVisibility(View containerView, int viewId, boolean visible) {
        View view = containerView.findViewById(viewId);
        if (view == null) {
            return;
        }
        view.setVisibility(visible ? 0 : 8);
    }
}
