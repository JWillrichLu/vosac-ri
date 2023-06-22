package com.nuchwezi.vosac.utils;

import android.content.Context;

import com.nuchwezi.vosac.R;
import com.nuchwezi.vosac.utils.nlp.Cleaner;
import com.nuchwezi.vosac.utils.nlp.PorterStemmer;
import com.nuchwezi.vosac.utils.nlp.StopwordRemover;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;

/***
 *  Facilities for the reduction of data structures into simpler forms for further analysis or processing
 ***/
public class ReductionMachines {

    public interface ReductionMachine {
        public String reduce(String input);
    }

    public static class StringReductionMachine implements ReductionMachine {

        private final HashSet<String> stopWordsSet;

        public StringReductionMachine(HashSet<String> stopWordsSet) throws IOException {
            this.stopWordsSet = stopWordsSet;
        }

        @Override
        public String reduce(String input) {
            if(input == null)
                return null;

            // STEP 1
            Cleaner cleaner = new Cleaner();
            String clean_input = cleaner.processText(input);

            // STEP 2
            StopwordRemover stopwordRemover = new StopwordRemover(stopWordsSet);
            String filtered_input = stopwordRemover.processText(clean_input);

            // STEP 3
            PorterStemmer porterStemmer = new PorterStemmer();
            String stemmed_input = porterStemmer.processText(filtered_input);

            // STEP 4
            return stemmed_input;

        }
    }


}
