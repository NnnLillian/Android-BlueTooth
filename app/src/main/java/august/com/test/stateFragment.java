package august.com.test;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class stateFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.state_main, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        StateController controller = StateController.get();
        controller.bind(getView());
        final Button btnConnect, btnOn, btnOff;
        btnConnect = getView().findViewById(R.id.button1);
        assert btnConnect != null : "failed to find connect btn";
        switch (controller.state) {
            case CONNECTED:
                controller.setConnectButton("Connected");
                break;
            case AIR:
                controller.setConnectButton("Connected");
                controller.state = State.AIR;
                controller.setAir8SmokeLayout(controller.state);
                break;
            case SMOKE:
                controller.setConnectButton("Connected");
                controller.state = State.SMOKE;
                controller.setAir8SmokeLayout(controller.state);
                break;
            default:
                break;
        }
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StateController stateController = StateController.get();
                stateController.onConnectClicked();
                if (stateController.state == State.CONNECTED) {
                    stateController.resetBluetooth();
                }
            }
        });
        btnOn = getView().findViewById(R.id.on);
        assert btnOn != null : "failed to find on btn";
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StateController stateController = StateController.get();
                stateController.sendMsg("ON");
            }
        });
        btnOff = getView().findViewById(R.id.off);
        assert btnOff != null : "failed to find off btn";
        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StateController stateController = StateController.get();
                stateController.sendMsg("OFF");
            }
        });

    }
}