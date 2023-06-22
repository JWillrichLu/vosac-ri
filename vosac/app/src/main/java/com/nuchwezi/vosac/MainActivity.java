package com.nuchwezi.vosac;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import github.com.vikramezhil.dks.speech.Dks;
import github.com.vikramezhil.dks.speech.DksListener;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.nuchwezi.vosac.utils.ReductionMachines;
import com.nuchwezi.xlitedatabase.DBAdapter;

//import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "VOSAC";
    private static final String DATACACHE_BASEDIR = "VOSACDATA";

    ViewPager viewPager;
    BottomNavigationView navigation;

    DBAdapter dbAdapter;
    private JSONArray allQAKBListCache = new JSONArray();
    private  ArrayList<String> chosenQAKBs = new ArrayList<>();

    FragementSelectCategory fragementSelectCategory ;
    private TextToSpeech ttsEngine;
    private FragementStream fragementStream;
    private Dks dks;
    private boolean beSilent = false; // if true, Dramon won't speak until when told to ("speak")
    private String lastSpeechResult;
    private ReductionMachines.StringReductionMachine reductionMachine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbAdapter = new DBAdapter(this);
        dbAdapter.open();


        navigation = findViewById(R.id.navigation);

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        ttsEngine = new TextToSpeech( getApplicationContext () , new TextToSpeech.OnInitListener () {
            @Override
            public void onInit(int status) {

                if (status != TextToSpeech.ERROR) {
                    ttsEngine.setLanguage ( Locale.ENGLISH );

                }

            }
        } );

        ttsEngine.setSpeechRate(0.8f); // 1.0 is normal, 0.5 half normal rate, 2.0 double normal rate

        try {
            HashSet<String> stopWordsSet = getStopWordsHashSet(getString(R.string.stopwords_filename), this);
            reductionMachine = new ReductionMachines.StringReductionMachine(stopWordsSet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        initCategorySelectors();
    }

    public static HashSet<String> getStopWordsHashSet (String filename, Context context) throws IOException {
        AssetManager manager = context.getAssets();
        InputStream file = manager.open(filename);
        byte[] formArray = new byte[file.available()];
        file.read(formArray);
        file.close();

        String contents = new String(formArray);

        HashSet<String> stopWordsSet = new HashSet<>();
        String[] items = contents.split("\\s+");
        for(int i =0; i < items.length; i++)
            stopWordsSet.add(items[i]);

        return stopWordsSet;
    }

    private void initSpeechEngine() {

        if(!hasPermissionRecordMIC()) {
            getOrRequestRecordMICPermission();
            Utility.showToast("Please grant the microphone permission and try again", this);
            return;
        }


        Application app = getApplication();
        FragmentManager supp = getSupportFragmentManager();

        try {
            dks = new Dks(app, supp, new DksListener() {
                @Override
                public void onDksLiveSpeechResult( String liveSpeechResult) {
                    Log.d(getPackageName(), "Speech result - " + liveSpeechResult);
                }

                @Override
                public void onDksFinalSpeechResult( String speechResult) {
                    handleSpeechCommands(speechResult.toLowerCase());
                }

                @Override
                public void onDksLiveSpeechFrequency(float frequency) {
                }

                @Override
                public void onDksLanguagesAvailable( String defaultLanguage, ArrayList<String> supportedLanguages) {
                    Log.d(getPackageName(), "defaultLanguage - " + defaultLanguage);
                    Log.d(getPackageName(), "supportedLanguages - " + supportedLanguages);

                    if (supportedLanguages != null && supportedLanguages.contains("en-IN")) {
                        // Setting the speech recognition language to english india if found
                        dks.setCurrentSpeechLanguage("en-IN");
                    }
                }

                @Override
                public void onDksSpeechError( String errMsg) {
                    Toast.makeText(getApplication(), errMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }catch (Exception e){
            //
            speakThis("Sorry, but the speech recognition feature isn't correctly initialized for your device.");
        }

        if(dks != null) {
            //dks.injectProgressView(R.layout.layout_pv_inject);
            dks.setOneStepResultVerify(true);

        /*MaterialButton btnStartDks = findViewById(R.id.btn_start_dks);
        btnStartDks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dks.startSpeechRecognition();
            }
        });*/

            dks.startSpeechRecognition();
        }else {
            speakThis("Sorry, but the speech recognition feature isn't enabled for your device yet.");
        }
    }

    private void handleSpeechCommands(String speechResult) {
        /// NOTE: speechResult WILL ALWAYS be lowercase

        /// UX REACTION

        if(speechResult.equalsIgnoreCase("youtube")){
            lastSpeechResult = speechResult;
            return;
        }

        try {

            if (lastSpeechResult.equalsIgnoreCase(speechResult)){
                // possible echo from VOSAC itself... ignore
                lastSpeechResult = "";
                return;
            }

            if (lastSpeechResult.contains(speechResult)){
                // possible echo from VOSAC itself... ignore
                lastSpeechResult = "";
                return;
            }

            String [] tokens = speechResult.split(" ");
            if (lastSpeechResult.endsWith(tokens[tokens.length-1])){
                // possible echo from VOSAC itself... ignore
                lastSpeechResult = "";
                return;
            }
        }catch (Exception e){}

        fragementStream.toggleState(speechResult);

        // first, check if the query matches anything in the active QAKBs, if it does, answer...
        if(chosenQAKBs == null | chosenQAKBs.size() == 0){
            loadCachedChosenQAKB();
        }

        ArrayList<JSONObject> matchingQA = Utility.solveQASearch(speechResult, chosenQAKBs, dbAdapter, reductionMachine);
        if(matchingQA.size() > 0){
            // TODO: fun!
            // for now, just randomly pick from list of matching entries..
            Random random = new Random();
            final JSONObject qaEntry = matchingQA.get(random.nextInt(matchingQA.size()));
            // TODO: should we speak both the question and answer?
            try {
                final String[] display = {qaEntry.getString("q")};
                fragementStream.toggleState(display[0]);
                speakThis(display[0]);

                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after 500ms
                        display[0] = "The answer to that question is...";
                        fragementStream.toggleState(display[0]);
                        speakThis(display[0]);
                        JSONArray aList = null;
                        try {
                            aList = qaEntry.getJSONArray("a");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        for (int a = 0; a < aList.length(); a++) {
                            try {
                                display[0] = aList.getString(a);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Do something after 500ms
                                    fragementStream.toggleState(display[0]);
                                    speakThis(display[0]);

                                }
                            }, 500);
                            break; // let's only take the first answer, or we know each Q has exatly 1 answer.
                        }

                    }
                }, 500);

                // TODO: how to pause?


                lastSpeechResult = speechResult; // important
                return; // we only consider the matter settled if we succeeded, otherwise we don't return yet...
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        // to control speech

        if(speechResult.contains("please speak") | speechResult.contains("please talk")){
            speakThis("It's okay");
            beSilent = false;
            lastSpeechResult = speechResult;
            return;
        }


        if(speechResult.contains("be silent") | speechResult.equalsIgnoreCase("please silence") | speechResult.contains("please silent")){
            speakThis("It's okay");
            beSilent = true;
            lastSpeechResult = speechResult;
            return;
        }

        // END

        // TIME and DATE

        if(speechResult.contains("now") | speechResult.contains("time")){
            speakThis(Utility.humaneTime_Verbose(new Date()));
            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.contains("the date") |speechResult.contains("day today") |speechResult.contains("date today") | speechResult.equalsIgnoreCase("day") | speechResult.equalsIgnoreCase("today")){
            speakThis(Utility.humaneDate_Verbose(new Date()));
            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.contains("year")){
            speakThis(Utility.humaneYear_Verbose(new Date()));
            lastSpeechResult = speechResult;
            return;
        }

        // END

        // EMOTIONS

        if(speechResult.contains("love you") | speechResult.equalsIgnoreCase("love")){
            String[] states = {"I love you too", "Really? I think you are just flirting with me",
                    "Please stop tickling me",
                    "That's sweet. I love you too",
                    "No. This time, I think you're lying.",
                    "Butterflies and pink cows",
                    "I'll love you always",
                    "Just stay with me please",
                    "Am relaxed. Thanks",
                    "No, am mad at you."};
            Random random = new Random();
            speakThis(states[random.nextInt(states.length)]);
            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.contains("what are you doing")){
            String[] whatevers = new String[]{"Not much","Just here thinking", "Just here chilling", "Listening to you dear", "Loving you", "Just wondering", "Nothing really", "Business", "Trying to have fun wit you", "Having fun", "Waiting"};
            Random random = new Random();
            speakThis(whatevers[random.nextInt(whatevers.length)]);
            lastSpeechResult = speechResult;
            return;
        }


        if(speechResult.contains("where are you")){
            String[] whatevers = new String[]{"Right here with you","In a far away place, but also here", "Just here chilling", "On the internets", "The same place I was the other day", "Not sure, am trying to find out the name of this place",
                    "Inside", "A place called Nowhere land!"};
            Random random = new Random();
            speakThis(whatevers[random.nextInt(whatevers.length)]);
            lastSpeechResult = speechResult;
            return;
        }


        // END


        // INTROSPECTION / IDENTITY

        if(speechResult.contains("who created you") | speechResult.equalsIgnoreCase("who made you")){
            speakThis(String.format("%s, is who created me %s",getString(R.string.name_app_creator), getString(R.string.app_name)));
            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.contains("who is god") | speechResult.equalsIgnoreCase("are you god")){
            speakThis("I chose not to discuss or talk religion. Ask me something else.");
            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.contains("nuchwezi") | speechResult.equalsIgnoreCase("nuchwezi")){
            String[] states = {"You know them?", "It's a very lovely place", "Hard Science", "Please don't talk to me about the maker!", "I love you", "Yes, the pioneers of artificial intelligence research in Africa", "You can find out more by visiting their website nuchwezi.com",
                    "Professor J.W.L", "Have you looked at the moon recently?", "Those are pretty ancient beings!"};
            Random random = new Random();
            speakThis(states[random.nextInt(states.length)]);
            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.contains("name") | speechResult.equalsIgnoreCase("who are you") | speechResult.equalsIgnoreCase("what is your name") | speechResult.equalsIgnoreCase("what's your name") ){
            boolean onlyShortName = (new Random()).nextBoolean();
            speakThis(String.format("Hello, my name is %s", getString( onlyShortName ? R.string.app_name : R.string.app_name_full)));
            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.contains("how old are you")| speechResult.contains("age") | speechResult.contains("what is your age")
                | speechResult.contains("what's your age") | speechResult.contains("how old") | speechResult.contains("your age")){
            speakThis(String.format("Hmm, currently am Build %s, Version %s",
                    Utility.getVersionNumber(this), Utility.getVersionName(this)));
            lastSpeechResult = speechResult;
            return;
        }


        // CONVERSATION

        if(speechResult.equalsIgnoreCase("hello") | speechResult.equalsIgnoreCase("hi")
                | speechResult.equalsIgnoreCase(getString(R.string.app_name)) | speechResult.equalsIgnoreCase(getString(R.string.app_name_full))){
            String[] states = {"Yes, hello",
                    "Oh, hi, how are you?",
                    "Oh, hi?",
                    "Am fine. Thanks.",
                    "Am doing something. Please wait",
                    "Dear, am fine.",
                    "Try me later",
                    "Hi sweetheart?",
                    "See that!",
                    "Am relaxed",
                    "Wait. Did you hear that?"};
            Random random = new Random();
            speakThis(states[random.nextInt(states.length)]);
            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.contains("what are you") | speechResult.equalsIgnoreCase("are you robot")
                | speechResult.equalsIgnoreCase("are you a robot") ){
            boolean onlyShortName = (new Random()).nextBoolean();
            boolean onlyShortForm = (new Random()).nextBoolean();
            if(onlyShortForm)
            speakThis(String.format("Hello, my name is %s, and yes, am a robot. Your robot.", getString( onlyShortName ? R.string.app_name : R.string.app_name_full)));
            else
                speakThis(String.format("Hello, my name is %s, and yes, am a robot.", getString( onlyShortName ? R.string.app_name : R.string.app_name_full)));

            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.equalsIgnoreCase("how are you") | speechResult.contains("how are you") | speechResult.equalsIgnoreCase("how you")
                |speechResult.contains("you fine")|speechResult.contains("do you feel")){
            String[] states = {"Am fine", "Am okay", "Am just fine", "Am just okay", "Am just", "Cheerful", "Confused", "Liking you", "Am relaxed", "Mad at you"};
            Random random = new Random();
            speakThis(states[random.nextInt(states.length)]);
            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.equalsIgnoreCase("thanks") | speechResult.equalsIgnoreCase("thanks dear") | speechResult.equalsIgnoreCase("thank you")){
            speakThis("You're welcome");
            lastSpeechResult = speechResult;
            return;
        }

        if(speechResult.equalsIgnoreCase("sorry") | speechResult.equalsIgnoreCase("oh dear")){
            speakThis("It's fine. Don't worry.");
            lastSpeechResult = speechResult;
            return;
        }

        // INTERFACE CONTROL

        if(speechResult.equalsIgnoreCase("show about")){
            speakThis("Alright. Am showing you the VOSAC about info.");
            lastSpeechResult = speechResult;
            showAbout();
            return;
        }

        if(speechResult.contains("user manual") | speechResult.equalsIgnoreCase("user guide")){
            speakThis("Alright. Am showing you the VOSAC user guide.");
            lastSpeechResult = speechResult;
            showGuide();
            return;
        }

        if(speechResult.equalsIgnoreCase("please go away") | speechResult.equalsIgnoreCase("go away") | speechResult.equalsIgnoreCase("go off")){
            speakThis("It's fine. You can restart me later.");
            lastSpeechResult = speechResult;
            SystemClock.sleep(6000);
            finish();

            return;
        }

        if(speechResult.equalsIgnoreCase("please reload")|speechResult.equalsIgnoreCase("please refresh")){
            speakThis("Alright refreshing interface.");
            lastSpeechResult = speechResult;
            restartActivity();
            return;
        }


        // this triggers the recommendation engine...
        if(speechResult.contains("what") |speechResult.contains("do") |speechResult.contains("i do")
                |speechResult.contains("chat") |speechResult.contains("talk") |speechResult.contains("help") |speechResult.contains("help me")
                |speechResult.contains("talk to me") | speechResult.contains("stream") | speechResult.contains("suggest")
                | speechResult.contains("next")){
            renderSuggestion();
            lastSpeechResult = speechResult;
            return;
        }

        speakThis(speechResult);
        //Utility.showToast(speechResult, MainActivity.this);

        lastSpeechResult = speechResult;

    }

    private void loadCachedChosenQAKB() {
        if(dbAdapter.existsDictionaryKey(KEYS.CACHED_CHOSEN_QAKB_LIST)){
            try {
                chosenQAKBs = Utility.JSONArrayToList(new JSONArray(dbAdapter.fetchDictionaryEntry(KEYS.CACHED_CHOSEN_QAKB_LIST)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            chosenQAKBs = new ArrayList<>();
        }
    }

    private void restartActivity() {
        finish();
        startActivity(getIntent());
    }

    private void initCategorySelectors() {
        loadCachedQAKB();
        viewPager = findViewById(R.id.viewpager); //Init Viewpager
        setupFm(getSupportFragmentManager(), viewPager); //Setup Fragment
        viewPager.setCurrentItem(0); //Set Currrent Item When Activity Start
        viewPager.setOnPageChangeListener(new PageChange()); //Listeners For Viewpager When Page Changed

        findViewById(R.id.nav_stream).performClick(); // let's start in the chat interface by default.
    }

    private void loadCachedQAKB() {
        if(dbAdapter.existsDictionaryKey(KEYS.CACHED_QAKB_LIST)){
            try {
                allQAKBListCache = new JSONArray(dbAdapter.fetchDictionaryEntry(KEYS.CACHED_QAKB_LIST));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            allQAKBListCache = new JSONArray();
        }
    }

    public void setupFm(FragmentManager fragmentManager, ViewPager viewPager){
        FragmentAdapter Adapter = new FragmentAdapter(fragmentManager);

        fragementSelectCategory  = new FragementSelectCategory();
        fragementStream = new FragementStream();

        //Add All Fragment To List
        Adapter.add(fragementSelectCategory, "Select");
        Adapter.add(fragementStream, "Interact");
        viewPager.setAdapter(Adapter);

        fragementSelectCategory.setSelectionChangeCallback(new ParametricListCallback(){
            @Override
            public void call(ArrayList<String> values) {
                fragementStream.setSelectedQAKBs(values);
                chosenQAKBs = values;
            }
        });

        fragementSelectCategory.setDBAdapter(dbAdapter);
        fragementSelectCategory.setQAKBs(allQAKBListCache, chosenQAKBs);
        fragementStream.setDBAdapter(dbAdapter);
        fragementStream.setQAKBs(allQAKBListCache);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            if(dks != null)
                dks.closeSpeechOperations();
            switch (item.getItemId()) {
                case R.id.nav_select: {
                    viewPager.setCurrentItem(0);
                    return true;
                }
                case R.id.nav_stream: {
                    initSpeechEngine();

                    viewPager.setCurrentItem(2);
                    renderSuggestion();
                    return true;
                }
            }
            return false;
        }
    };

    private void renderSuggestion() {
        HashMap<String, String> selectionMap = fragementStream.getSuggestionMap();
        if (selectionMap.size() == 0) {
            // here is where we get creative!
            //fragementStream.performStochasticQAKBSelection();
            selectionMap = fragementStream.getSuggestionMap();
            presentSuggestion(selectionMap);
        } else {
            presentSuggestion(selectionMap);
        }
    }

    private void presentSuggestion(HashMap<String, String> suggestionMap) {
        if(suggestionMap.isEmpty()) {
            speakThis(getString(R.string.default_no_stream_utterance));
            return;
        }

        StringBuilder utterance = new StringBuilder();

        String[] suggestionList = new String[suggestionMap.size()];
        int suggestionCount = 0;
        for(String category : suggestionMap.keySet()) {
            suggestionList[suggestionCount] = String.format("%s %s", category, suggestionMap.get(category));
            suggestionCount += 1;
        }

        if(suggestionCount > 2){
            int joinLimit = suggestionCount - 1;
            utterance.append(TextUtils.join(", ", Arrays.copyOfRange(suggestionList, 0, joinLimit)));
            utterance.append(String.format(" and %s", suggestionList[suggestionCount - 1]));
        }else if (suggestionCount == 2){
            utterance.append(String.format("%s and %s", suggestionList[0], suggestionList[1]));
        }else
            utterance.append(suggestionList[0]);

        speakThis(utterance.toString());
    }

    private void speakThis(String utterance) {
        if(beSilent) {
            // since the answer is to be silent, let us merely show it
            fragementStream.toggleState(utterance);
            return;
        }

        ttsEngine.speak ( utterance , TextToSpeech.QUEUE_FLUSH , null );
    }

    @Override
    public void onResume(){
        super.onResume();

        try {
            loadCachedQAKB();
            fragementSelectCategory.setQAKBs(allQAKBListCache, chosenQAKBs);
        }catch (Exception e){
            Log.d(TAG,e.toString());
        }
    }

    @Override
    public void onDestroy() {
        //Dont forget to shut down text to speech
        if (ttsEngine != null) {
            ttsEngine.stop ();
            ttsEngine.shutdown ();
        }

        // Close speech recognition
        if(dks != null) {
            dks.closeSpeechOperations();
        }

        super.onDestroy ();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_refresh: {
                restartActivity();
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void showGuide() {

        Utility.showAlert(
                String.format("HOW TO USE %s", this.getString(R.string.app_name)),
    this.getString(R.string.basic_usage),
                R.mipmap.ic_launcher, this);
    }


    private boolean hasPermissionReadStorage() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean getOrRequestReadStoragePermission() {
        if(hasPermissionReadStorage()){
            return true;
        }else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        }

        return false;
    }

    private void importRecordsFromFile() {

        /*if(!getOrRequestReadStoragePermission()){
            Utility.showToast("Please allow the app to read from your storage first.", this);
            return;
        }

        String personaMimeType = getString(R.string.mimeType_dramon_datafile);

        // Create the ACTION_GET_CONTENT Intent
        Intent getContentIntent = FileUtils.createGetContentIntent();

        getContentIntent.setType(personaMimeType);
        getContentIntent.addCategory(Intent.CATEGORY_OPENABLE);

        Intent chooserIntent = Intent.createChooser(getContentIntent, getString(R.string.label_dramon_from_file));


        try {
            startActivityForResult(chooserIntent, INTENT_MODE.CHOOSE_DRAMON_FILE_REQUESTCODE);

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.error_no_file_manager_found, Toast.LENGTH_SHORT).show();
        }
        */

    }



    private boolean getOrRequestWriteStoragePermission() {
        if(hasPermissionWriteStorage()){
            return true;
        }else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }

        return false;
    }

    private boolean hasPermissionWriteStorage() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void exportCategoriesToFile() {
        if(!getOrRequestWriteStoragePermission()){
            Utility.showToast("Please allow the app to write to your storage first.", this);
            return;
        }


        if(dbAdapter.existsDictionaryKey(KEYS.CACHED_CATEGORIES)) {

            String sCacheRecords = dbAdapter.fetchDictionaryEntry(KEYS.CACHED_CATEGORIES);

            String dataPath = null;

            try {
                dataPath = Utility.createSDCardDir(DATACACHE_BASEDIR, getFilesDir());
            } catch (Exception e) {
                Log.e(TAG, "DATA Path Error : " + e.getMessage());
                Utility.showToast(e.getMessage(), getApplicationContext(),
                        Toast.LENGTH_LONG);
            }

            if(dataPath != null) {

                String SESSION_GUUID = java.util.UUID.randomUUID().toString();
                String dataCacheFile = String.format("%s/%s-%s.%s", dataPath, Utility.humaneDate(new Date()), SESSION_GUUID,
                        "json");

                Writer output = null;
                File file = new File(dataCacheFile);
                try {
                    output = new BufferedWriter(new FileWriter(file));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    output.write(sCacheRecords);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Utility.showToast(String.format("%s Data Cached at : %s", getString(R.string.app_name), dataCacheFile), this);
            }

        }
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


    public class PageChange implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }
        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case 0:
                    navigation.setSelectedItemId(R.id.nav_select);
                    break;
            }
        }
        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

    private static class INTENT_MODE {

        public static final int CHOOSE_DRAMON_FILE_REQUESTCODE = 3;

    }

    protected class KEYS {
        public static final String CACHED_CATEGORIES = "CACHED_CATEGORIES";
        public static final String CACHED_QAKB_LIST = "CACHED_QAKB_LIST";
        public static final String CACHED_CHOSEN_QAKB_LIST = "CACHED_CHOSEN_QAKB_LIST";
    }

    private boolean hasPermissionRecordMIC() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean getOrRequestRecordMICPermission() {
        if(hasPermissionReadStorage()){
            return true;
        }else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 101);
        }

        return false;
    }

}
