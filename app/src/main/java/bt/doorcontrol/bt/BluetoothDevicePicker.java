/***********************************************************************
 * Copyright (c) 2017 Milan Jaitner                                   *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package bt.doorcontrol.bt;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.widget.ArrayAdapter;

public class BluetoothDevicePicker
{
    public interface IDeviceSelectedCallback
    {
        void onDeviceSelected(BluetoothDevice device);
    }

    private class DiscoveredDevice
    {
        private BluetoothDevice device;

        public DiscoveredDevice(BluetoothDevice device)
        {
            this.device = device;
        }

        public BluetoothDevice getBluetoothDevice()
        {
            return device;
        }

        @Override
        public String toString()
        {
            String name = device.getName();

            if (name == null)
                name = device.getAddress();

            if (name == null)
                name = "Unknown";

            return name;
        }
    }

    private final ArrayAdapter<DiscoveredDevice> arrayAdapter;

    private final BluetoothAdapter bluetoothAdapter;

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                boolean contains = false;
                for (int i = 0; i < arrayAdapter.getCount(); i++)
                {
                    DiscoveredDevice item = arrayAdapter.getItem(i);

                    if (item != null)
                    {
                        BluetoothDevice bd = item.getBluetoothDevice();

                        if (bd != null && bd.equals(device))
                        {
                            contains = true;
                            break;
                        }
                    }
                }

                if (!contains)
                    arrayAdapter.add(new DiscoveredDevice(device));
            }
        }
    };

    //

    public BluetoothDevicePicker(Context ctx)
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        ctx.registerReceiver(discoveryReceiver, filter);

        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.arrayAdapter = new ArrayAdapter<DiscoveredDevice>(ctx, android.R.layout.select_dialog_singlechoice);
    }

    public void startDiscovery(Context ctx, final IDeviceSelectedCallback callback)
    {
        if (ctx == null)
            return;

        arrayAdapter.clear();

        bluetoothAdapter.startDiscovery();
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(ctx);
        builderSingle.setTitle("Searching for devices...");

        builderSingle.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialogInterface)
            {
                bluetoothAdapter.cancelDiscovery();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                DiscoveredDevice dd = arrayAdapter.getItem(which);

                if (dd != null)
                {
                    BluetoothDevice device = dd.getBluetoothDevice();

                    if (callback != null && device != null)
                        callback.onDeviceSelected(device);
                }


                dialog.dismiss();
            }
        });
        builderSingle.show();
    }
}
