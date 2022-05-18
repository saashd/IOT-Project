package com.example.tutorial6;
// binds bluetooth service. Maintains the graph

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {


    private double lastMag = 0d;
    private double avgMag = 0d;
    private double netMag = 0d;
    private Integer stepCount = 0;
    private static int SMOOTHING_WINDOW_SIZE = 20;

    private float mRawAccelValues[] = new float[3];

    // smoothing accelerometer signal variables
    private float mAccelValueHistory[][] = new float[3][SMOOTHING_WINDOW_SIZE];
    private float mRunningAccelTotal[] = new float[3];
    private float mCurAccelAvg[] = new float[3];
    private int mCurReadIndex = 0;

    public static float mStepCounter = 0;
    public static float mStepCounterAndroid = 0;
    public static float mInitialStepCount = 0;

    private double mGraph1LastXValue = 0d;
    private double mGraph2LastXValue = 0d;

    //peak detection variables
    private double lastXPoint = 1d;
    double stepThreshold = 1.0d;
    double noiseThreshold = 2d;
    private int windowSize = 10;

    private TextView textView;

    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private String selectedMode;
    private String fileName;
    private String numOfSteps;
    private Boolean isReceiving = false;

    private List<String[]> receivedData = new ArrayList<>();

    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;


    LineChart mpLineChart;
    LineDataSet lineDataSetAxisX;
    LineDataSet lineDataSetAxisY;
    LineDataSet lineDataSetAxisZ;
    LineDataSet lineDataSetAxisAcc;

    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    LineData data;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
        selectedMode = getArguments().getString("mode");
        fileName = getArguments().getString("fileName");
        numOfSteps = getArguments().getString("numOfSteps");


    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
//        textView = view.findViewById(R.id.maintv1);

        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));

        mpLineChart = (LineChart) view.findViewById(R.id.line_chart);
        mpLineChart.getDescription().setText("Time [sec]");

        lineDataSetAxisX = new LineDataSet(emptyDataValues(), "X-axis");
        lineDataSetAxisX.setColors(Color.RED);

        lineDataSetAxisY = new LineDataSet(emptyDataValues(), "Y-axis");
        lineDataSetAxisY.setColors(Color.GREEN);

        lineDataSetAxisZ = new LineDataSet(emptyDataValues(), "Z-axis");
        lineDataSetAxisZ.setColors(Color.BLUE);

        lineDataSetAxisAcc = new LineDataSet(emptyDataValues(), "Acc. Norm");
        lineDataSetAxisAcc.setColors(Color.YELLOW);

        //        Set dataset labels that appear in the bottom of the chart
        Legend l = mpLineChart.getLegend();
        l.setTextSize(15f);
        l.setTextColor(Color.BLACK);
        l.setForm(Legend.LegendForm.LINE);
        XAxis xval = mpLineChart.getXAxis();

        xval.setDrawLabels(true);
        xval.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xval.setGranularity(1f);


        dataSets.add(lineDataSetAxisX);
        dataSets.add(lineDataSetAxisY);
        dataSets.add(lineDataSetAxisZ);
        dataSets.add(lineDataSetAxisAcc);

        data = new LineData(dataSets);
        mpLineChart.setData(data);
        mpLineChart.invalidate();

        Button buttonReset = (Button) view.findViewById(R.id.resetButton);
        Button buttonSaveData = (Button) view.findViewById(R.id.saveButton);
        Button buttonStop = (Button) view.findViewById(R.id.stopButton);
        Button buttonStart = (Button) view.findViewById(R.id.startButton);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Start recording if first time or if data was stopped or reset been made
                Toast.makeText(getContext(), "Recording Started", Toast.LENGTH_SHORT).show();
                if (!isReceiving) {
                    isReceiving = true;
                }
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseReceiving();

            }
        });


        buttonSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseReceiving();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setHint("Update Number of Steps");

                String currSteps = numOfSteps;
                builder.setMessage("Current Number of Steps: " + currSteps)
                        .setView(input)
                        .setCancelable(false)
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                numOfSteps = input.getText().toString();
                                saveToCsv();
                                reset();
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Return", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //  Action for 'NO' Button
                                dialog.cancel();
                            }
                        });
                //Creating dialog box
                AlertDialog alert = builder.create();
                alert.show();

            }
        });

        buttonReset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                reset();
                Toast.makeText(getContext(), "Reset", Toast.LENGTH_SHORT).show();
            }

        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
//        if (id == R.id.clear) {
//            receiveText.setText("");
//            return true;
//        } else
        if (id == R.id.load2) {
            pauseReceiving();
            Fragment fragment = new CsvFragment();
            getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
            return true;

        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private String[] clean_str(String[] stringsArr) {
        for (int i = 0; i < stringsArr.length; i++) {
            stringsArr[i] = stringsArr[i].replaceAll(" ", "");
        }


        return stringsArr;
    }

    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    // Wont be used in our project
    private void send(String str) {
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if (hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void saveToCsv() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());

            File file = new File("/storage/self/primary/IOT/");
            file.mkdirs();
            String csv = "/storage/self/primary/IOT/" + fileName + ".csv";

//            File csvFile = new File("/storage/self/primary/Terminal/", fileName + ".csv");
//            if (!csvFile.exists()) {
//            }
            CSVWriter csvWriter = new CSVWriter(new FileWriter(csv, false));
            String[] row = new String[]{"NAME:", fileName + ".csv"};
            csvWriter.writeNext(row);
            row = new String[]{"EXPERIMENT TIME:", currentDateandTime};
            csvWriter.writeNext(row);
            row = new String[]{"ACTIVITY TYPE:", selectedMode};
            csvWriter.writeNext(row);
            row = new String[]{"COUNT OF ACTUAL STEPS", numOfSteps};
            csvWriter.writeNext(row);
            row = new String[]{"   "};
            csvWriter.writeNext(row);
            row = new String[]{"Time [sec]", "ACC X", "ACC Y", "ACC Z"};
            csvWriter.writeNext(row);
            for (String[] r : receivedData) {
                csvWriter.writeNext(r);
            }
            csvWriter.close();
            Toast.makeText(getContext(), "Data Saved", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(getContext(), "ERROR", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void reset() {
        //Reset Saved Data
        isReceiving = false;
        receivedData = new ArrayList<>();

        //Clear displayed graph
        LineData data = mpLineChart.getData();
        ILineDataSet set1 = data.getDataSetByIndex(0);
        ILineDataSet set2 = data.getDataSetByIndex(1);
        ILineDataSet set3 = data.getDataSetByIndex(2);
        ILineDataSet set4 = data.getDataSetByIndex(3);
        set1.removeLast();
        set2.removeLast();
        set3.removeLast();
        set4.removeLast();
        while (set1.removeLast()) {
        }
        while (set2.removeLast()) {
        }
        while (set3.removeLast()) {

        }
        while (set4.removeLast()) {

        }
        mpLineChart.notifyDataSetChanged();
        mpLineChart.invalidate();
        receiveText.setText("");

    }

    private void pauseReceiving() {
        isReceiving = false;
        Toast.makeText(getContext(), "Data Receiving Paused", Toast.LENGTH_SHORT).show();
    }


    // Updates done while message received from the device
    @SuppressLint("SetTextI18n")
    private void receive(byte[] message) {
        if (hexEnabled) {
            receiveText.append(TextUtil.toHexString(message) + '\n');
        } else {
            String msg = new String(message);
            if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                String msg_to_save = msg;
                msg_to_save = msg.replace(TextUtil.newline_crlf, TextUtil.emptyString);
                if (msg_to_save.length() > 1) {

                    String[] parts = msg_to_save.split(",");
                    parts = clean_str(parts);

                    mRawAccelValues[0] = Float.parseFloat(parts[0]);
                    mRawAccelValues[1] = Float.parseFloat(parts[1]);
                    mRawAccelValues[2] = Float.parseFloat(parts[2]);
                    float time_msec = Integer.valueOf(parts[3]);


                    String row[] = new String[]{String.valueOf(time_msec / 1000), parts[0], parts[1], parts[2]};
                    receivedData.add(row);

                    data.addEntry(new Entry(time_msec / 1000, mRawAccelValues[0]), 0);
                    lineDataSetAxisX.notifyDataSetChanged(); // let the data know a dataSet changed

                    data.addEntry(new Entry(time_msec / 1000, mRawAccelValues[1]), 1);
                    lineDataSetAxisY.notifyDataSetChanged(); // let the data know a dataSet changed

                    data.addEntry(new Entry(time_msec / 1000, mRawAccelValues[2]), 2);
                    lineDataSetAxisZ.notifyDataSetChanged(); // let the data know a dataSet changed

//                    lastMag = Math.sqrt(Math.pow(mRawAccelValues[0], 2) + Math.pow(mRawAccelValues[1], 2) + Math.pow(mRawAccelValues[2], 2));
//                    for (int i = 0; i < 3; i++) {
//                        mRunningAccelTotal[i] = mRunningAccelTotal[i] - mAccelValueHistory[i][mCurReadIndex];
//                        mAccelValueHistory[i][mCurReadIndex] = mRawAccelValues[i];
//                        mRunningAccelTotal[i] = mRunningAccelTotal[i] + mAccelValueHistory[i][mCurReadIndex];
//                        mCurAccelAvg[i] = mRunningAccelTotal[i] / SMOOTHING_WINDOW_SIZE;
//                    }
//                    mCurReadIndex++;
//                    if (mCurReadIndex >= SMOOTHING_WINDOW_SIZE) {
//                        mCurReadIndex = 0;
//                    }

//                    avgMag = Math.sqrt(Math.pow(mCurAccelAvg[0], 2) + Math.pow(mCurAccelAvg[1], 2) + Math.pow(mCurAccelAvg[2], 2));
//
//                    netMag = lastMag - avgMag; //removes gravity effect
//
//                    data.addEntry(new Entry(time_msec / 1000, (float) netMag), 3);
//                    lineDataSetAxisAcc.notifyDataSetChanged(); // let the data know a dataSet changed
//
//                    textView.setText(stepCount.toString());

                    mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                    mpLineChart.invalidate(); // refresh

                }

                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                // sand here msg to function that saves it to csv
                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg.charAt(0) == '\n') {
                    Editable edt = receiveText.getEditableText();
                    if (edt != null && edt.length() > 1)
                        edt.replace(edt.length() - 2, edt.length(), "");
                }
                pendingNewline = msg.charAt(msg.length() - 1) == '\r';
            }
            receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
        }
    }


    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        if (isReceiving) {
            try {
                receive(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private ArrayList<Entry> emptyDataValues() {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        return dataVals;
    }

    private void OpenLoadCSV() {
        Intent intent = new Intent(getContext(), LoadCSV.class);
        intent.putExtra("fileName", fileName);
        intent.putExtra("numOfSteps", numOfSteps);
        startActivity(intent);
    }

}
