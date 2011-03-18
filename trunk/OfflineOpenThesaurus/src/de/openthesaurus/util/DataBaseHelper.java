package de.openthesaurus.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class provides the opportunity to use your own sqlite database.
 * It copies the database from the assets folder into the app system database.
 * 
 * There is a size limitation for the files in the assets folder. If a file
 * is bigger than 1 MB, it will be compressed. The compression is a very
 * expensive operation because the whole file would be loaded into the memory.
 * To avoid the compression I renamed the file extension into an already 
 * compressed file format 'PNG'. If you have a better solution, please email me.
 * 
 * This class is inspired by the ReignDesign Group, you can find the reference
 * implementation on http://www.reigndesign.com/blog/using-your-own-sqlite
 * -database-in-android-applications/ posted by Juan-Manuel Fluxa.
 * 
 * 
 * @author v.fichtner
 *
 */
public class DataBaseHelper extends SQLiteOpenHelper {

	private static String DB_PATH = "/data/data/de.openthesaurus/databases/";
	
	/**
	 * Magic Hack: the extension of the database
	 */
	private static String DB_NAME = "openthesaurus.png";

	private SQLiteDatabase sqliteDatabase;
	private final Context myContext;
	
	private Cursor termCursor;
	

	/**
	 * Constructor Takes and keeps a reference of the passed context in order to
	 * access to the application assets and resources.
	 * 
	 * @param context
	 */
	public DataBaseHelper(Context context) {

		super(context, DB_NAME, null, 1);
		this.myContext = context;
	}

	/**
	 * Creates a empty database on the system and rewrites it with your own
	 * database.
	 * */
	public void createDataBase() throws IOException {

		boolean dbExist = checkDataBase();

		if (!dbExist) {
			
			// By calling this method and empty database will be created into
			// the default system path
			// of your application so we are gonna be able to overwrite that
			// database with our database.
			this.getReadableDatabase();

			try {

				copyDataBase();

			} catch (IOException e) {

				throw new Error("Error copying database");
			}
		}
	}

	/**
	 * Check if the database already exist to avoid re-copying the file each
	 * time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBase() {

		SQLiteDatabase checkDB = null;

		try {
			String myPath = DB_PATH + DB_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null,
					SQLiteDatabase.OPEN_READONLY);

		} catch (SQLiteException e) {

			// database does't exist yet.

		}

		if (checkDB != null) {

			checkDB.close();

		}

		return checkDB != null ? true : false;
	}

	/**
	 * Copies your database from your local assets-folder to the just created
	 * empty database in the system folder, from where it can be accessed and
	 * handled. This is done by transfering bytestream.
	 * */
	private void copyDataBase() throws IOException {

		// Open your local db as the input stream
		InputStream myInput = myContext.getAssets().open(DB_NAME);

		// Path to the just created empty db
		String outFileName = DB_PATH + DB_NAME;

		// Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);

		// transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();

	}

	public void openDataBase() throws SQLException {

		// Open the database
		String myPath = DB_PATH + DB_NAME;
		sqliteDatabase = SQLiteDatabase.openDatabase(myPath, null,
				SQLiteDatabase.OPEN_READONLY);

	}

	@Override
	public synchronized void close() {

		if (sqliteDatabase != null)
			sqliteDatabase.close();

		super.close();

	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	
	
	public Cursor getSynonymCursor(String txt){
		Cursor retCursor = null;
		
		if(sqliteDatabase != null){
			
			final String query ="SELECT DISTINCT t2._id, t2.word, tl.short_level_name, " +
					"c.category_name FROM term t" 
			+" LEFT JOIN synset s ON t.synset_id =s._id"
			+" LEFT JOIN term t2 ON t2.synset_id = s._id"
			+" LEFT JOIN category_link cl ON t2.synset_id = cl.synset_id"
			+" LEFT JOIN category c ON c._id = cl.category_id"
			+" LEFT JOIN term_level tl ON t2.level_id = tl.id"
			+" WHERE t.word like ?"
			+" GROUP BY t2.word";
			
			retCursor = sqliteDatabase.rawQuery(query, new String[]{txt});	

		}
		
		
		return retCursor;
	}
	
	/**
	 * This method contains a constraint for the auto complete function.
	 * @param txt name of the search keyword
	 * @return Cursor
	 */
	public Cursor getAutocompleteCursor(String txt){
		
		Cursor retCursor=null;
		if(sqliteDatabase != null){
			
			String query = "SELECT DISTINCT word,_id FROM term"+ 
			" WHERE word like \""+txt+"%\""+
			" GROUP BY word LIMIT 15;";

			retCursor = sqliteDatabase.rawQuery(query, null);	
		}		
		
		return retCursor;
	}
	
	public Cursor getTermCursor(){
		
		if(termCursor==null && sqliteDatabase != null){
			termCursor =sqliteDatabase.query("term", new String[]{"word", "_id"}, null, null, null, null, null);
		}
		
		return termCursor;
	}
	

}
