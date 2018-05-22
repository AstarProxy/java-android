package io.github.astarProxy;

/**
 * Created by maryam on 5/19/2018.
 */

public class HostUnreachableException extends Exception {
    public AddressType addressType;
    public String address;
    public int port;
    public HostUnreachableException(AddressType addressType, String address, int port) {
        this.addressType = addressType;
        this.address = address;
        this.port = port;
    }
}
