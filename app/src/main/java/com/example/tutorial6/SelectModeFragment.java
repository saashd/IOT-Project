package com.example.tutorial6;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SelectModeFragment} factory method to
 * create an instance of this fragment.
 */
public class SelectModeFragment extends Fragment {

    Spinner spinner;
    Button startButton;
    String selectedMode;
    EditText textFileName;
    private String deviceAddress;


    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_select_mode, container, false);
        spinner = (Spinner) view.findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.modes_array, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMode = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        textFileName = (EditText) view.findViewById(R.id.textFileName);
        startButton = (Button) view.findViewById(R.id.startButton);


        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getContext(), "Recording Started", Toast.LENGTH_SHORT).show();

                Bundle args = new Bundle();
                args.putString("mode", selectedMode);
                args.putString("device", deviceAddress);


                String currFileName = textFileName.getText().toString();
                if (currFileName.matches("")) {
                    Toast.makeText(getContext(), "You did not enter a fille name", Toast.LENGTH_SHORT).show();
                    return;
                }

                args.putString("fileName", currFileName);
                Fragment fragment = new TerminalFragment();
                fragment.setArguments(args);
                getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
            }
        });


        // Inflate the layout for this fragment
        return view;
    }


}