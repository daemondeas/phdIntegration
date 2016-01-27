package se.gladpingvin.develop.demohealthgateway;

/**
 * A JNI Bridge class (i.e. a class that acts as a bridge between the Java world and the native
 * world in which the Antidote library resides), more or less the same code as in the Android
 * example of Antidote (https://github.com/signove/antidote). The main difference is the addition
 * of Javadoc comments to public methods.
 */
public class JNIBridge {
    private HealthService healthService;

    JNIBridge(HealthService service) {
        healthService = service;
        healthd_init();
    }

    protected void finalize() {
        healthd_finalize();
    }

    /**
     * To be used by Antidote(hence the C style method name), disconnects the Bluetooth device with
     * a given context index
     * @param context the context index of the BluetoothDevice which should be disconnected
     */
    public void disconnect_channel(int context)
    {
        healthService.disconnect_channel(context);
    }

    /**
     * To be used by Antidote (hence the C style method name), sends binary data to the Bluetooth
     * device with a given context index
     * @param context the context index of the Bluetooth device to send data to
     * @param data the actual binary data to send
     */
    public void send_data(int context, byte [] data)
    {
        healthService.send_data(context, data);
    }

    /**
     * To be used by the Bluetooth service, tells Antidote that the Bluetooth channel for the device
     * with a given context id is connected
     * @param context the context id of the connected Bluetooth device
     */
    public synchronized void channel_connected(int context) {
        Cchannelconnected(context);
    }

    /**
     * To be used by the Bluetooth service, tells Antidote that the Bluetooth channel for the device
     * with a given context id is disconnected
     * @param context the context id of the disconnected Bluetooth device
     */
    public synchronized void channel_disconnected(int context) {
        Cchanneldisconnected(context);
    }

    /**
     * To be used by the Bluetooth service, submits received data from a Personal Health Device to
     * Antidote
     * @param context the context id of the Bluetooth device which sent the data
     * @param data the actual data
     */
    public synchronized void data_received(int context, byte [] data)
    {
        Cdatareceived(context, data);
    }

    // Declaration of native functions, they will appear as not existing in Android Studio, because
    // the Antidote library is only included as pre-compiled binaries in the project
    public native void Cchannelconnected(int context);
    public native void Cchanneldisconnected(int context);
    public native void Cdatareceived(int context, byte [] data);

    /**
     * To be used by Antidote (hence the C style method name), stops and removes the timer with a
     * given timer id
     * @param handle the timer id for the timer to stop and remove
     */
    public void cancel_timer(int handle)
    {
        healthService.cancel_timer(handle);
    }

    /**
     * To be used by Antidote (hence the C style method name), creates a timer that sends an alarm
     * to a given context at a given interval
     * @param milliseconds the interval length in milliseconds
     * @param handle the context to send alarm signals to
     * @return the id of the created timer
     */
    public int create_timer(int milliseconds, int handle)
    {
        return healthService.create_timer(milliseconds, handle);
    }

    /**
     * To be used by Antidote, sends an "Associated message" to the HealthAgent with a given context
     * id and removes all other HealthAgents from the list of configured HealthAgents
     * @param context the context id of the HealthAgent to send the "Associated message" to
     * @param xml the "Associated message" as an xml String
     */
    public void associated(int context, String xml)
    {
        healthService.associated(context, xml);
    }

    /**
     * To be used by Antidote, sends a "Disassociated message" to the HealthAgent with a given
     * context id and removes all other HealthAgents from the list of configured HealthAgents
     * @param context the context id of the HealthAgent to send the "Disassociated message" to
     */
    public void disassociated(int context)
    {
        healthService.disassociated(context);
    }

    /**
     * To be used by Antidote, sends an "Attributes message" to the HealthAgent with a given context
     * id and removes all other HealthAgents from the list of configured HealthAgents
     * @param context the context id of the HealthAgent to send the "Attributes message" to
     * @param xml the "Attributes message" as an xml String
     */
    public void deviceattributes(int context, String xml)
    {
        healthService.deviceattributes(context, xml);
    }

    /**
     * To be used by Antidote, sends a "Measurement message" to the HealthAgent with a given context
     * id and removes all other HealthAgents from the list of configured HealthAgents
     * @param context the context id of the HealthAgent to send the "Measurement message" to
     * @param xml the "Measurement message" as an xml String
     */
    public void measurementdata(int context, String xml)
    {
        healthService.measurementdata(context, xml);
    }

    // FIXME implement PM-Store calls

    /**
     * Called by the alarm timers created by HealthService, just forwards the alarm for a device's
     * context to Antidote
     * @param handle the context for which the alarm is
     */
    public synchronized void timer_alarm(int handle)
    {
        Ctimeralarm(handle);
    }

    /**
     * Tells Antidote to initialise the healthd service with the Application's file path
     */
    public synchronized void healthd_init()
    {
        Chealthdinit(healthService.getApplicationContext().getFilesDir().toString());
    }

    /**
     * Called by the finalize method (meaning it is called when a JNIBridge object is garbage
     * collected), just calls Antidote's finalize function.
     */
    public synchronized void healthd_finalize()
    {
        Chealthdfinalize();
    }

    /**
     * To be called by the Bluetooth service, tells Antidote to release the association for the
     * Bluetooth device with a given context id
     * @param context the context id of the Bluetooth device
     */
    public synchronized void releaseassoc(int context)
    {
        Creleaseassoc(context);
    }

    /**
     * To be called by the Bluetooth service, tells Antidote to abort the association for the
     * Bluetooth device with a given context id
     * @param context the context id of the Bluetooth device
     */
    public synchronized void abortassoc(int context)
    {
        Cabortassoc(context);
    }

    /**
     * To be called by the Bluetooth service, asks Antidote about the configuration for the
     * Bluetooth device with a given context id
     * @param context the context id of the Bluetooth device
     * @return the configuration for the Bluetooth device with context as its context id as a String
     */
    public synchronized String getconfig(int context)
    {
        return Cgetconfig(context);
    }

    /**
     * To be called by the Bluetooth service, asks Antidote about the device attributes of the
     * Bluetooth device with a given context id
     * @param context the context id of the Bluetooth device
     */
    public synchronized void reqmdsattr(int context)
    {
        Creqmdsattr(context);
    }

    /**
     * To be called by the Bluetooth service, requests that Antidote activates the scanner for the
     * Bluetooth device with a given context and a given handle
     * @param context the context id of the Bluetooth device
     * @param handle the handle id
     */
    public synchronized void reqactivationscanner(int context, int handle)
    {
        Creqactivationscanner(context, handle);
    }

    /**
     * To be called by the Bluetooth service, requests that Antidote deactivates the scanner for the
     * Bluetooth device with a given context and a given handle
     * @param context the context id of the Bluetooth device
     * @param handle the handle id
     */
    public synchronized void reqdeactivationscanner(int context, int handle)
    {
        Creqdeactivationscanner(context, handle);
    }

    /**
     * To be called by the Bluetooth service, requests measurement data from the Bluetooth device
     * with a given context and a given handle
     * @param context
     */
    public synchronized void reqmeasurement(int context)
    {
        Creqmeasurement(context);
    }

    // Declaration of native functions, they will appear as not existing in Android Studio, because
    // the Antidote library is only included as pre-compiled binaries in the project
    public native void Ctimeralarm(int handle);
    public native void Chealthdinit(String tmp_path);
    public native void Chealthdfinalize();
    public native void Creleaseassoc(int context);
    public native void Cabortassoc(int context);
    public native String Cgetconfig(int context);
    public native void Creqmdsattr(int context);
    public native void Creqactivationscanner(int context, int handle);
    public native void Creqdeactivationscanner(int context, int handle);
    public native void Creqmeasurement(int context);

   static {
        System.loadLibrary("healthd");
   }
}
