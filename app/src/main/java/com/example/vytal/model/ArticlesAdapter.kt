package com.example.vytal.model

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vytal.R
import com.example.vytal.ui.ArticleDetailActivity

class ArticlesAdapter(
    private val context: Context,
    private val articles: List<Article>
) : RecyclerView.Adapter<ArticlesAdapter.ArticleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.title.text = article.title
        holder.subtitle.text = article.subtitle

        // Click listener: open article detail activity
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ArticleDetailActivity::class.java)
            intent.putExtra("article_title", article.title)
            intent.putExtra("article_subtitle", article.subtitle)
            intent.putExtra("article_url", article.url)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = articles.size

    class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.article_title)
        val subtitle: TextView = itemView.findViewById(R.id.article_subtitle)
    }
}

