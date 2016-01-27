package se.gladpingvin.develop.demohealthgateway;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHealth;
import android.bluetooth.BluetoothHealthAppConfiguration;
import android.bluetooth.BluetoothHealthCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Service Class for handling the actual Bluetooth communication, quite much the same code as in the
 * Android example from the Antidote project (https://github.com/signove/antidote), which in turn
 * was heavily influenced by the HDP example from the Android SDK.
 */
public class BluetoothCommunicationService extends Service {
    private static final String TAG = "BTCSERVICE";

    public static final int RESULT_OK = 0;
    public static final int RESULT_FAIL = -1;

    // Status codes to be sent to "clients"
    public static final int STATUS_HEALTH_APP_REG = 100;
    public static final int STATUS_HEALTH_APP_UNREG = 101;
    public static final int STATUS_CREATE_CHANNEL = 102;
    public static final int STATUS_DESTROY_CHANNEL = 103;
    public static final int STATUS_READ_DATA = 104;
    public static final int STATUS_READ_DATA_DONE = 105;

    // Message codes for "clients" to send
    public static final int MSG_REG_CLIENT = 200;
    public static final int MSG_UNREG_CLIENT = 201;
    public static final int MSG_REG_HEALTH_APP = 300;
    public static final int MSG_UNREG_HEALTH_APP = 301;
    public static final int MSG_CONNECT_CHANNEL = 400;
    public static final int MSG_DISCONNECT_CHANNEL = 401;
    public static final int MSG_SEND_DATA = 501;

    private List<BluetoothHealthAppConfiguration> configurations =
            new ArrayList<>();

    private HashMap<BluetoothDevice, List<BluetoothHealthAppConfiguration>> deviceConfigurations =
            new HashMap<>();

    private BluetoothHealth bluetoothHealth;

    private Messenger clientMessenger;

    private HashMap<BluetoothDevice, Integer> channelIds = new HashMap<>();
    private HashMap<BluetoothDevice, FileOutputStream> writers =
            new HashMap<>();

    private boolean acceptsConfiguration(BluetoothHealthAppConfiguration configuration) {
        return configurations.contains(configuration);
    }

    private List<BluetoothHealthAppConfiguration> getDeviceConfigurations(BluetoothDevice device) {
        if (!deviceConfigurations.containsKey(device)) {
            if (configurations.size() > 0) {
                return configurations;
            } else {
                return null;
            }
        }

        return deviceConfigurations.get(device);
    }

    private BluetoothHealthAppConfiguration getDeviceConfiguration(BluetoothDevice device) {
        List<BluetoothHealthAppConfiguration> configurations =
                getDeviceConfigurations(device);

        // TODO: Add logic that chooses the right one from the list
        return configurations != null ? configurations.get(0) : null;
    }

    private void insertDeviceConfiguration(BluetoothDevice device,
                                            BluetoothHealthAppConfiguration configuration) {
        List<BluetoothHealthAppConfiguration> configurationList =
                new ArrayList<>();
        configurationList.add(configuration);

        insertDeviceConfigurations(device, configurationList);
    }

    private synchronized void insertDeviceConfigurations(BluetoothDevice device,
                                             List<BluetoothHealthAppConfiguration> configurations) {
        if (deviceConfigurations.containsKey(device)) {
            List<BluetoothHealthAppConfiguration> currentList = deviceConfigurations.get(device);
            configurations.addAll(currentList);
        }

        deviceConfigurations.put(device, configurations);
    }

    private int getChannelId(BluetoothDevice device) {
        return channelIds.containsKey(device) ? channelIds.get(device) : RESULT_FAIL;
    }

    private synchronized void insertChannelId(BluetoothDevice device, int channelId) {
        channelIds.put(device, channelId);
    }

    private FileOutputStream getWriter(BluetoothDevice device) {
        return writers.containsKey(device) ? writers.get(device) : null;
    }

    private synchronized void insertWriter(BluetoothDevice device, FileOutputStream writer) {
        writers.put(device, writer);
    }

    private synchronized void removeWriter(BluetoothDevice device) {
        FileOutputStream writer = getWriter(device);
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ioe) {
                Log.e(TAG, "Couldn't close writer for " + device.getName());
            }

            writers.remove(device);
        }
    }

    @SuppressLint("HandlerLeak")
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REG_CLIENT:
                    Log.v(TAG, "Activity registered!");
                    clientMessenger = msg.replyTo;
                    break;
                case  MSG_UNREG_CLIENT:
                    clientMessenger = null;
                    break;
                case MSG_REG_HEALTH_APP:
                    registerApp(msg.arg1);
                    break;
                case MSG_UNREG_HEALTH_APP:
                    unregisterApp();
                    break;
                case MSG_CONNECT_CHANNEL:
                    connectChannel((BluetoothDevice) msg.obj);
                    break;
                case MSG_SEND_DATA:
                    Object[] data = (Object[]) msg.obj;
                    sendData((BluetoothDevice) data[0], (byte[]) data[1]);
                    break;
                case MSG_DISCONNECT_CHANNEL:
                    disconnectChannel((BluetoothDevice) msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private final Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            // Shouldn't happen as Bluetooth is required to install the app
            Log.e(TAG, "Bluetooth was not found :o!!");
            stopSelf();
            return;
        }

        if (!bluetoothAdapter.getProfileProxy(this, bluetoothServiceListener,
                BluetoothProfile.HEALTH)) {
            // Shouldn't happen as the app shouldn't install on Android version that are too old,
            // meaning the Health Profile should exist!
            Log.e(TAG, "Health profile not found :'(...");
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Bluetooth service is up and running :D!");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    private void registerApp(int dataType) {
        bluetoothHealth.registerSinkAppConfiguration(TAG, dataType, healthCallback);
    }

    private void unregisterApp() {
        for (BluetoothHealthAppConfiguration configuration : configurations) {
            bluetoothHealth.unregisterAppConfiguration(configuration);
        }
    }

    private void connectChannel(BluetoothDevice device) {
        Log.v(TAG, "connectChannel(" + device + ")");
        BluetoothHealthAppConfiguration configuration = getDeviceConfiguration(device);
        if (configuration != null) {
            bluetoothHealth.connectChannelToSource(device, configuration);
        }
    }

    private void sendData (BluetoothDevice device, byte[] data) {
        FileOutputStream writer = getWriter(device);
        if (writer == null) {
            Log.w(TAG, "No stream for sending data to HDP!");
            return;
        }

        try {
            writer.write(data);
        } catch (IOException ioe) {
            removeWriter(device);
            Log.e(TAG, "Couldn't send data to HDP for some reason!");
        }
    }

    private void disconnectChannel(BluetoothDevice device) {
        Log.v(TAG, "disconnectChannel(" + device + ")");
        BluetoothHealthAppConfiguration configuration = getDeviceConfiguration(device);

        if (configuration != null) {
            bluetoothHealth.disconnectChannel(device, configuration, getChannelId(device));
        }
    }

    private final BluetoothProfile.ServiceListener bluetoothServiceListener =
            new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (profile == BluetoothProfile.HEALTH) {
                        bluetoothHealth = (BluetoothHealth) proxy;
                        Log.v(TAG, "onServiceConnected to profile: " + profile);
                    }
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    if (profile == BluetoothProfile.HEALTH) {
                        bluetoothHealth = null;
                    }
                }
            };

    private final BluetoothHealthCallback healthCallback = new BluetoothHealthCallback() {
        @Override
        public void onHealthAppConfigurationStatusChange(BluetoothHealthAppConfiguration config,
                                                         int status) {
            if (status == BluetoothHealth.APP_CONFIG_REGISTRATION_FAILURE) {
                sendMessage(STATUS_HEALTH_APP_REG, RESULT_FAIL, null);
            } else if (status == BluetoothHealth.APP_CONFIG_REGISTRATION_SUCCESS) {
                configurations.add(config);
                sendMessage(STATUS_HEALTH_APP_REG, RESULT_OK, null);
            } else if (status == BluetoothHealth.APP_CONFIG_UNREGISTRATION_FAILURE ||
                    status == BluetoothHealth.APP_CONFIG_UNREGISTRATION_SUCCESS) {
                sendMessage(STATUS_HEALTH_APP_UNREG,
                        status == BluetoothHealth.APP_CONFIG_UNREGISTRATION_SUCCESS ?
                            RESULT_OK : RESULT_FAIL,
                            null);
            }
        }

        @Override
        public void onHealthChannelStateChange(BluetoothHealthAppConfiguration config, BluetoothDevice device, int prevState, int newState, ParcelFileDescriptor fd, int channelId) {
            if (prevState == BluetoothHealth.STATE_CHANNEL_DISCONNECTED &&
                    newState == BluetoothHealth.STATE_CHANNEL_CONNECTED) {
                if (acceptsConfiguration(config)) {
                    insertDeviceConfiguration(device, config);
                    insertChannelId(device, channelId);
                    sendMessage(STATUS_CREATE_CHANNEL, RESULT_OK, device);
                    FileOutputStream writer = new FileOutputStream(fd.getFileDescriptor());
                    insertWriter(device, writer);
                    (new ReadThread(device, fd)).start();
                } else {
                    sendMessage(STATUS_CREATE_CHANNEL, RESULT_FAIL, device);
                }
            } else if (prevState == BluetoothHealth.STATE_CHANNEL_CONNECTING &&
                    newState == BluetoothHealth.STATE_CHANNEL_DISCONNECTED) {
                sendMessage(STATUS_CREATE_CHANNEL, RESULT_FAIL, device);
                removeWriter(device);
            } else if (newState == BluetoothHealth.STATE_CHANNEL_DISCONNECTED) {
                if (acceptsConfiguration(config)) {
                    sendMessage(STATUS_DESTROY_CHANNEL, RESULT_OK, device);
                    removeWriter(device);
                } else {
                    sendMessage(STATUS_DESTROY_CHANNEL, RESULT_FAIL, device);
                    removeWriter(device);
                }
            }
        }
    };

    private void sendMessage(int what, int value, Object obj) {
        if (clientMessenger == null) {
            Log.w(TAG, "Tried sending a message without any clients connected!");
            return;
        }

        try {
            Message msg = Message.obtain(null, what, value);
            msg.obj = obj;
            clientMessenger.send(msg);
        } catch (RemoteException re) {
            Log.e(TAG, "Couldn't send message to client");
        }
    }

    private class ReadThread extends Thread {
        private ParcelFileDescriptor pfd;
        private BluetoothDevice device;

        public ReadThread(BluetoothDevice device, ParcelFileDescriptor pfd) {
            super();
            this.pfd = pfd;
            this.device = device;
        }

        @Override
        public void run() {
            FileInputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
            final byte[] data = new byte[8192];

            try {
                int length;
                while ((length = inputStream.read(data)) > -1) {
                    if (length > 0) {
                        byte[] buffer = new byte[length];
                        System.arraycopy(data, 0, buffer, 0, length);

                        Log.v(TAG, "Reading stuffs!");

                        Object[] pair = new Object[2];
                        pair[0] = device;
                        pair[1] = buffer;
                        sendMessage(STATUS_READ_DATA, RESULT_OK, pair);
                    }
                }
            } catch (IOException ioe) {
                if (pfd != null) {
                    try {
                        pfd.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Couldn't close file descriptor " + pfd.toString());
                    }
                }
            }

            removeWriter(device);
            sendMessage(STATUS_READ_DATA_DONE, RESULT_OK, device);
        }
    }
}
