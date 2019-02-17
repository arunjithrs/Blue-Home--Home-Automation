package com.example.arunjith.bluehome;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter BTAdapter;
    public static int REQUEST_BLUETOOTH = 1;
    ListView listView;
    private OutputStream outputStream;
    private InputStream inStream;


    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    LinearLayout lightBtn;
    LinearLayout fanBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setBackgroundDrawable(
                new ColorDrawable(Color.parseColor("#060606")));

        lightBtn = (LinearLayout) findViewById(R.id.lightBtn);
        fanBtn = (LinearLayout) findViewById(R.id.fanBtn);


        listView = (ListView) findViewById(R.id.deviceList);
        fanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    write("f");
                    write("s");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        lightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    write("l");
                    write("s");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        // Phone does not support Bluetooth so let the user know and exit.
        if (BTAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        if (!BTAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_BLUETOOTH);
        }

        if (BTAdapter.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = BTAdapter.getBondedDevices();

            List<String> s = new ArrayList<String>();
            for (BluetoothDevice bt : pairedDevices) {
                s.add(bt.getName());

                if (bt.getName().equals("HC-05")) {
                    try {
                        getListDevices(bt);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            listView.setAdapter(new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, s));

        }
    }

    public void getListDevices(BluetoothDevice bt) throws IOException {
        Toast.makeText(this, "Connected ", Toast.LENGTH_LONG).show();

        BluetoothDevice device = (BluetoothDevice) bt;
        ParcelUuid[] uuids = device.getUuids();

        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
        socket.connect();

        outputStream = socket.getOutputStream();
        inStream = socket.getInputStream();

        write("s");
        beginListenForData();

    }

    public void write(String s) throws IOException {
        outputStream.write(s.getBytes());
    }

//    public void read(BluetoothSocket socket) throws IOException {
//
//        InputStream socketInputStream = socket.getInputStream();
//        byte[] buffer = new byte[256];
//        int bytes;
//
//        // Keep looping to listen for received messages
//       // while (true) {
//            try {
//                bytes = socketInputStream.read(buffer);            //read bytes from input buffer
//                String readMessage = new String(buffer, 0, bytes);
//                // Send the obtained bytes to the UI Activity via handler
//                Log.i("logging", readMessage + "");
//            } catch (IOException e) {
//          //      break;
//            }
//        //}
//    }

//    public void run() {
//        final int BUFFER_SIZE = 1024;
//        byte[] buffer = new byte[BUFFER_SIZE];
//        int bytes = 0;
//        int b = BUFFER_SIZE;
//
//        while (true) {
//            try {
//                bytes = inStream.read(buffer, bytes, BUFFER_SIZE - bytes);
//                Log.i("logging", bytes + "");
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }



    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = inStream.available();
                        if(bytesAvailable > 0)
                        {

                            final int BUFFER_SIZE = 1024;
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int bytes = 0;
                            int b = BUFFER_SIZE;

                            bytes = inStream.read(buffer, bytes, BUFFER_SIZE - bytes);
                            final String readMessage = new String(buffer, 0, bytes);
                            Log.i("logging", readMessage + "");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    // light on fan off
                                    if (readMessage.equals("1"))

                                    {
                                        changeText("light", "off");
                                    }

                                    // light off fan off
                                    if (readMessage.equals("2"))

                                    {
                                        changeText("light", "on");

                                    }

                                    // light off fan off
                                    if (readMessage.equals("3"))

                                    {
                                        changeText("fan", "off");
                                    }

                                    // light on fan on
                                    if (readMessage.equals("4"))

                                    {
                                        changeText("fan", "on");
                                    }
                                }
                            });

                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public void changeText(String item, String state) {

        TextView lightText = (TextView) findViewById(R.id.lightText);
        TextView fanText = (TextView) findViewById(R.id.fanText);

        if(item == "light") {
            if(state == "on") {
                lightBtn.setBackgroundColor(getResources().getColor(R.color.colorBkBtn));
                lightText.setText("LIGHT OFF");
            } else {
                lightBtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                lightText.setText("LIGHT ON");
            }
        } else {
            if(state == "on") {
                fanBtn.setBackgroundColor(getResources().getColor(R.color.colorBkBtn));
                fanText.setText("FAN OFF");
            } else {
                fanBtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                fanText.setText("FAN ON");
            }
        }
    }

}


