package com.android.documentsui.dirlist;

import android.graphics.Point;
import android.view.DragEvent;
import android.view.View;
import com.android.documentsui.ItemDragListener;
import com.android.documentsui.selection.ViewAutoScroller;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

class DragHoverListener implements View.OnDragListener {
    private final BooleanSupplier mCanScrollDown;
    private final BooleanSupplier mCanScrollUp;
    private Point mCurrentPosition;
    private final ItemDragListener<? extends ItemDragListener.DragHost> mDragHandler;
    private boolean mDragHappening;
    private final Runnable mDragScroller;
    private final IntSupplier mHeight;
    private final Predicate<View> mIsScrollView;

    DragHoverListener(ItemDragListener<? extends ItemDragListener.DragHost> itemDragListener, IntSupplier intSupplier, Predicate<View> predicate, BooleanSupplier booleanSupplier, BooleanSupplier booleanSupplier2, ViewAutoScroller.ScrollerCallbacks scrollerCallbacks) {
        this.mDragHandler = itemDragListener;
        this.mHeight = intSupplier;
        this.mIsScrollView = predicate;
        this.mCanScrollUp = booleanSupplier;
        this.mCanScrollDown = booleanSupplier2;
        this.mDragScroller = new ViewAutoScroller(new ViewAutoScroller.ScrollHost() {
            @Override
            public Point getCurrentPosition() {
                return DragHoverListener.this.mCurrentPosition;
            }

            @Override
            public int getViewHeight() {
                return DragHoverListener.this.mHeight.getAsInt();
            }

            @Override
            public boolean isActive() {
                return DragHoverListener.this.mDragHappening;
            }
        }, scrollerCallbacks);
    }

    static DragHoverListener create(ItemDragListener<? extends ItemDragListener.DragHost> itemDragListener, final View view) {
        ViewAutoScroller.ScrollerCallbacks scrollerCallbacks = new ViewAutoScroller.ScrollerCallbacks() {
            @Override
            public void scrollBy(int i) {
                view.scrollBy(0, i);
            }

            @Override
            public void runAtNextFrame(Runnable runnable) {
                view.postOnAnimation(runnable);
            }

            @Override
            public void removeCallback(Runnable runnable) {
                view.removeCallbacks(runnable);
            }
        };
        Objects.requireNonNull(view);
        return new DragHoverListener(itemDragListener, new IntSupplier() {
            @Override
            public final int getAsInt() {
                return view.getHeight();
            }
        }, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DragHoverListener.lambda$create$0(view, (View) obj);
            }
        }, new BooleanSupplier() {
            @Override
            public final boolean getAsBoolean() {
                return view.canScrollVertically(-1);
            }
        }, new BooleanSupplier() {
            @Override
            public final boolean getAsBoolean() {
                return view.canScrollVertically(1);
            }
        }, scrollerCallbacks);
    }

    static boolean lambda$create$0(View view, View view2) {
        return view == view2;
    }

    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {
        int action = dragEvent.getAction();
        if (action != 4) {
            switch (action) {
                case 1:
                    this.mDragHappening = true;
                    break;
                case 2:
                    handleLocationEvent(view, dragEvent.getX(), dragEvent.getY());
                    break;
            }
        } else {
            this.mDragHappening = false;
        }
        return this.mDragHandler.onDrag(view, dragEvent);
    }

    private boolean handleLocationEvent(View view, float f, float f2) {
        this.mCurrentPosition = transformToScrollViewCoordinate(view, f, f2);
        if (insideDragZone()) {
            this.mDragScroller.run();
            return true;
        }
        return false;
    }

    private Point transformToScrollViewCoordinate(View view, float f, float f2) {
        float x;
        boolean zTest = this.mIsScrollView.test(view);
        if (!zTest) {
            x = view.getX();
        } else {
            x = 0.0f;
        }
        return new Point(Math.round(x + f), Math.round((zTest ? 0.0f : view.getY()) + f2));
    }

    private boolean insideDragZone() {
        if (this.mCurrentPosition == null) {
            return false;
        }
        float asInt = this.mHeight.getAsInt() * 0.125f;
        return ((((float) this.mCurrentPosition.y) > asInt ? 1 : (((float) this.mCurrentPosition.y) == asInt ? 0 : -1)) < 0 && this.mCanScrollUp.getAsBoolean()) || ((((float) this.mCurrentPosition.y) > (((float) this.mHeight.getAsInt()) - asInt) ? 1 : (((float) this.mCurrentPosition.y) == (((float) this.mHeight.getAsInt()) - asInt) ? 0 : -1)) > 0 && this.mCanScrollDown.getAsBoolean());
    }
}
