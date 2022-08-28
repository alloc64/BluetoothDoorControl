/***********************************************************************
 * Copyright (c) 2017 Milan Jaitner                                   *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package bt.doorcontrol.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.Set;
import java.util.UUID;

public class BluetoothObserver
{
    public enum State
    {
        Disconnected,
        Discovery,
        Connecting,
        Connected,
    }

    public interface Callback
    {
        void onDiscoveryStarted();

        void onBluetoothConnecting(BluetoothDevice device);

        void onBluetoothConnected();

        void onBluetoothConnectFailed(Exception e);

        void onBluetoothConnectionLost(Exception e);

        void onBluetoothDataReceived(byte[] data);

        void onDiscoveryFinished();
    }

    public static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Context ctx;
    private BluetoothAdapter bluetoothAdapter;

    private Callback callback;

    private BluetoothConnector bluetoothConnector;

    private boolean isBluetoothEnabled;

    private State state;

    public BluetoothObserver(Context ctx, Callback callback)
    {
        this.ctx = ctx;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.callback = callback;
    }

    public State getState()
    {
        return state;
    }

    public void setState(State state)
    {
        this.state = state;
    }

    public BluetoothDevice getPairedDevice(String deviceMac)
    {
        Set<BluetoothDevice> pairedDevices = this.bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device : pairedDevices)
            if (deviceMac != null && deviceMac.equals(device.getAddress()))
                return device;

        return null;
    }

    public void connect(BluetoothDevice device)
    {
        if (state == State.Connected)
            return;

        if (bluetoothConnector != null)
        {
            bluetoothConnector.disconnect();
            bluetoothConnector.interrupt();
        }

        this.bluetoothConnector = new BluetoothConnector(this, bluetoothAdapter, callback);

        try
        {
            bluetoothConnector.connect(device);
        }
        catch (Exception e)
        {
            //TODO: interakce?
            e.printStackTrace();

            if (callback != null)
                callback.onBluetoothConnectFailed(e);
        }
    }

    public void write(String data)
    {
        if (data == null)
            return;

        write(data.getBytes());
    }

    public void write(byte[] data)
    {
        //TODO: pokud se pokusim zapisovat do neinicializovanyho pripojeni, zkusime connect

        if (bluetoothConnector != null)
            bluetoothConnector.write(data);
    }

    public void disconnect()
    {
        if (bluetoothConnector != null)
            bluetoothConnector.disconnect();
    }

    public boolean enableBluetooth(boolean state)
    {
        this.isBluetoothEnabled = bluetoothAdapter.isEnabled();

        if (state && !isBluetoothEnabled)
            return bluetoothAdapter.enable();
        else if (!state && isBluetoothEnabled)
            return bluetoothAdapter.disable();

        return true;
    }

    public boolean isBluetoothEnabled()
    {
        return bluetoothAdapter.isEnabled();
    }
}
