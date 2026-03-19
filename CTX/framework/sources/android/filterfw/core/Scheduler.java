package android.filterfw.core;

public abstract class Scheduler {
    private FilterGraph mGraph;

    abstract void reset();

    abstract Filter scheduleNextNode();

    Scheduler(FilterGraph filterGraph) {
        this.mGraph = filterGraph;
    }

    FilterGraph getGraph() {
        return this.mGraph;
    }

    boolean finished() {
        return true;
    }
}
