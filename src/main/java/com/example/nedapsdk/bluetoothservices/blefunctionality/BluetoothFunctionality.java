package com.example.nedapsdk.bluetoothservices.blefunctionality;

import static com.example.nedapsdk.bluetoothservices.Constants.MACE_ID;
import static com.example.nedapsdk.bluetoothservices.Constants.MACE_RX;
import static com.example.nedapsdk.bluetoothservices.Constants.MACE_TX;
import static com.example.nedapsdk.bluetoothservices.Constants.NOTIFICATION_DESCRIPTOR_UUID;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.Handler;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.example.nedapsdk.bluetoothservices.Constants;
import com.example.nedapsdk.bluetoothservices.OnConnectEventListener;
import com.example.nedapsdk.bluetoothservices.OnCustomAuthenticator;
import com.example.nedapsdk.bluetoothservices.OnCustomEventListener;
import com.example.nedapsdk.bluetoothservices.OnCustomLogsListener;
import com.example.nedapsdk.bluetoothservices.encryption.Aes128;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class BluetoothFunctionality
{
    boolean isAuthenticReader =false;
    private BluetoothLeScanner bluetoothLeScanner;
    public static Context context;
    private Handler handler = new Handler();
    public static BluetoothAdapter bluetoothAdapter;
    private static final String TAG = "ReaderAuthenticator";
    public static BluetoothFunctionality INSTANCE;
    private static String UIDA = "";
    private static String MKEY = "";
    private static  double userDistance  =0;
    private Aes128 aes128;
    private static final byte AUTH_RESPONSE = 0x42;
    public static final byte AUTH_FINISH = 0x43;
    public static final byte AUTH_CHALLENGE = 0x41;
    protected byte[] diversified_key;
    protected byte[] rndA;
    private static final String TAGG = "BLEReaderAuthenticator";

    public static final UUID MACE_SERVICE = UUID.fromString("87b1de8d-e7cb-4ea8-a8e4-290209522c83");

    private OnConnectEventListener onCustomConnectionListener;
    private BluetoothGatt bluetoothGatt;
    private boolean isConnected = false;
    private OnCustomLogsListener onCustomLogsListener;
    private ArrayList<BluetoothDevice> bondedDevices = new ArrayList<>();
    private ArrayList<Double> devicesDistance = new ArrayList<>();
    private OnCustomEventListener mCustomEventListener;
   private OnCustomAuthenticator onCustomAuthenticator;
    private ScanCallback leScanCallback = new ScanCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            double rssi = result.getRssi();
            double measuredPower = 3.0;
            double n=4.0;
            if (result.getDevice().getName() != null && !result.getDevice().getName().isEmpty()) {
                for (BluetoothDevice bluetoothDevice : bondedDevices) {
                    if (bluetoothDevice.getAddress().equals(result.getDevice().getAddress())) {
                        return;
                    }
                }
                double measuredPowerValue = (measuredPower -rssi)/(10 * n);
               double calculatedDistance = Math.pow(10.0 , measuredPowerValue);


                System.out.println("--------------------------------------------");
                System.out.println("Distances are..."+calculatedDistance);
                System.out.println("Measured PowerValue is..."+measuredPowerValue);
                System.out.println("RSSI are..."+rssi);

                System.out.println("Names are..."+result.getDevice().getName());
                System.out.println("--------------------------------------------");
                if(calculatedDistance<userDistance)
                {
//                    if(result.getDevice().getName().equals("NMS08"))
//                    {
//                    double finalRssi = result.getRssi();
//                    String connectedRssi =  Double.toString(finalRssi);
                    onCustomLogsListener.onLogsEvent("Rssi: "+Double.toString(result.getRssi())+"-------"+"Distance: "+Double.toString(calculatedDistance));
                    System.out.println("Logs Distances are..."+calculatedDistance);
                    System.out.println("Logs Rssi are..."+result.getRssi());
                    bondedDevices.add(result.getDevice());
                        devicesDistance.add(calculatedDistance);
                        mCustomEventListener.onEvent(result.getDevice().getName() + "\n" + result.getDevice().getAddress());

//                    }

                }

            }
        }
    };

//    private ScanCallback leScanAndConnectCallback = new ScanCallback() {
//        @SuppressLint("MissingPermission")
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            int rssi = result.getRssi();
//            int measuredPower = 3;
//            int n=4;
//            if (result.getDevice().getName() != null && !result.getDevice().getName().isEmpty()) {
//                for (BluetoothDevice bluetoothDevice : bondedDevices) {
//                    if (bluetoothDevice.getAddress().equals(result.getDevice().getAddress())) {
//                        return;
//                    }
//                }
//
//                double distance = Math.pow(10 , ((measuredPower -rssi)/(10 * n)));
//                System.out.println("--------------------------------------------");
//                System.out.println("Distances are..."+distance);
//                System.out.println("RSSI are..."+rssi);
//                System.out.println("Names are..."+result.getDevice().getName());
//                System.out.println("--------------------------------------------");
//                if(distance<userDistance)
//                {
//                    if(result.getDevice().getName().equals("NMS08"))
//                    {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                            aa
////                            connectAndAuthenticate(result.getDevice().getAddress(),UIDA,MKEY);
//                        }
////                        bondedDevices.add(result.getDevice());
//                        mCustomEventListener.onEvent(result.getDevice().getName() + "\n" + result.getDevice().getAddress());
//
//                    }
//
//                }
//
//            }
//        }
//    };
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String deviceAddress = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    isConnected = true;
                    bluetoothGatt = gatt;
                    showMessage("Connection to bluetooth device " + deviceAddress + " successful!");
                    onCustomConnectionListener.onConnectedEvent();
                    bluetoothGatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isConnected = false;
                    showMessage("Disconnected from bluetooth device " + deviceAddress);
                    onCustomConnectionListener.onDisconnectedEvent();
                    gatt.close();
                }
            } else {
                isConnected = false;
                showMessage("Error " + status + " encountered for " + deviceAddress);
                onCustomConnectionListener.onDisconnectedEvent();
                gatt.close();
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            final BluetoothGattService service = gatt.getService(MACE_SERVICE);
            if(service == null){
                onCustomLogsListener.onLogsEvent("Service not found");
                Log.i("Check:","Service not found");
                return;
            }
            onCustomLogsListener.onLogsEvent("Service found");
            Log.i("Check:","Service found");
            final BluetoothGattCharacteristic IDChar = service.getCharacteristic(MACE_ID);
            final BluetoothGattCharacteristic RXChar = service.getCharacteristic(MACE_RX);
            final BluetoothGattCharacteristic TXChar = service.getCharacteristic(MACE_TX);
            if (IDChar != null && RXChar != null && TXChar != null) {
                onCustomLogsListener.onLogsEvent("All characteristics found.");
                Log.i("Check:","All characteristics found.");
                registerForMessagesFromReader(gatt);
            } else{
                onCustomLogsListener.onLogsEvent("All characteristics not found.");
                Log.i("Check:","All characteristics not found.");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, descriptor.getValue())) {
                    gatt.setCharacteristicNotification(descriptor.getCharacteristic(), true);
                    sendIdentification(gatt);
                }

                if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, descriptor.getValue())) {
                    onCustomLogsListener.onLogsEvent("Unregister from notifications succeeded");
                }

            } else {
              onCustomLogsListener.onLogsEvent("Descriptor Write failed for " + descriptor.getCharacteristic().getUuid());
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                byte[] bytes = characteristic.getValue();
                String str = new String(bytes, StandardCharsets.UTF_8);
                if (characteristic.getUuid().equals(MACE_RX)) {
                    onCustomLogsListener.onLogsEvent("READ(RX): " + str);
                    onCustomLogsListener.onLogsEvent("READ(RX) Length: " + str.length());
                }

                if (!bluetoothGatt.setCharacteristicNotification(characteristic, true)) {
                    showMessage("Failed to subscribe to sensor data!");
                }

            } else if (status == BluetoothGatt.GATT_READ_NOT_PERMITTED) {
                showMessage("Read not permitted!");
            } else {
                showMessage("Error " + status + " encountered in reading");
            }
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] bytes = characteristic.getValue();
            String str = byteArrayToHexString(bytes);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (MACE_ID.equals(characteristic.getUuid())) {
                    onCustomLogsListener.onLogsEvent("Identification successfully written.");
                    onCustomLogsListener.onLogsEvent("WRITE(ID): " + str);
                    onCustomLogsListener.onLogsEvent("WRITE(ID) Length: " + str.length());
                }else if (MACE_TX.equals(characteristic.getUuid())) {
                    onCustomLogsListener.onLogsEvent("Challenge response successfully written.");
                    onCustomLogsListener.onLogsEvent("WRITE(TX): " + str);
                    onCustomLogsListener.onLogsEvent("WRITE(TX) Length: " + str.length());
                } else {
                    onCustomLogsListener.onLogsEvent("Something written, but we are in unexpected state");
                }

            } else {
                onCustomLogsListener.onLogsEvent("Write to Write Characteristic failed");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] bytes = characteristic.getValue();
            String str = byteArrayToHexString(bytes);

            if (characteristic.getUuid().equals(MACE_RX)) {
                switch (bytes[0]) {
                    case Constants.AUTH_CHALLENGE:
                        writeChallengeResponse(gatt, bytes);
                        break;
                    case Constants.AUTH_FINISH:
                         isAuthenticReader = isAuthenticReader(bytes);
                        //---------------------check---------------------------
                        onCustomAuthenticator.isAuthenticcatorReader(isAuthenticReader);
                        //---------------------check---------------------------
                        break;
                    default:
                        break;
                }
            }
        }
    };
    public static void showMessage(String message){
//        int duration = Toast.LENGTH_SHORT;
//        Toast.makeText(context, message, duration).show();
    }
    private void registerForMessagesFromReader(final BluetoothGatt gatt) {

        final BluetoothGattService service = gatt.getService(MACE_SERVICE);
        final BluetoothGattCharacteristic RXChar = service.getCharacteristic(MACE_RX);
        final BluetoothGattDescriptor descriptor = RXChar.getDescriptor(NOTIFICATION_DESCRIPTOR_UUID);

        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            final boolean descriptorWritten = writeDescriptor(gatt, descriptor);

            if (descriptorWritten) {
                onCustomLogsListener.onLogsEvent("Descriptor Write success for RX Char");
            } else {
                onCustomLogsListener.onLogsEvent("Descriptor Write failed for RX Char");
            }

        } else {
            onCustomLogsListener.onLogsEvent("Descriptor not found for RX Char");
        }

    }

    public void setDiversifiedKey(String key) {
        if (key != null && !key.isEmpty()) {
            diversified_key = hexStringToByteArray(key);
        }
    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for(int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }
    public static String byteArrayToHexString(byte[] buf) {
        if (buf == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim().replace(" ", "");
    }
//-----------------authenticator--------------------------------
@SuppressLint("MissingPermission")
private boolean writeDescriptor(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor) {
    final BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
    final int originalWriteType = characteristic.getWriteType();
    try {
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        return gatt.writeDescriptor(descriptor);
    } finally {
        characteristic.setWriteType(originalWriteType);
    }
}

    public boolean isAuthenticReader(final byte[] finishMessage) {
        Log.d(TAGG, "Finish message:" + byteArrayToHexString(finishMessage));
        if (finishMessage[0] == AUTH_FINISH && diversified_key != null && rndA != null) {
            byte[] finishWithoutToken = Arrays.copyOfRange(finishMessage, 1, finishMessage.length);
            return checkReader(finishWithoutToken);
        }

        return false;
    }
    private void sendIdentification(final BluetoothGatt gatt) {
        setDiversifiedKey(aes128.keyDiversification(MKEY,UIDA));

        byte[] valueToWrite = hexStringToByteArray(UIDA);
        if (valueToWrite != null) {
            onCustomLogsListener.onLogsEvent("Writing identifier message: " + UIDA);
            final BluetoothGattService service = gatt.getService(MACE_SERVICE);
            final BluetoothGattCharacteristic IDChar = service.getCharacteristic(MACE_ID);
            IDChar.setValue(valueToWrite);
            @SuppressLint("MissingPermission") final boolean success = gatt.writeCharacteristic(IDChar);
            if (!success) {
                onCustomLogsListener.onLogsEvent("Failed to write Identification");
            }
        } else {
            onCustomLogsListener.onLogsEvent("No identifier to write");
        }
    }

    public byte[] generateResponseToChallenge(final byte[] challengeMessage) {
        byte[] responseMessage = null;
        Log.d(TAGG, "ChallengeMessage: " + byteArrayToHexString(challengeMessage));

        if (challengeMessage[0] == AUTH_CHALLENGE && diversified_key != null) {
            final byte[] arr = Arrays.copyOfRange(challengeMessage, 1, challengeMessage.length);
            responseMessage = prepend(createResponse(arr), AUTH_RESPONSE);
            Log.d(TAGG, "Response message:" + byteArrayToHexString(responseMessage));
        }
        return responseMessage;
    }

    //-----------------authenticator--------------------------------
    private void writeChallengeResponse(final BluetoothGatt gatt, byte[] challenge) {
        byte[] responseToChallenge = generateResponseToChallenge(challenge);
        if (responseToChallenge != null) {
            onCustomLogsListener.onLogsEvent("Sending a challenge response. " + Thread.currentThread());
            final BluetoothGattService service = gatt.getService(MACE_SERVICE);
            final BluetoothGattCharacteristic characteristic = service.getCharacteristic(MACE_TX);
            characteristic.setValue(responseToChallenge);

            @SuppressLint("MissingPermission") final boolean success = gatt.writeCharacteristic(characteristic);

            if (success) {
                onCustomLogsListener.onLogsEvent("Successfully wrote the response to the challenge");

            } else {
                onCustomLogsListener.onLogsEvent("Failed to successfully write the response to the challenge");
            }
        } else {
            onCustomLogsListener.onLogsEvent("Failed to create challenge response. Disconnecting");

        }
    }

    public boolean checkReader(byte[] challenge) {
        try {
            byte[] decryptedMessage = aes128.decrypt(diversified_key, challenge);
            Log.i(TAG, "Decrypted message: " + byteArrayToHexString(decryptedMessage));
            byte[] randomRotaded = new byte[8];
            System.arraycopy(decryptedMessage, 0, randomRotaded, 0, decryptedMessage.length / 2);

            return Arrays.equals(rotateLeft(rndA), randomRotaded);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public byte[] createResponse(byte[] arr) {
        byte[] responseMessage = null;
        rndA = new byte[8];
        new Random().nextBytes(rndA);
        rndA = hexStringToByteArray("A0A1A2A3A4A5A6A7");
        Log.i(TAG, "Randomly generated: " + byteArrayToHexString(rndA));
        byte[] rndB2 = rotateLeft(arr);
        arr = concat(rndA, rndB2);
        Log.i(TAG, "RNDA + RNDB2: " + byteArrayToHexString(arr));

        try {
            byte[] encryptedData = aes128.encrypt(diversified_key, arr);
            Log.i(TAG, "Encrypted message:" + byteArrayToHexString(encryptedData));
            responseMessage = encryptedData;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseMessage;
    }

    public static byte[] rotateLeft(byte[] arr) {
        if (arr == null || arr.length < 2) {
            return arr;
        }
        byte[] head = Arrays.copyOfRange(arr, 0, 1);
        byte[] tail = Arrays.copyOfRange(arr, 1, arr.length);
        return concat(tail, head);
    }
    public static byte[] concat(byte[] array1, byte[] array2) {
        byte[] concatenated = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, concatenated, 0, array1.length);
        System.arraycopy(array2, 0, concatenated, array1.length, array2.length);
        return concatenated;
    }
    public static byte[] prepend(byte[] a, byte el) {
        byte[] c = new byte[a.length + 1];
        c[0] = el;
        System.arraycopy(a, 0, c, 1, a.length);
        return c;
    }


    public static BluetoothFunctionality getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new BluetoothFunctionality();
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothFunctionality.context = context;
        }
        return INSTANCE;
    }

    public void setCustomEventListener(OnCustomEventListener eventListener) {
        mCustomEventListener = eventListener;
    }
    public void setCustomConnectionListener(OnConnectEventListener eventConnectListener) {
        onCustomConnectionListener = eventConnectListener;
    }
    public void setCustomLogsListener(OnCustomLogsListener eventLogsListener) {
        onCustomLogsListener = eventLogsListener;
    }
    public void setCustomAuthenticator(OnCustomAuthenticator onCustomAuthenticatorListener) {
        onCustomAuthenticator = onCustomAuthenticatorListener;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean checkPermissions() {
        // Android M Permission check 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED ||
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED ||
                    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startScaning(double distance) {

        userDistance =distance;
        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if (!checkPermissions()) {
            showMessage("Enable all permissions first!");
            return;
        }
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showMessage("Device doesn't support BLE!");
            return;
        }
        if (bluetoothLeScanner == null) {
            showMessage("Enable Bluetooth first!");
            return;
        }
        if (!bondedDevices.isEmpty()) {
            bondedDevices.clear();
        }
        // Stops scanning after a pre-defined scan period.
//        handler.postDelayed(() -> {
//            if (bondedDevices.isEmpty()) {
//                showMessage("No Devices Available!");
//                System.out.println("No Devices Available!");
//            }
//            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                System.out.println("No Devices Available!");
//                showMessage("Permission issue");
//                return;
//            }
//            stopScanning();
////            bluetoothLeScanner.stopScan(leScanCallback);
//        }, Constants.SCAN_PERIOD);

        bluetoothLeScanner.startScan(leScanCallback);
    }
    @SuppressLint("MissingPermission")
    public void stopScanning()
    {
        bluetoothLeScanner.stopScan(leScanCallback);
    }
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void scanConnectAuthenticate(double distance,String uida,String masterKey) {
        System.out.println("scanConnectAuthenticate Run");
        UIDA =uida;
        MKEY =masterKey;
        userDistance=distance;

        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if (!checkPermissions()) {
            showMessage("Enable all permissions first!");
            return;
        }
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showMessage("Device doesn't support BLE!");
            return;
        }
        if (bluetoothLeScanner == null) {
            showMessage("Enable Bluetooth first!");
            return;
        }
        if (!bondedDevices.isEmpty()) {
            bondedDevices.clear();
        }
        // Stops scanning after a pre-defined scan period.
        handler.postDelayed(() -> {

            stopScanning();
            if(!bondedDevices.isEmpty())
            {
                closestDevice();
            }
//            bluetoothLeScanner.stopScan(leScanAndConnectCallback);
        }, Constants.SCAN_PERIOD);

        bluetoothLeScanner.startScan(leScanCallback);
    }
    @SuppressLint("MissingPermission")
    private void closestDevice()
    {
        System.out.println("Closestt Run");
        double minDistance = devicesDistance.get(0);
        int deviceIndex =0;
        for(int i=0;i<devicesDistance.size();i++)
        {
            if(devicesDistance.get(i) < minDistance)
            {
                minDistance = devicesDistance.get(i);
                deviceIndex =i;
            }
        }
        System.out.println("Closestt final Run"+bondedDevices.get(deviceIndex).getName());
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(bondedDevices.get(deviceIndex).getAddress());
        bluetoothDevice.connectGatt(context,false,gattCallback);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    public void connectAndAuthenticate(String macAddress,String uida,String mKey){
        UIDA =uida;
        MKEY =mKey;
         isAuthenticReader =false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!checkPermissions()){
            showMessage("Enable all permissions first!");
            return;
        }
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showMessage("Device doesn't support BLE!");
            return;
        }
        if(bluetoothAdapter == null){
            showMessage("Enable Bluetooth first!");
            return;
        }
        if(macAddress.equals("")){
            showMessage("Go to settings to add a device");
            return;
        }

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);

        if(bluetoothDevice == null){
            showMessage("Device not found!");
            return;
        }
        Disconnect();
        bluetoothGatt = bluetoothDevice.connectGatt(context,false,gattCallback);
    }

    @SuppressLint("MissingPermission")
    public void Disconnect () {
        if ( bluetoothGatt != null ) {
            bluetoothGatt.close();
        }
    }

    public static void showMessageDebug(String message){
        showMessage(message);
    }

}