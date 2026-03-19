package org.junit.experimental.categories;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.experimental.categories.Categories;
import org.junit.runner.FilterFactory;
import org.junit.runner.FilterFactoryParams;
import org.junit.runner.manipulation.Filter;

public final class ExcludeCategories extends CategoryFilterFactory {
    @Override
    public Filter createFilter(FilterFactoryParams filterFactoryParams) throws FilterFactory.FilterNotCreatedException {
        return super.createFilter(filterFactoryParams);
    }

    @Override
    protected Filter createFilter(List<Class<?>> list) {
        return new ExcludesAny(list);
    }

    private static class ExcludesAny extends Categories.CategoryFilter {
        public ExcludesAny(List<Class<?>> list) {
            this(new HashSet(list));
        }

        public ExcludesAny(Set<Class<?>> set) {
            super(true, null, true, set);
        }

        @Override
        public String describe() {
            return "excludes " + super.describe();
        }
    }
}
