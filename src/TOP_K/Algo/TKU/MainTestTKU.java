package TOP_K.Algo.TKU;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

public class MainTestTKU {

	public static void main(String [] arg) throws IOException{

		// Input file path
		String input = fileToPath("DB_Utility.txt");
		
		// Output file path
		String output = "output_TKU.txt";
		
		// Tham số k
		int k = 10;  

		// Áp dụng thuật toán TKU
		AlgoTKU algo = new AlgoTKU();
		algo.runAlgorithm(input, output, k);
		
		// Hiển thị kết quả thống kê
		algo.printStats();
	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = MainTestTKU.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}
