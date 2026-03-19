package java.util.prefs;

class FileSystemPreferencesFactory implements PreferencesFactory {
    FileSystemPreferencesFactory() {
    }

    @Override
    public Preferences userRoot() {
        return FileSystemPreferences.getUserRoot();
    }

    @Override
    public Preferences systemRoot() {
        return FileSystemPreferences.getSystemRoot();
    }
}
