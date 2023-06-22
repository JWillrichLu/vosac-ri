package com.nuchwezi.vosac;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nuchwezi.vosac.R;
import com.nuchwezi.vosac.utils.ReductionMachines;
import com.nuchwezi.xlitedatabase.DBAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class Utility {
    public static String Tag = MainActivity.TAG;

    public static int getRandomColor() {
        RandomColors randomColors = new RandomColors();
        return  randomColors.getColor();
    }

    public static int getContrastVersionForColor(int color) {
        float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color),
                hsv);
        if (hsv[2] < 0.5) {
            hsv[2] = 0.99f;//0.7f;
        } else {
            hsv[2] = 0.3f;
        }
        hsv[1] = hsv[1] * 0.25f;//0.2f
        return Color.HSVToColor(hsv);
    }

    /*
     * Display a toast with the default duration : Toast.LENGTH_SHORT
     */
    public static void showToast(String message, Context context) {
        showToast(message, context, Toast.LENGTH_SHORT);
    }

    /*
     * Display a toast with given Duration
     */
    public static void showToast(String message, Context context, int duration) {
        Toast.makeText(context, message, duration).show();
    }

    public static void showAlert(String title, String message, Context context) {
        showAlert(title, message, R.mipmap.ic_launcher, context, null, null,null);
    }

    public static void showAlert(String title, String message, int iconId, Context context) {
        showAlert(title, message, iconId, context,  null, null,null);
    }

    public static void showAlert(String title, String message, Context context, Runnable yesCallback,  Runnable noCallback, Runnable cancelCallback ) {
        showAlert(title, message, R.mipmap.ic_launcher, context, yesCallback, noCallback,cancelCallback);
    }

    public static void showAlert(String title, String message, int iconId, Context context, Runnable yesCallback,  Runnable noCallback, Runnable cancelCallback ) {
        showAlertFactory(title, message,iconId, context, yesCallback, noCallback,cancelCallback);
    }

    public static void showAlertFactory(String title, String message, int iconId,
                                        Context context, final Runnable yesCallback, final Runnable noCallback, final Runnable cancelCallback) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setIcon(iconId);
            builder.setTitle(title);

            LayoutInflater mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogContent = mInflater.inflate(R.layout.alert_view, null);
            ((TextView)dialogContent.findViewById(R.id.msgText)).setText(message);
            builder.setView(dialogContent);

            if(yesCallback != null){
                builder.setPositiveButton( noCallback == null ? "OK"  : "YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        yesCallback.run();
                    }
                });
            }

            if(noCallback != null){
                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        noCallback.run();
                    }
                });
            }

            if(cancelCallback != null){
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        cancelCallback.run();
                    }
                });
            }

            AlertDialog alert = builder.create();
            alert.show();
        } catch (Exception e) {
            Log.e(Tag, "Alert Error : " + e.getMessage());
        }

    }

    public static void showAlertPrompt(String title, final boolean allowEmpty, boolean addMask, int iconId,
                                       final Context context, final ParametricCallback yesCallback, final Runnable cancelCallback) {
        try {
            LayoutInflater layoutInflaterAndroid = LayoutInflater.from(context);
            final View dialogView = layoutInflaterAndroid.inflate(R.layout.alert_prompt, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);

            builder.setIcon(iconId);
            builder.setTitle(title);

            if(addMask){
                EditText editText = dialogView.findViewById(R.id.eTxtPromptValue);
                editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                editText.setTransformationMethod(new PasswordTransformationMethod());
            }

            if(yesCallback != null){
                builder.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText editText = dialogView.findViewById(R.id.eTxtPromptValue);
                        String value = editText.getText().toString();
                        if(!allowEmpty){
                            if(value.trim().length() == 0){
                                Utility.showToast("Please set a value!", context);
                            }
                        }
                        yesCallback.call(value);
                    }
                });
            }

            if(cancelCallback != null){
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        cancelCallback.run();
                    }
                });
            }

            AlertDialog alert = builder.create();
            alert.show();
        } catch (Exception e) {
            Log.e(Tag, "Alert Error : " + e.getMessage());
        }

    }


    public static int getVersionNumber(Context context) {
        PackageInfo pinfo = null;
        try {
            pinfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pinfo != null ? pinfo.versionCode : 1;
    }

    public static String getVersionName(Context context) {
        PackageInfo pinfo = null;
        try {
            pinfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pinfo != null ? pinfo.versionName : "DEFAULT";
    }

    public static JSONArray removeField(JSONArray jsonArray, int index) {

        JSONArray newArray = new JSONArray();

        for(int i = 0; i < jsonArray.length(); i++)
            if(i != index)
                try {
                    newArray.put(jsonArray.get(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

        return newArray;
    }

    public static ArrayList<String> setToList(HashSet<String> set) {
        ArrayList<String> items = new ArrayList<>();
        for(String s: set)
            items.add(s);

        return items;
    }

    public static String selectRandom(JSONObject categoriesMap, String category) {
        try {
            JSONArray items = categoriesMap.getJSONArray(category);
            if(items.length() == 0)
                return null;
            else{
                Random random = new Random();
                int rIndex = random.nextInt(items.length());
                return items.getString(rIndex);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<String> toList(Iterator<String> iterator) {
        ArrayList<String> items = new ArrayList<>();
        for(; iterator.hasNext();) {
            items.add(iterator.next());
        }
        return items;
    }

    /*
     * Will create directory on the External Storage Card with the given dirName
     * name.
     *
     * Throws an exception is dirName is null, and returns the name of the
     * created directory if successful
     */
    public static String createSDCardDir(String dirName, File internalFilesDir) {

        Log.d(Tag, "Creating Dir on sdcard...");

        if (dirName == null) {
            Log.e(Tag, "No Directory Name Specified!");
            return null;
        }

        File exDir = Environment.getExternalStorageDirectory();

        if (exDir != null) {

            File folder = new File(exDir, dirName);

            boolean success = false;

            if (!folder.exists()) {
                success = folder.mkdirs();
                Log.d(Tag, "Created Dir on sdcard...");
            } else {
                success = true;
                Log.d(Tag, "Dir exists on sdcard...");
            }

            if (success) {
                return folder.getAbsolutePath();
            } else {
                Log.e(Tag, "Failed to create on sdcard...");
                return null;
            }
        } else {

            File folder = new File(internalFilesDir, dirName);

            boolean success = false;

            if (!folder.exists()) {
                success = folder.mkdirs();
                Log.d(Tag, "Created Dir on sdcard...");
            } else {
                success = true;
                Log.d(Tag, "Dir exists on sdcard...");
            }

            if (success) {
                return folder.getAbsolutePath();
            } else {
                Log.e(Tag, "Failed to create on sdcard...");
                return null;
            }
        }
    }

    public static String humaneYear_Verbose(Date date) {

        // year
        SimpleDateFormat formatYear  = new SimpleDateFormat("yyyy");
        int year = Integer.parseInt(formatYear.format(date));


        return String.format("It is %s", year);
    }


    public static String humaneDate_Verbose(Date date) {
        String[] suffixes =
                //    0     1     2     3     4     5     6     7     8     9
                { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th",
                        //    10    11    12    13    14    15    16    17    18    19
                        "th", "th", "th", "th", "th", "th", "th", "th", "th", "th",
                        //    20    21    22    23    24    25    26    27    28    29
                        "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th",
                        //    30    31
                        "th", "st" };

        // day
        SimpleDateFormat formatDayOfMonth  = new SimpleDateFormat("d");
        int day = Integer.parseInt(formatDayOfMonth.format(date));
        String dayStr = day + suffixes[day];

        // weekday
        SimpleDateFormat formatWeekDay  = new SimpleDateFormat("EEEE");
        String weekday = formatWeekDay.format(date);

        // month
        SimpleDateFormat formatMonth  = new SimpleDateFormat("MMMM");
        String month = formatMonth.format(date);

        return String.format("It is %s, the %s of %s", weekday, dayStr, month);
    }

    public static String humaneTime_Verbose(Date date) {

        // min
        SimpleDateFormat formatMin  = new SimpleDateFormat("m");
        int min = Integer.parseInt(formatMin.format(date));

        // hour
        SimpleDateFormat formatHour  = new SimpleDateFormat("h");
        int hour = Integer.parseInt(formatHour.format(date));

        // 24hour
        SimpleDateFormat formatHour24  = new SimpleDateFormat("H");
        int hour24 = Integer.parseInt(formatHour24.format(date));

        // ampm
        SimpleDateFormat formatAMPM  = new SimpleDateFormat("a");
        String ampm = formatAMPM.format(date);

        return String.format("It is %s minutes %s %s %s",
                min > 30? 60 -min: min,
                min > 30 ? "to" : "past",
                min > 30? (hour + 1)%12 : hour,
                min > 30? ((hour24+1) >= 13? "pm" : "am") : hour24 >= 13? "pm" : "am");
    }

    public static String humaneDate(Date date) {
        DateFormat df = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        return df.format(date);
    }

    public static String readFileToString(String filePath)  {
        File fl = new File(filePath);
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(fl);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        String ret = null;
        try {
            ret = convertStreamToString(fin);
        } catch (IOException e) {
            return null;
        }
        //Make sure you close all streams.
        try {
            fin.close();
        } catch (IOException e) {
            return null;
        }
        return ret;
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        // http://www.java2s.com/Code/Java/File-Input-Output/ConvertInputStreamtoString.htm
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        Boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if(firstLine){
                sb.append(line);
                firstLine = false;
            } else {
                sb.append("\n").append(line);
            }
        }
        reader.close();
        return sb.toString();
    }

    public static HashSet<String> JSONArrayToSet(JSONArray jsonArray) {
        HashSet<String> set = new HashSet<>();
        for(int i = 0; i < jsonArray.length(); i++) {
            try {
                set.add(jsonArray.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return set;
    }

    public static JSONArray setToJSONArray(HashSet<String> hashSet) {
        JSONArray jsonArray = new JSONArray();
        for(String item: hashSet)
            jsonArray.put(item);

        return jsonArray;
    }

    public static ArrayList<String> JSONArrayToList(JSONArray jsonArray) {
        ArrayList<String> list = new ArrayList<>();
        for(int i=0; i < jsonArray.length(); i++) {
            try {
                list.add(jsonArray.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return list;
    }

    public static String selectRandom(ArrayList<String> list) {
        Random random = new Random();
        return  list.get(random.nextInt(list.size()));
    }

    public static JSONArray ListToJSONArray(ArrayList<String> selectedQAKBs) {
        JSONArray jsonArray = new JSONArray();
        for(int i = 0; i < selectedQAKBs.size(); i++){
            jsonArray.put(selectedQAKBs.get(i));
        }
        return jsonArray;
    }

    // match query against question-answer entries
    // qaKB structure:
 /*   qakb|CACHED_CATEGORIES: {
        question1: [answer11, answer12,...],
        question2: [answer21, answer21,...]
        ...
    }*/
    public static ArrayList<JSONObject> solveQASearch(String query, ArrayList<String> chosenQAKBs, DBAdapter dbAdapter, ReductionMachines.StringReductionMachine reductionMachine) {

        ArrayList<JSONObject> matchingQA = new ArrayList<>();

        String semantically_reduced_query = reductionMachine.reduce(query);
        String lQuery = query.toLowerCase(Locale.ROOT);

        if(semantically_reduced_query.length() == 0) {
            if(lQuery.length() > 0){ // we might not tolerate this, at least we fallback to raw query
                semantically_reduced_query = lQuery;
            }else {
                return matchingQA;
            }
        }

        for(String qaKBNAME : chosenQAKBs){
            try {
                JSONObject qakb = new JSONObject(dbAdapter.fetchDictionaryEntry(qaKBNAME));
                for (Iterator<String> iter = qakb.keys(); iter.hasNext(); ) {
                    String qEntry = iter.next();
                    String lqEntry = qEntry.toLowerCase(Locale.ROOT);
                    // semantically reduce question
                    String qEntryReduced = reductionMachine.reduce(qEntry);
                    if (qEntryReduced.contains(semantically_reduced_query) || lqEntry.contains(lQuery)) {
                        JSONObject jQA = new JSONObject();
                        jQA.put("q", qEntry); // question
                        jQA.put("a", qakb.getJSONArray(qEntry)); // list of answers
                        matchingQA.add(jQA);
                        continue;
                    }

                    if(qEntry.equalsIgnoreCase("QRCODE"))
                        continue;

                    /*
                    try {
                        JSONArray jA = qakb.getJSONArray(qEntry);
                        for (int i = 0; i < jA.length(); i++) {
                            String answerEntry = jA.getString(i);
                            // semantically reduce answer
                            String answerEntryReduced = reductionMachine.reduce(answerEntry);
                            if (answerEntryReduced.contains(semantically_reduced_query)) {
                                JSONObject jQA = new JSONObject();
                                jQA.put("q", qEntry); // question
                                jQA.put("a", qakb.getJSONArray(qEntry)); // list of answers
                                matchingQA.add(jQA);
                                break;
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }*/
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return matchingQA;
    }


    public static class HTTP_METHODS {
        public static final String POST = "POST";
        public static final String GET = "GET";
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null, otherwise check
        // if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    /*
    public static void getHTTP(Context context, String url, final ParametricCallbackJSONObject parametricCallback) {

        Log.d(Tag, String.format("HTTP GET, FETCH URI: %s", url));


        Ion.with(context)
                .load(HTTP_METHODS.GET, url)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {

                        JSONObject apiCallStatus = null;
                        try {
                            apiCallStatus = new JSONObject(result);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        parametricCallback.call(apiCallStatus);
                    }
                });

    }

    public static void getHTTP(Context context, String url, final ParametricCallback parametricCallback) {

        Log.d(Tag, String.format("HTTP GET, FETCH URI: %s", url));


        Ion.with(context)
                .load(HTTP_METHODS.GET, url)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {

                        parametricCallback.call(result);
                    }
                });

    }*/

    public static void getHTTP(Context context, String url, final ParametricCallbackJSONObject parametricCallback ) {

        Log.d(Tag, String.format("HTTP GET, FETCH URI: %s", url));

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                parametricCallback.call(null);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        parametricCallback.call(null);
                    }else {
                        JSONObject apiCallStatus = null;
                        try {
                            apiCallStatus = new JSONObject(response.body().string());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        parametricCallback.call(apiCallStatus);
                    }
                }
            }
        });

    }

}
