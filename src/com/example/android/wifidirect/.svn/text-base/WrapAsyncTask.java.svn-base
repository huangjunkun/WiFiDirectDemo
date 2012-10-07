/**
 * 
 */
package com.example.android.wifidirect;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.widget.Toast;




class RequestSendFileAsyncTask extends
		AsyncTask<Void, Void, Boolean> {
	private String fileName;
	private int fileSize;
	private String host;
	private int port;
	RequestSendFileAsyncTask(String name, int size, String host, int port) {
		this.fileName = name;
		this.fileSize = size;
		this.host = host;
		this.port = port;
	}
	
	@Override
	protected Boolean doInBackground(Void... params) {
		if (Utility.sendFileInfo(fileName, fileSize, host, port))
			return Boolean.TRUE;
		else
			return Boolean.FALSE;
	}

	@Override
	protected void onPostExecute(Boolean result) {
//		Toast toast = Toast.makeText(context, tips, Toast.LENGTH_LONG);
//		toast.setGravity(Gravity.CENTER, 0, 0);
//		toast.show();
		Log.d(this.getClass().getName(), "onPostExecute result:" + result);
		
	}
}
/**
 * send file.
 */
class SendFileAsyncTask extends
		AsyncTask<Activity, Void, Boolean> {

	private String fileUri;
	private String host;
	private int port;
	private Context context;

	public SendFileAsyncTask(String fileUri, String host, int port) {
		this.fileUri = fileUri;
		this.host = host;
		this.port = port;
	}

	@Override
	protected Boolean doInBackground(Activity... activitys) {
		assert (activitys.length == 1);
		Boolean result = Boolean.TRUE;
		WiFiDirectActivity activity = (WiFiDirectActivity) activitys[0];
		this.context = activity;
		Socket socket = new Socket();
		try {
			Log.d(this.getClass().getName(), "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), ConfigInfo.SOCKET_TIMEOUT);
			Log.d(this.getClass().getName(),
					"Client socket - " + socket.isConnected());
			OutputStream outs = socket.getOutputStream();
			outs.write(ConfigInfo.COMMAND_ID_SEND_FILE);// id
			Uri uri = Uri.parse(fileUri);
			Pair<String, Integer>  pair = Utility.getFileNameAndSize(activity, uri);
			String name = pair.first;
			long size = pair.second;
			activity.setSendFileSize(size);
			activity.setSendFileName(name);
			String strSend = "size:" + size + "name:" + name;
			outs.write(strSend.length());
			outs.write(strSend.getBytes(), 0, strSend.length());

			ContentResolver cr = activity.getContentResolver();
			InputStream ins = null;
			try {
				ins = cr.openInputStream(uri);
			} catch (FileNotFoundException e) {
				Log.d(this.getClass().getName(), e.toString());
			}

			byte buf[] = new byte[1024];
			int len;
			while ((len = ins.read(buf)) != -1) {
				outs.write(buf, 0, len);
				// 通知界面发送/接收文件进度。
				Message msg = new Message();
				msg.what = ConfigInfo.MSG_SEND_RECV_FILE_BYTES;
				msg.arg1 = len;// send;
				msg.arg2 = 0;// recv;
				activity.getHandler().sendMessage(msg);
			}

			ins.close();
			outs.close();
			Log.d(this.getClass().getName(), "Client: Data written");

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

	@Override
	protected void onPostExecute(Boolean result) {
		Log.d("SendFileAsyncTask", "onPostExecute end. result " + result);

		WiFiDirectActivity activity = (WiFiDirectActivity) context;
		activity.onSendFileEnd();
		String tips = "";
		if (result.booleanValue())
			tips = "Send file ok.";
		else
			tips = "Send file failed.";
		Toast toast = Toast.makeText(context, tips, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
}

/**
 * client send local ip & port to server.
 */
class SendPeerInfoAsyncTask extends
		AsyncTask<String, Void, Boolean> {

	Context context;

	public SendPeerInfoAsyncTask(Context context) {
		this.context = context;
	}

	@Override
	protected Boolean doInBackground(String... params) {
		assert (params.length == 2);		
		String host = params[0];
		int port = Integer.parseInt(params[1]);
		if (Utility.sendPeerInfo(host, port))
			return Boolean.TRUE;
		else
			return Boolean.FALSE;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		Log.d("SendPeerInfoAsyncTask", "onPostExecute end.");
		String tips = "";
		if (result.booleanValue())
			tips = "Send peer's info ok.";
		else
			tips = "Send peer's info failed.";
		Toast toast = Toast.makeText(context, tips, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
}


/**
 * client send a stream to host:port.
 */
class SendStreamAsyncTask extends
		AsyncTask<InputStream, Void, Boolean> {

	Context context;
	private String host;
	private int port;

	public SendStreamAsyncTask(Context context, String host, int port) {
		this.context = context;
		this.host = host;
		this.port = port;
	}

	@Override
	protected Boolean doInBackground(InputStream... params) {
		assert (params.length == 1);
		if (Utility.sendStream(host, port, params[0]))
			return Boolean.TRUE;
		else
			return Boolean.FALSE;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		Log.d("SendStringAsyncTask", "onPostExecute end.");
		String tips = "";
		if (result.booleanValue())
			tips = "Send ok.";
		else
			tips = "Send failed.";
		Toast toast = Toast.makeText(context, tips, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
}
