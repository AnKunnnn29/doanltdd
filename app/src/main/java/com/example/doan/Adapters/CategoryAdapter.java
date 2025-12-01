package com.example.doan.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan.Models.Category;
import com.example.doan.R;
import com.example.doan.Network.RetrofitClient;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<Category> categoryList;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(android.content.Context context, List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    public void updateCategories(List<Category> newList) {
        this.categoryList.clear();
        this.categoryList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.bind(category, listener);
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryImage;
        TextView categoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.category_image);
            categoryName = itemView.findViewById(R.id.category_name);
        }

        public void bind(final Category category, final OnCategoryClickListener listener) {
            categoryName.setText(category.getName());
            
            String imageUrl;
            if (category.getImage() != null && category.getImage().startsWith("http")) {
                 imageUrl = category.getImage();
            } else {
                 imageUrl = RetrofitClient.getBaseUrl() + "/images/categories/" + category.getImage();
            }
            
            Glide.with(itemView.getContext())
                 .load(imageUrl)
                 .placeholder(R.drawable.ic_launcher_background)
                 .into(categoryImage);

            itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        }
    }
}
