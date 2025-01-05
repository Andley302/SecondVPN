package app.one.secondvpnlite.util;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Singleton class para manejar las preferencias usando SharedPreferences.
 **/

/*
 * ==============================
 *          author:staffnetDev
 * ==============================
 */
public class SharedPref {
    private static SharedPref instance;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // Nombre del archivo de preferencias
    private static final String PREF_NAME = "SECONDVPN_PREFS";

    // Claves comunes
    public static final String KEY_SELECTED_APPS = "selectedApps";
    // Puedes agregar más claves aquí según tus necesidades

    /**
     * Constructor privado para implementar el patrón Singleton.
     *
     * @param context Contexto de la aplicación.
     */
    private SharedPref(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * Método para obtener la instancia única de SharedPref.
     *
     * @param context Contexto de la aplicación.
     * @return Instancia única de SharedPref.
     */
    public static synchronized SharedPref getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPref(context);
        }
        return instance;
    }

    // Métodos para manejar Strings
    public void putString(String key, String value) {
        editor.putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    // Métodos para manejar Enteros
    public void putInt(String key, int value) {
        editor.putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    // Métodos para manejar Booleanos
    public void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    // Métodos para manejar Floats
    public void putFloat(String key, float value) {
        editor.putFloat(key, value).apply();
    }

    public float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    // Métodos para manejar Longs
    public void putLong(String key, long value) {
        editor.putLong(key, value).apply();
    }

    public long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    // Métodos para manejar Doubles (no soportados nativamente, se almacenan como Strings)
    public void putDouble(String key, double value) {
        editor.putString(key, Double.toString(value)).apply();
    }

    public double getDouble(String key, double defaultValue) {
        String value = sharedPreferences.getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    // Métodos para manejar Sets de Strings
    public void putStringSet(String key, Set<String> value) {
        editor.putStringSet(key, value).apply();
    }

    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        return sharedPreferences.getStringSet(key, defaultValue);
    }

    // Métodos para remover entradas
    public void remove(String key) {
        editor.remove(key).apply();
    }

    /**
     * Este método elimina una clave específica concatenada con un valor por defecto.
     * Asegúrate de que esto sea lo que realmente deseas hacer.
     *
     * @param key          Clave base.
     * @param defaultValue Valor por defecto a concatenar.
     */
    public void remove(String key, String defaultValue) {
        editor.remove(key + defaultValue).apply();
    }

    // Método para limpiar todas las preferencias
    public void clear() {
        editor.clear().apply();
    }

    /**
     * Método para remover una aplicación específica de la lista de apps seleccionadas.
     *
     * @param appToRemove Nombre del paquete de la app a remover.
     */
    public void removeAppFromList(String appToRemove) {
        // Obtener la lista actual de apps seleccionadas
        Set<String> appList = new HashSet<>(sharedPreferences.getStringSet(KEY_SELECTED_APPS, new HashSet<>()));
        // Remover la app específica
        appList.remove(appToRemove);
        // Guardar la lista actualizada
        editor.putStringSet(KEY_SELECTED_APPS, appList).apply();
    }
}
