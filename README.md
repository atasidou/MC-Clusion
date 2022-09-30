# MC-Clusion 

MC-Clusion is a multi-client extension of the [Clusion](https://github.com/encryptedsystems/Clusion) searchable encryption library, created based on the relevant paper \[[KM17][KM17]\].

MC-Clusion, like Clusion, is provided as-is under the *GNU General Public License v3 (GPLv3)*. 

## Quick instructions

+ Install Java (OpenJDK version 17 tested, but should work with both newer and older versions (>=11))
+ Install Maven
+ Download/Git clone MC-Clusion
+ Run below commands to build the jar

```bash
cd MC-Clusion/Multi-clientClusion
mvn clean install
```
	

## Quick Test

For a quick test:

##### 1. Generate a test dataset by running the `org.crypto.sse.TestDataGen` class with the appropriate parameters.
For example, run:

```bash
java -cp target/Clusion-1.0-CUSTOM-jar-with-dependencies.jar:target/test-classes org.crypto.sse.TestDataGen 1000 1
```
This will create 1000 documents with the RNG seed of 1 and store them in the subdirectory of the working directory named `gen-data-1000-1`.

##### 2. Create a JSON file which will contain the queries to perform; see the example in `example/queries.json`.
 
##### 3. Run the server search class `org.crypto.sse.TestBIEX` example with the previously created queries and dataset.
For example, run:

```bash
java -cp lib/bcprov-jdk15on-1.54.jar:target/Clusion-1.0-CUSTOM-jar-with-dependencies.jar:target/test-classes org.crypto.sse.TestBIEX gen-data-1000-1 100 1 50 1 example/queries.json
```

This will use the dataset in `gen-data-1000-1` and the queries in `example/queries.json`.
The rest of the parameters are explained in the source code. 


## References

- \[[KM17](https://eprint.iacr.org/2017/126.pdf)\]: :  *Boolean Searchable Symmetric Encryption with Worst-Case Sub-Linear Complexity* by S. Kamara and T. Moataz. 

[KM17]: https://eprint.iacr.org/2017/126.pdf
