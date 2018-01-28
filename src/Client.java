import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * 
 * @author Todd Robbins
 * @description
 */
public class Client implements Runnable {
	private static Socket clientSocket = null; // The client socket
	private static PrintStream outputLine = null; // The output Line
	private static BufferedReader inputLine = null; // The input Line
	private static BufferedReader inputStreamLine = null; // The input Line
	private static DataInputStream inputStream = null; // The input Stream
	private static DataOutputStream outputStream = null; // The output Stream
	private static boolean closed = false;
	public static String name;
    private static final int PORT = 1337;

	/**
	 * main method to run the client connection to AWS server. 
	 * @param args
	 */
	public static void main(String[] args) {
		int port; 
		String TimeStamp;
//		String host = "localhost";
		if (args.length == 0) { // check to see if the String array is empty
			System.out.println("There were no commandline arguments passed!");
			System.exit(0);
		}
		name = args[0];
	    if (args.length < 2) {
	    	port = PORT;
	    } else {
	      name = args[0]; //server Address
	      port = Integer.valueOf(args[1]).intValue();
	    }

		try {
//		      /** Obtain an address object of the server */
//		     InetAddress name = InetAddress.getByName(host);
//		     port = PORT;
//		      /** Establish a socket connetion */
			 clientSocket = new Socket(name, port); // Open a socket on a given address and port.
		     TimeStamp = new java.util.Date().toString();
		     String process = "Calling the Socket Server on port " + port + " at " + TimeStamp +  (char) 13;
		     System.out.println(process);  
			inputLine = new BufferedReader(new InputStreamReader(System.in)); //read chat input on server
			outputLine = new PrintStream(clientSocket.getOutputStream()); //print text output to user
			inputStream = new DataInputStream(clientSocket.getInputStream()); //receive server data
			inputStreamLine = new BufferedReader(new InputStreamReader(inputStream)); //read chat input on server
			outputStream = new DataOutputStream(clientSocket.getOutputStream()); //send server data

		} catch (IOException e) {
			TimeStamp = new java.util.Date().toString();
			System.out.println("Couldn't get I/O for the connection" + name + "at" + TimeStamp +  (char) 13);
		}
		if (clientSocket != null && outputLine != null && inputStream != null) { //socket is open, text is written, and input still exists
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
	public void run() {
		String responseLine;
		try { 
			while ((responseLine = inputStreamLine.readLine()) != null) {

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