package TOP_K.Algo.TKO;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import TOP_K.Algo.hui_miner.Element;
import TOP_K.Algo.hui_miner.UtilityList;
import TOP_K.Algo.tools.MemoryLogger;


public class AlgoTKO_Basic {

	/** thời gian thuật toán kết thúc */
	long totalTime = 0; 
	
	/** số lượng HUI được tạo ra  */
	int huiCount = 0; 

	/** tham số k */
	int k = 0;
	
	/** giá trị min_util*/
	long minutility = 0; 

	PriorityQueue<ItemsetTKO> kItemsets; 

	/** tạo một map để lưu trữ TWU của từng mặt hàng */
	final Map<Integer, Integer> mapItemToTWU = new HashMap<Integer, Integer>();

	/** lớp này đại diện cho một item và độ hữu ích của nó trong một giao dịch */
	class Pair {
		/** an item */
		int item = 0;
		
		/** độ hữu ích của item */
		int utility = 0;
	}

	/** 
	 * Constructor
	 */
	public AlgoTKO_Basic() {

	}

	
	public void runAlgorithm(String input, String output, int k)
			throws IOException {
		MemoryLogger.getInstance().reset();
		long startTimestamp = System.currentTimeMillis();
		this.minutility = 1;
		this.k = k;

		this.kItemsets = new PriorityQueue<ItemsetTKO>();

		// quét cơ sở dữ liệu lần đầu tiên để tính toán TWU của từng mặt hàng.
		BufferedReader myInput = null;
		String thisLine;
		try {
			FileInputStream fin = new FileInputStream(new File(input));
			myInput = new BufferedReader(new InputStreamReader(fin));
			// for each line (transaction)
			while ((thisLine = myInput.readLine()) != null) {
				
				if (thisLine.isEmpty() == true ||	thisLine.charAt(0) == '#' 
						|| thisLine.charAt(0) == '%'
					|| thisLine.charAt(0) == '@') {
					continue;
				}
				
				String split[] = thisLine.split(":");
				// danh sách các mục
				String items[] = split[0].split(" ");
				// độ hữu ích giao dịch
				int transactionUtility = Integer.parseInt(split[1]);
				// đối với mỗi mặt hàng, thêm độ hữu ích giao dịch vào TWU của nó
				for (int i = 0; i < items.length; i++) {
					Integer item = Integer.parseInt(items[i]);
					// lấy giá trị TWU hiện tại
					Integer twu = mapItemToTWU.get(item);
					// cập nhật twu
					twu = (twu == null) ? transactionUtility : twu
							+ transactionUtility;
					mapItemToTWU.put(item, twu);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myInput != null) {
				myInput.close();
			}
		}

		// TẠO list ĐỂ LƯU TRỮ CÁC tập có TWU >= MIN_UTILITY.
		List<UtilityList> listItems = new ArrayList<UtilityList>();

		// tạo một MAP để lưu trữ danh sách hữu ích cho mỗi item.
		Map<Integer, UtilityList> mapItemToUtilityList = new HashMap<Integer, UtilityList>(
				10000);
		// Với mỗi item
		for (Integer item : mapItemToTWU.keySet()) {
			UtilityList uList = new UtilityList(item);
			// thêm item vào danh sách các item có TWU cao
			listItems.add(uList);
			// tạo một Danh sách tiện ích trống
			mapItemToUtilityList.put(item, uList);
		}
		// SẮP XẾP DANH SÁCH CÁC item TWU CAO THEO THỨ TỰ TĂNG CƯỜNG
		Collections.sort(listItems, new Comparator<UtilityList>() {
			public int compare(UtilityList o1, UtilityList o2) {
				int compare = mapItemToTWU.get(o1.item)
						- mapItemToTWU.get(o2.item);
				if (compare == 0) {
					
					return o1.item - o2.item;
				}
				return compare;
			}
		});

		// QUÉT CƠ SỞ DỮ LIỆU THỨ HAI ĐỂ XÂY DỰNG DANH SÁCH TIỆN ÍCH
		// của 1-ITEMSETS CÓ TWU >= minutil (các item tiềm năng)
		try {
			myInput = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(input))));
			int tid = 0;
			// for each line (transaction)
			while ((thisLine = myInput.readLine()) != null) {
				// if the line is  a comment, is  empty or is a
				// kind of metadata
				if (thisLine.isEmpty() == true ||	thisLine.charAt(0) == '#' 
						|| thisLine.charAt(0) == '%'
					|| thisLine.charAt(0) == '@') {
					continue;
				}
				
				
				String split[] = thisLine.split(":");
				// danh sách item
				String items[] = split[0].split(" ");
				// danh sách giá trị độ hữu ích
				String utilityValues[] = split[2].split(" ");

				int remainingUtility = 0;

				// tạo danh sách để lưu trữ item
				List<Pair> revisedTransaction = new ArrayList<Pair>();
				// for each item
				for (int i = 0; i < items.length; i++) {
					// / chuyển đổi giá trị thành số nguyên
					Pair pair = new Pair();
					pair.item = Integer.parseInt(items[i]);
					pair.utility = Integer.parseInt(utilityValues[i]);
					revisedTransaction.add(pair);
					remainingUtility += pair.utility;
				}

				Collections.sort(revisedTransaction, new Comparator<Pair>() {
					public int compare(Pair o1, Pair o2) {
						return compareItems(o1.item, o2.item);
					}
				});

				// for each item left in the transaction
				for (Pair pair : revisedTransaction) {

					// trừ tiện ích của item này khỏi phần còn lại
					// tiện ích
					remainingUtility = remainingUtility - pair.utility;

					// lấy danh sách tiện ích của item
					UtilityList utilityListOfItem = mapItemToUtilityList
							.get(pair.item);

					// Thêm một phần tử mới vào danh sách hữu ích của item này
					// tương ứng với giao dịch này
					Element element = new Element(tid, pair.utility,
							remainingUtility);

					utilityListOfItem.addElement(element);
				}
				tid++; // tăng số tid cho giao dịch tiếp theo
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myInput != null) {
				myInput.close();
			}
		}
		

		// kiểm tra bộ nhớ
		MemoryLogger.getInstance().checkMemory();

		// Khai thác cơ sở dữ liệu theo cách đệ quy
		search(new int[0], null, listItems);

		// kiểm tra lại mức sử dụng bộ nhớ và đóng tệp.

		MemoryLogger.getInstance().checkMemory();
		totalTime = (System.currentTimeMillis() - startTimestamp) / 1000;
	}

	/**
	 * Đây là phương pháp đệ quy để tìm tất cả các tập mục tiện ích cao.  
	 * Nó ghi các tập mục vào tệp đầu ra.
	 * 
	 * @param prefix
	 *            Đây là prefix hiện tại.
	 * @param pUL
	 *            Đây là danh sách tiện ích của prefix.
	 * @param ULs
	 *            Tiện ích liệt kê tương ứng với mỗi phần mở rộng của tiền tố.
	 * @param minUtility
	 *            The minUtility threshold.
	 * @throws IOException
	 */
	private void search(int[] prefix, UtilityList pUL, List<UtilityList> ULs)
			throws IOException {
		MemoryLogger.getInstance().checkMemory();

		// For each extension X of prefix P
		for (int i = 0; i < ULs.size(); i++) {
			UtilityList X = ULs.get(i);

			// If pX is a high utility itemset.
			// we save the itemset: pX
			if (X.sumIutils >= minutility) {
				writeOut(prefix, X.item, X.sumIutils);
			}

			// Nếu tổng các tiện ích còn lại cho pX 
			// cao hơn minUtility, chúng ta khám phá các phần mở rộng của pX
			if (X.sumRutils + X.sumIutils >= minutility) {
				// Danh sách này sẽ chứa các danh sách tiện ích của phần mở rộng pX.
				List<UtilityList> exULs = new ArrayList<UtilityList>();
				// Với mỗi phần mở rộng của p xuất hiện
				// sau X theo thứ tự tăng dần
				for (int j = i + 1; j < ULs.size(); j++) {
					UtilityList Y = ULs.get(j);
					// chúng ta xây dựng phần mở rộng pXY
					// và thêm nó vào danh sách các phần mở rộng của pX
					exULs.add(construct(pUL, X, Y));
				}
				// Chúng tôi tạo tiền tố (prefix) mới pX
				int[] newPrefix = new int[prefix.length + 1];
				System.arraycopy(prefix, 0, newPrefix, 0, prefix.length);
				newPrefix[prefix.length] = X.item;

				// Thực hiện một đệ quy để khám phá tất cả các tập mục với
				// prefix pX
				search(newPrefix, X, exULs);
			}

		}
	}

	/**
	 * Phương pháp ghi tập hữu ích cao vào tệp đầu ra.
	 * @param a prefix itemset
	 * @param an item to be appended to the prefix
	 * @param utility the utility of the prefix concatenated with the item
	 */
	private void writeOut(int[] prefix, int item, long utility) {
		ItemsetTKO itemset = new ItemsetTKO(prefix, item, utility);
		kItemsets.add(itemset);
		if (kItemsets.size() > k) {
			if (utility > this.minutility) {
				ItemsetTKO lower;
				do {
					lower = kItemsets.peek();
					if (lower == null) {
						break; // / IMPORTANT
					}
					kItemsets.remove(lower);
				} while (kItemsets.size() > k);
				this.minutility = kItemsets.peek().utility;
//				System.out.println(this.minutility);
			}
		}
	}

	/**
	 * Phương thức này xây dựng danh sách tiện ích của pXY
	 * @param P :  the utility list of prefix P.
	 * @param px : the utility list of pX
	 * @param py : the utility list of pY
	 * @return the utility list of pXY
	 */
	private UtilityList construct(UtilityList P, UtilityList px, UtilityList py) {
		// tạo một danh sách tiện ích trống cho pXY
		UtilityList pxyUL = new UtilityList(py.item);
		// cho từng phần tử trong danh sách tiện ích của pX
		for(Element ex : px.elements){
			// thực hiện tìm kiếm nhị phân để tìm phần tử ey trong py với tid = ex.tid
			Element ey = findElementWithTID(py, ex.tid);
			if(ey == null){
				continue;
			}
			// nếu tiền tố p là null
			if(P == null){
				// Tạo phần tử mới
				Element eXY = new Element(ex.tid, ex.iutils + ey.iutils, ey.rutils);
				// thêm phần tử mới vào danh sách tiện ích của pXY
				pxyUL.addElement(eXY);
				
			}else{
				// tìm phần tử trong danh sách tiện ích của p có cùng tid
				Element e = findElementWithTID(P, ex.tid);
				if(e != null){
					// Tạo phần tử mới
					Element eXY = new Element(ex.tid, ex.iutils + ey.iutils - e.iutils,
								ey.rutils);
					// thêm phần tử mới vào danh sách tiện ích của pXY
					pxyUL.addElement(eXY);
				}
			}	
		}
		// trả về danh sách tiện ích của pXY.
		return pxyUL;
	}
	
	/**
	 * Thực hiện tìm kiếm nhị phân để tìm phần tử có tid nhất định trong danh sách tiện ích
	 * @param ulist the utility list
	 * @param tid  the tid
	 * @return  the element or null if none has the tid.
	 */
	private Element findElementWithTID(UtilityList ulist, int tid){
		List<Element> list = ulist.elements;
		
		// thực hiện tìm kiếm nhị phân để kiểm tra xem tập con có xuất hiện ở mức k-1 hay không.
        int first = 0;
        int last = list.size() - 1;
       
        // the binary search
        while( first <= last )
        {
        	int middle = ( first + last ) >>> 1; // divide by 2

            if(list.get(middle).tid < tid){
            	first = middle + 1;  //  the itemset compared is larger than the subset according to the lexical order
            }
            else if(list.get(middle).tid > tid){
            	last = middle - 1; //  the itemset compared is smaller than the subset  is smaller according to the lexical order
            }
            else{
            	return list.get(middle);
            }
        }
		return null;
	}

	
	public void writeResultTofile(String path) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(path));
		Iterator<ItemsetTKO> iter = kItemsets.iterator();
		while (iter.hasNext()) {
			StringBuffer buffer = new StringBuffer();
			ItemsetTKO itemset = (ItemsetTKO) iter.next();
			
			// thêm tiền tố
			for (int i = 0; i < itemset.getItemset().length; i++) {
				buffer.append(itemset.getItemset()[i]);
				buffer.append(' ');
			}
			buffer.append(itemset.item);
			
			// thêm giá trị hữu ích
			buffer.append(" #UTILITY: ");
			buffer.append(itemset.utility);
			
			// ghi file
			writer.write(buffer.toString());
			if(iter.hasNext()){
				writer.newLine();
			}
		}
		writer.close();
	}


	private int compareItems(int item1, int item2) {
		int compare = mapItemToTWU.get(item1) - mapItemToTWU.get(item2);
		return (compare == 0) ? item1 - item2 : compare;
	}

	public void printStats() {
		System.out
				.println("THUC NGHIEM THUAT TOAN TKO");
		System.out
				.println(" SO TAP HUU ICH : " + kItemsets.size());
		System.out.println(" TONG THOI GIAN THUC THI: " + totalTime
				+ " s");
		System.out.println(" TIEU THU BO NHO: " + MemoryLogger.getInstance().getMaxMemory() + " MB");
		System.out.println("--------------------------");
	}
}