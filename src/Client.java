import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * 
 * @author Todd Robbins
 * @description
 */
public class Client implements Runnable {
	private static Socket clientSocket = null; // The client socket
	private static PrintStream outputLine = null; // The output Line
	private static BufferedReader inputLine = null; // The input Line
	private static DataInputStream inputStream = null; // The input Stream
	private static DataOutputStream outputStream = null; // The output Stream
	private static boolean closed = false;
	public static String name;

	/**
	 * main method to run the client connection to AWS server. 
	 * @param args
	 */
	public static void main(String[] args) {
		int portNumber; // The default port.
		String host = "localhost"; // The default host.
		name = args[0]; //server Address
		if (args.length < 2) { //
			portNumber = 2222;
		} else {
			portNumber = Integer.valueOf(args[1]).intValue();
		}

		try {
			clientSocket = new Socket(host, portNumber); // Open a socket on a given host and port.
			inputLine = new BufferedReader(new InputStreamReader(System.in)); //read chat input on server
			outputLine = new PrintStream(clientSocket.getOutputStream()); //print text output to user
			inputStream = new DataInputStream(clientSocket.getInputStream()); //receive server data
			outputStream = new DataOutputStream(clientSocket.getOutputStream()); //send server data

		} catch (UnknownHostException e) {
			System.out.println("Don't know about host " + host);
		} catch (IOException e) {
			System.out.println("Couldn't get I/O for the connection to the host " + host);
		}

		/*
		 * If everything's initialized, then we can write some data to
		 * the socket we have opened a connection to on the port portNumber.
		 */
		if (clientSocket != null && outputLine != null && inputStream != null) { //socket is open, text is wriiten, and input still exists
			try {
				new Thread(new Client()).start(); // Create a thread to read from the server/Chat Room.
				while (!closed) {
					System.out.println("Please enter command as a String");
					outputLine.println(inputLine.readLine());
					System.out.println("Message sent");
				}
				outputLine.close(); //Close the output stream
				inputStream.close(); //close the input stream
				outputStream.close();
				clientSocket.close(); //close the socket.
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
			}
		}
	}

	/**
	 * Create a thread to read from the server.
	 * Keep on reading from the socket 
	 * until we receive "Bye" from the server. 
	 * In this case we end connection. 
	 * 
	 */
	@SuppressWarnings("deprecation")
	public void run() {
		String responseLine;
		try { 
			while ((responseLine = inputStream.readLine()) != null) {

				if (responseLine.equals("Enter name")) {
					outputLine.println(name);
				} else {
					System.out.println(responseLine);
				}
				if (responseLine.indexOf("*** Bye") != -1)
					break;
			}
			closed = true;
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}
	}
}