package io.github.overrun.util;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("ALL")
public class Universal {

	public static void isMkdirs(String path) {
		File file = new File(path);
		if (!file.exists() && !file.isDirectory()) {
			file.mkdirs();
		}
	}

	/**
	 * mkdirs and create file
	 * @author baka4n
	 * @param path : file path
	 * @param file : file name
	 * @throws IOException : IOException
	 * @date 2022.2.15 : 2020.2.15
	 */
	public static void isCreateNewFile(String path,String file) throws IOException {
		File a = new File(file);
		isMkdirs(path);
		if (!a.exists()) {
			a.createNewFile();
		}
	}
}
