package com.mxchip.easylink_minus;

import java.util.ArrayList;
import java.util.List;

import com.mxchip.helper.ProbeReqData;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ShowLogActivity extends Activity {
	private Context ctx;
	private TextView wifi_ssid;
	private TextView wifi_psw;
	public TextView configinfoid;
	private IntentFilter mIntentFilter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.showlog);
		this.ctx = ShowLogActivity.this;

		wifi_ssid = (TextView) findViewById(R.id.wifi_ssid);
		wifi_psw = (TextView) findViewById(R.id.wifi_psw);
		Button sendbtnid = (Button) findViewById(R.id.sendbtnid);
		Button stopsendbtnid = (Button) findViewById(R.id.stopsendbtnid);
		configinfoid = (TextView) findViewById(R.id.configinfoid);

		sendbtnid.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				EasyLink_minus(ctx);
				String Userinfo = "?1234"; // faked ip
				startTransmit(wifi_ssid.getText().toString().trim(), wifi_psw
						.getText().toString().trim(), Userinfo);
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

	private void startTransmit(String Ssid, String Key, String userinfo) {
		// String param = "#" + userinfo + Ssid + "@" + Key;
		String param[] = new ProbeReqData().bgProtocol(Ssid, Key);
		String valstr = "原始数据: ";
		for (byte bt : param[0].getBytes()) {
			valstr += ((int) bt) + " ";
		}
		valstr +="\r\nSSID: ";
		for (byte bt : param[1].getBytes()) {
			valstr += ((int) bt) + " ";
		}
		configinfoid.setText(valstr);

		WifiManager localWifiManager = (WifiManager) ctx
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo localWifiInfo = localWifiManager.getConnectionInfo();
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
		for (int netId : mNetId) {
			localWifiManager.disableNetwork(netId);
			localWifiManager.enableNetwork(netId, false);
			localWifiManager.startScan();
		}
	}
}
