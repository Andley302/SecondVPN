package app.one.secondvpnlite.injector;

import android.content.Context;

import com.trilead.ssh2.ProxyData;

import org.conscrypt.Conscrypt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.Security;
import java.util.Arrays;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import app.one.secondvpnlite.logs.AppLogManager;

public class SSLTunnelProxy implements ProxyData

{
	class HandshakeTunnelCompletedListener implements HandshakeCompletedListener {
        private final String val$host;
        private final int val$port;
        private final SSLSocket val$sslSocket;



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
	//private int stunnelPort = 443;
	private String stunnelHostSNI;
	private String TLSVersion;
	private Context context;
	private static final int delay = 1000; // in millis



	static {
		try {
			Security.insertProviderAt(Conscrypt.newProvider(), 1);

		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
		}}

	private Socket input;

	private Socket mSocket;

	public SSLTunnelProxy(String server, int port, String hostSni,String TLSVersion,Context context) {
		this.stunnelServer = server;
		this.stunnelPort = port;
		this.stunnelHostSNI = hostSni;
		this.TLSVersion= TLSVersion;
		this.context = context;
	}

	public void SSLSupport(Socket in)
	{
		input = in;
		//http = ApplicationBase.getUtils();
		//dsp = ApplicationBase.getDefSharedPreferences();
	}



	private void sendForwardSuccess(Socket socket) throws IOException
	{
		String respond = "HTTP/1.1 200 OK\r\n\r\n";
		socket.getOutputStream().write(respond.getBytes());
		socket.getOutputStream().flush();
	}

	@Override
	public Socket openConnection(String hostname, int port, int connectTimeout, int readTimeout) throws IOException
	{
		//ver pq nao funciona
		//sendForwardSuccess(input);
		mSocket = SocketChannel.open().socket();
		mSocket.connect(new InetSocketAddress(stunnelServer, stunnelPort), 15000);

		if (mSocket.isConnected()) {
			mSocket = doSSLHandshake(hostname, stunnelHostSNI, port);



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
			/*SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault()
					.createSocket(host, port);*/

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
			socket.setEnabledCipherSuites(socket.getEnabledCipherSuites());
            socket.addHandshakeCompletedListener(new HandshakeTunnelCompletedListener(host, port, socket));
			AppLogManager.addLog("Starting SSL Handshake...");
			socket.startHandshake();
			return socket;
        } catch (Exception e) {
            IOException iOException = new IOException(new StringBuffer().append("SSL handshake error: ").append(e).toString());
			throw iOException;
        }
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

	private void setSNIHost(final SSLSocketFactory factory, final SSLSocket socket, final String hostname) {
		if (factory instanceof android.net.SSLCertificateSocketFactory && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			((android.net.SSLCertificateSocketFactory)factory).setHostname(socket, hostname);
		} else {
			try {
				socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
			} catch (Throwable e) {
				// ignore any error, we just can't set the hostname...
			}
		}
	}

}
