package de.openthesaurus.util;

import java.util.ArrayList;
import java.util.List;

public class SearchWordCache {

	private List<String> storage;

	public SearchWordCache() {
		this.storage = new ArrayList<String>();
	}

	public void addSearchWord(String searchWord) {
		if(searchWord != null){
			storage.add(searchWord);
		}
	}

	public String getLastSearchWord() {

		int lIndex = storage.size();

		if (lIndex == 0) {
			return null;
		}

		lIndex -= 1;

		return storage.remove(lIndex);
	}

	public void removeLastSearchWord() {
		int lIndex = storage.size();

		if (lIndex != 0) {
			lIndex -= 1;
			storage.remove(lIndex);
		}
	}

}
