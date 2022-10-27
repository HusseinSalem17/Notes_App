package com.hussein.notesapp.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.hussein.notesapp.R;
import com.hussein.notesapp.adapters.NotesAdapter;
import com.hussein.notesapp.database.NotesDatabase;
import com.hussein.notesapp.entities.Note;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_ADD_NOTE = 1;

    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

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
        notesAdapter = new NotesAdapter(noteList);
        notesRecyclerView.setAdapter(notesAdapter);

        getNotes();
    }

    //Just as you need an async task to save a note, you will also need it to get notes from database
    private void getNotes() {

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

                if (noteList.size() == 0) {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else {
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                }

                notesRecyclerView.smoothScrollToPosition(0);

            }
        }

        new GetNotesTask().execute();

    }

    /*
        Since we have started "CreateNoteActivity" for the result, we need to handle the result in
        "onActivityResult" method to update the note list after adding a note form "CreateNoteActivity"
        so called onActivityResult to update the list after return
    */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //REQUEST_CODE_ADD_NOTE is name for the activity & RESULT_OK is a name for the type of return can be many resultCode for on activity
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {

            getNotes();

        }
    }
}
