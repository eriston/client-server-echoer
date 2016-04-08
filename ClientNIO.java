package comp734assignment1rewrite4;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class ClientNIO implements Runnable {
	private String host;
	private int port;
	private String clientID;

	private Selector selector;
	private SocketChannel socket;

	private LinkedList<String> pendingChangesString = new LinkedList<String>();
	ByteBuffer readBuffer = ByteBuffer.allocate(UtilitiesNIO.maxBufferSize);

	private ArrayList<String> receivedMessageHistory = new ArrayList<String>();

	public void writeToSocket(String msg) {

		synchronized (pendingChangesString) {
			String msg1 = new String(msg);

			pendingChangesString.add(msg1);
		}

		// try {Thread.sleep(250);} catch (InterruptedException ie) {}

		try {
			socket.register(selector, SelectionKey.OP_WRITE);
		} catch (Exception ie) {
			ie.printStackTrace();
		}
		selector.wakeup();
	}

	public ClientNIO(String host, int port, String clientID) {
		this.host = host;
		this.port = port;
		this.clientID = clientID;
		try {
			socket = SocketChannel.open();
			socket.configureBlocking(false);

			socket.connect(new InetSocketAddress(host, port));

			selector = Selector.open();
			socket.register(selector, SelectionKey.OP_CONNECT);

		} catch (Exception e) {

		}

		new Thread(this).start();
		System.out.println("Started client: " + this.clientID);
	}

	public void run() {

		try {

			while (true) {

				int num = selector.select();

				if (num == 0) {
					continue;
				}

				Set keys = selector.selectedKeys();
				Iterator selectedKeysIterator = keys.iterator();

				while (selectedKeysIterator.hasNext()) {

					SelectionKey key = (SelectionKey) selectedKeysIterator.next();

					if (!key.isValid()) {
						continue;
					}

					if (key.isConnectable()) {
						SocketChannel keyChannel = (SocketChannel) key.channel();
						if (keyChannel.isConnectionPending()) {
							keyChannel.finishConnect();
						}
						key.interestOps(SelectionKey.OP_READ);
					} else if (key.isReadable()) {
						synchronized (readBuffer) {
							synchronized (receivedMessageHistory) {
								UtilitiesNIO.read(key, readBuffer);
								UtilitiesNIO.processReadBuffer(readBuffer, receivedMessageHistory);
								executeMessageHistory();

							}
						}
					} else if (key.isWritable()) {
						synchronized (pendingChangesString) {
							UtilitiesNIO.write(key, pendingChangesString);
						}
					}
					selectedKeysIterator.remove();
				}

			} // end while
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}

	private int nextMessageToExecute = 0;

	private void executeMessageHistory() {

		while (receivedMessageHistory.size() > nextMessageToExecute) {
			System.out.println(this.clientID + " got message: " + receivedMessageHistory.get(nextMessageToExecute));
			nextMessageToExecute++;
		}
	}

	private int receivedMessageHistoryNext = 0;

	public boolean hasNextCommand() {
		return receivedMessageHistory.size() > receivedMessageHistoryNext;
	}

	public String getNextCommand() {

		String ret = receivedMessageHistory.get(receivedMessageHistoryNext);
		receivedMessageHistoryNext++;
		return ret;
	}

	public void showMessageHistory() {
		System.out.println(this.clientID + " " + receivedMessageHistory);
	}

	String s = new String();

	static public void main(String args[]) throws Exception {
		String host = "localhost";
		int port = 6688;
		int maxRandClientId = 1000;
		new ClientNIO(host, port, "Client-" + (int) (Math.random() * maxRandClientId));
	}
}