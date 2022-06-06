package com.example.project72471;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project72471.R;


public class StartScreenActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            public void run() {
                try {
                    Intent intent;
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    if (user != null) {
                        intent = new Intent(StartScreenActivity.this, TrainingActivity.class);
                    } else {

                        intent = new Intent(StartScreenActivity.this, MainActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
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
