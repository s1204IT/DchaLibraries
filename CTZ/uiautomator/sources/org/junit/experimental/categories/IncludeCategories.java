package org.junit.experimental.categories;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.experimental.categories.Categories;
import org.junit.runner.FilterFactory;
import org.junit.runner.FilterFactoryParams;
import org.junit.runner.manipulation.Filter;

public final class IncludeCategories extends CategoryFilterFactory {
    @Override
    public Filter createFilter(FilterFactoryParams filterFactoryParams) throws FilterFactory.FilterNotCreatedException {
        return super.createFilter(filterFactoryParams);
    }

    @Override
    protected Filter createFilter(List<Class<?>> list) {
        return new IncludesAny(list);
    }

    private static class IncludesAny extends Categories.CategoryFilter {
        public IncludesAny(List<Class<?>> list) {
            this(new HashSet(list));
        }

        public IncludesAny(Set<Class<?>> set) {
            super(true, set, true, null);
        }

        @Override
        public String describe() {
            return "includes " + super.describe();
        }
    }
}
