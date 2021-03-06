package com.example.project72471.PersonalDetails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project72471.R;


public class PersonalDetailsFragment extends Fragment {

    private EditText age;
    private EditText height;
    private EditText weight;
    private EditText emergency;
    public Integer maxHR;
    public double bmi;
    private String gender;
    private String emergencyEmail;
    private int ageVal;
    private int heightVal;
    private int weightVal;
    DatabaseReference reference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        reference = FirebaseDatabase.getInstance().getReference().child("Users").child(userUid);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_details, container, false);


        Button applyBtn = (Button) view.findViewById(R.id.applyButton);

        age = (EditText) view.findViewById(R.id.age);
        height = (EditText) view.findViewById(R.id.height);
        weight = (EditText) view.findViewById(R.id.weight);
        emergency = (EditText) view.findViewById(R.id.emergency);
        Spinner genderSpinner = view.findViewById(R.id.genderSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.gender_array, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        genderSpinner.setAdapter(adapter);

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                gender = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Object data = dataSnapshot.child("gender").getValue();
                if (data != null) {
                    gender = (String) data;
                    int spinnerPosition = adapter.getPosition(gender);
                    genderSpinner.setSelection(spinnerPosition);
                }
                data = dataSnapshot.child("ageVal").getValue();
                if (data != null) {
                    ageVal = ((Long) data).intValue();
                    age.setText(String.valueOf(ageVal));
                }
                data = dataSnapshot.child("heightVal").getValue();
                if (data != null) {

                    heightVal = ((Long) data).intValue();
                    height.setText(String.valueOf(heightVal));
                }
                data = dataSnapshot.child("weightVal").getValue();
                if (data != null) {
                    weightVal = ((Long) data).intValue();
                    weight.setText(String.valueOf(weightVal));
                }
                data = dataSnapshot.child("emergencyContact").getValue();
                if (data != null) {
                    emergencyEmail = (String) data;
                    emergency.setText(String.valueOf(emergencyEmail));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ageVal = Integer.parseInt(age.getText().toString());
                heightVal = Integer.parseInt(height.getText().toString());
                weightVal = Integer.parseInt(weight.getText().toString());
                maxHR = 200 - ageVal;
                double heightMeters = heightVal / 100.0;
                bmi = weightVal / (heightMeters * heightMeters);

                reference.child("gender").setValue(gender);
                reference.child("ageVal").setValue(ageVal);
                reference.child("heightVal").setValue(heightVal);
                reference.child("weightVal").setValue(weightVal);
                reference.child("maxHR").setValue(maxHR);
                reference.child("bmi").setValue(bmi);
                reference.child("emergencyContact").setValue(emergency.getText().toString());

                Bundle args = new Bundle();
                args.putString("maxHR", String.valueOf(maxHR));
                args.putString("bmi", String.valueOf(bmi));

                Fragment fragment = new HealthDetailsFragment();
                fragment.setArguments(args);

                getFragmentManager().beginTransaction().replace(R.id.activity_training, fragment, "terminal").addToBackStack(null).commit();


            }
        });
        return view;

    }

}