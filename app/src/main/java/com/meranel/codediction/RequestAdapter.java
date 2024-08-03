package com.meranel.codediction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class RequestAdapter extends ArrayAdapter<String> {

    private List<String> requestList;
    private PendingActivity pendingActivity;

    public RequestAdapter(@NonNull Context context, List<String> requestList, PendingActivity pendingActivity) {
        super(context, 0, requestList);
        this.requestList = requestList;
        this.pendingActivity = pendingActivity;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.request_item, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.request_text);
        textView.setText(getItem(position));

        Button acceptButton = convertView.findViewById(R.id.accept_button);
        Button declineButton = convertView.findViewById(R.id.decline_button);

        acceptButton.setOnClickListener(v -> pendingActivity.onAcceptClick(v, position));
        declineButton.setOnClickListener(v -> pendingActivity.onDeclineClick(v, position));

        return convertView;
    }
}
