/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;
//import com.example.android.wifidirect.XL_Log;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements
		ConnectionInfoListener {

	private View mContentView = null;
	private WifiP2pDevice device;
	private WifiP2pInfo info;
	private boolean bConnected = false;
	ProgressDialog progressDialog = null;

	private WiFiDirectActivity getWiFiDirectActivity() {
		return (WiFiDirectActivity) this.getActivity();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onDestroyView() {
		bConnected = false;
		super.onDestroyView();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// super.onCreateView(inflater, container, savedInstanceState);

		mContentView = inflater.inflate(R.layout.device_detail, null);
		mContentView.findViewById(R.id.btn_connect).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						WifiP2pConfig config = new WifiP2pConfig();
						config.deviceAddress = device.deviceAddress;
						config.wps.setup = WpsInfo.PBC;
						if (progressDialog != null
								&& progressDialog.isShowing()) {
							progressDialog.dismiss();
						}
						progressDialog = ProgressDialog.show(getActivity(),
								"Press back to cancel", "Connecting to :"
										+ device.deviceAddress, true, true
										// TODO let's go and test ...
										, new DialogInterface.OnCancelListener() {						
											 @Override
											 public void onCancel(DialogInterface dialog) {
													Toast.makeText(getActivity(),
															"Cancel connect.",
															Toast.LENGTH_SHORT).show();
												 ((DeviceActionListener) getActivity()).cancelDisconnect();
											 }
										 }
								);
						((DeviceActionListener) getActivity()).connect(config);

					}
				});

		mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						((DeviceActionListener) getActivity()).disconnect();
					}
				});

		mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// Allow user to pick an image from Gallery or other
						// registered apps

						if (info.isGroupOwner)
							getWiFiDirectActivity().showSelectPeerDialog(); // 若本机为Owner，需先触发选择Peer逻辑。
						else {
							// TODO showSelectMediaDialog ...
							//getWiFiDirectActivity().showSelectMediaDialog();
							getWiFiDirectActivity().startSelectImage();
						}
					}
				});

		mContentView.findViewById(R.id.btn_send_ip).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (bConnected) {
							getWiFiDirectActivity().reportPeerInfo();
							Toast.makeText(getActivity(),
									"Send peer's info to server ...",
									Toast.LENGTH_SHORT).show();
						} else
							Toast.makeText(
									getActivity(),
									"Sorry, this button just for the connected peer.",
									Toast.LENGTH_SHORT).show();

					}
				});

		return mContentView;
	}

	@Override
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.info = info;
		this.getView().setVisibility(View.VISIBLE);
		bConnected = true;

		// The owner IP is now known.
		TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(getResources().getString(R.string.group_owner_text)
				+ ((info.isGroupOwner == true) ? getResources().getString(
						R.string.yes) : getResources().getString(R.string.no)));

		// InetAddress from WifiP2pInfo struct.
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText("Group Owner IP - "
				+ info.groupOwnerAddress.getHostAddress() + "\n local ip:"
				+ Utility.getLocalIpAddress());

		// After the group negotiation, we assign the group owner as the file
		// server. The file server is single threaded, single connection server
		// socket.

		Log.d(this.getClass().getName(), "info:" + info);
		if (info.groupFormed && info.isGroupOwner) {
			showBroadcastPeerList();
		} else if (info.groupFormed) {
			showSendFileVeiw();
			showReportIPVeiw();
		}

		// hide the connect button
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
	}

	public void showReportIPVeiw() {
		Toast toast = Toast.makeText(this.getActivity(),
				"Now you can report ip.", Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
		mContentView.findViewById(R.id.btn_send_ip).setVisibility(
				View.VISIBLE);
	}
	public void showBroadcastPeerList() {
		//mContentView.findViewById(R.id.btn_send_ip).setTag(...);
		mContentView.findViewById(R.id.btn_send_ip).setVisibility(
				View.VISIBLE);
	}
	
	public void showSendFileVeiw() {
		// The other device acts as the client. In this case, we enable the
		// get file button.
		mContentView.findViewById(R.id.btn_start_client).setVisibility(
				View.VISIBLE);
		((TextView) mContentView.findViewById(R.id.status_text))
				.setText(getResources().getString(R.string.client_text));
	}

	public void showStatus(String text) {
		TextView view = (TextView) mContentView.findViewById(R.id.status_text);
		view.setText(text);
		Log.d("showSendRecvFileStatus", text);		
	}
	
	/**
	 * Updates the UI with device data
	 * 
	 * @param device
	 *            the device to be displayed
	 */
	public void showDetails(WifiP2pDevice device) {
		this.device = device;
		this.getView().setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView
				.findViewById(R.id.device_address);
		view.setText(device.deviceAddress);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(device.toString());

	}

	/**
	 * Clears the UI fields after a disconnect or direct mode disable operation.
	 */
	public void resetViews() {
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView
				.findViewById(R.id.device_address);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.status_text);
		view.setText(R.string.empty);
		mContentView.findViewById(R.id.btn_start_client).setVisibility(
				View.GONE);
		this.getView().setVisibility(View.GONE);
	}

}
