package tileworld;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WriteFile {
	public static void main(String[] args){
		clearFile();
		writeFile(1);
		writeFile(2);
	}
	
	public static void clearFile() {
		String filePath = "D:\\蒋浩锋\\硕士\\课程\\AI6125\\Project\\data"+Parameters.xDimension+".txt";
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(filePath);
			fileOutputStream.write( "".getBytes());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	//使用FIleOutputStream将数据写到文件中
	//如果文件不存在则创建文件
	public static void writeFile(int result){
		//创建FIleOutputStream对象
		String filePath = "D:\\蒋浩锋\\硕士\\课程\\AI6125\\Project\\data"+Parameters.xDimension+".txt";
		FileOutputStream fileOutputStream = null;


		try {
			fileOutputStream = new FileOutputStream(filePath, true);

			//此处的a改为getreward（）；
			int a = 1200;
			String str = Integer.toString(result);
			fileOutputStream.write( str.getBytes());
			fileOutputStream.write( "\n".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		finally {
			try {
				fileOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/
	}
	
	
}

