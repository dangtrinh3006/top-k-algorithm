package TOP_K.Algo.TKO;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import TOP_K.Algo.TKO.AlgoTKO_Basic;


public class MainTestTKOBasic {

	public static void main(String [] arg) throws IOException{

		// input file path
		String input = fileToPath("DB_Utility.txt");
		
		// output file path
		String output = "output_TKO.txt";
		
		// tham số k
		int k = 100;
		
		// áp dụng thuật toán tko
		AlgoTKO_Basic algorithm = new AlgoTKO_Basic();
		algorithm.runAlgorithm(input, output, k);
		algorithm.writeResultTofile(output);
		
		// in dữ liệu thống kê
		algorithm.printStats();
	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = MainTestTKOBasic.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}
