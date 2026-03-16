package android.filterfw.core;

import java.util.Random;
import java.util.Vector;

public class RandomScheduler extends Scheduler {
    private Random mRand;

    public RandomScheduler(FilterGraph graph) {
        super(graph);
        this.mRand = new Random();
    }

    @Override
    public void reset() {
    }

    @Override
    public Filter scheduleNextNode() {
        Vector<Filter> candidates = new Vector<>();
        for (Filter filter : getGraph().getFilters()) {
            if (filter.canProcess()) {
                candidates.add(filter);
            }
        }
        if (candidates.size() <= 0) {
            return null;
        }
        int r = this.mRand.nextInt(candidates.size());
        return candidates.elementAt(r);
    }
}
