import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * 
 * @author Todd Robbins
 * @description A chat server that delivers 
 * public and private messages.
 */
public class Server {

	private static ServerSocket serverSocket = null; // The server socket.
	private static Socket clientSocket = null; // The client socket.
	
	/*
	 * The AWS free server only has 1 Core 
	 * to simulate multiple (AS I am a broke college student)
	 * each thread is assigned to run on an array position "specific core" 
	 * though it will not enhance performance necessarily. Chat-Room\AWS server properties.PNG
	 */
	private static final int CoreSimulator = 4;
	private static final ChatThread[] chatThreads = new ChatThread[CoreSimulator]; // currently 4 threads

	/**
	 * main method for AWS threaded Server
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		String TimeStamp;

		int port = 1337; 
		if (args.length < 1) {
			System.out.println("Using TCP port number= " + port);
		} else {
			port = Integer.valueOf(args[0]).intValue();
		}
		try {
			serverSocket = new ServerSocket(port); // Open a server socket on the portNumber
		     TimeStamp = new java.util.Date().toString();
		     String process = "opening Socket" + port + " at " + TimeStamp +  (char) 13;
		     System.out.println(process);  
		} catch (IOException e) {
			System.out.println(e); //need to select different port
		     TimeStamp = new java.util.Date().toString();
		     String process = "need to select different port than" + port + " error at " + TimeStamp +  (char) 13;
		     System.out.println(process);  
		}
		
		while (true) {
			try {
				clientSocket = serverSocket.accept(); // Create a client socket
				for (int i = 0; i < CoreSimulator; i++) { //for each connection <=amount of threads/cores.
					if (chatThreads[i] == null) { //space available
						(chatThreads[i] = new ChatThread(clientSocket, chatThreads)).start(); //give client a thread
						i = CoreSimulator; 
					} else {
						PrintStream outputStream = new PrintStream(clientSocket.getOutputStream());
						outputStream.println("Server too busy for more users as it would effect performance. Try later.");
						outputStream.close();
						clientSocket.close();
					}
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}


/**
 * The chat/clients thread. 
 * This client thread opens the input and the output streams for a particular client. 
 * From there we perform the following functionality.  
 * 1. ask the client's name 
 * 2. informs all the clients connected to the server about the new client 
 * 3. adds client to the chat room.
 * 4. echos data back to all other clients. (as long as it receives data) 
 * 5. incoming messages are broadcast to all clients in chat
 * 6. When a client leaves the chat room, clients are informed and terminates.
 *
 * @author Todd Robbins
 *
 */
class ChatThread extends Thread {

	private String clientName = null; 
	private DataInputStream inputStream = null; //input Stream from Client
	private static BufferedReader inputLine = null; // The input Line
	private PrintStream outputLine = null; //print output from client
	private Socket clientSocket = null; //socket 
	private final ChatThread[] chatThreads; //array of threads
	
	/**
	 * constructor 
	 * @param clientSocket
	 * @param threads
	 */
	public ChatThread(Socket clientSocket, ChatThread[] threads) {
		this.clientSocket = clientSocket;
		this.chatThreads = threads;
	}
	/**
	 * 
	 */
	public void run() {
		ChatThread[] chatThreads = this.chatThreads;

		try {
			inputStream = new DataInputStream(clientSocket.getInputStream()); //input Streams for client
			inputLine = new BufferedReader(new InputStreamReader(inputStream)); //read chat input on server
			outputLine = new PrintStream(clientSocket.getOutputStream()); //output Streams for client
			String name = null;
			while (true && (name == null)) {
				outputLine.println("Enter name");
				name = inputStream.readUTF().trim();
				System.out.println("Client " + name + " connected");
			}

			synchronized (this) {
				for (ChatThread client : chatThreads) {
					if (client != null && client == this) {
						clientName = name;
					}
				}
			}
			while (true) { //enter chat room
				String line = inputLine.readLine();
				if (line.startsWith("/quit")) {
					break; //would use return but need to end the clients connection
				}
				if (line.startsWith("private")) { //private message
					String[] words = line.split("\\s", 0);
					if (words.length > 1 && words[1].equals("message")) { //message
						if (!words[words.length - 1].isEmpty()) {
							synchronized (this) {
								String msg = "";
								for (int i = 2; i < words.length - 1; i++) { //create message
									msg = msg + words[i];
								}
								for (ChatThread client : chatThreads) { //send to desired user
 									if (client != null && client != this && client.clientName != null && client.clientName
													.equals(words[words.length - 1])) {

										client.outputLine.println("@" + name + ": " + msg);
									}
								}
								System.out.println(name + " private message to " + words[words.length - 1]); //display message
							}
						}
					} else { //file
						synchronized (this) {
							for (ChatThread client : chatThreads) {
								if (client != null && client != this && client.clientName != null
										&& client.clientName.equals(words[words.length - 1])) {
									
									int index = words[2].lastIndexOf("/");
									String filename = words[2].substring(index + 1);
									FileInputStream input = new FileInputStream(words[2]);
									String target = "../"+ client.clientName + "/"+ filename;
									FileOutputStream output = new FileOutputStream(target);
									byte[] buffer = new byte[1024];
									int length;
									
									while ((length = input.read(buffer)) > 0) {
										output.write(buffer, 0, length); // Copy the bits from input stream to output stream
									}
									output.flush();
									input.close();
									output.close();
									client.outputLine.println("File " + filename + " was sent by " + name);
								}
							}
							System.out.println(name + " private file to " + words[words.length - 1]);
						}
					}
				} else if (line.startsWith("public")) { //show all clients
					String[] words = line.split("\\s", 3);
					if (words.length > 1 && words[1].equals("message") && words[2] != null) { //message
						synchronized (this) {
							for (ChatThread client : chatThreads) {
								if (client != null && client.clientName != null && client != this) {
									client.outputLine.println("@" + name + ": " + words[2]);
								}
							}
							System.out.println(name + " public message");
						}

					} else { //file
						synchronized (this) {
							for (ChatThread client : chatThreads) {
								if (client != null && client.clientName != null && client!= this) {
									int index = words[2].lastIndexOf("/");
									String filename = words[2].substring(index + 1);
									FileInputStream input = new FileInputStream(words[2]);
									String target = "../" +  client.clientName + "/" + filename;
									FileOutputStream output = new FileOutputStream(target);
									byte[] buffer = new byte[1024];
									int length;
									
									while ((length = input.read(buffer)) > 0) {
										output.write(buffer, 0, length); // Copy the bits from input stream to output stream
									}
									output.flush();
									input.close();
									output.close();
									client.outputLine.println("File " + filename + " was sent by " + name);
								}
							}
							System.out.println(name + " public file");
						}
					}
				} else {
					String[] words = line.split("\\s", 0);
					if (words.length > 1 && words[1].equals("message")) {
						if (!words[words.length - 1].isEmpty()) {
							synchronized (this) {
								String msg = "";
								for (int i = 2; i < words.length - 1; i++) {
									msg = msg + " " + words[i];
								}
								for (ChatThread client : chatThreads) {
									if (client != null && client != this && client.clientName != null
											&& !(client.clientName.equals(words[words.length - 1]))) {

										client.outputLine.println("@" + name + ": " + msg);
									}
								}
								System.out.println(name + " blockcast message excluding " + words[words.length - 1]);
							}
						}
					} else {
						synchronized (this) {
							for (ChatThread client : chatThreads) {
								if (client != null && client != this && client.clientName != null 
										&& !(client.clientName.equals(words[words.length - 1]))) {
									int index = words[2].lastIndexOf("/");
									String filename = words[2].substring(index + 1);
									FileInputStream input = new FileInputStream(words[2]);
									String target = "../" + client.clientName + "/" + filename;
									FileOutputStream output = new FileOutputStream(target);
									byte[] buffer = new byte[1024];
									int length;
									while ((length = input.read(buffer)) > 0) {
										output.write(buffer, 0, length); // Copy the bits from input stream to output stream
									}
									input.close();
									output.close();
									client.outputLine.println("File " + filename + " was sent by " + name);
								}
							}
							System.out.println(name + " blockcast file excluding " + words[words.length - 1]);
						}
					}
				}
			}
			synchronized (this) {
				for (ChatThread client : chatThreads) {
					if (client != null && client != this && client.clientName != null) {
						client.outputLine.println("*** The user " + name + " is leaving the chat room !!! ***");
					}
				}
			}
			outputLine.println("*** Bye " + name + " ***");

			/*
			 * Clean up. Set the current thread variable to null so that a new
			 * client could be accepted by the server.
			 */
			synchronized (this) {
				for (ChatThread client : chatThreads) {
					if (client == this) {
						client = null;
					}
				}
			}
			inputStream.close(); // close the input stream
			outputLine.close(); // close the output stream
			clientSocket.close(); // close the socket.
		} catch (IOException e) {
		}
	}
}

