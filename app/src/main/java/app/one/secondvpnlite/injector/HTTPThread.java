package app.one.secondvpnlite.injector;

import android.content.*;
import java.io.*;
import java.net.*;
import java.util.*;
import android.util.*;

import app.one.secondvpnlite.logs.AppLogManager;



public class HTTPThread extends Thread
{
	private final String TAG = "ProxyThread";
	Socket incoming;
	Socket outgoing;
	private boolean clientToServer;

	private SharedPreferences sp;

	public HTTPThread(Socket socket, Socket socket2, boolean z)
	{
		incoming = socket;
		outgoing = socket2;
		this.clientToServer = z;
		setDaemon(true);
	}


	public final void run() {
		try{

			byte[] buffer;
			if (clientToServer) {
				buffer = new byte[16384];
			} else {
				buffer = new byte[32768];
			}
			InputStream FromClient = this.incoming.getInputStream();
			OutputStream ToClient = this.outgoing.getOutputStream();

			while (true) {

				//int numberRead = 0;
				int numberRead = FromClient.read(buffer);

				if (numberRead == -1) {
					break;
				}

				String result = new String(buffer, 0, numberRead);
				if (this.clientToServer) {

					ToClient.write(buffer, 0, numberRead);
					ToClient.flush();
				} else {

					String[] split = result.split("\r\n");
					String FullResult = result;

					if (split[0].toLowerCase(Locale.getDefault()).startsWith("http")) {
						result = split[0].substring(9, 12);
						AppLogManager.addLog("<b>" + split[0] + "</b>");
						if (result.indexOf("200") >= 0) {
							if (FullResult.contains("html")){
								ToClient.write(new StringBuilder(String.valueOf(split[0].split(" ")[0])).append(" 200 OK\r\n\r\n").toString().getBytes());
							}else{
								ToClient.write(buffer, 0, numberRead);
							}
							ToClient.flush();

						} else if (true) {
							if (split[0].split(" ")[0].equals("HTTP/1.1")) {
								ToClient.write(new StringBuilder(String.valueOf(split[0].split(" ")[0])).append(" 200 OK\r\n\r\n").toString().getBytes());
							} else {
								try {
									ToClient.write(new StringBuilder(String.valueOf(split[0].split(" ")[0])).append(" 200 Connection established\r\n\r\n").toString().getBytes());
								} catch (Exception e) {
									try {
										if (this.incoming != null) {
											this.incoming.close();
										}
										if (this.outgoing != null) {
											this.outgoing.close();
											return;
										}
										return;
									} catch (IOException e2) {
										return;
									}
								} catch (Throwable th) {
									try {
										if (this.incoming != null) {
											this.incoming.close();
										}
										if (this.outgoing != null) {
											this.outgoing.close();
										}
									} catch (IOException e3) {
									}
								}
							}
							ToClient.flush();
						} else {
							if (FullResult.contains("html")){
								ToClient.write(new StringBuilder(String.valueOf(split[0].split(" ")[0])).append(" 200 OK\r\n\r\n").toString().getBytes());
							}else{
								ToClient.write(buffer, 0, numberRead);
							}
							ToClient.flush();
						}
					} else {
						if (FullResult.contains("html")){
							ToClient.write(new StringBuilder(String.valueOf(split[0].split(" ")[0])).append(" 200 OK\r\n\r\n").toString().getBytes());
						}else{
							ToClient.write(buffer, 0, numberRead);
						}
						ToClient.flush();
					}
				}
			}
			FromClient.close();
			ToClient.close();
		}catch(Exception e){
			try {
				if (this.incoming != null) {
					this.incoming.close();
				}
				if (this.outgoing != null) {
					this.outgoing.close();
				}
			} catch (IOException e4) {
			}
		}
	}

}
