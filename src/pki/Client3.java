package pki;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
public class Client3 {
	private static String[] route = Planner.route;
	private static List<PublicKey> publickeys;
	public static KeyPair generateKeyPair () throws NoSuchAlgorithmException {
		// Generate a key-pair
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(512); // 512 is the keysize.
		KeyPair kp = kpg.generateKeyPair();
		return kp;
	}
	
	private static byte[] decrypt(byte[] inpBytes, PublicKey key, String xform) throws Exception{
		Cipher cipher = Cipher.getInstance(xform);
	    cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(inpBytes);
	}
	
	public static PublicKey getPublicKey(int portNumber) throws UnknownHostException, IOException, InvalidKeySpecException, NoSuchAlgorithmException{
		ServerSocket publickKeySocket = new ServerSocket(portNumber);
		while(true){
			Socket connectionSocket = publickKeySocket.accept();
			BufferedReader inFromClient1OrRouter2 = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
			String pubk = inFromClient1OrRouter2.readLine(); 
			byte[] decodedPublicKey = Base64.getDecoder().decode(pubk);
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPublicKey));
			return publicKey;
		}
	}
	
	private static Message readMsgFromRouter2(int portNumber) throws UnknownHostException, IOException, InvalidKeySpecException, NoSuchAlgorithmException, ClassNotFoundException{
		ServerSocket router2MsgSocket = new ServerSocket(portNumber);
		Socket connectionSocket = router2MsgSocket.accept();
		ObjectInputStream ois2 = new ObjectInputStream(connectionSocket.getInputStream());
		Message msgFromRouter2 = (Message)ois2.readObject();
		connectionSocket.close();
		return msgFromRouter2;
	}
	
	private static byte[] decryptMsg(byte[] encryptedMsg, PublicKey client1_publickey, PublicKey router2_publickey) throws Exception {
		String xform = "RSA/ECB/NoPadding";
		encryptedMsg = Client3.decrypt(encryptedMsg, router2_publickey, xform);
		String encodedByClient1 = Base64.getEncoder().encodeToString(encryptedMsg);
		System.out.println("first decrypted msg: " + encodedByClient1);
		
		encryptedMsg = Client3.decrypt(encryptedMsg, client1_publickey, xform);
		//String encodedHashMsg = Hex.encodeHex(encryptedMsg);
		StringBuffer sb = new StringBuffer();
		for (byte b : encryptedMsg) {
			sb.append(String.format("%02x", b & 0xff));
		}

		//System.out.println("original:" + original);

		//byte[] decodedMsg = Base64.getDecoder().decode(sb.toString());
		System.out.println("***digested(hex):" + sb.toString());
		//System.out.println("second decrypted msg: " + new String(encryptedMsg));
		//System.out.println("encoded hashed msg: " + encodedHash.length());
		String hash = sb.substring(sb.length() - 32);
		System.out.println("hash string length: " + hash.getBytes("UTF-8").length);
		
		return hash.getBytes("UTF-8");
	}
	
	
	public static void main(String[] args) throws Exception {
		//String msg = "hello world";
		//MessageDigest hashedMsg = MessageDigest.getInstance("MD5"); 
		//hashedMsg.update(msg.getBytes(), 0, msg.length());
		//String hashedStringMsg = new BigInteger(1, hashedMsg.digest()).toString(64); 
		//byte[] hashedMsg = MD5Hash.MD5Hash(msg);
		
		//String encodedhash = Base64.getEncoder().encodeToString(hashedMsg);
		//System.out.println("hashed msg: " + encodedhash);
		//System.out.println("hashed msg size: " + hashedMsg.length);
			
 		KeyPair client3_kp = Client3.generateKeyPair();
 		int client1PubkPort = 4444;
 		int router2PubkPort = 4445;
 		int msgFromRouter2Port = 5678;
 		// get router2 and client1 public keys
 		PublicKey router2_publickey= Client3.getPublicKey(router2PubkPort);
 		PublicKey client1_publickey= Client3.getPublicKey(client1PubkPort);

 		// message from router2
		Message msgFromRouter2 = Client3.readMsgFromRouter2(msgFromRouter2Port);
		byte[] decodedMsg = Base64.getDecoder().decode(msgFromRouter2.getEncodedMsg());

		String router2Msg = Base64.getEncoder().encodeToString(decodedMsg);
		System.out.println("reading msg: " + router2Msg);
		// decrypted message
		byte[] decryptedMsg = Client3.decryptMsg(decodedMsg, client1_publickey, router2_publickey);
		String original = msgFromRouter2.getOrginalMsg();
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(original.getBytes());
		byte[] digest = md.digest();
		StringBuffer sb = new StringBuffer();
		for (byte b : digest) {
			sb.append(String.format("%02x", b & 0xff));
		}

		System.out.println("original:" + original);
		System.out.println("digested(hex):" + sb.toString());

		System.out.println("digest length: " + sb.length());
		System.out.println(Arrays.equals(sb.toString().getBytes("UTF-8"), decryptedMsg));
	}

}
