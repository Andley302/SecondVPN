package app.one.secondvpnlite.injector;

import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.one.secondvpnlite.R;
import app.one.secondvpnlite.SecondVPN;
import app.one.secondvpnlite.logs.AppLogManager;

public class HTTPSupport3
{
	private Socket incoming;
	private String netDataString;
	private String[] bugHostRotate;
    private String[] bugHostRotate2;
    private String[] bugHostRotate3;
    private int countRotate = 0;
    private int countRotate2 = 0;
    private int countRotate3 = 0;
	private int coA = 0;
    private int coB = 0;
    private int coC = 0;
    private int coD = 0;
	private String cow;
    private String[] cox;
    private String[] coy;
    private String[] coz;
	private int h = 0;

	

	private SharedPreferences sp;
	
	public HTTPSupport3(Socket in){

		incoming = in;
	}
	
	public Socket socket() {
        String trim;
		Socket socket = null;
		
        try {
			String str;
			String replace;
            Matcher matcher;
            String str2;
            String str3;
            String remote = SecondVPN.getProxyIPDomain();
			String[] split = remote.trim().split(":");
			int i = 80;
			String payload = "CONNECT [host_port] [protocol][crlf][crlf]";
			if (split.length > 1) {
				trim = split[0].trim();
				try {
					i = Integer.parseInt(split[1].trim());
				} catch (NumberFormatException e) {
					i = 80;
				}
			} else {
				trim = split[0].trim();
			}
            socket = new Socket(trim, i);
			payload = SecondVPN.getPayloadKey();
            OutputStream outputStream = socket.getOutputStream();
            Reader reader = new InputStreamReader(incoming.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(reader);
            String readLine = bufferedReader.readLine();
            String str4 = readLine;
            if (readLine != null) {
                str4 = new StringBuffer().append(str4).append("").toString();
            }
            String[] split2 = str4.split(" ");
            String str5 = "80";
            if (split2[1].startsWith("http") || split2[1].indexOf(":") <= 0) {
                str = split2[1];
            } else {
                str = split2[1].split(":")[0];
                str5 = split2[1].split(":")[1];
            }
            netDataString = payload.replace("realData", "netData");
            int indexOf = netDataString.indexOf("netData");
            if (indexOf < 0) {
                replace = netDataString.replace("[METHOD]", split2[0]).replace("[method]", split2[0]).replace("[SSH]", split2[1]).replace("[IP_PORT]", split2[1]).replace("[ip_port]", split2[1]).replace("[IP]", str).replace("[ip]", str).replace("[PORT]", str5).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split2[2]).replace("[host]", str).replace("[port]", str5).replace("[host_port]", split2[1]).replace("[ssh]", split2[1]).replace("[ua]", this. ua()).replace("[raw]", new StringBuffer().append(str4).append("\r\n\r\n").toString()).replace("[real_raw]", new StringBuffer().append(str4).append("\r\n\r\n").toString()).replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n");
            } else if (netDataString.substring(indexOf + 7, (indexOf + 7) + 1).equals("@")) {
                matcher = Pattern.compile("\\[.*?@(.*?)\\]").matcher(netDataString);
                str2 = "";
                if (matcher.find()) {
                    str2 = matcher.group(1);
                }
                str3 = netDataString;
                String r28 = new StringBuffer().append(new StringBuffer().append("[netData@").append(str2.trim()).toString()).append("]").toString();
                replace = str3.replace(r28, new StringBuffer().append(split2[0]).append(" ").append(split2[1]).append("@").append(str2.trim()).append(" ").append(split2[2])).replace("[METHOD]", split2[0]).replace("[method]", split2[0]).replace("[SSH]", split2[1]).replace("[IP_PORT]", split2[1]).replace("[ip_port]", split2[1]).replace("[IP]", str).replace("[ip]", str).replace("[PORT]", str5).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split2[2]).replace("[host]", str).replace("[port]", str5).replace("[host_port]", split2[1]).replace("[ssh]", split2[1]).replace("[ua]", this. ua()).replace("[raw]", new StringBuffer().append(str4).append("\r\n\r\n").toString()).replace("[real_raw]", new StringBuffer().append(str4).append("\r\n\r\n").toString()).replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n");
            } else {
                if (indexOf == 0) {
                    indexOf = 1;
                }
                matcher = Pattern.compile("\\[(.*?)@.*?\\]").matcher(netDataString);
                str2 = "";
                if (matcher.find()) {
                    str2 = matcher.group(1);
                }
                if (netDataString.substring(indexOf - 1, indexOf).equals("@")) {
                    str3 = netDataString;
                    String r28 = new StringBuffer().append("[").append(str2.trim()).append("@netData]").toString();
                    replace = str3.replace(r28, new StringBuffer().append(split2[0]).append(" ").append(str2.trim()).append("@").append(split2[1]).append(" ").append(split2[2])).replace("[METHOD]", split2[0]).replace("[method]", split2[0]).replace("[SSH]", split2[1]).replace("[IP_PORT]", split2[1]).replace("[ip_port]", split2[1]).replace("[IP]", str).replace("[ip]", str).replace("[PORT]", str5).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split2[2]).replace("[host]", str).replace("[port]", str5).replace("[host_port]", split2[1]).replace("[ssh]", split2[1]).replace("[ua]", this. ua()).replace("[raw]", new StringBuffer().append(str4).append("\r\n\r\n").toString()).replace("[real_raw]", new StringBuffer().append(str4).append("\r\n\r\n").toString())
					.replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n");
                } else {
                    replace = netDataString.replace("[netData]", str4).replace("[METHOD]", split2[0]).replace("[method]", split2[0]).replace("[SSH]", split2[1]).replace("[IP_PORT]", split2[1]).replace("[ip_port]", split2[1]).replace("[IP]", str).replace("[ip]", str).replace("[PORT]", str5).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split2[2]).replace("[host]", str).replace("[port]", str5).replace("[host_port]", split2[1]).replace("[ssh]", split2[1]).replace("[ua]", this. ua()).replace("[raw]", new StringBuffer().append(str4).append("\r\n\r\n").toString()).replace("[real_raw]", new StringBuffer().append(str4).append("\r\n\r\n").toString()).replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n");
                }
            }
            matcher = Pattern.compile(".*?\\[rotation_method=(.*?)\\].*?").matcher(replace);
            while (matcher.find()) {
                str2 = matcher.group(1);
                bugHostRotate2 = str2.split(";");
                if (countRotate2 + 1 > bugHostRotate2.length)
				{
                    countRotate2 = 0;
                }
                str3 = replace;
                replace = str3.replace(new StringBuffer().append("[rotation_method=").append(str2).append("]").toString(), bugHostRotate2[countRotate2]);
            }
            Matcher matcher2 = Pattern.compile(".*?\\[rotation=(.*?)\\].*?").matcher(replace);
            while (matcher2.find()) {
                String group = matcher2.group(1);
                bugHostRotate = group.split(";");
                if (countRotate + 1 > bugHostRotate.length) {
                    countRotate = 0;
                }
                str3 = replace;
                replace = str3.replace(new StringBuffer().append("[rotation=").append(group).append("]").toString(), bugHostRotate[countRotate]);
            }
            Matcher matcher3 = Pattern.compile(".*?\\[rotate=(.*?)\\].*?").matcher(replace);
            while (matcher3.find()) {
                String group2 = matcher3.group(1);
                bugHostRotate3 = group2.split(";");
                if (countRotate3 + 1 > bugHostRotate3.length) {
                    countRotate3 = 0;
                }
                str3 = replace;
                replace = str3.replace(new StringBuffer().append("[rotate=").append(group2).append("]").toString(), bugHostRotate3[countRotate3]);
            }
            replace = d(replace);
            String[] split3;
            int i2;
            if (replace.contains("[split]")) {
                split3 = replace.split("\\[split\\]");
                for (i2 = 0; i2 < split3.length; i2++) {
                   //	AppLogManager.addLog(split3[i2].replace("\r", "\\r").replace("\n", "\\n"));
                    outputStream.write(split3[i2].getBytes());
                    outputStream.flush();
                }
            } else if (replace.contains("[splitNoDelay]")) {
                split3 = replace.split("\\[splitNoDelay\\]");
                for (i2 = 0; i2 < split3.length; i2++) {
                    AppLogManager.addLog(split3[i2].replace("\r", "\\r").replace("\n", "\\n"));
                    outputStream.write(split3[i2].getBytes());
                    outputStream.flush();
                }
            } else if (replace.contains("[instant_split]")) {
                split3 = replace.split("\\[instant_split\\]");
                for (i2 = 0; i2 < split3.length; i2++) {
                   AppLogManager.addLog(split3[i2].replace("\r", "\\r").replace("\n", "\\n"));
                    outputStream.write(split3[i2].getBytes());
                    outputStream.flush();
                }
            } else if (replace.contains("[delay]")) {
                split3 = replace.split("\\[delay\\]");
                for (i2 = 0; i2 < split3.length; i2++) {
                    AppLogManager.addLog(split3[i2].replace("\r", "\\r").replace("\n", "\\n"));
                    outputStream.write(split3[i2].getBytes());
                    outputStream.flush();
                    if (i2 != split3.length - 1) {
                        Thread.sleep((long) 1000);
                    }
                }
            } else if (replace.contains("[delay_split]")) {
                split3 = replace.split("\\[delay_split\\]");
                for (i2 = 0; i2 < split3.length; i2++) {
                   AppLogManager.addLog(split3[i2].replace("\r", "\\r").replace("\n", "\\n"));
                    outputStream.write(split3[i2].getBytes());
                    outputStream.flush();
                    if (i2 != split3.length - 1) {
                        Thread.sleep((long) 1000);
                    }
                }
            } else if (replace.contains("[split_delay]")) {
                split3 = replace.split("\\[split_delay\\]");
                for (i2 = 0; i2 < split3.length; i2++) {
                   AppLogManager.addLog(split3[i2].replace("\r", "\\r").replace("\n", "\\n"));
                    outputStream.write(split3[i2].getBytes());
                    outputStream.flush();
                    if (i2 != split3.length - 1) {
                        Thread.sleep((long) 1000);
                    }
                }
            } else {
                AppLogManager.addLog(replace.replace("\r", "\\r").replace("\n", "\\n"));
                outputStream.write(replace.getBytes());
                outputStream.flush();
            }
           AppLogManager.addLog("HTTP: Sending payload");
        } catch (Exception e2) {
            AppLogManager.addLog("HTTP: Fail to remote proxy");
        }
        countRotate++;
        countRotate2++;
        countRotate3++;
        return socket;
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

    private String auth() {
        String str = "";
        try {
          } catch (Exception e) {
           AppLogManager.addLog(e.getMessage());
        }
        return str;
    }

	public String ua() {
        String property = System.getProperty("http.agent");
        return property == null ? "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36" : property;
    }
	
	public Socket inject() {
        //String str;
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
					AppLogManager.addLog("Get request data failed, empty requestline");
					return null;
				}
						String c = c(stringBuilder.toString());
				if (c != null) {
					AppLogManager.addLog("Try connect...");
					//h.a("Socket Server", "Connecting to " + str);
					Socket socket = new Socket();
                    String[] ipSplit = SecondVPN.getProxyIPDomain().split(":");
                    String ip = ipSplit[0];
                    int port = Integer.parseInt(ipSplit[1]);
					socket.connect(new InetSocketAddress(ip, Integer.valueOf(port).intValue()), 15000);
					a(c, socket);
					return socket;
				}
				return null;
            }
        } catch (Exception e) {
          	return null;
        }
    }
	
	private String c(String str) {
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
                    str2 = d(SecondVPN.getPayloadKey()).replace("[real_raw]", str).replace("[raw]", charSequence).replace("[method]", split[0]).replace("[host_port]", split[1]).replace("[host]", f).replace("[port]", i).replace("[protocol]", split[2])/*.replace("[auth]", this.j)*/.replace("[ua]", ua()).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr]", "\n\r").replace("\\r", "\r").replace("\\n", "\n");
                    return str2;
                }
            } catch (Exception e) {
                //h.a("Payload Error", e.getMessage());
            }
        }
        //h.a("Payload Error", "Payload is null or empty");
        return str2;
    }
	
	private void a(String str, Socket socket) throws Exception{
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
        } else if (a(str, socket, outputStream)) {
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
	
	
	
	public String b(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            stringBuilder.append("*");
        }
        return stringBuilder.toString();
    }
	
	public Socket socket2() {
        String trim;
        int i = 0;
        Socket cfQ = null;
		String payload = SecondVPN.getPayloadKey();
		String remote = SecondVPN.getProxyIPDomain();
		String[] split = remote.trim().split(":");
         int i2 = 80;
        if (split.length > 1) {
            trim = split[0].trim();
            try {
                i2 = Integer.parseInt(split[1].trim());
            } catch (NumberFormatException e) {
                i2 = 80;
            }
        } else {
            trim = split[0].trim();
        }
        try {
            String[] split2;
            CharSequence charSequence;
            CharSequence f;

            cfQ = new Socket(trim, i2);
           /* cfQ = new Socket();
            cfQ.setSoTimeout(15000);
            cfQ.connect(new InetSocketAddress(trim, i2), 15000);*/

            OutputStream outputStream = cfQ.getOutputStream();

            String readLine = new BufferedReader(new InputStreamReader(incoming.getInputStream())).readLine();
            String[] split3 = readLine.split(" ");
            CharSequence charSequence2 = split3[1];
            if (split3[0].equals("CONNECT")) {
                split2 = split3[1].split(":");
                charSequence2 = split2[0];
                charSequence = split2.length < 2 ? "443" : split2[1];
            } else {
                charSequence = "80";
            }
            String cR = payload;
            if (cR.contains("[random]")) {
                Random random = new Random();
                split = cR.split(Pattern.quote("[random]"));
                cR = split[random.nextInt(split.length)];
            }
            this.cow = cR;
            cR = payload;
            if (cR.contains("[repeat]")) {
                String[] split4 = cR.split(Pattern.quote("[repeat]"));
                cR = split4[this.coD];
                if (this.coD + 1 > split4.length) {
                    this.coD = 0;
                }
            }
            this.cow = cR;
            this.cow = payload.replace("realData", "netData");
            int indexOf = this.cow.indexOf("netData");
            if (indexOf < 0) {
                trim = this.cow.replace("[METHOD]", split3[0]).replace("[method]", split3[0]).replace("[SSH]", split3[1]).replace("[IP_PORT]", split3[1]).replace("[ip_port]", split3[1]).replace("[IP]", charSequence2).replace("[ip]", charSequence2).replace("[PORT]", charSequence).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split3[2]).replace("[host]", charSequence2).replace("[port]", charSequence).replace("[host_port]", split3[1]).replace("[ssh]", split3[1]).replace("[ua]", this. ua()).replace("[raw]", readLine + "\r\n\r\n").replace("[real_raw]", readLine + "\r\n\r\n").replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n");
            } else if (this.cow.substring(indexOf + 7, (indexOf + 7) + 1).equals("@")) {
                Matcher matcher = Pattern.compile("\\[.*?@(.*?)\\]").matcher(this.cow);
                cR = "";
                if (matcher.find()) {
                    cR = matcher.group(1);
                }
                trim = this.cow.replace("[netData@" + cR.trim() + "]", split3[0] + " " + split3[1] + "@" + cR.trim() + " " + split3[2]).replace("[METHOD]", split3[0]).replace("[method]", split3[0]).replace("[SSH]", split3[1]).replace("[IP_PORT]", split3[1]).replace("[ip_port]", split3[1]).replace("[IP]", charSequence2).replace("[ip]", charSequence2).replace("[PORT]", charSequence).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split3[2]).replace("[host]", charSequence2).replace("[port]", charSequence).replace("[host_port]", split3[1]).replace("[ssh]", split3[1]).replace("[ua]", this. ua()).replace("[raw]", readLine + "\r\n\r\n").replace("[real_raw]", readLine + "\r\n\r\n").replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n");
            } else {
                int i3 = indexOf == 0 ? 1 : indexOf;
                Matcher matcher2 = Pattern.compile("\\[(.*?)@.*?\\]").matcher(this.cow);
                cR = "";
                if (matcher2.find()) {
                    cR = matcher2.group(1);
                }
                trim = this.cow.substring(i3 + -1, i3).equals("@") ? this.cow.replace("[" + cR.trim() + "@netData]", split3[0] + " " + cR.trim() + "@" + split3[1] + " " + split3[2]).replace("[METHOD]", split3[0]).replace("[method]", split3[0]).replace("[SSH]", split3[1]).replace("[IP_PORT]", split3[1]).replace("[ip_port]", split3[1]).replace("[IP]", charSequence2).replace("[ip]", charSequence2).replace("[PORT]", charSequence).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split3[2]).replace("[host]", charSequence2).replace("[port]", charSequence).replace("[host_port]", split3[1]).replace("[ssh]", split3[1]).replace("[ua]", this. ua()).replace("[raw]", readLine + "\r\n\r\n").replace("[real_raw]", readLine + "\r\n\r\n").replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n") : this.cow.replace("[netData]", readLine).replace("[METHOD]", split3[0]).replace("[method]", split3[0]).replace("[SSH]", split3[1]).replace("[IP_PORT]", split3[1]).replace("[ip_port]", split3[1]).replace("[IP]", charSequence2).replace("[ip]", charSequence2).replace("[PORT]", charSequence).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split3[2]).replace("[host]", charSequence2).replace("[port]", charSequence).replace("[host_port]", split3[1]).replace("[ssh]", split3[1]).replace("[ua]", this. ua()).replace("[raw]", readLine + "\r\n\r\n").replace("[real_raw]", readLine + "\r\n\r\n").replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n");
            }
            Matcher matcher3 = Pattern.compile(".*?\\[rotation_method=(.*?)\\].*?").matcher(trim);
            while (matcher3.find()) {
                cR = matcher3.group(1);
                this.cox = cR.split(";");
                if (this.coA + 1 > this.cox.length) {
                    this.coA = 0;
                }
                trim = trim.replace("[rotation_method=" + cR + "]", this.cox[this.coA]);
            }
            matcher3 = Pattern.compile(".*?\\[rotation=(.*?)\\].*?").matcher(trim);
            while (matcher3.find()) {
                cR = matcher3.group(1);
                this.coy = cR.split(";");
                if (this.coB + 1 > this.coy.length) {
                    this.coB = 0;
                }
                trim = trim.replace("[rotation=" + cR + "]", this.coy[this.coB]);
            }
            matcher3 = Pattern.compile(".*?\\[rotate=(.*?)\\].*?").matcher(trim);
            while (matcher3.find()) {
                cR = matcher3.group(1);
                this.coz = cR.split(";");
                if (this.coC + 1 > this.coz.length) {
                    this.coC = 0;
                }
                trim = trim.replace("[rotate=" + cR + "]", this.coz[this.coC]);
            }
            trim = d(trim);
                   String[] split5;
            if (trim.contains("[split]")) {
                split5 = trim.split("\\[split\\]");
                while (i < split5.length) {
                    
                    outputStream.write(split5[i].getBytes());
                    outputStream.flush();
                    i++;
                }
            } else if (trim.contains("[splitNoDelay]")) {
                split5 = trim.split("\\[splitNoDelay\\]");
                while (i < split5.length) {
                    
                    outputStream.write(split5[i].getBytes());
                    outputStream.flush();
                    i++;
                }
            } else if (trim.contains("[instant_split]")) {
                split5 = trim.split("\\[instant_split\\]");
                while (i < split5.length) {
                    
                    outputStream.write(split5[i].getBytes());
                    outputStream.flush();
                    i++;
                }
            } else if (trim.contains("[delay]")) {
                split5 = trim.split("\\[delay\\]");
                while (i < split5.length) {
                    
                    outputStream.write(split5[i].getBytes());
                    outputStream.flush();
                    if (i != split5.length - 1) {
                        Thread.sleep(1000);
                    }
                    i++;
                }
            } else if (trim.contains("[delay_split]")) {
                split5 = trim.split("\\[delay_split\\]");
                while (i < split5.length) {
                    
                    outputStream.write(split5[i].getBytes());
                    outputStream.flush();
                    if (i != split5.length - 1) {
                        Thread.sleep(1000);
                    }
                    i++;
                }
            } else if (trim.contains("[split_delay]")) {
                split2 = trim.split("\\[split_delay\\]");
                for (int i4 = 0; i4 < split2.length; i4++) {
                    
                    outputStream.write(split2[i4].getBytes());
                    outputStream.flush();
                    if (i4 != split2.length - 1) {
                        Thread.sleep(1000);
                    }
                }
            } else {
                
                outputStream.write(trim.getBytes());
                outputStream.flush();
            }
            AppLogManager.addLog(R.string.state_proxy_inject);
            this.coB++;
            this.coA++;
            this.coC++;
            this.coD++;
            return cfQ;
        } catch (Exception e22) {
			//AppLogManager.addLog("There was an error connecting.");
			AppLogManager.addLog("Error: " + e22);
           //a.a("ProxyServer", "[" + new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(new Date()) + "] <font color=#FF0000>" + e22.getMessage(), null);
            return null;
        }
    }


    private static String f(String str, String str2, String str3) {
        while (str.contains(str2)) {
            Matcher matcher = Pattern.compile("\\[.*?\\*(.*?[0-9])\\]").matcher(str);
            if (matcher.find()) {
                int intValue = Integer.valueOf(matcher.group(1)).intValue();
                String str4 = "";
                for (int i = 0; i < intValue; i++) {
                    str4 = str4 + str3;
                }
                str = str.replace(str2 + String.valueOf(intValue) + "]", str4);
            }
        }
        return str;
    }

    
}
