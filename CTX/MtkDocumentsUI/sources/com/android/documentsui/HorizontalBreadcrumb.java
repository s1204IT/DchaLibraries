package com.android.documentsui;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.documentsui.ItemDragListener;
import com.android.documentsui.NavigationViewManager;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.State;
import com.android.documentsui.dirlist.AccessibilityEventRouter;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

public final class HorizontalBreadcrumb extends RecyclerView implements ItemDragListener.DragHost, NavigationViewManager.Breadcrumb {
    private BreadcrumbAdapter mAdapter;
    private IntConsumer mClickListener;
    private LinearLayoutManager mLayoutManager;

    public HorizontalBreadcrumb(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public HorizontalBreadcrumb(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public HorizontalBreadcrumb(Context context) {
        super(context);
    }

    @Override
    public void setup(NavigationViewManager.Environment environment, State state, IntConsumer intConsumer) {
        this.mClickListener = intConsumer;
        this.mLayoutManager = new LinearLayoutManager(getContext(), 0, false);
        this.mAdapter = new BreadcrumbAdapter(state, environment, new ItemDragListener(this), new View.OnKeyListener() {
            @Override
            public final boolean onKey(View view, int i, KeyEvent keyEvent) {
                return this.f$0.onKey(view, i, keyEvent);
            }
        });
        setAccessibilityDelegateCompat(new AccessibilityEventRouter(this, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Boolean.valueOf(this.f$0.onAccessibilityClick((View) obj));
            }
        }));
        setLayoutManager(this.mLayoutManager);
        addOnItemTouchListener(new ClickListener(getContext(), new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.onSingleTapUp((MotionEvent) obj);
            }
        }));
    }

    @Override
    public void show(boolean z) {
        if (z) {
            setVisibility(0);
            boolean z2 = !hasUserDefineScrollOffset();
            if (getAdapter() == null) {
                setAdapter(this.mAdapter);
            } else {
                int itemCount = this.mAdapter.getItemCount();
                int lastItemSize = this.mAdapter.getLastItemSize();
                if (itemCount > lastItemSize) {
                    this.mAdapter.notifyItemRangeInserted(lastItemSize, itemCount - lastItemSize);
                    this.mAdapter.notifyItemChanged(lastItemSize - 1);
                } else if (itemCount < lastItemSize) {
                    this.mAdapter.notifyItemRangeRemoved(itemCount, lastItemSize - itemCount);
                    this.mAdapter.notifyItemChanged(itemCount - 1);
                }
            }
            if (z2) {
                this.mLayoutManager.scrollToPosition(this.mAdapter.getItemCount() - 1);
            }
        } else {
            setVisibility(8);
            setAdapter(null);
        }
        this.mAdapter.updateLastItemSize();
    }

    private boolean hasUserDefineScrollOffset() {
        return (computeHorizontalScrollRange() - computeHorizontalScrollExtent()) - computeHorizontalScrollOffset() > 5;
    }

    private boolean onAccessibilityClick(View view) {
        int childAdapterPosition = getChildAdapterPosition(view);
        if (childAdapterPosition != getAdapter().getItemCount() - 1) {
            this.mClickListener.accept(childAdapterPosition);
            return true;
        }
        return false;
    }

    private boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (i == 66) {
            return onAccessibilityClick(view);
        }
        return false;
    }

    @Override
    public void postUpdate() {
    }

    @Override
    public void runOnUiThread(Runnable runnable) {
        post(runnable);
    }

    @Override
    public void setDropTargetHighlight(View view, boolean z) {
        RecyclerView.ViewHolder childViewHolder = getChildViewHolder(view);
        if (childViewHolder instanceof BreadcrumbHolder) {
            ((BreadcrumbHolder) childViewHolder).setHighlighted(z);
        }
    }

    @Override
    public void onDragEntered(View view) {
    }

    @Override
    public void onDragExited(View view) {
    }

    @Override
    public void onViewHovered(View view) {
        int childAdapterPosition = getChildAdapterPosition(view);
        if (childAdapterPosition != this.mAdapter.getItemCount() - 1) {
            this.mClickListener.accept(childAdapterPosition);
        }
    }

    @Override
    public void onDragEnded() {
    }

    private void onSingleTapUp(MotionEvent motionEvent) {
        int childAdapterPosition = getChildAdapterPosition(findChildViewUnder(motionEvent.getX(), motionEvent.getY()));
        if (childAdapterPosition != this.mAdapter.getItemCount() - 1) {
            this.mClickListener.accept(childAdapterPosition);
        }
    }

    private static final class BreadcrumbAdapter extends RecyclerView.Adapter<BreadcrumbHolder> {
        private final View.OnKeyListener mClickListener;
        private final View.OnDragListener mDragListener;
        private final NavigationViewManager.Environment mEnv;
        private int mLastItemSize;
        private final State mState;

        public BreadcrumbAdapter(State state, NavigationViewManager.Environment environment, View.OnDragListener onDragListener, View.OnKeyListener onKeyListener) {
            this.mState = state;
            this.mEnv = environment;
            this.mDragListener = onDragListener;
            this.mClickListener = onKeyListener;
            this.mLastItemSize = this.mState.stack.size();
        }

        @Override
        public BreadcrumbHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new BreadcrumbHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.navigation_breadcrumb_item, (ViewGroup) null));
        }

        @Override
        public void onBindViewHolder(BreadcrumbHolder breadcrumbHolder, int i) {
            DocumentInfo item = getItem(i);
            int dimension = (int) breadcrumbHolder.itemView.getResources().getDimension(R.dimen.breadcrumb_item_padding);
            if (i == 0) {
                breadcrumbHolder.title.setText(this.mEnv.getCurrentRoot().title);
                breadcrumbHolder.title.setPadding(0, 0, dimension, 0);
            } else {
                breadcrumbHolder.title.setText(item.displayName);
                breadcrumbHolder.title.setPadding(dimension, 0, dimension, 0);
            }
            if (i != getItemCount() - 1) {
                breadcrumbHolder.arrow.setVisibility(0);
            } else {
                breadcrumbHolder.arrow.setVisibility(8);
            }
            breadcrumbHolder.itemView.setOnDragListener(this.mDragListener);
            breadcrumbHolder.itemView.setOnKeyListener(this.mClickListener);
        }

        private DocumentInfo getItem(int i) {
            return this.mState.stack.get(i);
        }

        @Override
        public int getItemCount() {
            return this.mState.stack.size();
        }

        public int getLastItemSize() {
            return this.mLastItemSize;
        }

        public void updateLastItemSize() {
            this.mLastItemSize = this.mState.stack.size();
        }
    }

    private static class BreadcrumbHolder extends RecyclerView.ViewHolder {
        protected ImageView arrow;
        protected DragOverTextView title;

        public BreadcrumbHolder(View view) {
            super(view);
            this.title = (DragOverTextView) view.findViewById(R.id.breadcrumb_text);
            this.arrow = (ImageView) view.findViewById(R.id.breadcrumb_arrow);
        }

        public void setHighlighted(boolean z) {
            this.title.setHighlight(z);
        }
    }

    private static final class ClickListener extends GestureDetector implements RecyclerView.OnItemTouchListener {
        public ClickListener(Context context, final Consumer<MotionEvent> consumer) {
            super(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent motionEvent) {
                    consumer.accept(motionEvent);
                    return true;
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
            onTouchEvent(motionEvent);
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
            onTouchEvent(motionEvent);
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean z) {
        }
    }
}
