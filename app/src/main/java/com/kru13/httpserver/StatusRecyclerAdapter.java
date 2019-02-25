package com.kru13.httpserver;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class StatusRecyclerAdapter extends RecyclerView.Adapter<StatusRecyclerAdapter.MyViewHolder> {

    private ArrayList<String> mDataset;
    private Context mContext;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public LinearLayout layout;

        public MyViewHolder(LinearLayout v) {
            super(v);
            layout = v;
        }
    }

    public StatusRecyclerAdapter(Context context, ArrayList mDataset) {
        this.mDataset = mDataset;
        this.mContext = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public StatusRecyclerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.status_list_item, parent, false);


        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        View view = holder.layout;
        final TextView name = view.findViewById(R.id.status_text_view);

        name.setText(mDataset.get(holder.getAdapterPosition()));

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset != null ? mDataset.size() : 0;
    }

    public void insert(String message){
        mDataset.add(message);
        notifyItemInserted(mDataset.size() - 1);
    }
}
