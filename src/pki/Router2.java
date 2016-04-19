package pki;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import java.util.Base64;
import java.util.HashMap;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Router2 {
	private static String[] route = Planner.route;
	public static KeyPair generateKeyPair () throws NoSuchAlgorithmException {
		// Generate a key-pair
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(512); // 512 is the keysize.
		KeyPair kp = kpg.generateKeyPair();
		return kp;
	}
	
	private static byte[] encrypt(byte[] inpBytes, PrivateKey key, String xform) throws Exception {
		Cipher cipher = Cipher.getInstance(xform);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(inpBytes);
	}
	
	private static void sendPublickey(KeyPair kp, int portNumber) throws UnknownHostException, IOException{
		String xform = "RSA/ECB/NoPadding";
		BufferedReader inFromClient1 = new BufferedReader( new InputStreamReader(System.in));
		Socket client1Socket = new Socket("localhost", portNumber);
		DataOutputStream outToClient3 = new DataOutputStream(client1Socket.getOutputStream());
		PublicKey pubk = kp.getPublic();
		String encoded = Base64.getEncoder().encodeToString(pubk.getEncoded());
		outToClient3.writeBytes(encoded + '\n');
		outToClient3.close();
	}
	
	private static Message readMsgFromClient1(int portNumber) throws UnknownHostException, IOException, InvalidKeySpecException, NoSuchAlgorithmException, ClassNotFoundException{
		ServerSocket client1MsgSocket = new ServerSocket(portNumber);
		Socket connectionSocket = client1MsgSocket.accept();
		ObjectInputStream ois1 = new ObjectInputStream(connectionSocket.getInputStream());
		Message msgFromClient1 = (Message)ois1.readObject();
		connectionSocket.close();
		return msgFromClient1;
		
		/*BufferedReader inFromClient1 = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
		String msg = inFromClient1.readLine(); 
		byte[] decodedMsg = Base64.getDecoder().decode(msg);
		client1MsgSocket.close();
		return decodedMsg;*/
	}
	
	private static void sendMsgToClient3(KeyPair kp, Message msg,int portNumber) throws Exception {
		String xform = "RSA/ECB/NoPadding";
		PrivateKey prvk = kp.getPrivate();
		Socket router2Socket = new Socket("localhost", portNumber);
		byte[] decodedMsg = Base64.getDecoder().decode(msg.getEncodedMsg());
		
		
		
		//DataOutputStream outToClient3 = new DataOutputStream(router2Socket.getOutputStream());
		byte[] encryptedMsg = encrypt(decodedMsg, prvk, xform);
		String encodedMsg = Base64.getEncoder().encodeToString(encryptedMsg);
		msg.setEncodedMsg(encodedMsg);
		String[] updatedRoute = {"Client3"};
		msg.setRoute(updatedRoute);
		System.out.println("sending msg: " + msg.toString());
		
        ObjectOutputStream oos1 = new ObjectOutputStream(router2Socket.getOutputStream());
        oos1.writeObject(msg);
        router2Socket.close();
		//outToClient3.writeBytes(encodedMsg + '\n');
		//outToClient3.close();
	}
	
	public static void main(String[] args) throws Exception {

		String xform = "RSA/ECB/NoPadding";
 		KeyPair router2_kp = Router2.generateKeyPair();
 		int publicKeySenderPort = 4445;
 		int client1MsgPort = 6789;
 		int toClient3Port = 5678;
 		Router2.sendPublickey(router2_kp, publicKeySenderPort);
 		Message msgFromClient1 = Router2.readMsgFromClient1(client1MsgPort);
 		msgFromClient1.toString();
 		//System.out.println("MsgFromClient1 Size: " + msgFromClient1.length);
 		Router2.sendMsgToClient3(router2_kp, msgFromClient1, toClient3Port);
	}


}
