package august.com.test;

public class Calibration
{
    private String pressureValue;
    private String leakSizeValue;

    public Calibration(String pressureValue, String leakSizeValue) {
        this.pressureValue = pressureValue;
        this.leakSizeValue = leakSizeValue;
    }

    public String getPressureValue() {
        return pressureValue;
    }

    public void setPressureValue(String pressureValue) {
        this.pressureValue = pressureValue;
    }

    public String getLeakSizeValue() {
        return leakSizeValue;
    }

    public void setLeakSizeValue(String leakSizeValue) {
        this.leakSizeValue = leakSizeValue;
    }

}
