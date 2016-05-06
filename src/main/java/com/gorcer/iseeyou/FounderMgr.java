package com.gorcer.iseeyou;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.Vector;

import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;

/**
 * Клас для 
 * - хранения информации о полученном результате распознавания
 * - управления временными папками
 * - подсчета времени выполнения
 * @author gorcer
 *
 */
public class FounderMgr {

	public static FounderMgr self=null;
	public Vector<PlateInfo> plates;
	public long startTime, endTime;
	
	public String tmpPath = "/tmp/iSeeYouAnrp";
	private String tmpPathPostfix;
	
	public IplImage sourceImage;
	
	public static FounderMgr getInstance() {
		if (self == null)
		{
			//System.out.println("Create founder");
			self = new FounderMgr();
			self.plates = new Vector<PlateInfo>();
		}		
		return self;
	}	
	
	public boolean prepareEnv() {
		
		tmpPathPostfix = UUID.randomUUID().toString(); 
		//System.out.println("Prepare env " + tmpPathPostfix);
		// Создаем временное хранилище
		Path path = Paths.get(tmpPath + "/" + tmpPathPostfix);
		
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
			}
			catch (IOException e)
			{
				System.out.println(e.toString());
				return false;
			}
		} 
				
		return true;		
	}
	
	// Получение персональной папки для хранения временных файлов инстанса
	public String getPersonalTmpPath() {
		return tmpPath + "/" + tmpPathPostfix;
	}
	
	public void addPlate(PlateInfo plate) {
		plates.add(plate);
	}
	
	public void start() {
		startTime = System.currentTimeMillis();
	}
	
	public long finish() {
		endTime = System.currentTimeMillis();
		
		return endTime - startTime;
	}
	
	public long getWorkTime() {
		return (endTime - startTime)/1000;
	}
	
	public Vector<String> getNumbers() {
		
		Vector<String> result = new Vector<String>();
		
		for (int i=0;i<plates.size();i++) 
		for (int j=0;j<plates.get(i).numbers.size();j++)
		{
			if (!result.contains(plates.get(i).numbers.get(j)))
				result.add(plates.get(i).numbers.get(j));
		}
		
		return result;
	}

	public Vector<CvSeq> getSquares() {
		
		Vector<CvSeq> result =new Vector<CvSeq>();
		
		for (int i=0;i<plates.size();i++) {
			result.add(plates.get(i).plateCoords);
		}
		
		// TODO Auto-generated method stub
		return result;
	}
}