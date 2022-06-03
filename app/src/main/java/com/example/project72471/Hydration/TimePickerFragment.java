package com.example.project72471.Hydration;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    private String time;
    private DatabaseReference reference;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        reference = FirebaseDatabase.getInstance().getReference().child("Users").child(userUid).child("Hydration");

        time = getArguments().getString("time");

        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (time.equals("end")) {
            reference.child("end_min").setValue(minute);
            reference.child("end_hour").setValue(hourOfDay);
        }
        else{
            reference.child("start_min").setValue(minute);
            reference.child("start_hour").setValue(hourOfDay);
        }

    }

}
