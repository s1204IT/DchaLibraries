package android.filterfw.core;

public class SimpleScheduler extends Scheduler {
    public SimpleScheduler(FilterGraph filterGraph) {
        super(filterGraph);
    }

    @Override
    public void reset() {
    }

    @Override
    public Filter scheduleNextNode() {
        for (Filter filter : getGraph().getFilters()) {
            if (filter.canProcess()) {
                return filter;
            }
        }
        return null;
    }
}
