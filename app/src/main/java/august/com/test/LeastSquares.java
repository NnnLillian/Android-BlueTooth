package august.com.test;

/**
 * 功能：最小二乘法 线性回归（Least squares）
 * <p>
 * y = a x + b b = sum( y ) / n - a * sum( x ) / n a = ( n * sum( xy ) - sum( x
 * ) * sum( y ) ) / ( n * sum( x^2 ) - sum(x) ^ 2 )
 */
public class LeastSquares {

    //返回估计的y值
    public static float estimate(float[] x, float[] y, float input) {
        float a = getA(x, y);
        float b = getB(x, y);
        System.out.println("线性回归系数a值：\t" + a + "\n" + "线性回归系数b值：\t" + b);
        return (a * input + b);
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
        float[] x = args[0];
//                { 205, 325, 445, 505, 625 };
        float[] y = args[1];
//                { 100, 123, 148, 407, 319, 301 };
        System.out.println("经线性回归后的y值：\t" + estimate(x, y, input));
    }
}