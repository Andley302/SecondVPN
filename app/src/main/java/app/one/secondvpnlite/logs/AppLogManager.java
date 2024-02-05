package app.one.secondvpnlite.logs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Vector;

import app.one.secondvpnlite.R;

public class AppLogManager
{
	private static final LinkedList<LogItem> logbuffer;
	
	private static Vector<LogListener> logListener;
	
    
	static final int MAXLOGENTRIES = 1000;
	
	public static enum LogLevel {
		
        INFO(2),
        ERROR(-2),
        WARNING(1),
        VERBOSE(3),
        DEBUG(4);

        protected int mValue;

        LogLevel(int value) {
            mValue = value;
        }

        public int getInt() {
            return mValue;
        }

        public static LogLevel getEnumByValue(int value) {
            switch (value) {
                case 2:
                    return INFO;
                case -2:
                    return ERROR;
                case 1:
                    return WARNING;
                case 3:
                    return VERBOSE;
                case 4:
                    return DEBUG;

                default:
                    return null;
            }
        }
    }

	static {
        logbuffer = new LinkedList<>();
        logListener = new Vector<>();
		addLogrmation();
    }
	

    public synchronized static void clearLog() {
        logbuffer.clear();
		addLogrmation();
		
		for (LogListener li : logListener) {
			li.onClear();
		}
    }
	
	public synchronized static LogItem[] getlogbuffer() {

        // The stoned way of java to return an array from a vector
        // brought to you by eclipse auto complete
        return logbuffer.toArray(new LogItem[logbuffer.size()]);
    }
	
	private static void addLogrmation() {
		addLog(R.string.app_mobile_info, "");
		
	}

	
	/**
	* Listeners
	*/
	
	public interface LogListener {
        void newLog(LogItem logItem);
		void onClear();
    }


	
    public synchronized static void addLogListener(LogListener ll) {
        if (!logListener.contains(ll)) {
			logListener.add(ll);
		}
    }
    

	/**
	* NewLog
	*/
	
    static void newLogItem(LogItem logItem) {
        newLogItem(logItem, false);
    }

    synchronized static void newLogItem(LogItem logItem, boolean cachedLine) {
        if (cachedLine) {
            logbuffer.addFirst(logItem);
        } else {
            logbuffer.addLast(logItem);
        }

        if (logbuffer.size() > MAXLOGENTRIES + MAXLOGENTRIES / 2) {
            while (logbuffer.size() > MAXLOGENTRIES)
                logbuffer.removeFirst();
        }

        for (LogListener ll : logListener) {
            ll.newLog(logItem);
        }
    }

	
	/**
	* Logger static methods
	*/
	
	public static void logException(String context, Exception e) {
        logException(LogLevel.ERROR, context, e);
    }
	
	public static void logException(LogLevel ll, String context, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

		LogItem li;
		
		if (context != null)
			li = new LogItem(ll, String.format("%s: %s, %s", context, e.getMessage(), sw.toString()));
		else
			li = new LogItem(ll, String.format("Erro: %s, %s", e.getMessage(), sw.toString()));

        newLogItem(li);
    }

	public static void logException(Exception e) {
        logException(LogLevel.ERROR, null, e);
    }
	
	public static void addLog(String message) {
        newLogItem(new LogItem(LogLevel.INFO, message));
    }

    public static void logDebug(String message) {
        newLogItem(new LogItem(LogLevel.DEBUG, message));
    }

    public static void addLog(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.INFO, resourceId, args));
    }

    public static void logDebug(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.DEBUG, resourceId, args));
    }

    public static void logError(String msg) {
        newLogItem(new LogItem(LogLevel.ERROR, msg));
    }

    public static void logWarning(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.WARNING, resourceId, args));
    }

    public static void logWarning(String msg) {
        newLogItem(new LogItem(LogLevel.WARNING, msg));
    }

    public static void logError(int resourceId) {
        newLogItem(new LogItem(LogLevel.ERROR, resourceId));
    }

    public static void logError(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.ERROR, resourceId, args));
    }

}
