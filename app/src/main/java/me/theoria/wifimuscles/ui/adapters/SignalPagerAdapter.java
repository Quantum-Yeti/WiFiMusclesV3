package me.theoria.wifimuscles.ui.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SignalPagerAdapter extends RecyclerView.Adapter<SignalPagerAdapter.VH> {

    private final List<View> pages;

    public SignalPagerAdapter(List<View> pages) {
        this.pages = pages;
    }

    static class VH extends RecyclerView.ViewHolder {
        VH(View itemView) {
            super(itemView);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = pages.get(viewType);

        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }

        // 🔥 THIS is the key fix
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        view.setLayoutParams(lp);

        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) { }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}
