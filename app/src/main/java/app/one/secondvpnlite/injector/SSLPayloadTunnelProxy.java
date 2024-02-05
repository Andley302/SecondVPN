package app.one.secondvpnlite.injector;

import android.content.Context;
import android.util.Log;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.HTTPProxyException;
import com.trilead.ssh2.ProxyData;
import com.trilead.ssh2.crypto.Base64;
import com.trilead.ssh2.sftp.Packet;
import com.trilead.ssh2.transport.ClientServerHello;

import org.conscrypt.Conscrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.Security;
import java.util.Arrays;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import app.one.secondvpnlite.R;
import app.one.secondvpnlite.logs.AppLogManager;
import app.one.secondvpnlite.tunnel.TunnelUtils;

public class SSLPayloadTunnelProxy implements ProxyData

{
	class HandshakeTunnelCompletedListener implements HandshakeCompletedListener {
		private final String val$host;
		private final int val$port;
		private final SSLSocket val$sslSocket;
		private Connection mConnection;





		HandshakeTunnelCompletedListener( String str, int i, SSLSocket sSLSocket) {
			this.val$host = str;
			this.val$port = i;
			this.val$sslSocket = sSLSocket;
		}

		public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
			//AppLogManager.addLog(new StringBuffer().append("<b>Established ").append(handshakeCompletedEvent.getSession().getProtocol()).append(" connection with ").append(val$host).append(":").append(this.val$port).append(" using ").append(handshakeCompletedEvent.getCipherSuite()).append("</b>").toString());
			//AppLogManager.addLog(new StringBuffer().append("<b>Established ").append(handshakeCompletedEvent.getSession().getProtocol()).append(" connection ").append("using ").append(handshakeCompletedEvent.getCipherSuite()).append("</b>").toString());
			//AppLogManager.addLog(new StringBuffer().append("Supported cipher suites: ").append(Arrays.toString(this.val$sslSocket.getSupportedCipherSuites())).toString());
			//AppLogManager.addLog(new StringBuffer().append("Enabled cipher suites: ").append(Arrays.toString(this.val$sslSocket.getEnabledCipherSuites())).toString());
			AppLogManager.addLog(new StringBuffer().append("SSL: Supported protocols: <br>").append(Arrays.toString(val$sslSocket.getSupportedProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
			AppLogManager.addLog(new StringBuffer().append("SSL: Enabled protocols: <br>").append(Arrays.toString(val$sslSocket.getEnabledProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
			AppLogManager.addLog("SSL: Using cipher " + handshakeCompletedEvent.getSession().getCipherSuite());
			AppLogManager.addLog("SSL: Using protocol " + handshakeCompletedEvent.getSession().getProtocol());
			AppLogManager.addLog("SSL: Handshake finished");
		}
	}

	private String stunnelServer;
	private int stunnelPort;
	private String stunnelHostSNI;
	private String requestPayload ;
	private String TLSVersion;
	private Context context;
	private final String proxyUser;
	private final String proxyPass;



	static {
		try {
			Security.insertProviderAt(Conscrypt.newProvider(), 1);

		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
		}}

	private Socket input;

	public static Socket mSocket;

	public SSLPayloadTunnelProxy(String server, int port, String hostSni, String requestPayload, String TLSVersion, Context context) {
		this.stunnelServer = server;
		this.stunnelPort = port;
		this.stunnelHostSNI = hostSni;
		this.proxyUser = null;
		this.proxyPass = null;
		this.requestPayload = requestPayload;
		this.TLSVersion= TLSVersion;
		this.context = context;

	}

	public void SSLSupport(Socket in)
	{
		input = in;
	}





	@Override
	public Socket openConnection(String hostname, int port, int connectTimeout, int readTimeout) throws IOException
	{
		//ver pq nao funciona
		//sendForwardSuccess(input);
		Log.d("SSL Payload","Start SSL on " + stunnelServer + ":" + stunnelPort + " with SNI " + stunnelHostSNI +" and " +requestPayload);
		//ABRE SOCKET
		mSocket = SocketChannel.open().socket();
		mSocket.connect(new InetSocketAddress(stunnelServer, stunnelPort), 15000);

		if (mSocket.isConnected()) {
			Log.d("SSL Payload","Socket is connected");

			mSocket = doSSLHandshake(hostname, stunnelHostSNI, port);
			//INJECT PAYLOAD
			String requestPayload = getRequestPayload(hostname, port);
			Log.d("SSL Payload","Inject payload");
			OutputStream out = mSocket.getOutputStream();

			// suporte a [split] na payload
			AppLogManager.addLog(context.getString(R.string.injeting));
			if (!TunnelUtils.injectSplitPayload(requestPayload, out)) {
				try {
					out.write(requestPayload.getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(requestPayload.getBytes());
				}
				out.flush();
			}

			// suporta Dropbear (SSH + PAYLOAD)
			/*if (modoDropbear) {
				return sock;
			}*/

			byte[] buffer = new byte[1024];
			InputStream in = mSocket.getInputStream();

			// lê primeira linha
			int len = ClientServerHello.readLineRN(in, buffer);

			String httpReponseFirstLine = "";
			try {
				httpReponseFirstLine = new String(buffer, 0, len, "ISO-8859-1");
			} catch (UnsupportedEncodingException e3) {
				httpReponseFirstLine = new String(buffer, 0, len);
			}

			AppLogManager.addLog("<strong>" + httpReponseFirstLine + "</strong>");

			String str2 = httpReponseFirstLine;

			int parseInt = Integer.parseInt(str2.substring(9, 12));
			if (parseInt == 200) {
				return mSocket;
			}else if (parseInt != 200){
				Log.d("SSL Payload","Replace with 200 OK");
				AppLogManager.addLog("Proxy: Auto Replace Header");
				AppLogManager.addLog("<b>HTTP/1.1 200 Connection established</b>");
				return mSocket;
			}
			/*else if (parseInt == 101) {
				AppLogManager.addLog("Proxy: Auto Replace Header");
				AppLogManager.addLog("<b>HTTP/1.1 200 Connection established</b>");
				return mSocket;
			}else if (parseInt == 521) {
				AppLogManager.addLog(context.getString(R.string.http_521));
				return mSocket;
			}else {
				AppLogManager.addLog("Sending HTTP/1.1 200 OK ...");
				return mSocket;
			}*/



			// lê o restante
			String httpReponseAll = httpReponseFirstLine;
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
						return mSocket;
					}
				} catch (NumberFormatException e4) {
					throw new IOException("The proxy did not send back a valid HTTP response.");
				}
			} else if (parseInt < 0 || len > 999) {
				throw new IOException("The proxy did not send back a valid HTTP response.");
			}
			else {
				return mSocket;
			}

			//

		}

		return mSocket;
	}


	private SSLSocket doSSLHandshake(String host, String sni, int port) throws IOException {
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers()
					{
						return null;
					}
					public void checkClientTrusted(
							java.security.cert.X509Certificate[] certs, String authType)
					{
					}
					public void checkServerTrusted(
							java.security.cert.X509Certificate[] certs, String authType)
					{
					}
				}
		};

		try {

			X509TrustManager tm = Conscrypt.getDefaultX509TrustManager();
			SSLContext sslContext = SSLContext.getInstance("TLS", "Conscrypt");
			sslContext.init(null, new TrustManager[] { tm }, null);

			TLSSocketFactory tsf = new TLSSocketFactory();
			SSLSocket socket = (SSLSocket) tsf.createSocket(host, port);

			try {
				socket.getClass().getMethod("setHostname", String.class).invoke(socket, sni);

			} catch (Throwable e) {
				// ignore any error, we just can't set the hostname...
			}


			String[] currentTlsVersion = (new String[] {""});
			try{
				if (TLSVersion.equals("auto")){
                  //currentTlsVersion = (new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"});
					currentTlsVersion = socket.getSupportedProtocols();
				}else{
					currentTlsVersion = (new String[] {TLSVersion});

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			socket.setEnabledProtocols(currentTlsVersion);

			//socket.setEnabledProtocols(socket.getSupportedProtocols());
			socket.setEnabledCipherSuites(socket.getEnabledCipherSuites());
			socket.addHandshakeCompletedListener(new HandshakeTunnelCompletedListener(host, port, socket));
			AppLogManager.addLog("Starting SSL Handshake...");
			socket.startHandshake();


			return socket;

		} catch (Exception e) {
			IOException iOException = new IOException(new StringBuffer().append("Could not complete SSL handshake: ").append(e).toString());
			throw iOException;
		}

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
		try {
			if (mSocket != null) {
				mSocket.close();
			}
		} catch(IOException e) {}
	}



}
