/***********************************************************************
 * Copyright (c) 2017 Milan Jaitner                                   *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package bt.doorcontrol;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import bt.doorcontrol.bt.BluetoothDevicePicker;
import bt.doorcontrol.bt.BluetoothObserver;
import bt.doorcontrol.device.PairedDevice;
import bt.doorcontrol.service.BackgroundService;

import java.io.IOException;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends Activity implements BluetoothObserver.Callback
{
    private static final int REQUEST_BT_PAIRING_CODE = 42;
    public static final String INTERACT_WITH_DOORS = "interact_with_doors";

    private final char OPEN = '1';
    private final char OPENING = '2';
    private final char CLOSE = '6';
    private final char CLOSING = '7';
    private final char STOPPED = '9';

    private enum DoorState
    {
        Open, Opening, Closing, Stopped, Close
    }

    //

    //private String pairedDeviceMAC = "B8:27:EB:CC:5C:C9";

    private PairedDevice pairedDevice;

    private BluetoothObserver bluetoothObserver;

    //

    private View unlockContainerView;
    private View pairGuideContainer;

    private Button unlockButton;

    private TextView statusTextView;

    private Button pairButton;

    private DoorState currentDoorState = DoorState.Close;

    private BroadcastReceiver deviceSelectedBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String deviceName = device.getName();
                String mac = device.getAddress();

                Log.d(Config.tag, "Paring with device device: " + deviceName + ", mac: " + mac + ", bond state: " + device.getBondState());

                if (device.getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    pairedDevice = new PairedDevice(deviceName, mac);
                    pairedDevice.save(MainActivity.this);

                    initialize(device);
                }
            }
        }
    };

    //


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothDevice device = null;

        this.bluetoothObserver = new BluetoothObserver(this, this);

        this.pairedDevice = PairedDevice.create(this);

        if (pairedDevice != null)
            device = bluetoothObserver.getPairedDevice(pairedDevice.getMac());

        this.pairGuideContainer = findViewById(R.id.pair_guide_container);
        this.unlockContainerView = findViewById(R.id.unlock_container);

        updateLayouts();

        this.unlockButton = (Button) findViewById(R.id.unlock);
        unlockButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onClickUnlock();
            }
        });

        this.statusTextView = (TextView) findViewById(R.id.status);

        this.pairButton = (Button) findViewById(R.id.pair);
        pairButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Activity ctx = MainActivity.this;
                new BluetoothDevicePicker(ctx).startDiscovery(ctx, new BluetoothDevicePicker.IDeviceSelectedCallback()
                {
                    @Override
                    public void onDeviceSelected(BluetoothDevice device)
                    {
                        pairedDevice = new PairedDevice(device);
                        pairedDevice.save(MainActivity.this);

                        initialize(device);
                    }
                });
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(deviceSelectedBroadcastReceiver, filter);

        if (device != null)
            initialize(device);

        BackgroundService.start(this);
    }

    private void updateLayouts()
    {
        boolean hasPairedDevice = pairedDevice != null;

        pairGuideContainer.setVisibility(hasPairedDevice ? GONE : VISIBLE);
        unlockContainerView.setVisibility(hasPairedDevice ? VISIBLE : GONE);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (bluetoothObserver != null)
        {
            //	bluetoothObserver.enableBluetooth(false);
            bluetoothObserver.disconnect();
        }

        unregisterReceiver(deviceSelectedBroadcastReceiver);
    }

    private void onClickUnlock()
    {
        switch (currentDoorState)
        {
            case Open:
                bluetoothObserver.write("close");
                break;

            case Opening:
            case Closing:
                bluetoothObserver.write("stop");
                break;

            case Stopped:
                bluetoothObserver.write("resume");
                break;

            case Close:
                bluetoothObserver.write("open");
                break;
        }
    }

    //

    private void initialize(BluetoothDevice device)
    {
        if (device == null)
            return;

        bluetoothObserver.enableBluetooth(true);
        bluetoothObserver.connect(device);

        updateLayouts();
    }

    @Override
    public void onDiscoveryStarted()
    {
        setProgressMessage("Searching...");
    }

    @Override
    public void onDiscoveryFinished()
    {

    }

    @Override
    public void onBluetoothConnecting(BluetoothDevice device)
    {
        setProgressMessage("Connecting to device " + device.getName());
    }

    @Override
    public void onBluetoothConnected()
    {
        setProgressMessage("Connected");

        bluetoothObserver.write("stat");
    }

    @Override
    public void onBluetoothConnectFailed(Exception e)
    {
        if (e instanceof IOException)
            setProgressMessage("Garage not in range");
        else
            setProgressMessage("Connection failed");
    }

    @Override
    public void onBluetoothConnectionLost(Exception e)
    {
        setProgressMessage("Connection lost");
    }

    @Override
    public void onBluetoothDataReceived(byte[] data)
    {
        if (data != null && data.length > 0)
        {
            byte flag = data[0];

            switch (flag)
            {
                case OPEN:
                    currentDoorState = DoorState.Open;
                    setUnlockButtonText("Close");
                    break;

                case OPENING:
                    currentDoorState = DoorState.Opening;
                    setUnlockButtonText("Opening...");
                    break;

                case CLOSING:
                    currentDoorState = DoorState.Closing;
                    setUnlockButtonText("Closing...");
                    break;

                case CLOSE:
                    currentDoorState = DoorState.Close;
                    setUnlockButtonText("Open");
                    break;

                case STOPPED:
                    currentDoorState = DoorState.Stopped;
                    setUnlockButtonText("Stopped");
                    break;
            }

            Log.i(Config.tag, "received data: " + (char) flag);
        }
    }

    //

    private void setProgressMessage(final String msg)
    {
        Log.d(Config.tag, msg);

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                statusTextView.setText(msg);
            }
        });
    }

    private void setUnlockButtonText(final String text)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                unlockButton.setText(text);
            }
        });
    }
}

