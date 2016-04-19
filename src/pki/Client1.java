package pki;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import java.util.Base64;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Client1 {
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
		
		BufferedReader inFromClient1 = new BufferedReader( new InputStreamReader(System.in));
		Socket client1Socket = new Socket("localhost", portNumber);
		DataOutputStream outToClient3 = new DataOutputStream(client1Socket.getOutputStream());
		PublicKey pubk = kp.getPublic();
		String encoded = Base64.getEncoder().encodeToString(pubk.getEncoded());
		outToClient3.writeBytes(encoded + '\n');
		outToClient3.close();
	}
	
	private static void sendMsgToRouter2(KeyPair kp, int portNumber) throws Exception {
		String xform = "RSA/ECB/NoPadding";
		String msg;
		BufferedReader inFromClient = new BufferedReader( new InputStreamReader(System.in)); 
		msg = inFromClient.readLine();
		
		String original = msg;
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(original.getBytes());
		byte[] digest = md.digest();

		System.out.println("original:" + original);
		
		PrivateKey prvk = kp.getPrivate();
		
		Socket client1Socket = new Socket("localhost", portNumber);
		//DataOutputStream outToRouter2 = new DataOutputStream(client1Socket.getOutputStream());
		byte[] encryptedMsg = encrypt(digest, prvk, xform);
		String encodedMsg = Base64.getEncoder().encodeToString(encryptedMsg);
		System.out.println("sending msg: " + encodedMsg);
		String[] updatedRoute = {"Router2", "Client3"};
		
        Message msgObj = new Message(original, encodedMsg, updatedRoute);
        msgObj.toString();
        ObjectOutputStream oos1 = new ObjectOutputStream(client1Socket.getOutputStream());
        oos1.writeObject(msgObj);
        
		//outToRouter2.writeBytes(encodedMsg + '\n');
        client1Socket.close();
	}
	
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
 		KeyPair client1_kp = Client1.generateKeyPair();
 		int publicKeySenderPort = 4444;
 		int msgPort = 6789;
 		Client1.sendPublickey(client1_kp, publicKeySenderPort);
 		Client1.sendMsgToRouter2(client1_kp, msgPort);
	}

}
