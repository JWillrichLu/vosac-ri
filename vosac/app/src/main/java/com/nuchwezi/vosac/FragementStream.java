package com.nuchwezi.vosac;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.nuchwezi.xlitedatabase.DBAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragementStream extends Fragment {
    private JSONObject categoriesMap;
    private ArrayList<String> selectedQAKBs;
    View rootView;
    private LayoutInflater mInflater;
    HashMap<String, Integer> categoryColorMapping = new HashMap<>();
    HashMap<String, String> activeSuggestionMap = new HashMap<>(); // to hold most recent active suggestion mapping
    private ArrayList<String> allQAKBList;
    private WebView webView;
    private com.nuchwezi.xlitedatabase.DBAdapter DBAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragment_one = inflater.inflate(R.layout.fragment_stream, container, false);
        rootView = fragment_one;
        this.mInflater = (LayoutInflater)rootView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        getAndRenderSuggestions();
        return fragment_one;
    }

    protected void getAndRenderSuggestions() {
        if(categoriesMap == null)
            return;

        if(rootView != null){
            if(selectedQAKBs == null)
                return;

            // first, pick which qakb we'll work with...
            if(selectedQAKBs.size() > 0) {
                String selectedQAKB = Utility.selectRandom(selectedQAKBs);
                categoriesMap = initCategoriesMap(selectedQAKB);
            }

            webView = rootView.findViewById(R.id.dramon_avatar);

            WebSettings webSetting = webView.getSettings();
            webSetting.setBuiltInZoomControls(true);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new WebViewClient());

            // NOTE: VOSAC Client name is hard-coded in this template
            webView.loadUrl("file:///android_asset/chatbg/index.html");

            FlexboxLayout flexboxLayout = rootView.findViewById(R.id.containerSuggestions);
            flexboxLayout.removeAllViews();

            activeSuggestionMap.clear(); // reset suggestion mapping

            for(Iterator<String> iter = categoriesMap.keys(); iter.hasNext();) {
                final String category = iter.next();

                if(!selectedQAKBs.contains(category))
                    continue;

                View suggestionView = mInflater.inflate(R.layout.category_item_suggestion, null);
                TextView txtCategoryName = suggestionView.findViewById(R.id.txtQAKBName);
                LinearLayout categoryContainer = suggestionView.findViewById(R.id.qakbContainer);
                TextView txtItem = suggestionView.findViewById(R.id.txtItem);
                LinearLayout itemContainer = suggestionView.findViewById(R.id.itemContainer);

                String item = Utility.selectRandom(categoriesMap,category);

                txtCategoryName.setText(category);
                txtItem.setText(item);

                activeSuggestionMap.put(category, item); // yes, update this

                int categoryThemeColor = categoryColorMapping.get(category);
                int complimentaryColor = Utility.getContrastVersionForColor(categoryThemeColor);

                categoryContainer.setBackgroundColor(categoryThemeColor);
                txtCategoryName.setTextColor(complimentaryColor);

                itemContainer.setBackgroundColor(complimentaryColor);
                txtItem.setTextColor(categoryThemeColor);

                suggestionView.getBackground().setAlpha(20);

                flexboxLayout.addView(suggestionView);
            }
        }
    }

    private JSONObject initCategoriesMap(String selectedQAKB) {
        JSONObject map = new JSONObject();
        if(DBAdapter.existsDictionaryKey(selectedQAKB)){
            try {
                JSONObject qakb = new JSONObject(DBAdapter.fetchDictionaryEntry(selectedQAKB));
                return qakb;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return map;
    }

    /*protected ArrayList<String> performStochasticQAKBSelection() {
        Random random = new Random();
        int seed = (int)(allQAKBList == null? 1:  Math.ceil(allQAKBList.size() * 0.5));
        seed = seed > 0 ? seed : 1;
        int howMany = random.nextInt(seed) % SUGGESTION_LIMIT;
        // should we really allow AI to always suggest or sometimes say, "no, there's no suggestion from me at this time, go figure things out on your own, human!"
        howMany = howMany > 0 ? howMany : 1;

        setRandomAnySelection.clear();

        for(int i = 0; i < howMany; i++){
            setRandomAnySelection.add(Utility.selectRandom(allQAKBList));
        }

        return Utility.setToList(setRandomAnySelection);
    }*/

    public void setQAKBs(JSONArray allQAKBListCache) {
        this.categoriesMap = new JSONObject();
        this.selectedQAKBs = new ArrayList<>(); // reset
        this.allQAKBList = Utility.JSONArrayToList(allQAKBListCache);
        categoryColorMapping = new HashMap<>();
        for(int i = 0; i < allQAKBList.size(); i ++) {
            String category = allQAKBList.get(i);
            categoryColorMapping.put(category, Utility.getRandomColor());
        }
    }

    public HashMap<String, String> getSuggestionMap() {
        //  then resolve their mappings
        getAndRenderSuggestions();
        // then return suggestion map
        return activeSuggestionMap;
    }

    public void toggleState(String expression) {
        webView.loadUrl("javascript: (function(){document.getElementById('string').value ='" + expression + "';changeEvent.trigger();})();");
    }

    public void setSelectedQAKBs(ArrayList<String> values) {
        this.selectedQAKBs = values;
        //TODO:
        // updateLookupTable();
        // we could have cached in the calling activity, however, for now, let's do it here
        cacheSELECTEDQAKB(this.selectedQAKBs);
    }

    private void cacheSELECTEDQAKB(ArrayList<String> qAKBNameList) {
        DBAdapter dbAdapter = getDBAdapter();
        JSONArray jChosenQAKBList = Utility.ListToJSONArray(qAKBNameList);

        if (dbAdapter.existsDictionaryKey(MainActivity.KEYS.CACHED_CHOSEN_QAKB_LIST)) {
            dbAdapter.updateDictionaryEntry(new DBAdapter.DictionaryKeyValue(MainActivity.KEYS.CACHED_CHOSEN_QAKB_LIST, jChosenQAKBList.toString()));
        } else {
            dbAdapter.createDictionaryEntry(new DBAdapter.DictionaryKeyValue(MainActivity.KEYS.CACHED_CHOSEN_QAKB_LIST, jChosenQAKBList.toString()));
        }
    }

    public void setDBAdapter(DBAdapter dbAdapter) {
        this.DBAdapter = dbAdapter;
    }

    public DBAdapter getDBAdapter() {
        return DBAdapter;
    }
}