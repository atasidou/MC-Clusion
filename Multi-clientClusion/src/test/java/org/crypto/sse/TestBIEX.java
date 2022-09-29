package org.crypto.sse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.crypto.NoSuchPaddingException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class TestBIEX {
	
	public static void main(String[] args) throws Exception {
		if(args.length != 6) {
			System.out.println("Syntax: program <generated-data-dir/> <smallBlock> <maxParallelThreads> <numberOfQueries> <randomQuerySelectionSeed> <queriesFilename>");
			System.out.println("Example: TestBIEX gen-data-dir-1000/ 20 8 1000 1 queries.json");
			System.exit(-1);
		}
		String genDataDir = args[0]; 
		String smallBlockStr = args[1];
		int smallBlockParam = Integer.parseInt(smallBlockStr);
		String maxParallelThreadsStr = args[2];
		int maxParallelThreads = Integer.parseInt(maxParallelThreadsStr);
		int numberOfQueries = Integer.parseInt(args[3]);
		int randomQuerySelectionSeed = Integer.parseInt(args[4]);
		String loadJsonQueriesFilename = args[5];
		System.out.println("Running TestBIEX: "+genDataDir+"|"+smallBlockStr+"|"+maxParallelThreadsStr+"|"+numberOfQueries+"|"+randomQuerySelectionSeed+"|"+loadJsonQueriesFilename);
		
		Printer.addPrinter(new Printer(Printer.LEVEL.EXTRA));
		
		Instant start =  null;
		start = Instant.now();
		BIEXRR2LevDataOwner.init(genDataDir, smallBlockParam, maxParallelThreads);
		System.out.println("BIEXRR2LevDataOwner.init(): " + Duration.between(start, Instant.now()).toNanos() + "ns");
		for(String keyword: TextExtractPar.lp1.keySet()) {
			System.out.println("Global Selectivity of keyword |"+keyword+"|:"+TextExtractPar.lp1.get(keyword).size());
		}
		System.out.println("maxParallelThreads="+maxParallelThreads);
		
		ArrayList<Map.Entry<Integer, String>> keywordSelectivities = new ArrayList<>();
		TextExtractPar.lp1.asMap().forEach((k, v) -> keywordSelectivities.add(Map.entry(Integer.valueOf(v.size()), k)));
		keywordSelectivities.sort(Collections.reverseOrder(Map.Entry.comparingByKey()));
		keywordSelectivities.forEach(sk -> System.out.println(sk));
		ArrayList<String> keywords = new ArrayList<>();
		keywordSelectivities.forEach(sk -> keywords.add(sk.getValue()));

		Map<String, Object> jsonQueriesAndKeywords = readJson(loadJsonQueriesFilename);
		List<List<List<String>>> jsonQueries = (List<List<List<String>>>)jsonQueriesAndKeywords.get("queries");
		
		List<String [][]> queries = new ArrayList<String [][]>();
		Set<String> allKeywordsSet = new HashSet<String>();

		for(List<List<String>> query: jsonQueries) {
			String [][] resultQuery = new String [query.size()][];
			for(int i = 0; i< query.size(); i++) {
				resultQuery[i] = new String[query.get(i).size()];
				for(int j =0; j< query.get(i).size(); j++) {
					resultQuery[i][j] = (String) query.get(i).get(j);
					allKeywordsSet.add(resultQuery[i][j]);
				}
			}
			queries.add(resultQuery);
		}
		String[] allKeywords = allKeywordsSet.toArray(new String[0]);
		serverSearch(allKeywords, queries);
		originalBIEXTester(allKeywords, queries);
	}
	
	public static void originalBIEXTester(String[] allKeywords, List<String[][]> queries) throws Exception{
		System.out.println("originalBIEXTester: Initial total Combinations evaluated: "+queries.size());
		

		int combinationIndex = -1;
		for(String[][] query: queries) {
			combinationIndex++;
			System.out.println("----------------------------------Combination---------");
			System.out.println("Search: [" + combinationIndex+ "]: "+ BIEXRR2LevVerifier.json(query)+ "\n");
			final int tokenN = 10;
			Instant start = Instant.now();
			Map<String, List<TokenDIS>> token = null;
			for(int i=0; i< tokenN; i++)
				token = token_BIEX(BIEXRR2LevDataOwner.listSKs, query);
			Instant end = Instant.now();
			System.out.println("token_BIEX(): " + Duration.between(start, end).toNanos() + "ns in "+tokenN+" iterations. Per iteration: "+Duration.between(start, end).toNanos()/tokenN+"ns");
			
			final int searchN = 10;
			Set<String> tmpBol = null;
			start = Instant.now();
			for(int i=0; i< searchN; i++)
				tmpBol = query_BIEX(BIEXRR2LevDataOwner.disj, token);	
			end = Instant.now();
			System.out.println("query_BIEX(): " + Duration.between(start, end).toNanos() + "ns in "+searchN+" iterations. Per iteration: "+Duration.between(start, end).toNanos()/searchN+"ns");
			
			tmpBol = new HashSet<String>(tmpBol);
			
//			System.out.println("Final result count: " + tmpBol.size());
			Set<String> correct = new BIEXRR2LevVerifier().submitSearch(query);
			System.out.println("Selectivity of query |"+BIEXRR2LevVerifier.json(query)+"|:"+correct.size());
			System.out.println("Verification: "+combinationIndex+", Verdict: " + (correct.equals(tmpBol) ? "OK" : "Error!! Unequal!"));
			if(!correct.equals(tmpBol)) {
				Set<String> correctDiffResult = new HashSet<String>(correct);
				correctDiffResult.removeAll(tmpBol);
				Set<String> resultDiffCorrect= new HashSet<String>(tmpBol);
				resultDiffCorrect.removeAll(correct);
				Set<String> resultIntersectionCorrect= new HashSet<String>(tmpBol);
				resultIntersectionCorrect.retainAll(correct);
				System.out.println("Correct / response:  \n" + BIEXRR2LevVerifier.json(correctDiffResult)); 
				System.out.println("Response / Correct:  \n" + BIEXRR2LevVerifier.json(resultDiffCorrect));
				System.out.println("Response ^ Correct:  \n" + BIEXRR2LevVerifier.json(resultIntersectionCorrect));
				
			}
		}
	}


	static Map<String, Object> readJson(String filename) throws IOException {
		Path filePath = Path.of(filename);

		String json = Files.readString(filePath);

		HashMap<String, Object> map = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		// Convert Map to JSON
		map = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

		return map;
	}	
	
	

	public static void combinationSearchTester(String[] allKeywords1, int numberOfQueries, int randomQuerySelectionSeed, int maxKeywordsPerDisjunction) throws Exception {
		List<String> keptKeywords = new ArrayList<String>();
		for(String keyword: allKeywords1) {
			if(!TextExtractPar.lp1.containsKey(keyword)) {
				System.out.println("Filtered keyword |"+keyword+"|");
			}else {
				System.out.println("Selectivity of keyword |"+keyword+"|:"+TextExtractPar.lp1.get(keyword).size());
				keptKeywords.add(keyword);
			}
		}
		allKeywords1 = keptKeywords.toArray(new String[0]);
		
		Set<Set<String>> allKeywords1Powerset = Sets.powerSet(ImmutableSet.copyOf(allKeywords1)); 
		
		String[] allKeywords = String.join(" ", allKeywords1).toLowerCase().split(" ");
		
		List<String [][]> queries = new ArrayList<String[][]>();
		for(Set<String> set1: allKeywords1Powerset) {
			if(set1.size() <= maxKeywordsPerDisjunction) {
				for(Set<String> set2: allKeywords1Powerset) {
					if(set2.size() <= maxKeywordsPerDisjunction) {
						if(set1.size()>0 && set2.size()>0) {
							queries.add(new String[][] {String.join(" ", set1).toLowerCase().split(" "), String.join(" ", set2).toLowerCase().split(" ")});
						}
					}
				}
			}
		}
		Random queryRandom = new Random(randomQuerySelectionSeed);
		Collections.shuffle(queries, queryRandom);

		System.out.println("multiTester: Initial total Combinations evaluated: "+queries.size());
		System.out.println("multiTester: Limited Combinations evaluated: "+numberOfQueries);
		
		serverSearch(allKeywords, queries);

	}
	
	public static void serverSearch(String[] allKeywords, List<String[][]> queries) throws Exception {
		System.out.println("Server search: Initial total Combinations evaluated: "+queries.size());
		List<TokenDIS> allKeywordsToken = BIEXRR2LevDataOwner.createToken(allKeywords, true);

		final int superTokenN = 10;
		Instant start = Instant.now();
		for(int i=0; i< superTokenN; i++)
			allKeywordsToken = BIEXRR2LevDataOwner.createToken(allKeywords, false);
		Instant end = Instant.now();
		System.out.println("BIEXRR2LevDataOwner.createToken(): " + Duration.between(start, end).toNanos() + "ns in "+superTokenN+" iterations. Per iteration: "+Duration.between(start, end).toNanos()/superTokenN+"ns");
		
		int combinationIndex = -1;
		for(String[][] query: queries) {
			combinationIndex++;
			System.out.println("----------------------------------Combination---------");
			System.out.println("Search: [" + combinationIndex+ "]: "+ BIEXRR2LevVerifier.json(query)+ "\n");
	
			start = Instant.now();
			BIEXRR2LevServer server = new BIEXRR2LevServer(BIEXRR2LevDataOwner.disj);
			end = Instant.now();
			System.out.println("new BIEXRR2LevServer: " + Duration.between(start, end).toNanos() + "ns");
	
			start = Instant.now();
			BIEXRR2LevClient client = new BIEXRR2LevClient(allKeywords, allKeywordsToken, server);
			end = Instant.now();
			System.out.println("new BIEXRR2LevClient: " + Duration.between(start, end).toNanos() + "ns");
	
			start = Instant.now();
			Set<String> tmpBol = client.submitSearch(query);
			end = Instant.now();
			System.out.println("client.submitSearch(): " + Duration.between(start, end).toNanos() + "ns");
			
			tmpBol = new HashSet<String>(tmpBol);
			
//			System.out.println("Final result count: " + tmpBol.size());
			Set<String> correct = new BIEXRR2LevVerifier().submitSearch(query);
			System.out.println("Selectivity of query |"+BIEXRR2LevVerifier.json(query)+"|:"+correct.size());
			System.out.println("Verification: "+combinationIndex+", Verdict: " + (correct.equals(tmpBol) ? "OK" : "Error!! Unequal!"));
			if(!correct.equals(tmpBol)) {
				Set<String> correctDiffResult = new HashSet<String>(correct);
				correctDiffResult.removeAll(tmpBol);
				Set<String> resultDiffCorrect= new HashSet<String>(tmpBol);
				resultDiffCorrect.removeAll(correct);
				Set<String> resultIntersectionCorrect= new HashSet<String>(tmpBol);
				resultIntersectionCorrect.retainAll(correct);
				System.out.println("Correct / response:  \n" + BIEXRR2LevVerifier.json(correctDiffResult)); 
				System.out.println("Response / Correct:  \n" + BIEXRR2LevVerifier.json(resultDiffCorrect));
				System.out.println("Response ^ Correct:  \n" + BIEXRR2LevVerifier.json(resultIntersectionCorrect));
				
			}
		}
	}
	
	
	
	public static void superTokenTester(String[] allKeywords1) throws Exception {
		List<String> keptKeywords = new ArrayList<String>();
		for(String keyword: allKeywords1) {
			if(!TextExtractPar.lp1.containsKey(keyword)) {
//				System.out.println("Filtered keyword |"+keyword+"|");
			}else {
//				System.out.println("Selectivity of keyword |"+keyword+"|:"+TextExtractPar.lp1.get(keyword).size());
				keptKeywords.add(keyword);
			}
		}
		allKeywords1 = keptKeywords.toArray(new String[0]);
		
		String[] allKeywords = String.join(" ", allKeywords1).toLowerCase().split(" ");

		List<TokenDIS> allKeywordsToken = BIEXRR2LevDataOwner.createToken(allKeywords, true);
		final int N = 1;
		Instant start = Instant.now();
		for(int rep=0; rep < N; rep++) {
			allKeywordsToken = BIEXRR2LevDataOwner.createToken(allKeywords, false);
		}
		Instant end = Instant.now();
		System.out.println("superTokenCreation: keywordCount=" + allKeywords.length + ", all iterations duration=" + Duration.between(start, end).toNanos() + "ns, iterations="+N + ", per iteration="+(Duration.between(start, end).toNanos()*1.0/N) + "ns");

		
		String[][] originalTokenQuery = new String[1][];
		originalTokenQuery[0] = allKeywords;
		start = Instant.now();
		for(int rep=0; rep < N; rep++) {
			Map<String, List<TokenDIS>> result = token_BIEX(BIEXRR2LevDataOwner.listSKs, originalTokenQuery);
			try {
				Path serializedFileName = Files.createTempFile("dumpfiles", ".tmp.originaltoken");
				BIEXRR2LevDataOwner.serialiseT("originaltoken", serializedFileName, result);
				Files.delete(serializedFileName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		end = Instant.now();
		System.out.println("originalTokenCreation: keywordCount=" + allKeywords.length + ", all iterations duration=" + Duration.between(start, end).toNanos() + "ns, iterations="+N + ", per iteration="+(Duration.between(start, end).toNanos()*1.0/N) + "ns");
	}	
	
	public static void clientTokenTester(String[] allKeywords1) throws Exception {
		List<String> keptKeywords = new ArrayList<String>();
		for(String keyword: allKeywords1) {
			if(!TextExtractPar.lp1.containsKey(keyword)) {
//				System.out.println("Filtered keyword |"+keyword+"|");
			}else {
//				System.out.println("Selectivity of keyword |"+keyword+"|:"+TextExtractPar.lp1.get(keyword).size());
				keptKeywords.add(keyword);
			}
		}
		allKeywords1 = keptKeywords.toArray(new String[0]);
		
		String[] allKeywords = String.join(" ", allKeywords1).toLowerCase().split(" ");
		List<String> allKeywordsList = Arrays.asList(allKeywords);

		List<TokenDIS> allKeywordsToken = BIEXRR2LevDataOwner.createToken(allKeywords, true);
		
		BIEXRR2LevServer server = new BIEXRR2LevServer(BIEXRR2LevDataOwner.disj);
		
		BIEXRR2LevClient client = new BIEXRR2LevClient(allKeywords, allKeywordsToken, server);

		
		for(int numKeywords=1; numKeywords<= allKeywords1.length; numKeywords++) {
			
			String [][] subquery = new String[][] {allKeywordsList.subList(0, numKeywords).toArray(new String[0])};
			
			// Create client to serialise it and measure size
			client.subTokenResourcesPrepare(subquery, 1);
			Map<String, List<TokenDIS>> token = client.subToken(subquery);

			for(Map.Entry<String, List<TokenDIS>> sTokenEntry: token.entrySet()) {
				for(TokenDIS tdis: sTokenEntry.getValue()) {
					tdis.makeProper();
				}
			}
			
			try {
				Path serializedFileName = Files.createTempFile("dumpfiles", ".tmp.clientsearchtoken");
				BIEXRR2LevDataOwner.serialiseT("clientsearchtoken", serializedFileName, token);
				Files.delete(serializedFileName);
			} catch (Exception e) {
				e.printStackTrace();
			}

			final int N = 2000;
			client.subTokenResourcesPrepare(subquery, N);
			Instant start = Instant.now();
			for(int rep=0; rep < N; rep++) {
				client.subToken(subquery);
			}
			Instant end = Instant.now();
			System.out.println("clientTokenCreation: superTokenKeywordCount=" +allKeywords.length+ ", clientKeywordCount=" + numKeywords + ", all iterations duration=" + Duration.between(start, end).toNanos() + "ns, iterations="+N + ", per iteration="+(Duration.between(start, end).toNanos()*1.0/N) + "ns");

		}
	}		
	
	public static Map<String, List<TokenDIS>> token_BIEX(List<byte[]> listSK, String[][] query) throws UnsupportedEncodingException {
		
		Map<String, List<TokenDIS>> token = new HashMap<String, List<TokenDIS>>();
		

		for (int i = 1; i < query.length; i++) {
			for (int k = 0; k < query[0].length; k++) {
				List<String> searchTMP = new ArrayList<String>();
				searchTMP.add(query[0][k]);
				
				for (int r = 0; r < query[i].length; r++) {
					searchTMP.add(query[i][r]);
				}
	
				List<TokenDIS> tokenTMP = IEX2Lev.token(listSK, searchTMP);
				token.put(i+" "+k, tokenTMP);
			}
		}
		
		// Generate the IEX token
		List<String> searchBol = new ArrayList<String>();
		for (int i = 0; i < query[0].length; i++) {
			searchBol.add(query[0][i]);
		}
		List<TokenDIS> tokenGeneral = IEX2Lev.token(listSK, searchBol);
		token.put(query.length+" "+query[0].length, tokenGeneral);
		
		return token;
	}	
	
	
	public static Set<String> query_BIEX(IEX2Lev disj, Map<String, List<TokenDIS>> token) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
			NoSuchProviderException, NoSuchPaddingException, UnsupportedEncodingException, IOException {

		
		int queryLength = 0;
		int firstQueryLength = 0;

		// determining the query length
		for (String label : token.keySet()) {
			
			String[] values = label.split(" ");

			
			if (Integer.parseInt(values[0]) > queryLength) {
				queryLength = Integer.parseInt(values[0]);
			}
			
			if (Integer.parseInt(values[1]) > firstQueryLength) {
				firstQueryLength = Integer.parseInt(values[1]);
			}
			
		}
		
	

		Set<String> tmpBol = IEX2Lev.query(token.get(queryLength+" "+firstQueryLength), disj);


		for (int i = 1; i < queryLength; i++) {
			Set<String> finalResult = new HashSet<String>();
			for (int k = 0; k < firstQueryLength; k++) {
				
				List<TokenDIS> tokenTMP = token.get(i+" "+k);

				if (!(tmpBol.size() == 0)) {
					List<Integer> temp = new ArrayList<Integer>(
							disj.getDictionaryForMM().get(new String(tokenTMP.get(0).getTokenDIC())));

					if (!(temp.size() == 0)) {
						int pos = temp.get(0);

						for (int j = 0; j < tokenTMP.get(0).getTokenMMLocal().size(); j++) {

							Set<String> temporary = new HashSet<String>();
							List<String> tempoList = RR2Lev.query(tokenTMP.get(0).getTokenMMLocal().get(j),
									disj.getLocalMultiMap()[pos].getDictionary(),
									disj.getLocalMultiMap()[pos].getArray());

							if (!(tempoList == null)) {
								temporary = new HashSet<String>(
										RR2Lev.query(tokenTMP.get(0).getTokenMMLocal().get(j),
												disj.getLocalMultiMap()[pos].getDictionary(),
												disj.getLocalMultiMap()[pos].getArray()));
							}

							finalResult.addAll(temporary);

							if (tmpBol.isEmpty()) {
								break;
							}

						}
					}

				}
			}
			tmpBol.retainAll(finalResult);			

		}

//		System.out.println("Final result count: " + tmpBol.size());

		return tmpBol;
	}
	
}
