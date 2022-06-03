package com.example.project72471;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.example.project72471.Hydration.ReminderService;
import com.example.project72471.Menu.OptionsMenuActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project72471.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class TrainingActivity extends OptionsMenuActivity implements FragmentManager.OnBackStackChangedListener {

    private Toolbar toolbar;
    private DatabaseReference reference;


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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userUid = user.getUid();
        reference = FirebaseDatabase.getInstance().getReference().child("Users").child(userUid).child("Hydration");

        ValueEventListener postListener = (new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean notifyTurnedOn = false;
                int hour = 0, minute = 0;
                String endTime = null;
                String startTime = null;
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                int interval = 0;
                Object data = dataSnapshot.child("notify_turned_on").getValue();
                if (data != null) {
                    notifyTurnedOn = (boolean) data;
                }
                if (notifyTurnedOn) {
                    data = dataSnapshot.child("end_hour").getValue();
                    if (data != null) {

                        hour = ((Long) data).intValue();
                        minute = ((Long) dataSnapshot.child("end_min").getValue()).intValue();
                        endTime = hour + ":" + minute;
                    }
                    data = dataSnapshot.child("start_hour").getValue();
                    if (data != null) {
                        hour = ((Long) data).intValue();
                        minute = ((Long) dataSnapshot.child("start_min").getValue()).intValue();
                        startTime = hour + ":" + minute;
                    }
                    data = dataSnapshot.child("notification_interval").getValue();
                    if (data != null) {
                        interval = ((Long) data).intValue();
                    }

                    Date d1 = null;
                    Date d2 = null;
                    Date d3 = null;
                    String now = DateFormat.getTimeInstance().format(new Date());
                    try {
                        d1 = sdf.parse(startTime);
                        d2 = sdf.parse(endTime);
                        d3 = sdf.parse(now);
                        if ((d1.getTime() <= d3.getTime() && d3.getTime() <= d2.getTime()) && interval != 0) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(System.currentTimeMillis());
                            calendar.set(Calendar.HOUR_OF_DAY, hour);
                            calendar.set(Calendar.MINUTE, minute);


                            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                            long when = System.currentTimeMillis();         // notification time
                            Intent reminder = new Intent(TrainingActivity.this, ReminderService.class);
                            PendingIntent pendingIntent = PendingIntent.getService(TrainingActivity.this, 0, reminder, 0);
                            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis()+(2 * 1000), (interval * 60 * 1000), pendingIntent);

                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        reference.addValueEventListener(postListener);

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
