package com.example.tutorial6;
// fragment where we chose the device

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.ArrayList;
import java.util.Collections;

public class DevicesFragment extends ListFragment {

    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, listItems) {
            @NonNull
            @Override
            public View getView(int position, View view, @NonNull ViewGroup parent) {
                BluetoothDevice device = listItems.get(position);

                //                TODO: remove this. Only for testing
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(getContext()));
                }

                Python py = Python.getInstance();
                PyObject pyobj = py.getModule("test");
                int[] data = new int[3];
                data = new int[]{6, 2, 3};
                PyObject obj = pyobj.callAttr("main", data);

                if (view == null)
                    view = getActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);

//                TODO: remove this. Only for testing
                Toast.makeText(getContext(), obj.toString(), Toast.LENGTH_SHORT).show();

                TextView text1 = view.findViewById(R.id.text1);
                TextView text2 = view.findViewById(R.id.text2);
                text1.setText(device.getName());
                text2.setText(device.getAddress());
                return view;
            }
        };
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        View header = getActivity().getLayoutInflater().inflate(R.layout.device_list_header, null, false);
        getListView().addHeaderView(header, null, false);
        setEmptyText("initializing...");
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        setListAdapter(listAdapter);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_devices, menu);
        if (bluetoothAdapter == null)
            menu.findItem(R.id.bt_settings).setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bluetoothAdapter == null)
            setEmptyText("<bluetooth not supported>");
        else if (!bluetoothAdapter.isEnabled())
            setEmptyText("<bluetooth is disabled>");
        else
            setEmptyText("<no bluetooth devices found>");
        refresh();
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
            Fragment fragment = new CsvFragment();
            getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    void refresh() {
        listItems.clear();
        if (bluetoothAdapter != null) {
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                    listItems.add(device);
        }
        Collections.sort(listItems, DevicesFragment::compareTo);
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        BluetoothDevice device = listItems.get(position - 1);
        Bundle args = new Bundle();
        args.putString("device", device.getAddress());

        //Before mod.
//        Fragment fragment = new TerminalFragment();
//        fragment.setArguments(args);
//        getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();

        // After mod. Displays selection of mode( run/walk ) and start button (second fragment)
        Fragment modeFragment = new SelectModeFragment();
        modeFragment.setArguments(args);
        getFragmentManager().beginTransaction().replace(R.id.fragment, modeFragment, "terminal").addToBackStack(null).commit();
    }

    /**
     * sort by name, then address. sort named devices first
     */
    static int compareTo(BluetoothDevice a, BluetoothDevice b) {
        boolean aValid = a.getName() != null && !a.getName().isEmpty();
        boolean bValid = b.getName() != null && !b.getName().isEmpty();
        if (aValid && bValid) {
            int ret = a.getName().compareTo(b.getName());
            if (ret != 0) return ret;
            return a.getAddress().compareTo(b.getAddress());
        }
        if (aValid) return -1;
        if (bValid) return +1;
        return a.getAddress().compareTo(b.getAddress());
    }
}
