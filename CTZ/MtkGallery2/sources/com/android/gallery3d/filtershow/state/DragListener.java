package com.android.gallery3d.filtershow.state;

import android.view.DragEvent;
import android.view.View;
import android.widget.ArrayAdapter;

class DragListener implements View.OnDragListener {
    private static float sSlope = 0.2f;
    private PanelTrack mStatePanelTrack;

    public DragListener(PanelTrack panelTrack) {
        this.mStatePanelTrack = panelTrack;
    }

    private void setState(DragEvent dragEvent) {
        float y = dragEvent.getY() - this.mStatePanelTrack.getTouchPoint().y;
        float fAbs = 1.0f - (Math.abs(y) / this.mStatePanelTrack.getCurrentView().getHeight());
        if (this.mStatePanelTrack.getOrientation() == 1) {
            float x = dragEvent.getX() - this.mStatePanelTrack.getTouchPoint().x;
            fAbs = 1.0f - (Math.abs(x) / this.mStatePanelTrack.getCurrentView().getWidth());
            this.mStatePanelTrack.getCurrentView().setTranslationX(x);
        } else {
            this.mStatePanelTrack.getCurrentView().setTranslationY(y);
        }
        this.mStatePanelTrack.getCurrentView().setBackgroundAlpha(fAbs);
    }

    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {
        switch (dragEvent.getAction()) {
            case 1:
            case 3:
            default:
                return true;
            case 2:
                if (this.mStatePanelTrack.getCurrentView() != null) {
                    setState(dragEvent);
                    View viewFindChildAt = this.mStatePanelTrack.findChildAt((int) dragEvent.getX(), (int) dragEvent.getY());
                    if (viewFindChildAt != null && viewFindChildAt != this.mStatePanelTrack.getCurrentView() && ((StateView) viewFindChildAt) != this.mStatePanelTrack.getCurrentView()) {
                        int iFindChild = this.mStatePanelTrack.findChild(viewFindChildAt);
                        int iFindChild2 = this.mStatePanelTrack.findChild(this.mStatePanelTrack.getCurrentView());
                        ArrayAdapter arrayAdapter = (ArrayAdapter) this.mStatePanelTrack.getAdapter();
                        if (iFindChild2 != -1 && iFindChild != -1) {
                            State state = (State) arrayAdapter.getItem(iFindChild2);
                            arrayAdapter.remove(state);
                            arrayAdapter.insert(state, iFindChild);
                            this.mStatePanelTrack.fillContent(false);
                            this.mStatePanelTrack.setCurrentView(this.mStatePanelTrack.getChildAt(iFindChild));
                        }
                    }
                }
                return true;
            case 4:
                if (this.mStatePanelTrack.getCurrentView() != null && this.mStatePanelTrack.getCurrentView().getAlpha() > sSlope) {
                    setState(dragEvent);
                }
                this.mStatePanelTrack.checkEndState();
                return true;
            case 5:
                this.mStatePanelTrack.setExited(false);
                if (this.mStatePanelTrack.getCurrentView() != null) {
                    this.mStatePanelTrack.getCurrentView().setVisibility(0);
                }
                return true;
            case 6:
                if (this.mStatePanelTrack.getCurrentView() != null) {
                    setState(dragEvent);
                    this.mStatePanelTrack.getCurrentView().setVisibility(4);
                }
                this.mStatePanelTrack.setExited(true);
                return true;
        }
    }
}
