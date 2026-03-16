package java.util.prefs;

class FilePreferencesFactoryImpl implements PreferencesFactory {
    private static final Preferences USER_ROOT = new FilePreferencesImpl(System.getProperty("user.home") + "/.java/.userPrefs", true);
    private static final Preferences SYSTEM_ROOT = new FilePreferencesImpl(System.getProperty("java.home") + "/.systemPrefs", false);

    @Override
    public Preferences userRoot() {
        return USER_ROOT;
    }

    @Override
    public Preferences systemRoot() {
        return SYSTEM_ROOT;
    }
}
