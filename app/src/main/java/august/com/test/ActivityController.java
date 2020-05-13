package august.com.test;

import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.ViewPager;
import android.util.Log;

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
        BottomNavigationView navigation = activity.findViewById(R.id.navigation_launch);
        navigation.getMenu().getItem(i).setChecked(true);
        ViewPager viewPager = activity.findViewById(R.id.viewpager_launch);
        viewPager.setCurrentItem(i);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        Log.i("address: ", address);
    }
}
