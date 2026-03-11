package com.android.systemui.recents.tv.views;

import android.animation.Animator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.LaunchTvTaskEvent;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.AnimationProps;
import java.util.ArrayList;
import java.util.List;

public class TaskStackHorizontalViewAdapter extends RecyclerView.Adapter<ViewHolder> {
    private TaskStackHorizontalGridView mGridView;
    private List<Task> mTaskList;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Task mTask;
        private TaskCardView mTaskCardView;

        public ViewHolder(View v) {
            super(v);
            this.mTaskCardView = (TaskCardView) v;
        }

        public void init(Task task) {
            this.mTaskCardView.init(task);
            this.mTask = task;
            this.mTaskCardView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            try {
                if (this.mTaskCardView.isInDismissState()) {
                    this.mTaskCardView.startDismissTaskAnimation(getRemoveAtListener(getAdapterPosition(), this.mTaskCardView.getTask()));
                } else {
                    EventBus.getDefault().send(new LaunchTvTaskEvent(this.mTaskCardView, this.mTask, null, -1));
                }
            } catch (Exception e) {
                Log.e("TaskStackViewAdapter", v.getContext().getString(R.string.recents_launch_error_message, this.mTask.title), e);
            }
        }

        private Animator.AnimatorListener getRemoveAtListener(int position, final Task task) {
            return new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    TaskStackHorizontalViewAdapter.this.removeTask(task);
                    EventBus.getDefault().send(new DeleteTaskDataEvent(task));
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            };
        }
    }

    public TaskStackHorizontalViewAdapter(List tasks) {
        this.mTaskList = new ArrayList(tasks);
    }

    public void setNewStackTasks(List tasks) {
        this.mTaskList.clear();
        this.mTaskList.addAll(tasks);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewHolder viewHolder = new ViewHolder(inflater.inflate(R.layout.recents_tv_task_card_view, parent, false));
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = this.mTaskList.get(position);
        Recents.getTaskLoader().loadTaskData(task);
        holder.init(task);
    }

    @Override
    public int getItemCount() {
        return this.mTaskList.size();
    }

    public void removeTask(Task task) {
        int position = this.mTaskList.indexOf(task);
        if (position < 0) {
            return;
        }
        this.mTaskList.remove(position);
        notifyItemRemoved(position);
        if (this.mGridView == null) {
            return;
        }
        this.mGridView.getStack().removeTask(task, AnimationProps.IMMEDIATE, false);
    }

    public int getPositionOfTask(Task task) {
        int position = this.mTaskList.indexOf(task);
        if (position >= 0) {
            return position;
        }
        return 0;
    }

    public void setTaskStackHorizontalGridView(TaskStackHorizontalGridView gridView) {
        this.mGridView = gridView;
    }

    public void addTaskAt(Task task, int position) {
        this.mTaskList.add(position, task);
        notifyItemInserted(position);
    }
}
