/*
 BlueFlyVario flight instrument - http://www.alistairdickie.com/blueflyvario/
 Copyright (C) 2011-2012 Alistair Dickie

 BlueFlyVario is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 BlueFlyVario is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with BlueFlyVario.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bfv;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.bfv.view.component.BFVViewComponent;

import java.io.File;
import java.util.ArrayList;


/**
 * This Activity appears as a dialog.
 */
public class FileChooserListActivity extends Activity {

    public static String fileExt;
    public static String title;
    public static File dir;


    // Member fields

    private ArrayAdapter<String> fileParameterArrayAdapter;
    private BFVViewComponent selectedFile;
    private ArrayList<File> selectableFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.file_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to quit
        Button cancelButton = (Button) findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK);
                finish();
            }
        });


        // Initialize array adapters.
        fileParameterArrayAdapter = new ArrayAdapter<String>(this, R.layout.file);


        ListView pairedListView = (ListView) findViewById(R.id.file_list);
        pairedListView.setAdapter(fileParameterArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);


        //Find the name
        TextView textView = (TextView) findViewById(R.id.title_file_list);

        textView.setText(title);

        selectableFiles = new ArrayList<File>();

        if (dir.canRead()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                Uri selectedUri = Uri.fromFile(file);
                String fileExtension
                        = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
                if (fileExtension.equalsIgnoreCase(fileExt)) {
                    fileParameterArrayAdapter.add(file.getName());
                    selectableFiles.add(file);
                }
            }
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


    }


    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {

            File selectedFile = selectableFiles.get(position);


            // Create the result Intent and include the file name
            Intent intent = new Intent();
            if (selectedFile != null) {
                intent.putExtra("File", selectedFile.getAbsolutePath());
            } else {
                intent.putExtra("File", "");
            }


            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };


}
