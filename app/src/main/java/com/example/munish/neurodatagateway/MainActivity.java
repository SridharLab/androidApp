package com.example.munish.neurodatagateway;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Socket socket;
    String Ipaddr = "";
    int Portno = 0;
    boolean serverConnection = false;
    boolean dataMode = false;


    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    Button startButton, stopButton, connectButton,clearButton;
    TextView textView;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    byte[] data;
    int argLength = 0;


    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {

            try {
                argLength = arg0.length;
                data = arg0;
                // Check if DataMode is true or not
                onClickConnect(connectButton);
            } catch (Exception e) {
                e.printStackTrace();
                tvAppend(textView, "ERROR :Probably Out.println error./n");
            }
        }
    };
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            //setUiEnabled(true);
                            serialPort.setBaudRate(115200);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            //if(dataMode) {
                                serialPort.read(mCallback);
                            /*} else{
                                onClickConnect(connectButton);
                            }*/
                            tvAppend(textView,"Serial Connection Opened!\n");
                        } else {
                            tvAppend(textView, "ERROR : PORT NOT OPEN.");
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                        tvAppend(textView, "ERROR : PORT IS NULL.");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                    tvAppend(textView, "ERROR : PERMISSION NOT GRANTED.");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);
        connectButton = (Button) findViewById(R.id.buttonConnect);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        textView = (TextView) findViewById(R.id.textView3);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
    }


    public void onClickStart(View view) {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341 ||deviceVID == 5824)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    public void onClickStop(View view) {
        try{
            if(serialPort != null) {
                serialPort.close();
                tvAppend(textView, "\nSerial Connection Closed! \n");
            }
            if(socket != null) {
                socket.close();
                tvAppend(textView, "\nServer Connection Closed! \n");
            }
        }catch(IOException e) {
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }



    }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }






    //This is the sserver-client code starting from here
    public void onClickConnect(View view) {
        try {
            EditText etip = (EditText) findViewById(R.id.editText1);
            EditText etport = (EditText) findViewById(R.id.editText2);
            String IPadd = etip.getText().toString();
            String port = etport.getText().toString();
            Ipaddr = IPadd;
            Portno = (Integer) Integer.parseInt(port);
            //if(dataMode) {
                new Thread(new MainActivity.ClientThread()).start();
            /*} else {
                new Thread(new MainActivity.SetModeThread()).start();
            }*/
        } catch (NumberFormatException e) {
            e.printStackTrace();
            tvAppend(textView,"ERROR : Port should be given as a number.\n");
        }
    }

    /*class SetModeThread implements Runnable{
        @Override
        public void run() {
            try {
                InetAddress ipaddress = InetAddress.getByName(Ipaddr);
                socket = new Socket(ipaddress, Portno);
                serverConnection = true;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String tempString = in.readLine();
                tvAppend(textView,"INFO : Waiting Start Data Mode Command.\n");
                while(true){
                    if(tempString != null && tempString.contains("StartDataMode")){
                        dataMode = true;
                        tvAppend(textView,"INFO : Received Start Data Mode Command.\n");
                        break;
                    }
                    tempString = in.readLine();
                }

            } catch (UnknownHostException e) {
                tvAppend(textView,"ERROR : Unknown host, connection not established.\n");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                tvAppend(textView,"ERROR : "+e+"\n");
            }
        }
    }*/

    class ClientThread implements Runnable{
        @Override
        public void run() {
            try {
                InetAddress ipaddress = InetAddress.getByName(Ipaddr);
                if(!serverConnection) {
                    socket = new Socket(ipaddress, Portno);
                    serverConnection = true;
                }
                OutputStream os = null;
                os = socket.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                if(dataMode) {
                    os.write(data, 0, argLength);
                } else {
                    String tempString = in.readLine();
                    tvAppend(textView,"INFO : Waiting Start Data Mode Command.\n");
                    while(true){
                        if(tempString != null && tempString.contains("StartDataMode")){
                            dataMode = true;
                            tvAppend(textView,"INFO : Received Start Data Mode Command.\n");
                            break;
                        } else{
                            
                        }
                        tempString = in.readLine();
                    }
                }
            } catch (UnknownHostException e) {
                tvAppend(textView,"ERROR : Unknown host, connection not established.\n");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                tvAppend(textView,"ERROR : "+e+"\n");
            }
        }
    }
}