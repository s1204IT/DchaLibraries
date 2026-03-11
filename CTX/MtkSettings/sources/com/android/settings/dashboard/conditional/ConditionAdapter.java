package com.android.settings.dashboard.conditional;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import java.util.List;
import java.util.Objects;

public class ConditionAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardItemHolder> {
    private List<Condition> mConditions;
    private final Context mContext;
    private boolean mExpanded;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private View.OnClickListener mConditionClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Condition condition = (Condition) view.getTag();
            ConditionAdapter.this.mMetricsFeatureProvider.action(ConditionAdapter.this.mContext, 375, condition.getMetricsConstant());
            condition.onPrimaryClick();
        }
    };
    ItemTouchHelper.SimpleCallback mSwipeCallback = new ItemTouchHelper.SimpleCallback(0, 48) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2) {
            return true;
        }

        @Override
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.getItemViewType() == R.layout.condition_tile) {
                return super.getSwipeDirs(recyclerView, viewHolder);
            }
            return 0;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
            Object item = ConditionAdapter.this.getItem(viewHolder.getItemId());
            if (item != null) {
                ((Condition) item).silence();
            }
        }
    };

    public ConditionAdapter(Context context, List<Condition> list, boolean z) {
        this.mContext = context;
        this.mConditions = list;
        this.mExpanded = z;
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        setHasStableIds(true);
    }

    public Object getItem(long j) {
        for (Condition condition : this.mConditions) {
            if (Objects.hash(condition.getTitle()) == j) {
                return condition;
            }
        }
        return null;
    }

    @Override
    public DashboardAdapter.DashboardItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new DashboardAdapter.DashboardItemHolder(LayoutInflater.from(viewGroup.getContext()).inflate(i, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(DashboardAdapter.DashboardItemHolder dashboardItemHolder, int i) {
        bindViews(this.mConditions.get(i), dashboardItemHolder, i == this.mConditions.size() - 1, this.mConditionClickListener);
    }

    @Override
    public long getItemId(int i) {
        return Objects.hash(this.mConditions.get(i).getTitle());
    }

    @Override
    public int getItemViewType(int i) {
        return R.layout.condition_tile;
    }

    @Override
    public int getItemCount() {
        if (this.mExpanded) {
            return this.mConditions.size();
        }
        return 0;
    }

    public void addDismissHandling(RecyclerView recyclerView) {
        new ItemTouchHelper(this.mSwipeCallback).attachToRecyclerView(recyclerView);
    }

    private void bindViews(final Condition condition, DashboardAdapter.DashboardItemHolder dashboardItemHolder, boolean z, View.OnClickListener onClickListener) {
        if (condition instanceof AirplaneModeCondition) {
            Log.d("ConditionAdapter", "Airplane mode condition has been bound with isActive=" + condition.isActive() + ". Airplane mode is currently " + WirelessUtils.isAirplaneModeOn(condition.mManager.getContext()));
        }
        View viewFindViewById = dashboardItemHolder.itemView.findViewById(R.id.content);
        viewFindViewById.setTag(condition);
        viewFindViewById.setOnClickListener(onClickListener);
        dashboardItemHolder.icon.setImageDrawable(condition.getIcon());
        dashboardItemHolder.title.setText(condition.getTitle());
        CharSequence[] actions = condition.getActions();
        setViewVisibility(dashboardItemHolder.itemView, R.id.buttonBar, actions.length > 0);
        dashboardItemHolder.summary.setText(condition.getSummary());
        final int i = 0;
        while (i < 2) {
            Button button = (Button) dashboardItemHolder.itemView.findViewById(i == 0 ? R.id.first_action : R.id.second_action);
            if (actions.length > i) {
                button.setVisibility(0);
                button.setText(actions[i]);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Context context = view.getContext();
                        FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context, 376, condition.getMetricsConstant());
                        condition.onActionClick(i);
                    }
                });
            } else {
                button.setVisibility(8);
            }
            i++;
        }
        setViewVisibility(dashboardItemHolder.itemView, R.id.divider, !z);
    }

    private void setViewVisibility(View view, int i, boolean z) {
        View viewFindViewById = view.findViewById(i);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(z ? 0 : 8);
        }
    }
}
