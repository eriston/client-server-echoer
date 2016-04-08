package comp734assignment1rewrite4;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;



public class UtilitiesNIO {
	
	final static int maxBufferSize = 16384;

	public static void write(SelectionKey key , LinkedList<String> pendingChangesString) {
	SocketChannel socketChannel = (SocketChannel) key.channel();
	String sending;

	synchronized (pendingChangesString) {

		while (!pendingChangesString.isEmpty()) {

			ByteBuffer buffer = ByteBuffer.allocate(maxBufferSize);
			sending = pendingChangesString.getFirst();
			
			buffer.putInt(pendingChangesString.getFirst().length());

			buffer.put(pendingChangesString.getFirst().getBytes());
			buffer.flip();

			while (buffer.hasRemaining()) {
				try {
					socketChannel.write(buffer);
					
				} catch (IOException ie) {
					key.cancel();
					try {
						socketChannel.close();
					} catch (IOException e) {
						System.out.println("Unable to close socket on read exception");
						System.out.println(e);
					}
				} 
			}

			pendingChangesString.removeFirst();
			buffer.clear();
		}
		key.interestOps(SelectionKey.OP_READ);
		if(!pendingChangesString.isEmpty()) {
			key.interestOps(SelectionKey.OP_WRITE);
		}

	}
}
	
	public static ByteBuffer read(SelectionKey key, ByteBuffer buffer) {

		SocketChannel socketChannel = null;
	
		try {
			socketChannel = (SocketChannel) key.channel();
			socketChannel.read(buffer);
		} catch (IOException ie) {
			key.cancel();
			try {
				socketChannel.close();
			} catch (IOException e) {
				System.out.println("Unable to close socket on read exception");
				System.out.println(e);
			}
		} 
		
		try {
			key.interestOps(SelectionKey.OP_READ);
		} catch (CancelledKeyException e) {
			System.out.println("This key has been canceled, the client is probably disconnected");
			System.out.println("key: "+key);
		}
		return buffer;
	}

	public static void processReadBuffer(ByteBuffer readBuffer, ArrayList<String> receivedMessageHistory){
		synchronized (receivedMessageHistory) {
		readBuffer.flip();
		String desc = "";
		while(readBuffer.hasRemaining()){
			int n = readBuffer.getInt();
	        byte[] bytes = new byte[n]; 
	        readBuffer.get(bytes);
	        desc = new String(bytes);
	        receivedMessageHistory.add(desc);
		}
		readBuffer.compact();
		}
	}
	
}
