/** * Copyright (C) 2016 Tarik Moataz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//***********************************************************************************************//

// This file contains IEX-2Lev implementation. KeyGen, Setup, Token and Query algorithms. 
// We also propose an implementation of a possible filtering mechanism that reduces the storage overhead. 

//***********************************************************************************************//	

package org.crypto.sse;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class IEX2Lev implements Serializable {

	// Parameter of Disjunctive search
	public static int maxDocumentIDs = 0;

	// Change it based on data distribution and storage restrictions
	static double filterParameter = 0.0; //0.2 = 20% filter parameter;

	public static long numberPairs = 0;
	RR2Lev globalMM = null;
	RR2Lev[] localMultiMap = null;
	SetMultimap<String, Integer> dictionaryForMM = null;

	public IEX2Lev(RR2Lev globalMM, RR2Lev[] localMultiMap, SetMultimap<String, Integer> dictionaryForMM) {
		this.globalMM = globalMM;
		this.localMultiMap = localMultiMap;
		this.dictionaryForMM = dictionaryForMM;
	}

	public RR2Lev getGlobalMM() {
		return globalMM;
	}

	public void setGlobalMM(RR2Lev globalMM) {
		this.globalMM = globalMM;
	}

	public RR2Lev[] getLocalMultiMap() {
		return localMultiMap;
	}

	public void setLocalMultiMap(RR2Lev[] localMultiMap) {
		this.localMultiMap = localMultiMap;
	}

	public SetMultimap<String, Integer> getDictionaryForMM() {
		return dictionaryForMM;
	}

	public void setDictionaryForMM(SetMultimap<String, Integer> dictionaryForMM) {
		this.dictionaryForMM = dictionaryForMM;
	}

	// ***********************************************************************************************//

	///////////////////// Setup /////////////////////////////

	// ***********************************************************************************************//

	public static IEX2Lev setup(List<byte[]> keys, SetMultimap<String, String> lookup, SetMultimap<String, String> lookup2,
			int bigBlock, int smallBlock, int dataSize, int maxParallelThreads) throws InterruptedException, ExecutionException, IOException {

		// Instantiation of the object that contains Global MM, Local MMs and
		// the dictionary
		RR2Lev[] localMultiMap = new RR2Lev[lookup.keySet().size()];
		SetMultimap<String, Integer> dictionaryForMM = HashMultimap.create();

		Printer.debugln("Number of (w, id) pairs " + lookup.size());

		Printer.debugln("Number of keywords " + lookup.keySet().size());

		Printer.debugln("\n *********************Stats******* \n");
		Printer.debugln("\n Number of (w, id) pairs " + lookup2.size());
		Printer.debugln("\n Number of keywords " + lookup.keySet().size());

		int counter = 0;

		///////////////////// Computing Filtering Factor and exact needed data
		///////////////////// size/////////////////////////////
		Instant start = null; 
		Instant end = null; 
		start = Instant.now();

		HashMap<Integer, Integer> histogram = new HashMap<Integer, Integer>();
		Printer.debugln("Number of documents " + lookup2.keySet().size());
		for (String keyword : lookup.keySet()) {
			if (histogram.get(lookup.get(keyword).size()) != null) {
				int tmp = histogram.get(lookup.get(keyword).size());
				histogram.put(lookup.get(keyword).size(), tmp + 1);
			} else {
				histogram.put(lookup.get(keyword).size(), 1);
			}

			if (dataSize < lookup.get(keyword).size()) {
				dataSize = lookup.get(keyword).size();
			}

		}
		System.out.println("IEX2Lev.setup()/getExactData: " + Duration.between(start, Instant.now()).toNanos() + "ns");
		System.out.println("IEX2Lev.setup: smallBlock: "+ smallBlock + ", dataSize: "+dataSize);

		// Construction of the global multi-map
		Printer.debugln("\nBeginning of Global MM creation \n");

		start = Instant.now();

		IEX2Lev disj2 = new IEX2Lev(RR2Lev.constructEMMParGMM(keys.get(0), lookup, bigBlock, smallBlock, dataSize, maxParallelThreads),
				localMultiMap, dictionaryForMM);
		end = Instant.now();
		System.out.println("IEX2Lev.setup()/MMglobal/duration: " + Duration.between(start, end).toNanos() + "ns");
		long duration = Duration.between(start, end).toNanos();

		System.out.println("IEX2Lev.setup()/MMglobal/Number of DB pairs: " + lookup2.size());
		System.out.println("IEX2Lev.setup()/MMglobal/duration//DBpair: " + (1.0f*duration) / lookup2.size()+"ns");

		numberPairs = numberPairs + lookup.size();

		// Construction of the local multi-map

		Printer.debugln("Start of Local Multi-Map construction");

		start = Instant.now();
		int keyCount = 0;
		int keyTotal = lookup.keySet().size();
		for (String keyword : lookup.keySet()) {
			keyCount++;
			System.out.println("LMM for keyword: "+ keyword + ", " + keyCount+ "/"+keyTotal);


			// Filter setting optional. For a setup without any filtering set
			// filterParameter to 0
			if (((double) lookup.get(keyword).size() / TextExtractPar.maxTupleSize > filterParameter)) {
				// First computing V_w. Determine Doc identifiers

				Set<String> VW = new HashSet<String>();
				for (String idDoc : lookup.get(keyword)) {
					VW.addAll(lookup2.get(idDoc));
				}

				SetMultimap<String, String> secondaryLookup = HashMultimap.create();

				// here we are only interested in documents in the intersection
				// between "keyword" and "word"
				for (String word : VW) {
					// Filter setting optional. For a setup without any
					// filtering set filterParameter to 0
					if (((double) lookup.get(word).size() / TextExtractPar.maxTupleSize > filterParameter)) {
						Set<String> l1 = new HashSet<String>(lookup.get(word));
						Set<String> l2 = lookup.get(keyword);
						l1.retainAll(l2);
						secondaryLookup.putAll(word, l1);
					}
				}

				// End of VW construction
				RR2Lev.counter = 0;
				disj2.getLocalMultiMap()[counter] = RR2Lev.constructEMMParGMM(
						CryptoPrimitives.generateHmac(keys.get(0), keyword), secondaryLookup, bigBlock, smallBlock,
						dataSize, maxParallelThreads);
				byte[] key3 = CryptoPrimitives.generateHmac(keys.get(1), 3 + keyword);
				numberPairs = numberPairs + secondaryLookup.size();
				dictionaryForMM.put(new String(key3), counter);

			}else {
			}
			counter++;

		}

		end = Instant.now();
		System.out.println("BIEXRR2LevDataOwner.setup()/LMMS/duration: " + Duration.between(start, end).toNanos() + "ns");
		disj2.setDictionaryForMM(dictionaryForMM);
		return disj2;

	}

	// ***********************************************************************************************//

	///////////////////// Search Token Generation /////////////////////////////

	// ***********************************************************************************************//

	public static List<TokenDIS> token(List<byte[]> listOfkeys, List<String> search)
			throws UnsupportedEncodingException {
		List<TokenDIS> token = new ArrayList<TokenDIS>();

		for (int i = 0; i < search.size(); i++) {

			List<String> subSearch = new ArrayList<String>();
			// Create a temporary list that carry keywords in *order*
			for (int j = i; j < search.size(); j++) {
				subSearch.add(search.get(j));
			}

			token.add(new TokenDIS(subSearch, listOfkeys).decodeLocalMMStr());
		}
		return token;

	}

	// ***********************************************************************************************//

	///////////////////// Search Token Generation /////////////////////////////

	// ***********************************************************************************************//

	public static List<TokenDIS> tokenWithSelf(List<byte[]> listOfkeys, List<String> search)
			throws UnsupportedEncodingException {
		List<TokenDIS> token = new ArrayList<TokenDIS>();

		for (int i = 0; i < search.size(); i++) {

			List<String> subSearch = new ArrayList<String>();
			// Create a temporary list that carry keywords in *order*
			subSearch.add(search.get(i));// add self
			for (int j = 0; j < search.size(); j++) {
				subSearch.add(search.get(j));
			}

			token.add(new TokenDIS(subSearch, listOfkeys).decodeLocalMMStr());
		}
		return token;

	}
	// ***********************************************************************************************//

	///////////////////// Query /////////////////////////////

	// ***********************************************************************************************//

	public static Set<String> query(List<TokenDIS> token, IEX2Lev disj)
			throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
			NoSuchProviderException, NoSuchPaddingException, IOException {

		Set<String> finalResult = new TreeSet<String>();
		for (int i = 0; i < token.size(); i++) {

			byte[][] tokenMMGlobal = token.get(i).getTokenMMGlobal();
			SetMultimap<String, byte[]> dictionary = disj.getGlobalMM().getDictionary();
			byte[][] globalMMArray = disj.getGlobalMM().getArray();
			List<String> queryResultList = RR2Lev.query(tokenMMGlobal,
					dictionary, globalMMArray);
			Set<String> result = new HashSet<String>(queryResultList);

			if (!(result.size() == 0)) {
				List<Integer> temp = new ArrayList<Integer>(
						disj.getDictionaryForMM().get(new String(token.get(i).getTokenDIC())));

				if (!(temp.size() == 0)) {
					int pos = temp.get(0);

					for (int j = 0; j < token.get(i).getTokenMMLocal().size(); j++) {

						Set<String> temporary = new HashSet<String>();
						List<String> tempoList = RR2Lev.query(token.get(i).getTokenMMLocal().get(j),
								disj.getLocalMultiMap()[pos].getDictionary(), disj.getLocalMultiMap()[pos].getArray());

						if (!(tempoList == null)) {
							temporary = new HashSet<String>(RR2Lev.query(token.get(i).getTokenMMLocal().get(j),
									disj.getLocalMultiMap()[pos].getDictionary(),
									disj.getLocalMultiMap()[pos].getArray()));
						}

						result = Sets.difference(result, temporary);
						if (result.isEmpty()) {
							break;
						}

					}
				}
				finalResult.addAll(result);
			}
		}
		return finalResult;
	}
}
