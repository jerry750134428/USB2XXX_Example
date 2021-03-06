package com.usbxyz.usb2xxxdemo.USB2LIN;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.toomoss.USB2XXX.USB_Device;
import com.usbxyz.usb2xxxdemo.R;

import java.util.HashMap;

public class LINMasterActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION ="com.usbxyz.USB_PERMISSION";
    //导入库，必须要
    static{
        System.loadLibrary("jnidispatch");
        System.loadLibrary("USB2XXX");
        System.loadLibrary("usb");
    }
    private UsbManager usbManager;
    private UsbDevice[] usbDevice=new UsbDevice[20];
    int DevNum=0;
    private PendingIntent pendingIntent;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linmaster);
        USBInit(this);
        textView = findViewById(R.id.textView);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText(null);
                int ret;
                int DevHandle = 0;
                int[] PinValue = new int[1];
                boolean state;
                int[] DevHandleArry = new int[20];
                if(DevNum <= 0){
                    textView.append("Please connect device\n");
                    return;
                }
                //扫描设备
                int[] pFd = new int[20];
                for(int i=0;i<DevNum;i++){
                    UsbDeviceConnection mUsbDeviceConnection = usbManager.openDevice(usbDevice[i]);
                    pFd[i] = mUsbDeviceConnection.getFileDescriptor();
                    System.out.println("fd = "+pFd[i]);
                }
                ret = USB_Device.INSTANCE.USB_ScanDevice(DevHandleArry,pFd,DevNum);
                if(ret > 0){
                    textView.append("DeviceNum = "+ret+"\n");
                    for(int i=0;i<ret;i++) {
                        textView.append("DevHandle = " + String.format("0x%08X", DevHandleArry[i]) + "\n");
                    }
                }else{
                    textView.append("No device "+String.format("%d\n",ret));
                    return;
                }
                DevHandle = DevHandleArry[0];

                //打开设备
                state = USB_Device.INSTANCE.USB_OpenDevice(DevHandle);
                if(!state){
                    textView.append("Open device error");
                    return;
                }else{
                    textView.append("Open device success\n");
                }
                //获取设备信息
                try {
                    USB_Device.DEVICE_INFO DevInfo = new USB_Device.DEVICE_INFO();
                    byte[] funcStr = new byte[128];
                    state = USB_Device.INSTANCE.DEV_GetDeviceInfo(DevHandle,DevInfo,funcStr);
                    if(!state){
                        USB_Device.INSTANCE.USB_CloseDevice(DevHandle);
                        textView.append("Get device infomation error");
                        return;
                    }else{
                        textView.append("Firmware Info:\n");
                        textView.append("--Name:" + new String(DevInfo.FirmwareName, "UTF-8")+"\n");
                        textView.append("--Build Date:" + new String(DevInfo.BuildDate, "UTF-8")+"\n");
                        textView.append(String.format("--Firmware Version:v%d.%d.%d\n", (DevInfo.FirmwareVersion >> 24) & 0xFF, (DevInfo.FirmwareVersion >> 16) & 0xFF, DevInfo.FirmwareVersion & 0xFFFF));
                        textView.append(String.format("--Hardware Version:v%d.%d.%d\n", (DevInfo.HardwareVersion >> 24) & 0xFF, (DevInfo.HardwareVersion >> 16) & 0xFF, DevInfo.HardwareVersion & 0xFFFF));
                        textView.append("--Functions:" + new String(funcStr, "UTF-8")+"\n");
                    }
                } catch (Exception ep) {
                    ep.printStackTrace();
                }

                //关闭设备
                USB_Device.INSTANCE.USB_CloseDevice(DevHandle);
            }
        });
    }
    private void USBInit(Context context){
        usbManager = (UsbManager) context.getSystemService(context.USB_SERVICE);
        //注册USB插拔检测服务
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.setPriority(Integer.MAX_VALUE); //设置级别
        UsbDeviceDetachedBroadcastReceiver usbStateReceiver = new UsbDeviceDetachedBroadcastReceiver();
        context.registerReceiver(usbStateReceiver,filter);
        //扫描已经连接的USB设备
        DevNum = 0;
        HashMap<String, UsbDevice> map = usbManager.getDeviceList();
        for(UsbDevice device : map.values()){
            if(USB_Device.DEV_VID == device.getVendorId() && USB_Device.DEV_PID == device.getProductId()){
                usbDevice[DevNum] = device;
                if(!usbManager.hasPermission(device)) {
                    pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(usbDevice[DevNum], pendingIntent);
                }
                DevNum++;
            }
        }
    }

    //定义USB插拔广播接收器
    class UsbDeviceDetachedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Toast.makeText(context, "Device Detached", Toast.LENGTH_SHORT).show();
            }else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
                Toast.makeText(context, "Device Attached", Toast.LENGTH_SHORT).show();
            }
            DevNum = 0;
            HashMap<String, UsbDevice> map = usbManager.getDeviceList();
            for(UsbDevice device : map.values()){
                if(USB_Device.DEV_VID == device.getVendorId() && USB_Device.DEV_PID == device.getProductId()){
                    usbDevice[DevNum] = device;
                    if(!usbManager.hasPermission(device)) {
                        pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                        usbManager.requestPermission(usbDevice[DevNum], pendingIntent);
                    }
                    DevNum++;
                }
            }
        }
    }
}
