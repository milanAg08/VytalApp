package com.example.vytal.api

import com.example.vytal.model.Post
import retrofit2.Response
import retrofit2.http.GET

interface CommunityApiService {
    @GET("posts")
    suspend fun getPosts(): Response<List<Post>>
}
