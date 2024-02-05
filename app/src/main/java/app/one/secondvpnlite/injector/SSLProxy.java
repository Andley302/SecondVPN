package app.one.secondvpnlite.injector;

import android.util.Log;

import org.conscrypt.Conscrypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.Security;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import app.one.secondvpnlite.SecondVPN;
import app.one.secondvpnlite.logs.AppLogManager;


public class SSLProxy
{

	private Socket incoming;
	private int h = 0;

    static {
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1);

        } catch (NoClassDefFoundError e) {
            e.printStackTrace();
        }}

	public SSLProxy(Socket in){
		incoming = in;
	}

	public Socket inject() {
        try {
            String readLine;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.incoming.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            while (true) {
                readLine = bufferedReader.readLine();
                if (readLine != null && readLine.length() > 0) {
                    stringBuilder.append(readLine);
                    stringBuilder.append("\r\n");
                }
				if (stringBuilder.toString().equals("")) {
					return null;
				}
				String Payload = RetornaPayload(stringBuilder.toString());
				if (Payload != null) {
                    String[] ipSplit = SecondVPN.getServidorSSHDomain().split(":");
					String servidor = ipSplit[0];
                    int porta = Integer.parseInt(ipSplit[1]);
					String sni = SecondVPN.getSNI();
					//Socket socket = SocketChannel.open().socket();
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(servidor, porta), 15000);
					if(socket.isConnected()){
						socket = doSSLHandshake(servidor, sni ,porta);
					}
					InjetaPayload(Payload, socket);
					return socket;
				}
				return null;
            }
        } catch (Exception e) {
			return null;
        }
    }


	
	/*private SSLSocket doSSLHandshake(String host, String sni, int port) throws IOException {
        try {
			TLSSocketFactory tsf = new TLSSocketFactory();
            SSLSocket socket = (SSLSocket) tsf.createSocket(host, port);
			try {
				socket.getClass().getMethod("setHostname", String.class).invoke(socket, sni);
				//addLog("Setting up SNI: "+sni);
			} catch (Throwable e) {
				// ignore any error, we just can't set the hostname...
			}

            String[] currentTlsVersion = (new String[] {""});
            try{
                if (SecondVPN.getTLSVersion().equals("auto")){
                    //currentTlsVersion = (new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"});
                    currentTlsVersion = socket.getSupportedProtocols();
                }else{
                    currentTlsVersion = (new String[] {SecondVPN.getTLSVersion()});

                }

            } catch (Exception e) {
                AppLogManager.addLog("Error: " + e);
                e.printStackTrace();
            }
            socket.setEnabledProtocols(currentTlsVersion);

			//socket.setEnabledProtocols(socket.getSupportedProtocols());
            socket.addHandshakeCompletedListener(new mHandshakeCompletedListener(host, port, socket));
            AppLogManager.addLog("Starting SSL Handshake...");
			socket.startHandshake();
			return socket;
        } catch (Exception e) {
            IOException iOException = new IOException(new StringBuffer().append("Could not do SSL handshake: ").append(e).toString());
            throw iOException;
        }
    }*/

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
                if (SecondVPN.getTLSVersion().equals("auto")){
                    //currentTlsVersion = (new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"});
                    currentTlsVersion = socket.getSupportedProtocols();
                }else{
                    currentTlsVersion = (new String[] {SecondVPN.getTLSVersion()});

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            socket.setEnabledProtocols(currentTlsVersion);

            //socket.setEnabledProtocols(socket.getSupportedProtocols());
            socket.setEnabledCipherSuites(socket.getEnabledCipherSuites());
            socket.addHandshakeCompletedListener(new SSLProxy.mHandshakeCompletedListener(host, port, socket));
            AppLogManager.addLog("Starting SSL Handshake...");
            socket.startHandshake();


            return socket;

        } catch (Exception e) {
            AppLogManager.addLog("SSL Error: " + e);
            IOException iOException = new IOException(new StringBuffer().append("Could not complete SSL handshake: ").append(e).toString());
            throw iOException;
        }

    }
	
	class mHandshakeCompletedListener implements HandshakeCompletedListener {
        private final String val$host;
        private final int val$port;
        private final SSLSocket val$sslSocket;

        mHandshakeCompletedListener( String str, int i, SSLSocket sSLSocket) {
            this.val$host = str;
            this.val$port = i;
            this.val$sslSocket = sSLSocket;
        }

        public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {

			//AppLogManager.addLog("<b><font color=#49C53C>SSL Handshake: " + handshakeCompletedEvent.getSession().getCipherSuite()+"</font></b>");
			//addLog("SSL Handshake: protocol " + handshakeCompletedEvent.getSession().getProtocol());
			//addLog("SSL Handshake: finished");
           // AppLogManager.addLog(new StringBuffer().append("SSL: Supported protocols: <br>").append(Arrays.toString(val$sslSocket.getSupportedProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
            AppLogManager.addLog(new StringBuffer().append("SSL: Enabled protocols: <br>").append(Arrays.toString(val$sslSocket.getEnabledProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
            AppLogManager.addLog("<b><font color=#49C53C>SSL: Using cipher " + handshakeCompletedEvent.getSession().getCipherSuite()+"</font></b>");
            AppLogManager.addLog("SSL: Using protocol " + handshakeCompletedEvent.getSession().getProtocol());
            AppLogManager.addLog("SSL: Handshake finished");
        }
    }
	
	

	private String d(String str) {
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

    private String a(String str, String str2, String str3) {
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



	public String ua() {
        String property = System.getProperty("http.agent");
        return property == null ? "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36" : property;
    }

	private String RetornaPayload(String str) {
        String str2 = null;
		String f = null;
		String i;
        if (str != null) {
            try {
                if (!str.equals("")) {
                    String charSequence = str.split("\r\n")[0];
                    String[] split = charSequence.split(" ");
                    String[] split2 = split[1].split(":");
                    f = split2[0];
                    i = split2[1];
                    str2 = d(SecondVPN.getPayloadKey().replace("[real_raw]", str).replace("[raw]", charSequence).replace("[method]", split[0]).replace("[host_port]", split[1]).replace("[host]", f).replace("[port]", i).replace("[protocol]", split[2])/*.replace("[auth]", auth())*/.replace("[ua]", ua()).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr]", "\n\r").replace("\\r", "\r").replace("\\n", "\n"));
                    return str2;
                }
            } catch (Exception e) {
                //h.a("Payload Error", e.getMessage());
            }
        }
        //h.a("Payload Error", "Payload is null or empty");
        return str2;
    }

	private void InjetaPayload(String str, Socket socket) throws Exception{
        AppLogManager.addLog("<b><font color=#49C53C>Sending payload...</font></b>");
        int i = 0;
		String[] split;
        OutputStream outputStream = socket.getOutputStream();
        if (str.contains("[random]")) {
            Random g = new Random();
            split = str.split(Pattern.quote("[random]"));
            str = split[g.nextInt(split.length)];
        }
        if (str.contains("[repeat]")) {
            split = str.split(Pattern.quote("[repeat]"));
            str = split[this.h];
            this.h++;
            if (this.h > split.length - 1) {
                this.h = 0;
            }
        }
        if (str.contains("[split_delay]")) {
            split = str.split(Pattern.quote("[split_delay]"));
            int length = split.length;
            while (i < length) {
                String str2 = split[i];
                if (a(str2, socket, outputStream)) {
                    outputStream.write(str2.getBytes());
                    outputStream.flush();
                    //b(str2, socket);
                    //sleep(1500);
                }
                i++;
            }
        }
		else if (str.contains("[splitNoDelay]")) {
			split = str.split("\\[splitNoDelay\\]");
			for (i = 0; i < split.length; i++) {
				//addLog(split[i].replace("\r", "\\r").replace("\n", "\\n"));
				outputStream.write(split[i].getBytes());
				outputStream.flush();
			}
		} else if (str.contains("[instant_split]")) {
			split = str.split("\\[instant_split\\]");
			for (i = 0; i < split.length; i++) {
				//addLog(split[i].replace("\r", "\\r").replace("\n", "\\n"));
				outputStream.write(split[i].getBytes());
				outputStream.flush();
			}
		} else if (str.contains("[delay]")) {
			split = str.split("\\[delay\\]");
			for (i = 0; i < split.length; i++) {
				//addLog(split[i].replace("\r", "\\r").replace("\n", "\\n"));
				outputStream.write(split[i].getBytes());
				outputStream.flush();
				if (i != split.length - 1) {
					Thread.sleep((long) 1000);
				}
			}
		} else if (str.contains("[delay_split]")) {
			split = str.split("\\[delay_split\\]");
			for (i = 0; i < split.length; i++) {
				//addLog(split[i].replace("\r", "\\r").replace("\n", "\\n"));
				outputStream.write(split[i].getBytes());
				outputStream.flush();
				if (i != split.length - 1) {
					Thread.sleep((long) 1000);
				}
			}
		} 
//		else if (str.contains("[split_delay]")) {
//			split = str.split("\\[split_delay\\]");
//			for (i = 0; i < split.length; i++) {
//                    addLog(split[i].replace("\r", "\\r").replace("\n", "\\n"));
//                    outputStream.write(split[i].getBytes());
//                    outputStream.flush();
//                    if (i != split.length - 1) {
//                        Thread.sleep((long) 1000);
//                    }
//					}
//                }
		else if (a(str, socket, outputStream)) {
            outputStream.write(str.getBytes());
            outputStream.flush();
            //b(str, socket);
        }
    }

	private boolean a(String str, Socket socket, OutputStream outputStream) throws Exception{
        if (!str.contains("[split]")) {
            return true;
        }
        for (String str2 : str.split(Pattern.quote("[split]"))) {
            outputStream.write(str2.getBytes());
            outputStream.flush();
            //b(str2, socket);
        }
        return false;
    }


	

}
