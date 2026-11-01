package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * QuoteFetcher: fetches a random quote from ZenQuotes (free, no API key).
 * Cached in SharedPreferences so a new quote shows each launch within a day.
 */
public class QuoteFetcher {

    private static final String PREF_NAME = "tvhome_prefs";
    private static final String KEY_QUOTE = "quote_text";
    private static final String KEY_AUTHOR = "quote_author";
    private static final String KEY_DATE = "quote_date";

    public interface Callback {
        void onQuote(String quote, String author);
    }

    /**
     * Fetch a random quote. If we already have one cached for today, return it
     * immediately without hitting the network.
     */
    public static void fetch(final Context context, final Callback callback) {
        String today = java.time.LocalDate.now().toString();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String cachedDate = prefs.getString(KEY_DATE, null);

        if (today.equals(cachedDate)) {
            String q = prefs.getString(KEY_QUOTE, null);
            String a = prefs.getString(KEY_AUTHOR, null);
            if (q != null && a != null) {
                callback.onQuote(q, a);
                return;
            }
        }

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
                // fall back to cached or default
            }

            if (quote == null || author == null) {
                quote = "The best time to plant a tree was 20 years ago.";
                author = "Chinese proverb";
            }

            // Cache for today
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_DATE, today);
            editor.putString(KEY_QUOTE, quote);
            editor.putString(KEY_AUTHOR, author);
            editor.apply();

            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> callback.onQuote(quote, author));
        }).start();
    }

    /** Return today's cached quote, or null if none. */
    public static String getCachedQuote(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String today = java.time.LocalDate.now().toString();
        String cachedDate = prefs.getString(KEY_DATE, null);
        if (!today.equals(cachedDate)) return null;
        return prefs.getString(KEY_QUOTE, null);
    }

    public static String getCachedAuthor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String today = java.time.LocalDate.now().toString();
        String cachedDate = prefs.getString(KEY_DATE, null);
        if (!today.equals(cachedDate)) return null;
        return prefs.getString(KEY_AUTHOR, null);
    }
}
