package network;

import java.io.Serializable;

public class Address implements Serializable {
    private final int id;

    public Address(int id) {
        this.id = id;
    }

    public Address(Address other) {
        this.id = other.id;
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Address other = (Address) obj;
        return id == other.id;
    }

    public boolean lt(Address other) {
        return other == null || this.id < other.id;
    }

    public boolean greaterThan(Address other) {
        if (other == null) return false;
        return this.id > other.id;
    }

    public String toString() {
        return String.valueOf(id);
    }
}
