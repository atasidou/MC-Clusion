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

package org.crypto.sse;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//***********************************************************************************************//

/////////////////////    Implementation of 2Lev scheme of NDSS'14 paper by David Cash Joseph Jaeger Stanislaw Jarecki  Charanjit Jutla Hugo Krawczyk Marcel-Catalin Rosu and Michael Steiner. Finding 
//		the right parameters--- of the array size as well as the threshold to differentiate between large and small database,  to meet the same reported benchmarks is empirically set in the code
//		as it was not reported in the paper. The experimental evaluation of the  scheme is one order of magnitude slower than the numbers reported by Cash as we use Java and not C
//		Plus, some enhancements on the code itself that can be done.

///		This class can be used independently of the IEX-2Lev or IEX-ZMF if needed /////////////////////////////

//***********************************************************************************************//	

public class RR2Lev implements Serializable {

	// define the number of character that a file identifier can have
	public static int sizeOfFileIdentifer = 24;

	// instantiate the Secure Random Object
	public static SecureRandom random = new SecureRandom();

	public static int counter = 0;

	public SetMultimap<String, byte[]> dictionary = HashMultimap.create();
	public static int[] free = null;
	public static int freeNextIndex = 0;
	private static final Object freeLock = new Object();
	static byte[][] array = null;
	byte[][] arr = null;

	public RR2Lev(SetMultimap<String, byte[]> dictionary, byte[][] arr) {
		this.dictionary = dictionary;
		this.arr = arr;
	}

	public SetMultimap<String, byte[]> getDictionary() {
		return dictionary;
	}

	public void setDictionary(SetMultimap<String, byte[]> dictionary) {
		this.dictionary = dictionary;
	}

	public byte[][] getArray() {
		return arr;
	}

	public void setArray(byte[][] array) {
		this.arr = array;
	}

	private static void shuffleArray(int[] array)
	{
	    int index, temp;
	    for (int i = array.length - 1; i > 0; i--)
	    {
	        index = random.nextInt(i + 1);
	        temp = array[index];
	        array[index] = array[i];
	        array[i] = temp;
	    }
	}

	public static RR2Lev constructEMMParGMM(final byte[] key, final SetMultimap<String, String> lookup, final int bigBlock,
			final int smallBlock, final int dataSize, int maxParallelThreads) throws InterruptedException, ExecutionException, IOException {

		
		random.setSeed(CryptoPrimitives.randomSeed(16));
		
		free = new int[dataSize];
		for (int i = 0; i < dataSize; i++) {
			// initialize all buckets with random values
			free[i] = i;
		}
		shuffleArray(free);
		freeNextIndex = 0;

		List<String> listOfKeyword = new ArrayList<String>(lookup.keySet());
		int threads = Math.min(Math.min(Runtime.getRuntime().availableProcessors(), maxParallelThreads), listOfKeyword.size());

		ExecutorService service = Executors.newFixedThreadPool(threads);
		
		array = new byte[dataSize][];

		final SetMultimap<String, byte[]> dictionary = HashMultimap.create();
		ArrayList<String[]> inputs = new ArrayList<String[]>(threads);
		final int keywordCount = listOfKeyword.size();
		for (int i = 0; i < threads; i++) {
			String[] tmp;
			final int pieceSize = keywordCount / threads;
			if (i == threads - 1) {
				final int chunkSize = pieceSize + listOfKeyword.size() % threads;
				tmp = new String[chunkSize];
				for (int j = 0; j < chunkSize; j++) {
					final int chunkIndex = pieceSize * i + j;
					tmp[j] = listOfKeyword.get(chunkIndex);
				}
			} else {
				tmp = new String[pieceSize];
				for (int j = 0; j < pieceSize; j++) {
					final int chunkIndex = pieceSize * i + j;
					tmp[j] = listOfKeyword.get(chunkIndex);
				}
			}
			inputs.add(i, tmp);
		}
			

		List<Future<SetMultimap<String, byte[]>>> futures = new ArrayList<Future<SetMultimap<String, byte[]>>>();
		for (final String[] input : inputs) {
			Callable<SetMultimap<String, byte[]>> callable = new Callable<SetMultimap<String, byte[]>>() {
				public SetMultimap<String, byte[]> call() throws Exception {

					SetMultimap<String, byte[]> output = setup(key, input, lookup, bigBlock, smallBlock, dataSize);
					return output;
				}
			};
			futures.add(service.submit(callable));
		}

		service.shutdown();

		for (Future<SetMultimap<String, byte[]>> future : futures) {
			Set<String> keys = future.get().keySet();

			for (String k : keys) {
				dictionary.putAll(k, future.get().get(k));
			}

		}

		return new RR2Lev(dictionary, array);
	}
	
	private static boolean areEqualWithArrayValue(Set<byte[]> first, Set<byte[]> second) {
	    if (first.size() != second.size()) {
	    	System.out.print("Different_size");
	        return false;
	    }
	    List<byte[]> firstl = first.stream().sorted().collect(Collectors.toList());
	    List<byte[]> secondl = second.stream().sorted().collect(Collectors.toList());
	    for(int i=0;i<firstl.size();i++) {
	    	if(!Arrays.equals(firstl.get(i), secondl.get(i))) {
		    	System.out.print("Different_contents:"+firstl.get(i).length+" vs "+secondl.get(i).length);
	    		return false;
	    	}
	    }
	    return true;
	}

	// ***********************************************************************************************//

	///////////////////// Setup /////////////////////////////

	// ***********************************************************************************************//

	public static SetMultimap<String, byte[]> setup(byte[] key, String[] listOfKeyword, SetMultimap<String, String> lookup,
			int bigBlock, int smallBlock, int dataSize) throws Exception {

		SetMultimap<String, byte[]> gamma = HashMultimap.create();
		long startTime = System.nanoTime();

        byte[] iv = new byte[16];
        int percentage = (int)(listOfKeyword.length / 100.0);
        if (percentage == 0)
        	percentage = 1;
		for (String word : listOfKeyword) {
			
			counter++;

			// generate the tag
			byte[] key1 = CryptoPrimitives.generateHmac(key, 1 + word);
			byte[] key2 = CryptoPrimitives.generateHmac(key, 2 + word);
			int t = (int) Math.ceil((float) lookup.get(word).size() / bigBlock);

			if (lookup.get(word).size() <= smallBlock) {
				// pad DB(w) to "small block"
				byte[] l = CryptoPrimitives.generateHmac(key1, Integer.toString(0));
				random.nextBytes(iv);
				byte[] v =CryptoPrimitives.encryptAES_CTR_String(key2, iv,
						"1 " + lookup.get(word).toString(), smallBlock * sizeOfFileIdentifer);
				gamma.put(new String(l), v);
			}

			else {

				List<String> listArrayIndex = new ArrayList<String>();

				for (int j = 0; j < t; j++) {

					List<String> tmpList = new ArrayList<String>(lookup.get(word));

					if (j != t - 1) {
						tmpList = tmpList.subList(j * bigBlock, (j + 1) * bigBlock);
					} else {
						int sizeList = tmpList.size();

						tmpList = tmpList.subList(j * bigBlock, tmpList.size());

						for (int s = 0; s < ((j + 1) * bigBlock - sizeList); s++) {
							tmpList.add("XX");
						}

					}

					int tmpPos;
					while(true) {
						synchronized (freeLock) {
							tmpPos = free[freeNextIndex];
							if(array[tmpPos] == null) {
								listArrayIndex.add(tmpPos + "");
								freeNextIndex++;
								break;
							}else {
								System.out.print("word:" + word+ ": >smallBlock, t:" + t + ", j:"+j);
								System.out.print(", freeNextIndex:" + freeNextIndex);
								System.out.print(", tmpPos:" + tmpPos);
								System.out.println(", free.size: "+ free.length);
								System.out.println("Smallcase => array[tmpPos] is already allocated. Error");
								throw new Exception("Smallcase: array[tmpPos] is already allocated. Error");
							}
						}
					}
	
					random.nextBytes(iv);
					array[tmpPos] = CryptoPrimitives.encryptAES_CTR_String(key2, iv,
							tmpList.toString(), bigBlock * sizeOfFileIdentifer);
					listArrayIndex.add(tmpPos + "");
				}
				// medium case
				if (t <= smallBlock) {
					byte[] l = CryptoPrimitives.generateHmac(key1, Integer.toString(0));
					random.nextBytes(iv);
					byte[] v = CryptoPrimitives.encryptAES_CTR_String(key2, iv,
									"2 " + listArrayIndex.toString(), smallBlock * sizeOfFileIdentifer);
					gamma.put(new String(l),v);
				}
				// big case
				else {
					int tPrime = (int) Math.ceil((float) t / bigBlock);

					List<String> listArrayIndexTwo = new ArrayList<String>();
					int tmpPos;

					for (int l = 0; l < tPrime; l++) {
						List<String> tmpListTwo = new ArrayList<String>(listArrayIndex);

						if (l != tPrime - 1) {
							tmpListTwo = tmpListTwo.subList(l * bigBlock, (l + 1) * bigBlock);
						} else {

							int sizeList = tmpListTwo.size();

							tmpListTwo = tmpListTwo.subList(l * bigBlock, tmpListTwo.size());
							for (int s = 0; s < ((l + 1) * bigBlock - sizeList); s++) {
								tmpListTwo.add("XX");
							}
						}

						synchronized (freeLock) {
							tmpPos = free[freeNextIndex];
							
							if(array[tmpPos] == null) {
								listArrayIndexTwo.add(tmpPos + "");
								freeNextIndex++;
							}else {
								System.out.print("word:" + word+ ": >smallBlock, t:" + t + ", l:"+l);
								System.out.print(", freeNextIndex:" + freeNextIndex);
								System.out.print(", tmpPos:" + tmpPos);
								System.out.println(", free.size: "+ free.length);
								System.out.println("Big case => array[tmpPos] is already allocated. Try again.");
								throw new Exception("Bigcase: array[tmpPos] is already allocated. Error");
							}
						}

						random.nextBytes(iv);

						array[tmpPos] = CryptoPrimitives.encryptAES_CTR_String(key2, iv,
								tmpListTwo.toString(), bigBlock * sizeOfFileIdentifer);
					}

					byte[] l = CryptoPrimitives.generateHmac(key1, Integer.toString(0));
					random.nextBytes(iv);
					byte[] v = CryptoPrimitives.encryptAES_CTR_String(key2, iv,
							"3 " + listArrayIndexTwo.toString(), smallBlock * sizeOfFileIdentifer);
					gamma.put(new String(l),v);	
				}

			}

		}
		long endTime = System.nanoTime();
		long totalTime = endTime - startTime;
		return gamma;
	}

	// ***********************************************************************************************//

	///////////////////// Search Token generation /////////////////////
	///////////////////// /////////////////////////////

	// ***********************************************************************************************//

	public static byte[][] token(byte[] key, String word) throws UnsupportedEncodingException {

		byte[][] keys = new byte[2][];
		keys[0] = CryptoPrimitives.generateHmac(key, 1 + word);
		keys[1] = CryptoPrimitives.generateHmac(key, 2 + word);

		return keys;
	}

	// ***********************************************************************************************//

	///////////////////// Query Alg /////////////////////////////

	// ***********************************************************************************************//

	public static List<String> query(byte[][] keys, SetMultimap<String, byte[]> dictionary, byte[][] array)
			throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
			NoSuchProviderException, NoSuchPaddingException, IOException {

		byte[] l = CryptoPrimitives.generateHmac(keys[0], Integer.toString(0));

		List<byte[]> tempList = new ArrayList<byte[]>(dictionary.get(new String(l)));

		if (!(tempList.size() == 0)) {
			String temp = (new String(CryptoPrimitives.decryptAES_CTR_String(tempList.get(0), keys[1])))
					.split("\t\t\t")[0];
			temp = temp.replaceAll("\\s", "");
			temp = temp.replace('[', ',');
			temp = temp.replace("]", "");

			String[] result = temp.split(",");

			List<String> resultFinal = new ArrayList<String>(Arrays.asList(result));
			// We remove the flag that identifies the size of the dataset

			if (result[0].equals("1")) {

				resultFinal.remove(0);
				return resultFinal;
			}

			else if (result[0].equals("2")) {
				resultFinal.remove(0);

				List<String> resultFinal2 = new ArrayList<String>();

				for (String key : resultFinal) {

					boolean flag = true;
					int counter = 0;
					while (flag) {

						if (counter < key.length() && Character.isDigit(key.charAt(counter))) {

							counter++;
						}

						else {
							flag = false;
						}
					}

					String temp2 = "";
					int arrayIndex = Integer.parseInt((String) key.subSequence(0, counter));
					byte[] bs = array[arrayIndex];
					if (!(bs == null)) {
						String string = new String(CryptoPrimitives.decryptAES_CTR_String(
								bs, keys[1]));
						temp2 = string.split("\t\t\t")[0];
					}
					temp2 = temp2.replaceAll("\\s", "");

					temp2 = temp2.replaceAll(",XX", "");

					temp2 = temp2.replace("[", "");
					temp2 = temp2.replace("]", "");

					String[] result3 = temp2.split(",");

					List<String> tmp = new ArrayList<String>(Arrays.asList(result3));

					resultFinal2.addAll(tmp);
				}

				return resultFinal2;
			}

			else if (result[0].equals("3")) {
				resultFinal.remove(0);
				List<String> resultFinal2 = new ArrayList<String>();
				for (String key : resultFinal) {

					boolean flag = true;
					int counter = 0;
					while (flag) {

						if (counter < key.length() && Character.isDigit(key.charAt(counter))) {

							counter++;
						}

						else {
							flag = false;
						}
					}
					String temp2 = (new String(CryptoPrimitives.decryptAES_CTR_String(
							array[Integer.parseInt((String) key.subSequence(0, counter))], keys[1])))
									.split("\t\t\t")[0];
					temp2 = temp2.replaceAll("\\s", "");

					temp2 = temp2.replaceAll(",XX", "");
					temp2 = temp2.replace("[", "");
					temp2 = temp2.replace("]", "");

					String[] result3 = temp2.split(",");
					List<String> tmp = new ArrayList<String>(Arrays.asList(result3));
					resultFinal2.addAll(tmp);
				}
				List<String> resultFinal3 = new ArrayList<String>();

				for (String key : resultFinal2) {

					boolean flag = true;
					int counter = 0;
					while (flag) {

						if (counter < key.length() && Character.isDigit(key.charAt(counter))) {

							counter++;
						}

						else {
							flag = false;
						}
					}
					String temp2 = (new String(CryptoPrimitives.decryptAES_CTR_String(
							array[Integer.parseInt((String) key.subSequence(0, counter))], keys[1])))
									.split("\t\t\t")[0];
					temp2 = temp2.replaceAll("\\s", "");
					temp2 = temp2.replaceAll(",XX", "");

					temp2 = temp2.replace("[", "");
					temp2 = temp2.replace("]", "");
					String[] result3 = temp2.split(",");

					List<String> tmp = new ArrayList<String>(Arrays.asList(result3));

					resultFinal3.addAll(tmp);
				}

				return resultFinal3;
			}
		}
		return null;
	}
}
