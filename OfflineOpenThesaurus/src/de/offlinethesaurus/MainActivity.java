/***
 Offline Thesaurus

 Copyright (C) 2011 vfichtn

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
package de.offlinethesaurus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import de.offlinethesaurus.apprater.AppRater;
import de.offlinethesaurus.schema.Word;
import de.offlinethesaurus.util.DataBaseHelper;
import de.offlinethesaurus.util.SearchWordCache;
import de.offlinethesaurus.util.SectionedAdapter;
import de.offlinethesaurus.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

public class MainActivity extends Activity {

	private static final String MAIN_ACTIVITY = "MainActivity";
	private DataBaseHelper dataBaseHelper;
	private Cursor cursor;
	private AutoCompleteCursor autoCompleteCursor;

	private AutoCompleteTextView autoCompleteTextView;
	private ProgressDialog progressDialog;
	private ListView listView;
	// private View searchView;

	private SearchWordCache searchWordCache;
	private String currentSearchWord;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Log.i(MAIN_ACTIVITY, " intent data = " + getIntent().getDataString());
		final String intentDataSearchItem = getIntent().getDataString();

		init();

		progressDialog.show();

		// start database operations in a background thread

		Thread initDbThread = new Thread(new Runnable() {
			public void run() {
				createDatabase(dataBaseHelper);
				openDatabase(dataBaseHelper);

				progressDialog.dismiss();
			}
			

		});

		initDbThread.start();
		initUIElements();
		
		
		if (intentDataSearchItem != null) {
			try {
				initDbThread.join(); // waiting for end of the database init
				
				autoCompleteTextView.setText(intentDataSearchItem);//set the intent data
				autoCompleteCursor.setSearchOn(false);//disable auto complete
				querySynonym(intentDataSearchItem);//perform search
				autoCompleteCursor.setSearchOn(true);//enable auto complete
			
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
		AppRater.app_launched(this); 
		//AppRater.showRateDialog(this,null);
		
	}

	private void init() {
		dataBaseHelper = new DataBaseHelper(this);
		autoCompleteCursor = new AutoCompleteCursor(this,
				dataBaseHelper.getTermCursor(), 0, dataBaseHelper);
		autoCompleteCursor.setSearchOn(true);

		progressDialog = new ProgressDialog(this);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage("Installiere Datenbank...");
		progressDialog.setCancelable(false);

		searchWordCache = new SearchWordCache();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {

			// perform an old search
			String searchWord = searchWordCache.getLastSearchWord();

			if (searchWord != null) {

				((AutoCompleteCursor) autoCompleteTextView.getAdapter())
						.setSearchOn(false);
				autoCompleteTextView.setText(searchWord);

				querySynonym(searchWord);

				((AutoCompleteCursor) autoCompleteTextView.getAdapter())
						.setSearchOn(true);

				searchWordCache.getLastSearchWord();

			} else {
				// show notification that the cache is empty
				Toast toast = Toast.makeText(listView.getContext(),
						"Keine weiteren Suchwörter im Speicher.",
						Toast.LENGTH_SHORT);
				toast.show();
				
			}

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		super.onDestroy();
		clearSearchword();

	}

	private void initUIElements() {

		autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.AutoCompleteTextView01);
		autoCompleteTextView.setThreshold(2);
		autoCompleteTextView.setAdapter(autoCompleteCursor);
		autoCompleteTextView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				String searchItem = ((SQLiteCursor) arg0
						.getItemAtPosition(arg2)).getString(0);

				querySynonym(searchItem);
			}

		});

		// execute search through key pressing
		autoCompleteTextView.setOnKeyListener(new View.OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {

				// perform search
				if (keyCode == KeyEvent.KEYCODE_ENTER
						&& event.getAction() == KeyEvent.ACTION_DOWN) {

					((AutoCompleteTextView) v).dismissDropDown();

					String searchItem = ((AutoCompleteTextView) v).getText()
							.toString();
					querySynonym(searchItem);

					return true;
				}

				return false;
			}
		});

		listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				String item = ((TwoLineListItem) arg1).getText1().getText()
						.toString();

				// set the selected item
				((AutoCompleteCursor) autoCompleteTextView.getAdapter())
						.setSearchOn(false);
				autoCompleteTextView.setText(item);
				querySynonym(item);
				((AutoCompleteCursor) autoCompleteTextView.getAdapter())
						.setSearchOn(true);
			}

		});

		// searchView = findViewById(R.id.LinearLayoutLoad);

	}

	private void openDatabase(DataBaseHelper myDbHelper) {
		try {
			myDbHelper.openDataBase();
		} catch (SQLException sqle) {

			throw sqle;
		}
	}

	private void createDatabase(DataBaseHelper myDbHelper) throws Error {
		try {
			myDbHelper.createDataBase();
		} catch (IOException ioe) {

			// create an alert dialog with the input text
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Exception was thrown! ");
			builder.setCancelable(false);
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog alert = builder.create();

			alert.show();

			throw new Error("Unable to create database");
		}
	}

	/**
	 * This method sends a query to the database.
	 * 
	 * @param item
	 */
	public void querySynonym(final String item) {

		if (item.length() < 2)
			return;

		performSearch(item);

		autoCompleteTextView.selectAll();

		// It is not necessary to shift the search to an
		// extra thread and provide a visual feedback.
		// My tests have shown that the search consumes
		// more time if I provide a visual feedback.

		// searchView.setVisibility(View.VISIBLE);
		//
		// progressDialog = new ProgressDialog(this);
		// progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		// progressDialog.setMessage("Suche Läuft...");
		// progressDialog.setCancelable(false);
		//
		// SearchTask searchTask = new SearchTask();
		// searchTask.setMainActivity(this);
		// searchTask.execute(item);

	}

	private void performSearch(String item) {
		// save the old search word into the cache
		searchWordCache.addSearchWord(currentSearchWord);
		currentSearchWord = item; // current search word

		try {

			cursor = dataBaseHelper.getSynonymCursor(item);
			startManagingCursor(cursor);

			adapter.clearSections();

			HashMap<String, List<Word>> map = new HashMap<String, List<Word>>();

			while (cursor.moveToNext()) {

				String category = cursor.getString(3);

				Word word = new Word();
				word.setWord(cursor.getString(1));
				word.setLevel(cursor.getString(2));

				if (map.containsKey(category)) {
					map.get(category).add(word);
				} else {
					ArrayList<Word> l = new ArrayList<Word>();
					l.add(word);
					map.put(category, l);
				}
			}

			ListView viewList = getListView();

			if (map.size() >= 1) {

				final String[] matrix = { "_id", "word", "level" };
				final String[] columns = { "word", "level" };
				final int[] layouts = { android.R.id.text1, android.R.id.text2 };

				int key = 0;
				for (Map.Entry<String, List<Word>> cItem : map.entrySet()) {

					// skip the empty category
					// it will be stored at the end of the list
					if (cItem.getKey() == null)
						continue;

					MatrixCursor cursor = new MatrixCursor(matrix);

					for (Word wordItem : cItem.getValue()) {
						cursor.addRow(new Object[] { key++, wordItem.getWord(),
								wordItem.getLevel() });
					}

					adapter.addSection(cItem.getKey(), new SimpleCursorAdapter(
							this, R.layout.simple_list_item_2, cursor, columns,
							layouts));
				}

				// store the empty category
				List<Word> lastItem = map.get(null);

				if (lastItem != null) {

					MatrixCursor cursor = new MatrixCursor(matrix);
					for (Word lWord : lastItem) {
						cursor.addRow(new Object[] { key++, lWord.getWord(),
								lWord.getLevel() });
					}

					adapter.addSection("", new SimpleCursorAdapter(this,
							R.layout.simple_list_item_2, cursor, columns,
							layouts));
				}

			} else {

				adapter.addSection("Keine Treffer", new SimpleCursorAdapter(
						this, R.layout.simple_list_item_2, null, null, null));
			}

			viewList.setAdapter(adapter);

		} catch (SQLException sqle) {

			throw sqle;

		}
	}

	private ListView getListView() {

		return (ListView) findViewById(R.id.ListView01);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.item01:

			// create an alert dialog with the input text
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Version: 1.1 \n" + "DB: 2011-02-05 22:30\n\n"
					+ "http://code.google.com/p/offline-openthesaurus-de/");
			builder.setCancelable(false);
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog alert = builder.create();

			alert.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void clearSearchword() {
		this.autoCompleteTextView.setText(null);
		searchWordCache.clear();
		adapter.clearSections();
	}

	SectionedAdapter adapter = new SectionedAdapter() {

		protected View getHeaderView(String caption, int index,
				View convertView, ViewGroup parent) {
			TextView result = (TextView) convertView;

			if (convertView == null) {

				result = (TextView) getLayoutInflater().inflate(
						R.layout.header, null);
			}

			result.setText(caption);

			return (result);
		}
	};

	private class SearchTask extends AsyncTask<String, Void, Void> {

		private Activity mainActivity;

		public void setMainActivity(Activity mainActivity) {
			this.mainActivity = mainActivity;
		}

		@Override
		protected Void doInBackground(String... params) {

			String item = params[0];

			// save the old search word into the cache
			searchWordCache.addSearchWord(currentSearchWord);
			currentSearchWord = item; // current search word

			try {

				cursor = dataBaseHelper.getSynonymCursor(item);
				startManagingCursor(cursor);

			} catch (SQLException sqle) {

				throw sqle;

			}
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {

			adapter.clearSections();

			HashMap<String, List<Word>> map = new HashMap<String, List<Word>>();

			// create
			while (cursor.moveToNext()) {

				String category = cursor.getString(3);

				Word word = new Word();
				word.setWord(cursor.getString(1));
				word.setLevel(cursor.getString(2));

				if (map.containsKey(category)) {
					map.get(category).add(word);
				} else {
					ArrayList<Word> l = new ArrayList<Word>();
					l.add(word);
					map.put(category, l);
				}
			}

			final String[] matrix = { "_id", "word", "level" };
			final String[] columns = { "word", "level" };
			final int[] layouts = { android.R.id.text1, android.R.id.text2 };

			int key = 0;
			for (Map.Entry<String, List<Word>> cItem : map.entrySet()) {

				// skip the empty category
				// it will be stored at the end of the list
				if (cItem.getKey() == null)
					continue;

				MatrixCursor cursor = new MatrixCursor(matrix);

				for (Word wordItem : cItem.getValue()) {
					cursor.addRow(new Object[] { key++, wordItem.getWord(),
							wordItem.getLevel() });
				}

				adapter.addSection(cItem.getKey(), new SimpleCursorAdapter(
						mainActivity, R.layout.simple_list_item_2, cursor,
						columns, layouts));
			}

			// store the empty category
			List<Word> lastItem = map.get(null);

			if (lastItem != null) {

				MatrixCursor cursor = new MatrixCursor(matrix);
				for (Word lWord : lastItem) {
					cursor.addRow(new Object[] { key++, lWord.getWord(),
							lWord.getLevel() });
				}

				adapter.addSection("", new SimpleCursorAdapter(mainActivity,
						R.layout.simple_list_item_2, cursor, columns, layouts));
			}

			// searchView.setVisibility(View.GONE);

			progressDialog.dismiss();

			// set adapter to the list view
			ListView viewList = getListView();
			viewList.setAdapter(adapter);

		}

	}

	/*
     * 
     */
	private class AutoCompleteCursor extends CursorAdapter {
		private int columnIndex;
		private DataBaseHelper myDbHelper;
		private Boolean isSearchOn;

		public AutoCompleteCursor(Context context, Cursor c, int col,
				DataBaseHelper myDbHelper) {
			super(context, c);
			this.columnIndex = col;
			this.myDbHelper = myDbHelper;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final LayoutInflater inflater = LayoutInflater.from(context);

			final TextView view = (TextView) inflater.inflate(
					R.layout.suggest_item, parent, false);

			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((TextView) view).setText(cursor.getString(columnIndex));
		}

		@Override
		public String convertToString(Cursor cursor) {
			String clickedItem = cursor.getString(columnIndex);

			return clickedItem;
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			if (constraint != null && isSearchOn) {
				Cursor cursor = myDbHelper.getAutocompleteCursor(constraint
						.toString());
				startManagingCursor(cursor);
				return cursor;
			} else {
				return null;
			}
		}

		public void setSearchOn(Boolean isSearchOn) {
			this.isSearchOn = isSearchOn;
		}

	}

}