package com.android.launcher3;

import com.android.launcher3.Workspace;

class TransitionStates {
    final boolean allAppsToWorkspace;
    final boolean oldStateIsNormal;
    final boolean oldStateIsNormalHidden;
    final boolean oldStateIsOverview;
    final boolean oldStateIsOverviewHidden;
    final boolean oldStateIsSpringLoaded;
    final boolean overviewToAllApps;
    final boolean overviewToWorkspace;
    final boolean stateIsNormal;
    final boolean stateIsNormalHidden;
    final boolean stateIsOverview;
    final boolean stateIsOverviewHidden;
    final boolean stateIsSpringLoaded;
    final boolean workspaceToAllApps;
    final boolean workspaceToOverview;

    public TransitionStates(Workspace.State fromState, Workspace.State toState) {
        this.oldStateIsNormal = fromState == Workspace.State.NORMAL;
        this.oldStateIsSpringLoaded = fromState == Workspace.State.SPRING_LOADED;
        this.oldStateIsNormalHidden = fromState == Workspace.State.NORMAL_HIDDEN;
        this.oldStateIsOverviewHidden = fromState == Workspace.State.OVERVIEW_HIDDEN;
        this.oldStateIsOverview = fromState == Workspace.State.OVERVIEW;
        this.stateIsNormal = toState == Workspace.State.NORMAL;
        this.stateIsSpringLoaded = toState == Workspace.State.SPRING_LOADED;
        this.stateIsNormalHidden = toState == Workspace.State.NORMAL_HIDDEN;
        this.stateIsOverviewHidden = toState == Workspace.State.OVERVIEW_HIDDEN;
        this.stateIsOverview = toState == Workspace.State.OVERVIEW;
        this.workspaceToOverview = this.oldStateIsNormal ? this.stateIsOverview : false;
        this.workspaceToAllApps = this.oldStateIsNormal ? this.stateIsNormalHidden : false;
        this.overviewToWorkspace = this.oldStateIsOverview ? this.stateIsNormal : false;
        this.overviewToAllApps = this.oldStateIsOverview ? this.stateIsOverviewHidden : false;
        this.allAppsToWorkspace = this.stateIsNormalHidden ? this.stateIsNormal : false;
    }
}
