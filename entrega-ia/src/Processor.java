
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
	FileWriter troll = null;
	FileWriter notroll = null;
	FileWriter dict = null;
	FileWriter corpus = null;
	FileWriter aprendizajeT = null;
	FileWriter aprendizajeNT = null;
	String filename;
	HashMap<String, Integer> hashmaptroll = new HashMap<String, Integer>();
	HashMap<String, Integer> hashmapnotroll = new HashMap<String, Integer>();
	HashMap<String, Integer> hashmaptotal = new HashMap<String, Integer>();

	public static void main(String[] args) {
		Processor algo = new Processor("entrega-ia/src/CTR_TRAIN.txt");
		algo.ficheroToCSV();
	}

	public Processor(String filename) {
		this.filename = filename;
	}

	public void ficheroToCSV() {
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(new File(filename)));
			System.out.println(filename.replaceAll("\\..+", "") + ".txt");
			troll = new FileWriter("troll.txt");
			notroll = new FileWriter("notroll.txt");
			dict = new FileWriter("dict.txt");
			corpus = new FileWriter("corpustodo.txt");
			aprendizajeT = new FileWriter("aprendizajeT.txt");
			aprendizajeNT = new FileWriter("aprendizajeNT.txt");
			String text = null;

			while ((text = reader.readLine()) != null) {
				if (text.isEmpty()) {
					continue;
				}

				String[] valores = text.split(",");
				valores[0] = valores[0].replaceAll("\"", "").trim();
				// System.out.println(valores[1]);

				// escribe en el fichero troll o notroll
				if (valores[1].trim().equalsIgnoreCase("troll")) {
					// System.out.println(valores[1] + " : troll");
					troll.write(valores[0] + "\n");
					// contar las palabras
					for (String linea : valores[0].split("\\s+")) {
						if (!hashmaptroll.containsKey(linea)) {
							hashmaptroll.put(linea, 1);
						} else {
							hashmaptroll.put(linea, hashmaptroll.get(linea) + 1);
						}
					}
				} else {
					// System.out.println(valores[1] + " : not_troll");
					notroll.write(valores[0] + "\n");
					for (String linea : valores[0].split("\\s+")) {
						if (!hashmapnotroll.containsKey(linea)) {
							hashmapnotroll.put(linea, 1);
						} else {
							hashmapnotroll.put(linea, hashmapnotroll.get(linea) + 1);
						}
					}
				}
				corpus.write(valores[0] + "\n");
			}

			// merge hash maps
			hashmaptotal.putAll((Map<? extends String, ? extends Integer>) hashmaptroll.clone());
			hashmapnotroll.forEach((key, value) -> {
				if (hashmaptotal.containsKey(key)) {
					hashmaptotal.put(key, hashmaptotal.get(key) + value);
				} else {
					hashmaptotal.put(key, value);
				}
			});

			// iterate through total hashmap onto dictionary
			Iterator<?> it = ((HashMap<String, Integer>) hashmaptotal.clone()).entrySet().iterator();
			dict.append("Numero de palabras total:" + hashmaptotal.size() + "\n");

			while (it.hasNext()) {
				Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>) it.next();
				dict.write("Numero de palabras:" + pair.getValue() + "\n");
				dict.write("Palabra:" + pair.getKey() + "\n");
				it.remove(); // avoids a ConcurrentModificationException
			}

			// iterate through troll hashmap onto troll learn model
			it = hashmaptroll.entrySet().iterator();
			aprendizajeT.append("Numero de documentos del corpus :<número entero>\n" + "Número de palabras del corpus:"
					+ hashmaptroll.size() + "\n");

			while (it.hasNext()) {
				Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>) it.next();
				aprendizajeT.write("Palabra:" + pair.getKey() + " Frec:" + pair.getValue() + " LogProb:"
						+ Math.log(pair.getValue() / hashmaptotal.get(pair.getKey())) + "\n");
				it.remove(); // avoids a ConcurrentModificationException
			}

			// iterate through no troll hashmap onto no troll learn model
			it = hashmapnotroll.entrySet().iterator();
			aprendizajeNT.append("Numero de documentos del corpus :<número entero>\n" + "Número de palabras del corpus:"
					+ hashmapnotroll.size() + "\n");

			while (it.hasNext()) {
				Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>) it.next();
				aprendizajeNT.write("Palabra:" + pair.getKey() + " Frec:" + pair.getValue() + " LogProb:"
						+ Math.log(pair.getValue() / hashmaptotal.get(pair.getKey())) + "\n");
				it.remove(); // avoids a ConcurrentModificationException
			}
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
				System.out.println("Done.");
			} catch (IOException e) {
			}
		}
	}
}