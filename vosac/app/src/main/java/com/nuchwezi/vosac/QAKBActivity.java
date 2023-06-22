package com.nuchwezi.vosac;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.nuchwezi.xlitedatabase.DBAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

import androidx.appcompat.app.AppCompatActivity;

public class QAKBActivity extends AppCompatActivity {

    DBAdapter dbAdapter;
    private JSONObject allQAKBCache = new JSONObject();
    private QAKBAdapter qakbAdapter;
    private JSONArray allQAKBNAMEList;
    String standardQAKBURL = "https://gist.githubusercontent.com/mcnemesis/52c65f607cf7f3b7395326655d23effc/raw/vosac_default_knowledgebase.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        dbAdapter = new DBAdapter(this);
        dbAdapter.open();

        (findViewById(R.id.btnScanQAKB)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // scan QAKB QRCODE...
                launchScanner();
            }
        });

        loadCachedQAKBs();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.qakb_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_refresh: {
                refreshAllKnownQAKB();
                return true;
            }
            case R.id.action_categories: {
                Intent intent = new Intent(this, QAKBActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_about: {
                showAbout();
                return true;
            }
            case R.id.action_guide: {
                showGuide();
                return true;
            }
            case R.id.action_import_default: {
                importRecordsFromOnlineConfig();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void importRecordsFromOnlineConfig() {

        if (!Utility.isNetworkAvailable(this)) {
            Utility.showToast("Sorry, but you need an active connection to import the online DRAMON configuration!", QAKBActivity.this);
            return;
        }


        Utility.showToast("Wait as the standard QAKB is fetched online...", QAKBActivity.this, Toast.LENGTH_LONG);
        Utility.getHTTP(this, standardQAKBURL,
                new ParametricCallbackJSONObject() {
                    @Override
                    public void call(JSONObject data) {
                        if (data == null) {
                            Utility.showToast("Error importing QAKB. Check connectivity and try again", QAKBActivity.this);
                        } else {
                            try {
                                String qakbName = data.getString("name");
                                String qakbType = data.getString("kind");

                                final String importedCategoryName = String.format("%s:%s", qakbType, qakbName);

                                JSONObject qakbQRCODE = new JSONObject();
                                qakbQRCODE.put("url", standardQAKBURL);
                                qakbQRCODE.put("name", qakbName);
                                qakbQRCODE.put("kind", qakbType);

                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        processImportedQAKB(importedCategoryName, data, qakbQRCODE.toString());
                                        Utility.showToast(String.format("Standard QAKB (%s) has been successfully loaded. Refresh VOSAC to see these updates.", importedCategoryName),
                                                QAKBActivity.this, Toast.LENGTH_LONG);
                                    }
                                });
                            }catch (Exception e){
                                e.printStackTrace();
                                Utility.showToast("Error importing QAKB: " + e.getMessage(), QAKBActivity.this);
                            }

                        }
                    }
                });

    }

    private void refreshAllKnownQAKB() {

        if(allQAKBNAMEList == null)
            return;

        updateQAKBCache(allQAKBNAMEList);

    }

    private void updateQAKBCache(JSONArray QAKBNAMEList) {
        for(int i = 0 ; i < QAKBNAMEList.length(); i ++){
            try {
                String qakbName = allQAKBNAMEList.getString(i);

                if(dbAdapter.existsDictionaryKey(qakbName)) {
                    try {
                        JSONObject qakb = new JSONObject(dbAdapter.fetchDictionaryEntry(qakbName));

                        if(qakb.has("QRCODE")){

                            String sqrcodePayload = qakb.getString("QRCODE");
                            JSONObject qrcodePayload = new JSONObject(sqrcodePayload);
                            processQAKBQRCODE(qrcodePayload);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void processQAKBQRCODE(final JSONObject jsonObject){

        try {
            String qakbhURL = jsonObject.getString("url");
            String qakbName = jsonObject.getString("name");
            String qakbType = jsonObject.getString("kind");

            final String importedCategoryName = String.format("%s:%s", qakbType, qakbName);

            // let's fetch the QAKB
            if (!Utility.isNetworkAvailable(this)) {
                Utility.showToast("Sorry, but you need an active connection to import the online QAKB!", QAKBActivity.this);
                return;
            }


            Utility.showToast(String.format("Wait as the QAKB %s is fetched online...", importedCategoryName), QAKBActivity.this, Toast.LENGTH_LONG);
            Utility.getHTTP(this, qakbhURL,
                    new ParametricCallbackJSONObject() {
                        @Override
                        public void call(JSONObject data) {
                            if (data == null) {
                                Utility.showToast(String.format("Error importing QAKB (%s). Check connectivity and try again", importedCategoryName), QAKBActivity.this);
                            } else {
                                processImportedQAKB(importedCategoryName, data, jsonObject.toString());




                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        Utility.showToast(String.format("QAKB (%s) has been successfully loaded. Restart VOSAC to see these updates.",
                                                importedCategoryName), QAKBActivity.this, Toast.LENGTH_LONG);
                                    }
                                });
                            }
                        }
                    });
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void showGuide() {

        Utility.showAlert(
                String.format("HOW TO USE %s", this.getString(R.string.app_name)),
                this.getString(R.string.basic_usage),
                R.mipmap.ic_launcher, this);
    }


    private void showAbout() {

        Utility.showAlert(
                this.getString(R.string.app_name),
                String.format("Version %s (Build %s)\n\n%s",
                        Utility.getVersionName(this),
                        Utility.getVersionNumber(this),
                        this.getString(R.string.powered_by)),
                R.mipmap.ic_launcher, this);
    }

    private void launchScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt(getString(R.string.message_scan));
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            if (intent != null) {
                final String scannedJSON = intent.getStringExtra(Intents.Scan.RESULT);
                String format = intent.getStringExtra(Intents.Scan.RESULT_FORMAT);

                try {
                    JSONObject jsonObject = new JSONObject(scannedJSON);

                    processQAKBQRCODE(scannedJSON, jsonObject);


                } catch (JSONException e) {
                    e.printStackTrace();
                    Utility.showAlert("INVALID QAKB QRCode", "It is possible that this is an invalid QAKB QRCODE. Please ensure to scan a valid VOSA QRCODE as generated from \n\nqa.chwezi.tech", this);
                }

            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }

    private void processQAKBQRCODE(final String scannedJSON, JSONObject jsonObject) throws JSONException {
        String qakbhURL = jsonObject.getString("url");
        String qakbName = jsonObject.getString("name");
        String qakbType = jsonObject.getString("kind");

        final String importedCategoryName = String.format("%s:%s", qakbType, qakbName);

        // let's fetch the QAKB
        if (!Utility.isNetworkAvailable(this)) {
            Utility.showToast("Sorry, but you need an active connection to import the online QAKB!", QAKBActivity.this);
            return;
        }


        Utility.showToast(String.format("Wait as the QAKB %s is fetched online...", importedCategoryName), QAKBActivity.this, Toast.LENGTH_LONG);
        Utility.getHTTP(this, qakbhURL,
                new ParametricCallbackJSONObject() {
                    @Override
                    public void call(JSONObject data) {
                        if (data == null) {
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    Utility.showToast(String.format("Error importing QAKB (%s). Check connectivity and try again", importedCategoryName), QAKBActivity.this);
                                }
                            });

                        } else {

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    processImportedQAKB(importedCategoryName, data, scannedJSON);
                                    Utility.showToast(String.format("QAKB (%s) has been successfully loaded. Restart VOSAC to see these updates.", importedCategoryName), QAKBActivity.this, Toast.LENGTH_LONG);
                                }
                            });
                        }
                    }
                });
    }

    private void processImportedQAKB(String effectiveQAKBName, JSONObject importedQAKB, String qakbQRCODE) {
        JSONObject  mergedCategories = new JSONObject();

        try {
            String qakbName = importedQAKB.getString("name");
            String qakbType = importedQAKB.getString("kind");

            // override if set
            String importedCategoryName = qakbName.length() > 0 && qakbType.length() > 0 ? String.format("%s:%s", qakbType, qakbName) : null;
            effectiveQAKBName = importedCategoryName != null ? importedCategoryName : effectiveQAKBName;
        }catch (JSONException e){

        }

        String CACHE_KEY = String.format("%s|%s", effectiveQAKBName, MainActivity.KEYS.CACHED_CATEGORIES);

        if(dbAdapter.existsDictionaryKey(CACHE_KEY)){
            try {
                mergedCategories = new JSONObject(dbAdapter.fetchDictionaryEntry(CACHE_KEY));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {

            JSONArray qa = importedQAKB.getJSONArray("qa");

            for (int i = 0; i < qa.length(); i++) {
                JSONObject entry = qa.getJSONObject(i);
                JSONArray values = new JSONArray();
                values.put(entry.getString("a"));

                String question = entry.getString("q");

                if(mergedCategories.has(question)){
                    JSONArray existingVals = mergedCategories.getJSONArray(question);
                    HashSet<String> mergedSet = Utility.JSONArrayToSet(existingVals);
                    HashSet<String> importedSet = Utility.JSONArrayToSet(values);
                    for(String item : importedSet)
                        mergedSet.add(item);

                    mergedCategories.put(question, Utility.setToJSONArray(mergedSet));
                }else {
                    mergedCategories.put(question, values);
                }

                // then handle alternate questions:
                JSONArray aq = entry.getJSONArray("aq");
                for (int j = 0; j < aq.length(); j++) {
                    question = aq.getString(j);

                    if(mergedCategories.has(question)){
                        JSONArray existingVals = mergedCategories.getJSONArray(question);
                        HashSet<String> mergedSet = Utility.JSONArrayToSet(existingVals);
                        HashSet<String> importedSet = Utility.JSONArrayToSet(values);
                        for(String item : importedSet)
                            mergedSet.add(item);

                        mergedCategories.put(question, Utility.setToJSONArray(mergedSet));
                    }else {
                        mergedCategories.put(question, values);
                    }
                }
            }

        } catch (JSONException e) {
            // Something went wrong!
            e.printStackTrace();
        }

        try {
            mergedCategories.put("QRCODE", qakbQRCODE);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        updateQAKBCacheList(CACHE_KEY);
        updateQAKBCache(CACHE_KEY, mergedCategories);
        loadCachedQAKBs();
    }

    private void updateQAKBCacheList(String cache_key) {
        // first get list of all QAKBs..
        if (dbAdapter.existsDictionaryKey(MainActivity.KEYS.CACHED_QAKB_LIST)) {
            try {
                JSONArray allQAKBList = new JSONArray(dbAdapter.fetchDictionaryEntry(MainActivity.KEYS.CACHED_QAKB_LIST));
                HashSet<String> qakbSet = Utility.JSONArrayToSet(allQAKBList);
                qakbSet.add(cache_key);
                if (qakbSet.size() > allQAKBList.length()) {
                    allQAKBList = Utility.setToJSONArray(qakbSet);
                    dbAdapter.updateDictionaryEntry(new DBAdapter.DictionaryKeyValue(MainActivity.KEYS.CACHED_QAKB_LIST, allQAKBList.toString()));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            JSONArray allQAKBList = new JSONArray();
            allQAKBList.put(cache_key);
            dbAdapter.createDictionaryEntry(new DBAdapter.DictionaryKeyValue(MainActivity.KEYS.CACHED_QAKB_LIST, allQAKBList.toString()));
        }
    }


    private void loadCachedQAKBs() {
        // first get list of all QAKBs..
        if(dbAdapter.existsDictionaryKey(MainActivity.KEYS.CACHED_QAKB_LIST)) {

            try {
                allQAKBNAMEList = new JSONArray(dbAdapter.fetchDictionaryEntry(MainActivity.KEYS.CACHED_QAKB_LIST));
                showQAKBCache(allQAKBNAMEList);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }else{
            allQAKBNAMEList = new JSONArray();
        }
    }

    private void showQAKBCache(JSONArray allQAKBList) {
        qakbAdapter = new QAKBAdapter(this, allQAKBList, dbAdapter,
                new QAKBRunnable() {
                    @Override
                    public void run(String qakbName) {
                        deleteQAKB(qakbName);
                    }
                });
        ExpandableListView itemsList = findViewById(R.id.categoriesListContainer);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                itemsList.setAdapter(qakbAdapter);
            }
        });

    }

    private void deleteQAKB(String qakbKEY) {
        dbAdapter.deleteDictionaryEntry(qakbKEY);
        // also remove this from list of cached qakb..
        deleteFromQAKBNameList(qakbKEY);
        loadCachedQAKBs();
    }

    private void deleteFromQAKBNameList(String qakbKEY) {
        // first get list of all QAKBs..
        if(dbAdapter.existsDictionaryKey(MainActivity.KEYS.CACHED_QAKB_LIST)) {
            JSONArray newQAKBName = new JSONArray();

            try {
                allQAKBNAMEList = new JSONArray(dbAdapter.fetchDictionaryEntry(MainActivity.KEYS.CACHED_QAKB_LIST));
                for(int i = 0 ; i < allQAKBNAMEList.length(); i ++){
                    if(!allQAKBNAMEList.getString(i).equals(qakbKEY)){
                        newQAKBName.put(allQAKBNAMEList.getString(i));
                    }
                }

                dbAdapter.updateDictionaryEntry(new DBAdapter.DictionaryKeyValue(MainActivity.KEYS.CACHED_QAKB_LIST, newQAKBName.toString()));

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }


    private void updateQAKBCache(String CACHE_KEY, JSONObject qakbCache) {
        if(dbAdapter.existsDictionaryKey(CACHE_KEY)){
            dbAdapter.updateDictionaryEntry(new DBAdapter.DictionaryKeyValue(CACHE_KEY, qakbCache.toString()));
        }else{
            dbAdapter.createDictionaryEntry(new DBAdapter.DictionaryKeyValue(CACHE_KEY, qakbCache.toString()));
        }
    }

    public abstract class QAKBRunnable {
        public abstract void run(String category);

    }
}
