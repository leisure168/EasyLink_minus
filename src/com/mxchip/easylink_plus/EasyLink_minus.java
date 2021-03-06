package com.mxchip.easylink_plus;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.util.Log;

import com.mxchip.helper.ProbeReqData;

/**
 * Created by wangchao on 6/9/15.
 */
public class EasyLink_minus {
	private static final String TAG = "EasyLink_minus";
	private Thread mCallbackThread; // call start ap after wifi enabled
	private Context mContext;
	boolean mStopTransmitting = false;
	private IntentFilter mIntentFilter = null;
	private boolean mScanning;
	private int mErrorId = 0;

	public boolean isScanning() {
		return mScanning;
	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context paramAnonymousContext, Intent intent) {
			EasyLink_minus.this.mScanning = false;
			mContext.unregisterReceiver(this);
			Log.d(TAG, "action:" + intent.getAction());
			if (intent.getAction().equals("android.net.wifi.SCAN_RESULTS")) {
				System.out.println("SCAN_RESULTS_AVAILABLE");
				EasyLink_minus.this.mScanning = false;
			}
			if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
				try {
					Parcelable localParcelable = intent
							.getParcelableExtra("networkInfo");
					if ((localParcelable != null)
							&& (!((NetworkInfo) localParcelable).isAvailable())) {
						EasyLink_minus.this.mErrorId = 102;
						EasyLink_minus.this.mScanning = false;
						EasyLink_minus.this.clearNetList();
						return;
					}
				} catch (Exception localException2) {
					localException2.printStackTrace();
					return;
				}
			}
			try {
				switch (intent.getIntExtra("wifi_state", 0)) {
				case 0:
					EasyLink_minus.this.clearNetList();
					return;
				}
			} catch (Exception localException1) {
				localException1.printStackTrace();
				return;
			}
			EasyLink_minus.this.mErrorId = 101;
			EasyLink_minus.this.mScanning = false;
			return;
		}
	};

	private List<Integer> mNetId = new ArrayList<Integer>();

	public EasyLink_minus(Context ctx, Thread t) {
		this(ctx);
		mCallbackThread = t;
	}

	public EasyLink_minus(Context ctx) {
		mContext = ctx;
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction("android.net.wifi.SCAN_RESULTS");
		mIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
		mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
		mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
	}

	private void clearNetList() {
		WifiManager localWifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		if (localWifiManager == null
				|| localWifiManager.getConfiguredNetworks() == null) {
			return;
		}
		for (WifiConfiguration localWifiConfiguration : localWifiManager
				.getConfiguredNetworks()) {
			String ssid = localWifiConfiguration.SSID.replaceAll("\"", "");
			if (!ssid.contains("#?1234"))
				// if (!ssid.matches("^#[0x00-0xff]*@[0x00-0xff]*"))
				// if (!ssid.matches("^#.*@.*"))
				continue;
			localWifiManager.removeNetwork(localWifiConfiguration.networkId);
			localWifiManager.saveConfiguration();
		}
	}

	public boolean startTransmit(String Ssid, String Key, String Userinfo) {
		Log.d(TAG, new String(Ssid));
		clearNetList();
		mIsWorking = true;
		mStopTransmitting = false;
		// String param = "#" + Userinfo + Ssid + "@" + Key;
		int ip = 1;// ---ces
		String param[] = new ProbeReqData().bgProtocol(Ssid, Key, ip);

		WifiManager localWifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		if (localWifiManager == null)
			return false;
		if (!localWifiManager.isWifiEnabled()) {
			if (!localWifiManager.setWifiEnabled(true)) {
				return false;
			}
		}
		WifiInfo localWifiInfo = localWifiManager.getConnectionInfo();
		if (localWifiInfo == null)
			return false;
		WifiConfiguration config = new WifiConfiguration();
		config.SSID = String.format("\"%s\"", new Object[] { param[1] });
		config.BSSID = null;
		config.preSharedKey = null;
		config.wepKeys = new String[4];
		config.wepTxKeyIndex = 0;
		config.priority = 50;
		config.hiddenSSID = true;
		config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		config.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.TKIP);
		config.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.CCMP);
		config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		localWifiManager.addNetwork(config);
		localWifiManager.saveConfiguration();

		for (WifiConfiguration cfg : localWifiManager.getConfiguredNetworks()) {
			if (cfg.SSID.equals(config.SSID)) {
				mNetId.add(cfg.networkId);
			}
		}
		sendProbeRequest(localWifiManager, mNetId);
		return true;
	}

	public void transmitSettings(final String Ssid, final String Key) {
		final String Userinfo = "?1234"; // faked ip
		new Thread() {
			@Override
			public void run() {
				// sendProbeRequest(Ssid, Key, Userinfo);
				startTransmit(Ssid, Key, Userinfo);
			}
		}.start();
	}

	// 实验室wifi模式
	private void sendProbeReq(String ssid, String pwd, String Userinfo) {
		WifiManager wifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		WifiConfiguration wc = new WifiConfiguration();

		// 这个具体设计发送内容
		String param = "#" + Userinfo + ssid + "@" + pwd;
		// wc.SSID = String.format("\"%s\"", new Object[] { param });
		wc.SSID = "\"" + ssid + "\"";
		wc.preSharedKey = null;
		// 设置网络为隐藏AP，使得设备可以主动发Probe Request帧
		wc.hiddenSSID = true;
		wc.status = WifiConfiguration.Status.ENABLED;
		wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

		int id = wifiManager.addNetwork(wc);
		// 第二个参数设为false 则不会断掉当前网络。
		wifiManager.enableNetwork(id, false);

		// 建议开新线程扫描
		for (int i = 0; i < 10; i++) {
			wifiManager.startScan();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// 删除网络
		wifiManager.removeNetwork(id);
	}

	private void sendProbeRequest(WifiManager localWifiManager,
			List<Integer> netIds) {
		try {
			for (int netId : netIds) {
				// Log.e("---netIds---", "netIds = " + netIds);
				localWifiManager.disableNetwork(netId);
				// Thread.sleep(10L);
				localWifiManager.enableNetwork(netId, false);
				// Thread.sleep(10L);
				// Log.e("---minus--->---", "netId = " + netId);
				// mScanning = true;
				// if (mCallbackThread != null) {
				// mCallbackThread.start();
				// }
				// localWifiManager.setWifiEnabled(true);
				localWifiManager.startScan();
				// Log.e("---minus--->---", "--->---startScan");
				// localWifiManager.startScan();
				// do {
				// Log.d(TAG, "Probe Waiting SCAN END");
				// Thread.sleep(10L);
				// if (!mScanning && mErrorId <= 0) break;
				// if (mStopTransmitting) break;
				// if (!mIsWorking) break;
				// }
				// while (mErrorId <= 0);
			}
		} catch (Exception e) {
			// Log.d(TAG, e.getMessage());
		} finally {
			mNetId = new ArrayList<Integer>();
			// clearNetList();
		}
	}

	boolean mIsWorking = false;

	public void stopTransmitting() {
		mIsWorking = false;
		mStopTransmitting = true;
		clearNetList();
	}
}
