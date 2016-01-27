package se.gladpingvin.develop.demohealthgateway;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Service class for handling the communication between BluetoothCommunicationService and the
 * Antidote library (via JNIBridge). More or less the same code as in the Android example of the
 * Antidote project (https://github.com/signove/antidote).
 */
public class HealthService extends Service {
    private String TAG = "HSS";
    private String PATH_PREFIX = "/se/gladpingvin/develop/demohealthgateway/device/";
    private Handler handler;
    private JNIBridge antidote;

    private static final int [] HEALTH_PROFILE_SOURCE_DATA_TYPES = {0x1004, 0x1007, 0x1029, 0x100f};

    private Messenger mHealthService;
    private boolean mHealthServiceBound;

    private List<HealthAgentAPI> agents = new ArrayList<>();

    private int timer_id = 0;
    private int context_id = 0;
    private HashMap<Integer, Runnable> timers = new HashMap<>();

    private HashMap<Integer, BluetoothDevice> ctx_dev = new HashMap<>();
    private HashMap<String, Integer> addr_ctx = new HashMap<>();
    private HashMap<String, Integer> path_ctx = new HashMap<>();

    private int new_context()
    {
        ++context_id;
        return context_id;
    }

    private synchronized int insert_context(BluetoothDevice dev)
    {
        String addr = dev.getAddress();

        if (addr_ctx.containsKey(addr)) {
            throw new AssertionError("Trying to reinsert context for " + addr);
        }
        int context = new_context();

        ctx_dev.put(context, dev);
        addr_ctx.put(addr, context);
        path_ctx.put(PATH_PREFIX + context, context);

        return context;
    }

    /**
     * Returns the context index for a Bluetooth device (and creates it if it doesn't yet exist).
     * @param dev the BluetoothDevice for which to get the context index
     * @return the context index for dev
     */
    public int get_context(BluetoothDevice dev)
    {
        String addr = dev.getAddress();

        if (! addr_ctx.containsKey(addr)) {
            insert_context(dev);
        }

        return addr_ctx.get(addr);
    }

    /**
     * Returns the context index for a Personal Health Device, using its path as identifier.
     * @param path the path of the PHD for which to get the context index
     * @return the context index of the PHD with path as its path, returns 0 if the path isn't yet
     * registered
     */
    public int get_context(String path)
    {
        if (! path_ctx.containsKey(path)) {
            Log.w(TAG, "Path " + path + " has no associated context");
            return 0;
        }
        return path_ctx.get(path);
    }

    /**
     * Returns the BluetoothDevice object for a context index
     * @param context the context index for which to return the corresponding BluetoothDevice
     * @return the BluetoothDevice with context as its context index, null if no device is
     * registered with context as index
     */
    public BluetoothDevice get_device(int context)
    {
        if (! ctx_dev.containsKey(context)) {
            Log.w(TAG, "Context " + context + " has no associated device");
            return null;
        }
        return ctx_dev.get(context);
    }

    /**
     * To be used by Antidote(hence the C style method name), disconnects the Bluetooth device with
     * a given context index
     * @param context the context index of the BluetoothDevice which should be disconnected
     */
    public void disconnect_channel(int context)
    {
        disconnectChannel(context);
    }

    /**
     * To be used by Antidote (hence the C style method name), sends binary data to the Bluetooth
     * device with a given context index
     * @param context the context index of the Bluetooth device to send data to
     * @param data the actual binary data to send
     */
    public void send_data(int context, byte [] data)
    {
        sendData(context, data);
    }

    /**
     * To be used by Antidote (hence the C style method name), stops and removes the timer with a
     * given timer id
     * @param timer_id the timer id for the timer to stop and remove
     */
    public synchronized void cancel_timer(int timer_id)
    {
        Runnable r = timers.get(timer_id);
        if (r != null) {
            Log.w(TAG, "Cancelling timer " + timer_id);
            handler.removeCallbacks(r);
            timers.remove(timer_id);
        } else {
            Log.w(TAG, "Tried to cancel unknown timer " + timer_id);
        }
    }

    /**
     * To be used by Antidote (hence the C style method name), creates a timer that sends an alarm
     * to a given context at a given interval
     * @param milliseconds the interval length in milliseconds
     * @param context the context to send alarm signals to
     * @return the id of the created timer
     */
    public synchronized int create_timer(int milliseconds, int context)
    {
        final int ctx = context;

        if (++timer_id > 0x7ffffffe) {
            timer_id = 1;
        }

        Log.w(TAG, "Creating timer " + timer_id + " timeout " + milliseconds + "ms");

        final Runnable task = new Runnable () {
            public void run() {
                Log.w(TAG, "Timer callback " + timer_id + " ctx " + ctx);
                antidote.timer_alarm(ctx);
            }
        };

        handler.postDelayed(task, milliseconds);
        timers.put(timer_id, task);

        return timer_id;
    }

    /**
     * To be used by Antidote, sends an "Associated message" to the HealthAgent with a given context
     * id and removes all other HealthAgents from the list of configured HealthAgents
     * @param context the context id of the HealthAgent to send the "Associated message" to
     * @param xml the "Associated message" as an xml String
     */
    public void associated(int context, String xml)
    {
        sendAssociated(context, xml);
    }

    /**
     * To be used by Antidote, sends a "Disassociated message" to the HealthAgent with a given
     * context id and removes all other HealthAgents from the list of configured HealthAgents
     * @param context the context id of the HealthAgent to send the "Disassociated message" to
     */
    public void disassociated(int context)
    {
        sendDisassociated(context);
    }

    /**
     * To be used by Antidote, sends an "Attributes message" to the HealthAgent with a given context
     * id and removes all other HealthAgents from the list of configured HealthAgents
     * @param context the context id of the HealthAgent to send the "Attributes message" to
     * @param xml the "Attributes message" as an xml String
     */
    public void deviceattributes(int context, String xml)
    {
        sendDeviceAttributes(context, xml);
    }

    /**
     * To be used by Antidote, sends a "Measurement message" to the HealthAgent with a given context
     * id and removes all other HealthAgents from the list of configured HealthAgents
     * @param context the context id of the HealthAgent to send the "Measurement message" to
     * @param xml the "Measurement message" as an xml String
     */
    public void measurementdata(int context, String xml)
    {
        sendMeasurementData(context, xml);
    }

    // Handles events sent by {@link HealthHDPService}.
    @SuppressLint("HandlerLeak")
    private Handler mIncomingHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int context;
            BluetoothDevice mDevice;

            switch (msg.what) {

                case BluetoothCommunicationService.STATUS_HEALTH_APP_REG:
                    Log.w(TAG, "HDP Registered");
                    break;

                case BluetoothCommunicationService.STATUS_HEALTH_APP_UNREG:
                    Log.w(TAG, "HDP Unregistered");
                    break;

                case BluetoothCommunicationService.STATUS_READ_DATA:
                    Log.w(TAG, "HDP data");
                    Object [] pair = (Object []) msg.obj;
                    context = get_context((BluetoothDevice) pair[0]);
                    if (context > 0) {
                        antidote.data_received(context, (byte []) pair[1]);
                    }
                    break;

                case BluetoothCommunicationService.STATUS_READ_DATA_DONE:
                    Log.w(TAG, "HDP closed channel");
                    mDevice = (BluetoothDevice) msg.obj;
                    context = get_context(mDevice);
                    if (context > 0) {
                        antidote.channel_disconnected(context);
                        sendDisconnected(context);
                    }
                    break;

                case BluetoothCommunicationService.STATUS_CREATE_CHANNEL:
                    Log.w(TAG, "HDP create channel complete");
                    mDevice = (BluetoothDevice) msg.obj;
                    context = get_context(mDevice);
                    if (context > 0) {
                        antidote.channel_connected(context);
                        sendConnected(context, mDevice);
                    }
                    break;
                // Channel destroy complete.
                case BluetoothCommunicationService.STATUS_DESTROY_CHANNEL:
                    Log.w(TAG, "HDP destroy channel complete");
                    mDevice = (BluetoothDevice) msg.obj;
                    context = get_context(mDevice);
                    if (context > 0) {
                        antidote.channel_disconnected(context);
                        sendDisconnected(context);
                    }
                    break;
            }
        }
    };

    private final Messenger mMessenger = new Messenger(mIncomingHandler);


    // Sets up communication with {@link BluetoothHDPService}.
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.w(TAG, "HDP service connected");
            mHealthServiceBound = true;
            Message msg = Message.obtain(null, BluetoothCommunicationService.MSG_REG_CLIENT);
            msg.replyTo = mMessenger;
            mHealthService = new Messenger(service);
            try {
                mHealthService.send(msg);
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to register client to service.");
                e.printStackTrace();
            }
            for (int type: HEALTH_PROFILE_SOURCE_DATA_TYPES) {
                sendMessage(BluetoothCommunicationService.MSG_REG_HEALTH_APP, type);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mHealthService = null;
            mHealthServiceBound = false;
        }
    };

    private void connectChannel(int context) {
        // Note: context must be known. If device never connected before,
        // context/device mapping must be injected someway
        BluetoothDevice dev = get_device(context);
        if (dev != null)
            sendMessageWithDevice(BluetoothCommunicationService.MSG_CONNECT_CHANNEL, dev);
    }

    private void disconnectChannel(int context) {
        BluetoothDevice dev = get_device(context);
        if (dev != null)
            sendMessageWithDevice(BluetoothCommunicationService.MSG_DISCONNECT_CHANNEL, dev);
    }

    private void sendData(int context, byte [] data) {
        BluetoothDevice dev = get_device(context);
        if (dev != null) {
            Object [] pair = new Object[2];
            pair[0] = dev;
            pair[1] = data;
            sendMessageWithData(BluetoothCommunicationService.MSG_SEND_DATA, pair);
        }
    }

    private void initialize() {
        // Starts health service.
        Log.w(TAG, "initialize()");
        Intent intent = new Intent(this, BluetoothCommunicationService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    // Intent filter and broadcast receive to handle Bluetooth on event.
    private IntentFilter initIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return filter;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) ==
                        BluetoothAdapter.STATE_ON) {
                    initialize();
                }
            }
        }
    };

    // Sends a message to {@link BluetoothHDPService}.
    private void sendMessage(int what, int value) {
        if (mHealthService == null) {
            Log.w(TAG, "Health Service not connected.");
            return;
        }

        try {
            mHealthService.send(Message.obtain(null, what, value, 0));
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to reach service.");
            e.printStackTrace();
        }
    }

    // Sends an update message, along with an HDP BluetoothDevice object, to
    // {@link BluetoothHDPService}.  The BluetoothDevice object is needed by the channel creation
    // method.
    private void sendMessageWithDevice(int what, BluetoothDevice mDevice) {
        if (mHealthService == null) {
            Log.d(TAG, "Health Service not connected.");
            return;
        }

        try {
            mHealthService.send(Message.obtain(null, what, mDevice));
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to reach service.");
            e.printStackTrace();
        }
    }

    private void sendMessageWithData(int what, Object [] pair) {
        if (mHealthService == null) {
            Log.d(TAG, "Health Service not connected.");
            return;
        }

        try {
            mHealthService.send(Message.obtain(null, BluetoothCommunicationService.MSG_SEND_DATA, pair));
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to reach service.");
            e.printStackTrace();
        }
    }

    private void sendConnected(int context, BluetoothDevice dev) {
        List<HealthAgentAPI> tmp = new ArrayList<>(agents);
        for (HealthAgentAPI agent: tmp) {
            try {
                agent.Connected(PATH_PREFIX + context, dev.getAddress());
                Log.w(TAG, "Sent connected to " + agent);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to comm with listener " + agent);
                agents.remove(agent);
            }
        }
    }

    private void sendAssociated(int context, String xml_associated) {
        List<HealthAgentAPI> tmp = new ArrayList<>(agents);
        for (HealthAgentAPI agent: tmp) {
            try {
                agent.Associated(PATH_PREFIX + context, xml_associated);
                Log.w(TAG, "Sent associated to " + agent);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to comm with listener " + agent);
                agents.remove(agent);
            }
        }
    }

    private void sendMeasurementData(int context, String xml_measurement) {
        List<HealthAgentAPI> tmp = new ArrayList<>(agents);
        for (HealthAgentAPI agent: tmp) {
            try {
                agent.MeasurementData(PATH_PREFIX + context, xml_measurement);
                Log.w(TAG, "Sent measurement to " + agent);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to comm with listener " + agent);
                agents.remove(agent);
            }
        }
    }

    private void sendDisassociated(int context) {
        List<HealthAgentAPI> tmp = new ArrayList<>(agents);
        for (HealthAgentAPI agent: tmp) {
            try {
                agent.Disassociated(PATH_PREFIX + context);
                Log.w(TAG, "Sent disassociated to " + agent);
            } catch (RemoteException e) {
                agents.remove(agent);
                Log.w(TAG, "Failed to comm with listener " + agent);
            }
        }
    }

    private void sendDisconnected(int context) {
        List<HealthAgentAPI> tmp = new ArrayList<>(agents);
        for (HealthAgentAPI agent: tmp) {
            try {
                agent.Disconnected(PATH_PREFIX + context);
                Log.w(TAG, "Sent disconnected to " + agent);
            } catch (RemoteException e) {
                agents.remove(agent);
                Log.w(TAG, "Failed to comm with listener " + agent);
            }
        }
    }

    private void sendDeviceAttributes(int context, String xml_attributes) {
        List<HealthAgentAPI> tmp = new ArrayList<>(agents);
        for (HealthAgentAPI agent: tmp) {
            try {
                agent.DeviceAttributes(PATH_PREFIX + context, xml_attributes);
                Log.w(TAG, "Sent device attributes to " + agent);
            } catch (RemoteException e) {
                agents.remove(agent);
                Log.w(TAG, "Failed to comm with listener " + agent);
            }
        }

    }

    private HealthServiceAPI.Stub apiEndpoint = new HealthServiceAPI.Stub() {
        @Override
        public void RequestDeviceAttributes(String dev) throws RemoteException {
            Log.w(TAG, "Asking deviceAttributes");
            int context = get_context(dev);
            antidote.reqmdsattr(context);
        }

        @Override
        public String GetConfiguration(String dev) throws RemoteException {
            Log.w(TAG, "Returning config");
            int context = get_context(dev);
            return antidote.getconfig(context);
        }

        // FIXME add to AIDL
        // @Override
        public void ReleaseAssociation(String dev) throws RemoteException {
            Log.w(TAG, "Releasing association (asked by client)");
            int context = get_context(dev);
            antidote.releaseassoc(context);
        }

        // FIXME add to AIDL
        // @Override
        public void AbortAssociation(String dev) throws RemoteException {
            Log.w(TAG, "Aborting association (asked by client)");
            int context = get_context(dev);
            antidote.abortassoc(context);
        }

        @Override
        public void ConfigurePassive(HealthAgentAPI agt, int [] specs) throws RemoteException {
            Log.w(TAG, "ConfigurePassive");
            agents.add(agt);
            Log.w(TAG, "Configured agent " + agt);
        }

        @Override
        public void Unconfigure(HealthAgentAPI agt) throws RemoteException  {
            Log.w(TAG, "Unconfigure");
            agents.remove(agt);
            Log.w(TAG, "Unconfigured agent " + agt);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return apiEndpoint;
    }

    @Override
    public void onCreate() {
        // If Bluetooth is not on, request that it be enabled.
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth not available");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        } else {
            initialize();
        }

        mHealthServiceBound = false;

        registerReceiver(mReceiver, initIntentFilter());

        handler = new Handler();
        antidote = new JNIBridge(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHealthServiceBound) unbindService(mConnection);
        unregisterReceiver(mReceiver);
    }
}