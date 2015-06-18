package com.mxchip.easylink_minus;

import com.mxchip.easylink_plus.EasyLink_plus;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private Context ctx;
	private TextView wifi_ssid;
	private TextView wifi_psw;
	private EasyLink_plus minus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.ctx = MainActivity.this;

		wifi_ssid = (TextView) findViewById(R.id.wifi_ssid);
		wifi_psw = (TextView) findViewById(R.id.wifi_psw);
		Button sendbtnid = (Button) findViewById(R.id.sendbtnid);
		Button stopsendbtnid = (Button) findViewById(R.id.stopsendbtnid);
		minus = EasyLink_plus.getInstence(ctx);

		sendbtnid.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				minus.transmitSettings(wifi_ssid.getText().toString().trim(),
						wifi_psw.getText().toString().trim());
			}
		});
		stopsendbtnid.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				minus.stopTransmitting();
			}
		});
	}
}
