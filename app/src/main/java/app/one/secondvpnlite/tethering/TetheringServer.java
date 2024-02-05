package app.one.secondvpnlite.tethering;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import app.one.secondvpnlite.MainActivity;
import app.one.secondvpnlite.R;
import app.one.secondvpnlite.logs.AppLogManager;

/**
 * Created for http://stackoverflow.com/q/16351413/1266906.
 */
public class TetheringServer implements Runnable {
    private static final String CONNECT = "CONNECT";
    private static final String HTTP_OK = "HTTP/1.1 200 OK\n";
    private static final String TAG = "ProxyServer";
    private static String SERVER_IP;
    // HTTP Headers
    private static final String HEADER_CONNECTION = "connection";
    private static final String HEADER_PROXY_CONNECTION = "proxy-connection";

    public static boolean mIsRunning = false;
    public static ServerSocket serverSocket;
    private int mPort;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    // Get list of IP addresses from all local network interfaces. (JDK1.7)
    public List<InetAddress> getHotspotIpAddress(Boolean useIPv4){
        List<InetAddress> addrList           = new ArrayList<InetAddress>();
        Enumeration<NetworkInterface> enumNI = null;
        try {
            enumNI = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while ( enumNI.hasMoreElements() ){
            NetworkInterface ifc  = enumNI.nextElement();
            try {
                if( ifc.isUp() ){
                    Enumeration<InetAddress> enumAdds  = ifc.getInetAddresses();
                    while ( enumAdds.hasMoreElements() ){
                        InetAddress addr  = enumAdds.nextElement();
                       if (useIPv4){
                           if (!addr.toString().contains(":") && !addr.toString().contains("127.0.0.1") && addr.toString().contains("192.168")){
                              addrList.add(addr);
                           }
                       }else{
                           addrList.add(addr);
                       }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return addrList;
    }


    public static Thread startServerSocket = new Thread() {
        @Override
        public void run() {
            try {
               // InetAddress addr = InetAddress.getByName(SERVER_IP);
                int port = 44355;
               serverSocket = new ServerSocket(port);
               serverSocket.setReuseAddress(true);

                Socket socket;
                try {
                    while ((socket = serverSocket.accept()) != null) {
                        if (!mIsRunning) {
                            break;
                        }
                        socket.setKeepAlive(true);
                        socket.setTcpNoDelay(true);

                        (new Handler(socket)).start();

                    }
                } catch (Exception e) {
                    //AppLogManager.addLog( "<strong></font><font color=red>" + mContext.getString(R.string.hostpot_stop) +" " + "</strong>");
                    e.printStackTrace();  // TODO: implement catch
                }
            }
            catch (IOException e1) {
                Log.e(TAG, "Failed to start proxy server", e1);
            }

        }
    };






    public  static boolean isToStopTethering = false;
    Thread thread = null;
    @Override
    public void run() {
        this.mContext = MainActivity.sContext;
         if (!isToStopTethering){
             thread = new Thread(() -> {
                 try  {
                     //Your code goes here
                     try{

                         String addr = String.valueOf(getHotspotIpAddress(true)).replace("[","").replace("]","").replace(",","<br>");
                         int port = 44355;
                         AppLogManager.addLog( "<strong></font><font color=\"#49C53C\">" + mContext.getString(R.string.tethering_info) +" " + "</strong>"+ "<br><br><strong>" + "IPs:" + "</strong> " +"<strong></font><font color=red>" + addr.replace("/","") +  "</strong> " +"<br><strong>" + mContext.getString(R.string.tethering_port) + "</strong> "+"<strong></font><font color=font color=red>" + port +  "</strong> ");

                         try{
                             mIsRunning = true;
                             startServerSocket.start();
                         } catch (Exception e) {
                             startServerSocket.run();
                             // AppLogManager.addLog("Tethering ServerSocket error: " + String.valueOf(e));
                             e.printStackTrace();
                         }


                     } catch (Exception e) {
                         AppLogManager.addLog("Tethering start service error: " + e);
                         e.printStackTrace();
                     }
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             });

             thread.start();
         }else{
             mIsRunning = false;
             try{
                 startServerSocket.interrupt();
                 AppLogManager.addLog( "<strong></font><font color=red>" + mContext.getString(R.string.tethering_stopped) +" " + "</strong>");
                 try {
                     serverSocket.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
                 try{
                     if (thread != null){
                         thread.stop();
                         thread.interrupt();
                     }

                 } catch (Exception e) {
                     e.printStackTrace();
                 }
                 //stopSelf();
                 //stopForeground(true);
             } catch (Exception e) {
                 //AppLogManager.addLog("Tethering stop service error: " + String.valueOf(e));
                 e.printStackTrace();
             }
         }

    }

    public static class Handler extends Thread {
        private final Socket connection;
        private Handler(Socket connection) {
            this.connection = connection;
        }

        @Override
        public void run() {

            try {
                String requestLine = getLine(connection.getInputStream());
                String[] splitLine = requestLine.split(" ");
                if (splitLine.length < 3) {
                    connection.close();
                    return;
                }
                String requestType = splitLine[0];
                String urlString = splitLine[1];
                String httpVersion = splitLine[2];
                URI url = null;
                String host;
                int port;
                if (requestType.equals(CONNECT)) {
                    String[] hostPortSplit = urlString.split(":");
                    host = hostPortSplit[0];
                    // Use default SSL port if not specified. Parse it otherwise
                    if (hostPortSplit.length < 2) {
                        port = 443;
                    } else {
                        try {
                            port = Integer.parseInt(hostPortSplit[1]);
                        } catch (NumberFormatException nfe) {
                            connection.close();
                            return;
                        }
                    }
                    urlString = "https://" + host + ":" + port;
                } else {
                    try {
                        url = new URI(urlString);
                        host = url.getHost();
                        port = url.getPort();
                        if (port < 0) {
                            port = 80;
                        }
                    } catch (URISyntaxException e) {
                        connection.close();
                        return;
                    }
                }
                List<Proxy> list = Lists.newArrayList();
                try {
                    list = ProxySelector.getDefault().select(new URI(urlString));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                Socket server = null;
                for (Proxy proxy : list) {
                    try {
                        if (!proxy.equals(Proxy.NO_PROXY)) {
                            // Only Inets created by PacProxySelector.
                            InetSocketAddress inetSocketAddress =
                                    (InetSocketAddress)proxy.address();
                            server = new Socket(inetSocketAddress.getHostName(),
                                    inetSocketAddress.getPort());
                            sendLine(server, requestLine);
                        } else {
                            server = new Socket(host, port);
                            if (requestType.equals(CONNECT)) {
                                skipToRequestBody(connection);
                                // No proxy to respond so we must.
                                sendLine(connection, HTTP_OK);
                            } else {
                                // Proxying the request directly to the origin server.
                                sendAugmentedRequestToHost(connection, server,
                                        requestType, url, httpVersion);
                            }
                        }
                    } catch (IOException ioe) {
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "Unable to connect to proxy " + proxy, ioe);
                        }
                    }
                    if (server != null) {
                        break;
                    }
                }
                if (list.isEmpty()) {
                    server = new Socket(host, port);
                    if (requestType.equals(CONNECT)) {
                        skipToRequestBody(connection);
                        // No proxy to respond so we must.
                        sendLine(connection, HTTP_OK);
                    } else {
                        // Proxying the request directly to the origin server.
                        sendAugmentedRequestToHost(connection, server,
                                requestType, url, httpVersion);
                    }
                }
                // Pass data back and forth until complete.
                if (server != null) {
                    SocketConnect.connect(connection, server);
                }
            } catch (Exception e) {
                Log.d(TAG, "Problem Proxying", e);
            }
            try {
                connection.close();
            } catch (IOException ioe) {
                // Do nothing
            }
        }

        /**
         * Sends HTTP request-line (i.e. the first line in the request)
         * that contains absolute path of a given absolute URI.
         *
         * @param server server to send the request to.
         * @param requestType type of the request, a.k.a. HTTP method.
         * @param absoluteUri absolute URI which absolute path should be extracted.
         * @param httpVersion version of HTTP, e.g. HTTP/1.1.
         * @throws IOException if the request-line cannot be sent.
         */
        private void sendRequestLineWithPath(Socket server, String requestType,
                                             URI absoluteUri, String httpVersion) throws IOException {
            String absolutePath = getAbsolutePathFromAbsoluteURI(absoluteUri);
            String outgoingRequestLine = String.format("%s %s %s",
                    requestType, absolutePath, httpVersion);
            sendLine(server, outgoingRequestLine);
        }
        /**
         * Extracts absolute path form a given URI. E.g., passing
         * <code>http://google.com:80/execute?query=cat#top</code>
         * will result in <code>/execute?query=cat#top</code>.
         *
         * @param uri URI which absolute path has to be extracted,
         * @return the absolute path of the URI,
         */
        private String getAbsolutePathFromAbsoluteURI(URI uri) {
            String rawPath = uri.getRawPath();
            String rawQuery = uri.getRawQuery();
            String rawFragment = uri.getRawFragment();
            StringBuilder absolutePath = new StringBuilder();
            if (rawPath != null) {
                absolutePath.append(rawPath);
            } else {
                absolutePath.append("/");
            }
            if (rawQuery != null) {
                absolutePath.append("?").append(rawQuery);
            }
            if (rawFragment != null) {
                absolutePath.append("#").append(rawFragment);
            }
            return absolutePath.toString();
        }
        private String getLine(InputStream inputStream) throws IOException {
            StringBuilder buffer = new StringBuilder();
            int byteBuffer = inputStream.read();
            if (byteBuffer < 0) return "";
            do {
                if (byteBuffer != '\r') {
                    buffer.append((char)byteBuffer);
                }
                byteBuffer = inputStream.read();
            } while ((byteBuffer != '\n') && (byteBuffer >= 0));
            return buffer.toString();
        }
        private void sendLine(Socket socket, String line) throws IOException {
            OutputStream os = socket.getOutputStream();
            os.write(line.getBytes());
            os.write('\r');
            os.write('\n');
            os.flush();
        }
        /**
         * Reads from socket until an empty line is read which indicates the end of HTTP headers.
         *
         * @param socket socket to read from.
         * @throws IOException if an exception took place during the socket read.
         */
        private void skipToRequestBody(Socket socket) throws IOException {
            while (getLine(socket.getInputStream()).length() != 0);
        }
        /**
         * Sends an augmented request to the final host (DIRECT connection).
         *
         * @param src socket to read HTTP headers from.The socket current position should point
         *            to the beginning of the HTTP header section.
         * @param dst socket to write the augmented request to.
         * @param httpMethod original request http method.
         * @param uri original request absolute URI.
         * @param httpVersion original request http version.
         * @throws IOException if an exception took place during socket reads or writes.
         */
        private void sendAugmentedRequestToHost(Socket src, Socket dst,
                                                String httpMethod, URI uri, String httpVersion) throws IOException {
            sendRequestLineWithPath(dst, httpMethod, uri, httpVersion);
            filterAndForwardRequestHeaders(src, dst);
            // Currently the proxy does not support keep-alive connections; therefore,
            // the proxy has to request the destination server to close the connection
            // after the destination server sent the response.
            sendLine(dst, "Connection: close");
            // Sends and empty line that indicates termination of the header section.
            sendLine(dst, "");
        }
        /**
         * Forwards original request headers filtering out the ones that have to be removed.
         *
         * @param src source socket that contains original request headers.
         * @param dst destination socket to send the filtered headers to.
         * @throws IOException if the data cannot be read from or written to the sockets.
         */
        private void filterAndForwardRequestHeaders(Socket src, Socket dst) throws IOException {
            String line;
            do {
                line = getLine(src.getInputStream());
                if (line.length() > 0 && !shouldRemoveHeaderLine(line)) {
                    sendLine(dst, line);
                }
            } while (line.length() > 0);
        }
        /**
         * Returns true if a given header line has to be removed from the original request.
         *
         * @param line header line that should be analysed.
         * @return true if the header line should be removed and not forwarded to the destination.
         */
        private boolean shouldRemoveHeaderLine(String line) {
            int colIndex = line.indexOf(":");
            if (colIndex != -1) {
                String headerName = line.substring(0, colIndex).trim();
                return headerName.regionMatches(true, 0, HEADER_CONNECTION, 0,
                        HEADER_CONNECTION.length())
                        || headerName.regionMatches(true, 0, HEADER_PROXY_CONNECTION,
                        0, HEADER_PROXY_CONNECTION.length());
            }
            return false;
        }
    }



}