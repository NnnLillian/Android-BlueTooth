package august.com.test;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class settingFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_mian, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // 页面切换监听器
        ActivityController.FragmentChangeListener listener = new ActivityController.FragmentChangeListener() {
            @Override
            public void updateFragment(int i) {
                updateUnit();
            }
        };
        ActivityController controller = ActivityController.get();
        controller.registerFragmentChangeListener(listener);

        final MainActivity mainActivity = (MainActivity) getActivity();
        final Button btnExchangeUnit = getView().findViewById(R.id.exchangeUnit);
        btnExchangeUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.onAlertExchangeDialog();
            }
        });

        // 数据变化监听器
        StateController.SettingFragmentChangeListener settingFragmentChangeListener = new StateController.SettingFragmentChangeListener() {
            @Override
            public void update() {
                updateUnit();
            }
        };
        StateController stateController = StateController.get();
        stateController.registerSettingFragmentChangeListener(settingFragmentChangeListener);

    }

    void updateUnit() {
        TextView tvUnit = getView().findViewById(R.id.currentUnit);
        StateController stateController = StateController.get();
        tvUnit.setText(stateController.currentUnit);
    }
}
