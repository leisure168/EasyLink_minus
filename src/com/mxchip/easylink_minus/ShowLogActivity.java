package com.mxchip.easylink_minus;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.mxchip.helper.ProbeReqData;

public class ShowLogActivity extends Activity {
	private Context ctx;
	private TextView wifi_ssid;
	private TextView wifi_psw;
	private TextView appip;
	public TextView configinfoid;
	private IntentFilter mIntentFilter = null;
	private boolean swiTag = true;
	private Thread nth;
	public static int ipAddr;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.showlog);
		this.ctx = ShowLogActivity.this;

		wifi_ssid = (TextView) findViewById(R.id.wifi_ssid);
		wifi_psw = (TextView) findViewById(R.id.wifi_psw);
		appip = (TextView) findViewById(R.id.appip);
		Button cleanwifiid = (Button) findViewById(R.id.cleanwifiid);
		Button sendbtnid = (Button) findViewById(R.id.sendbtnid);
		Button stopsendbtnid = (Button) findViewById(R.id.stopsendbtnid);
		configinfoid = (TextView) findViewById(R.id.configinfoid);

		cleanwifiid.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				clearNetList();
			}
		});

		sendbtnid.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				swiTag = true;
				EasyLink_minus(ctx);
				// final String Userinfo = "?1234"; // faked ip
				// ipAddr = mWifiManager.getCurrentIpAddressConnectedInt();
				ipAddr = Integer.parseInt(appip.getText().toString().trim());
				startTransmit(wifi_ssid.getText().toString().trim(), wifi_psw
						.getText().toString().trim(), ipAddr);
			}
		});

		stopsendbtnid.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				swiTag = false;
			}
		});
	}

	private String EasyLink_minus(Context ctx) {
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction("android.net.wifi.SCAN_RESULTS");
		mIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
		mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
		return null;
	}

	private List<Integer> mNetId = new ArrayList<Integer>();

	private void startTransmit(String Ssid, String Key, int ip) {
		// String param = "#" + userinfo + Ssid + "@" + Key;
		String param[] = new ProbeReqData().bgProtocol(Ssid, Key, ip);
		String valstr = "原始数据: ";
		for (byte bt : param[0].getBytes()) {
			valstr += ((int) bt) + " ";
		}
		valstr += "\r\nSSID: ";
		for (byte bt : param[1].getBytes()) {
			valstr += ((int) bt) + " ";
		}
		configinfoid.setText(valstr);

		final WifiManager localWifiManager = (WifiManager) ctx
				.getSystemService(Context.WIFI_SERVICE);
		// WifiInfo localWifiInfo = localWifiManager.getConnectionInfo();
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

		nth = new Thread(new Runnable() {
			@Override
			public void run() {
				while (swiTag) {
					for (int netId : mNetId) {
						localWifiManager.disableNetwork(netId);
						localWifiManager.enableNetwork(netId, false);
						localWifiManager.startScan();
					}
					try {
						Thread.sleep(200L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		nth.start();
	}

	private void clearNetList() {
		WifiManager localWifiManager = (WifiManager) ctx
				.getSystemService(Context.WIFI_SERVICE);
		if (localWifiManager == null
				|| localWifiManager.getConfiguredNetworks() == null) {
			return;
		}
		for (WifiConfiguration localWifiConfiguration : localWifiManager
				.getConfiguredNetworks()) {
			String ssid = localWifiConfiguration.SSID.replaceAll("\"", "");

			for (byte bt : ssid.getBytes()) {
				if (bt == 1) {
					localWifiManager
							.removeNetwork(localWifiConfiguration.networkId);
					localWifiManager.saveConfiguration();
				}
			}
			// if (!ssid.contains("#?1234"))
			// continue;
		}
	}
}
