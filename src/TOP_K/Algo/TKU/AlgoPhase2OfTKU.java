package TOP_K.Algo.TKU;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import TOP_K.Algo.datastructures.redblacktree.RedBlackTree;
/** Triển khai giai đoạn 2 thuật toán TKU */
class AlgoPhase2OfTKU {

	/** ngưỡng tiện ích tối thiểu */
	private int minUtility;
	private int theCurrentK;

	/** Số lượng giao dịch (trans) */
	private int numberOfTransactions;

	private String inputFilePath;
	
	/** Tệp các ứng viên đã được sắp xếp */
	private String sortedCandidatePath;

	private String temporaryFilePathWHUIs = "HUI.txt";

	/** the output file */
	private String outputTopKHUIsFilePath;

	/** dấu phân cách được TKU sử dụng trong một số tệp */
	private final String delimiter = ":";

	/** Số lượng tập hữu ích cao top-k tìm được */
	private int numTopKHUI;

	void runAlgorithm(int minUtil, int transactionCount,
			int currentK, String inputPath, String sortedCandidateFile,
			String outputFile) throws IOException {

		minUtility = minUtil;
		numberOfTransactions = transactionCount;
		theCurrentK = currentK;
		inputFilePath = inputPath;
		sortedCandidatePath = sortedCandidateFile;
		outputTopKHUIsFilePath = outputFile;

		FileWriter fw = new FileWriter(temporaryFilePathWHUIs);
		BufferedWriter bfw = new BufferedWriter(fw);

		ArrayList<Integer> HDB[] = new ArrayList[numberOfTransactions];
		ArrayList<Integer> BNF[] = new ArrayList[numberOfTransactions];

		// Khởi tạo
		initialization(HDB, BNF, HDB.length);

		// Đọc cơ sở dữ liệu vào bộ nhớ
		readDatabase(HDB, BNF, HDB.length, inputFilePath);

	
		// Đọc ứng viên từ đĩa
		readCandidateItemsets(HDB, BNF, HDB.length, sortedCandidatePath, bfw);


		FileReader bf1 = new FileReader(temporaryFilePathWHUIs);
		BufferedReader bfr1 = new BufferedReader(bf1);

		FileWriter fw1 = new FileWriter(outputTopKHUIsFilePath);
		BufferedWriter bfw1 = new BufferedWriter(fw1);

		String record = "";
		setNumberOfTopKHUIs(0);
		while ((record = bfr1.readLine()) != null) {
			String temp[] = record.split(":");

			if (Integer.parseInt(temp[1]) >= minUtility) {
				
				bfw1.write(record);
				bfw1.newLine();
				setNumberOfTopKHUIs(getNumberOfTopKHUIs() + 1);
			}
		}

		bfw1.flush();
		fw1.close();
		bfw1.close();

		bf1.close();
		bfr1.close();

	

		fw.close();
		bfw.close();
		
		File fileToDelete = new File(temporaryFilePathWHUIs);
		fileToDelete.delete();
		fileToDelete = new File(sortedCandidateFile);
		fileToDelete.delete();

	}// End main()


	int readCandidateItemsets(ArrayList<Integer> HDB[], ArrayList<Integer> BNF[], int num_trans,
			String CIPath,  BufferedWriter Lbfw) throws IOException {

		RedBlackTree<StringPair> Heap = new RedBlackTree<StringPair>(true);

		FileReader bf = new FileReader(CIPath);
		BufferedReader bfr = new BufferedReader(bf);

		int num_HU = 0;

		String CIR = "";
		while ((CIR = bfr.readLine()) != null) {

			String CI[] = CIR.split(delimiter); // CIR[0] Candidate;
			// CIR[1] Estimate Utility;
			int Match_Count = 0;
			int EUtility = 0;

			String candidate[] = CI[0].split(" "); // candidate[0]:1
													// candidate[1]:3
													// candidate[2]:4
													// candidate[3]:5
			// Sort items in candidate

			// int candidate[] = sort_candidate(candidate1);

			// *********
			// System.out.println(CI[0]);

			if (Integer.parseInt(CI[1]) >= minUtility) {

				// For each Candidate CI[0], Scan DB
				for (int i = 0; i < num_trans; i++) {
					if (HDB[i].size() != 0) {
						// System.out.println(HDB[i]);

						Match_Count = 0;
						int PUtility = 0;

						for (int s = 0; s < candidate.length; s++) {
							if (HDB[i].contains(Integer.parseInt(candidate[s]))) {
								Match_Count++;

								int index = HDB[i].indexOf(Integer
										.parseInt(candidate[s]));
								ArrayList<Integer> B = BNF[i];

								int Ben = B.get(index);
								PUtility = PUtility + Ben;

								// System.out.println("Yes,utility="+Ben);
							} else {
								PUtility = 0;
								break;
							}

						}// end for-s


						if (Match_Count == candidate.length) {
							EUtility += PUtility;

						}// End if

					}// End if(HDB[i].size()!=0)

				}// End for-i


				if (EUtility >= minUtility) {
					Lbfw.write(CI[0] + ":" + EUtility);
					Lbfw.newLine();
					// System.out.println("HU:"+CI[0]+":"+EUtility);

					updateHeap(Heap, CI[0], EUtility);

					num_HU++;
				}
			}// if(CIR[1] >= MSC)

		}// End while

		Lbfw.flush();


		bf.close();
		bfr.close();

		return num_HU;

	}// End ReadCI

	static void readDatabase(ArrayList<Integer> HDB[], ArrayList<Integer> BNF[], int num_trans,
			String DBPath) throws IOException {
		FileReader bf = new FileReader(DBPath);
		BufferedReader bfr = new BufferedReader(bf);

		String record = "";
		int trans_count = 0;
		while ((record = bfr.readLine()) != null) {
			String data[] = record.split(":"); // data[0] Transaction;
												// data[1] TWU
												// data[2] Benefit

			String transaction[] = data[0].split(" ");
			String benefit[] = data[2].split(" ");

			for (int i = 0; i < transaction.length; i++) {
				HDB[trans_count].add(Integer.parseInt(transaction[i]));
				BNF[trans_count].add(Integer.parseInt(benefit[i]));

			}// End for-i
			trans_count++;

		}// End while

	}// End ReadDB

	void initialization(ArrayList HDB[], ArrayList BNF[], int num_trans) {
		for (int i = 0; i < num_trans; i++) {
			HDB[i] = new ArrayList<Integer>(0);
			BNF[i] = new ArrayList<Integer>(0);

		}// End for

	}// End Initialization

	void updateHeap(RedBlackTree<StringPair> NCH, String HUI, int Utility) {
		if (NCH.size() < theCurrentK) {
			NCH.add(new StringPair(HUI, Utility));
		} else if (NCH.size() >= theCurrentK) {
			if (Utility > minUtility) {
				NCH.add(new StringPair(HUI, Utility));
				NCH.popMinimum();

			}
		}// End if-else

		if ((NCH.minimum().y > minUtility) && (NCH.size() >= theCurrentK)) {
			minUtility = NCH.minimum().y;

			// System.out.println("raise ---->:"+gMin_Util);
		}

	}// End UpdateNodeCountHeap

	int getNumberOfTopKHUIs() {
		return numTopKHUI;
	}

	void setNumberOfTopKHUIs(int numTopKHUI) {
		this.numTopKHUI = numTopKHUI;
	}

}