package org.crypto.sse;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.SetMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class BIEXRR2LevVerifier {
	public static String json(Object o) {
	    GsonBuilder gsonBuilder = new GsonBuilder();
	    Gson gson = gsonBuilder.create();
	    String json= gson.toJson(o);
	    return json;
	}

	public Set<String> submitSearch(String[][] subquery){
		SetMultimap<String, String> lp1 = TextExtractPar.lp1;
		Set<String> finalOut = null;
		for(String[] disjunction: subquery) {
			Set<String> inter = new HashSet<String>();
			for(String disjunctionTerm: disjunction) {
				inter.addAll(lp1.get(disjunctionTerm));
			}
			if(finalOut == null) {
				finalOut = new HashSet<String>(inter);
			} else {
				finalOut.retainAll(inter);
			}
		}
		return finalOut;
	}
}
