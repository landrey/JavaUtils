package fr.loria.madynes.javautils;

import java.io.File;

/**
 * Ugly utility class to handle file path on a mixed environment Unix/Windows.
 * @author andrey
 *
 */
public class FilePath {
	/** 
	 * WARING: there are many problems: special characters, \ or / in path...
	 * @param args
	 */
	public static String convertToCurrentOS(String path){
		String result;
		if (path.contains(File.separator)){
			result=path;
		}else{
			result=path.replace(File.separatorChar=='/'?'\\':'/', File.separatorChar);
		}
		return result;
	}
	public static String getBaseName(String path){
		return (new File(convertToCurrentOS(path)).getName()); // heavy lastIndex + subtring...
	}
	public static String getBaseNameNoPrefix(String path){
		String s=getBaseName(path);
		int dotIdx=s.lastIndexOf('.');
		if (dotIdx!=-1){
			s=s.substring(0, dotIdx);
		}
		return s;
	}
}
