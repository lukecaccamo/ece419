package shared.hashring;

import java.util.*; 
import java.math.*;

public class HashComparator implements Comparator<String> {
    @Override
    public int compare(String s1, String s2) {
        BigInteger c1 = new BigInteger(s1, 32);
        BigInteger c2 = new BigInteger(s2, 32);
        return c1 == c2 ? 0 : c1.subtract(c2).intValue();
    }
}