package sero583.mcbedrockquery;

import java.util.HashMap;
import java.util.Map;

public class QueryResult {
	//TODO: Implement
	private Map<String, String> data = new HashMap<String, String>();
	
	/*public QueryResult() {
		
	}*/
	
	public void addData(String key, String value) {
		this.data.put(key, value);
	}
	
	public String getDataAsString() {
		String data = "";
		int i = 0;
		int max = this.data.size();
		for(Map.Entry<String, String> entry : this.data.entrySet()) {
			data += entry.getKey() + ": " + entry.getValue();
			
			if(i<max) {
				data += "\n";
			}
			i++;
		}
		return data.equals("") == true ? "No data found." : data;
	}
	
	public Map<String, String> getData() {
		return this.data;
	}
}
