package com.hussein.notesapp.listeners;

import com.hussein.notesapp.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note,int position);
}
