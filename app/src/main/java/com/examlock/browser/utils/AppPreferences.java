package com.examlock.browser.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREF_NAME = "examlock_prefs";
    private static AppPreferences instance;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_EXAM_ACTIVE = "exam_active";
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String KEY_IS_ADMIN = "is_admin";
    private static final String KEY_EXAM_URL = "exam_url";
    private static final String KEY_EXIT_PASSWORD = "exit_password";
    private static final String KEY_STUDENT_PASSWORD = "student_password";
    private static final String KEY_APP_PASSWORD = "app_password";
    private static final String KEY_INSTITUTION_NAME = "institution_name";

    private AppPreferences(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public static synchronized AppPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new AppPreferences(context);
        }
        return instance;
    }

    public void setLoggedIn(boolean loggedIn) {
        editor.putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setExamActive(boolean active) {
        editor.putBoolean(KEY_EXAM_ACTIVE, active).apply();
    }

    public boolean isExamActive() {
        return prefs.getBoolean(KEY_EXAM_ACTIVE, false);
    }

    public void setCurrentUser(String user) {
        editor.putString(KEY_CURRENT_USER, user).apply();
    }

    public String getCurrentUser() {
        return prefs.getString(KEY_CURRENT_USER, "siswa");
    }

    public void setIsAdmin(boolean admin) {
        editor.putBoolean(KEY_IS_ADMIN, admin).apply();
    }

    public boolean isAdmin() {
        return prefs.getBoolean(KEY_IS_ADMIN, false);
    }

    public void setExamUrl(String url) {
        editor.putString(KEY_EXAM_URL, url).apply();
    }

    public String getExamUrl() {
        return prefs.getString(KEY_EXAM_URL, "");
    }

    public void setExitPassword(String password) {
        editor.putString(KEY_EXIT_PASSWORD, password).apply();
    }

    public String getExitPassword() {
        return prefs.getString(KEY_EXIT_PASSWORD, "1234");
    }

    public void setStudentPassword(String password) {
        editor.putString(KEY_STUDENT_PASSWORD, password).apply();
    }

    public String getStudentPassword() {
        return prefs.getString(KEY_STUDENT_PASSWORD, "siswa2024");
    }

    public void setAppPassword(String password) {
        editor.putString(KEY_APP_PASSWORD, password).apply();
    }

    public String getAppPassword() {
        return prefs.getString(KEY_APP_PASSWORD, "admin123");
    }

    public void setInstitutionName(String name) {
        editor.putString(KEY_INSTITUTION_NAME, name).apply();
    }

    public String getInstitutionName() {
        return prefs.getString(KEY_INSTITUTION_NAME, "SINAU Secure Browser");
    }

    public void clearAll() {
        editor.clear().apply();
    }
}
