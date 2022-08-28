/***********************************************************************
 * Copyright (c) 2017 Milan Jaitner                                   *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package bt.doorcontrol.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import bt.doorcontrol.utils.Prefs;

public class PairedDevice
{
    private static final String PAIRED_DEVICE_NAME = "pd_name";
    private static final String PAIRED_DEVICE_MAC = "pd_mac";

    private String name;
    private String mac;

    public static PairedDevice create(Context ctx)
    {
        String name = Prefs.getString(ctx, PAIRED_DEVICE_NAME, null);
        String mac = Prefs.getString(ctx, PAIRED_DEVICE_MAC, null);

        if (name == null || mac == null)
            return null;

        return new PairedDevice(name, mac);
    }

    public PairedDevice(BluetoothDevice device)
    {
        this(device.getName(), device.getAddress());
    }

    public PairedDevice(String name, String mac)
    {
        this.name = name;
        this.mac = mac;
    }

    //

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getMac()
    {
        return mac;
    }

    public void setMac(String mac)
    {
        this.mac = mac;
    }

    //

    public void save(Context ctx)
    {
        Prefs.setString(ctx, PAIRED_DEVICE_NAME, name);
        Prefs.setString(ctx, PAIRED_DEVICE_MAC, mac);
    }
}
