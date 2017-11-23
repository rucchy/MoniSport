package com.example.chema.monisport;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import com.example.chema.monisport.SensorTag.BarometerCalibrationCoefficients;
import com.example.chema.monisport.SensorTag.GattInfo;
import com.example.chema.monisport.SensorTag.MagnetometerCalibrationCoefficients;
import com.example.chema.monisport.SensorTag.SensorTag;
import com.example.chema.monisport.SensorTag.SensorTagGatt;
import com.example.chema.monisport.utils.Point3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import zephyr.android.BioHarnessBT.BTClient;


/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class ConexionBluetooth extends AppCompatActivity {

    /**
     * Tag for Log
     */
    private static final String TAG = "ActividadBluetooth";

    /**
     * Return Intent extra
     */
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    /**
     * Member fields
     */
    private BluetoothAdapter mBtAdapter;

    /**
     * Newly discovered devices
     */
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    private TextView texto_info;
    private ProgressDialog dialog;
    private HashMap conectados;
    private ZephyrListener zl = null;
    private BTClient btClient = null;
    private BluetoothService mBluetoothLeService = null;
    private static BluetoothManager mBluetoothManager;
    private BluetoothGatt mBtGatt = null;
    private boolean mIsSensorTag2;
    protected static ConexionBluetooth mThis = null;
    private String mFwRev;
    private List<SensorTag> mEnabledSensors = new ArrayList<SensorTag>();
    private boolean mMagCalibrateRequest = true;
    private boolean mHeightCalibrateRequest = true;
    private static final int GATT_TIMEOUT = 1000; // milliseconds


    private BluetoothGattService mOadService = null;
    private BluetoothGattService mConnControlService = null;
    private static List<BluetoothGattService> mServiceList = null;
    private boolean mServicesRdy = false;
    private Grabacion g = new Grabacion();
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mThis=this;
        mHandler=g.getHandler();
        mFwRev = new String("1.5"); // Assuming all SensorTags are up to date until actual FW revision is read

        conectados = new HashMap<String, BluetoothService>();
        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.launcher_monisport);
        setContentView(R.layout.bluetooth);

        final Bundle bundle_datos_deportista=this.getIntent().getExtras();

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);
        texto_info = (TextView) findViewById(R.id.texto_info);
        dialog = new ProgressDialog(ConexionBluetooth.this);
        dialog.setMessage(getResources().getString(R.string.scanning));
        dialog.setCancelable(false);
        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.buttonSearch);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                }
                doDiscovery();
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        ArrayAdapter<String> pairedDevicesArrayAdapter =
                new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.listViewPaired);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.listViewDetected);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothService.ACTION_GATT_CONNECTED);
        filter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);

        // Initializes a Bluetooth adapter. For API level 18 and above, get a
        // reference to BluetoothAdapter through BluetoothManager.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBluetoothManager.getAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }

        startBluetoothLeService();
        // Create GATT object
        //mServiceList = new ArrayList<BluetoothGattService>();

        Button btn = (Button) findViewById(R.id.button_ir_camara);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent (v.getContext(), Grabacion.class);
                intent.putExtras(bundle_datos_deportista);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }


        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
        if (mBluetoothLeService != null) {
            unbindService(mServiceConnection);
            mBluetoothLeService.close();
            mBluetoothLeService = null;
        }
        if(btClient!=null && btClient.IsConnected()){
            btClient.Close();
        }

    }
    @Override
    protected void onResume() {
        super.onResume();

        if (mBluetoothLeService != null) {
            mBluetoothLeService.close();
        }
        if(btClient!=null && btClient.IsConnected()){
            btClient.Close();
        }

    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        //Log.d(TAG, "doDiscovery()");

        mNewDevicesArrayAdapter.clear();
        // Indicate scanning in the title
        setSupportProgressBarIndeterminateVisibility(true);
        texto_info.setText(R.string.scanning);


        dialog.show();

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            connectDevice(address);
            /*// Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();*/
        }
    };

    /**
     * Establish connection with other device
     *
     * @param data String with MAC.
     */
    private void connectDevice(String data) {
        // Get the device MAC address
        String address = data;
        // Get the BluetoothDevice object
        BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        // Attempt to connect to the device
        // Start the thread to connect with the given device

        if (device.getName().startsWith("BH")) {
            btClient = new BTClient(mBtAdapter, address);
            zl = new ZephyrListener(mHandler, mHandler);
            btClient.addConnectedEventListener(zl);

            if(btClient.IsConnected()) {
                btClient.start();
                Toast.makeText(getApplication(), "Conectado con dispositivo Zephyr",
                        Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(getApplication(), "Fallo en la conexión con el dispositivo Zephyr",
                        Toast.LENGTH_LONG).show();
            }
        }else if (device.getName().startsWith("SensorTag")) {
            mIsSensorTag2 = device.getName().startsWith("SensorTag2");

            mBluetoothLeService.setHandler(mHandler);
            mBluetoothLeService.initialize();
           
            boolean ok = mBluetoothLeService.connect(address);
            Toast.makeText(getApplication(), "Conectando con dispositivo",
                    Toast.LENGTH_LONG).show();
            // Initialize sensor list
            updateSensorList();
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                texto_info.setText(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);

                }
                dialog.hide();
            }
            else if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                // GATT connect
                int status = intent.getIntExtra(BluetoothService.EXTRA_STATUS,
                        BluetoothGatt.GATT_FAILURE);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mBtGatt = BluetoothService.getBtGatt();
                    mBtGatt.discoverServices();
                } else
                    Toast.makeText(getApplication(), "Connect failed. Status: " + status,
                            Toast.LENGTH_LONG).show();
            }
        }
    };




    // Code to manage Service life cycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
                return;
            }
            final int n = mBluetoothLeService.numConnectedDevices();
            if (n > 0) {
                runOnUiThread(new Runnable() {
                    public void run() {

                    }
                });
            } else {
                //startScan();
                // Log.i(TAG, "BluetoothLeService connected");
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            // Log.i(TAG, "BluetoothLeService disconnected");
        }
    };
    private void startBluetoothLeService() {
        boolean f;
        mBluetoothLeService = new BluetoothService();

        Intent bindIntent = new Intent(this, com.example.chema.monisport.BluetoothService.class);
        startService(bindIntent);
        f = bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        if (!f) {
            finish();
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int status = intent.getIntExtra(BluetoothService.EXTRA_STATUS,
                    BluetoothGatt.GATT_SUCCESS);

            if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Toast.makeText(getApplication(), "Descubrimiento de servicio completado",
                            Toast.LENGTH_LONG).show();
                    //displayServices();
                    //checkOad();
                    enableDataCollection(true);
                    //getFirmwareRevison();
                } else {
                    Toast.makeText(getApplication(), "Fallo en el descubrimiento de servicio",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (BluetoothService.ACTION_DATA_NOTIFY.equals(action)) {
                // Notification
                byte[] value = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothService.EXTRA_UUID);
                onCharacteristicChanged(uuidStr, value);
            } else if (BluetoothService.ACTION_DATA_WRITE.equals(action)) {
                // Data written
                String uuidStr = intent.getStringExtra(BluetoothService.EXTRA_UUID);
                onCharacteristicWrite(uuidStr, status);
            } else if (BluetoothService.ACTION_DATA_READ.equals(action)) {
                // Data read
                String uuidStr = intent.getStringExtra(BluetoothService.EXTRA_UUID);
                byte[] value = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                onCharacteristicsRead(uuidStr, value, status);
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Toast.makeText(getApplication(), "Descubrimiento de servicio completado"+ status,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    private void enableSensors(boolean f) {
        final boolean enable = f;

        for (SensorTag sensor : mEnabledSensors) {
            UUID servUuid = sensor.getService();
            UUID confUuid = sensor.getConfig();

            // Skip keys
            if (confUuid == null)
                break;
            if (!mIsSensorTag2) {
                // Barometer calibration
                if (confUuid.equals(SensorTagGatt.UUID_BAR_CONF) && enable) {
                    calibrateBarometer();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            BluetoothGattService serv = mBtGatt.getService(servUuid);
            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(confUuid);
                byte value = enable ? sensor.getEnableSensorCode()
                        : SensorTag.DISABLE_SENSOR_CODE;

                if (mBluetoothLeService.writeCharacteristic(charac, value)) {
                    mBluetoothLeService.waitIdle(500);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplication(), "Sensor config failed: " + serv.getUuid().toString(),
                            Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

    private void enableNotifications(boolean f) {
        final boolean enable = f;

        for (SensorTag sensor : mEnabledSensors) {
            UUID servUuid = sensor.getService();
            UUID dataUuid = sensor.getData();
            BluetoothGattService serv = mBtGatt.getService(servUuid);
            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);

                if (mBluetoothLeService.setCharacteristicNotification(charac, enable)) {
                    mBluetoothLeService.waitIdle(500);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplication(), "Sensor notification failed: " + serv.getUuid().toString(),
                            Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }
    public boolean isSensorTag2() {
        return mIsSensorTag2;
    }
    public String firmwareRevision() {
        return mFwRev;
    }
    public static ConexionBluetooth getInstance() {
        return (ConexionBluetooth) mThis;
    }

    /*
	 * Calibrating the barometer includes
	 *
	 * 1. Write calibration code to configuration characteristic. 2. Read
	 * calibration values from sensor, either with notifications or a normal read.
	 * 3. Use calibration values in formulas when interpreting sensor values.
	 */
    private void calibrateBarometer() {
        if (mIsSensorTag2)
            return;

        UUID servUuid = SensorTag.BAROMETER.getService();
        UUID configUuid = SensorTag.BAROMETER.getConfig();
        BluetoothGattService serv = mBtGatt.getService(servUuid);
        BluetoothGattCharacteristic config = serv.getCharacteristic(configUuid);

        // Write the calibration code to the configuration registers
        mBluetoothLeService.writeCharacteristic(config, SensorTag.CALIBRATE_SENSOR_CODE);
        mBluetoothLeService.waitIdle(500);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BluetoothGattCharacteristic calibrationCharacteristic = serv
                .getCharacteristic(SensorTagGatt.UUID_BAR_CALI);
        mBluetoothLeService.readCharacteristic(calibrationCharacteristic);
        mBluetoothLeService.waitIdle(500);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void getFirmwareRevison() {
        UUID servUuid = SensorTagGatt.UUID_DEVINFO_SERV;
        UUID charUuid = SensorTagGatt.UUID_DEVINFO_FWREV;
        BluetoothGattService serv = mBtGatt.getService(servUuid);
        BluetoothGattCharacteristic charFwrev = serv.getCharacteristic(charUuid);

        // Write the calibration code to the configuration registers
        mBluetoothLeService.readCharacteristic(charFwrev);
        mBluetoothLeService.waitIdle(500);

    }
    private void onCharacteristicWrite(String uuidStr, int status) {
        // Log.d(TAG, "onCharacteristicWrite: " + uuidStr);
    }

    private void onCharacteristicChanged(String uuidStr, byte[] value) {

            if (mMagCalibrateRequest) {
                if (uuidStr.equals(SensorTagGatt.UUID_MAG_DATA.toString())) {
                    Point3D v = SensorTag.MAGNETOMETER.convert(value);

                    MagnetometerCalibrationCoefficients.INSTANCE.val = v;
                    mMagCalibrateRequest = false;
                    Toast.makeText(getApplication(), "Magnetómetro calibrado", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            if (mHeightCalibrateRequest) {
                if (uuidStr.equals(SensorTagGatt.UUID_BAR_DATA.toString())) {
                    Point3D v = SensorTag.BAROMETER.convert(value);

                    BarometerCalibrationCoefficients.INSTANCE.heightCalibration = v.x;
                    mHeightCalibrateRequest = false;
                    Toast.makeText(getApplication(), "Medición de la altura calibrada",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }


    private void onCharacteristicsRead(String uuidStr, byte[] value, int status) {
        // Log.i(TAG, "onCharacteristicsRead: " + uuidStr);

        if (uuidStr.equals(SensorTagGatt.UUID_DEVINFO_FWREV.toString())) {
            mFwRev = new String(value, 0, 3);
            Toast.makeText(getApplication(), "Firmware revision: " + mFwRev,Toast.LENGTH_LONG).show();
        }

        if (mIsSensorTag2)
            return;

        if (uuidStr.equals(SensorTagGatt.UUID_BAR_CALI.toString())) {
            // Sanity check
            if (value.length != 16)
                return;

            // Barometer calibration values are read.
            List<Integer> cal = new ArrayList<Integer>();
            for (int offset = 0; offset < 8; offset += 2) {
                Integer lowerByte = (int) value[offset] & 0xFF;
                Integer upperByte = (int) value[offset + 1] & 0xFF;
                cal.add((upperByte << 8) + lowerByte);
            }

            for (int offset = 8; offset < 16; offset += 2) {
                Integer lowerByte = (int) value[offset] & 0xFF;
                Integer upperByte = (int) value[offset + 1];
                cal.add((upperByte << 8) + lowerByte);
            }

            BarometerCalibrationCoefficients.INSTANCE.barometerCalibrationCoefficients = cal;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        fi.addAction(BluetoothService.ACTION_DATA_NOTIFY);
        fi.addAction(BluetoothService.ACTION_DATA_WRITE);
        fi.addAction(BluetoothService.ACTION_DATA_READ);
        return fi;
    }
    boolean isEnabledByPrefs(final SensorTag sensor) {
        String preferenceKeyString = "pref_"
                + sensor.name().toLowerCase(Locale.ENGLISH) + "_on";

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        Boolean defaultValue = true;
        return prefs.getBoolean(preferenceKeyString, defaultValue);
    }
    //
    // Application implementation
    //
    private void updateSensorList() {
        mEnabledSensors.clear();

        for (int i = 0; i < SensorTag.SENSOR_TAG_LIST.length; i++) {
            SensorTag sensor = SensorTag.SENSOR_TAG_LIST[i];
            if (isEnabledByPrefs(sensor)) {
                mEnabledSensors.add(sensor);
            }
        }
    }
    private void checkOad() {
        // Check if OAD is supported (needs OAD and Connection Control service)
        mOadService = null;
        mConnControlService = null;

        for (int i = 0; i < mServiceList.size()
                && (mOadService == null || mConnControlService == null); i++) {
            BluetoothGattService srv = mServiceList.get(i);
            if (srv.getUuid().equals(GattInfo.OAD_SERVICE_UUID)) {
                mOadService = srv;
            }
            if (srv.getUuid().equals(GattInfo.CC_SERVICE_UUID)) {
                mConnControlService = srv;
            }
        }
    }
    private void displayServices() {
        mServicesRdy = true;

        try {
            mServiceList = mBluetoothLeService.getSupportedGattServices();
        } catch (Exception e) {
            e.printStackTrace();
            mServicesRdy = false;
        }

        // Characteristics descriptor readout done
        if (!mServicesRdy) {
            Toast.makeText(getApplication(), "Failed to read services",
                    Toast.LENGTH_LONG).show();
        }
    }
    private void enableDataCollection(boolean enable) {
        enableSensors(enable);
        enableNotifications(enable);
    }
}