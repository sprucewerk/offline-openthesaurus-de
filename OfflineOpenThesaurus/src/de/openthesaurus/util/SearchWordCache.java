/*
 Offline OpenThesaurus

 Copyright (C) 2011 Vitali Fichtner

 This program is free software; you can redistribute it and/or modify it 
 under the terms of the GNU General Public License as published by the 
 Free Software Foundation; either version 3 of the License, or (at your 
 option) any later version.
 
 This program is distributed in the hope that it will be useful, but 
 WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 more details.

 You should have received a copy of the GNU General Public License along 
 with this program; if not, see <http://www.gnu.org/licenses/>.

 */
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
	
	public void clear(){
		storage.clear();
	}
	

	public void removeLastSearchWord() {
		int lIndex = storage.size();

		if (lIndex != 0) {
			lIndex -= 1;
			storage.remove(lIndex);
		}
	}

}
