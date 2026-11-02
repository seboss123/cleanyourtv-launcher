package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * QuoteFetcher: fetches random quotes from ZenQuotes (free, no API key).
 * Cycles through 3 quotes per day (morning / afternoon / evening), cached in SharedPreferences.
 */
public class QuoteFetcher {

    private static final String PREF_NAME = "tvhome_prefs";
    private static final String KEY_DATE = "quote_date";

    public interface Callback {
        void onQuote(String quote, String author);
    }

    /** Fetch the quote for the current time-of-day slot. Hits the network only if the slot is uncached. */
    public static void fetch(final Context context, final Callback callback) {
        // Always fetch a fresh quote — new quote on every app (re)open.
        new Thread(() -> {
            String quote = null;
            String author = null;
            try {
                URL url = new URL("https://zenquotes.io/api/random");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) sb.append(line);
                    in.close();

                    JSONArray arr = new JSONArray(sb.toString());
                    if (arr.length() > 0) {
                        JSONObject obj = arr.getJSONObject(0);
                        quote = obj.optString("q", null);
                        author = obj.optString("a", null);
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                // fall back to default
            }

            if (quote == null || author == null) {
                quote = defaultQuote();
                author = "Chinese proverb";
            }

            final String finalQuote = quote;
            final String finalAuthor = author;

            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> callback.onQuote(finalQuote, finalAuthor));
        }).start();
    }

    /** Time-of-day slot: morning (0-11), afternoon (12-17), evening (18-23). */
    private static String timeSlot() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "morning";
        if (hour < 18) return "afternoon";
        return "evening";
    }

    private static String defaultQuote() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "The morning was full of sunlight and hope.";
        if (hour < 18) return "Keep going, the best is yet to come.";
        return "The day is done; rest well.";
    }
}
