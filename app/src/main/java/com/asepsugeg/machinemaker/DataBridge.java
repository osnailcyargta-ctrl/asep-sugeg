package com.asepsugeg.machinemaker;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * JS bridge exposed to the WebView as `AndroidStorage`.
 * Persists save data as a JSON file in the app's internal storage
 * (/data/data/com.asepsugeg.machinemaker/files/saves/), NOT localStorage.
 *
 * Exposed to JS:
 *   AndroidStorage.saveData(filename, jsonString) -> "OK" or "ERROR: ..."
 *   AndroidStorage.loadData(filename) -> jsonString or "" if not found
 *   AndroidStorage.deleteData(filename) -> "OK" or "ERROR: ..."
 *   AndroidStorage.listSaves() -> comma-separated filenames
 */
public class DataBridge {
    private static final String TAG = "DataBridge";
    private static final String SAVE_DIR = "saves";
    private final Context context;

    public DataBridge(Context context) {
        this.context = context;
    }

    private File getSaveDir() {
        File dir = new File(context.getFilesDir(), SAVE_DIR);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private File getSaveFile(String filename) {
        // sanitize filename: only allow alphanumeric, dash, underscore, dot
        String safe = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.isEmpty()) safe = "default";
        if (!safe.endsWith(".json")) safe += ".json";
        return new File(getSaveDir(), safe);
    }

    @JavascriptInterface
    public String saveData(String filename, String jsonString) {
        try {
            File file = getSaveFile(filename);
            FileOutputStream fos = new FileOutputStream(file, false);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            writer.write(jsonString);
            writer.flush();
            writer.close();
            fos.close();
            Log.d(TAG, "Saved to " + file.getAbsolutePath());
            return "OK";
        } catch (Exception e) {
            Log.e(TAG, "saveData failed", e);
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String loadData(String filename) {
        try {
            File file = getSaveFile(filename);
            if (!file.exists()) return "";
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            reader.close();
            fis.close();
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "loadData failed", e);
            return "";
        }
    }

    @JavascriptInterface
    public String deleteData(String filename) {
        try {
            File file = getSaveFile(filename);
            if (file.exists() && !file.delete()) {
                return "ERROR: delete failed";
            }
            return "OK";
        } catch (Exception e) {
            Log.e(TAG, "deleteData failed", e);
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public String listSaves() {
        try {
            File dir = getSaveDir();
            File[] files = dir.listFiles();
            if (files == null) return "";
            StringBuilder sb = new StringBuilder();
            for (File f : files) {
                if (sb.length() > 0) sb.append(',');
                sb.append(f.getName());
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "listSaves failed", e);
            return "";
        }
    }
}
