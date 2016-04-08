package comp734assignment1rewrite4;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class ServerNIO implements Runnable {
	private int port;

	public ServerNIO(int port) {
		this.port = port;
		new Thread(this).start();
	}

	ByteBuffer readBuffer = ByteBuffer.allocate(UtilitiesNIO.maxBufferSize);
	private HashMap<SocketChannel, LinkedList<String>> pendingWritesBySocketString = new HashMap<SocketChannel, LinkedList<String>>();

	private ArrayList<SocketChannel> connectedSockets = new ArrayList<SocketChannel>();
	Selector selector = null;

	private ArrayList<String> receivedMessageHistory = new ArrayList<String>();
	private int nextMessageToExecute = 0;

	public void run() {
		try {
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);

			ServerSocket ss = ssc.socket();
			InetSocketAddress isa = new InetSocketAddress(port);
			ss.bind(isa);

			selector = Selector.open();

			ssc.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("Listening on port " + port);

			while (true) {

				int num = selector.select();

				if (num == 0) {
					continue;
				}

				Set keys = selector.selectedKeys();
				Iterator selectedKeysIterator = keys.iterator();

				while (selectedKeysIterator.hasNext()) {

					SelectionKey key = (SelectionKey) selectedKeysIterator.next();

					if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {

						Socket s = ss.accept();
						SocketChannel sc = s.getChannel();
						sc.configureBlocking(false);
						connectedSockets.add(sc);
						sc.register(selector, SelectionKey.OP_READ);
						System.out.println("connectedSockets->" + connectedSockets);

					} else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
						synchronized (readBuffer) {
							synchronized (receivedMessageHistory) {
								UtilitiesNIO.read(key, readBuffer);
								UtilitiesNIO.processReadBuffer(readBuffer, receivedMessageHistory);
								executeMessageHistory();
							}
						}
					} else if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
						synchronized (pendingWritesBySocketString) {
							UtilitiesNIO.write(key, pendingWritesBySocketString.get((key.channel())));
						}
					}
					selectedKeysIterator.remove();
				}

			}
		} catch (IOException ie) {
			System.err.println(ie);
		}
	}

	private void executeMessageHistory() {

		while (receivedMessageHistory.size() > nextMessageToExecute) {
			System.out.println("Server got message: " + receivedMessageHistory.get(nextMessageToExecute));
			writeToAllClients(receivedMessageHistory.get(nextMessageToExecute));
			nextMessageToExecute++;
		}
	}

	private void writeToClient(SocketChannel clientSocket, String message) {
		if (!clientSocket.isConnected()) {
			connectedSockets.remove(clientSocket);
			System.out.println("This client is not connected, removing from connected list");
			System.out.println("clientSocket: " + clientSocket);
			return;
		}
		synchronized (pendingWritesBySocketString) {
			if (pendingWritesBySocketString.containsKey(clientSocket)) {
				LinkedList<String> oldList = pendingWritesBySocketString.get(clientSocket);
				oldList.add(message);
				pendingWritesBySocketString.remove(clientSocket);
				pendingWritesBySocketString.put(clientSocket, oldList);

			} else {
				LinkedList<String> newList = new LinkedList<String>();
				newList.add(message);
				pendingWritesBySocketString.put(clientSocket, newList);

			}
		}
		try {
			clientSocket.register(selector, SelectionKey.OP_WRITE);
		} catch (ClosedChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		selector.wakeup();
	}

	private void writeToAllClients(String message) {
		// crashes with java.util.ConcurrentModificationException if a client has disconnected
		for (SocketChannel s : connectedSockets) {
			writeToClient(s, message);
		}

	}

	static public void main(String args[]) throws Exception {
		int port = 6688;
		new ServerNIO(port);
	}
}