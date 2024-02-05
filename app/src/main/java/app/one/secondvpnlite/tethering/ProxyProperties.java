package app.one.secondvpnlite.tethering;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ProxyProperties implements Parcelable {

    private final String mHost;
    private final int mPort;
    private final String mExclusionList;
    private final String[] mParsedExclusionList;

    private ProxyProperties(String host, int port, String exclList, String[] parsedExclList) {
        mHost = host;
        mPort = port;
        mExclusionList = exclList;
        mParsedExclusionList = parsedExclList;
    }

    public String getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }

    // comma separated
    public String getExclusionList() {
        return mExclusionList;
    }


    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mHost != null) {
            sb.append("[");
            sb.append(mHost);
            sb.append("] ");
            sb.append(mPort);
            if (mExclusionList != null) {
                sb.append(" xl=").append(mExclusionList);
            }
        } else {
            sb.append("[ProxyProperties.mHost == null]");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProxyProperties)) return false;
        ProxyProperties p = (ProxyProperties) o;
        if (mExclusionList != null && !mExclusionList.equals(p.getExclusionList())) return false;
        if (mHost != null && p.getHost() != null && !mHost.equals(p.getHost())) {
            return false;
        }
        if (mHost != null && p.mHost == null) return false;
        if (mHost == null && p.mHost != null) return false;
        return mPort == p.mPort;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    /*
     * generate hashcode based on significant fields
     */
    public int hashCode() {
        return ((null == mHost) ? 0 : mHost.hashCode())
                + ((null == mExclusionList) ? 0 : mExclusionList.hashCode())
                + mPort;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (mHost != null) {
            dest.writeByte((byte) 1);
            dest.writeString(mHost);
            dest.writeInt(mPort);
        } else {
            dest.writeByte((byte) 0);
        }
        dest.writeString(mExclusionList);
        dest.writeStringArray(mParsedExclusionList);
    }

    public static final Creator<ProxyProperties> CREATOR =
            new Creator<ProxyProperties>() {
                public ProxyProperties createFromParcel(Parcel in) {
                    String host = null;
                    int port = 0;
                    if (in.readByte() == 1) {
                        host = in.readString();
                        port = in.readInt();
                    }
                    String exclList = in.readString();
                    //String[] parsedExclList = in.readStringArray();
                    return new ProxyProperties(host, port, exclList, null);
                }

                public ProxyProperties[] newArray(int size) {
                    return new ProxyProperties[size];
                }
            };
}