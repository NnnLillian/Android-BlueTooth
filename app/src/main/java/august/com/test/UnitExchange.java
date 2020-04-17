package august.com.test;

public class UnitExchange {

    // psi -> kpa; f -> c(C=5/9(F-32)）
    static public String[] British2Metric(String pressure, String temperature) {
        String[] res = {"0", "0"};
        double p = Double.parseDouble(pressure) * 6.895;
        double t = 5 * (Double.parseDouble(temperature) - 32) / 9;
        res[0] = String.format("%.2f", p);
        res[1] = String.format("%.2f", t);
        return res;
    }

    // kpa -> psi; c -> f(F=9/5C+32）
    static public String[] Metric2British(String pressure, String temperature) {
        String[] res = {"0", "0"};
        double p = Double.parseDouble(pressure) / 6.895;
        double t = 9 * Double.parseDouble(temperature) / 5 + 32;
        res[0] = String.format("%.2f", p);
        res[1] = String.format("%.2f", t);
        return res;
    }
}
