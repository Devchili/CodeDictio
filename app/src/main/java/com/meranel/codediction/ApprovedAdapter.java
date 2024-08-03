package com.meranel.codediction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class ApprovedAdapter extends ArrayAdapter<String> {
    private Context mContext;
    private ArrayList<String> meaningsList;

    public ApprovedAdapter(@NonNull Context context, ArrayList<String> list) {
        super(context, 0, list);
        mContext = context;
        meaningsList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_approved, parent, false);
        }

        String currentMeaning = meaningsList.get(position);
        String[] parts = currentMeaning.split(": ");
        String word = parts[0];
        String meaning = parts[1].split(" \\(by ")[0];
        String author = parts[1].split(" \\(by ")[1].replace(")", "");

        TextView wordTextView = convertView.findViewById(R.id.word_text);
        TextView meaningTextView = convertView.findViewById(R.id.meaning_text);
        TextView authorTextView = convertView.findViewById(R.id.author_text);

        wordTextView.setText(word);
        meaningTextView.setText(meaning);
        authorTextView.setText(author);

        return convertView;
    }
}
