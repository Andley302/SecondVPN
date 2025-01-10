package app.one.secondvpnlite.service;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.ProxyInfo;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ConnectionMonitor;
import com.trilead.ssh2.DebugLogger;
import com.trilead.ssh2.DynamicPortForwarder;
import com.trilead.ssh2.HTTPProxyData;
import com.trilead.ssh2.InteractiveCallback;
import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.ProxyData;
import com.trilead.ssh2.ServerHostKeyVerifier;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import app.one.secondvpnlite.MainActivity;
import app.one.secondvpnlite.R;
import app.one.secondvpnlite.SecondVPN;
import app.one.secondvpnlite.injector.HTTPSupport3;
import app.one.secondvpnlite.injector.HTTPThread;
import app.one.secondvpnlite.injector.HttpProxyCustom;
import app.one.secondvpnlite.injector.SSLProxy;
import app.one.secondvpnlite.injector.SSLTunnelProxy;
import app.one.secondvpnlite.logs.AppLogManager;
import app.one.secondvpnlite.notification.NotificationService;
import app.one.secondvpnlite.tethering.TetheringServer;
import app.one.secondvpnlite.tunnel.IPUtil;
import app.one.secondvpnlite.tunnel.IpAddress;
import app.one.secondvpnlite.tunnel.TunnelUtils;
import app.one.secondvpnlite.tunnel.vpn.CIDRIP;
import app.one.secondvpnlite.tunnel.vpn.NetworkSpace;
import app.one.secondvpnlite.tunnel.vpn.Pdnsd;
import app.one.secondvpnlite.tunnel.vpn.Tun2Socks;
import app.one.secondvpnlite.tunnel.vpn.VpnUtils;

public class TunnelManager extends VpnService implements Runnable, ConnectionMonitor, InteractiveCallback,
        ServerHostKeyVerifier, DebugLogger {

    private final String TAG = "TunnelManager";
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final Charset ISO = Charset.forName("ISO-8859-1");
    private CountDownLatch mTunnelThreadStopSignal;

    private ConnectivityManager.NetworkCallback networkCallback;

    private boolean mRunning = false, mStopping = false, mStarting = false;
    private final int listen_port = 9090;
    private HTTPProxyData coS;
    private Thread mInjectThread;
    private ServerSocket listen_socket;
    private Socket input;
    private static Socket output = null;
    private HTTPThread sc1;
    private HTTPThread sc2;
    public boolean mReconnecting = false;
    public static boolean bypassToInject;
    public boolean fakeTunnelIsStarted = false;
    public static boolean isToStopService = false;
    public static boolean stoppingLog = false;
    public static boolean stoppedLog = false;
    public static boolean vpnDestroyedLog = false;
    private ConnectivityManager connectivityManager;
    private Context mContext;
    public static boolean isServiceRunning = false;
    private Thread mTetheringThread;
    private TetheringServer mTetheringServer;
    PowerManager.WakeLock wakeLock;

    public TunnelManager() {
        mContext = MainActivity.sContext;
        wakeLock = MainActivity.wakeLock;
    }

    private void setWakelock() {
        try {
            //CERTO DEPOIS DE 10 MIN
            //this.wakeLock.acquire(10*60*1000L /*10 minutes*/);
            wakeLock.acquire();
        } catch (Exception e) {
            Log.d("WAKELOCK", e.getMessage());
        }
    }


    private void unsetWakelock() {
        if (this.wakeLock != null && this.wakeLock.isHeld()) {
            try {
                this.wakeLock.release();
                AppLogManager.addLog(mContext.getString(R.string.wakelock_disabled));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Cliente SSH
     */

    private final static int AUTH_TRIES = 5; //ORIGINAL 1
    private final static int RECONNECT_TRIES = 15; //ORIGINAL 5

    private Connection mConnection;

    public void stopMultiStatusInjectThread() {
        //Log.i(TAG,"Socket Stopped");
        try {
            if (sc1 != null) {
                sc1.interrupt();
                sc1 = null;
            }
            if (sc2 != null) {
                sc2.interrupt();
                sc2 = null;
            }

            if (listen_socket != null) {
                listen_socket.close();
                listen_socket = null;
            }
            if (input != null) {
                input.close();
                input = null;
            }
            if (output != null) {
                output.close();
                output = null;
            }
            if (mInjectThread != null) {
                mInjectThread.interrupt();
            }


        } catch (Exception e) {

        }
    }

    Runnable newinjectTh = new Runnable() {

        @Override
        public void run() {

            // TODO: Implement this method
            try {

                if (listen_socket != null) {
                    try {
                        listen_socket.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                listen_socket = new ServerSocket(listen_port);

                listen_socket.setReuseAddress(true);


                while (true) {
                    input = listen_socket.accept();
                    input.setSoTimeout(0);


                    if (SecondVPN.getConnectionMode().equals("MODO_HTTP") && !SecondVPN.isHTTPDirect()) {
                        output = new HTTPSupport3(input).socket2();
                    }

                    if (SecondVPN.getConnectionMode().equals("MODO_HTTPS") && SecondVPN.isPayloadAfterTLS()) {
                        output = new SSLProxy(input).inject();
                    }


                    if (input != null) {
                        input.setKeepAlive(true);
                    }
                    if (output != null) {
                        output.setKeepAlive(true);
                    }
                    if (output == null) {
                        output.close();
                    } else if (output.isConnected()) {
                        //AppLogManager.addLog(mContext.getString(R.string.state_proxy_running));
                        sc1 = new HTTPThread(input, output, true);
                        sc2 = new HTTPThread(output, input, false);
                        sc1.setDaemon(true);
                        sc1.start();
                        sc2.setDaemon(true);
                        sc2.start();
                    }
                }
            } catch (Exception e) {
                if (listen_socket != null) {
                    try {
                        listen_socket.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            }

        }
    };

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!(cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected())) {
            //Do something
            return false;
        }
        return true;
    }

    private void startMultiStatusInject() {
        try {
            if (mInjectThread != null) {
                mInjectThread.interrupt();
            }
            mInjectThread = new Thread(newinjectTh, "mInjectThread");
            mInjectThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public synchronized void closeSSH() {
        stopForwarder();
        if (mConnection != null) {
            mConnection.close();
        }
    }

    public synchronized void closeSSH2() {
        //INICIA VPN TUNNEL COM BYPASS
        if (!fakeTunnelIsStarted) {
            try {
                startFakeTunnelVpnService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mConnection != null) {
            mConnection.close();
        }


        //DESCONECTECA SSH
        stopForwarderSocks();
		/*if (mConnection != null) {
			//AppLogManager.logDebug("Parando SSH");
			mConnection.close();
		}*/

    }

    private void stopNetworkCallback() {
        if (networkCallback != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                    ;
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                }
            } catch (Exception e) {
                AppLogManager.addLog(e.toString());
                //throw new RuntimeException(e);
            }
        }


    }

    public void stopAll() {
        mStopping = true;

        try {
            stopNetworkCallback();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // if (mStopping) return;

        //ADICIONA AO LOG
        if (!stoppingLog) {
            AppLogManager.addLog(mContext.getString(R.string.stopping));
            stoppingLog = true;
        }


        //SETA STATUS VPN
        SecondVPN.setCurrentVpnStatus("PARANDO");

        try {
            MainActivity.setButtonStatus("PARANDO");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (SecondVPN.isEnableWakeLock()) {
            try {
                unsetWakelock();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //AppLogManager.addLog(mContext.getString(R.string.stopping));
        isToStopService = true;
        new Thread(() -> {
            mStopping = true;

            if (mTunnelThreadStopSignal != null)
                mTunnelThreadStopSignal.countDown();

            closeSSH();

            if (mConnection != null) {
                mConnection.close();
            }
            try {
                stopMultiStatusInjectThread();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                //Thread.sleep(1000);
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }


            mRunning = false;
            mStarting = false;
            mReconnecting = false;


        }).start();


        //PARA TUDO
        //stopSelf();
        //INTERROMPE NOTIFICAÇÃO
        try {
            stopNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            closeSSH2();
            closeSSH();
            stopMultiStatusInjectThread();
            stopForwarderSocks();
            stopForwarder();
            //PARA TETHERING SE HABILITADO
            if (SecondVPN.isEnableTethering()) {
                StopTethering();
            }
            if (SecondVPN.isEnableTetheringRoot()) {
                stopTetheringRoot();
            }
            //LOG
            if (!stoppedLog) {
                AppLogManager.addLog("<strong></font><font color=#FF0000>" + mContext.getString(R.string.disconnected_log) + "</strong>");
                stoppedLog = true;
            }


            //PARA TUNNEL SE AINDA ATIVO
            disconnectTunnel();

            //SETA STATUS VPN
            SecondVPN.setCurrentVpnStatus("DESCONECTADO");
            try {
                MainActivity.setButtonStatus("DESCONECTADO");
            } catch (Exception e) {
                e.printStackTrace();
            }


            //PARA TUDO
            //stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
        }


        stopSelf();
    }

    private void stopNotification() {
        //NOTIFICAÇÃO
        if (SecondVPN.isEnableNotification()) {
            Intent stopNotification = new Intent(mContext, NotificationService.class);
            try {
                mContext.stopService(stopNotification);
            } catch (Exception e) {
                NotificationService.stopNotification();
                e.printStackTrace();
            }
        }
    }

    private boolean mConnected = false;
    private String servidor = "";
    private String servidor_domain = "";

    public static String resolveHostDomain(String hostIP) {
        try {
            InetAddress a = InetAddress.getByName(hostIP);
            hostIP = a.getHostAddress();
            return hostIP;
        } catch (Exception e) {
            //AppLogManager.addToLog("Unresolved host, continuing connection...");

            //AppLogManager.addLog("Unresolved host: " + e.toString().replace(hostIP, "*"));
            return hostIP;
        }
    }

    public void startSSH() throws Exception {

        //NOTIFICAÇÃO VPN RODANDO
        if (SecondVPN.isEnableNotification()) {
            Intent startNotification = new Intent(mContext, NotificationService.class);
            startNotification.putExtra("TITLE", mContext.getString(R.string.app_name));
            startNotification.putExtra("BODY", mContext.getString(R.string.starting_notification));
            startNotification.putExtra("IS_CONNECTED", false);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mContext.startForegroundService(startNotification);
                } else {
                    mContext.startService(startNotification);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        if (!isToStopService) {
            //Monitora rede
            try {
                startNetworkCallback();
            } catch (Exception e) {
                AppLogManager.addLog(e.toString());
            }

            SecondVPN.setCurrentVpnStatus("INICIANDO");
            mStopping = false;
            mRunning = true;

            String[] ipSplit = SecondVPN.getServidorSSHDomain().split(":");

            String[] userAndPasSplit = SecondVPN.getUsuarioAndPass().split("@");
            String usuario = userAndPasSplit[0];
            String _senha = userAndPasSplit[1];


            try {
                servidor = ipSplit[0];
                servidor_domain = ipSplit[0];

                currentIPAddr = resolveHostDomain(servidor);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (servidor.isEmpty()) {
                servidor = ipSplit[0];
            }
            if (servidor == null) {
                servidor = ipSplit[0];
            }

            int porta = Integer.parseInt(ipSplit[1]);

            // String senha = _senha.isEmpty() ? PasswordCache.getAuthPassword(null, false) : _senha;

            String keyPath = "";
            int portaLocal = 1080;

            try {
                conectar(servidor, porta);


                for (int i = 0; i < AUTH_TRIES; i++) {
                    if (mStopping) {
                        return;
                    }

                    try {
                        authentication(usuario, _senha, keyPath);

                        break;
                    } catch (IOException e) {
                        if (i + 1 >= AUTH_TRIES) {
                            throw new IOException("Autenticação falhou");
                        } else {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e2) {
                                return;
                            }
                        }
                    }
                }

                //SETA BOTAO CONECTADO
                try {
                    MainActivity.setButtonStatus("CONECTADO");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //CARREGA AD
                try {
                    MainActivity.ladi();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                ////AppLogManager.updateStateString(//AppLogManager.SSH_CONECTADO, "Conexão SSH estabelecida");
                AppLogManager.addLog("<strong></font><font color=#49C53C>" + mContext.getString(R.string.connected) + "</strong>");
                SecondVPN.setCurrentVpnStatus("CONECTADO");
                if (SecondVPN.isEnableWakeLock()) {
                    AppLogManager.addLog(mContext.getString(R.string.wakelock_enabled));
                }
                //NOTIFICAÇÃO CONECTADO
                if (SecondVPN.isEnableNotification()) {
                    reconnectNotificationIsStarted = false;
                    Intent startNotification = new Intent(mContext, NotificationService.class);
                    startNotification.putExtra("TITLE", mContext.getString(R.string.app_name));
                    startNotification.putExtra("BODY", mContext.getString(R.string.connected_notification));
                    startNotification.putExtra("IS_CONNECTED", true);
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mContext.startForegroundService(startNotification);
                        } else {
                            mContext.startService(startNotification);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                if (SecondVPN.isEnableTetheringRoot()) {
                    AppLogManager.addLog(mContext.getString(R.string.tethering_root_enable));
                    AppLogManager.addLog(mContext.getString(R.string.tethering_root_not_avaliable));
                }

                startForwarder(portaLocal);

                Thread.sleep(3500);


            } catch (Exception e) {
                mConnected = false;
                throw e;
            }
        } else {
            try {
                stopAll();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //INTERROMPE NOTIFICAÇÃO
            try {
                stopNotification();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void startNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    // Lógica para quando a rede está disponível
                    //Toast.makeText(mContext, "Rede disponível", Toast.LENGTH_SHORT).show();
                    if (mConnected) {
                        Throwable reasonClosedCause = new Exception("A network error occurred (network lost).");
                        connectionLost(reasonClosedCause);
                    }
                }

                @Override
                public void onLost(Network network) {
                    // Lógica para quando a rede é perdida
                    //Toast.makeText(mContext, "Rede perdida", Toast.LENGTH_SHORT).show();
                    /*if (mConnected){
                        Throwable reasonClosedCause = new Exception("A network error occurred (network lost).");
                        connectionLost(reasonClosedCause);
                    }*/
                }
            };

            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        }
    }

    private void showCurrentDNS() {
        try {
            String dnsList = "";
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    List<InetAddress> dnsServers = connectivityManager
                            .getLinkProperties(connectivityManager.getActiveNetwork())
                            .getDnsServers();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        dnsList = dnsServers.stream()
                                .map(InetAddress::getHostAddress)
                                .collect(Collectors.joining(", "));
                    }
                }
            } catch (Exception e) {
                dnsList = "?";
                e.printStackTrace();
            }

            AppLogManager.addLog("DNS: " + dnsList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected void startForwarder(int portaLocal) throws Exception {
        if (!mConnected) {
            throw new Exception();
        }

        startForwarderSocks(portaLocal);
        try {
            //VER PQ NAO GERA DADOS AQUI
            bypassToInject = false;
            startTunnelVpnService();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static DynamicPortForwarder dpf;

    private synchronized void startForwarderSocks(int portaLocal) throws Exception {
        if (!mConnected) {
            throw new Exception();
        }


        try {
            dpf = mConnection.createDynamicPortForwarder(portaLocal);
        } catch (Exception e) {
            //throw new Exception();
            AppLogManager.addLog(e.toString());
        }
    }

    public static synchronized void stopForwarderSocks() {
        if (dpf != null) {
            try {
                dpf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dpf = null;
        }
    }

    protected void stopForwarder() {
        stopTunnelVpnService();
        stopForwarderSocks();
    }


    protected synchronized void stopTunnelVpnService() {
        disconnectTunnel();

    }


    private void StartTethering() {
        //HOTSTOP
        try {
            WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int apState = (Integer) wifiManager.getClass().getMethod("getWifiApState").invoke(wifiManager);
            if (apState == 13) {
                TetheringServer.isToStopTethering = false;
                mTetheringServer = new TetheringServer();
                mTetheringThread = new Thread(mTetheringServer);
                mTetheringThread.start();
            } else {
                AppLogManager.addLog("<strong></font><font color=red>" + mContext.getString(R.string.hostpot_isOFF) + " " + "</strong>");

            }

        } catch (Exception e) {
            AppLogManager.addLog("<strong></font><font color=red>" + mContext.getString(R.string.hostpot_error) + " " + "</strong>");

            e.printStackTrace();
        }
    }

    private void StopTethering() {
        try {
            TetheringServer.isToStopTethering = true;
            mTetheringServer = new TetheringServer();
            mTetheringThread = new Thread(mTetheringServer);
            mTetheringThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopTetheringRoot() {
        AppLogManager.addLog(mContext.getString(R.string.tethering_root_stoppped));
    }

    /**
     * Vpn Tunnel
     */


    public static String[] m_dnsResolvers = new String[]{"1.1.1.1", "1.0.0.1"};

    private void setDNS() {
        if (SecondVPN.isEnableCustomDNS()) {
            m_dnsResolvers = new String[]{SecondVPN.customDNS1(), SecondVPN.customDNS2()};
        } else {
            List<String> lista = VpnUtils.getNetworkDnsServer(mContext);
            m_dnsResolvers = new String[]{lista.get(0)};

        }
    }

    public static String currentIPAddr;

    protected void startTunnelVpnService() throws IOException {
        isBypass = false;
        setDNS();
        startTunnelPrefs();
        establishVpn();

        fakeTunnelIsStarted = false;
    }


    protected void startFakeTunnelVpnService() throws IOException {
        isBypass = true;
        startTunnelPrefs();
        establishVpn();

        fakeTunnelIsStarted = true;
    }

    private void startTunnelPrefs() {
        mRoutes = new NetworkSpace();
        mRoutesv6 = new NetworkSpace();
        IpAddress ipAddr = new IpAddress();
        privateIpAddress = ipAddr.getPrivateAddress();
        prefixLength = ipAddr.getPrefixLength();
        mRouter = ipAddr.getPrivateAddressRouter();

        try {
            mPrivateAddress = VpnUtils.selectPrivateAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void conectar(String servidor, int porta) throws Exception {
        try {
            coS = new HTTPProxyData("127.0.0.1", 9090);
            mConnection = new Connection(servidor, porta);

            if (SecondVPN.getConnectionMode().equals("MODO_HTTP") && !SecondVPN.isHTTPDirect()) {
                if (this.coS != null) {
                    mConnection.setProxyData(coS);
                }
            }

            if (SecondVPN.getConnectionMode().equals("MODO_HTTPS") && SecondVPN.isPayloadAfterTLS()) {
                if (this.coS != null) {
                    mConnection.setProxyData(coS);
                }
            }

            // delay sleep
            if (SecondVPN.isEnableNoTCPDelay()) {
                mConnection.setTCPNoDelay(true);
            }

            if (SecondVPN.isEnableSSHCompress()) {
                mConnection.setCompression(true);
                AppLogManager.addLog(mContext.getString(R.string.enabled_ssh_compress));
            }

            addProxy(mConnection);

            // monitora a conexão
            mConnection.addConnectionMonitor(this);
            if (Build.VERSION.SDK_INT >= 23) {
                ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                ProxyInfo proxy = cm.getDefaultProxy();
                if (proxy != null) {
                    AppLogManager.addLog("<strong>Proxy:</strong> " + String.format("%s:%d", proxy.getHost(), proxy.getPort()));
                }
            }

            AppLogManager.addLog(R.string.state_connecting);

            mConnection.connect(this, 10 * 1000, 20 * 1000);

            mConnected = true;

        } catch (Exception e) {

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            String cause = e.getCause().toString();
            if (useProxy && cause.contains("Key exchange was not finished")) {
                AppLogManager.addLog("<strong></font><font color=#FF0000>Proxy: Key exchange was not finished</strong>");
            } else if (!mStopping) {
                //PROTEGER AQUI DO DOMINIO DO SERVIDOR QUANDO ARQUIVO CUSTOMIZADO
                if (SecondVPN.getIsCustomFileIsLocked()) {
                    String cause_rep = cause;


                    AppLogManager.addLog("<strong></font><font color=#FF0000>SSH Error: </strong><strong>" + cause_rep.replaceAll(servidor, "*").replaceAll(servidor_domain, "*") + "</strong>");
                } else {
                    AppLogManager.addLog("<strong></font><font color=#FF0000>SSH Error: </strong><strong>" + cause + "</strong>");
                }

                if (!mReconnecting) {
                    //DELAY RECONNECT
                    try {
                        AppLogManager.addLog("<strong><font color=\"#ff8c00\">" + mContext.getString(R.string.state_reconnecting) + "...</strong>");
                        //Thread.sleep(1200); //1200 ms
                        Thread.sleep(2000); //1200 ms
                    } catch (InterruptedException e1) {
                    }
                    //RECONECTA
                    reconnectSSH();
                }


            }

            throw new Exception(e);
        }
    }

    private boolean useProxy = false;

    protected void addProxy(Connection conn) throws Exception {
        useProxy = true;
        String ModoConexao = SecondVPN.getConnectionMode();
        if (ModoConexao.equals("MODO_HTTP")) {
            String[] ipSplit = SecondVPN.getServidorSSHDomain().split(":");
            String ip = ipSplit[0];
            int port = Integer.parseInt(ipSplit[1]);
            if (SecondVPN.isHTTPDirect()) {
                useProxy = false;
                String customPayloadDirect = SecondVPN.getPayloadKey();
                if (customPayloadDirect != null) {

                    try {
                        ProxyData proxyData = new HttpProxyCustom(ip, port, null, null, customPayloadDirect, true, mContext);
                        conn.setProxyData(proxyData);

                    } catch (Exception e) {
                        throw new Exception("Invalid Payload!");
                    }
                } else {
                    useProxy = false;
                }
            } else {
                useProxy = true;
            }

        } else if (ModoConexao.equals("MODO_HTTPS")) {
            String[] ipSplit = SecondVPN.getServidorSSHDomain().split(":");
            String ip = ipSplit[0];
            int port = Integer.parseInt(ipSplit[1]);

            Log.d(TAG, "Is HTTPS Proxy");
            if (SecondVPN.isPayloadAfterTLS()) {
                useProxy = false;
            } else {
                useProxy = false;
                String customSNI = SecondVPN.getSNI();
                //VERSÃO TLS
                String TLSVersion = SecondVPN.getTLSVersion();
                String sshServer = ip;
                int sshPort = port;
                try {
                    ProxyData sslTypeData = new SSLTunnelProxy(sshServer, sshPort, customSNI, TLSVersion, mContext);
                    conn.setProxyData(sslTypeData);
                } catch (Exception e) {
                    AppLogManager.addLog(e.getMessage());
                }
            }

        }


    }

    /**
     * Autenticação
     */

    private static final String AUTH_PUBLICKEY = "publickey",
            AUTH_PASSWORD = "password", AUTH_KEYBOARDINTERACTIVE = "keyboard-interactive";

    protected void authentication(String usuario, String senha, String keyPath) throws IOException {
        if (!mConnected) {
            throw new IOException();
        }

        ////AppLogManager.updateStateString(//AppLogManager.SSH_AUTENTICANDO, mContext.getString(R.string.state_auth));

        try {
            if (mConnection.isAuthMethodAvailable(usuario,
                    AUTH_PASSWORD)) {

                AppLogManager.addLog(mContext.getString(R.string.validating_acess));

                if (mConnection.authenticateWithPassword(usuario, senha)) {
                    AppLogManager.addLog(mContext.getString(R.string.state_auth_success));
                }

            }
        } catch (IllegalStateException e) {
            Log.e(TAG,
                    "Connection went away while we were trying to authenticate",
                    e);
        } catch (Exception e) {
            Log.e(TAG, "Problem during handleAuthentication()", e);
        }

        try {
            if (mConnection.isAuthMethodAvailable(usuario,
                    AUTH_PUBLICKEY) && keyPath != null && !keyPath.isEmpty()) {
                File f = new File(keyPath);
                if (f.exists()) {
                    if (senha.equals("")) senha = null;

                    AppLogManager.addLog(mContext.getString(R.string.autenticating_public_key));

                    if (mConnection.authenticateWithPublicKey(usuario, f,
                            senha)) {
                        AppLogManager.addLog("<strong>" + mContext.getString(R.string.state_auth_success) + "</strong>");
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Host does not support 'Public key' authentication.");
        }


        if (!mConnection.isAuthenticationComplete()) {
            AppLogManager.addLog("<strong></font><font color=#FF0000>" + mContext.getString(R.string.login_error) + "</strong>");
            //stopAll();

            throw new IOException("Incorrect username or password");
        }
    }


    @Override
    public void log(int level, String className, String message) {

    }

    /**
     * The actual verifier method, it will be called by the key exchange code
     * on EVERY key exchange - this can happen several times during the lifetime
     * of a connection.
     * <p>
     * Note: SSH-2 servers are allowed to change their hostkey at ANY time.
     *
     * @param hostname               the hostname used to create the {@link Connection} object
     * @param port                   the remote TCP port
     * @param serverHostKeyAlgorithm the public key algorithm
     * @param serverHostKey          the server's public key blob
     * @return if the client wants to accept the server's host key - if not, the
     * connection will be closed.
     * @throws Exception Will be wrapped with an IOException, extended version of returning false =)
     */
    @Override
    public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {
        String fingerPrint = KnownHosts.createHexFingerprint(
                serverHostKeyAlgorithm, serverHostKey);
        AppLogManager.addLog(R.string.service_connect_server);
        AppLogManager.addLog("Fingerprint: " + fingerPrint);
        AppLogManager.addLog("Using algorithm: " + serverHostKeyAlgorithm);
        //AppLogManager.addLog("Algorithm key type: " + serverHostKey);
        return true;
    }

    /**
     * This method is called after the connection's underlying
     * socket has been closed. E.g., due to the {@link Connection#close()} request of the
     * user, if the peer closed the connection, due to a fatal error during connect()
     * (also if the socket cannot be established) or if a fatal error occured on
     * an established connection.
     * <p>
     * This is an experimental feature.
     * <p>
     * You MUST NOT make any assumption about the thread that invokes this method.
     * <p>
     * <b>Please note: if the connection is not connected (e.g., there was no successful
     * connect() call), then the invocation of {@link Connection#close()} will NOT trigger
     * this method.</b>
     *
     * @param reason Includes an indication why the socket was closed.
     * @see Connection#addConnectionMonitor(ConnectionMonitor)
     */
    @Override
    public void connectionLost(Throwable reason) {
        if (mStarting /*|| mStopping*/ || mReconnecting) {
            return;
        }
        //BYPASS PRA RECONECTAR
        bypassToInject = true;


        if (reason != null || !isToStopService) {
            //LOG CONEXÃO PERDIDA
            if (SecondVPN.getIsCustomFileIsLocked()) {
                String reason_rep = Objects.requireNonNull(reason.getMessage());
                if (!mStopping) {
                    AppLogManager.addLog("<strong></font><font color=#ff8c00>Connection Lost: </strong><strong>" + reason_rep.replaceAll(servidor, "*").replaceAll(servidor_domain, "*") + "</strong>");
                }

            } else {
                if (!mStopping) {
                    AppLogManager.addLog("<strong></font><font color=#ff8c00>Connection Lost: </strong><strong>" + Objects.requireNonNull(reason.getMessage()) + "</strong>");
                }
            }
            //DELAY RECONNECT
            if (!mStopping) {
                try {
                    AppLogManager.addLog("<strong><font color=\"#ff8c00\">" + mContext.getString(R.string.state_reconnecting) + "...</strong>");
                    Thread.sleep(2000); //1200 ms
                } catch (InterruptedException e) {
                }
                //RECONECTA
                reconnectSSH();
            }

        } else {
            stopAll();
        }

    }

    private boolean no_network_showed = false;
    private boolean reconnectNotificationIsStarted = false;

    public void reconnectSSH() {
        if (!isToStopService) {
            bypassToInject = true;
            if (mStarting || mStopping || mReconnecting) {
                return;
            }
            mReconnecting = true;

            //RECONNECT NOTIFICATION
            if (!reconnectNotificationIsStarted) {
                if (SecondVPN.isEnableNotification()) {
                    reconnectNotificationIsStarted = true;
                    Intent startNotification = new Intent(mContext, NotificationService.class);
                    startNotification.putExtra("TITLE", mContext.getString(R.string.app_name));
                    startNotification.putExtra("BODY", mContext.getString(R.string.state_nonetwork));
                    startNotification.putExtra("IS_CONNECTED", false);
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mContext.startForegroundService(startNotification);
                        } else {
                            mContext.startService(startNotification);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }


            try {
                stopMultiStatusInjectThread();
                if (mInjectThread != null) {
                    mInjectThread.interrupt();
                }

                if (SecondVPN.getConnectionMode().equals("MODO_HTTP") && !SecondVPN.isHTTPDirect()) {
                    mInjectThread = new Thread(newinjectTh, "mInjectThread");
                    mInjectThread.start();
                }

                if (SecondVPN.getConnectionMode().equals("MODO_HTTPS") && SecondVPN.isPayloadAfterTLS()) {
                    mInjectThread = new Thread(newinjectTh, "mInjectThread");
                    mInjectThread.start();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

            closeSSH2();

            try {
                //Thread.sleep(800); //50 ms
                Thread.sleep(2000); //50 ms
            } catch (InterruptedException e) {
                mReconnecting = false;
                return;
            }

            for (int i = 0; i < RECONNECT_TRIES; i++) {
                if (mStopping) {
                    mReconnecting = false;
                    return;
                }

                int sleepTime = 3;
                //boolean isWaitingNetworkShowed = false;
                if (!TunnelUtils.isNetworkOnline(mContext)) {
                    // //AppLogManager.updateStateString(//AppLogManager.SSH_AGUARDANDO_REDE, "Aguardando rede..");


                    if (!no_network_showed) {
                        no_network_showed = true;
                        AppLogManager.addLog(R.string.state_nonetwork);

                        //RECONNECT NOTIFICATION
                        if (!reconnectNotificationIsStarted) {
                            if (SecondVPN.isEnableNotification()) {
                                reconnectNotificationIsStarted = true;
                                Intent startNotification = new Intent(mContext, NotificationService.class);
                                startNotification.putExtra("TITLE", mContext.getString(R.string.app_name));
                                startNotification.putExtra("BODY", mContext.getString(R.string.state_nonetwork));
                                startNotification.putExtra("IS_CONNECTED", false);
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        mContext.startForegroundService(startNotification);
                                    } else {
                                        mContext.startService(startNotification);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    }


                } else {
                    no_network_showed = false;
                    // isWaitingNetworkShowed = false;
                    sleepTime = 3;
                    mStarting = true;
                    ////AppLogManager.updateStateString(//AppLogManager.SSH_RECONECTANDO, "Reconectando..");
                    //AppLogManager.addLog("<strong>" + mContext.getString(R.string.state_reconnecting) + " SSH</strong>");

                    try {
                        if (!isToStopService) {
                            if (isRunning) {
                                try {
                                    startSSH();
                                    mStarting = false;
                                    mReconnecting = false;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    stopAll();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }

                        } else {
                            //PARA TUDO
                            try {
                                stopAll();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        //mConnected = true;

                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    mStarting = false;
                }


                try {
                    Thread.sleep(sleepTime * 1000);
                    i--;
                } catch (InterruptedException e2) {
                    mReconnecting = false;
                    return;
                }
            }

            mReconnecting = false;
            stopAll();
        } else {
            //PARA TUDO
            try {
                stopAll();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    @Override
    public void onReceiveInfo(int infoId, String infoMsg) {
        if (infoId == SERVER_BANNER) {
            AppLogManager.addLog("<strong>" + "<b>" + mContext.getString(R.string.server_message) + "</b>" + "</strong> " + infoMsg);
        }

        bypassToInject = false;
    }

    /**
     * This callback interface is used during a "keyboard-interactive"
     * authentication. Every time the server sends a set of challenges (however,
     * most often just one challenge at a time), this callback function will be
     * called to give your application a chance to talk to the user and to
     * determine the response(s).
     * <p>
     * Some copy-paste information from the standard: a command line interface
     * (CLI) client SHOULD print the name and instruction (if non-empty), adding
     * newlines. Then for each prompt in turn, the client SHOULD display the
     * prompt and read the user input. The name and instruction fields MAY be
     * empty strings, the client MUST be prepared to handle this correctly. The
     * prompt field(s) MUST NOT be empty strings.
     * <p>
     * Please refer to draft-ietf-secsh-auth-kbdinteract-XX.txt for the details.
     * <p>
     * Note: clients SHOULD use control character filtering as discussed in
     * RFC4251 to avoid attacks by including
     * terminal control characters in the fields to be displayed.
     *
     * @param name        the name String sent by the server.
     * @param instruction the instruction String sent by the server.
     * @param numPrompts  number of prompts - may be zero (in this case, you should just
     *                    return a String array of length zero).
     * @param prompt      an array (length <code>numPrompts</code>) of Strings
     * @param echo        an array (length <code>numPrompts</code>) of booleans. For
     *                    each prompt, the corresponding echo field indicates whether or
     *                    not the user input should be echoed as characters are typed.
     * @return an array of reponses - the array size must match the parameter
     * <code>numPrompts</code>.
     * @throws Exception exception
     */
    @Override
    public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) throws Exception {
        return new String[0];
    }


    @Override
    public void run() {

        //SETA TUNNEL COMO NAO DESTROIDO
        tunFdDestroyLog = false;
        //PARA TODAS THREADS DOA PP
        stopAllThreads();

        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!isToStopService) {

            //NOTIFICAÇÃO VPN RODANDO
            if (SecondVPN.isEnableNotification()) {
                Intent startNotification = new Intent(mContext, NotificationService.class);
                startNotification.putExtra("TITLE", mContext.getString(R.string.app_name));
                startNotification.putExtra("BODY", mContext.getString(R.string.starting_notification));
                startNotification.putExtra("IS_CONNECTED", false);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mContext.startForegroundService(startNotification);
                    } else {
                        mContext.startService(startNotification);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            isServiceRunning = true;
            try {
                connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                bypassToInject = true;
                startFakeTunnelVpnService();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mStarting = true;
            mTunnelThreadStopSignal = new CountDownLatch(1);

            int tries = 0;
            while (!mStopping) {

                try {
                    if (!TunnelUtils.isNetworkOnline(mContext)) {
                        //AppLogManager.updateStateString(//AppLogManager.SSH_AGUARDANDO_REDE, mContext.getString(R.string.state_nonetwork));
                        AppLogManager.addLog(R.string.state_nonetwork);

                        try {
                            //Thread.sleep(500);
                            Thread.sleep(2000);
                        } catch (InterruptedException e2) {
                            stopAll();
                            break;
                        }
                    } else {
                        //BOTÃO INICIAR
                        try {
                            MainActivity.setButtonStatus("INICIANDO");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //INFO DA REDE
                        try {
                            AppLogManager.addLog(mContext.getString(R.string.current_network) + " " + TunnelUtils.getOperatorName(mContext) + " | " + TunnelUtils.getAPN(mContext));
                            String ipAddress = TunnelUtils.getLocalIpAddress();
                            String[] ipAddresses = ipAddress.split("\n");
                            for (String ip : ipAddresses) {
                                AppLogManager.addLog(mContext.getString(R.string.local_ip) + " " + ip);
                            }


                        } catch (Exception e) {
                            AppLogManager.addLog(String.valueOf(e));

                            e.printStackTrace();
                        }

                        //INICIANDO TUNNEL
                        AppLogManager.addLog(mContext.getString(R.string.starting_tunnel_service));

                        //WAKELOCK
                        if (SecondVPN.isEnableWakeLock()) {
                            try {
                                setWakelock();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (tries > 0)
                            AppLogManager.addLog("<strong><font color=\"#ff8c00\">" + mContext.getString(R.string.state_reconnecting) + "...</strong>");

                        try {
                            Thread.sleep(100); //500 original
                            //Thread.sleep(1500); //500 original
                        } catch (InterruptedException e2) {
                            stopAll();
                            break;
                        }


                        if (SecondVPN.getConnectionMode().equals("MODO_HTTP") && !SecondVPN.isHTTPDirect()) {
                            startMultiStatusInject();
                        }
                        if (SecondVPN.getConnectionMode().equals("MODO_HTTPS") && SecondVPN.isPayloadAfterTLS()) {
                            startMultiStatusInject();
                        }
                        if (!isToStopService) {
                            startSSH();
                            mStarting = false;
                            mReconnecting = false;
                        } else {
                            //PARA TUDO
                            try {
                                stopAll();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        break;
                    }
                } catch (Exception e) {
                    closeSSH2();

                    try {
                        //Thread.sleep(500); //500 original
                        Thread.sleep(2000); //500 original
                    } catch (InterruptedException e2) {
                        stopAll();
                        break;
                    }
                }

                tries++;
            }

            mStarting = false;

            if (!mStopping) {
                try {
                    mTunnelThreadStopSignal.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }/*else{
            isServiceRunning = false;
            stopAll(); //PARA O SERVICO


        }*/

        while (!Thread.interrupted()) {
            // Se você decidir parar o serviço, chame stopSelf()
            if (isToStopService) {
                isServiceRunning = false;
                stopAll(); //PARA O SERVICO
                mStopping = true;
                break;
            }

            // Lógica contínua do serviço VPN
            // ...
        }


    }

    public void stopAllThreads() {
        //PARA TUDO ANTES DE INICIAR
        try {
            if (mInjectThread != null) {
                //SE NÃO BUGAR NÉ (THREADS INJEÇAO)
                mInjectThread.interrupt();
                mInjectThread.stop();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //SE NÃO BUGAR NÉ (PDNSD)
            if (pdnsdProcess != null) {
                pdnsdProcess.destroy();
                mPdnsd.interrupt();
                mPdnsd.stop();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            tun2socksThread.interrupt();
            mTun2Socks.interrupt();
            tun2socksThread.stop();
            mTun2Socks.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //SE NÃO BUGAR NÉ (TUNNEL)
            if (tunFd != null) {
                tunFd.close();
                tunFd.detachFd();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //SE NÃO BUGAR NÉ (SOCKETS)
            if (output != null) {
                output.close();
                listen_socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //SETA TUDO NULL
        tunFd = null;
        output = null;
        listen_socket = null;
        mTun2Socks = null;
        mInjectThread = null;

        try {
            Thread.sleep(500);
            //Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String privateIpAddress;
    private int prefixLength;
    private java.lang.Process pdnsdProcess;
    public static boolean isRunning = false;
    public static final String LOCAL_SERVER_ADDRESS = "127.0.0.1";
    private String mRouter;
    private int mMtu = 1500;
    private static final String DNS = "DNSCUSTOM";
    private VpnUtils.PrivateAddress mPrivateAddress;
    public ParcelFileDescriptor tunFd;
    private Tun2Socks mTun2Socks;
    private Pdnsd mPdnsd;
    private NetworkSpace mRoutes;
    private NetworkSpace mRoutesv6;
    private boolean isBypass = false;

    private Thread tun2socksThread = null;

    static {
        System.loadLibrary("tun2socks");
    }


    public synchronized boolean establishVpn() {
        //addLog("Starting Injector VPN Service");
        try {
            Locale.setDefault(Locale.ENGLISH);

            VpnService.Builder builder = new VpnService.Builder();
            builder.addAddress(mPrivateAddress.mIpAddress, mPrivateAddress.mPrefixLength);
            String release = Build.VERSION.RELEASE;
            if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                    && !release.startsWith("4.4.4") && !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                    && mMtu < 1280) {
                mMtu = 1280;
            }

            builder.setMtu(mMtu);
            mRoutes.addIP(new CIDRIP("0.0.0.0", 0), true);
            mRoutes.addIP(new CIDRIP("10.0.0.0", 8), false);

            mRoutes.addIP(new CIDRIP(mPrivateAddress.mSubnet, mPrivateAddress.mPrefixLength), true);

            /**
             * @param  GetIPV6Mask(hostString)
             * @see  hostString
             *author: staffnetDev github
             *The provided code snippet checks if the hostString contains a colon (:), which indicates that it is an IPv6 address. If it is an IPv6 address, it initializes Inet6Address and Inet4Address variables to null. It then iterates over all the addresses returned by InetAddress.getAllByName(hostString) and assigns the appropriate address to either ipv6 or ipv4.
             * If an IPv4 address is found (ipv4 is not null), it adds this address to the mRoutes with a subnet mask of 32.
             * If an IPv6 address is found (ipv6 is not null), it calculates the mask using the GetIPV6Mask method if the hostString contains a subnet mask, otherwise, it defaults to 128. It then adds this IPv6 address to mRoutesv6.
             * If the hostString does not contain a colon, it is
             * */
            if (!isBypass) {
                String hostString = TunnelManager.currentIPAddr;


                if(hostString.contains(":")) {

                    Inet6Address ipv6 = null;
                    Inet4Address ipv4 = null;

                    for(InetAddress addr : InetAddress.getAllByName(hostString)) {
                        if(addr instanceof Inet6Address)
                            ipv6 = (Inet6Address)addr;
                        if(addr instanceof Inet4Address)
                            ipv4 = (Inet4Address)addr;
                    }

                    if (ipv4 != null) {
                        mRoutes.addIP(new CIDRIP(ipv4.getHostAddress(), 32), false);
                    }


                    if (ipv6 !=null){
                        int mask = (hostString.contains("/")) ? GetIPV6Mask(hostString)  : 128;
                        mRoutesv6.addIPv6(ipv6, mask, true);
                    }

                }else

                    mRoutes.addIP(new CIDRIP(hostString, 32), false);


            } else {
                mRoutes.addIP(new CIDRIP("192.198.0.1", 32), false);
            }


            //Commented because no data is generated when connecting to the VPN


            boolean allowUnsetAF = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;



            if (allowUnsetAF) {
                /**
                 * Disable this code to allow all traffic to pass through the VPN
                 * @param allowAllAFFamilies(builder);
                 * */
                if (!isBypass) {
                    setAllowedVpnPackages(builder);
                }

            }

            // Add Dns
            String[] dnsResolver = m_dnsResolvers;

            for (String dns : dnsResolver) {
                try {
                    // String dns2 = "208.67.222.123";
                    if (dns.contains(":")) {
                        builder.addDnsServer("1.1.1.1");
                        //mRoutes.addIP(new CIDRIP("1.1.1.1", 32), true);
                    } else {
                        builder.addDnsServer(dns);
                        //mRoutes.addIP(new CIDRIP(dns, 32), true);
                    }
                } catch (IllegalArgumentException iae) {
                    if (!isBypass) {
                        AppLogManager.addLog(
                                String.format(
                                        "DNS error: <br> %s, %s",
                                        dns,
                                        iae.getLocalizedMessage()
                                )
                        );
                    }
                }
            }

            boolean subroute = false;
            boolean tethering = false;
            boolean lan = false;

            if (subroute) {
                // Exclude IP ranges
                List<IPUtil.CIDR> listExclude = new ArrayList<>();
                listExclude.add(new IPUtil.CIDR("127.0.0.0", 8)); // localhost

                if (tethering) {

                    listExclude.add(new IPUtil.CIDR("127.0.0.0", 8)); // localhost

                    // USB tethering 192.168.42.x
                    // Wi-Fi tethering 192.168.43.x
                    listExclude.add(new IPUtil.CIDR("192.168.42.0", 23));
                    // Bluetooth tethering 192.168.44.x
                    listExclude.add(new IPUtil.CIDR("192.168.44.0", 24));
                    // Wi-Fi direct 192.168.49.x
                    listExclude.add(new IPUtil.CIDR("192.168.49.0", 24));
                }

                if (lan) {
                    try {
                        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                        while (nis.hasMoreElements()) {
                            NetworkInterface ni = nis.nextElement();
                            if (ni != null && ni.isUp() && !ni.isLoopback() &&
                                    ni.getName() != null && !ni.getName().startsWith("tun"))
                                for (InterfaceAddress ia : ni.getInterfaceAddresses())
                                    if (ia.getAddress() instanceof Inet4Address) {
                                        IPUtil.CIDR local = new IPUtil.CIDR(ia.getAddress(), ia.getNetworkPrefixLength());
                                        addLog("Excluding " + ni.getName() + " " + local);
                                        listExclude.add(local);
                                    }
                        }
                    } catch (SocketException ex) {
                        // Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                    }
                }

                // https://en.wikipedia.org/wiki/Mobile_country_code
                Configuration config = mContext.getResources().getConfiguration();

                // T-Mobile Wi-Fi calling
                if (config.mcc == 310 && (config.mnc == 160 ||
                        config.mnc == 200 ||
                        config.mnc == 210 ||
                        config.mnc == 220 ||
                        config.mnc == 230 ||
                        config.mnc == 240 ||
                        config.mnc == 250 ||
                        config.mnc == 260 ||
                        config.mnc == 270 ||
                        config.mnc == 310 ||
                        config.mnc == 490 ||
                        config.mnc == 660 ||
                        config.mnc == 800)) {
                    listExclude.add(new IPUtil.CIDR("66.94.2.0", 24));
                    listExclude.add(new IPUtil.CIDR("66.94.6.0", 23));
                    listExclude.add(new IPUtil.CIDR("66.94.8.0", 22));
                    listExclude.add(new IPUtil.CIDR("208.54.0.0", 16));
                }

                // Verizon wireless calling
                if ((config.mcc == 310 &&
                        (config.mnc == 4 ||
                                config.mnc == 5 ||
                                config.mnc == 6 ||
                                config.mnc == 10 ||
                                config.mnc == 12 ||
                                config.mnc == 13 ||
                                config.mnc == 350 ||
                                config.mnc == 590 ||
                                config.mnc == 820 ||
                                config.mnc == 890 ||
                                config.mnc == 910)) ||
                        (config.mcc == 311 && (config.mnc == 12 ||
                                config.mnc == 110 ||
                                (config.mnc >= 270 && config.mnc <= 289) ||
                                config.mnc == 390 ||
                                (config.mnc >= 480 && config.mnc <= 489) ||
                                config.mnc == 590)) ||
                        (config.mcc == 312 && (config.mnc == 770))) {
                    listExclude.add(new IPUtil.CIDR("66.174.0.0", 16)); // 66.174.0.0 - 66.174.255.255
                    listExclude.add(new IPUtil.CIDR("66.82.0.0", 15)); // 69.82.0.0 - 69.83.255.255
                    listExclude.add(new IPUtil.CIDR("69.96.0.0", 13)); // 69.96.0.0 - 69.103.255.255
                    listExclude.add(new IPUtil.CIDR("70.192.0.0", 11)); // 70.192.0.0 - 70.223.255.255
                    listExclude.add(new IPUtil.CIDR("97.128.0.0", 9)); // 97.128.0.0 - 97.255.255.255
                    listExclude.add(new IPUtil.CIDR("174.192.0.0", 9)); // 174.192.0.0 - 174.255.255.255
                    listExclude.add(new IPUtil.CIDR("72.96.0.0", 9)); // 72.96.0.0 - 72.127.255.255
                    listExclude.add(new IPUtil.CIDR("75.192.0.0", 9)); // 75.192.0.0 - 75.255.255.255
                    listExclude.add(new IPUtil.CIDR("97.0.0.0", 10)); // 97.0.0.0 - 97.63.255.255
                }

                // Broadcast
                listExclude.add(new IPUtil.CIDR("224.0.0.0", 3));

                Collections.sort(listExclude);

                try {
                    InetAddress start = InetAddress.getByName("0.0.0.0");
                    for (IPUtil.CIDR exclude : listExclude) {
                        addLog("Exclude " + exclude.getStart().getHostAddress() + ", " + exclude.getEnd().getHostAddress());
                        for (IPUtil.CIDR include : IPUtil.toCIDR(start, IPUtil.minus1(exclude.getStart())))
                            try {
                                builder.addRoute(include.address, include.prefix);
                            } catch (Throwable ex) {
                                // Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                            }
                        start = IPUtil.plus1(exclude.getEnd());
                    }
                    String end = (lan ? "255.255.255.254" : "255.255.255.255");
                    for (IPUtil.CIDR include : IPUtil.toCIDR("224.0.0.0", end))
                        try {
                            builder.addRoute(include.address, include.prefix);
                        } catch (Throwable ex) {
                            // Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                        }
                } catch (UnknownHostException ex) {
                    // Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                }
            }

            NetworkSpace.IpAddress multicastRange = new NetworkSpace.IpAddress(new CIDRIP("224.0.0.0", 3), true);

            for (NetworkSpace.IpAddress route : mRoutes.getPositiveIPList()) {
                try {
                    if (multicastRange.containsNet(route)) {
                        ////AppLogManager.logDebug("VPN: Ignorando rota multicast: " + route.toString());
                    } else {
                        builder.addRoute(route.getIPv4Address(), route.networkMask);
                    }

                } catch (IllegalArgumentException ia) {
                    //mHostService.onDiagnosticMessage("Rota rejeitada: " + route + " " + ia.getLocalizedMessage());
                }
            }

            for (NetworkSpace.IpAddress route6 : mRoutesv6.getPositiveIPList()) {
                try {
                    builder.addRoute(route6.getIPv6Address(), route6.networkMask);
                } catch (IllegalArgumentException ia) {
                    //SkStatus.logInfo("Rejected routes: " + route + " " + ia.getLocalizedMessage());
                }
            }

            if (!isBypass) {
                try {
                    String checkV6 = TextUtils.join(", ", mRoutesv6.getNetworks(false));
                    if (SecondVPN.getIsCustomFileIsLocked()) {
                        addLog("Routes: " + TextUtils.join(", ", mRoutes.getNetworks(true)).replaceFirst(TunnelManager.currentIPAddr, "*"));
                        addLog("Routes excluded (IPv4): " + TextUtils.join(", ", mRoutes.getNetworks(false)).replaceFirst(TunnelManager.currentIPAddr, "*"));
                        if (checkV6 != "") {
                            addLog("Routes excluded (IPv6): " + TextUtils.join(", ", mRoutesv6.getNetworks(false)).replaceFirst(TunnelManager.currentIPAddr, "*"));
                        }
                    } else {
                        addLog("Routes: " + TextUtils.join(", ", mRoutes.getNetworks(true)));
                        addLog("Routes excluded (IPv4): " + TextUtils.join(", ", mRoutes.getNetworks(false)));
                        if (checkV6 != "") {
                            addLog("Routes excluded (IPv6):" + TextUtils.join(", ", mRoutesv6.getNetworks(false)));
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            // addLog("Routes installed: " + TextUtils.join(", ", positiveIPv4Routes));

            if (!isBypass) {
                if (SecondVPN.isEnableTethering()) {
                    StartTethering();
                }

            }

            if (isBypass) {
                builder.addDisallowedApplication(mContext.getPackageName());
            }


            tunFd = builder
                    .setSession(getApplicationName())
                    //.setConfigureIntent(SSHCoreService.getGraphPendingIntent(this))
                    .establish();


            String m_socksServerAddress = String.format("127.0.0.1:%s", 1080);
            String m_udpResolver = SecondVPN.isEnableCustomUDP() ? SecondVPN.getUDPResolver() : null;

            if (m_udpResolver != null && !m_udpResolver.matches("^\\d{1,3}(\\.\\d{1,3}){3}:\\d+$")) {
                m_udpResolver = null;
            }

            connectTunnel(
                    m_socksServerAddress,
                    m_udpResolver
            );

            mRoutes.clear();

            return tunFd != null;
        } catch (Exception e) {
            AppLogManager.addLog("Failed to establish the VPN " + e);
            return false;
        }
    }

    private static final String VPN_INTERFACE_NETMASK = "255.255.255.0";
    private static final int DNS_RESOLVER_PORT = 53;
    boolean transparentDns = !SecondVPN.isEnableCustomDNS();

    public synchronized void connectTunnel(
            final String socksServerAddress,
            String m_udpResolver

    ) {
        if (socksServerAddress == null) {
            throw new IllegalArgumentException("Must provide an IP address to a SOCKS server.");
        }
        if (tunFd == null) {
            throw new IllegalStateException("Must establish the VPN before connecting the tunnel.");
        }
        if (tun2socksThread != null) {
            throw new IllegalStateException("Tunnel already connected");
        }

        isRunning = true;

        if (isBypass) {
            return;
        }

        String dnsgwRelay;
        int pdnsdPort = VpnUtils.findAvailablePort(8091, 10);

        String[] mServidorDNS = m_dnsResolvers;

        dnsgwRelay =
                String.format("%s:%d", mPrivateAddress.mIpAddress, pdnsdPort);

        mPdnsd =
                new Pdnsd(
                        mContext,
                        mServidorDNS,
                        DNS_RESOLVER_PORT,
                        mPrivateAddress.mIpAddress,
                        pdnsdPort
                );

        String finalDnsgwRelay = dnsgwRelay;
        mPdnsd.setOnPdnsdListener(
                new Pdnsd.OnPdnsdListener() {
                    @Override
                    public void onStart() {
                        if (!isBypass) {
                            AppLogManager.addLog("DNS relay: " + finalDnsgwRelay);

                        }
                        //addLog("pdnsd started");
                    }

                    @Override
                    public void onStop() {
                        //addLog("pdnsd stopped");
                        //stop();
                    }
                }
        );

        mPdnsd.start();

        // Tun2socks
        mTun2Socks =
                new Tun2Socks(
                        mContext,
                        tunFd,
                        mMtu,
                        mPrivateAddress.mRouter,
                        VPN_INTERFACE_NETMASK,
                        socksServerAddress,
                        m_udpResolver,
                        dnsgwRelay,
                        transparentDns
                );

        mTun2Socks.setOnTun2SocksListener(
                new Tun2Socks.OnTun2SocksListener() {
                    @Override
                    public void onStart() {
                        if (!isBypass) {
                            AppLogManager.addLog("Socks local: " + socksServerAddress);
                        }
                    }

                    @Override
                    public void onStop() {
                        //addLog("tun2socks stopped");
                        //stop();
                    }
                }
        );

        mTun2Socks.start();

        //EXIBE DNS DA REDE
        if (!isBypass) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //VPN OK
        if (!isBypass) {
            addLog("<b><font color=#49C53C>Tunnel VPN Conectado!</font></b>");
            showCurrentDNS();
        }

    }

    //
    /* Disconnects a tunnel created by a previous call to |connectTunnel|. */
    private boolean tunFdDestroyLog = false;

    public synchronized void disconnectTunnel() {
        //DELAY DESTRUIR TUNNEL
        try {
            Thread.sleep(300); //300 original
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (!tunFdDestroyLog) {
                if (!vpnDestroyedLog) {
                    AppLogManager.addLog("<strong>" + mContext.getString(R.string.vpn_destroyed) + "</strong>");
                    vpnDestroyedLog = true;
                }

                tunFdDestroyLog = true;
            }

            //tunFd.detachFd();//SE NÃO BUGAR NÉ
            tunFd.close();
            tunFd = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (mTun2Socks != null && mTun2Socks.isAlive()) {
                mTun2Socks.interrupt();
            }

            mTun2Socks = null;


            if (mPdnsd != null && mPdnsd.isAlive()) {
                mPdnsd.interrupt();
            }

            mPdnsd = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (pdnsdProcess != null) {
            pdnsdProcess.destroy();
            pdnsdProcess = null;
            //addLog("pdnsd stopped");
        }

        try {
            if (tun2socksThread != null) {
                //  Tun2Socks.Stop();
                tun2socksThread.join();
                tun2socksThread = null;
                //addLog("tun2socks stopped");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        isRunning = false;

    }

    public static String b(Context context, int i) {
        Scanner useDelimiter = new Scanner(context.getResources().openRawResource(i), "UTF-8").useDelimiter("\\A");
        StringBuilder stringBuilder = new StringBuilder();
        while (useDelimiter.hasNext()) {
            stringBuilder.append(useDelimiter.next());
        }
        useDelimiter.close();
        return stringBuilder.toString();
    }

    private void allowAllAFFamilies(VpnService.Builder builder) {

        builder.allowFamily(OsConstants.AF_INET);
        builder.allowFamily(OsConstants.AF_INET6);
    }


    /**
     * author: staffnetDev git
     * This method configures the VPN service by setting the allowed VPN packages.
     * It adds the packages that are disallowed from using the VPN.
     *
     * @param builder The VpnService.Builder instance used to configure the VPN.
     * @see #setAllowedVpnPackages(VpnService.Builder)
     */
    private void setAllowedVpnPackages(VpnService.Builder builder) {
        Set<String> excludedApps;
        excludedApps = SecondVPN.app_prefs.getStringSet("selectedApps", new HashSet<>());

        for (int i = 0; i < excludedApps.size(); i++) {
            try {
                if (!excludedApps.toArray()[i].toString().equals(mContext.getPackageName())) {
                    builder.addDisallowedApplication(excludedApps.toArray()[i].toString());
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Log the error if the package name is not found
                AppLogManager.addLog("<strong></font><font color=red>" + mContext.getString(R.string.app_no_longer_exists) + " </strong>" + excludedApps.toArray()[i].toString());
            }
        }
    }

    private List<String> getActiveNetworkDnsResolver(Context context) throws Exception {
        Collection<InetAddress> dnsResolvers = getActiveNetworkDnsResolvers(context);

        if (!dnsResolvers.isEmpty()) {
            ArrayList<String> lista = new ArrayList<String>();
            int max = 2;

            Iterator it = dnsResolvers.iterator();
            while (it.hasNext()) {
                String dnsResolver = it.next().toString();

                // strip the leading slash e.g., "/192.168.1.1"
                if (dnsResolver.startsWith("/")) {
                    dnsResolver = dnsResolver.substring(1);
                }

                // remove ipv6 ips
                if (dnsResolver.contains(":")) {
                    continue;
                }

                lista.add(dnsResolver);

                max -= 1;
                if (max <= 0) break;
            }

            return lista;
        } else throw new Exception("no active network DNS resolver");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Collection<InetAddress> getActiveNetworkDnsResolvers(Context context)
            throws Exception {
        final String errorMessage = "getActiveNetworkDnsResolvers failed";
        ArrayList<InetAddress> dnsAddresses = new ArrayList<InetAddress>();
        try {
            // Hidden API
            // - only available in Android 4.0+
            // - no guarantee will be available beyond 4.2, or on all vendor devices
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Class<?> LinkPropertiesClass = Class.forName("android.net.LinkProperties");
            Method getActiveLinkPropertiesMethod = ConnectivityManager.class.getMethod("getActiveLinkProperties", new Class[]{});
            Object linkProperties = getActiveLinkPropertiesMethod.invoke(connectivityManager);
            if (linkProperties != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Method getDnsesMethod = LinkPropertiesClass.getMethod("getDnses", new Class[]{});
                    Collection<?> dnses = (Collection<?>) getDnsesMethod.invoke(linkProperties);
                    for (Object dns : dnses) {
                        dnsAddresses.add((InetAddress) dns);
                    }
                } else {
                    // LinkProperties is public in API 21 (and the DNS function signature has changed)
                    for (InetAddress dns : ((LinkProperties) linkProperties).getDnsServers()) {
                        dnsAddresses.add(dns);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new Exception(errorMessage, e);
        } catch (NoSuchMethodException e) {
            throw new Exception(errorMessage, e);
        } catch (IllegalArgumentException e) {
            throw new Exception(errorMessage, e);
        } catch (IllegalAccessException e) {
            throw new Exception(errorMessage, e);
        } catch (InvocationTargetException e) {
            throw new Exception(errorMessage, e);
        } catch (NullPointerException e) {
            throw new Exception(errorMessage, e);
        }

        return dnsAddresses;
    }


    public synchronized String getLocalServerAddress(String port) throws IllegalStateException {
        return String.format(Locale.ROOT, "%s:%s", LOCAL_SERVER_ADDRESS, port);
    }

    void addLog(String msg) {
        if (!isBypass) {
            AppLogManager.addLog(msg);
        }

    }

    public final String getApplicationName() throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mContext.getPackageManager();
        ApplicationInfo appInfo = packageManager.getApplicationInfo(mContext.getPackageName(), 0);
        return (String) packageManager.getApplicationLabel(appInfo);
    }

    private int GetIPV6Mask(String cidr){
        if (cidr.contains("/")) {
            int index = cidr.indexOf("/");
            String networkPart = cidr.substring(index + 1);
            return Integer.parseInt(networkPart);
        } else {
            throw new IllegalArgumentException("not an valid CIDR format!");
        }
    }

}