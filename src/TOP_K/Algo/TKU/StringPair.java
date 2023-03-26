package TOP_K.Algo.TKU;


class StringPair implements Comparable<StringPair>{

	public String x;
	public int y;
    public StringPair(String x, int y) {
    	this.x = x;
    	this.y = y;
    }
    
    public int compareTo(StringPair o){
    	return y - o.y;
    }
}