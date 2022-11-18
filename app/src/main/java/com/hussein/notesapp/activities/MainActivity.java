package com.hussein.notesapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.hussein.notesapp.R;
import com.hussein.notesapp.adapters.NotesAdapter;
import com.hussein.notesapp.database.NotesDatabase;
import com.hussein.notesapp.entities.Note;
import com.hussein.notesapp.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {

    //This request code is used to add a new note
    public static final int REQUEST_CODE_ADD_NOTE = 1;
    //This request code is used to update note
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    //This request code is used to display all notes
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    //for quick action(imageAddNote)
    public static final int REQUEST_CODE_SELECT_IMAGE = 4;
    //for quick action(imageAddNote)
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 5;

    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private int noteCLickedPosition = 1;

    private AlertDialog dialogAddURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);

        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(getApplicationContext(), CreateNoteActivity.class), REQUEST_CODE_ADD_NOTE
                );
            }
        });

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);

        /*
        This getNotes() method is called from onCreate() method of an activity.
        It means the application is just started and we need to display all notes
        from database and that's why we area passing REQUEST_CODE_SHOW_NOTES to that method.
         */
        /*
        Here,request code is REQUEST_CODE_SHOW_NOTES, it means we are displaying all
        notes from the database and therefore as a parameter isNoteDeleted we are passing 'false'
         */
        getNotes(REQUEST_CODE_SHOW_NOTES, false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (noteList.size() != 0) {
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });

        /*
        Here, we have a new note added from the home screen.
         */
        findViewById(R.id.imageAddNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(getApplicationContext(), CreateNoteActivity.class), REQUEST_CODE_ADD_NOTE
                );
            }
        });

        /*
        Here, we have a new note with the image added from the home screen.
         */
        findViewById(R.id.imageAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
                } else {
                    selectImage();
                }
            }
        });

        /*
        Here, we have a new note with URL added from the home screen
         */
        findViewById(R.id.imageAddWebLink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddURLDialog();
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteCLickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewORUpdate", true);
        intent.putExtra("Note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);

    }

    //Just as you need an async task to save a note, you will also need it to get notes from database
    private void getNotes(final int requestCode, final boolean isNoteDeleted) {

        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {


            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase.getDatabase(getApplicationContext()).noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                /*
                We checked if the note list is empty it means the app is just started since we have declared it
                as a global variable,in this case, we are adding all notes from the database to this note list and
                notify the adapter about the new dataset. In another case, if the note list is note empty then
                it means notes are already loaded from the database so we are just adding only latest note to the note list
                and notify adapter about new note inserted. And last we scrolled out recycler view to the top
                 */
                /*if (noteList.size() == 0) {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else {
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                }

                notesRecyclerView.smoothScrollToPosition(0);
*/
                /*
                Here, request code is REQUEST_CODE_SHOW_NOTES so we are adding all notes form database
                to noteList and notify adapter about the new data set
                 */
                if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                }
                /*
                Here, request code is REQUEST_CODE_ADD_NOTE, so we are adding an only first note(newly added note)
                form the database to noteList and notify the adapter for the newly inserted item and scrolling
                recycler view to the top.
                 */
                else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    notesRecyclerView.smoothScrollToPosition(0);
                }
                /*
                Here, request code is REQUEST_CODE_UPDATE_NOTE, so we are removing note from the clicked
                position and adding the latest updated note from same position from the database and notify
                the adapter for item changed at the position.
                 */
                /*
                if request code is REQUEST_CODE_UPDATE_NOTE, first, we remove the note from list.
                Then we checked whether the note is deleted or not, if the note is deleted then
                notifying adapter about item removed. if the note is not deleted then it must be
                updated that's why we are adding a newly updated note to that same position where
                we removed and notifying adapter about item changed
                 */
                else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    noteList.remove(noteCLickedPosition);

                    if (isNoteDeleted) {
                        notesAdapter.notifyItemRemoved(noteCLickedPosition);
                    } else {
                        noteList.add(noteCLickedPosition, notes.get(noteCLickedPosition));
                        notesAdapter.notifyItemChanged(noteCLickedPosition);
                    }
                }


            }
        }

        new GetNotesTask().execute();

    }

    /*
        Since we have started "CreateNoteActivity" for the result, we need to handle the result in
        "onActivityResult" method to update the note list after adding a note form "CreateNoteActivity"
        so called onActivityResult to update the list after return
    */

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //REQUEST_CODE_ADD_NOTE is name for the activity & RESULT_OK is a name for the type of return can be many resultCode for on activity
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {

            /*
            This getNotes() methode is called form onActivityResult() method of activity and we checked
            the current request code is for add note and the result is RESULT_OK. It means a new note
            is added from CreateNote activity and its result is sent back to this activity that's why we
            are passing REQUEST_CODE_ADD_NOTE to that method
             */
            /*
            Here, request code is REQUEST_CODE_ADD_NOTE, it means we have added a new note to the database
            , and therefore as parameter isNoteDeleted, we ara passing 'false'
             */
            getNotes(REQUEST_CODE_ADD_NOTE, false);

        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
            /*
            This getNotes() methode is called form onActivityResult() method of activity and we checked
            the current request code is for update note and the result is RESULT_OK. It means already
            available note is updated from CreateNote activity and its result is sent back to this activity
            that's why we are passing REQUEST_CODE_UPDATE_NOTE to that method.
            */
            /*
            Here, request code is  REQUEST_CODE_UPDATE_NOTE, it means we are updating already available
            note from the database, and it may be possible that note gets deleted therefore as parameter
            isNoteDeleted, we are passing value from CreateNoteActivity, whether the note is deleted
            or not using intent data with key 'isNoteDelete'
            */
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDelete", false));
            }
        } else if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectImageUri = data.getData();
                if (selectImageUri != null) {
                    try {
                        String selectImagePath = getPathFromUri(selectImageUri);
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selectImagePath);
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    } catch (Exception exception) {
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_add_url, (ViewGroup) findViewById(R.id.layoutAddUrlContainer));
            builder.setView(view);

            dialogAddURL = builder.create();
            if (dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (inputURL.getText().toString().trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                        Toast.makeText(MainActivity.this, "Enter valid URL", Toast.LENGTH_SHORT).show();
                    } else {
                        dialogAddURL.dismiss();
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "URL");
                        intent.putExtra("URL", inputURL.getText().toString());
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogAddURL.dismiss();
                }
            });
        }
        dialogAddURL.show();
    }

}
