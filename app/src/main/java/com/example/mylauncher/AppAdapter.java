package com.example.mylauncher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mylauncher.R;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private final Context context;
    private final List<AppInfo> originalAppList;
    private List<AppInfo> displayedAppList;
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    public AppAdapter(Context context, List<AppInfo> appList, OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
        this.context = context;
        this.originalAppList = new ArrayList<>(appList);
        this.displayedAppList = new ArrayList<>(appList);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView label;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_name);
        }

        void bind(final AppInfo app, final OnItemClickListener clickListener, final OnItemLongClickListener longClickListener) {
            if (app != null) {
                if (app.getLabel() != null) {
                    label.setText(app.getLabel());
                }
                if (app.getIcon() != null) {
                    icon.setImageDrawable(app.getIcon());
                }

                if (clickListener != null) {
                    itemView.setOnClickListener(v -> clickListener.onItemClick(app));
                }
                if (longClickListener != null) {
                    itemView.setOnLongClickListener(v -> {
                        longClickListener.onItemLongClick(app);
                        return true;
                    });
                }
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.app_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (displayedAppList != null && position >= 0 && position < displayedAppList.size()) {
            AppInfo app = displayedAppList.get(position);
            if (holder != null && app != null) {
                holder.bind(app, clickListener, longClickListener);
            }
        }
    }

    @Override
    public int getItemCount() {
        return displayedAppList != null ? displayedAppList.size() : 0;
    }

    public void filter(String query) {
        List<AppInfo> newFilteredList;
        if (query == null || query.isEmpty()) {
            newFilteredList = new ArrayList<>(originalAppList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            newFilteredList = originalAppList.stream()
                    .filter(app -> app.getLabel().toString().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
        }

        AppDiffCallback diffCallback = new AppDiffCallback(this.displayedAppList, newFilteredList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.displayedAppList.clear();
        this.displayedAppList.addAll(newFilteredList);
        diffResult.dispatchUpdatesTo(this);
    }

    // Explicitly static public interfaces
    public static interface OnItemClickListener {
        void onItemClick(AppInfo app);
    }

    public static interface OnItemLongClickListener {
        void onItemLongClick(AppInfo app);
    }

    private static class AppDiffCallback extends DiffUtil.Callback {
        private final List<AppInfo> oldList;
        private final List<AppInfo> newList;

        public AppDiffCallback(List<AppInfo> oldList, List<AppInfo> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getPackageName()
                    .equals(newList.get(newItemPosition).getPackageName());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            AppInfo oldApp = oldList.get(oldItemPosition);
            AppInfo newApp = newList.get(newItemPosition);
            return oldApp.getLabel().equals(newApp.getLabel()) &&
                   oldApp.getPackageName().equals(newApp.getPackageName());
        }
    }
}
