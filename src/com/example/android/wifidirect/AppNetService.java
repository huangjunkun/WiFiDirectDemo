/**
 * 
 */
package com.example.android.wifidirect;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

/**
 * @author xldownloadlib
 * 
 */

class PeerInfo {
	public String host;
	public int port;

	public PeerInfo(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	@Override
	public String toString() {
		return "peer:" + host + "port:" + port;
	}
}

interface WifiP2pNetServiceListener extends ConnectionInfoListener, PeerListListener {
	public void updateThisDevice(WifiP2pDevice device);
}

class AppNetServiceListener implements ConnectionInfoListener, PeerListListener {

	public ConnectionInfoListener connListener;
	public PeerListListener peersListener;

	AppNetServiceListener(ConnectionInfoListener listener1, PeerListListener listener2) {
		connListener = listener1;
		peersListener = listener2;
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		peersListener.onPeersAvailable(peers);
	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		connListener.onConnectionInfoAvailable(info);
	}
}

public class AppNetService extends Service implements ChannelListener,
		WifiP2pNetServiceListener {
	// 定义个一个Tag标签
	private static final String TAG = "AppNetService";
	private boolean retryChannel = false;
	// 这里定义吧一个Binder类，用在onBind()有方法里，这样Activity那边可以获取到
	private NetServiceBinder mBinder = new NetServiceBinder();
	private ThreadPoolManager serviceThread = null;
	private WifiP2pManager manager = null;
	private Channel channel = null;
	private BroadcastReceiver receiver = null;
	private final IntentFilter intentFilter = new IntentFilter();
	private List<WifiP2pDevice> p2pDeviceList = new ArrayList<WifiP2pDevice>();

	public List<WifiP2pDevice> getP2pDeviceList() {
		return p2pDeviceList;
	}

	public boolean isWifiP2pAviliable() {
		return manager != null;
	}

	public boolean isWifiP2pManager() {
		return manager != null;
	}

	public boolean isWifiP2pChannel() {
		return channel != null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "start IBinder~~~");
		return mBinder;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "start onCreate~~~");
		super.onCreate();

		// add necessary intent values to be matched.

		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter
				.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter
				.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = initialize(this, getMainLooper(), this);

		receiver = new WiFiDirectBroadcastReceiver(this, this);
		registerReceiver(receiver, intentFilter);

		try {
			serviceThread = new ThreadPoolManager(this, ConfigInfo.LISTEN_PORT, 5);
		} catch (IOException ex) {
			Log.e("NetworkService", "onActivityCreated() IOException ex", ex);
		}
	}

	private void initServiceThread() {
		Log.d(TAG, "initServiceThread.");
		serviceThread.init();
	}

	private void uninitServiceThread() {
		Log.d(TAG, "uninitServiceThread.");
		serviceThread.uninit();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "start onStart~~~");
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "start onDestroy~~~");
		super.onDestroy();
		serviceThread.destory();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "start onUnbind~~~");
		unregisterReceiver(receiver);
		return super.onUnbind(intent);
	}

	@Override
	public void onChannelDisconnected() {
		// we will try once more
		if (isWifiP2pManager() && !retryChannel) {
			Toast.makeText(this, "Channel lost. Trying again",
					Toast.LENGTH_LONG).show();
			resetPeers();
			retryChannel = true;
			channel = initialize(this, getMainLooper(), this);
		} else {
			Toast.makeText(
					this,
					"Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
					Toast.LENGTH_LONG).show();
		}
	}

	public class NetServiceBinder extends Binder {
		AppNetService getService() {
			return AppNetService.this;
		}
	}

	private ArrayList<PeerInfo> peerInfoList = new ArrayList<PeerInfo>();

	final public ArrayList<PeerInfo> getPeerInfoList() {
		return peerInfoList;
	}

	private WifiP2pInfo wifiP2pInfo;

	final String hostAddress() {
		return wifiP2pInfo.groupOwnerAddress.getHostAddress();
	}

	final boolean isPeer() {
		return !wifiP2pInfo.isGroupOwner;
	}

	final boolean isGroupOwner() {
		return wifiP2pInfo.isGroupOwner;
	}

	private WiFiDirectActivity activity = null;
	WiFiDirectActivity getActivity() {
		return activity;
	}

	public void bindAcitivity(Activity activity) {
		this.activity = (WiFiDirectActivity) activity;
		if (localDevice != null)
			updateThisDevice(localDevice);
		// 刷新peer列表 .
		discoverPeers();
	}

	AppNetServiceListener serviceListener = null;

	final public void bindListener(AppNetServiceListener listener) {
		serviceListener = listener;
	}

	private boolean isWifiP2pEnabled = false;

	final public void setIsWifiP2pEnabled(boolean isEnabled) {
		this.isWifiP2pEnabled = isEnabled;
		if (isWifiP2pEnabled) {
			initServiceThread();
		} else {
			uninitServiceThread();
		}
	}

	public boolean discoverPeers() {
		if (activity != null) {
			if (!isWifiP2pEnabled) {
				Toast.makeText(this, R.string.p2p_off_warning,
						Toast.LENGTH_SHORT).show();
			} else {
				((WiFiDirectActivity) activity).showDiscoverPeers();
				manager.discoverPeers(channel,
						new WifiP2pManager.ActionListener() {
							@Override
							public void onSuccess() {
								Toast.makeText(AppNetService.this,
										"Discovery Initiated",
										Toast.LENGTH_SHORT).show();
							}

							@Override
							public void onFailure(int reasonCode) {
								Toast.makeText(AppNetService.this,
										"Discovery Failed : " + reasonCode,
										Toast.LENGTH_SHORT).show();
							}
						});
				return true;
			}
		}
		return false;

	}

	public void connect(WifiP2pConfig config) {
		manager.connect(channel, config, new ActionListener() {

			@Override
			public void onSuccess() {
				// WiFiDirectBroadcastReceiver will notify us. Ignore for now.
			}

			@Override
			public void onFailure(int reason) {
				Toast.makeText(AppNetService.this, "Connect failed. Retry.",
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void cancelDisconnect() {
		manager.cancelConnect(channel, new ActionListener() {
			@Override
			public void onSuccess() {
				Toast.makeText(AppNetService.this, "Aborting connection",
						Toast.LENGTH_SHORT).show();
			}
			@Override
			public void onFailure(int reasonCode) {
				Toast.makeText(
						AppNetService.this,
						"Connect abort request failed. Reason Code: "
								+ reasonCode, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public WifiP2pManager.Channel initialize(Context srcContext,
			Looper srcLooper, WifiP2pManager.ChannelListener listener) {
		return manager.initialize(srcContext, srcLooper, listener);
	}

	public void requestPeers(WifiP2pManager.PeerListListener listener) {
		manager.requestPeers(channel, listener);
	}

	public void requestConnectionInfo(
			WifiP2pManager.ConnectionInfoListener listener) {
		manager.requestConnectionInfo(channel, listener);
	}

	public void removeGroup() {
		manager.removeGroup(channel, new ActionListener() {

			@Override
			public void onFailure(int reasonCode) {
				Log.e(TAG, "Disconnect failed. Reason :" + reasonCode);
				//reason  The reason for failure could be one of P2P_UNSUPPORTED 1, ERROR 0 or BUSY 2.
			}

			@Override
			public void onSuccess() {
				((WiFiDirectActivity) activity).onDisconnect();
			}
		});
	}

	public void resetPeers() {
		p2pDeviceList.clear();
		if (activity != null)
			activity.resetPeers();
	}

	private boolean bVerifyRecvFile = false;

	public boolean isbVerifyRecvFile() {
		return bVerifyRecvFile;
	}

	public void setbVerifyRecvFile(boolean bVerifyRecvFile) {
		this.bVerifyRecvFile = bVerifyRecvFile;
	}

	public void postRecvPeerList(int count) {
		Message msg = new Message();
		msg.what = ConfigInfo.MSG_REPORT_RECV_PEER_LIST;
		msg.arg1 = count;
		activity.getHandler().sendMessage(msg);			
	}
	
	public void postSendStringResult(int sendBytes)
	{
		Message msg = new Message();
		msg.what = ConfigInfo.MSG_SEND_STRING;
		msg.arg1 = sendBytes;// send;
		activity.getHandler().sendMessage(msg);		
	}
	
	public void postSendRecvBytes(int sendBytes, int recvBytes)
	{
		Message msg = new Message();
		msg.what = ConfigInfo.MSG_SEND_RECV_FILE_BYTES;
		msg.arg1 = sendBytes;// send;
		msg.arg2 = recvBytes;// recv;
		activity.getHandler().sendMessage(msg);		
	}
	
	public void postSendPeerInfoResult(int result) {
		Message msg = new Message();
		msg.what = ConfigInfo.MSG_REPORT_SEND_PEER_INFO_RESULT;
		msg.arg1 = result;
		activity.getHandler().sendMessage(msg);
	}
	
	public void postRecvPeerInfo(PeerInfo info) {
		Message msg = new Message();
		msg.what = ConfigInfo.MSG_RECV_PEER_INFO;
		activity.getHandler().sendMessage(msg);
		
	}
	
	public void postVerifyRecvFile() {
		Message msg = new Message();
		msg.what = ConfigInfo.MSG_VERIFY_RECV_FILE_DIALOG;
		activity.getHandler().sendMessage(msg);
	}
	
	public void postRecvFileResult(int result) {
		Message msg = new Message();
		msg.what = ConfigInfo.MSG_REPORT_RECV_FILE_RESULT;
		msg.arg1 = result;
		activity.getHandler().sendMessage(msg);
	}
		
	public void postSendFileResult(int result) {
		Message msg = new Message();
		msg.what = ConfigInfo.MSG_REPORT_SEND_FILE_RESULT;
		msg.arg1 = result;
		activity.getHandler().sendMessage(msg);
	}
	
	public void postSendStreamResult(int result) {
		Message msg = new Message();
		msg.what = ConfigInfo.MSG_REPORT_SEND_STREAM_RESULT;
		msg.arg1 = result;
		activity.getHandler().sendMessage(msg);
	}	
	private SocketAddress remoteSockAddr;

	public void setRemoteSockAddress(SocketAddress sockAddr) {
		remoteSockAddr = sockAddr;
	}

	public SocketAddress getRemoteSockAddress() {
		return remoteSockAddr;
	}

	public void handleSendFile() {
		
	}
	
	public String getFileInfo(Uri uri) throws IOException {
		Pair<String, Integer>  pair = Utility.getFileNameAndSize(getActivity(), uri);
		String name = pair.first;
		long size = pair.second;
		getActivity().setSendFileSize(size);
		getActivity().setSendFileName(name);
		String fileInfo = "size:" + size + "name:" + name;
		return fileInfo;
	}

	public InputStream getInputStream(Uri uri) throws FileNotFoundException  {
		ContentResolver cr = getActivity().getContentResolver();
		return cr.openInputStream(uri);
	}
	
	public void handleRecvFile(InputStream ins) {
		handleRecvFileInfo(ins);

		// TODO 等待界面返回通知是否答应接收文件。
		String extName = ".jpg"; // default .
		if (!activity.recvFileName().isEmpty()) {
			int dotIndex = activity.recvFileName().lastIndexOf(".");
			if (dotIndex != -1
					&& dotIndex != activity.recvFileName().length() - 1) {
				extName = activity.recvFileName().substring(dotIndex);
			}
		}
		Log.d(TAG, "activity.recvFileName():" + activity.recvFileName()
				+ " extName:" + extName);

		if (waitForVerifyRecvFile() && isbVerifyRecvFile()) {
			recvFileAndSave(ins, extName);
		} else
			postRecvFileResult(-1);
	}

	private CountDownLatch startRecvFileSignal = null;

	public void verifyRecvFile() {
		assert (startRecvFileSignal != null);
		startRecvFileSignal.countDown();
	}

	private boolean waitForVerifyRecvFile() {
		try {
			startRecvFileSignal = new CountDownLatch(1);// 重新初始化。
			boolean res = startRecvFileSignal.await(10, TimeUnit.SECONDS);
			return res;
		} catch (InterruptedException e) {
			Log.e(this.getClass().getName(), "waitForVerifyRecvFile e:", e);
			e.printStackTrace();
			return false;
		}
	}

	public boolean handleRecvPeerList(InputStream ins) {
		try {
			peerInfoList.clear();
			int peerListSize = ins.read();
			for (int i = 0; i < peerListSize; ++i) {
				int bufferLen = ins.read();
				byte[] buffer = new byte[256];
				ins.read(buffer, 0, bufferLen);
				String strBuffer = new String(buffer, 0, bufferLen);
				int offset1 = strBuffer.indexOf("peer:");
				int offset2 = strBuffer.indexOf("port:");
				Log.d(WiFiDirectActivity.TAG, "recvPeerSockAddr strBuffer:"
						+ strBuffer);
				if (offset1 != -1 && offset2 != -1) {
					assert (offset1 < offset2);
					String host = strBuffer.substring(offset1 + 5, offset2);
					int port = Integer.parseInt(strBuffer.substring(offset2 + 5,
							strBuffer.length()));
					peerInfoList.add(new PeerInfo(host, port));
					Log.d(WiFiDirectActivity.TAG, "peerInfoList.add(...). size:"
							+ peerInfoList.size());
				}
			}
			postRecvPeerList(peerInfoList.size());
			return true;
		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
			return false;
		}
	}
	
	public boolean handleRecvFileInfo(InputStream ins) {
		activity.resetRecvFileInfo();
		try {
			int iSize = ins.read();
			byte[] buffer = new byte[iSize];
			int len = ins.read(buffer, 0, iSize);
			String strBuffer = new String(buffer, 0, len);
			assert (strBuffer.length() == iSize);
			int offset1 = strBuffer.indexOf("size:");
			int offset2 = strBuffer.indexOf("name:");
			Log.d(WiFiDirectActivity.TAG, "recvDistFileInfo strBuffer:"
					+ strBuffer);
			if (offset1 != -1 && offset2 != -1) {
				assert (offset1 < offset2);
				String strSize = strBuffer.substring(offset1 + 5, offset2);
				activity.setRecvFileSize(Long.parseLong(strSize));
				activity.setRecvFileName(strBuffer.substring(offset2 + 5,
						strBuffer.length()));

				Log.d(WiFiDirectActivity.TAG,
						"iFileSize:"
								+ Integer.parseInt(strSize)
								+ " strFileName:"
								+ strBuffer.substring(offset2 + 5,
										strBuffer.length()));
				postVerifyRecvFile();
				return true;
			}
			return false;
		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
			return false;
		}
	}

	public boolean handleRecvPeerInfo(InputStream ins) {
		try {
			String strBuffer = "";
			byte[] buffer = new byte[1024];
			int len;
			while ((len = ins.read(buffer)) != -1) {
				strBuffer = strBuffer + new String(buffer, 0, len);
			}

			int offset1 = strBuffer.indexOf("peer:");
			int offset2 = strBuffer.indexOf("port:");
			Log.d(WiFiDirectActivity.TAG, "recvPeerSockAddr strBuffer:"
					+ strBuffer);
			if (offset1 != -1 && offset2 != -1) {
				assert (offset1 < offset2);
				String host = strBuffer.substring(offset1 + 5, offset2);
				int port = Integer.parseInt(strBuffer.substring(offset2 + 5,
						strBuffer.length()));
				Log.d(WiFiDirectActivity.TAG, "new host:" + host);

				// 通知界面显示发送图片。
				PeerInfo info = new PeerInfo(host, port);
				postRecvPeerInfo(info);
				for (Iterator<PeerInfo> iter = peerInfoList.iterator(); iter
						.hasNext();) {
					PeerInfo peer = iter.next();
					Log.d(WiFiDirectActivity.TAG, "host:" + peer.host
							+ " port:" + peer.port);
					if (peer.host.equals(host))
						return true;//不需要add ...
				}
				peerInfoList.add(info);
				Log.d(WiFiDirectActivity.TAG, "peerInfoList.add(...). size:"
						+ peerInfoList.size());
			}
			return true;
		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
			return false;
		}
	}

	public boolean recvFileAndSave(InputStream ins, String extName) {
		try {
			final File recvFile = new File(
					Environment.getExternalStorageDirectory()
							+ "/wifi-direct/wifip2pshared-"
							+ System.currentTimeMillis() + extName);

			File dirs = new File(recvFile.getParent());
			if (!dirs.exists())
				dirs.mkdirs();
			recvFile.createNewFile();

			Log.d(WiFiDirectActivity.TAG,
					"server: copying files " + recvFile.toString());
			FileOutputStream fileOutS = new FileOutputStream(recvFile);

			byte buf[] = new byte[1024];
			int len;
			while ((len = ins.read(buf)) != -1) {
				fileOutS.write(buf, 0, len);
				// 通知界面发送/接收文件进度。
				postSendRecvBytes(0, len);

			}
			fileOutS.close();
			String strFile = recvFile.getAbsolutePath();
			if (strFile != null) {
				//  Go, let's go and test a new cool & powerful method.
				Utility.openFile(activity, recvFile);
			}
			return true;
		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, "IOException", e);
			e.printStackTrace();
			return false;
		}
	}

	public void handleSendPeerInfo() {		
		serviceThread.execute(new SendPeerInfoRunable(new PeerInfo(hostAddress(), ConfigInfo.LISTEN_PORT), //Owner's address
				this));
	}

	public void handleSendFile(String host, int port, Uri uri) {	
		Log.d(this.getClass().getName(), "handleSendFile");	
		serviceThread.execute(new SendFileRunable(host, port, uri, this));
	}
	
	public void handleBroadcastPeerList() {
		if (isGroupOwner()) {
			ByteArrayOutputStream outs = new ByteArrayOutputStream();
			//ArrayList<PeerInfo> peerInfoLists = getPeerInfoList();
			outs.write(peerInfoList.size());
			for (PeerInfo peerInfo : peerInfoList) {
				String tmp = peerInfo.toString();
				//String tmp = "peer:" + peerInfo.host + "port:" + peerInfo.port;
				outs.write(tmp.length());
				try {
					outs.write(tmp.getBytes());
				} catch (IOException e) {
					Log.e(TAG, " e:" + e);
					e.printStackTrace();
				}
			}

			Log.d(TAG, " outs:" + outs);
			ByteArrayInputStream ins = new ByteArrayInputStream(
					outs.toByteArray());
			Log.d(TAG, " ins's length:" + ins.available());
			for (PeerInfo peerInfo : peerInfoList) {
				handleSendStream(peerInfo.host, peerInfo.port, ins);
			}
		}
	}
	public void handleSendStream(String host, int port, InputStream ins) {
		// let's go and test ...
		serviceThread.execute(new SendStreamRunable(host, port, ins, this));
	}	

	public void handleSendString(String host, int port, String data) {
		serviceThread.execute(new SendStringRunable(host, port, data, this));
	}
	
	private WifiP2pDevice localDevice = null;
	
	@Override
	public void updateThisDevice(WifiP2pDevice device) {
		localDevice = device;
		if (activity != null)
			activity.updateThisDevice(device);
	}
	
	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		p2pDeviceList.clear();
		p2pDeviceList.addAll(peers.getDeviceList());

		if (p2pDeviceList.size() == 0) {
			Log.d(WiFiDirectActivity.TAG, "No devices found");
		}

		if (serviceListener != null)
			serviceListener.onPeersAvailable(peers);

	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		wifiP2pInfo = info;
		if (info.groupFormed && info.isGroupOwner) {
			Log.d(TAG, "owner - info.groupFormed && info.isGroupOwner...");
			handleBroadcastPeerList();
		} else if (info.groupFormed) {
			handleSendPeerInfo();
			Log.d(TAG, "peer - info.groupFormed.");
		}
		
		if (serviceListener != null)
			serviceListener.onConnectionInfoAvailable(info);
	}
}
