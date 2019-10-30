package com.eShopool.AndroidApp.BasicPages;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is a tool which is used to extend the AppCompatActivity.
 */
public class BasicActivity extends AppCompatActivity {
    /**
     * This method implements the super method.
     * The main purpose of this method is to apply system language.
     * @param savedInstanceState current state of the instance.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeAppLanguage();
    }

    /**
     * This function firstly find the current system language.
     * And then set the language of the MyCloudwear as the system language.
     */
    public void changeAppLanguage() {
        String language = Language.getLanguageLocal(this);
        if(language != null){
            // Find the language preference.
            Resources res = getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            /*
             * If the detected language is "English" then set the language of the MyCloudwear.
             * Else set the language to simplified chinese.
             */
            switch (language){
                case "English":
                    conf.setLocale(Locale.ENGLISH);
                    break;
                case "简体中文":
                    conf.setLocale(Locale.SIMPLIFIED_CHINESE);
                    break;
                default:
                    conf.setLocale(Locale.ENGLISH);
                    break;
            }
            res.updateConfiguration(conf, dm);
        }

    }

}
