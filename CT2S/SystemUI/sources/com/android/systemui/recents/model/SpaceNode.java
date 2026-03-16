package com.android.systemui.recents.model;

import java.util.ArrayList;

public class SpaceNode {
    SpaceNode mEndNode;
    TaskStack mStack;
    SpaceNode mStartNode;

    public void setStack(TaskStack stack) {
        this.mStack = stack;
    }

    public boolean hasTasks() {
        return this.mStack.getTaskCount() > 0 || (this.mStartNode != null && this.mStartNode.hasTasks()) || (this.mEndNode != null && this.mEndNode.hasTasks());
    }

    boolean isLeafNode() {
        return this.mStartNode == null && this.mEndNode == null;
    }

    private void getStacksRec(ArrayList<TaskStack> stacks) {
        if (isLeafNode()) {
            stacks.add(this.mStack);
        } else {
            this.mStartNode.getStacksRec(stacks);
            this.mEndNode.getStacksRec(stacks);
        }
    }

    public ArrayList<TaskStack> getStacks() {
        ArrayList<TaskStack> stacks = new ArrayList<>();
        getStacksRec(stacks);
        return stacks;
    }
}
