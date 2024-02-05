package app.one.secondvpnlite.tunnel;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class IpAddress
{
    private String privateAddress = null;

    public IpAddress() 
	{
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("10.0.0.1");
        arrayList.add("172.16.0.1");
        arrayList.add("192.168.0.1");
        arrayList.add("169.254.1.1");
        try {
            for (NetworkInterface inetAddresses : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                Iterator it = Collections.list(inetAddresses.getInetAddresses()).iterator();
                while (it.hasNext()) {
                    InetAddress inetAddress = (InetAddress) it.next();
                    if (inetAddress instanceof Inet4Address) {
                        String hostAddress = inetAddress.getHostAddress();
                        if (hostAddress.startsWith("10.")) {
                            arrayList.remove("10.0.0.1");
                        } else if (hostAddress.length() >= 6 && hostAddress.substring(0, 6).compareTo("172.16") >= 0 && hostAddress.substring(0, 6).compareTo("172.31") <= 0) {
                            arrayList.remove("172.16.0.1");
                        } else if (hostAddress.startsWith("192.168")) {
                            arrayList.remove("192.168.0.1");
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        if (arrayList.size() > 0) {
            this.privateAddress = arrayList.get(0);
        } else {
            this.privateAddress = "172.16.0.1";
        }
    }

    public String getPrivateAddress() {
        return this.privateAddress;
    }

    public int getPrefixLength() {
        return this.privateAddress.compareTo("10.0.0.1") == 0 ? 8 : this.privateAddress.compareTo("172.16.0.1") == 0 ? 12 : this.privateAddress.compareTo("192.168.0.1") == 0 ? 16 : this.privateAddress.compareTo("169.254.1.1") == 0 ? 24 : 0;
    }

    public String getPrivateAddressRouter() {
        return this.privateAddress.compareTo("10.0.0.1") == 0 ? "10.0.0.2" : this.privateAddress.compareTo("172.16.0.1") == 0 ? "172.16.0.2" : this.privateAddress.compareTo("192.168.0.1") == 0 ? "192.168.0.2" : this.privateAddress.compareTo("169.254.1.1") == 0 ? "169.254.1.2" : null;
    }
}
