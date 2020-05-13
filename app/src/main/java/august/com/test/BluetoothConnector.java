package august.com.test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnector {
    private static BluetoothConnector bluetoothConnector = new BluetoothConnector();
    private final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private ConnectTask task; // 蓝牙连接任务
    private ReceiveThread rThread = null;  // 数据接收线程
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private String ReceiveData = "";

    public static BluetoothConnector get() {
        return bluetoothConnector;
    }

    boolean isAvailable() {
        return mBluetoothAdapter != null;
    }

    public BluetoothConnector() {
    }

    public void init() {
        //得到一个蓝牙适配器
        if (!isAvailable()) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        mBluetoothAdapter.startDiscovery();
    }

    public ArrayList<Pair<String, String>> getDevicesList() {
        ArrayList<Pair<String, String>> deviceList = new ArrayList<Pair<String, String>>();
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice dev : devices) {
            deviceList.add(new Pair<String, String>(dev.getName(), dev.getAddress()));
            Log.e("设备：", "[" + dev.getName() + "]" + ":" + dev.getAddress());
        }
        return deviceList;
    }

    boolean connect(String address) {
        try {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            assert btSocket != null : "bSocket !- null assert failed";
            if (btSocket.isConnected()) {
                btSocket.close();
            }
            btSocket.connect();
            mBluetoothAdapter.cancelDiscovery();
            Log.e("message", "ON RESUME: BT connection established, data transfer link open.");
            outStream = btSocket.getOutputStream();
            inStream = btSocket.getInputStream();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    boolean isConnected() {
        return btSocket != null && btSocket.isConnected();
    }

    void postConnectTask(String address) {
        assert address != null : "address != null assert failed";
        task = new ConnectTask();
        task.execute(address);
    }

    void postMessageTask(String on) {
        new BluetoothConnector.SendInfoTask().execute(on);
    }

    public void close() throws IOException, InterruptedException {
        if (btSocket != null) {
            btSocket.close();
            btSocket = null;
        }
        if (rThread != null) {
            rThread.join();
            rThread = null;
        }
    }

    public void reset() {
        task.cancel(true);
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getReceiveData() {
        return ReceiveData;
    }

    public synchronized void setReceiveData(String ReceiveData) {
        Log.i("ReceiveData: ", ReceiveData);
    }

    interface BluetoothDataListener {
        void onReceived(String data);
    }

    void postDataNotifier(String data) {
        new BluetoothNotifyTask().execute(data);
    }

    ArrayList<BluetoothDataListener> listeners;

    void registerListener(BluetoothDataListener listener) {
        if (listeners.indexOf(listener) < 0) {
            listeners.add(listener);
        }
    }

    class BluetoothNotifyTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... param) {
            return param[0];
        }

        @Override    //这个方法是在主线程中运行的，所以可以更新界面
        public void onPostExecute(String data) {
            super.onPostExecute(data);
            for (BluetoothDataListener l : listeners) {
                l.onReceived(data);
            }
        }
    }

    class ConnectTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            BluetoothConnector connector = BluetoothConnector.get();
            boolean ok = connector.connect(params[0]);
            if (!ok) {
                try {
                    connector.close();
                    connector.connect(params[0]);
                } catch (Exception e) {
                    Log.e("error", "ON RESUME: Unable to close socket during connection failure", e);
                    e.printStackTrace();
                    return "Socket failed";
                }
            }
            return "Connected";
        }

        @Override    //这个方法是在主线程中运行的，所以可以更新界面
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //连接成功则启动监听
            rThread = new ReceiveThread();
            rThread.start();
            StateController controller = StateController.get();
            // 更改button样式
            // is_disconnect = true;
            boolean on = !(result.compareTo("Connected") == 0);
            controller.setConnectButton(result);
        }
    }

    //发送数据到蓝牙设备的异步任务
    class SendInfoTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            StateController controller = StateController.get();
            controller.onStatusChanged(result);
        }

        @Override
        protected String doInBackground(String... arg0) {
            if (btSocket == null) {
                return "No Connection";
            }

            if (arg0[0].length() > 0)//不是空白串
            {
                //String target=arg0[0];
                byte[] msgBuffer = arg0[0].getBytes();

                try {
                    //  将msgBuffer中的数据写到outStream对象中
                    outStream.write(msgBuffer);

                } catch (IOException e) {
                    Log.e("error", "ON RESUME: Exception during write.", e);
                    return "Fail";
                }
            }
            return "Success";
        }
    }

    //从蓝牙接收信息的线程
    class ReceiveThread extends Thread {
        @Override
        public void run() {
            while (btSocket != null && btSocket.isConnected()) {
                //定义一个存储空间buff
                byte[] buff = new byte[1024];
                try {
                    inStream = btSocket.getInputStream();
                    System.out.println("waitting for instream");
                    inStream.read(buff); //读取数据存储在buff数组中
//                    System.out.println("buff receive :"+buff.length);
                    processBuffer(buff, 1024);
                    //System.out.println("receive content:"+ReceiveData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void processBuffer(byte[] buff, int size) {
            int length = 0;
            for (int i = 0; i < size; i++) {
                if (buff[i] > '\0') {
                    length++;
                } else {
                    break;
                }
            }

            //			System.out.println("receive fragment size:"+length);

            byte[] newbuff = new byte[length];  //newbuff字节数组，用于存放真正接收到的数据

            if (length > 20) {
                for (int j = 0; j < length; j++) {
                    newbuff[j] = buff[j];
                }
                //            ReceiveData=ReceiveData+new String(newbuff);
                System.out.println(newbuff);
                setReceiveData(new String(newbuff));
                postDataNotifier(getReceiveData());
            }
        }
    }
}
