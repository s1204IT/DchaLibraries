package com.android.documentsui;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import com.android.documentsui.ItemDragListener.DragHost;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Timer;
import java.util.TimerTask;

public class ItemDragListener<H extends DragHost> implements View.OnDragListener {

    @VisibleForTesting
    static final int DEFAULT_SPRING_TIMEOUT = 1500;
    protected final H mDragHost;
    private final Timer mHoverTimer;
    private final int mSpringTimeout;

    public interface DragHost {
        void onDragEnded();

        void onDragEntered(View view);

        void onDragExited(View view);

        void onViewHovered(View view);

        void runOnUiThread(Runnable runnable);

        void setDropTargetHighlight(View view, boolean z);
    }

    public ItemDragListener(H h) {
        this(h, new Timer(), DEFAULT_SPRING_TIMEOUT);
    }

    public ItemDragListener(H h, int i) {
        this(h, new Timer(), i);
    }

    @VisibleForTesting
    protected ItemDragListener(H h, Timer timer, int i) {
        this.mDragHost = h;
        this.mHoverTimer = timer;
        this.mSpringTimeout = i;
    }

    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {
        switch (dragEvent.getAction()) {
            case 2:
                handleLocationEvent(view, dragEvent.getX(), dragEvent.getY());
                break;
            case 4:
                this.mDragHost.onDragEnded();
                handleExitedEndedEvent(view, dragEvent);
                break;
            case 5:
                handleEnteredEvent(view, dragEvent);
                break;
            case 6:
                this.mDragHost.onDragExited(view);
                handleExitedEndedEvent(view, dragEvent);
                break;
        }
        return true;
    }

    private void handleEnteredEvent(View view, DragEvent dragEvent) {
        this.mDragHost.onDragEntered(view);
        TimerTask timerTaskCreateOpenTask = createOpenTask(view, dragEvent);
        this.mDragHost.setDropTargetHighlight(view, true);
        if (timerTaskCreateOpenTask == null) {
            return;
        }
        view.setTag(R.id.drag_hovering_tag, timerTaskCreateOpenTask);
        this.mHoverTimer.schedule(timerTaskCreateOpenTask, this.mSpringTimeout);
    }

    private void handleLocationEvent(View view, float f, float f2) {
        Drawable background = view.getBackground();
        if (background != null) {
            background.setHotspot(f, f2);
        }
    }

    private void handleExitedEndedEvent(View view, DragEvent dragEvent) {
        this.mDragHost.setDropTargetHighlight(view, false);
        TimerTask timerTask = (TimerTask) view.getTag(R.id.drag_hovering_tag);
        if (timerTask != null) {
            timerTask.cancel();
        }
    }

    private boolean handleDropEvent(View view, DragEvent dragEvent) {
        if (dragEvent.getClipData() == null) {
            Log.w("ItemDragListener", "Received invalid drop event with null clipdata. Ignoring.");
            return false;
        }
        return handleDropEventChecked(view, dragEvent);
    }

    class AnonymousClass1 extends TimerTask {
        final View val$v;

        AnonymousClass1(View view) {
            this.val$v = view;
        }

        @Override
        public void run() {
            H h = ItemDragListener.this.mDragHost;
            final View view = this.val$v;
            h.runOnUiThread(new Runnable() {
                @Override
                public final void run() {
                    ItemDragListener.this.mDragHost.onViewHovered(view);
                }
            });
        }
    }

    public TimerTask createOpenTask(View view, DragEvent dragEvent) {
        return new AnonymousClass1(view);
    }

    public boolean handleDropEventChecked(View view, DragEvent dragEvent) {
        return false;
    }
}
