package org.crypto.sse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objenesis.strategy.SerializingInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import de.javakaffee.kryoserializers.guava.ArrayListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ArrayTableSerializer;
import de.javakaffee.kryoserializers.guava.HashBasedTableSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.LinkedHashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.LinkedListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.TreeBasedTableSerializer;
import de.javakaffee.kryoserializers.guava.TreeMultimapSerializer;

public class BIEXRR2LevClient {
	String[] query;
	List<TokenDIS> token;
	BIEXRR2LevServer server;
	Map<String, TokenDIS> keywordToToken;
	ArrayList<Map<String, TokenDIS>> keywordToTokenCopies = null;
	int keywordToTokenNextIndex = 0;

	public static <T> T clone(T obj) {
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
	
		return kryo.copy(obj);
	}

	
	public BIEXRR2LevClient(String[] query, List<TokenDIS> token, BIEXRR2LevServer server) {
		this.token = token;
		this.server = server;
		keywordToToken = new HashMap<String, TokenDIS>();
		for(int i=0; i<query.length; i++) {
			keywordToToken.put(query[i], token.get(i));
		}
	}
	
	private List<TokenDIS> subsetToken(List<String> subsetSearchBol) throws Exception {
		List<TokenDIS> subsetTokenList = new ArrayList<TokenDIS>();
		for(int i=0; i<subsetSearchBol.size();i++) {
			String subSearchKeyword = subsetSearchBol.get(i);
			TokenDIS subSearchKeywordToken = getNextKeywordToTokenCopy().get(subSearchKeyword);

			List<byte[][]> tokenMMLocal = new ArrayList<byte[][]>();
			for(int j=i+1; j<subsetSearchBol.size();j++) {
				String otherKeyword = subsetSearchBol.get(j);
				byte[][] lmmToken = subSearchKeywordToken.getMm().get(subSearchKeyword+"^"+otherKeyword);
				if(lmmToken != null)
					tokenMMLocal.add(lmmToken);
				else {
					System.out.println("WARNING: subsetToken("+subsetSearchBol+") i="+i+", j="+j+", otherKeyword:"+otherKeyword);
				}
			}
			subSearchKeywordToken.setTokenMMLocal(tokenMMLocal);
			subsetTokenList.add(subSearchKeywordToken);
		}
		return subsetTokenList;
	}	

	public Set<String> submitSearch(String[][] subquery) throws Exception {
		System.out.println("Client: Creating sub-search.....");

		try {
			subTokenResourcesPrepare(subquery, 1);
			Map<String, List<TokenDIS>> token = subToken(subquery);
			Path serializedFileName = Files.createTempFile("dumpfiles", ".tmp.clientsearchtoken");
			BIEXRR2LevDataOwner.serialiseT("clientsearchtoken", serializedFileName, token);
			Files.delete(serializedFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}

		final int clientN = 10;
		subTokenResourcesPrepare(subquery, clientN);
		Map<String, List<TokenDIS>> token = null;
		Instant start = Instant.now();
		for(int i = 0; i< clientN; i++)
			token = subToken(subquery);
		Instant end = Instant.now();
		System.out.println("Client: Subtoken created. Time taken (ns): " + Duration.between(start, end).toNanos() + "ns in "+clientN+" iterations. Per iteration: "+Duration.between(start, end).toNanos()/clientN+"ns");
		
		for(Map.Entry<String, List<TokenDIS>> sTokenEntry: token.entrySet()) {
			for(TokenDIS tdis: sTokenEntry.getValue()) {
				tdis.makeProper();
			}
		}
		

		final int serverN = 10;
		Set<String> tmpBol = null;
		start = Instant.now();
		for(int i = 0; i< serverN; i++)
			tmpBol = server.serverSearch(token);
		end = Instant.now();
		System.out.println("Server search time taken (ns): " + Duration.between(start, end).toNanos() + "ns in "+serverN+" iterations. Per iteration: "+Duration.between(start, end).toNanos()/serverN+"ns");
		
		return tmpBol;
	}

	public void subTokenResourcesPrepare(String[][] query, int repetitions) throws Exception {
		int copiesRequired = 0;
		for (int i = 1; i < query.length; i++) {
			for (int k = 0; k < query[0].length; k++) {
				List<String> searchTMP = new ArrayList<String>();
				searchTMP.add(query[0][k]);
				
				for (int r = 0; r < query[i].length; r++) {
					searchTMP.add(query[i][r]);
				}
				copiesRequired += searchTMP.size();
			}
		}
		copiesRequired += query[0].length;
		copiesRequired *= repetitions;

		keywordToTokenCopies = new ArrayList<Map<String, TokenDIS>>();
		keywordToTokenNextIndex = 0;
		
		for(int c=0; c<copiesRequired; c++)
			keywordToTokenCopies.add(clone(keywordToToken));
		
	}
	
	private Map<String, TokenDIS> getNextKeywordToTokenCopy() throws Exception {
		if(keywordToTokenCopies != null) {
			Map<String, TokenDIS> keywordToTokenCopy = keywordToTokenCopies.get(keywordToTokenNextIndex);
			keywordToTokenNextIndex++;
			return keywordToTokenCopy;
		} else {
			throw new Exception("No keywordToToken copies available. Use subTokenResourcesPrepare() to prepare enough copies.");
		}
	}
	
	public Map<String, List<TokenDIS>> subToken(String[][] query) throws Exception {
		Map<String, List<TokenDIS>> token = new HashMap<String, List<TokenDIS>>();

		for (int i = 1; i < query.length; i++) {
			for (int k = 0; k < query[0].length; k++) {
				List<String> searchTMP = new ArrayList<String>();
				searchTMP.add(query[0][k]);
				
				for (int r = 0; r < query[i].length; r++) {
					searchTMP.add(query[i][r]);
				}
				
				List<TokenDIS> tokenTMP = subsetToken(searchTMP);
				token.put(i+" "+k, tokenTMP);
			}
		}
		
		List<String> generalQ = new ArrayList<String>(Arrays.asList(query[0]));
		List<TokenDIS> tokenGeneral = subsetToken(generalQ);
		token.put(query.length+" "+query[0].length, tokenGeneral);	
		
		return token;
	}
}
