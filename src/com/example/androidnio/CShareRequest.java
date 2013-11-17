package com.example.androidnio;

import java.nio.ByteBuffer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataEmitterReader;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.parser.StringParser;

public class CShareRequest implements CompletedCallback, com.koushikdutta.async.callback.DataCallback {
	private AsyncSocket mSocket;
	private long lastPant;// 最后活动时间
	private final int TIME_OUT = 1500; // Session超时时间
	private AsyncServer mServer;
	private HeartBeatThread mHeartBeat;
	private final int READLENGTH_STATE = 0;
	private final int READSTRING_STATE = 1;
	private int mState;

	public CShareRequest(AsyncSocket socket) {
		mSocket = socket;
		mServer = socket.getServer();
		mHeartBeat = new HeartBeatThread(this);
		// mHeartBeat.start();
		socket.setDataCallback(this);
		socket.setClosedCallback(this);
	}

	// private void parseString() {
	// switch (mState) {
	// case READLENGTH_STATE:
	// read(4, LengthCallback);
	// break;
	// case READSTRING_STATE:
	// read(mLength, StringCallback);
	// break;
	// }
	// }

	int mLength = 0;
	private DataCallback LengthCallback = new DataCallback() {

		@Override
		public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
			if (bb.size() > 4) {
				mLength = bb.getInt();
				mState = READSTRING_STATE;
			}
		}
	};
	private DataCallback StringCallback = new DataCallback() {

		@Override
		public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
			try {
				String readString = bb.readString();
				JSONObject parseObject = JSON.parseObject(readString);
				Integer integer = parseObject.getInteger("type");
				switch (integer.intValue()) {
				case 1:// 心跳
					System.out.println("====================心跳==================");
					setAlive();
					break;
				case 2:// 接收到文件
					break;

				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	};

	private boolean isKeekAlive() {
		return lastPant + TIME_OUT > System.currentTimeMillis();
	}

	private void setAlive() {
		lastPant = System.currentTimeMillis();
	}

	private static class HeartBeatThread extends Thread {
		public CShareRequest mRequest;

		public HeartBeatThread(CShareRequest request) {
			mRequest = request;
		}

		@Override
		public void run() {
			super.run();
			mRequest.lastPant = System.currentTimeMillis();
			while (mRequest.isKeekAlive()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mRequest.mSocket.close();
		}
	}

	@Override
	public void onCompleted(Exception ex) {
		System.out.println("用户退出！！！");
		if (ex != null) {
			ex.printStackTrace();
		}
	}

	DataCallback mPendingRead = new DataCallback() {

		@Override
		public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
			System.out.println(bb.readString());
		}
	};
	int mPendingReadLength;
	ByteBufferList mPendingData = new ByteBufferList();

	private boolean handlePendingData(DataEmitter emitter) {
		System.out.println("handle peding :"+mPendingData.remaining());
		if (mPendingReadLength > mPendingData.remaining())
			return false;
		System.out.println("complete==============");

		mPendingReadLength = 0;
		mPendingRead.onDataAvailable(emitter, mPendingData);
		mPendingData.recycle();
		assert !mPendingData.hasRemaining();

		return true;
	}

	@Override
	public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
		ByteBuffer all = bb.getAll();

		if (mPendingReadLength == 0) {

			int length = NIOUtils.getPacketSizeFromByteBuffer(all, 4, false);
			System.out.println("length:" + length);
			mPendingReadLength = length;
		}
		System.out.println("========data coming=========:" + all.remaining());

		mPendingData.add(all);
		handlePendingData(emitter);
	}
	// @Override
	// public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
	//
	// System.out.println(bb.size() + " " + bb.remaining());
	// System.out.println(bb.readString());
	//
	// // try {
	// // String readString = bb.readString();
	// // JSONObject parseObject = JSON.parseObject(readString);
	// // Integer integer = parseObject.getInteger("type");
	// // switch (integer.intValue()) {
	// // case 1:// 心跳
	// // System.out.println("====================心跳==================");
	// // setAlive();
	// // break;
	// // case 2:// 接收到文件
	// // break;
	// //
	// // }
	// // } catch (Exception ex) {
	// // ex.printStackTrace();
	// // }
	// }
}
