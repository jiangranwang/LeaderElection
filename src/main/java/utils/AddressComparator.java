package utils;

import network.Address;

import java.util.Comparator;

public class AddressComparator<T extends Comparable<T>> implements Comparator<Address> {
    @Override
    public int compare(Address a, Address b) {
        if (a.getId() == b.getId()) return 0;
        return a.getId() > b.getId() ? 1 : -1;
    }

    public static Address getMin(Address a, Address b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.getId() < b.getId() ? a : b;
    }
}