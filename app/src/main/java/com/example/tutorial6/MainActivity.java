package com.example.tutorial6;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            public void run() {
                try {
                    Intent homeIntent = new Intent(MainActivity.this, TrainingActivity.class);
                    startActivity(homeIntent);
                    finish();

                } catch (Exception ex) {
                    // Here we are logging the exception to see why it happened.
                    Log.e("my app", ex.toString());
                }

            }
        }, 3000);


    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
