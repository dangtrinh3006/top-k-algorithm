package TOP_K.Algo.TKU;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.PriorityQueue;

import TOP_K.Algo.episodes.upspan.CalculateDatabaseInfo;
import TOP_K.Algo.datastructures.redblacktree.RedBlackTree;
import TOP_K.Algo.tools.MemoryLogger;

public class AlgoTKU {

	/** Đường dẫn cho CSDL */
	private String theInputFile;

	/** File tập tiềm năng */
	private String theCandidateFile;

	/** Tham số k, số lượng HUI mong muốn. */
	private int kValue;

	/** Số lượng item trong CSDL. */
	private int itemCount;

	/** min_util_Border (giá trị hữu ích tối thiểu biên). */
	private double globalMinUtil = 0;

	/** gAr_P1: Bộ P1. */
	private int[] arrayTWUItems;

	/** gAr_Miu: item và độ hữu ích tối thiểu */
	private int[] arrayMIU;

	/** gAr_Mau: item và độ hữu ích tối đa */
	private int[] arrayMAU;

	/** Tổng thời gian thực thi */
	private double totalTime;

	/** Số lượng mẫu được tìm thấy */
	private int patternCount;

	
	public void runAlgorithm(String inputFile, String outputFile, int k)
			throws IOException {

		MemoryLogger.getInstance().reset();
		totalTime = System.currentTimeMillis();

		globalMinUtil = 0;

		// ------------ PHASE 0 -----------------------
		// //Tính toán số liệu thống kê về cơ sở dữ liệu, theo yêu cầu của thuật toán
		CalculateDatabaseInfo tool = new CalculateDatabaseInfo(inputFile);
		tool.runCalculate();

		// Thời gian dựng cây

		ArrayList<Integer> ulist = new ArrayList<Integer>(0);

		kValue = k;
		theInputFile = inputFile;
		theCandidateFile = "topKcandidate.txt";
		itemCount = tool.getMaxID() + 1;

		arrayTWUItems = new int[itemCount];
		arrayMIU = new int[itemCount];
		arrayMAU = new int[itemCount];


		FileWriter fw_CI = new FileWriter(theCandidateFile);
		BufferedWriter bfw_CI = new BufferedWriter(fw_CI);

		// Tạo P1 & thực hiện Pre_Evaluation
		// Tạo 1 HUI tiềm năng

		globalMinUtil = preEvaluation(theInputFile, arrayTWUItems, itemCount,
				arrayMIU, arrayMAU, globalMinUtil, kValue);


		// Theo ulist để xây dựng FP-Tree
		FPtree tree = BuildUPTree(arrayTWUItems, theInputFile);

		// Tính toán có bao nhiêu nút trong UP-Tree
		tree.traverse_tree(tree.root, 0);


		RedBlackTree<Integer> DSNodeCountHeap = new RedBlackTree<Integer>(true);

		for (int i = 0; i < tree.root.childlink.size(); i++) {
			int[] Sum_DS = new int[itemCount];

			int DSItem = tree.root.childlink.get(i).item;

			tree.SumDescendent(tree.root.childlink.get(i), Sum_DS);

			for (int j = 0; j < Sum_DS.length; j++) {
				if ((Sum_DS[j] != 0) && (j != DSItem)) {
					int DS_Value = (arrayMIU[j] + arrayMIU[DSItem]) * Sum_DS[j];

					UpdateNodeCountHeap(DSNodeCountHeap, DS_Value);
				}
			}// End for-j
		}// End for-i


		DSNodeCountHeap = new RedBlackTree<Integer>(true);

		RedBlackTree<Integer> ISNodeCountHeap = new RedBlackTree<Integer>(true);


		getUlist(arrayTWUItems, ulist);

		// Giai đoạn khai thác
		String prefix = "";

		tree.UPGrowth(tree, ulist, prefix, bfw_CI, ISNodeCountHeap,
				arrayTWUItems);


		// Output P1
		for (int i = 0; i < arrayTWUItems.length; i++) {
			if (arrayTWUItems[i] >= globalMinUtil) {
				bfw_CI.write(i + ":" + arrayTWUItems[i]);
				bfw_CI.newLine();
			}
		}

		bfw_CI.close();
		fw_CI.close();

		MemoryLogger.getInstance().checkMemory();

		// //================================ PHASE 2
		String sortedTopKcandidateFile = "sortedTopKcandidate.txt";
		runSortHUIAlgorithm(theCandidateFile, sortedTopKcandidateFile);

		// Xóa tất cả các tệp tạm thời cho cơ sở dữ liệu vertical (dọc)
		File fileToDelete = new File(theCandidateFile);
		fileToDelete.delete();

		MemoryLogger.getInstance().checkMemory();

		// ================================== PHASE 3

		AlgoPhase2OfTKU algoPhase2 = new AlgoPhase2OfTKU();
		algoPhase2.runAlgorithm((int)globalMinUtil, tool.getDBSize(), k,
				inputFile, sortedTopKcandidateFile, outputFile);

		patternCount = algoPhase2.getNumberOfTopKHUIs();

		MemoryLogger.getInstance().checkMemory();

		// =================================== Thống kê
		totalTime = (System.currentTimeMillis() - totalTime) / 1000.0;
		

	}// End main()
	
	/**
	 * Run the algorithm
	 * @param theCandidateFile a file path to a candidate file
	 * @param sortedTopKcandidateFile a path to a sorted candidate file
	 * @throws IOException if error occur while reading/writing to file
	 */
	void runSortHUIAlgorithm(String theCandidateFile, String sortedTopKcandidateFile)
			throws IOException {


		// Ghi lại thời gian bắt đầu
		double StartTime = System.currentTimeMillis();

		// file lưu trữ tập ứng viên
		String gFp_HUI = theCandidateFile;

		// file lưu trữ tập ứng viên
		String TopK_HUI = sortedTopKcandidateFile;

		FileReader bf = new FileReader(gFp_HUI);
		BufferedReader bfr = new BufferedReader(bf);

		// TopK List
		RedBlackTree<StringPair> Heap = new RedBlackTree<StringPair>(true);

		String record = "";
		int numHUIs = 0;
		while ((record = bfr.readLine()) != null) {
			String temp[] = record.split(":");

			Heap.add(new StringPair(temp[0], Integer.parseInt(temp[1])));

			numHUIs++;

		}
		bfr.close();
		bf.close();

		FileWriter fw1 = new FileWriter(TopK_HUI);
		BufferedWriter bfw1 = new BufferedWriter(fw1);

		int nElements = Heap.size();
		for (int i = 0; i < nElements; i++) {
			bfw1.write(Heap.maximum().x + ":" + Heap.maximum().y);
			bfw1.newLine();


			Heap.popMaximum();
		}

		bfw1.flush();
		bfw1.close();
		fw1.close();
	}

	/**
	 * Print statistics about the algorithm execution
	 */
	public void printStats() {
		System.out.println("THUC NGHIEM THUAT TOAN TKU");
		System.out.println(" TONG THOI GIAN THUC THI : " + totalTime + " s");
		System.out.println(" SO TAP HUU ICH CAO top-k : "
				+ patternCount);
		System.out.println(" TIEU THU BO NHO : "
				+ MemoryLogger.getInstance().getMaxMemory() + " MB");
		System.out
				.println("--------------------------");
	}

	public double preEvaluation(String HDB, int[] TWU1, int num_Item,
			int[] MinBNF, int[] MaxBNF, double mini_utility, int pK)
			throws IOException {
		

		TKUTriangularMatrix a = new TKUTriangularMatrix(num_Item);

		// Đọc Database
		FileReader fr = new FileReader(HDB);
		BufferedReader bfr = new BufferedReader(fr);

		String transaction = "";

		while ((transaction = bfr.readLine()) != null) {

			// System.out.println("Transaction:#"+num_Trans);

			String temp1[] = transaction.split(":"); // Transaction:TWU:Items'
														// utility

			String temp2[] = temp1[0].split(" "); // Items in a transaction

			String temp3[] = temp1[2].split(" "); // utility of items in a
													// transaction

			for (int s = 0; s < temp2.length; s++) {
				// System.out.println("transaction:"+transaction);

				// System.out.println(Integer.parseInt(temp2[s])+":"+Integer.parseInt(temp3[s]));
				// System.out.println("MinBNF.length:"+MinBNF.length);

				// Find mius of items
				if (MinBNF[Integer.parseInt(temp2[s])] == 0) {
					if (Integer.parseInt(temp3[s]) > 0) {
						MinBNF[Integer.parseInt(temp2[s])] = Integer
								.parseInt(temp3[s]);
					}
				} else if (MinBNF[Integer.parseInt(temp2[s])] > (Integer
						.parseInt(temp3[s]))) {
					MinBNF[Integer.parseInt(temp2[s])] = Integer
							.parseInt(temp3[s]);
				}

				// Find maus of items
				if (MaxBNF[Integer.parseInt(temp2[s])] < (Integer
						.parseInt(temp3[s]))) {
					MaxBNF[Integer.parseInt(temp2[s])] = Integer
							.parseInt(temp3[s]);
				}

				// System.out.println("OKOK");

				TWU1[Integer.parseInt(temp2[s])] += Integer.parseInt(temp1[1]);

				if (s > 0) {
					// System.out.println(a.toString());

					// System.out.println(temp2[0]+" "+temp2[s]+":"+(Integer.parseInt(temp3[0])+Integer.parseInt(temp3[s])));

					a.incrementCount(
							Integer.parseInt(temp2[0]),
							Integer.parseInt(temp2[s]),
							Integer.parseInt(temp3[0])
									+ Integer.parseInt(temp3[s]));
				}

			}// End for

		}// End while

		fr.close();
		bfr.close();

		// End Read Database

		// System.out.println(a.toString());

		// use parameter k and the heap to determine the initial minimum utility

		MemoryLogger.getInstance().checkMemory();

		double Initial_BUT = getInitialUtility(a, num_Item, pK);

		return Initial_BUT;

	}// End fPre_Evaluation


	public class HeapEntry implements Comparable<HeapEntry> {

		/** A count value */
		protected int count;

		/** The priority of tihs value */
		protected int priority;

		@Override
		public int compareTo(HeapEntry o) {

			return o.priority - this.priority;
		}
	}

	public double getInitialUtility(TKUTriangularMatrix TM, int nItem, int K) {


		PriorityQueue<HeapEntry> topKList = new PriorityQueue<HeapEntry>();

		int count = 0;

		for (int i = 0; i < nItem; i++) {
			for (int j = 0; j < TM.matrix[i].length; j++) {
				if (TM.matrix[i][j] != 0) {
					if (topKList.size() < K) {
						count++;
						HeapEntry entry = new HeapEntry();
						entry.count = count;
						entry.priority = TM.matrix[i][j];
						topKList.add(entry);
						// topKList.add(count, TM.matrix[i][j]);
					} else if (topKList.size() >= K) {
						if (TM.matrix[i][j] > topKList.peek().priority) {
							count++;
							HeapEntry entry = new HeapEntry();
							entry.count = count;
							entry.priority = TM.matrix[i][j];
							topKList.add(entry);
							// topKList.enqueue(count, TM.matrix[i][j]);
							topKList.poll();
							// topKList.dequeueMin();
							// System.out.println("Raise threshold:"+TopKList.min().mPriority);
						}
					}// End if-else
				}
			}// End for-j
		}// End for-i
			// System.out.println("Initial utility is:"+topKList.peek().priority);
		// System.out.println("count:"+count);

		return topKList.peek().priority;

		
	}

	public void getUlist(int[] P1, ArrayList<Integer> list) {
		for (int i = 0; i < P1.length; i++) {
			if (P1[i] > 0) {
				if (P1[i] >= globalMinUtil) {
					// (Output P1)
					InsertItem(list, i, P1);
				}
			}// if(TWU1[i]>0)
		}// End for

	}

	public int InsertItem(ArrayList<Integer> list, int target, int Order[]) {
		if (list.size() == 0) {
			list.add(target);
		} else if (list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				if (Order[target] > Order[list.get(i)]) {
					list.add(i, target);
					return 0;
				} else if ((Order[target] == Order[list.get(i)])
						&& (target < list.get(i))) {
					list.add(i, target);
					return 0;
				} else if (i == (list.size() - 1)) {
					list.add(target);
					return 0;
				}
			}

		}// End if-else

		return -1; // if error return -1
	}// End InsertItem

	public void sorttrans(int tran[], int pre, int tranlen, int P1[]) {
		int temp;

		for (int i = pre; i < tranlen - 1; i++) {
			for (int j = pre; j < tranlen - 1; j++) {
				if (P1[tran[j]] < P1[tran[j + 1]]) {
					temp = tran[j];
					tran[j] = tran[j + 1];
					tran[j + 1] = temp;
				} else if (P1[tran[j]] == P1[tran[j + 1]]) {
					if (tran[j] > tran[j + 1]) {
						temp = tran[j];
						tran[j] = tran[j + 1];
						tran[j + 1] = temp;
					}
				}// End if-else
			}// End for-j
		}// End for-i
	}// End sorttrans

	public void sorttrans2(int tran[], String bran[], int pre, int tranlen,
			int P1[]) {
		int temp1;
		String temp2;

		for (int i = pre; i < tranlen - 1; i++) {
			for (int j = pre; j < tranlen - 1; j++) {
				if (P1[tran[j]] < P1[tran[j + 1]]) {
					temp1 = tran[j];
					temp2 = bran[j];

					tran[j] = tran[j + 1];
					bran[j] = bran[j + 1];

					tran[j + 1] = temp1;
					bran[j + 1] = temp2;
				} else if (P1[tran[j]] == P1[tran[j + 1]]) {
					if (tran[j] > tran[j + 1]) {
						temp1 = tran[j];
						temp2 = bran[j];

						tran[j] = tran[j + 1];
						bran[j] = bran[j + 1];

						tran[j + 1] = temp1;
						bran[j + 1] = temp2;
					}
				}// End if-else
			}// End for-j
		}// End for-i
	}// End sorttrans

	public void UpdateNodeCountHeap(RedBlackTree<Integer> NCH, int NewValue) {
		if (NCH.size() < kValue) {
			NCH.add(NewValue);
		} else if (NCH.size() >= kValue) {
			if (NewValue > globalMinUtil) {
				NCH.add(NewValue);
				NCH.popMinimum();
			}
		}// End if-else

		if ((NCH.minimum() > globalMinUtil) && (NCH.size() >= kValue)) {
			globalMinUtil = NCH.minimum();

			// System.out.println("raise ---->:"+gMin_Util);
		}

	}// End UpdateNodeCountHeap

	public FPtree BuildUPTree(int P1[], String HDB) throws IOException // �إ߾�
																		// //For
																		// Global
																		// Tree,
																		// use
																		// instrans2
	{
		// TopK List
		RedBlackTree<Integer> NodeCountHeap = new RedBlackTree<Integer>(true);

		FPtree tree = new FPtree();

		// Read Database again

		FileReader fr = new FileReader(HDB);
		BufferedReader bfr = new BufferedReader(fr);

		String transaction;
		while ((transaction = bfr.readLine()) != null) {
			// System.out.println(gMin_Util);

			String temp1[] = transaction.split(":");

			String temp2[] = temp1[0].split(" ");
			String bran[] = temp1[2].split(" ");
			String bran2[] = new String[bran.length];

			int tranlen = 0;
			int tran[] = new int[temp2.length];

			for (int m = 0; m < temp2.length; m++) {
				if (P1[Integer.parseInt(temp2[m])] >= globalMinUtil) {
					bran2[tranlen] = bran[m];
					tran[tranlen] = Integer.parseInt(temp2[m]);
					tranlen++;
				}
			}
			sorttrans2(tran, bran2, 0, tranlen, P1); // sort transaction
			tree.instrans3(tran, bran2, tranlen, P1, 1, NodeCountHeap); // Insert
																		// transaction
																		// to
																		// tree

		}// End for

		fr.close();
		bfr.close();

		MemoryLogger.getInstance().checkMemory();

		return tree;

	}// End BuildUPTree()

	public class treenode {
		int item;
		int count;
		int twu;

		treenode hlink = null;
		treenode parentlink = null;

		ArrayList<treenode> childlink;

		public treenode(int item, int TWU, int count) {
			this.item = item; // item of node X
			this.count = count; // count of node X
			this.twu = TWU;
			this.childlink = new ArrayList<treenode>(0); // children nodes of
															// node X
			this.hlink = null; // horizontal link of node X
			this.parentlink = null;

		}// End treenode() constructor

	}// End class treenode

	public class FPtree {
		treenode root; // the root of tree

		treenode HeaderTable[] = new treenode[itemCount];

		public FPtree() // constructor of FPTree
		{
			// the Item of root is -1 and count is 0
			this.root = new treenode(-1, 0, 0);

			// Initialization of HeaderTable

			for (int i = 0; i < HeaderTable.length; i++) {

				this.HeaderTable[i] = null;

			}// End for

		}// End constructor of FPtree

		// Insert transaction to FPtree
		public void insPatternBase(int tran[], int tranlen, int L1[], int TWU,
				int IC, int SumBNF) // For Local Tree
		{
			treenode par = root;

			for (int i = 0; i < tranlen; i++) {

				int target = tran[i];
				int cs = par.childlink.size();

				if (cs == 0) {
					int M = TWU - (SumBNF - arrayMIU[target] * IC);
					SumBNF = SumBNF - (arrayMIU[target] * IC);

					treenode nNode = new treenode(target, M, IC);

					par.childlink.add(nNode);

					nNode.parentlink = par;

					if (HeaderTable[target] == null) {
						HeaderTable[target] = nNode;
					} else {
						nNode.hlink = HeaderTable[target];
						HeaderTable[target] = nNode;
					}

					par = nNode;
				} else {
					for (int j = 0; j < cs; j++) {
						treenode comp = par.childlink.get(j);

						if (target == comp.item) {
							int M = TWU - (SumBNF - arrayMIU[target] * IC);
							SumBNF = SumBNF - arrayMIU[target] * IC;

							comp.twu += M;
							comp.count += IC;
							par = comp;
							break;
						} else if (L1[target] > L1[comp.item]) {
							int M = TWU - (SumBNF - arrayMIU[target] * IC);
							SumBNF = SumBNF - (arrayMIU[target] * IC);

							treenode nNode = new treenode(target, M, IC);
							par.childlink.add(j, nNode);

							nNode.parentlink = par;

							if (HeaderTable[target] == null) {
								HeaderTable[target] = nNode;
							} else {
								nNode.hlink = HeaderTable[target];
								HeaderTable[target] = nNode;
							}

							par = nNode;
							break;
						} else if (((L1[target] == L1[comp.item]))
								&& ((target < comp.item))) {

							int M = TWU - (SumBNF - arrayMIU[target] * IC);
							SumBNF = SumBNF - (arrayMIU[target] * IC);

							treenode nNode = new treenode(target, M, IC);
							par.childlink.add(j, nNode);

							nNode.parentlink = par;

							if (HeaderTable[target] == null) {
								HeaderTable[target] = nNode;
							} else {
								nNode.hlink = HeaderTable[target];
								HeaderTable[target] = nNode;
							}

							par = nNode;
							break;
						} else if (j == (cs - 1)) {
							int M = TWU - (SumBNF - arrayMIU[target] * IC);
							SumBNF = SumBNF - (arrayMIU[target] * IC);

							treenode nNode = new treenode(target, M, IC);
							par.childlink.add(nNode);

							nNode.parentlink = par;

							if (HeaderTable[target] == null) {
								HeaderTable[target] = nNode;
							} else {
								nNode.hlink = HeaderTable[target];
								HeaderTable[target] = nNode;
							}

							par = nNode;

						}// End if-else
					}// End for-j
				}// End if-else
			}// End for-i
		}// End instrans

		public void instrans2(int tran[], String bran[], int tranlen, int L1[],
				int IC) // For Global Tree
		{
			int TWU = 0;

			treenode par = root;

			for (int i = 0; i < tranlen; i++) {

				TWU += Integer.parseInt(bran[i]);

				int target = tran[i];
				int cs = par.childlink.size();

				if (cs == 0) {
					treenode nNode = new treenode(target, TWU, IC);
					par.childlink.add(nNode);

					nNode.parentlink = par;

					// �W�[horizontal link���{���X
					if (HeaderTable[target] == null) {
						HeaderTable[target] = nNode;
					} else {
						nNode.hlink = HeaderTable[target];
						HeaderTable[target] = nNode;
					}

					par = nNode;
				} else {
					for (int j = 0; j < cs; j++) {
						treenode comp = par.childlink.get(j);

						if (target == comp.item) {
							comp.twu += TWU;
							comp.count += IC;
							par = comp;
							break;
						} else if (L1[target] > L1[comp.item]) {
							treenode nNode = new treenode(target, TWU, IC);
							par.childlink.add(j, nNode);

							nNode.parentlink = par;

							// �W�[horizontal link���{���X
							if (HeaderTable[target] == null) {
								HeaderTable[target] = nNode;
							} else {
								nNode.hlink = HeaderTable[target];
								HeaderTable[target] = nNode;
							}

							par = nNode;
							break;
						} else if (((L1[target] == L1[comp.item]))
								&& ((target < comp.item))) {
							treenode nNode = new treenode(target, TWU, IC);
							par.childlink.add(j, nNode);

							nNode.parentlink = par;

							// �W�[horizontal link���{���X
							if (HeaderTable[target] == null) {
								HeaderTable[target] = nNode;
							} else {
								nNode.hlink = HeaderTable[target];
								HeaderTable[target] = nNode;
							}

							par = nNode;
							break;
						} else if (j == (cs - 1)) {
							treenode nNode = new treenode(target, TWU, IC);
							par.childlink.add(nNode);

							nNode.parentlink = par;

							if (HeaderTable[target] == null) {
								HeaderTable[target] = nNode;
							} else {
								nNode.hlink = HeaderTable[target];
								HeaderTable[target] = nNode;
							}

							par = nNode;

						}// End if-else
					}// End for-j
				}// End if-else
			}// End for-i
		}// End instrans

		public void instrans3(int tran[], String bran[], int tranlen, int L1[],
				int IC, RedBlackTree<Integer> NodeCountHeap) // For Global Tree
		{
			int TWU = 0;

			treenode par = root;

			for (int i = 0; i < tranlen; i++) {
				TWU += Integer.parseInt(bran[i]);

				int target = tran[i];
				int cs = par.childlink.size();

				if (cs == 0) {
					treenode nNode = new treenode(target, TWU, IC);
					par.childlink.add(nNode);

					if (nNode.twu > globalMinUtil) {
						UpdateNodeCountHeap(NodeCountHeap, nNode.twu);
					}

					nNode.parentlink = par;

					if (HeaderTable[target] == null) {
						HeaderTable[target] = nNode;
					} else {
						nNode.hlink = HeaderTable[target];
						HeaderTable[target] = nNode;
					}

					par = nNode;
				} else {
					for (int j = 0; j < cs; j++) {
						treenode comp = par.childlink.get(j);

						if (target == comp.item) {
							NodeCountHeap.remove(comp.twu);
							UpdateNodeCountHeap(NodeCountHeap, (comp.twu + TWU));

							comp.twu += TWU;
							comp.count += IC;
							par = comp;
							break;
						} else if (L1[target] > L1[comp.item]) {
							// new
							if (comp.twu > globalMinUtil) {
								UpdateNodeCountHeap(NodeCountHeap, TWU);
							}

							treenode nNode = new treenode(target, TWU, IC);
							par.childlink.add(j, nNode);

							nNode.parentlink = par;

							if (HeaderTable[target] == null) {
								HeaderTable[target] = nNode;
							} else {
								nNode.hlink = HeaderTable[target];
								HeaderTable[target] = nNode;
							}

							par = nNode;
							break;
						} else if (((L1[target] == L1[comp.item]))
								&& ((target < comp.item))) {
							// new
							if (comp.twu > globalMinUtil) {
								UpdateNodeCountHeap(NodeCountHeap, TWU);
							}

							treenode nNode = new treenode(target, TWU, IC);
							par.childlink.add(j, nNode);

							nNode.parentlink = par;

							if (HeaderTable[target] == null) {
								HeaderTable[target] = nNode;
							} else {
								nNode.hlink = HeaderTable[target];
								HeaderTable[target] = nNode;
							}

							par = nNode;
							break;
						} else if (j == (cs - 1)) {
							// new
							if (comp.twu > globalMinUtil) {
								UpdateNodeCountHeap(NodeCountHeap, TWU);
							}

							treenode nNode = new treenode(target, TWU, IC);
							par.childlink.add(nNode);

							nNode.parentlink = par;

							if (HeaderTable[target] == null) {
								HeaderTable[target] = nNode;
							} else {
								nNode.hlink = HeaderTable[target];
								HeaderTable[target] = nNode;
							}

							par = nNode;

						}// End if-else
					}// End for-j
				}// End if-else
			}// End for-i
		}// End instrans

		// For Global
		public void UPGrowth(FPtree tree2, ArrayList<Integer> flist2,
				String prefix, BufferedWriter bfw_UCI,
				RedBlackTree<Integer> ISNodeCountHeap, int LP1[])
				throws IOException {
			// Duyệt qua Buttom-up của Header Table
			for (int i = 0; i < flist2.size(); i++) {
				if (LP1[flist2.get(i)] >= globalMinUtil) {

					String Nprefix = "";
					if (prefix.equals("")) {
						Nprefix = prefix.concat(flist2.get(i) + "");
					} else {
						Nprefix = prefix.concat(" " + flist2.get(i));
					}

					

					int citem = flist2.get(i); // get current item
					treenode chlink = tree2.HeaderTable[citem]; // current
																// horizontal
																// link

					// Conditional pattern base & count
					ArrayList<ArrayList> CPB = new ArrayList<ArrayList>(0);
					ArrayList<Integer> CPBW = new ArrayList<Integer>(0); // for
																			// twu
					ArrayList<Integer> CPBC = new ArrayList<Integer>(0); // for
																			// count

					int LocalF1[] = new int[itemCount];
					int LocalCount[] = new int[itemCount];
					int HLink_count = 0;

					// Traverse Horizontal links, and merge subtrees into a
					// cofi_tree
					while (chlink != null) {

						// System.out.println(chlink);

						// System.out.println(chlink.item+":"+chlink.count);
						ArrayList<Integer> path = new ArrayList<Integer>(0);

						treenode cptr = chlink;

						while (cptr.parentlink != null) {
							// System.out.println(cptr.item);

							path.add(cptr.item);

							LocalF1[cptr.item] = LocalF1[cptr.item]
									+ chlink.twu;
							LocalCount[cptr.item] = LocalCount[cptr.item]
									+ chlink.count;
							cptr = cptr.parentlink;

						}// End while

						// Put path into Conditional pattern base

						path.remove((int) 0); // remove the first item

						CPB.add(path);
						CPBW.add(chlink.twu);
						CPBC.add(chlink.count);

						// Turn to the next horizontal link
						chlink = chlink.hlink;

					}// End while

					
					// tạo localflist
					ArrayList<Integer> localflist = new ArrayList<Integer>(0);

					for (int j = 0; j < LocalF1.length; j++) {
						if (LocalF1[j] < globalMinUtil) {
							LocalF1[j] = -1;
						} else {
							if (j != citem) {
								InsertItem(localflist, j, LocalF1);

								/*
								 * String fprefix=""+citem; fprefix=
								 * fprefix.concat(" "+j);
								 * 
								 * System.out.println(fprefix+":"+LocalF1[j]);
								 */
								/*--*/// Output FI

								String UTI = Nprefix + " " + j;
								String TempItem[] = UTI.split(" ");
								int SumMau = 0;
								int SumMiu = 0;

								for (int u = 0; u < TempItem.length; u++) {
									SumMau += arrayMAU[Integer
											.parseInt(TempItem[u])];
									SumMiu += arrayMIU[Integer
											.parseInt(TempItem[u])];
								}

								int MAU = SumMau * LocalCount[j];

								if (MAU >= globalMinUtil) {
									// System.out.println("yes-MAU");

									int MIU = SumMiu * LocalCount[j];

									// bfw_UCI.write(Nprefix +
									// " "+j+","+LocalF1[j]+","+MIU);

									bfw_UCI.write(Nprefix + " " + j + ":"
											+ LocalF1[j]);

									bfw_UCI.newLine();

									// if(gNum_CI%1000==0)
									// System.out.println(gNum_CI+"::"+ISNodeCountHeap.size()+"::"+gMin_Util+"::"+gMaxMemory);

									// System.out.println(Nprefix +
									// " "+j+","+LocalF1[j]+","+MIU);

									if (MIU > globalMinUtil) {
										// raise minimum utility
										// System.out.println(gNum_CI+" ***Put*** "+
										// Nprefix +
										// " "+j+","+LocalF1[j]+","+MIU);

										UpdateNodeCountHeap(ISNodeCountHeap,
												MIU);

									}// End if
								}// End if

								/*--*/// System.out.println(Nprefix +
											// " "+j+":"+LocalF1[j]);

								// if(num_FI % 10000 ==0)
								// System.out.println("num_CI:"+num_FI);

							}

						}// End if-else
					}// End for-j
						// local infrequent items are filtering

					// insert current item (prefix) into the first order
					// localflist.add(0,citem);

					/*--*/// System.out.println("localflist");

					if (CPB.size() == 0) {

					} else {

						// Build Tree for citem according to conditional pattern
						// base

						FPtree C_FPtree = new FPtree();

						/*--*/// System.out.println("Conditional pattern base (after sort)");

						for (int k = 0; k < CPB.size(); k++) {
							ArrayList<Integer> ltran = CPB.get(k);

							int Sum_MinBNF = 0;

							// System.out.println(ltran+":"+CPBC.get(k));

							// delete infrequent node
							int tran[] = new int[ltran.size()];
							int tranlen = 0;

							for (int h = 0; h < ltran.size(); h++) {
								if (LocalF1[ltran.get(h)] >= globalMinUtil) {

									Sum_MinBNF = Sum_MinBNF + CPBC.get(k)
											* arrayMIU[ltran.get(h)];

									tran[tranlen++] = ltran.get(h);
								} else {
									int sum = CPBW.get(k);
									sum = sum - CPBC.get(k)
											* arrayMIU[ltran.get(h)];

									CPBW.set(k, sum);
								}// End if-else
							}// End for-h

							sorttrans(tran, 0, tranlen, LocalF1);

							/*
							 * for(int h=0; h<tranlen; h++) {
							 * System.out.print(tran[h]+" "); if(h==(tranlen-1))
							 * System.out.println();
							 * 
							 * }//End for
							 */

							C_FPtree.insPatternBase(tran, tranlen, LocalF1,
									CPBW.get(k), CPBC.get(k), Sum_MinBNF);

							// C_FPtree.insPatternBase(tran, tranlen, LocalF1,
							// CPBW.get(k),CPBC.get(k),Sum_MinBNF);

						}// End for-k

						// System.out.println("localflist for "+"{"+Nprefix+"}:"+localflist);

						// traverse_tree(C_FPtree.root,0);

						// System.out.println();

						C_FPtree.UPGrowth_MinBNF(C_FPtree, localflist, Nprefix,
								bfw_UCI, ISNodeCountHeap, LocalF1);
					}
				}// End if(LP1[flist2.get(i)] >= gMin_Util)
			}// End for-i

			MemoryLogger.getInstance().checkMemory();

			bfw_UCI.flush();

		}// End FPGrowth()

		// ************************************************************************************************//

		public void UPGrowth_MinBNF(FPtree tree2, ArrayList<Integer> flist2,
				String prefix, BufferedWriter bfw_UCI,
				RedBlackTree<Integer> ISNodeCountHeap, int[] LP1)
				throws IOException {
			for (int i = 0; i < flist2.size(); i++) {
				if (LP1[flist2.get(i)] >= globalMinUtil) {

					String Nprefix = "";
					if (prefix.equals("")) {
						Nprefix = prefix.concat(flist2.get(i) + "");
					} else {
						Nprefix = prefix.concat(" " + flist2.get(i));
					}

					

					int citem = flist2.get(i); // get current item
					treenode chlink = tree2.HeaderTable[citem]; // current
																// horizontal
																// link

					// Conditional pattern base & count
					ArrayList<ArrayList> CPB = new ArrayList<ArrayList>(0);
					ArrayList<Integer> CPBW = new ArrayList<Integer>(0); // for
																			// twu
					ArrayList<Integer> CPBC = new ArrayList<Integer>(0); // for
																			// count

					int LocalF1[] = new int[itemCount]; // Local frequent
														// 1-items
					int LocalCount[] = new int[itemCount];

					
					while (chlink != null) {

						
						ArrayList<Integer> path = new ArrayList<Integer>(0);

						treenode cptr = chlink;

						while (cptr.parentlink != null) {
							// System.out.println(cptr.item);

							path.add(cptr.item);

							LocalF1[cptr.item] = LocalF1[cptr.item]
									+ chlink.twu;
							LocalCount[cptr.item] = LocalCount[cptr.item]
									+ chlink.count;

							cptr = cptr.parentlink;

						}// End while

						// Put path into Conditional pattern base

						path.remove((int) 0);

						CPB.add(path);
						CPBW.add(chlink.twu);
						CPBC.add(chlink.count);

						// turn to next horizontal link
						chlink = chlink.hlink;

					}// End while

					
					// tạo localflist
					ArrayList<Integer> localflist = new ArrayList<Integer>(0);

					for (int j = 0; j < LocalF1.length; j++) {
						if (LocalF1[j] < globalMinUtil) {
							LocalF1[j] = -1;
						} else {
							if (j != citem) {
								InsertItem(localflist, j, LocalF1);

								

								String UTI = Nprefix + " " + j;
								String TempItem[] = UTI.split(" ");
								int SumMau = 0;
								int SumMiu = 0;

								for (int u = 0; u < TempItem.length; u++) {
									SumMau += arrayMAU[Integer
											.parseInt(TempItem[u])];
									SumMiu += arrayMIU[Integer
											.parseInt(TempItem[u])];
								}

								int MAU = SumMau * LocalCount[j];

								if (MAU >= globalMinUtil) {
									int MIU = SumMiu * LocalCount[j];

									// bfw_UCI.write(Nprefix +
									// " "+j+","+LocalF1[j]+","+MIU);
									bfw_UCI.write(Nprefix + " " + j + ":"
											+ LocalF1[j]);

									bfw_UCI.newLine();

									

									if (MIU > globalMinUtil) {
										// raise minimum utility

										// System.out.println(gNum_CI+" ***Put*** "+
										// Nprefix +
										// " "+j+","+LocalF1[j]+","+MIU);

										UpdateNodeCountHeap(ISNodeCountHeap,
												MIU);

									}// End if
								}// End if

								

							}

						}// End if-else
					}// End for-j
						// local infrequent items are filtering

			
					if (CPB.size() == 0) {
						// nothing
					} else {

						FPtree C_FPtree = new FPtree();


						for (int k = 0; k < CPB.size(); k++) {
							ArrayList<Integer> ltran = CPB.get(k);

							int Sum_MinBNF = 0;

							// System.out.println(ltran+":"+CPBC.get(k));

							// delete infrequent node
							int tran[] = new int[ltran.size()];
							int tranlen = 0;

							for (int h = 0; h < ltran.size(); h++) {
								if (LocalF1[ltran.get(h)] >= globalMinUtil) {
									Sum_MinBNF = Sum_MinBNF + CPBC.get(k)
											* arrayMIU[ltran.get(h)];

									tran[tranlen++] = ltran.get(h);
								} else {
									int sum = CPBW.get(k);
									sum = sum - CPBC.get(k)
											* arrayMIU[ltran.get(h)];

									CPBW.set(k, sum);
								}
							}// End for

							sorttrans(tran, 0, tranlen, LocalF1);

							

							C_FPtree.insPatternBase(tran, tranlen, LocalF1,
									CPBW.get(k), CPBC.get(k), Sum_MinBNF);

						}// End for

						

						C_FPtree.UPGrowth_MinBNF(C_FPtree, localflist, Nprefix,
								bfw_UCI, ISNodeCountHeap, LocalF1);

					}// End if-else
				}// End if(LP1[flist2.get(i)] >= gMin_Util)
			}// End for-i

			MemoryLogger.getInstance().checkMemory();

			bfw_UCI.flush();

		}// end FPGrowth()

		// ************************************************************************************************//

		public void traverse_tree(treenode cNode, int level) {
			level++;

			if (cNode != null) {

				

				for (int i = 0; i < cNode.childlink.size(); i++) {
					// if(i==0) System.out.print("(");

					traverse_tree(cNode.childlink.get(i), level);

					// if(i==cNode.childlink.size()-1) System.out.print(")");

				}// End for-i
			}// End if
		}// End traverse_tree()

		public void SumDescendent(treenode cNode, int[] DS_Sum_Table) {
			if (cNode != null) {

				// System.out.print("["+cNode.item+"]" + ":" + cNode.count+
				// ",");
				/*--*/// System.out.print("["+level+"]:"+cNode.item + " : " +
							// cNode.count+ ",");

				DS_Sum_Table[cNode.item] += cNode.count;

				for (int i = 0; i < cNode.childlink.size(); i++) {
					// if(i==0) System.out.print("(");

					SumDescendent(cNode.childlink.get(i), DS_Sum_Table);

					// if(i==cNode.childlink.size()-1) System.out.print(")");

				}// End for-i
			}// End if

		}// End SumDescendent

	}// End class

}// End TKUAlgorithm