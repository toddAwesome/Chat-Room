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
	private static DataInputStream inputStream = null; // The input Stream
	private static DataOutputStream outputStream = null; // The output Stream
	private static BufferedReader inputLine = null; // The input Line
	private static boolean closed = false;
	public static String name;
    private static final int PORT = 2222;

	/**
	 * main method to run the client connection to AWS server. 
	 * @param args
	 */
	public static void main(String[] args) {
		int port; 
		String TimeStamp;
//		String host = "localhost"; //test on my own machine
		String host = "ec2-18-218-247-9.us-east-2.compute.amazonaws.com"; //aws server IP address
		if (args.length == 0) { // check to see if the String array is empty
			System.out.println("There were no arguments passed!");
			System.out.println("enter a client name into Program Arguments");
			System.exit(0);
		}
		name = args[0]; //client name entered in for arguments. 
	    if (args.length < 2) {
	    	port = PORT;
	    } else {
	      port = Integer.valueOf(args[1]).intValue(); //custom port
	    }

		try {
			 clientSocket = new Socket(host, port); // Open a socket on a given address and port.
			 
		     TimeStamp = new java.util.Date().toString();
		     String process = "Calling the Socket Server on port " + port + " at " + TimeStamp +  (char) 13;
		     System.out.println(process);  // gives time stamp
		     
			inputLine = new BufferedReader(new InputStreamReader(System.in)); //read chat input on server
			outputLine = new PrintStream(clientSocket.getOutputStream()); //print text output to user
			inputStream = new DataInputStream(clientSocket.getInputStream()); //receive server data
			outputStream = new DataOutputStream(clientSocket.getOutputStream()); //send server data

		}  catch (UnknownHostException e) {
			TimeStamp = new java.util.Date().toString();
		    System.out.println("Don't know about host " + host + " at " + TimeStamp +  (char) 13);
	    } catch (IOException e) {
			TimeStamp = new java.util.Date().toString();
			System.out.println("Couldn't get I/O for the connection " + host + " at " + TimeStamp +  (char) 13);
		}
		if (clientSocket != null && outputLine != null && inputStream != null) { //socket is open, text is written, and input still exists
			try {
				new Thread(new Client()).start(); // Create a thread to read from the server/Chat Room.
				while (!closed) {
					System.out.println("Please enter on of the following commands as String");
					System.out.println("1. public message "); 
					System.out.println("2. private message "); 
					System.out.println("3. 'insert Text' message username - excluding message of entered username"); 
					System.out.println("4. /quit"); 
					outputLine.println(inputLine.readLine());
					System.out.println("Message sent");
				}
				outputLine.close(); //Close printStream stream
				inputStream.close(); //close the input stream
				outputStream.close(); //Close output stream
				clientSocket.close(); //close the socket.
			} catch (IOException e) {
				System.out.println("IOException:  " + e);
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
		try { 
			while ((inputStream.readLine() != null) &&
					(inputStream.readLine().indexOf("*** Bye") != -1)) {
				 
				if (inputStream.readLine().equals("Enter name")) {
					outputLine.println(name);
				} else {
					System.out.println(inputStream.readLine());
				}
			}
			closed = true;
		} catch (IOException e) {
			System.out.println("IOException:  " + e);
		}
	}
}