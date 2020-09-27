package august.com.test;

/**
 * 功能：最小二乘法 线性回归（Least squares）
 * <p>
 * y = a * exp(bx)
 * lny = lna + bx
 * <p>
 * y = ax + b
 * b = sum( y ) / n - a * sum( x ) / n
 * a = ( n * sum( xy ) - sum( x ) * sum( y ) ) / ( n * sum( x^2 ) - sum(x) ^ 2 )
 */
public class LeastSquares {

    //返回估计的y值
    public static double estimate(float[] x, float[] y, float input) {
        for (int i = 0; i < y.length; i++) {
            y[i] = (float) Math.log(y[i]);
        }
        float aTemp = getA(x, y);
        float bTemp = getB(x, y);
        System.out.println("线性回归系数a值：\t" + aTemp + "\n" + "线性回归系数b值：\t" + bTemp);

        float a = (float) Math.exp(bTemp);
        float b = aTemp;
        System.out.println("指数函数系数a值\t" + a + "\n" + "指数函数系数b值：\t" + b);
        return (a * Math.exp(b * input));
    }

    // 返回x的系数a 公式：a = ( n sum( xy ) - sum( x ) sum( y ) ) / ( n sum( x^2 ) - sum(x) ^ 2 )
    public static float getA(float[] x, float[] y) {
        int n = x.length;
        return (float) ((n * pSum(x, y) - sum(x) * sum(y)) / (n * sqSum(x) - Math
                .pow(sum(x), 2)));
    }

    // 返回常量系数系数b 公式：b = sum( y ) / n - a sum( x ) / n
    public static float getB(float[] x, float[] y) {
        int n = x.length;
        float a = getA(x, y);
        return sum(y) / n - a * sum(x) / n;
    }

    // 求和
    private static float sum(float[] ds) {
        float s = 0;
        for (float d : ds) {
            s = s + d;
        }
        return s;
    }

    // 求平方和
    private static float sqSum(float[] ds) {
        float s = 0;
        for (float d : ds) {
            s = (float) (s + Math.pow(d, 2));
        }
        return s;
    }

    // 返回对应项相乘后的和
    private static float pSum(float[] x, float[] y) {
        float s = 0;
        for (int i = 0; i < x.length; i++) {
            s = s + x[i] * y[i];
        }
        return s;
    }

    // main()测试线性回归的最小二乘法java实现函数
    public static void main(float[][] args, float input) {
//        float[] x = args[0];
        float[] x = {4.8f, 1.8f, 1.2f, 0.4f, 0.1f};
//        float[] y = args[1];
        float[] y = {1, 2, 3, 4, 5};
        for (int i = 0; i < y.length; i++) {
            y[i] = (float) Math.log(y[i]);
            System.out.println(y[i]);
        }
        System.out.println("经线性回归后的y值：\t" + estimate(x, y, input));
    }
}