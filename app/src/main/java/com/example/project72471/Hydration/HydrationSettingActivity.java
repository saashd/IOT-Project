package com.example.project72471.Hydration;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project72471.R;

import java.util.Calendar;

public class HydrationSettingActivity extends Fragment {

    int cur_hour, cur_min;
    TextView t;
    boolean notifyTurnedOn = false;

    int start_hour;
    int start_min;
    int end_hour;
    int end_min;
    static int notification_interval;
    static int glass_size;
    double quantity;


    private DatabaseReference reference;
    private View view;

    public HydrationSettingActivity() {
        this.start_hour = -1;
        this.start_min = -1;
        this.end_hour = -1;
        this.end_min = -1;
        notification_interval = -1;
        this.quantity = 2.0;
        glass_size = 150;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_hydration_setting, container, false);

        TextView start_of_day = view.findViewById(R.id.start_of_day);
        start_of_day.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                t = (TextView) view.findViewById(R.id.start_of_day);
                showTimePickerDialog(v, "start");

            }
        });

        TextView end_of_day = view.findViewById(R.id.end_of_day);
        end_of_day.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                t = (TextView) view.findViewById(R.id.end_of_day);
                showTimePickerDialog(v, "end");

            }
        });

        Button saveBtn = (Button) view.findViewById(R.id.saveSettingsButton);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveToFB(getString(R.string.start_hour), start_hour);
                saveToFB(getString(R.string.start_min), start_min);
                saveToFB(getString(R.string.end_hour), end_hour);
                saveToFB(getString(R.string.end_min), end_min);
                saveToFB(getString(R.string.notification_interval), notification_interval);
                saveToFB(getString(R.string.quantity), Double.toString(quantity));
                saveToFB(getString(R.string.glass_size), glass_size);
                saveToFB(getString(R.string.notify_turned_on), notifyTurnedOn);
                Toast toast = Toast.makeText(getContext(), "Saved Successfully", Toast.LENGTH_LONG);
                toast.show();
            }
        });


        SwitchCompat reminderSwitch = (SwitchCompat) view.findViewById(R.id.reminderSwitch);
        reminderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    notifyTurnedOn = true;
                } else {
                    notifyTurnedOn = false;
                }
            }
        });


        Spinner spinner = (Spinner) view.findViewById(R.id.notification_interval);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getContext(),
                R.array.notification_interval_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        // set OnItemSelectedListener
        spinner.setOnItemSelectedListener(new SpinnerActivity());


        Spinner spinner_glass_size = (Spinner) view.findViewById(R.id.glass_size);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter_glass_size = ArrayAdapter.createFromResource(this.getContext(),
                R.array.glass_size_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter_glass_size.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner_glass_size.setAdapter(adapter_glass_size);
        // set OnItemSelectedListener
        spinner_glass_size.setOnItemSelectedListener(new SpinnerActivity());


        final EditText quant = (EditText) view.findViewById(R.id.quantity);


        final Calendar c = Calendar.getInstance();


        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Object data = dataSnapshot.child("end_hour").getValue();
                if (data != null) {
                    t = view.findViewById(R.id.end_of_day);
                    int hour, minute;
                    hour = ((Long) data).intValue();
                    minute = ((Long) dataSnapshot.child("end_min").getValue()).intValue();
                    setTime(hour, minute);

                }
                data = dataSnapshot.child("start_hour").getValue();
                if (data != null) {
                    t = (TextView) view.findViewById(R.id.start_of_day);
                    int hour, minute;
                    hour = ((Long) data).intValue();
                    minute = ((Long) dataSnapshot.child("start_min").getValue()).intValue();
                    setTime(hour, minute);
                }
                data = dataSnapshot.child("notify_turned_on").getValue();
                if (data != null) {
                    notifyTurnedOn = (boolean) data;
                    reminderSwitch.setChecked(notifyTurnedOn);
                }
                data = dataSnapshot.child("glass_size").getValue();
                if (data != null) {

                    int spinnerVal = ((Long) data).intValue();
                    int spinnerPosition = adapter_glass_size.getPosition(Integer.toString(spinnerVal));
                    spinner_glass_size.setSelection(spinnerPosition);
                }
                data = dataSnapshot.child("notification_interval").getValue();
                if (data != null) {

                    int spinnerVal = ((Long) data).intValue();
                    int spinnerPosition = adapter.getPosition(Integer.toString(spinnerVal));
                    spinner.setSelection(spinnerPosition);

                }
                data = dataSnapshot.child("quantity").getValue();
                if (data != null) {
                    String saved_quantity;
                    saved_quantity = (String) data;
                    quant.setText(saved_quantity);
                    quant.addTextChangedListener(new TextWatcher() {

                        public void afterTextChanged(Editable s) {
                            if (s.length() > 0) {
                                quantity = Double.parseDouble(quant.getText().toString());
                            }

                        }

                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            quantity = Double.parseDouble(saved_quantity);
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return view;
    }

    /* --------------------------------------------------- Helper functions ----------------------------------------- */


    public void setTime(int hour, int min) {
        this.cur_hour = hour;
        this.cur_min = min;
        this.setTimeInstanceVariable(t);
        this.setTimeTextView(t);
    }

    public void setTimeInstanceVariable(TextView t) {
        if (t == ((TextView) view.findViewById(R.id.start_of_day))) {
            start_hour = cur_hour;
            start_min = cur_min;
        } else {
            end_hour = cur_hour;
            end_min = cur_min;
        }

    }

    public void setTimeTextView(TextView t) {
        int h = cur_hour % 12, m = cur_min;
        String suffix;

        if (cur_hour == 12)
            h = 12;

        if (cur_hour >= 12) {
            suffix = "PM";
        } else {
            suffix = "AM";
        }
        if (cur_min < 10)
            t.setText(Integer.toString(h) + ":0" + Integer.toString(m) + " " + suffix);
        else
            t.setText(Integer.toString(h) + ":" + Integer.toString(m) + " " + suffix);
    }

    public void showTimePickerDialog(View v, String time) {
        Bundle args = new Bundle();
        args.putString("time", time);
        FragmentManager fm = getActivity().getFragmentManager();
        TimePickerFragment dialog = new TimePickerFragment();
        dialog.setArguments(args);

        dialog.show(fm, "timePicker");


    }


    public void initialSetup() {

    }


    private void saveToFB(String key, Object val) {
        reference.child(key).setValue(val);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        reference = FirebaseDatabase.getInstance().getReference().child("Users").child(userUid).child("Hydration");

    }

    /* --------------------------------------------------- Inner Class ------------------------------------------- */

    public static class SpinnerActivity extends Activity implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view,
                                   int pos, long id) {
            // An item was selected. You can retrieve the selected item using
            if (parent.getId() == R.id.glass_size)
                glass_size = Integer.parseInt(parent.getItemAtPosition(pos).toString());
            else if (parent.getId() == R.id.notification_interval)
                notification_interval = Integer.parseInt(parent.getItemAtPosition(pos).toString());

        }

        public void onNothingSelected(AdapterView<?> parent) {
            if (parent.getId() == R.id.glass_size)
                glass_size = Integer.parseInt(parent.getItemAtPosition(0).toString());
            else if (parent.getId() == R.id.notification_interval)
                notification_interval = Integer.parseInt(parent.getItemAtPosition(0).toString());
        }
    }


    public void showNotification(Context context, Intent intent, int reqCode) {

        PendingIntent pendingIntent = PendingIntent.getActivity(context, reqCode, intent, PendingIntent.FLAG_ONE_SHOT);
        String CHANNEL_ID = "channel_name";// The id of the channel.
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.water_glass)
                .setContentTitle("Drink water")
                .setContentText("It's time to have a glass of water!")
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel Name";// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationManager.createNotificationChannel(mChannel);
        }
        notificationManager.notify(reqCode, notificationBuilder.build()); // 0 is the request code, it should be unique id

        Log.d("showNotification", "showNotification: " + reqCode);
    }

}
