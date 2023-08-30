package com.example.textmaker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ListViewHolder>
{
    private final List<String> list;
    private final Context context;
    private final onNoteClickListener mClickListener;

    public ListAdapter(Context context, List<String> list, onNoteClickListener clickListener)
    {
        this.context = context;
        this.list = list;
        this.mClickListener = clickListener;
    }

    public void itemsRemoved()
    {
        int size = getItemCount();
        MainAppData.dataList.clear();
        list.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return new ListViewHolder(view, mClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewHolder holder, int position) {
        holder.text.setText(MainAppData.firstStringSymbolsOutput(list.get(position)));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView text;

        public ListViewHolder(@NonNull View itemView, onNoteClickListener onClickListener)
        {
            super(itemView);
            text = itemView.findViewById(R.id.text);
            itemView.setOnClickListener(view -> onClickListener.onNoteClick(getAdapterPosition()));
        }
    }

    public interface onNoteClickListener {
        void onNoteClick(int position);
    }
}