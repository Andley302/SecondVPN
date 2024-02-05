package app.one.secondvpnlite.tunnel;

import android.content.*;
import android.net.*;
import android.os.Build;
import android.util.*;



import java.io.*;
import java.util.*;
import java.util.regex.*;

import java.net.*;

import app.one.secondvpnlite.logs.AppLogManager;

public class TunnelUtils
{
	public static Map<String, CharSequence> BBCODES_LIST;

	public static String formatCustomPayload(String hostname, int port, String payload) {
		BBCODES_LIST = new ArrayMap<>();

		BBCODES_LIST.put("[method]", "CONNECT");
		BBCODES_LIST.put("[host]", hostname);
		BBCODES_LIST.put("[ip]", hostname);
		BBCODES_LIST.put("[protocol]", "HTTP/1.0");
		BBCODES_LIST.put("[port]", Integer.toString(port));

		BBCODES_LIST.put("[host_port]", String.format("%s:%d", hostname, port));
		BBCODES_LIST.put("[ssh]", String.format("%s:%d", hostname, port));
		BBCODES_LIST.put("[vpn]", String.format("%s:%d", hostname, port));


		BBCODES_LIST.put("[crlf]", "\r\n");
		BBCODES_LIST.put("[cr]", "\r");
		BBCODES_LIST.put("[lf]", "\n");
		BBCODES_LIST.put("[lfcr]", "\n\r");
		BBCODES_LIST.put("\\n", "\n");
		BBCODES_LIST.put("\\r", "\r");

		String ua = System.getProperty("http.agent");
		BBCODES_LIST.put("[ua]", ua == null ? "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36" : ua);

		if (!payload.isEmpty()) {
			for (String key : BBCODES_LIST.keySet()) {
				key = key.toLowerCase();
				payload = payload.replace(key, BBCODES_LIST.get(key));
			}
			payload = parseRandom(parseRotate(payload));

		}
		return d(payload);
	}

	public static boolean isNetworkOnline(Context context) {
		ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();

		return (networkInfo != null && networkInfo.isConnectedOrConnecting());
	}



	public static String getLocalIpAddress() {
		String ip = null;
		try{

			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
						String sAddr = inetAddress.getHostAddress();
						ip = sAddr;
						return sAddr.toString();
					}
				}
			}

			Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
			while( n.hasMoreElements()){
				NetworkInterface e = n.nextElement();
				Enumeration<InetAddress> a = e.getInetAddresses();
				while( a.hasMoreElements()){
					InetAddress addr = a.nextElement();
					String add = addr.getHostAddress().toString();
					if( add.length() < 17 ){
						System.out.println("IPv4 Address: " + add);
						//return add;
					}else{
						if (ip == null){
							ip = add;
						}
						//return add;
					}
				}


			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ip;
		/*try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
						String sAddr = inetAddress.getHostAddress();

						return sAddr.toString();
					}
				}
			}
		} catch (SocketException ex) {
			return "ERROR Obtaining IP";
		}
		return "No IP Available";*/
	}

	private static String d(String str) {
		String str2 = str;
		String str3 = str2;
		if (str2.contains("[cr*")) {
			str3 = a(str2, "[cr*", "\r");
		}
		String str4 = str3;
		if (str3.contains("[lf*")) {
			str4 = a(str3, "[lf*", "\n");
		}
		str2 = str4;
		if (str4.contains("[crlf*")) {
			str2 = a(str4, "[crlf*", "\r\n");
		}
		String str5 = str2;
		if (str2.contains("[lfcr*")) {
			str5 = a(str2, "[lfcr*", "\n\r");
		}
		return str5;
	}

	private static String a(String str, String str2, String str3) {
		while (str.contains(str2)) {
			Matcher matcher = Pattern.compile("\\[.*?\\*(.*?[0-9])\\]").matcher(str);
			if (matcher.find()) {
				int intValue = Integer.valueOf(matcher.group(1)).intValue();
				String str7 = "";
				for (int i = 0; i < intValue; i++) {
					str7 = new StringBuffer().append(str7).append(str3).toString();
				}
				String str8 = str;
				str = str8.replace(new StringBuffer().append(str2).append(String.valueOf(intValue)).append("]").toString(), str7);
			}
		}
		return str;
	}



	public static boolean injectSplitPayload(String requestPayload, OutputStream out) throws IOException {

		if (requestPayload.contains("[delay_split]")) {
			String[] split = requestPayload.split(Pattern.quote("[delay_split]"));

			for (int n = 0; n < split.length; n++) {
				String str = split[n];

				if (!injectSimpleSplit(str, out)) {
					try {
						out.write(str.getBytes("ISO-8859-1"));
					} catch (UnsupportedEncodingException e2) {
						out.write(str.getBytes());
					}
					out.flush();
				}

				try {
					if (n != (split.length-1))
						Thread.sleep(1000);
				} catch(InterruptedException e) {}
			}

			return true;
		}
		else if (injectSimpleSplit(requestPayload, out)) {
			return true;
		}

		return false;
	}


	private static boolean injectSimpleSplit(String requestPayload, OutputStream out) throws IOException {

		String[] split3;
		int i2;

		if (requestPayload.contains("[split]")) {
			split3 = requestPayload.split(Pattern.quote("[split]"));
			for (i2 = 0; i2 < split3.length; i2++)
			{
				try {
					out.write(split3[i2].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split3[i2].getBytes());
				}

				out.flush();

			}
		} else if (requestPayload.contains("[splitNoDelay]")) {
			split3 = requestPayload.split(Pattern.quote("[splitNoDelay]"));
			for (i2 = 0; i2 < split3.length; i2++) {
				try {
					out.write(split3[i2].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split3[i2].getBytes());
				}
				out.flush();
			}
		} else if (requestPayload.contains("[instant_split]")) {
			split3 = requestPayload.split(Pattern.quote("[instant_split]"));
			for (i2 = 0; i2 < split3.length; i2++) {
				try {
					out.write(split3[i2].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split3[i2].getBytes());
				}

				out.flush();
			}
		} else if (requestPayload.contains("[delay]")) {
			split3 = requestPayload.split(Pattern.quote("[delay]"));
			for (i2 = 0; i2 < split3.length; i2++) {
				try {
					out.write(split3[i2].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split3[i2].getBytes());
				}

				out.flush();
				try {
					if (i2 != (split3.length-1))
						Thread.sleep(1000);
				} catch(InterruptedException e) {}
			}

		} else if (requestPayload.contains("[split_delay]")) {
			split3 = requestPayload.split(Pattern.quote("[split_delay]"));
			for (i2 = 0; i2 < split3.length; i2++) {
				try {
					out.write(split3[i2].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split3[i2].getBytes());
				}

				out.flush();
				try {
					if (i2 != (split3.length-1))
						Thread.sleep(1000);
				} catch(InterruptedException e) {}
			}

			return true;
		}

		return false;
	}






	private static Map<Integer,Integer> lastRotateList = new ArrayMap<>();
	private static String lastPayload = "";

	public static String parseRotate(String payload) {
		Matcher match = Pattern.compile("\\[rotate=(.*?)\\]")
				.matcher(payload);

		if (!lastPayload.equals(payload)) {
			restartRotateAndRandom();
			lastPayload = payload;
		}



		int i = 0;
		while (match.find()) {
			String group = match.group(1);

			String[] split = group.split(";");
			if (split.length <= 0) continue;

			int split_key;
			if (lastRotateList.containsKey(i)) {
				split_key = lastRotateList.get(i)+1;
				if (split_key >= split.length) {
					split_key = 0;
				}
			}
			else  {
				split_key = 0;
			}

			String host = split[split_key];

			payload = payload.replace(match.group(0), host);

			lastRotateList.put(i, split_key);

			i++;
		}

		return payload;
	}


	public static String parseRandom(String payload) {
		Matcher match = Pattern.compile("\\[random=(.*?)\\]")
				.matcher(payload);


		int i = 0;
		while (match.find()) {
			String group = match.group(1);

			String[] split = group.split(";");
			if (split.length <= 0) continue;

			Random r = new Random();
			int split_key = r.nextInt(split.length);

			if (split_key >= split.length || split_key < 0) {
				split_key = 0;
			}

			String host = split[split_key];

			payload = payload.replace(match.group(0), host);

			i++;
		}

		return payload;
	}

	public static void restartRotateAndRandom() {
		lastRotateList.clear();

	}
	public static boolean isActiveVpn(Context mContext) {
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			Network network = cm.getActiveNetwork();
			NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);

			return (capabilities!= null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
		}
		else {
			NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_VPN);

			return (info != null && info.isConnectedOrConnecting());
		}
	}

	private static Socket output = null;

	public static void setSocket(Socket socks) {
		output = socks;
	}

	public static boolean protect(VpnService vpnService)
	{
		if (output == null)
		{
			addLog("Vpn Protect Socket is null");
			return false;
		}
		else if (output.isClosed())
		{
			addLog("Vpn Protect Socket is closed");
			return false;
		}
		else if (!output.isConnected())
		{
			addLog("Vpn Protect Socket not connected");
			return false;
		}
		else if (vpnService.protect(output))
		{
			addLog("Vpn Protect Socket has protected");
			return true;
		}
		else
		{
			addLog("Vpn Protect Failed to protecting socket, reboot this device required");
			return false;
		}
	}

	private static void addLog(String msg){
		AppLogManager.addLog(msg);
	}
}

