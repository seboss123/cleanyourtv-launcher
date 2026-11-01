package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetches current weather from Open-Meteo API (free, no key required).
 * Caches the result to avoid excessive network calls.
 */
public class WeatherFetcher {
    private static final String TAG = "TvHome.Weather";
    private static final String PREFS = "weather_cache";
    private static final String KEY_CONDITION = "condition";
    private static final String KEY_TEMP = "temp";
    private static final String KEY_TIME = "time";
    private static final long CACHE_MS = 30 * 60 * 1000; // 30 minutes
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public static final String CONDITION_CLEAR = "clear";
    public static final String CONDITION_CLOUDS = "clouds";
    public static final String CONDITION_RAIN = "rain";
    public static final String CONDITION_SNOW = "snow";
    public static final String CONDITION_FOG = "fog";
    public static final String CONDITION_DRIZZLE = "drizzle";
    public static final String CONDITION_THUNDERSTORM = "thunderstorm";
    
    public interface Callback {
        void onWeather(String condition, double temperature);
    }
    
    public static void fetch(Context context, Callback callback) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long cachedTime = prefs.getLong(KEY_TIME, 0);
        if (System.currentTimeMillis() - cachedTime < CACHE_MS) {
            String condition = prefs.getString(KEY_CONDITION, CONDITION_CLEAR);
            float temp = prefs.getFloat(KEY_TEMP, 0);
            callback.onWeather(condition, temp);
            return;
        }
        
        String lat = context.getString(R.string.latitude);
        String lon = context.getString(R.string.longitude);
        if (lat.isEmpty() || lon.isEmpty()) {
            callback.onWeather(CONDITION_CLEAR, 0);
            return;
        }
        
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + 
                     "&longitude=" + lon + "&current_weather=true";
        
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL request = new URL(url);
                conn = (HttpURLConnection) request.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                
                JSONObject json = new JSONObject(sb.toString());
                JSONObject current = json.getJSONObject("current_weather");
                int code = current.getInt("weathercode");
                double temp = current.getDouble("temperature");
                
                String condition = mapCode(code);
                
                prefs.edit()
                    .putString(KEY_CONDITION, condition)
                    .putFloat(KEY_TEMP, (float) temp)
                    .putLong(KEY_TIME, System.currentTimeMillis())
                    .apply();
                
                callback.onWeather(condition, temp);
            } catch (Exception e) {
                Log.e(TAG, "Weather fetch failed: " + e.getMessage());
                String condition = prefs.getString(KEY_CONDITION, CONDITION_CLEAR);
                float temp = prefs.getFloat(KEY_TEMP, 0);
                callback.onWeather(condition, temp);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
    
    private static String mapCode(int code) {
        if (code == 0) return CONDITION_CLEAR;
        if (code <= 3) return CONDITION_CLOUDS;
        if (code <= 48) return CONDITION_FOG;
        if (code <= 55) return CONDITION_DRIZZLE;
        if (code <= 65) return CONDITION_RAIN;
        if (code <= 75) return CONDITION_SNOW;
        if (code <= 82) return CONDITION_RAIN;
        if (code >= 95) return CONDITION_THUNDERSTORM;
        return CONDITION_CLEAR;
    }
    
    public static String getCachedCondition(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CONDITION, CONDITION_CLEAR);
    }
}
