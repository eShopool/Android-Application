package com.eShopool.AndroidApp.LoginPages;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;


import com.eShopool.AndroidApp.BasicPages.BasicActivity;
import com.eShopool.AndroidApp.Library.GoToNextActivity;
import com.eShopool.AndroidApp.Mainpages.HomepageActivity;
import com.eShopool.AndroidApp.R;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to show a splash.
 */
public class Splash extends BasicActivity {

    private GoToNextActivity thisActivity = new GoToNextActivity(this);
    /**
     * This method implements the super method.
     * The main purpose of this method is to show the splash.
     * The duration is 3 seconds.
     * @param savedInstanceState current state of the instance.
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(() -> {
            SharedPreferences sp = this.getSharedPreferences("config", this.MODE_PRIVATE);
            if(sp.getBoolean("remember",false))
                thisActivity.toNextActivityWithoutParameter(HomepageActivity.class);
            else
                startActivity(new Intent(Splash.this, LoginActivity.class));
            finish();
        }, 3000);

    }
}
