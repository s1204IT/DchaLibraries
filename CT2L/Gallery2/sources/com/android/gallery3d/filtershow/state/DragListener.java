package com.android.gallery3d.filtershow.state;

import android.support.v4.app.FragmentManagerImpl;
import android.view.DragEvent;
import android.view.View;
import android.widget.ArrayAdapter;

class DragListener implements View.OnDragListener {
    private static float sSlope = 0.2f;
    private PanelTrack mStatePanelTrack;

    public DragListener(PanelTrack statePanelTrack) {
        this.mStatePanelTrack = statePanelTrack;
    }

    private void setState(DragEvent event) {
        float translation = event.getY() - this.mStatePanelTrack.getTouchPoint().y;
        float alpha = 1.0f - (Math.abs(translation) / this.mStatePanelTrack.getCurrentView().getHeight());
        if (this.mStatePanelTrack.getOrientation() == 1) {
            float translation2 = event.getX() - this.mStatePanelTrack.getTouchPoint().x;
            alpha = 1.0f - (Math.abs(translation2) / this.mStatePanelTrack.getCurrentView().getWidth());
            this.mStatePanelTrack.getCurrentView().setTranslationX(translation2);
        } else {
            this.mStatePanelTrack.getCurrentView().setTranslationY(translation);
        }
        this.mStatePanelTrack.getCurrentView().setBackgroundAlpha(alpha);
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch (event.getAction()) {
            case 1:
            case 3:
            default:
                return true;
            case 2:
                if (this.mStatePanelTrack.getCurrentView() != null) {
                    setState(event);
                    View over = this.mStatePanelTrack.findChildAt((int) event.getX(), (int) event.getY());
                    if (over != null && over != this.mStatePanelTrack.getCurrentView()) {
                        StateView stateView = (StateView) over;
                        if (stateView != this.mStatePanelTrack.getCurrentView()) {
                            int pos = this.mStatePanelTrack.findChild(over);
                            int origin = this.mStatePanelTrack.findChild(this.mStatePanelTrack.getCurrentView());
                            ArrayAdapter array = (ArrayAdapter) this.mStatePanelTrack.getAdapter();
                            if (origin != -1 && pos != -1) {
                                State current = (State) array.getItem(origin);
                                array.remove(current);
                                array.insert(current, pos);
                                this.mStatePanelTrack.fillContent(false);
                                this.mStatePanelTrack.setCurrentView(this.mStatePanelTrack.getChildAt(pos));
                            }
                        }
                    }
                }
                return true;
            case 4:
                if (this.mStatePanelTrack.getCurrentView() != null && this.mStatePanelTrack.getCurrentView().getAlpha() > sSlope) {
                    setState(event);
                }
                this.mStatePanelTrack.checkEndState();
                return true;
            case 5:
                this.mStatePanelTrack.setExited(false);
                if (this.mStatePanelTrack.getCurrentView() != null) {
                    this.mStatePanelTrack.getCurrentView().setVisibility(0);
                }
                return true;
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                if (this.mStatePanelTrack.getCurrentView() != null) {
                    setState(event);
                    this.mStatePanelTrack.getCurrentView().setVisibility(4);
                }
                this.mStatePanelTrack.setExited(true);
                return true;
        }
    }
}
