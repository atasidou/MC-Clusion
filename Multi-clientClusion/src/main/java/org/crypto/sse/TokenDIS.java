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

/////////////////////    Generation of the token of IEX-ZMF 
//***********************************************************************************************//	

package org.crypto.sse;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenDIS implements Serializable {

	public byte[][] tokenMMGlobal;
	private String firstKeyword;
	public String getFirstKeyword() {
		return firstKeyword;
	}

	private List<String> subSearch;
	public byte[] tokenDIC;
	public List<byte[][]> tokenMMLocal = new ArrayList<byte[][]>();
	private Map<String, byte[][]> mm = new HashMap<String, byte[][]>();
	public Map<String, byte[][]> getMm() {
		return mm;
	}

	private List<String> tokenMMLocalStr = new ArrayList<String>();

	public TokenDIS(List<String> subSearch, List<byte[]> listOfkeys) throws UnsupportedEncodingException {
		this.subSearch = subSearch;
		this.firstKeyword = subSearch.get(0);
		this.tokenMMGlobal = RR2Lev.token(listOfkeys.get(0), subSearch.get(0));
		this.tokenDIC = CryptoPrimitives.generateHmac(listOfkeys.get(1), 3 + subSearch.get(0));

		for (int i = 1; i < subSearch.size(); i++) {
			byte[][] mmToken1 = RR2Lev.token(CryptoPrimitives.generateHmac(listOfkeys.get(0), subSearch.get(0)), subSearch.get(i));
			byte[][] mmToken2 = RR2Lev.token(CryptoPrimitives.generateHmac(listOfkeys.get(0), subSearch.get(i)), subSearch.get(0));
			this.mm.put(subSearch.get(0)+"^"+subSearch.get(i), mmToken1);
			this.mm.put(subSearch.get(i)+"^"+subSearch.get(0), mmToken2);
			ResolverDB.getDb().put(mmToken1, "MM/"+subSearch.get(0)+"^"+subSearch.get(i));
			tokenMMLocal.add(
					mmToken1);
		}
	}

	public byte[][] getTokenMMGlobal() {
		return tokenMMGlobal;
	}

	public void setTokenMMGlobal(byte[][] tokenMMGlobal) {
		this.tokenMMGlobal = tokenMMGlobal;
	}

	public byte[] getTokenDIC() {
		return tokenDIC;
	}

	public void setTokenDIC(byte[] tokenDIC) {
		this.tokenDIC = tokenDIC;
	}

	public List<byte[][]> getTokenMMLocal() {
		return tokenMMLocal;
	}

	public void setTokenMMLocal(List<byte[][]> tokenMMLocal) {
		this.tokenMMLocal = tokenMMLocal;
	}
	
	public TokenDIS decodeLocalMMStr() {
		tokenMMLocalStr = new ArrayList<String>();
		for(byte[][] mmKey: tokenMMLocal)
			tokenMMLocalStr.add(ResolverDB.getDb().get(mmKey));
		return this;
	}

	public void makeProper() {
		firstKeyword = "";
		subSearch = new ArrayList<String>();
		mm = new HashMap<String, byte[][]>();
		tokenMMLocalStr = new ArrayList<String>();
	}

}
