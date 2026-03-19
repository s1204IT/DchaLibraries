package android.filterfw.core;

import java.util.Set;

public class RoundRobinScheduler extends Scheduler {
    private int mLastPos;

    public RoundRobinScheduler(FilterGraph filterGraph) {
        super(filterGraph);
        this.mLastPos = -1;
    }

    @Override
    public void reset() {
        this.mLastPos = -1;
    }

    @Override
    public Filter scheduleNextNode() {
        Set<Filter> filters = getGraph().getFilters();
        if (this.mLastPos >= filters.size()) {
            this.mLastPos = -1;
        }
        int i = -1;
        int i2 = 0;
        Filter filter = null;
        for (Filter filter2 : filters) {
            if (filter2.canProcess()) {
                if (i2 <= this.mLastPos) {
                    if (filter == null) {
                        i = i2;
                        filter = filter2;
                    }
                } else {
                    this.mLastPos = i2;
                    return filter2;
                }
            }
            i2++;
        }
        if (filter == null) {
            return null;
        }
        this.mLastPos = i;
        return filter;
    }
}
