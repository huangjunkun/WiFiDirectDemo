package com.example.android.wifidirect;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.net.Uri;
import android.util.Log;

public class WrapRunable {

}


class SendPeerInfoRunable  implements Runnable {
	private PeerInfo peerInfo;
	private AppNetService netService;
	SendPeerInfoRunable(PeerInfo peerInfo, AppNetService netService) {
		this.peerInfo = peerInfo;
		this.netService = netService;
	}
	
	@Override
	public void run() {
		if (Utility.sendPeerInfo(peerInfo.host, peerInfo.port))
			netService.postSendPeerInfoResult(0);
		else
			netService.postSendPeerInfoResult(-1);
	}	
}


class SendFileRunable implements Runnable {
	private String host;
	private int port;
	private Uri uri;
	private AppNetService netService;
	SendFileRunable(String host, int port, Uri uri, AppNetService netService) {
		this.host = host;
		this.port = port;
		this.uri = uri;
		this.netService = netService;
	}
	
	@Override
	public void run() {
		if (sendFile())
			netService.postSendFileResult(0);
		else
			netService.postSendFileResult(-1);
	}

	private boolean sendFile() {
		Boolean result = Boolean.TRUE;
		Socket socket = new Socket();
		try {
			Log.d(this.getClass().getName(), "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), ConfigInfo.SOCKET_TIMEOUT);
			Log.d(this.getClass().getName(),
					"Client socket - " + socket.isConnected());
			OutputStream outs = socket.getOutputStream();
			outs.write(ConfigInfo.COMMAND_ID_SEND_FILE);// id
			String fileInfo = netService.getFileInfo(uri);
			Log.d(this.getClass().getName(), "fileInfo:" + fileInfo);
			outs.write(fileInfo.length());
			outs.write(fileInfo.getBytes(), 0, fileInfo.length());
			InputStream ins = netService.getInputStream(uri);
			byte buf[] = new byte[1024];
			int len;
			while ((len = ins.read(buf)) != -1) {
				outs.write(buf, 0, len);
				// 通知界面发送/接收文件进度。
				netService.postSendRecvBytes(len, 0);
			}

			ins.close();
			outs.close();
			Log.d(this.getClass().getName(), "Client: Data written");
		} catch (FileNotFoundException e) {
			Log.d(this.getClass().getName(), "send file exception " + e.toString());
		} catch (IOException e) {
			Log.e(this.getClass().getName(),
					"send file exception " + e.getMessage());
			result = Boolean.FALSE;
		} finally {
			if (socket != null) {
				if (socket.isConnected()) {
					try {
						socket.close();
						Log.d(this.getClass().getName(), "socket.close()");
					} catch (IOException e) {
						// Give up
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}
}

// SendStreamRunable 有进度回调，区别于SendStringmRunable无进度回调。
class SendStreamRunable implements Runnable {
	private String host;
	private int port;
	private InputStream ins;
	private AppNetService netService;
	SendStreamRunable(String host, int port, InputStream ins, AppNetService netService) {
		this.host = host;
		this.port = port;
		this.ins = ins;
		this.netService = netService;
	}
	
	@Override
	public void run() {
		if (sendStream())
			netService.postSendStreamResult(0);
		else
			netService.postSendStreamResult(-1);
	}
	
	private boolean sendStream() {
		Socket socket = new Socket();
		boolean result = true;

		try {
			Log.d(this.getClass().getName(), "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), ConfigInfo.SOCKET_TIMEOUT);

			Log.d(this.getClass().getName(), "Client socket - " + socket.isConnected());
			OutputStream outs = socket.getOutputStream();

			byte buf[] = new byte[1024];
			int len;
			while ((len = ins.read(buf)) != -1) {
				outs.write(buf, 0, len);
//				// 通知界面发送/接收文件进度。
//				netService.postSendStreamBytes(len, 0);
			}
			ins.close();
			outs.close();			
			Log.d(this.getClass().getName(), "send stream ok.");

		} catch (IOException e) {
			Log.e(this.getClass().getName(), e.getMessage());
			result = false;
		} finally {
			if (socket != null) {
				if (socket.isConnected()) {
					try {
						socket.close();
						Log.d(this.getClass().getName(), "socket.close();");
					} catch (IOException e) {
						// Give up
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}
}

class SendStringRunable implements Runnable {
	private String host;
	private int port;
	private String data;
	private AppNetService netService;
	SendStringRunable(String host, int port, String data, AppNetService netService) {
		this.host = host;
		this.port = port;
		this.data = data;
		this.netService = netService;
	}
	
	@Override
	public void run() {
		if (sendString())
			netService.postSendStringResult(data.length());
		else
			netService.postSendStringResult(-1);
	}
	
	private boolean sendString() {
		Socket socket = new Socket();
		boolean result = true;

		try {
			Log.d(this.getClass().getName(), "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), ConfigInfo.SOCKET_TIMEOUT);

			Log.d(this.getClass().getName(), "Client socket - " + socket.isConnected());
			OutputStream outs = socket.getOutputStream();
			outs.write(ConfigInfo.COMMAND_ID_SEND_STRING);
			outs.write(data.length());// NOTE: MAX = 255
			outs.write(data.getBytes());
			outs.close();
			Log.d(this.getClass().getName(), "send string ok.");

		} catch (IOException e) {
			Log.e(this.getClass().getName(), e.getMessage());
			result = false;
		} finally {
			if (socket != null) {
				if (socket.isConnected()) {
					try {
						socket.close();
						Log.d(this.getClass().getName(), "socket.close();");
					} catch (IOException e) {
						// Give up
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}
}
