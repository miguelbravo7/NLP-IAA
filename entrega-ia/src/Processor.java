
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Processor {
	String filename;

	HashMap<String, Double> hashmaptroll = new HashMap<String, Double>();
	HashMap<String, Double> hashmapnotroll = new HashMap<String, Double>();
	HashMap<String, Double> hashmaptotal = new HashMap<String, Double>();

	long acc_troll = 0;
	long acc_notroll = 0;
	long frases_troll = 0;
	long frases_notroll = 0;

	FileWriter troll = null;
	FileWriter notroll = null;
	FileWriter dict = null;
	FileWriter corpus = null;
	FileWriter aprendizajeT = null;
	FileWriter aprendizajeNT = null;

	public static void main(String[] args) {
		Processor algo = new Processor("src/CTR_TRAIN.txt");
		algo.evalDB("src/CTR_TRAIN.txt");
	}

	public Processor(String filename) {
		this.filename = filename;
		initDB();
	}

	/**
	 * Inicializa la base de conocimiento segun elarchivo especificado
	 */
	public void initDB() {
		BufferedReader reader = null;
		System.out.println("Database: " + filename);

		try {
			reader = new BufferedReader(new FileReader(new File(filename)));
			troll = new FileWriter("troll.txt");
			notroll = new FileWriter("notroll.txt");
			dict = new FileWriter("dict.txt");
			corpus = new FileWriter("corpustodo.txt");
			aprendizajeT = new FileWriter("aprendizajeT.txt");
			aprendizajeNT = new FileWriter("aprendizajeNT.txt");
			String text = null;

			//Popula los diccionarios troll y notroll 
			while ((text = reader.readLine()) != null) {
				if (text.isEmpty()) {
					continue;
				}

				String[] valores = text.split("\"\\s*,\\s*");
				if (valores.length == 1)
					continue;
				valores[0] = refactorPhrase(valores[0]);
				// System.out.println(valores[1]);

				// escribe en el fichero troll o notroll y aumenta el contador de frases
				if (valores[1].trim().equalsIgnoreCase("troll")) {
					// System.out.println(valores[1] + " : troll");
					frases_troll++;
					troll.write(valores[0] + "\n");
					for (String linea : valores[0].split("\\s+")) {
						linea = refactorWord(linea);
						if (excludingConditions(linea)) {
							continue;
						}
						addToHash(hashmaptroll, linea);
					}
				} else {
					// System.out.println(valores[1] + " : not_troll");
					frases_notroll++;
					notroll.write(valores[0] + "\n");
					for (String linea : valores[0].split("\\s+")) {
						linea = refactorWord(linea);
						if (excludingConditions(linea)) {
							continue;
						}
						addToHash(hashmapnotroll, linea);
					}
				}
				corpus.write(valores[0] + "\n");
			}

			// merge hash maps and get the acumulate tokens
			hashmaptotal.putAll((Map<? extends String, ? extends Double>) hashmaptroll.clone());
			hashmapnotroll.forEach((key, value) -> {
				if (hashmaptotal.containsKey(key)) {
					hashmaptotal.put(key, hashmaptotal.get(key) + value);
					acc_notroll += value;
				} else {
					hashmaptotal.put(key, value);
					acc_notroll += value;
				}
			});

			System.out.println("Bocab. troll size: " + hashmaptroll.size());
			System.out.println("Bocab. notroll size: " + hashmapnotroll.size());
			System.out.println("Bocab. total size: " + hashmaptotal.size());

			hashmaptroll.forEach((key, value) -> {
				acc_troll += value;
			});

			// itera total hashmap hacia el dicionario
			Iterator<?> it = ((HashMap<String, Double>) hashmaptotal.clone()).entrySet().iterator();
			dict.append("Numero de palabras total:" + hashmaptotal.size() + "\n");

			while (it.hasNext()) {
				Map.Entry<String, Double> pair = (Map.Entry<String, Double>) it.next();
				dict.write("Numero de palabras:" + pair.getValue() + "\n");
				dict.write("Palabra:" + pair.getKey() + "\n");
				it.remove(); // evita ConcurrentModificationException
			}

			writeFile(aprendizajeT, hashmaptroll, frases_troll, acc_troll);

			writeFile(aprendizajeNT, hashmapnotroll, frases_notroll, acc_notroll);

		} catch (FileNotFoundException e) {
			System.out.println(System.getProperty("user.dir"));
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				troll.close();
				notroll.close();
				dict.close();
				corpus.close();
				System.out.println("Done.\n");
			} catch (IOException e) {
			}
		}
	}

	public void evalDB(String filename) {
		System.out.println("Testbase: " + filename);
		BufferedReader reader = null;
		long trollT=0;
		long trollF=0;
		long notrollT=0;
		long notrollF=0;

		try {
			reader = new BufferedReader(new FileReader(new File(filename)));
			String text = null;

			//lee cada linea, evalua su prediccion y comprueba la suposicion
			while ((text = reader.readLine()) != null) {
				if (text.isEmpty()) {
					continue;
				}

				String[] valores = text.split("\"\\s*,\\s*");
				if (valores.length == 1)
					continue;
				valores[0] = refactorPhrase(valores[0]);
				// System.out.println(valores[1]);
				
				if(calcPropPhrase(hashmaptroll, acc_troll, valores[0]) < calcPropPhrase(hashmapnotroll, acc_notroll, valores[0])) {
				//if(calcPropPhrase(hashmaptroll, acc_troll, hashmapnotroll, acc_notroll, valores[0]) <= 0) {
					if("not_troll".equalsIgnoreCase(valores[1])){
						notrollT++;
					}else {
						notrollF++;
					}
				}else {
					if("troll".equalsIgnoreCase(valores[1])){
						trollT++;
					}else {
						trollF++;
					}
				}
			}
			
			System.out.println("troll\tnotroll");
			System.out.println(trollT+"\t"+trollF+"\ttroll");
			System.out.println(notrollF+"\t"+notrollT+"\tnotroll");
			System.out.println();
			System.out.println("troll: " +((double)trollT/(trollT+trollF)*100) + "%");
			System.out.println("notroll: " +((double)notrollT/(notrollT+notrollF)*100) + "%");
			System.out.println("Total: " +((double)(notrollT+trollT)/(notrollT+notrollF+trollT+trollF)*100) + "%");
			
		} catch (FileNotFoundException e) {
			System.out.println(System.getProperty("user.dir"));
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				System.out.println("Done.");
			} catch (IOException e) {
			}
		}
	}

	private int calcPropPhrase(HashMap<String, Double> map, long acumulado, HashMap<String, Double> map2, long acumulado2, String phrase) {
		int acc = 0;
		for (String word : phrase.split("\\s+")) {
			word = refactorWord(word);
			acc += calcProbWord(map, acumulado, word) > calcProbWord(map2, acumulado2, word) ? 1 : -1;
		}
		return acc;
	}

	private Double calcPropPhrase(HashMap<String, Double> map, long acumulado, String phrase) {
		Double acc = 0d;
		for (String word : phrase.split("\\s+")) {
			word = refactorWord(word);
			acc += calcProbWord(map, acumulado, word);
		}
		return acc;
	}

	private Double calcProbWord(HashMap<String, Double> map, long acumulado, String word) {
		return Math.log(((map.get(word) == null ? 1 : map.get(word)) + 1) / (acumulado + map.size() + 1));
	}

	private boolean excludingConditions(String word) {
		boolean out = false;
		if (word.startsWith("@")
				//|| word.length() <= 1
				|| word.startsWith("http")) {
			//System.out.println(word);
			out = true;
		}
		return out;
	}
	
	private String refactorPhrase(String phrase) {
		return phrase.substring(1, phrase.length());//.toLowerCase()
	}
	
	private String refactorWord(String word) {
		return word.replaceAll("[!?\\.()]|&lt;|&gt;|&quot;|&#\\d+;|\\b\\d+\\b", "")
				//.replaceAll("&amp;", "")
				.replaceAll("&apos;", "'");
	}

	private void writeFile(FileWriter file, HashMap<String, Double> values, long frases, long acumulado) {
		// iterate through total hashmap onto file
		Iterator<?> it = ((HashMap<String, Double>) values.clone()).entrySet().iterator();
		try {
			file.append("Numero de documentos del corpus :" + frases + "\n" + "N�mero de palabras del corpus:"
					+ acumulado + "\n");

			while (it.hasNext()) {
				Map.Entry<String, Double> pair = (Map.Entry<String, Double>) it.next();
				file.write("Palabra:" + pair.getKey() + "\tFrec:" + pair.getValue() + "\tLogProb:"
						+ Math.log((pair.getValue() + 1) / (acc_notroll + hashmaptotal.size() + 1)) + "\n");
				// System.out.println((pair.getValue()+1) / (acumulado+hashmaptotal.size()+1)+"
				// = "+(pair.getValue()+1)+"/"+acumulado+"+"+hashmaptotal.size()+1);
				it.remove(); // avoids a ConcurrentModificationException
			}
		} catch (IOException e) {
			System.out.println("Error en la escritura al fichero");
			e.printStackTrace();
		}
	}

	private void addToHash(HashMap<String, Double> map, String value) {
		if (!map.containsKey(value)) {
			map.put(value, 1d);
		} else {
			map.put(value, map.get(value) + 1);
		}
	}
}