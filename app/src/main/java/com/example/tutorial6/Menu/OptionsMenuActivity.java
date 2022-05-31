package com.example.tutorial6.Menu;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.tutorial6.CsvFragment;
import com.example.tutorial6.LogIn.RegistrationActivity;
import com.example.tutorial6.MainActivity;
import com.example.tutorial6.R;
import com.example.tutorial6.TrainingActivity;
import com.google.firebase.auth.FirebaseAuth;

import PersonalDetails.PersonalDetailsFragment;


public class OptionsMenuActivity extends AppCompatActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_devices, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.bt_settings) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;
        } else if (id == R.id.load1) {
            Fragment fragmentCSV = new CsvFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.activity_training, fragmentCSV, "terminal").addToBackStack(null).commit();
            return true;
        } else if (id == R.id.personal_details) {
            Fragment fragmentDetails = new PersonalDetailsFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.activity_training, fragmentDetails, "terminal").addToBackStack(null).commit();
            return true;
        } else if (id == R.id.logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent
                    = new Intent(OptionsMenuActivity.this,
                    MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }


}
