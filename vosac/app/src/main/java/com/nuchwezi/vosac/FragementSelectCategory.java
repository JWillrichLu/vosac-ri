package com.nuchwezi.vosac;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.flexbox.FlexboxLayout;
import com.nuchwezi.xlitedatabase.DBAdapter;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import static com.nuchwezi.vosac.Utility.Tag;

public class FragementSelectCategory extends Fragment {
    protected static final int SUGGESTION_LIMIT = 1;
    View rootView;
    private JSONArray qakbList;
    private ParametricListCallback selectionChangedCallback;
    HashSet<String> setSelected = new HashSet<>(), userSelected = new HashSet<>();
    //HashSet<String> allSelected = new HashSet<>();
    private ArrayList<String> categoriesList;
    HashSet<View> addedViews = new HashSet<>();
    private DBAdapter DBAdapter;
    private ArrayList<String> chosenQAKBs;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragment_one = inflater.inflate(R.layout.fragment_select_categories, container, false);
        rootView = fragment_one;
        renderCategories();
        return fragment_one;
    }

    public void setQAKBs(JSONArray allQAKBListCache, ArrayList<String> selectedQAKBs) {
        this.qakbList = allQAKBListCache;
        this.categoriesList = Utility.JSONArrayToList(allQAKBListCache);

        if(selectedQAKBs != null)
        for(String s: selectedQAKBs)
            userSelected.add(s);

        renderCategories();
    }

    public void setDBAdapter(DBAdapter dbAdapter) {
        this.DBAdapter = dbAdapter;
    }

    private void loadCachedChosenQAKB() {
        if(DBAdapter.existsDictionaryKey(MainActivity.KEYS.CACHED_CHOSEN_QAKB_LIST)){
            try {
                chosenQAKBs = Utility.JSONArrayToList(new JSONArray(DBAdapter.fetchDictionaryEntry(MainActivity.KEYS.CACHED_CHOSEN_QAKB_LIST)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            chosenQAKBs = new ArrayList<>();
        }

        if(chosenQAKBs != null)
            for(String s: chosenQAKBs)
                userSelected.add(s);
    }

    private void renderCategories() {
        if(qakbList == null)
            return;

        // first, check if the query matches anything in the active QAKBs, if it does, answer...
        if(userSelected == null | userSelected.size() == 0){
            loadCachedChosenQAKB();
        }

        if(rootView != null){
            FlexboxLayout flexboxLayout = rootView.findViewById(R.id.containerCategories);

            for(View view: addedViews)
                flexboxLayout.removeView(view); //to avoid adding category items multiple times


            for(int i = 0; i < qakbList.length(); i++) {
                String qaKBName = null;
                try {
                    qaKBName = qakbList.getString(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final String category = qaKBName.split("\\|")[0];
                Switch switchCat = new Switch(rootView.getContext());
                switchCat.setText(category);

                if(userSelected.contains(qaKBName))
                    switchCat.setChecked(true);

                flexboxLayout.addView(switchCat);
                addedViews.add(switchCat);
                final String finalQaKBName = qaKBName;
                switchCat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        if(checked){
                            setSelected.add(finalQaKBName);
                            userSelected.add(finalQaKBName);
                        } else {
                            setSelected.remove(finalQaKBName);
                            userSelected.remove(finalQaKBName);
                        }

                        selectionChangedCallback.call(Utility.setToList(userSelected));
                    }
                });
            }
        }
    }

    public void setSelectionChangeCallback(ParametricListCallback parametricListCallback) {
        this.selectionChangedCallback = parametricListCallback;
    }
}