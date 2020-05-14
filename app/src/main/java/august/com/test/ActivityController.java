package august.com.test;

import android.util.Log;
import java.util.ArrayList;

public class ActivityController {
    static ActivityController controller;

    private ActivityController() {
        BluetoothConnector connector = BluetoothConnector.get();
        connector.init();
    }

    protected MainActivity activity;
    String address = "";

    static ActivityController get() {
        if (controller == null) {
            controller = new ActivityController();
        }
        return controller;
    }


    void bind(MainActivity activity) {
        this.activity = activity;
    }

    void onFragmentChange(int i) {
        for (FragmentChangeListener fl : fragmentChangeListeners) {
            fl.updateFragment(i);
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        Log.i("address: ", address);
    }

    interface FragmentChangeListener {
        void updateFragment(int i);
    }

    ArrayList<FragmentChangeListener> fragmentChangeListeners = new ArrayList<FragmentChangeListener>();

    void registerFragmentChangeListener(FragmentChangeListener listener) {
        if (fragmentChangeListeners.indexOf(listener) < 0) {
            fragmentChangeListeners.add(listener);
        }
    }
}
