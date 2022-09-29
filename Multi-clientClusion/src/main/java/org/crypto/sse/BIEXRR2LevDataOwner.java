package org.crypto.sse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.crypto.NoSuchPaddingException;

import org.objenesis.strategy.SerializingInstantiatorStrategy;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.google.common.collect.SetMultimap;
import de.javakaffee.kryoserializers.guava.ArrayListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ArrayTableSerializer;
import de.javakaffee.kryoserializers.guava.HashBasedTableSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.LinkedHashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.LinkedListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.TreeBasedTableSerializer;
import de.javakaffee.kryoserializers.guava.TreeMultimapSerializer;

public class BIEXRR2LevDataOwner {
	public static int bigBlock = 1000;
	public static int smallBlock = 10;
	
	public static List<byte[]> listSKs;
	public static IEX2Lev disj;
	
	public static class SerializedProcessing implements Serializable {
		private static final long serialVersionUID = 1L;

		public List<byte[]> listSKs;
		public IEX2Lev disj;
		public SetMultimap<String, String> lp1;
		public SetMultimap<String, String> lp2;
		
		public SerializedProcessing() {}
	}
	
	public static <T> T deserialise(Path serializedFileName, Class<T> typeParameterClass) throws Exception{
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new SerializingInstantiatorStrategy()));
		ArrayListMultimapSerializer.registerSerializers( kryo );
		HashMultimapSerializer.registerSerializers( kryo );
		LinkedHashMultimapSerializer.registerSerializers( kryo );
		LinkedListMultimapSerializer.registerSerializers( kryo );
		TreeMultimapSerializer.registerSerializers( kryo );
		ArrayTableSerializer.registerSerializers( kryo );
		HashBasedTableSerializer.registerSerializers( kryo );
		TreeBasedTableSerializer.registerSerializers( kryo );

		if(Files.exists(serializedFileName) && !Files.isDirectory(serializedFileName)) { 
		    // load existing
			Input input = new Input(new FileInputStream(serializedFileName.toFile()));
			T obj2 = kryo.readObject(input, typeParameterClass);
			input.close(); 		
			return obj2;
		} else {
			// error
			return null;
		}
	}
	
	public static <T> void serialise(Path serializedFileName, SerializedProcessing sp) throws Exception{
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new SerializingInstantiatorStrategy()));
		ArrayListMultimapSerializer.registerSerializers( kryo );
		HashMultimapSerializer.registerSerializers( kryo );
		LinkedHashMultimapSerializer.registerSerializers( kryo );
		LinkedListMultimapSerializer.registerSerializers( kryo );
		TreeMultimapSerializer.registerSerializers( kryo );
		ArrayTableSerializer.registerSerializers( kryo );
		HashBasedTableSerializer.registerSerializers( kryo );
		TreeBasedTableSerializer.registerSerializers( kryo );

		if (sp != null) {
			// save passed
			Instant start = Instant.now();
			Output output = new Output(new FileOutputStream(serializedFileName.toFile()));
			kryo.writeObject(output, sp);
			output.flush();
			output.close();
			System.out.println("BIEXRR2LevDataOwner.serialise()/duration: " + Duration.between(start, Instant.now()).toNanos() + "ns");
			System.out.println("BIEXRR2LevDataOwner.serialise()/storageSize: " + Files.size(serializedFileName) + "bytes");
			
			
		} else {
			// error
			throw new Exception("Cannot serialise a null object");
		}
	}

	public static <T> void serialiseT(String target, Path serializedFileName, T sp) throws Exception{
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new SerializingInstantiatorStrategy()));
		ArrayListMultimapSerializer.registerSerializers( kryo );
		HashMultimapSerializer.registerSerializers( kryo );
		LinkedHashMultimapSerializer.registerSerializers( kryo );
		LinkedListMultimapSerializer.registerSerializers( kryo );
		TreeMultimapSerializer.registerSerializers( kryo );
		ArrayTableSerializer.registerSerializers( kryo );
		HashBasedTableSerializer.registerSerializers( kryo );
		TreeBasedTableSerializer.registerSerializers( kryo );

		if (sp != null) {
			// save passed
			Instant start = Instant.now();
			Output output = new Output(new FileOutputStream(serializedFileName.toFile()));
			kryo.writeObject(output, sp);
			output.flush();
			output.close();
			System.out.println("BIEXRR2LevDataOwner.serialise()/"+target+"/duration: " + Duration.between(start, Instant.now()).toNanos() + "ns");
			System.out.println("BIEXRR2LevDataOwner.serialise()/"+target+"/storageSize: " + Files.size(serializedFileName) + "bytes");
		
			
		} else {
			// error
			throw new Exception("Cannot serialise a null object");
		}
	}
	
	
	private static IEX2Lev createServerData(List<byte[]> listSKs, int maxParallelThreads)
			throws InterruptedException, ExecutionException, IOException {
		Instant start = Instant.now();
		IEX2Lev disj = IEX2Lev.setup(listSKs, TextExtractPar.lp1, TextExtractPar.lp2, bigBlock, smallBlock, 0, maxParallelThreads);
		System.out.println("BIEXRR2LevDataOwner.createServerData(): " + Duration.between(start, Instant.now()).toNanos() + "ns");
		return disj;
	}

	private static List<byte[]> createKeys() {
		List<byte[]> listSKs = new ArrayList<byte[]>();
		listSKs.add(new byte[32]);
		listSKs.add(new byte[32]);
		listSKs.add(new byte[32]);
		return listSKs;
	}

	private static void parseData(String genDataDir) throws IOException, InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeySpecException {
		String pathName = genDataDir;
	
		ArrayList<File> listOfFile = new ArrayList<File>();
		
	
		Instant start = Instant.now();
		TextProc.listf(pathName, listOfFile);
		
	
		TextProc.TextProc(false, pathName);
		System.out.println("BIEXRR2LevDataOwner.parseData(): " + Duration.between(start, Instant.now()).toNanos() + "ns");
	}
	
	public static void init(String genDataDir, int smallBlockParam, int maxParallelThreads) throws Exception {
		BIEXRR2LevDataOwner.smallBlock = smallBlockParam;
		Path serializedFileName = Paths.get("gen-data-processed-MultiClient:"+genDataDir+":"+smallBlockParam+".bin");
		SerializedProcessing l = deserialise(serializedFileName, SerializedProcessing.class);
		if(l == null) {
			System.out.println("Did not find existing serialised file. Processing and serialising now....");
			parseData(genDataDir);
			listSKs = createKeys();
			disj = createServerData(listSKs, maxParallelThreads);
			SerializedProcessing sp = new SerializedProcessing();
			sp.listSKs = listSKs;
			sp.disj = disj;
			sp.lp1 = TextExtractPar.lp1;
			sp.lp2 = TextExtractPar.lp2;
			serialise(serializedFileName, sp);
			System.out.println("Serialised into " + serializedFileName);
		} else {
			System.out.println("Found existing serialised file. Deserialising now....");
			listSKs = l.listSKs;
			disj = l.disj;
			TextExtractPar.lp1 = l.lp1;
			TextExtractPar.lp2 = l.lp2;
			System.out.println("Deserialised from " + serializedFileName);
			System.out.println("LP1: size="+l.lp1.size()+" keys.size="+l.lp1.keySet().size());
			System.out.println("LP2: size="+l.lp2.size()+" keys.size="+l.lp2.keySet().size());
		}
	}
	
	public static List<TokenDIS> createToken(String[] allKeywords, boolean serialise)
			throws UnsupportedEncodingException {
		List<TokenDIS> allKeywordsToken = IEX2Lev.tokenWithSelf(listSKs, Arrays.asList(allKeywords));
		if(serialise) {
			try {
				Path serializedFileName = Files.createTempFile("dumpfiles", ".tmp.supertoken");
				serialiseT("supertoken", serializedFileName, allKeywordsToken);
				Files.delete(serializedFileName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return allKeywordsToken;
	}
	
	public static List<String> getKeywords(){
		return new ArrayList<String>(TextExtractPar.lp1.keySet());
	}

}
