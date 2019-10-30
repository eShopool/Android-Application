package com.eShopool.AndroidApp.Library;

import android.content.Context;
import android.content.Intent;

import java.io.Serializable;
import java.util.List;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is a tool which is used to switch activities.
 */
public class GoToNextActivity {

    private Context context;

    // Construction.
    public GoToNextActivity(Context context){
        this.context = context;
    }

    /**
     * This function is used to start another activity without any parameters.
     * @param cls
     */
    public void toNextActivityWithoutParameter(Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        context.startActivity(intent);
    }

    /**
     * This function is used to start another activity with user phone number.
     * @param cls the current activity's class.
     * @param query the user query string.
     */
    public void toNextActivityWithParameter(Class<?> cls, String query) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        intent.putExtra("query", query);
        context.startActivity(intent);
    }
}
