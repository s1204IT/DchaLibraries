package junit.runner;

public class ReloadingTestSuiteLoader implements TestSuiteLoader {
    @Override
    public Class load(String suiteClassName) throws ClassNotFoundException {
        return createLoader().loadClass(suiteClassName, true);
    }

    @Override
    public Class reload(Class aClass) throws ClassNotFoundException {
        return createLoader().loadClass(aClass.getName(), true);
    }

    protected TestCaseClassLoader createLoader() {
        return new TestCaseClassLoader();
    }
}
