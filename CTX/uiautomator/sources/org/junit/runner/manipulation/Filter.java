package org.junit.runner.manipulation;

import java.util.Iterator;
import org.junit.runner.Description;

public abstract class Filter {
    public static final Filter ALL = new Filter() {
        @Override
        public boolean shouldRun(Description description) {
            return true;
        }

        @Override
        public String describe() {
            return "all tests";
        }

        @Override
        public void apply(Object obj) throws NoTestsRemainException {
        }

        @Override
        public Filter intersect(Filter filter) {
            return filter;
        }
    };

    public abstract String describe();

    public abstract boolean shouldRun(Description description);

    public static Filter matchMethodDescription(final Description description) {
        return new Filter() {
            @Override
            public boolean shouldRun(Description description2) {
                if (description2.isTest()) {
                    return description.equals(description2);
                }
                Iterator<Description> it = description2.getChildren().iterator();
                while (it.hasNext()) {
                    if (shouldRun(it.next())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String describe() {
                return String.format("Method %s", description.getDisplayName());
            }
        };
    }

    public void apply(Object obj) throws NoTestsRemainException {
        if (!(obj instanceof Filterable)) {
            return;
        }
        ((Filterable) obj).filter(this);
    }

    public Filter intersect(final Filter filter) {
        if (filter == this || filter == ALL) {
            return this;
        }
        return new Filter() {
            @Override
            public boolean shouldRun(Description description) {
                return this.shouldRun(description) && filter.shouldRun(description);
            }

            @Override
            public String describe() {
                return this.describe() + " and " + filter.describe();
            }
        };
    }
}
