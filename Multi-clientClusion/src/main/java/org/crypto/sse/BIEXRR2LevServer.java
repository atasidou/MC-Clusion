package org.crypto.sse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BIEXRR2LevServer {
	IEX2Lev disj;
	public BIEXRR2LevServer(IEX2Lev disj) {
		this.disj = disj;
	}

	public Set<String> serverSearch(Map<String, List<TokenDIS>> token)
			throws Exception {

		int conjunctionCount = 0;
		int firstConjunctionLength = 0;

		// determining the query length
		for (String label : token.keySet()) {
			
			String[] values = label.split(" ");

			
			if (Integer.parseInt(values[0]) > conjunctionCount) {
				conjunctionCount = Integer.parseInt(values[0]);
			}
			
			if (Integer.parseInt(values[1]) > firstConjunctionLength) {
				firstConjunctionLength = Integer.parseInt(values[1]);
			}
			
		}

		String firstKeywordKey = conjunctionCount+" "+firstConjunctionLength;
		List<TokenDIS> firstKeywordToken = token.get(firstKeywordKey);
		Set<String> finalIDs = IEX2Lev.query(firstKeywordToken, disj);

		for (int currentConjunctionIndex = 1; currentConjunctionIndex < conjunctionCount; currentConjunctionIndex++) {
			Set<String> intersectionOfIDsBetweenFirstConjuctionAndCurrentConjunction = new HashSet<String>();
			for (int keywordIndexInFirstConjunction = 0; keywordIndexInFirstConjunction < firstConjunctionLength; keywordIndexInFirstConjunction++) {
				
				List<TokenDIS> tokensForKeywordInFirstConjunctionWithCurrentConjunction = token.get(currentConjunctionIndex+" "+keywordIndexInFirstConjunction);

				if (!(finalIDs.size() == 0)) {
					List<Integer> positionInDXForFirstConjunctionKeyword = new ArrayList<Integer>(
							disj.getDictionaryForMM().get(new String(tokensForKeywordInFirstConjunctionWithCurrentConjunction.get(0).getTokenDIC())));

					if (!(positionInDXForFirstConjunctionKeyword.size() == 0)) {
						// There is a DX entry for this current conjunction keyword
						int pos = positionInDXForFirstConjunctionKeyword.get(0);

						for (int currentConjunctionKeywordIndex = 0; currentConjunctionKeywordIndex < tokensForKeywordInFirstConjunctionWithCurrentConjunction.get(0).getTokenMMLocal().size(); currentConjunctionKeywordIndex++) {

							Set<String> intersectionOfIDsBetweenFirstConjuctionKeywordAndCurrentConjunctionKeyword = new HashSet<String>();
							
							List<String> intersectionOfIDsBetweenFirstConjuctionKeywordAndCurrentConjunctionKeywordList = RR2Lev.query(tokensForKeywordInFirstConjunctionWithCurrentConjunction.get(0).getTokenMMLocal().get(currentConjunctionKeywordIndex),
									disj.getLocalMultiMap()[pos].getDictionary(),
									disj.getLocalMultiMap()[pos].getArray());

							if (intersectionOfIDsBetweenFirstConjuctionKeywordAndCurrentConjunctionKeywordList != null) {
								intersectionOfIDsBetweenFirstConjuctionKeywordAndCurrentConjunctionKeyword = new HashSet<String>(intersectionOfIDsBetweenFirstConjuctionKeywordAndCurrentConjunctionKeywordList);
							} else {
							}

							intersectionOfIDsBetweenFirstConjuctionAndCurrentConjunction.addAll(intersectionOfIDsBetweenFirstConjuctionKeywordAndCurrentConjunctionKeyword);

							if (finalIDs.isEmpty()) {
								break;
							}

						}
					} else {
						// There is no DX entry for this current conjunction keyword
						throw new Exception("There is no DX entry for this current conjunction keyword");
					}

				}
			}
			finalIDs.retainAll(intersectionOfIDsBetweenFirstConjuctionAndCurrentConjunction);			
		}
	
		return finalIDs;
	}

}
