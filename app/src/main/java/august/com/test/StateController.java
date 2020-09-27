package august.com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

enum State {
    DISCONNECT,
    CONNECTING,
    CONNECTED,
    AIR,
    SMOKE
};

abstract class StateEventListener {
    /* this function is triggered when data changed */
    void onUpdate() { /* empty code block*/ }

    /* this function is triggered when watch dog check failed */
    void onFailure() { /* empty code block*/ }

    /* this function is triggered when pressure exceeded the limit */
    void onCheck() { /* empty code block*/ }
}

public class StateController {
    private static StateController controller;

    protected View view;

    /**
     * 接收到的字符串将被分割为以下多个
     */
    String times = "";
    String pressure = "";
    /**
     * 初始情况 pressure = pressureKPa
     * 之后pressureKPa储存当单位为Kpa之后的压强值
     */
    String pressureKPa = "";
    String temperature = "";
    String flow = "";
    double flows;
    /** 通过计算得到的结果与对应字符串 */
    String LeakResult = "";
    double LeakResults;
    /** 初始英制单位 */
    String pressureUnit = "PSI";
    String temperatureUnit = "°F";
    String currentUnit = "British";
    /** 初始状态 */
    State state = State.DISCONNECT;
    /** 检测是否泄漏，只能发送一次onCheck监听事件 */
    boolean leakCheck = false;
    /** 机器是否强制被stopped */
    boolean forceStopped = false;
    /** 数据库 */
    DBHelper helper;

    Timer watchDogTimer;
    long watchdogTimestamp = -1l;

    boolean isForceStopped() {
        return forceStopped;
    }

    void insertHistory(String receiveData) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String time = dateFormat.format(timestamp);

        SQLiteDatabase db = helper.getReadableDatabase();
        SQLiteStatement statement = db.compileStatement("INSERT INTO history(time, LeakSize) VALUES(?, ?)");
        statement.bindString(1, time);
        statement.bindString(2, receiveData);
        statement.execute();
        statement.close();
    }

    void insertCalibration(String pressureValue, String leakSizeValue) {
        ContentValues values = new ContentValues();
        values.put("pressure", pressureValue);
        values.put("LeakSize", leakSizeValue);
        helper.getWritableDatabase().insert("calibration", null, values);
        updateTable();
    }

    int deleteCalibration(String pressureValue, String leakSizeValue) {
        updateTable();
        return helper.getWritableDatabase().delete("calibration", "pressure = ? and LeakSize = ?", new String[]{pressureValue, leakSizeValue});
    }

    float[] selectCalibrationByColumn(String column) {
        Cursor cursor = helper.getWritableDatabase().query("calibration", new String[]{column}, null, null, null, null, null);
        int n = cursor.getCount();
        float[] list = new float[n];
        //移动到首位
        cursor.moveToFirst();
        for (int i = 0; i < n; i++) {
            String p = cursor.getString(cursor.getColumnIndex(column));
            list[i] = Float.parseFloat(p);
            //移动到下一位
            cursor.moveToNext();
        }
        return list;
    }

    void updateDetection(float v, String type) {
        helper.putConfig(type.toLowerCase(), v);
        for (StateEventListener l : stateEventListeners)
            l.onUpdate();
    }


    public float pressureUpperBound() {
        return helper.getConfig("upper", 15.f);
    }

    public float pressureLowerBound() {
        return helper.getConfig("lower", 12.f);
    }

    public float remainTime() {
        return helper.getConfig("remain_time", 20);
    }

//    String[] selectCalibrationLeakSize() {
//        return helper.getWritableDatabase().query("calibration", new String[]{"LeakSize"}, null, null, null, null, null)
//    }

    BluetoothConnector.BluetoothDataListener listener = new BluetoothConnector.BluetoothDataListener() {
        @Override
        public void onReceived(String data) {
            updateStatus(data);
        }
    };

    private StateController() {
        BluetoothConnector connector = BluetoothConnector.get();
        connector.registerListener(listener);
    }

    static StateController get() {
        if (controller == null) {
            controller = new StateController();
        }
        return controller;
    }

    void bind(View view) {
        this.view = view;
        ActivityController activityController = ActivityController.get();
        assert activityController.activity != null : "bad activity";
        if (helper == null) {
            helper = new DBHelper(activityController.activity);
        }
    }

    boolean isWorking() {
        switch (state) {
            case CONNECTED:
            case AIR:
            case SMOKE:
                return true;
            default:
                return false;
        }
    }

    void onConnectClicked() {
        assert view != null : "view not bind";
        ActivityController activityController = ActivityController.get();
        String mAddress = activityController.getAddress();
        BluetoothConnector connector = BluetoothConnector.get();
        connector.postConnectTask(mAddress);
        final Button tOn, tOff, tConnect;
        tConnect = view.findViewById(R.id.button1);
        tOn = view.findViewById(R.id.on);
        tOff = view.findViewById(R.id.off);
        final TextView tStatus = view.findViewById(R.id.textView1);

        if (tConnect == null || tOn == null || tOff == null) {
            Log.e("error", "failed to find button");
            return;
        }

        if (state == State.DISCONNECT) {
            state = State.CONNECTING;
            tStatus.setText("Connecting");
        }
        tConnect.setEnabled(false);
    }

    void setConnectButton(String result) {
        boolean succeed = (result.compareTo("Connected") == 0);
        final Button tOn, tOff, tConnect;
        tConnect = view.findViewById(R.id.button1);
        tOn = view.findViewById(R.id.on);
        tOff = view.findViewById(R.id.off);
        final TextView tStatus = view.findViewById(R.id.textView1);
        if (tConnect == null || tOn == null || tOff == null) {
            Log.e("error", "failed to find button");
            return;
        }
        if (succeed) {
            tConnect.setEnabled(true);
            tConnect.setActivated(true);
            tOn.setEnabled(true);
            tOff.setEnabled(true);
            tConnect.setText("DISCONNECT");
            state = State.CONNECTED;
        } else {
            tConnect.setActivated(false);
            tConnect.setEnabled(true);
            tConnect.setText("CONNECT");
            tOn.setActivated(false);
            tOn.setEnabled(false);
            tOff.setEnabled(false);
            tStatus.setText("No Connection");
            state = State.DISCONNECT;
        }
        tStatus.setText(result);
    }

    void onStatusChanged(String result) {
        assert view != null : "view not bind";
        TextView tStatusLabels = view.findViewById(R.id.textView1);
        tStatusLabels.setText(result);
    }

    void setTextViewHelper(int id, String val, String def) {
        if (view == null)
            return;
        final TextView textView = view.findViewById(id);
        if (textView != null) {
            textView.setText((val == null) ? def : val);
        } else {
            Log.e("error", "failed to find button");
        }
    }

    void setPanelData(String T, String AP, String Tc, String AF, String LS, String PrUnit, String TeUnit, String Status) {
        assert view != null : "view not bind";
        setTextViewHelper(R.id.stoppedTime, T, "No Sensor");
        setTextViewHelper(R.id.press, AP, "No Sensor");
        setTextViewHelper(R.id.degree, Tc, "No Sensor");
        setTextViewHelper(R.id.flowData, AF, "No Sensor");
        setTextViewHelper(R.id.result, LS, "No Sensor");
        setTextViewHelper(R.id.PRunit, PrUnit, "PSI");
        setTextViewHelper(R.id.TEunit, TeUnit, "°F");
        setTextViewHelper(R.id.textView1, Status, "Bad Status");
    }

    void setPanelData() {
        setPanelData("0.00", "00.0", "00.0", "0.00", "0.00", "PSI", "°F", "No Connection");
    }

    void setUnitText(String PrUnit, String TeUnit) {
        setTextViewHelper(R.id.PRunit, PrUnit, "PSI");
        setTextViewHelper(R.id.TEunit, TeUnit, "°F");
    }

    void updateStatus(String ReceiveData) {
        feedWatchdog();
        String line[] = ReceiveData.split("\n");
        ReceiveData = line[0];
        System.out.println(ReceiveData);
        String pureData = ReceiveData.replaceAll("\\s", "");
        pureData = pureData.replaceAll("[A-z]{1,2}?[:]|[T]", ",");
        System.out.println(pureData);
        String item[] = pureData.split(",");
        times = item[1];
        pressure = pressureKPa = item[2];
        temperature = item[3];
        flow = item[4];
        flows = Double.parseDouble(flow);
        // 检查气压是否超标
        pressureCheck();

        // 指数函数拟合
        float[] x = selectCalibrationByColumn("pressure");
        float[] y = selectCalibrationByColumn("LeakSize");
        LeakResults = LeastSquares.estimate(x, y, Float.parseFloat(pressure));
        // 保留小数点后两位
        LeakResult = String.format("%.2f", LeakResults);
        Log.e("Data", ReceiveData);

        // 英式与公式换算
        switch (currentUnit) {
            case "British":
                String[] resultB = UnitExchange.Metric2British(pressure, temperature);
                pressureUnit = "PSI";
                temperatureUnit = " °F";
                pressure = resultB[0];
                temperature = resultB[1];
                break;
            case "Metric":
//                String[] result = UnitExchange.British2Metric(pressure, temperature);
                pressureUnit = "KPA";
                temperatureUnit = " ℃";
//                pressure = result[0];
//                temperature = result[1];
                break;
        }
        setPanelData(times, pressure, temperature, flow, LeakResult, pressureUnit, temperatureUnit, "Connected");
    }

    void resetBluetooth() {
        assert view != null : "view not bind";
        BluetoothConnector connector = BluetoothConnector.get();
        if (state == State.CONNECTED || state == State.CONNECTING) {
            Log.i("msg", "disconnect bluetooth");
            state = State.DISCONNECT;
            setConnectButton("");
            connector.reset();
        }
        setPanelData();
        setConnectButton("No Connection");
        setAir8SmokeLayout(state);
    }

    // 设置发送ON和OFF
    public void sendMsg(String Msg) {
        BluetoothConnector connector = BluetoothConnector.get();
        connector.postMessageTask(Msg);
        switch (Msg) {
            case "ON":
                switch (state) {
                    case CONNECTED:
                        state = State.AIR;
                        break;
                    case AIR:
                        state = State.SMOKE;
                        break;
                    default:
                        // alert
                        break;
                }
                leakCheck = false;
                tryStartWatchdog();
                break;
            case "OFF":
                switch (state) {
                    case SMOKE:
                        insertHistory(LeakResult);
                        state = State.CONNECTED;
                        break;
                    default:
                        state = connector.isConnected() ? State.CONNECTED : State.DISCONNECT;
                        break;
                }
                setPanelData();
                resetWatchdog();
                forceStopped = true;
                break;
        }
        setAir8SmokeLayout(state);
    }

    public void pressureCheck() {
        float p = Float.parseFloat(pressureKPa);
        if (p > pressureUpperBound() && !leakCheck) {
            for (StateEventListener l : stateEventListeners) {
                l.onCheck();
            }
            leakCheck = true;
        }
    }

    public void tryStartWatchdog() {
        forceStopped = false;
        if (isWorking() && watchDogTimer == null) {
            watchdogTimestamp = System.currentTimeMillis();
            watchDogTimer = new Timer();
            watchDogTimer.schedule(new WatchDogTask(), 0, 1200);
        } else {
            // already running
        }
    }

    public synchronized void feedWatchdog() {
        watchdogTimestamp = System.currentTimeMillis();
    }

    public void resetWatchdog() {
        if (watchDogTimer != null) {
            watchDogTimer.cancel();
            watchDogTimer = null;
        }
    }

    public class WatchDogTask extends TimerTask {
        @Override
        public void run() {
            long current = System.currentTimeMillis();
            if ((current - watchdogTimestamp) >= 1200) {
                for (StateEventListener l : stateEventListeners)
                    l.onFailure();
                cancel();
                forceStopped = true;
                resetWatchdog();
            }
            watchdogTimestamp = current;
        }
    }

    public void setAir8SmokeLayout(State state) {
        LinearLayout tAIR, tSMOKE;
        // 赋值air和smoke两个linear layout模块
        tAIR = view.findViewById(R.id.air);
        tSMOKE = view.findViewById(R.id.smoke);
        Button tOn = view.findViewById(R.id.on);
        switch (state) {
            case AIR:
                tAIR.setBackgroundColor(Color.parseColor("#327475"));
                break;
            case SMOKE:
                tAIR.setBackgroundColor(Color.parseColor("#327475"));
                tSMOKE.setBackgroundColor(Color.parseColor("#327475"));
                tOn.setActivated(true);
                break;
            default:
                tOn.setActivated(false);
                tAIR.setBackgroundColor(Color.parseColor("#d14246"));
                tSMOKE.setBackgroundColor(Color.parseColor("#d14246"));
                break;
        }
    }

    public void setUnit(String unit) {
        currentUnit = unit;
        switch (currentUnit) {
            case "British":
                pressureUnit = "PSI";
                temperatureUnit = " °F";
                break;
            case "Metric":
                pressureUnit = "KPA";
                temperatureUnit = " ℃";
                break;
        }
        setUnitText(pressureUnit, temperatureUnit);
        updateTable();
    }

    public String getPressureKPa() {
        return pressureKPa;
    }

    public void updateTable() {
        for (StateEventListener l : stateEventListeners)
            l.onUpdate();
    }

    ArrayList<StateEventListener> stateEventListeners = new ArrayList<StateEventListener>();

    void registerStateEventListener(StateEventListener listener) {
        if (stateEventListeners.indexOf(listener) < 0) {
            stateEventListeners.add(listener);
        }
    }
}