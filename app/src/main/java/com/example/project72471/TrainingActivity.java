package com.example.project72471;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.example.project72471.Menu.OptionsMenuActivity;
import com.project72471.R;


public class TrainingActivity extends OptionsMenuActivity implements FragmentManager.OnBackStackChangedListener {

    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        getSupportFragmentManager().addOnBackStackChangedListener(this);

        if (ContextCompat.checkSelfPermission(TrainingActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TrainingActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().add(R.id.activity_training, new DevicesFragment(), "devices").commit();
        else
            onBackStackChanged();

    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


}
