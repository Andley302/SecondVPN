package app.one.secondvpnlite.injector;

import android.content.Context;


import com.trilead.ssh2.HTTPProxyException;
import com.trilead.ssh2.ProxyData;
import com.trilead.ssh2.crypto.Base64;
import com.trilead.ssh2.sftp.Packet;
import com.trilead.ssh2.transport.ClientServerHello;
import com.trilead.ssh2.transport.TransportManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import app.one.secondvpnlite.logs.AppLogManager;
import app.one.secondvpnlite.tunnel.TunnelUtils;
import app.one.secondvpnlite.*;

/**
 * By Skank3r
 */
public class HttpProxyCustom
		implements ProxyData
{

	private final String proxyHost;
	private final String proxyPass;
	private final int proxyPort;
	private final String proxyUser;
	private final String requestPayload;
	private final boolean modoDropbear;
	private Socket sock;
	private final Context mContext;


	public HttpProxyCustom(String proxyHost, int proxyPort, String proxyUser, String proxyPass, Context context) {
		this(proxyHost, proxyPort, proxyUser, proxyPass, null, false, context);
	}

	public HttpProxyCustom(String proxyHost, int proxyPort, String proxyUser, String proxyPass, String requestPayload, boolean modoDropbear, Context context) {
		if (proxyHost == null) {
			throw new IllegalArgumentException("proxyHost must be non-null");
		} else if (proxyPort < 0) {
			throw new IllegalArgumentException("proxyPort must be non-negative");
		} else {
			this.proxyHost = proxyHost;
			this.proxyPort = proxyPort;
			this.proxyUser = proxyUser;
			this.proxyPass = proxyPass;
			this.requestPayload = requestPayload;
			this.modoDropbear = modoDropbear;
			this.mContext = context;
		}
	}

	@Override
	public Socket openConnection(String hostname, int port, int connectTimeout, int readTimeout) throws IOException {
		sock = new Socket();

		InetAddress addr = TransportManager.createInetAddress(this.proxyHost);
		sock.connect(new InetSocketAddress(addr, this.proxyPort), connectTimeout);
		sock.setSoTimeout(readTimeout);


		//AppLogManager.addLog(R.string.state_proxy_running);

		String requestPayload = getRequestPayload(hostname, port);

		// anti vpn sniffer
		/*if (TunnelUtils.isActiveVpn(mContext)) {
			AppLogManager.addLog("<strong>" + mContext.getString(R.string.error_vpn_sniffer_detected) + "</strong>");

			throw new IOException("error detected");
		}*/


		AppLogManager.addLog(R.string.state_proxy_inject);

		OutputStream out = sock.getOutputStream();

		// suporte a [split] na payload
		if (!TunnelUtils.injectSplitPayload(requestPayload, out)) {
			try {
				out.write(requestPayload.getBytes("ISO-8859-1"));
			} catch (UnsupportedEncodingException e2) {
				out.write(requestPayload.getBytes());
			}
			out.flush();
		}

		// suporta Dropbear (SSH + PAYLOAD)
		if (modoDropbear) {
			return sock;
		}

		byte[] buffer = new byte[1024];
		InputStream in = sock.getInputStream();

		// lê primeira linha
		int len = ClientServerHello.readLineRN(in, buffer);

		String httpReponseFirstLine;
		try {
			httpReponseFirstLine = new String(buffer, 0, len, "ISO-8859-1");
		} catch (UnsupportedEncodingException e3) {
			httpReponseFirstLine = new String(buffer, 0, len);
		}

		AppLogManager.addLog("<strong>" + httpReponseFirstLine + "</strong>");

		String str2 = httpReponseFirstLine;

		String httpReponseAll = httpReponseFirstLine;
		String[] split = httpReponseAll.split("\r\n");

		int parseInt = Integer.parseInt(str2.substring(9, 12));
		if (parseInt == 200) {
			return this.sock;
		}else if (parseInt != 200){
			//AppLogManager.addLog("Proxy: Auto Replace Header");
			//out.write(new StringBuilder(String.valueOf(split[0].split(" ")[0])).append(" 200 OK\r\n\r\n").toString().getBytes());
			//AppLogManager.addLog("<b>HTTP/1.1 200 Connection established</b>");
			return this.sock;
		}
		/*if (parseInt == 200) {
			return this.sock;
		}else if (parseInt == 101) {
			AppLogManager.addLog("Proxy: Auto Replace Header");
			AppLogManager.addLog("<b>Status: 200 (Connection established) Successful</b>");
			AppLogManager.addLog("<b>HTTP/1.1 200 Connection established</b>");
			return this.sock;
		}else if (parseInt == 521) {
			AppLogManager.addLog(context.getString(R.string.http_521));
			return this.sock;
		}else {
			AppLogManager.addLog("Sending HTTP/1.1 200 OK ...");
			return this.sock;
		}*/



		// lê o restante
		//String httpReponseAll = httpReponseFirstLine;
		while ((len = ClientServerHello.readLineRN(in, buffer)) != 0) {
			httpReponseAll += "\n";
			try {
				httpReponseAll += new String(buffer, 0, len, "ISO-8859-1");
			} catch (UnsupportedEncodingException e3) {
				httpReponseAll += new String(buffer, 0, len);
			}
		}



		if (!httpReponseAll.isEmpty())
			////AppLogManager.logDebug(httpReponseAll);

		if (!httpReponseFirstLine.startsWith("HTTP/")) {
			throw new IOException("The proxy did not send back a valid HTTP response.");
		} else if (httpReponseFirstLine.length() >= 14 && httpReponseFirstLine.charAt(8) == ' ' && httpReponseFirstLine.charAt(12) == ' ') {
			try {
				int errorCode = Integer.parseInt(httpReponseFirstLine.substring(9, 12));
				if (errorCode < 0 || errorCode > 999) {
					throw new IOException("The proxy did not send back a valid HTTP response.");
				} else if (errorCode != Packet.SSH_FXP_EXTENDED) {
					throw new HTTPProxyException(httpReponseFirstLine.substring(13), errorCode);
				} else {
					return sock;
				}
			} catch (NumberFormatException e4) {
				throw new IOException("The proxy did not send back a valid HTTP response.");
			}
		} else if (parseInt < 0 || len > 999) {
			throw new IOException("The proxy did not send back a valid HTTP response.");
		}
		else {
			return this.sock;
		}

		//qualquer coisa remove isso daqui,se bem que nem vai usar essa classe
		return this.sock;
	}

	private String getRequestPayload(String hostname, int port) {
		String payload = this.requestPayload;

		if (payload != null) {
			payload = TunnelUtils.formatCustomPayload(hostname, port, payload);
		}
		else {
			StringBuffer sb = new StringBuffer();

			sb.append("CONNECT ");
			sb.append(hostname);
			sb.append(':');
			sb.append(port);
			sb.append(" HTTP/1.0\r\n");
			if (!(this.proxyUser == null || this.proxyPass == null)) {
				char[] encoded;
				String credentials = this.proxyUser + ":" + this.proxyPass;
				try {
					encoded = Base64.encode(credentials.getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e) {
					encoded = Base64.encode(credentials.getBytes());
				}
				sb.append("Proxy-Authorization: Basic ");
				sb.append(encoded);
				sb.append("\r\n");
			}
			sb.append("\r\n");

			payload = sb.toString();
		}

		return payload;
	}

	@Override
	public void close()
	{
		if (sock == null) return;

		try {
			sock.close();
		} catch (IOException e) { /* failed */ }
	}

}
