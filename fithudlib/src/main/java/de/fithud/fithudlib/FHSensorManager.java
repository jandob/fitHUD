package de.fithud.fithudlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by jandob on 11/17/14.
 */
public class FHSensorManager extends MessengerService {

    public final class Messages extends MessengerService.Messages {
        public static final int HEARTRATE_MESSAGE = 1;
        public static final int CADENCE_MESSAGE = 2;
        public static final int SPEED_MESSAGE = 3;
        public static final int SENSOR_STATUS_MESSAGE = 4;
        public static final int ACC_RAW_MESSAGE = 5;
        public static final int HEIGTH_MESSAGE = 6;
        public static final int SEARCH_READY = 7;
        public static final int GUIDE_MESSAGE = 8;
        public static final int DISTANCE_MESSAGE = 9;
        public static final int CALORIES_MESSAGE = 10;
    }

    public final class Commands extends MessengerService.Commands {
        public static final int SEARCH_COMMAND = 1;
        public static final int WAKEUP_COMMAND = 2;
        public static final int WHEEL_LIGHT = 3;
        public static final int WHEEL_SPEED = 4;
    }

    @Override
    void handleMessage(Message msg) {
        int [] command = msg.getData().getIntArray("command");

        switch (command[0]){
            case Commands.SEARCH_COMMAND:
                startScan();
                break;

            case Commands.WAKEUP_COMMAND:

                if(spdAccWake_connected == 1) {
                    sendWakeupMessage();
                }
                else{
                    Log.i(TAG,"Wakeup not connected");
                }
                break;

            case Commands.WHEEL_LIGHT:
                if (wheelLightActive) {
                    wheelLightActive = false;
                    startStopLight(wheelLightActive);
                } else {
                    wheelLightActive = true;
                    startStopLight(true);
                }
                Log.d(TAG, "wheel light on: " + wheelLightActive);
                break;
            case Commands.WHEEL_SPEED:
                if (wheelSpeedActive) {
                    wheelSpeedActive = false;
                    showSpeedOnWheel(wheelSpeedActive);
                } else {
                    wheelSpeedActive = true;
                    showSpeedOnWheel(true);
                }
                Log.d(TAG, "wheel speed on: " + wheelSpeedActive);
                break;
        }
    }

    void sendMsgFloat(int messageType, float val){
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {Message msg = Message.obtain(null, messageType);
                Bundle bundle = new Bundle();
                // bundle.putFloat("value", val);
                bundle.putFloat("value", val);
                msg.setData(bundle);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    void sendMsg(int messageType, int[] val) {
        for (int i = mClients.size() - 1; i >= 0; i--) {

            try {Message msg = Message.obtain(null, messageType);
                Bundle bundle = new Bundle();
                // bundle.putFloat("value", val);
                bundle.putIntArray("value", val);
                msg.setData(bundle);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    private Context context;
    private static final String TAG = FHSensorManager.class.getSimpleName();
    private Set<BluetoothDevice> mBtDevices;
    BluetoothAdapter mBtAdapter;
    private List<BluetoothDevice> mBtDevicesReadyToConnect = new ArrayList<BluetoothDevice>();
    private List<String> mConnectableBtDevices = new ArrayList<String>();
    private List<String> mConnectedBtDevices = new ArrayList<String>();
    private List<BluetoothGatt> mConnectedBtDevicesGattServices = new ArrayList<BluetoothGatt>();
    private BluetoothGattCharacteristic wakeupCharacteristic;
    private BluetoothGatt   wakeupGATT;
    //private int stopScanCount = 20;

    // Devices:
    private final String H7 = "00:22:D0:3D:30:31";
    private final String CAD = "C7:9E:DF:E6:F8:D5";
    private final String SPD = "EB:03:59:83:C8:34";
    // Not added yet
    private final String SPD_ACC_WAKE = "02:80:E1:00:00:AA";
    private final String BAROMETER = "D4:BD:70:0E:E9:EE";

    // Services
    private final String HRService = "0000180d-0000-1000-8000-00805f9b34fb";
    private final String SPDCADService = "00001816-0000-1000-8000-00805f9b34fb";
    private final String ACCService =    "02366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    private final String WakeupService = "42821a40-e477-11e2-82d0-0002a5d5c51b";
    private final String BarometerService = "00001110-0000-1000-8000-00805f9b34fb";

    // Characteristics
    private final String WakeupCharacteristicUUID = "a32e5520-e477-11e2-a9e3-0002a5d5c51b";

    private boolean connectionInProgress = false;
    private int nrOfremainingDevices = 0;

    public int speedometer_connected = 0;
    public int heartrate_connected = 0;
    public int cadence_connected = 0;
    public int spdAccWake_connected = 0;
    public int barometer_connected = 0;

    private boolean barometer_calibrated = false;
    private short barometer_offset = 0;

    // Variables for speed calculations
    public static float last_speed = 0;
    public static int last_revolutions = 0;
    private static final double wheel_type = 4.4686;
    private static final double wheel_circumference = 2.2;

    // Variables for cadence calculations

    private static int lastRevolutionsCrank = 0;
    private static float lastSpeedCrank = 0;
    public static int firstWheelRevolution = 0;
    public static int totalWheelRevolution = 0;
    private static float distance = 0;
    private final int age = 24;
    private final double vo2max = 44.60;    // for 2500m
    private final int weight = 70;
    private double calories = 0.0;

    private boolean wheelLightActive = false;
    private boolean wheelSpeedActive = false;


    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    // end listener interface
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            //Log.d(TAG, "found ble device: " + device.getName() + ", UUID: "+ device.getAddress());
            //Log.i(TAG, mConnectableBtDevices.toString());
            if (mConnectableBtDevices.contains(device.getAddress()) && !connectionInProgress) {
                if (!mBtDevicesReadyToConnect.contains(device)) {
                    mBtDevicesReadyToConnect.add(device);
                    Log.i(TAG, "Added " + device.getAddress() + "to the connectable list");
                }
            }
        }
    };

    public void connectToAvailableDevices() {
        for (BluetoothDevice device : mBtDevicesReadyToConnect) {
            if (!mConnectedBtDevices.contains(device.getAddress())) {
                mConnectedBtDevices.add(device.getAddress());
                connectionInProgress = true;
                nrOfremainingDevices -= 1;
                Log.i(TAG,"Remaining: "+nrOfremainingDevices);
                Log.i(TAG, "connect to: " + device.getAddress());
                device.connectGatt(context, false, btleGattCallback);
                break;
            }
        }
        if ((nrOfremainingDevices == 0) && (connectionInProgress == false)){
            Log.i(TAG,"All devices connected");
            mBtDevicesReadyToConnect.clear();
            sendReadySearch();
        }
    }

    public void showSpeedOnWheel(boolean speedWheelSwitch){
        if (spdAccWake_connected == 1) {
            byte[] signal = new byte[1];
            if(speedWheelSwitch) {
                signal[0] = (byte)0xFC;
            }
            else
            {
                signal[0] = (byte)0xFB;
            }
            // Save local in characterstic
            wakeupCharacteristic.setValue(signal);
            // Send characteristic to remote device , afterwards onCharacteristicWrite is called
            wakeupGATT.writeCharacteristic(wakeupCharacteristic);
        }

    }

    public void startStopLight(boolean lightSwitch)
    {
        if (spdAccWake_connected == 1) {
            byte[] signal = new byte[1];
            if(lightSwitch) {
                signal[0] = (byte)0xFE;
            }
            else
            {
                signal[0] = (byte)0xFD;
            }
            // Save local in characterstic
            wakeupCharacteristic.setValue(signal);
            // Send characteristic to remote device , afterwards onCharacteristicWrite is called
            wakeupGATT.writeCharacteristic(wakeupCharacteristic);
        }

    }

    public void sendWakeupMessage(){
        byte[] wakeup_signal = new byte[1];
        wakeup_signal[0] = 0x00;
        // Save local in characterstic
        wakeupCharacteristic.setValue(wakeup_signal);
        // Send characteristic to remote device , afterwards onCharacteristicWrite is called
        wakeupGATT.writeCharacteristic(wakeupCharacteristic);
    }

    public void sendReadySearch(){
        int searchReady[] = new int[1];
        searchReady[0] = 1;
        sendMsg(Messages.SEARCH_READY, searchReady);
    }

    public void sendSensorStatus(){
        int sensorStatus_dataset[] = new int[5];
        sensorStatus_dataset[0] = heartrate_connected;
        sensorStatus_dataset[1] = speedometer_connected;
        sensorStatus_dataset[2] = cadence_connected;
        sensorStatus_dataset[3] = barometer_connected;
        sensorStatus_dataset[4] = spdAccWake_connected;
        sendMsg(Messages.SENSOR_STATUS_MESSAGE, sensorStatus_dataset);
    }

    public static short twoBytesToShort(byte b1, byte b2) {
        return (short) ((b1 << 8) | (b2 & 0xFF));
    }

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG,"Written on device with address: "+gatt.getDevice().getAddress().toString()+" on characteristic with uuid: "+characteristic.getUuid().toString());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            byte[] characteristicData = characteristic.getValue();
            //Log.i(TAG, "received data from characteristic:" + characteristic.getService().getUuid().toString());

            //Log.i(TAG,"Got MSG from characteristic: "+characteristic.getService().getUuid().toString());
            if (characteristic.getService().getUuid().toString().equals(SPDCADService)) {

                // Is cadence indicator set
                if ((characteristicData[0] & (1L << 1)) != 0) {
                    //Log.i(TAG, "Got message");
                    int high_crank = ((int)characteristicData[2]) & 0xff;
                    int low_crank = ((int)characteristicData[1]) & 0xff;
                    int crank_revolutions = (high_crank << 8) | low_crank;

                    int high_time = ((int)characteristicData[4]) & 0xff;
                    int low_time = ((int)characteristicData[3]) & 0xff;
                    int time_cadence = (high_time << 8) | low_time;
                    float timeDifference = 0;
                    int revolutions_difference = crank_revolutions - lastRevolutionsCrank;
                    if (time_cadence < lastSpeedCrank) {
                        timeDifference = (float) time_cadence + 65536 - lastSpeedCrank;
                    } else {
                        timeDifference = (float) time_cadence - lastSpeedCrank;
                    }
                    lastSpeedCrank = (float) time_cadence;
                    lastRevolutionsCrank = crank_revolutions;
                    timeDifference = timeDifference / 1024;

                    float cadenceRpm = 0;
                    if (timeDifference > 0) {
                        cadenceRpm = ((float)(revolutions_difference) / timeDifference) * (float)60;
                        Log.v(TAG, "Cadence : " + cadenceRpm);
                        sendMsgFloat(Messages.CADENCE_MESSAGE, cadenceRpm);
                    } else {
                        cadenceRpm = 0;
                    }


                    // Speed indicator is set
                }  else if ((characteristicData[0] & (1L << 0)) != 0) {
                    int fourth_wheel = ((int)characteristicData[4]) & 0xff;
                    int third_wheel = ((int)characteristicData[3]) & 0xff;
                    int second_wheel = ((int)characteristicData[2]) & 0xff;
                    int first_wheel = ((int)characteristicData[1]) & 0xff;
                    int wheel_revolutions = (fourth_wheel << 24) | (third_wheel << 16) | (second_wheel << 8) | (first_wheel);

                    int high_time = ((int)characteristicData[6]) & 0xff;
                    int low_time = ((int)characteristicData[5]) & 0xff;
                    int time_speed = (high_time << 8) | low_time;
                    // Construct array which holds speed raw data (not converted)
                    int speed_dataset[] = new int[2];
                    speed_dataset[0] = wheel_revolutions;
                    speed_dataset[1] = time_speed;
                    // Convert data in real speed float
                    float time_difference = 0;
                    int revolutions_difference = speed_dataset[0] - last_revolutions;
                    if (speed_dataset[1] < last_speed) {
                        time_difference = (float) speed_dataset[1] + 65536 - last_speed;
                    } else {
                        time_difference = (float) speed_dataset[1] - last_speed;
                    }
                    last_speed = (float) speed_dataset[1];
                    last_revolutions = speed_dataset[0];

                    time_difference = time_difference / 1024;
                    float speed = 0;
                    if (time_difference > 0) {
                        speed = ((revolutions_difference * (float)wheel_type) / time_difference) * (float)3.6;
                    } else {
                        speed = 0;
                    }
                    Log.v(TAG, "Speed: " + speed);

                    if(speed < 100) {                                  // if speed value valid
                        sendMsgFloat(Messages.SPEED_MESSAGE, speed);
                    }

                    if (firstWheelRevolution == 0){
                        firstWheelRevolution = wheel_revolutions;
                    }
                    totalWheelRevolution = wheel_revolutions - firstWheelRevolution;
                    distance = totalWheelRevolution * (float)wheel_circumference;

                    sendMsgFloat(Messages.DISTANCE_MESSAGE, distance);
                }
            }
            if (characteristic.getService().getUuid().toString().equals(HRService)) {
                int heartrate = (int) characteristicData[1];
                sendMsgFloat(Messages.HEARTRATE_MESSAGE, (float)heartrate);

                //double calories = ((0.380 * vo2max)+(0.450 * heartrate)+(0.274 * age)+(0.0468 * weight) - 59.3954) * time / 4.184;
                calories = calories + (((0.380 * vo2max) + (0.450 * heartrate) + (0.274 * age) + (0.0468 * weight) - 59.3954) / 4.184);
                /*
                * Source: Paper - Accurate Caloric Expenditure of Bicyclists using Cellphones
                * Calories =[(0.380 * VO2_max)+(0.450 * BPM)
                * +(0.274 * age)+(0.0468 * weight) - 59.3954] * time / 4.184
                * (for male biker) - in kJ ???
                * with: VO2_max = (D - 504.9) / 44.73
                */
                sendMsgFloat(Messages.CALORIES_MESSAGE, (float)calories);

            }

            if(characteristic.getService().getUuid().toString().equals(ACCService)){
                Log.i(TAG,"GOT ACC");
                int high_x = ((int)characteristicData[0]) & 0xff;
                int low_x = ((int)characteristicData[1]) & 0xff;
                int acc_x = (high_x << 8) | low_x;

                int high_y = ((int)characteristicData[2]) & 0xff;
                int low_y = ((int)characteristicData[3]) & 0xff;
                int acc_y = (high_y << 8) | low_y;

                int high_z = ((int)characteristicData[4]) & 0xff;
                int low_z = ((int)characteristicData[5]) & 0xff;
                int acc_z = (high_z << 8) | low_z;

                int acc_dataset[] = new int[3];
                acc_dataset[0] = acc_x;
                acc_dataset[1] = acc_y;
                acc_dataset[2] = acc_z;
                //sendMsg(Messages.ACC_RAW_MESSAGE, acc_dataset);
            }

            if(characteristic.getService().getUuid().toString().equals(BarometerService)){
                Log.i(TAG,"GOT Pressure");
                short barometer_value = twoBytesToShort(characteristicData[0],characteristicData[1]);

                if(!barometer_calibrated){
                    barometer_offset = barometer_value;
                    barometer_calibrated = true;
                }

                // TODO: Check int to short cast
                barometer_value = (short) (barometer_value - barometer_offset);

                int barometerFinal = (int)barometer_value-(int)barometer_offset;
                sendMsgFloat(Messages.HEIGTH_MESSAGE, (float) barometerFinal);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "OnDescriptionWrite staus: " + new Integer(status).toString());
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "connected to device: " + gatt.getDevice().getName());

                // Add Gatt to list for disconnecting purposes
                mConnectedBtDevicesGattServices.add(gatt);

                if(gatt.getDevice().getAddress().equals(H7)){heartrate_connected = 1;
                Log.i(TAG,"Heart connected");
                };
                if(gatt.getDevice().getAddress().equals(SPD)){speedometer_connected = 1; };
                if(gatt.getDevice().getAddress().equals(CAD)){cadence_connected = 1; };
                if(gatt.getDevice().getAddress().equals(BAROMETER)){barometer_connected = 1; };
                if(gatt.getDevice().getAddress().equals(SPD_ACC_WAKE)){
                    spdAccWake_connected = 1;
                    Log.i(TAG,"ACC connected");
                }

                Log.i(TAG, "nrOfremainingDevices: " + nrOfremainingDevices);
                gatt.discoverServices();
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Since it is not connected any more remove it out of the list
                mConnectedBtDevicesGattServices.remove(gatt);


                mConnectedBtDevices.remove(gatt.getDevice().getAddress());
                gatt.close();
                Log.i(TAG, "disconnected from " + gatt.getDevice().getAddress());
                if(gatt.getDevice().getAddress().equals(H7)){ heartrate_connected = 0;};
                if(gatt.getDevice().getAddress().equals(BAROMETER)){
                    barometer_connected = 0;
                    barometer_calibrated = false;};
                if(gatt.getDevice().getAddress().equals(SPD)){speedometer_connected = 0; };
                if(gatt.getDevice().getAddress().equals(CAD)){cadence_connected = 0; };
                if(gatt.getDevice().getAddress().equals(SPD_ACC_WAKE)){spdAccWake_connected = 0; };
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            List<BluetoothGattService> services = gatt.getServices();
            Log.i(TAG, gatt.getDevice().getName() + " discovered " + services.size() + " services:");
            for (BluetoothGattService service : services) {
                Log.i(TAG, service.getUuid().toString());
                if (!(service.getUuid().toString().equals(HRService) || service.getUuid().toString().equals(SPDCADService) || service.getUuid().toString().equals(ACCService) || service.getUuid().toString().equals(WakeupService) ||service.getUuid().toString().equals(BarometerService))) {
                    continue;
                }

                //H7=0000180d-0000-1000-8000-00805f9b34fb
                Log.i(TAG, service.toString());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {

                    if(characteristic.getUuid().toString().equals(WakeupCharacteristicUUID)){
                        wakeupCharacteristic = characteristic;
                        wakeupGATT = gatt;
                        Log.i(TAG,"Found wakeup-char");
                    }

                    final int charaProp = characteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                            //find descriptor UUID that matches Client Characteristic Configuration (0x2902)
                            // and then call setValue on that descriptor

                            Log.i(TAG, "enabling notification: " + descriptor.getUuid().toString());
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                }
            }
            connectionInProgress = false;
            connectToAvailableDevices();
        }
    };

    public void startScan() {
        mTick = 0;
        Log.i(TAG, "start scanning");
        mBtAdapter.startLeScan(leScanCallback);
    }

    public void stopScan() {
        Log.i(TAG, "stop scanning");
        mBtAdapter.stopLeScan(leScanCallback);
        nrOfremainingDevices = mBtDevicesReadyToConnect.size();
        connectToAvailableDevices();
    }

    public void closeConnections() {
        for (BluetoothGatt gatt : mConnectedBtDevicesGattServices) {
            gatt.disconnect();
            Log.i(TAG,"Disconnect from: "+gatt.getDevice().getAddress().toString());
        }
    }

    public void onCreate() {
        context = getBaseContext();
        // not yet used.
        SensorManager sensorManager =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        mBtAdapter = btManager.getAdapter();
        if (mBtAdapter != null && !mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //TODO
            //startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // T: Insert device UUID's to connect to.
        mConnectableBtDevices.add(H7);
        mConnectableBtDevices.add(CAD);
        mConnectableBtDevices.add(SPD);
        mConnectableBtDevices.add(BAROMETER);
        mConnectableBtDevices.add(SPD_ACC_WAKE);
        startScan();
        Log.i(TAG, "initialized");

        //debug, to be removed
        mHandler.removeCallbacks(mTickRunnable);
        mHandler.post(mTickRunnable);
    }

    // generates ticks for stopping the scan
    private long mTick = 0;
    private final Handler mHandler = new Handler();
    private final Runnable mTickRunnable = new Runnable() {

        public void run() {
            if(mTick<10) {
                mTick++;
                Log.i(TAG, "scanning since " + Long.valueOf(mTick) + "s");
                if(mTick==10)
                {
                    stopScan();
                    Log.i(TAG, "scanning stopped");
                }
            }
            sendSensorStatus();
            mHandler.postDelayed(mTickRunnable, 1000);
        }
    };


    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        closeConnections();
        super.onDestroy();
    }
}
