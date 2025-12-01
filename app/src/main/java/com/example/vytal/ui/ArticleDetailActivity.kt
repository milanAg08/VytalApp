package com.example.vytal.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.vytal.R

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var tvArticleTitle: TextView
    private lateinit var tvArticleSubtitle: TextView
    private lateinit var btnReadMore: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_detail)

        val title = intent.getStringExtra("article_title") ?: ""
        val subtitle = intent.getStringExtra("article_subtitle") ?: ""
        val url = intent.getStringExtra("article_url") ?: ""

        initViews()
        setupViews(title, subtitle, url)
    }

    private fun initViews() {
        tvArticleTitle = findViewById(R.id.tvArticleTitle)
        tvArticleSubtitle = findViewById(R.id.tvArticleSubtitle)
        btnReadMore = findViewById(R.id.btnReadMore)
    }

    private fun setupViews(title: String, subtitle: String, url: String) {
        tvArticleTitle.text = title
        tvArticleSubtitle.text = subtitle

        btnReadMore.setOnClickListener {
            if (url.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
    }
}

