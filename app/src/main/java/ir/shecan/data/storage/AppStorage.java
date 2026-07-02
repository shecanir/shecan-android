package ir.shecan.data.storage;

import android.content.Context;

public class AppStorage {

    private static final String Token_KEY = "TOKEN_MODEL";
    private static final String App_Config_KEY = "APP_CONFIG_MODEL";
    private static final String Issue_KEY = "Issue_MODEL";
    private static final String SERVICE_STATUS_KEY = "SERVICE_STATUS_MODEL1";
    private static final String SERVICE_SELECTION_EXPLICIT_KEY = "SERVICE_SELECTION_EXPLICIT";
    private static final String PROFILE_UPDATED_AT_KEY = "PROFILE_UPDATED_AT";
    private static final long PROFILE_SYNC_GRACE_MS = 10_000L;
    private static final String SERVICE_CATALOG_KEY = "SERVICE_CATALOG_MODEL";
    private static final String RATING_STATE_KEY = "RATING_STATE_MODEL100";


    private final GenericPreferenceManager pref;

    public AppStorage(Context context) {
        this.pref = GenericPreferenceManager.getInstance(context);
    }

    // ---------------- User Model ----------------
    public <T> void saveToken(T token) {
        pref.saveModel(Token_KEY, token);
    }

    public <T> T getToken(Class<T> clazz) {
        return pref.getModel(Token_KEY, clazz);
    }

    public void removeToken() {
        pref.remove(Token_KEY);
    }

    // ---------------- App Config ----------------
    public <T> void saveAppConfig(T appConfig) {
        pref.saveModel(App_Config_KEY, appConfig);
    }

    public <T> T getAppConfig(Class<T> clazz) {
        return pref.getModel(App_Config_KEY, clazz);
    }

    public void removeAppConfig() {
        pref.remove(App_Config_KEY);
    }

    // ---------------- Configs ----------------
    public <T> void saveIssues(T issue) {
        pref.saveModel(Issue_KEY, issue);
    }

    public <T> T getIssue(Class<T> clazz) {
        return pref.getModel(Issue_KEY, clazz);
    }

    public void removeIssue() {
        pref.remove(Issue_KEY);
    }

    // ---------------- Service Catalog ----------------
    public <T> void saveServiceCatalog(T catalog) {
        pref.saveModel(SERVICE_CATALOG_KEY, catalog);
    }

    public <T> T getServiceCatalog(Class<T> clazz) {
        return pref.getModel(SERVICE_CATALOG_KEY, clazz);
    }

    public void removeServiceCatalog() {
        pref.remove(SERVICE_CATALOG_KEY);
    }

    // ---------------- Rating State ----------------
    public <T> void saveRatingState(T issue) {
        pref.saveModel(RATING_STATE_KEY, issue);
    }

    public <T> T getRatingState(Class<T> clazz) {
        return pref.getModel(RATING_STATE_KEY, clazz);
    }

    public void removeRatingState() {
        pref.remove(RATING_STATE_KEY);
    }

    // ---------------- Configs ----------------

    public <T> void saveServiceStatus(T issue) {
        pref.saveModel(SERVICE_STATUS_KEY, issue);
    }

    public <T> T getServiceStatus(Class<T> clazz) {
        return pref.getModel(SERVICE_STATUS_KEY, clazz);
    }

    public void removeServiceStatus() {
        pref.remove(SERVICE_STATUS_KEY);
        pref.remove(SERVICE_SELECTION_EXPLICIT_KEY);
    }

    public void prepareServiceSelectionAfterLogin() {
        removeServiceStatus();
        removeIssue();
    }

    public void markServiceSelectionExplicit() {
        pref.saveModel(SERVICE_SELECTION_EXPLICIT_KEY, true);
    }

    public void clearServiceSelectionExplicit() {
        pref.remove(SERVICE_SELECTION_EXPLICIT_KEY);
    }

    public boolean isServiceSelectionExplicit() {
        Boolean explicit = pref.getModel(SERVICE_SELECTION_EXPLICIT_KEY, Boolean.class);
        return Boolean.TRUE.equals(explicit);
    }

    public void markProfileUpdated() {
        pref.saveModel(PROFILE_UPDATED_AT_KEY, System.currentTimeMillis());
    }

    public boolean shouldDeferProfileSync() {
        Long updatedAt = pref.getModel(PROFILE_UPDATED_AT_KEY, Long.class);
        return updatedAt != null
                && System.currentTimeMillis() - updatedAt < PROFILE_SYNC_GRACE_MS;
    }
    // -----------------------------------------------


    // پاک کردن همه چیز
    public void clearAll() {
        pref.clearAll();
    }
}
