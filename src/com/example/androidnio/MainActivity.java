package com.example.androidnio;

import java.nio.ByteBuffer;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import com.alibaba.fastjson.JSON;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;

public class MainActivity extends Activity {

	private AsyncSocket mSocket;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CShareServer server = new CShareServer();
				server.setErrorCallback(new CompletedCallback() {

					@Override
					public void onCompleted(Exception ex) {
						ex.printStackTrace();
					}
				});
				server.listen(12580);

			}
		});

		this.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				AsyncServer client = AsyncServer.getDefault();

				client.connectSocket("192.168.43.1", 12580, new
						ConnectCallback() {
							boolean isClose = false;

							@Override
							public void onConnectCompleted(Exception ex,
									AsyncSocket socket) {
								System.out.println("连接成功");
								mSocket = socket;
								Runnable heartbeatRunner = new
										Runnable() {
											@Override
											public void run() {
												if (isClose)
													return;
												byte[]
												bytes = JSON.toJSONString(new
														HeartBeat()).getBytes();
												mSocket.write(ByteBuffer.wrap(bytes));
												mSocket.getServer().postDelayed(this, 100);
											}
										};
								mSocket.setClosedCallback(new CompletedCallback() {
									@Override
									public void onCompleted(Exception ex) {
										isClose =
												true;
									}
								});
								mSocket.setEndCallback(new
										CompletedCallback() {
											@Override
											public void onCompleted(Exception ex) {

												isClose = true;

											}
										}); // heartbeatRunner.run();
							}
						});
			}
		});

		this.findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mSocket != null) {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < 5000; i++) {
						sb.append((i + ":============================================================"));
					}
					ByteBuffer m_header = ByteBuffer.allocate(4);
					ByteBuffer wrap = ByteBuffer.wrap(sb.toString().getBytes());
					NIOUtils.setPacketSizeInByteBuffer(m_header, m_header.capacity(),
							(int) wrap.remaining(), false);
					m_header.flip();
					ByteBuffer join = NIOUtils.join(m_header, wrap);

					mSocket.write(join);
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
