package august.com.test;

public class Guze {
    private double pressure;
    private double tempretures;
    private double flows;

    private Guze()
    {}

    public Guze(double pressure, double tempretures, double flows) {
        this.pressure = pressure;
        this.tempretures = tempretures;
        this.flows = flows;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public double getTempretures() {
        return tempretures;
    }

    public void setTempretures(double tempretures) {
        this.tempretures = tempretures;
    }

    public double getFlows() {
        return flows;
    }

    public void setFlows(double flows) {
        this.flows = flows;
    }
}
