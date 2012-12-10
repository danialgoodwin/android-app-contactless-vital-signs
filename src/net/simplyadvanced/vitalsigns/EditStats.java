package net.simplyadvanced.vitalsigns;

import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class EditStats extends Activity implements OnItemSelectedListener {
	EditText mEditTextAge, editTextSex, editTextWeight, editTextHeight;
	Spinner mSpinnerPosition;
    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_stats);
        
        mEditTextAge = (EditText) findViewById(R.id.editTextAge); // Connects variables here to ids in xml
        editTextSex = (EditText) findViewById(R.id.editTextSex);
        editTextWeight = (EditText) findViewById(R.id.editTextWeight);
        editTextHeight = (EditText) findViewById(R.id.editTextHeight);
        mSpinnerPosition = (Spinner) findViewById(R.id.spinnerPosition);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.spinnerPositionsArrray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // Specifies the layout to use when the list of choices appears
        mSpinnerPosition.setAdapter(adapter); // Applies the adapter to the spinner
        mSpinnerPosition.setOnItemSelectedListener(this);
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0); // Load saved stats
        mEditTextAge.setText(settings.getInt("age", 23) + "");
        editTextSex.setText(settings.getString("sex", "Male"));
        editTextWeight.setText(settings.getInt("weight", 160) + "");
        editTextHeight.setText(settings.getInt("height", 75) + "");
        
        if (settings.getString("position", "Sitting").contentEquals("Sitting")) {
            mSpinnerPosition.setSelection(0);
        } else if (settings.getString("position", "Sitting").contentEquals("Laying Down")) {
            mSpinnerPosition.setSelection(1);
        } else if (settings.getString("position", "Sitting").contentEquals("Standing")) {
            mSpinnerPosition.setSelection(2);
        } else {
            mSpinnerPosition.setSelection(0);
        }
    }
    
    public void Save(View v) {
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putInt("age", Integer.parseInt(mEditTextAge.getText().toString()));
    	editor.putString("sex", editTextSex.getText().toString());
    	editor.putInt("weight", Integer.parseInt(editTextWeight.getText().toString()));
    	editor.putInt("height", Integer.parseInt(editTextHeight.getText().toString()));
    	//editor.putString("position", editTextPosition.getText().toString()); // Done in onItemSelected()
    	editor.commit(); // This line saves the edits

    	finish(); // Navigate back on stack
	}
    public void Cancel(View v) {
    	finish(); // Navigate back on stack
	}
    
    
    
    
    
    
    
    
    
    
    /** These two callback methods required for AdapterView.OnItemSelectedListener */
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using parent.getItemAtPosition(pos)
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putString("position", parent.getItemAtPosition(pos).toString());
    	editor.commit(); // This line saves the edits
    }
    public void onNothingSelected(AdapterView<?> parent) {
    	// Do nothing
        // Another interface callback
    }
    
    
    
    
    
    
    
    
    
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_edit_stats, menu);
        return true;
    }

}
