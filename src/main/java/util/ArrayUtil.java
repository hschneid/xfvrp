package util;

public class ArrayUtil {

    public static void add(float[] a, float[] b, float[] result) {
        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = a[i] + b[i];
        }
    }
}
