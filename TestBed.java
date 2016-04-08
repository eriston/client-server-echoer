package comp734assignment1rewrite4;

public class TestBed {
	public static void main(String[] args) {
		ServerNIO server = new ServerNIO(6688);

		sleep();

		ClientNIO client1 = new ClientNIO("localhost", 6688, "Client1");
		ClientNIO client2 = new ClientNIO("localhost", 6688, "Client2");
		ClientNIO client3 = new ClientNIO("localhost", 6688, "Client3");

		sleep();

		client1.writeToSocket("first");
		client2.writeToSocket("2nd");
		client2.writeToSocket("3rd");
		client1.writeToSocket("second");
		client3.writeToSocket("third");
		client1.writeToSocket("four");
		client1.writeToSocket("fiv");
		client1.writeToSocket("fiv");

		sleep();
		System.out.println();
		client1.showMessageHistory();
		client2.showMessageHistory();
		client3.showMessageHistory();
	}

	private static void sleep() {
		int sleepTime = 1000;
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException ie) {
		}
	}
}
