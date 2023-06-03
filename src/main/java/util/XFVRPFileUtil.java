package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** 
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public class XFVRPFileUtil {

	public static void writeGZip(String content, String name){
		try (FileOutputStream output = new FileOutputStream(name + ".txt.gz")) {
			try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(output), StandardCharsets.ISO_8859_1)) {
				writer.write(content);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String readCompressedFile(String file){
		String out = null;
		File f = new File(file);
		try(
				FileInputStream fis = new FileInputStream(file);
				BufferedInputStream bis = new BufferedInputStream(fis);
				InputStream in = (f.getName().endsWith(".gz")) ?
						new GZIPInputStream(bis) :
						bis;
				) {
			out = getStreamContents(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}

	public static String getStreamContents(InputStream stream) throws IOException {
		StringBuilder content = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String lineSeparator = System.getProperty("line.separator");
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line + lineSeparator);
			}
			return content.toString();
		}
	}

	public static void main(String[] args) {
		writeGZip("This is a test", "test.txt");
		String out = readCompressedFile("test.txt");
		System.out.println(out);
	}
}
