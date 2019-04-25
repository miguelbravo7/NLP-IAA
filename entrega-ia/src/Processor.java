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
	String filename;
	HashMap<String, Integer> hm = new HashMap<String, Integer>();

	public static void main(String[] args) {
		Processor algo = new Processor("src/CTR_TRAIN.txt");
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
			String text = null;

			while ((text = reader.readLine()) != null) {
				if(text.isEmpty()) {
					continue;
				}
				String[] valores = text.split(",");
				valores[0] = valores[0].replaceAll("\"", "").trim();
				//System.out.println(valores[1]);
				// contar las palabras
				for (String linea : valores[0].split("\\s+")) {
					if (!hm.containsKey(linea)) {
						hm.put(linea, 1);
					} else {
						hm.put(linea, hm.get(linea) + 1);
					}
				}
				// escribe en el fichero troll
				if (valores[1].trim().equalsIgnoreCase("troll")) {
					//System.out.println(valores[1] + " : troll");
					troll.write(valores[0] + "\n");
				} else {
					//System.out.println(valores[1] + " : not_troll");
					notroll.write(valores[0] + "\n");
				}
				corpus.write(valores[0] + "\n");
			}
			Iterator<?> it = hm.entrySet().iterator();
			dict.append("Numero de palabras total:" + hm.size() + "\n");
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				dict.write("Numero de palabras:" + pair.getValue() + "\n");
				dict.write("Palabra:" + pair.getKey() + "\n");
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