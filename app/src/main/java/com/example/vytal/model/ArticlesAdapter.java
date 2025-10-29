package com.example.vytal.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vytal.R;
import java.util.List;

public class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.ArticleViewHolder> {

    private Context context;
    private List<Article> articles;

    // Constructor: store context and list
    public ArticlesAdapter(Context context, List<Article> articles) {
        this.context = context;
        this.articles = articles;
    }

    // Inflates item_article layout and creates a ViewHolder
    @Override
    public ArticleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    // Binds data to UI views inside ViewHolder
    @Override
    public void onBindViewHolder(ArticleViewHolder holder, int position) {
        Article article = articles.get(position);
        holder.title.setText(article.getTitle());
        holder.subtitle.setText(article.getSubtitle());

        // Example click listener: open article detail or webview (placeholder)
        holder.itemView.setOnClickListener(v -> {
            // In a real app, start an activity or open a webview with article.getUrl()
        });
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    // ViewHolder inner class that caches references to views
    public static class ArticleViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        public ArticleViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.article_title);
            subtitle = itemView.findViewById(R.id.article_subtitle);
        }
    }
}
