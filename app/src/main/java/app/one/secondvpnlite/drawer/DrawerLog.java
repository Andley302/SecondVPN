package app.one.secondvpnlite.drawer;

import android.app.Activity;
import android.os.Handler;
import android.view.View;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.one.secondvpnlite.R;

/**
* @author Skank3r
*/

public class DrawerLog
	implements LogsAdapter.OnItemClickListener
{
	private static final String TAG = DrawerLog.class.getSimpleName();
	
	private final Activity mActivity;
	private final Handler mHandler;
	private DrawerLayout drawerLayout;
	private RecyclerView drawerListView;
	private LogsAdapter mAdapter;
	
	public DrawerLog(Activity activity) {
		mActivity = activity;
		mHandler = new Handler();
	}
	
	// inicia Drawer e Toolbar
	public void setDrawer(DrawerLayout.DrawerListener listener) {

		drawerLayout = mActivity.findViewById(R.id.drawerLayout);
		drawerListView = mActivity.findViewById(R.id.recyclerDrawerView);
		
		drawerLayout.addDrawerListener(listener);
		
		LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity);
		
		mAdapter = new LogsAdapter(layoutManager, mActivity);
		mAdapter.setOnItemClickListener(this);

		drawerListView.setAdapter(mAdapter);
		drawerListView.setLayoutManager(layoutManager);
		
		mAdapter.scrollToLastPosition();
	}
	
	public DrawerLayout getDrawerLayout() {
		return drawerLayout;
	}

	public void clearLogs() {
		mAdapter.clearLog();
	}
	
	
	/**
	* Logs OnItemClickListener implementação
	*/
	
	@Override
	public void onItemClick(View view, int position, String logText) {}

	@Override
	public void onItemLongClick(View view, int position, String logText) {
		/*try {
			// copia log para clipboard
			Utils.copyToClipboard(mActivity, logText);
		} catch(Exception e) {
			AppLogManager.logException("Erro ao copiar Log", e);
			Toast.makeText(mActivity, "Não foi possível copiar log", Toast.LENGTH_SHORT)
				.show();
		}*/
	}


	/**
	* Eventos
	*/
	
	public void onResume() {
		/*if (new Settings(mActivity).getModoDebug()) {
			mAdapter.setLogLevel(4);
		}
		else {
			mAdapter.setLogLevel(3);
		}*/
		mAdapter.setLogLevel(3);
	}
	
	public void onDestroy() {
		//AppLogManager.removeLogListener(mAdapter);
	}
	
}
