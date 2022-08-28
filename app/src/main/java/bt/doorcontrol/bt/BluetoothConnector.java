/***********************************************************************
 * Copyright (c) 2017 Milan Jaitner                                   *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package bt.doorcontrol.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import bt.doorcontrol.Config;

import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothConnector extends Thread
{
    private BluetoothObserver bluetoothObserver;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothDevice device;

    private BluetoothSocket socket;

    private InputStream socketInputStream;

    private OutputStream socketOutputStream;

    private byte[] buffer = new byte[1024];

    private BluetoothObserver.Callback callback;

    private boolean isRunning = true;

    private boolean isConnected = false;

    //

    public BluetoothConnector(BluetoothObserver bluetoothObserver, BluetoothAdapter bluetoothAdapter, BluetoothObserver.Callback callback)
    {
        this.bluetoothObserver = bluetoothObserver;
        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;
    }

    @Override
    public void run()
    {
        super.run();

        do
        {
            try
            {
                bluetoothObserver.setState(BluetoothObserver.State.Connecting);

                if (callback != null)
                    callback.onBluetoothConnecting(device);

                this.socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{Integer.TYPE}).invoke(device, 1);
                //TODO: secure socket this.socket = device.createRfcommSocketToServiceRecord(BluetoothObserver.SerialPortServiceClass_UUID);

                this.socket.connect();

                // zde by jsme meli mit connection

                this.socketInputStream = socket.getInputStream();
                this.socketOutputStream = socket.getOutputStream();

                onBluetoothConnected();

                while (socket.isConnected())
                {
                    try
                    {
                        int length = this.socketInputStream.read(buffer);

                        if (length != -1)
                        {
                            byte[] readData = new byte[length];
                            System.arraycopy(buffer, 0, readData, 0, length);

                            onBluetoothDataReceived(readData);
                        }
                    }
                    catch (Exception e)
                    {
                        onBluetoothConnectionLost(e);

                        break;
                    }
                }
            }
            catch (Exception e2)
            {
                onBluetoothConnectFailed(e2);
            }

            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        while (!isConnected && isRunning);
    }

    //

    public void connect(BluetoothDevice device)
    {
        if (device != null)
        {
            this.device = device;
            start();
        }
        else
        {
            throw new IllegalStateException("BluetoothDevice not initialized.");
        }
    }

    public void write(byte[] data)
    {
        if (socketOutputStream == null)
            return;

        try
        {
            this.socketOutputStream.write(data);
        }
        catch (Exception e)
        {
            Log.d(Config.tag, "An error occured while writing to BT socket.");
        }
    }

    public void disconnect()
    {
        try
        {
            isRunning = false;
            socket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // region Events

    private void onBluetoothConnected()
    {
        bluetoothObserver.setState(BluetoothObserver.State.Connected);

        if (callback != null)
            callback.onBluetoothConnected();

        isConnected = true;
    }

    private void onBluetoothConnectFailed(Exception e)
    {
        bluetoothObserver.setState(BluetoothObserver.State.Disconnected);

        e.printStackTrace();

        if (callback != null)
            callback.onBluetoothConnectFailed(e);

        isConnected = false;
    }

    private void onBluetoothConnectionLost(Exception e)
    {
        bluetoothObserver.setState(BluetoothObserver.State.Disconnected);

        e.printStackTrace();

        if (callback != null)
            callback.onBluetoothConnectionLost(e);

        isConnected = false;
    }

    private void onBluetoothDataReceived(byte[] data)
    {
        Log.d(Config.tag, "onBluetoothDataReceived " + data.length);

        if (callback != null)
            callback.onBluetoothDataReceived(data);
    }

    // endregion
}
