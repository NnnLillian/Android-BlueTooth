package august.com.test;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothReceiver extends BroadcastReceiver{

//    String pin = "1234";//为连接设备的初始密码
    public BluetoothReceiver(){

    }

    @Override
    public void onReceive(Context context, Intent intent) {
    // 广播接收器，当远程蓝牙设备被发现时候，回调函数onReceiver()会被执行

        String action = intent.getAction();//得到 action
        Log.e("action1=",action);
        BluetoothDevice btDevice=null;// 创建一个蓝牙device对象
        btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);// 从Intent中获取设备对象

        if (BluetoothDevice.ACTION_FOUND.equals(action)) { // 发现设备
            Log.e("发现设备：", "[" + btDevice.getName() + "]" + ":" + btDevice.getAddress());
            if (btDevice.getName()!=null) {
                if (btDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.e("ywq","attemp to bond:"+"["+ btDevice.getName() + "]");
                    try {
                        //通过工具类ClsUtils调用createBond方法
                        ClsUtils.creatBond(btDevice.getClass(), btDevice);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }else
                Log.e("error", "Is faild");
        }else if (action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) {//再次得到的action，会等于PAIRING_REQUEST
            Log.e("action2=", action);
            if(btDevice.getName().contains("HC-02"))
            {
                Log.e("here", "OK");

                try {

                    //1.确认配对
                    ClsUtils.setPairingConfirmation(btDevice.getClass(), btDevice, true);
                    //2.终止有序广播
                    Log.i("order...", "isOrderedBroadcast:"+isOrderedBroadcast()+",isInitialStickyBroadcast:"+isInitialStickyBroadcast());
                    abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                    //3.调用setPin方法进行配对...
//                    boolean ret = ClsUtils.setPin(btDevice.getClass(), btDevice, pin);

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else
                Log.e("提示信息", "这个设备不是目标蓝牙设备");

        }
    }

}
