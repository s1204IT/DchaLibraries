package com.android.systemui.recents;

public class RecentsActivityLaunchState {
    public boolean launchedFromApp;
    public boolean launchedFromHome;
    public boolean launchedFromPipApp;
    public int launchedNumVisibleTasks;
    public int launchedNumVisibleThumbnails;
    public int launchedToTaskId;
    public boolean launchedViaDockGesture;
    public boolean launchedViaDragGesture;
    public boolean launchedWithAltTab;
    public boolean launchedWithNextPipApp;

    public void reset() {
        this.launchedFromHome = false;
        this.launchedFromApp = false;
        this.launchedFromPipApp = false;
        this.launchedWithNextPipApp = false;
        this.launchedToTaskId = -1;
        this.launchedWithAltTab = false;
        this.launchedViaDragGesture = false;
        this.launchedViaDockGesture = false;
    }
}
