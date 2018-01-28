import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
	 * though it will not enhance performance necessarily. 
	 */
	private static final int CoreSimulator = 4;
	private static final ChatThreads[] threads = new ChatThreads[CoreSimulator]; // currently 4 threads

	/**
	 * main method for AWS threaded Server
	 * 
	 * @param args
	 */
	public static void main(String args[]) {

		int portNumber = 2222; // The default port number.
		if (args.length < 1) {
			System.out.println("Using TCP port number=" + portNumber);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
		}
		try {
			serverSocket = new ServerSocket(portNumber); // Open a server socket on the portNumber
		} catch (IOException e) {
			System.out.println(e); //need to select different port
		}
		
		while (true) {
			try {
				clientSocket = serverSocket.accept(); // Create a client socket
				for (int i = 0; i < CoreSimulator; i++) { //for each connection <=10
					if (threads[i] == null) { //space available
						(threads[i] = new ChatThreads(clientSocket, threads)).start(); //give client a thread
						break; 
				//		i = maxClientsCount; 
					} else {
						PrintStream os = new PrintStream(clientSocket.getOutputStream());
						os.println("Server too busy. Try later.");
						os.close();
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
 * The chat clients thread. 
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
class ChatThreads extends Thread {

	private String clientName = null; 
	private DataInputStream inputStream = null; //input Stream from Client
	private PrintStream outputLine = null; //print output from client
	private Socket clientSocket = null; //socket 
	private final ChatThreads[] threads; //our thread
	private int maxClientsCount; //4
	
	/**
	 * constructor 
	 * @param clientSocket
	 * @param threads
	 */
	public ChatThreads(Socket clientSocket, ChatThreads[] threads) {
		this.clientSocket = clientSocket;
		this.threads = threads;
		maxClientsCount = threads.length;
	}
	/**
	 * 
	 */
	@SuppressWarnings("deprecation")
	public void run() {
		int maxClientsCount = this.maxClientsCount;
		ChatThreads[] threads = this.threads;

		try {
			/*
			 * Create input and output streams for this client.
			 */
			inputStream = new DataInputStream(clientSocket.getInputStream());
			outputLine = new PrintStream(clientSocket.getOutputStream());
			String name;
			while (true) {
				outputLine.println("Enter name");
				name = inputStream.readUTF().trim();
				System.out.println("Client " + name + " connected");
				if (name != null) {
					break;
				}
			}

			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] == this) {
						clientName = name;
						break;
					}
				}
			}
			/* Start the conversation. */
			while (true) {
				String line = inputStream.readLine();
				if (line.startsWith("/quit")) {
					break;
				}
				/* If the message is private sent it to the given client. */
				if (line.startsWith("unicast")) {
					String[] words = line.split("\\s", 0);
					if (words.length > 1 && words[1].equals("message")) {
						// words[1] = words[1].trim();
						if (!words[words.length - 1].isEmpty()) {
							synchronized (this) {
								String msg = "";
								for (int j = 2; j < words.length - 1; j++) {
									msg = msg + words[j];
								}
								for (int i = 0; i < maxClientsCount; i++) {
									if (threads[i] != null
											&& threads[i] != this
											&& threads[i].clientName != null
											&& threads[i].clientName
													.equals(words[words.length - 1])) {

										threads[i].outputLine.println("@" + name + ": "
												+ msg);
										/*
										 * Echo this message to let the client
										 * know the private message was sent.
										 */
										// this.os.println(">" + name + "> " +
										// words[1]);
										break;
									}
								}
								System.out.println(name
										+ " unicast message to "
										+ words[words.length - 1]);
							}
						}
					} else {
						synchronized (this) {
							for (int i = 0; i < maxClientsCount; i++) {
								if (threads[i] != null
										&& threads[i] != this
										&& threads[i].clientName != null
										&& threads[i].clientName
												.equals(words[words.length - 1])) {
									int index = words[2].lastIndexOf("/");
									String filename = words[2]
											.substring(index + 1);
									FileInputStream in = new FileInputStream(
											words[2]);
									String target = "../"
											+ threads[i].clientName + "/"
											+ filename;
									FileOutputStream out = new FileOutputStream(
											target);

									// Copy the bits from instream to outstream
									byte[] buf = new byte[1024];
									int len;
									while ((len = in.read(buf)) > 0) {
										out.write(buf, 0, len);
									}
									out.flush();
									in.close();
									out.close();
									threads[i].outputLine.println("File " + filename
											+ " was sent by " + name);
								}
							}
							System.out.println(name + " unicast file to "
									+ words[words.length - 1]);
						}
					}
				} else if (line.startsWith("broadcast")) {
					/* The message is public, broadcast it to all other clients. */
					String[] words = line.split("\\s", 3);
					if (words.length > 1 && words[1].equals("message")
							&& words[2] != null) {
						synchronized (this) {
							for (int i = 0; i < maxClientsCount; i++) {
								if (threads[i] != null
										&& threads[i].clientName != null
										&& threads[i] != this) {
									threads[i].outputLine.println("@" + name + ": "
											+ words[2]);
								}
							}
							System.out.println(name + " broadcasted message");
						}

					} else {
						synchronized (this) {
							for (int i = 0; i < maxClientsCount; i++) {
								if (threads[i] != null
										&& threads[i].clientName != null
										&& threads[i] != this) {
									int index = words[2].lastIndexOf("/");
									String filename = words[2]
											.substring(index + 1);
									FileInputStream in = new FileInputStream(
											words[2]);
									String target = "../"
											+ threads[i].clientName + "/"
											+ filename;
									FileOutputStream out = new FileOutputStream(
											target);

									// Copy the bits from instream to outstream
									byte[] buf = new byte[1024];
									int len;
									while ((len = in.read(buf)) > 0) {
										out.write(buf, 0, len);
									}
									out.flush();
									in.close();
									out.close();
									threads[i].outputLine.println("File " + filename
											+ " was sent by " + name);
								}
							}
							System.out.println(name + " broadcasted file");
						}
					}
				} else {
					String[] words = line.split("\\s", 0);
					if (words.length > 1 && words[1].equals("message")) {
						// words[1] = words[1].trim();
						if (!words[words.length - 1].isEmpty()) {
							synchronized (this) {
								String msg = "";
								for (int j = 2; j < words.length - 1; j++) {
									msg = msg + " " + words[j];
								}
								for (int i = 0; i < maxClientsCount; i++) {
									if (threads[i] != null
											&& threads[i] != this
											&& threads[i].clientName != null
											&& !(threads[i].clientName
													.equals(words[words.length - 1]))) {

										threads[i].outputLine.println("@" + name + ": "
												+ msg);
										/*
										 * Echo this message to let the client
										 * know the private message was sent.
										 */
										break;
									}
								}
								System.out.println(name
										+ " blockcast message excluding "
										+ words[words.length - 1]);
							}
						}
					} else {
						synchronized (this) {
							for (int i = 0; i < maxClientsCount; i++) {
								if (threads[i] != null && threads[i] != this && threads[i].clientName != null 
										&& !(threads[i].clientName.equals(words[words.length - 1]))) {
									int index = words[2].lastIndexOf("/");
									String filename = words[2].substring(index + 1);
									FileInputStream in = new FileInputStream(
											words[2]);
									String target = "../" + threads[i].clientName + "/" + filename;
									FileOutputStream out = new FileOutputStream(
											target);

									// Copy the bits from instream to outstream
									byte[] buf = new byte[1024];
									int len;
									while ((len = in.read(buf)) > 0) {
										out.write(buf, 0, len);
									}
									in.close();
									out.close();
									threads[i].outputLine.println("File " + filename + " was sent by " + name);
								}
							}
							System.out.println(name + " blockcast file excluding " + words[words.length - 1]);
						}
					}
				}
			}
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this && threads[i].clientName != null) {
						threads[i].outputLine.println("*** The user " + name + " is leaving the chat room !!! ***");
					}
				}
			}
			outputLine.println("*** Bye " + name + " ***");

			/*
			 * Clean up. Set the current thread variable to null so that a new
			 * client could be accepted by the server.
			 */
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
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

