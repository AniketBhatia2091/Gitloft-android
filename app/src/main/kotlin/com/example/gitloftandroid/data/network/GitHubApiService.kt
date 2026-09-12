package com.example.gitloftandroid.data.network

import com.example.gitloftandroid.data.model.GitHubContentItem
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.data.model.GitHubTreeResponse
import com.example.gitloftandroid.data.model.GitHubUser
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApiService {

    @GET("user")
    fun getUser(
        @Header("Authorization") authHeader: String? = null
    ): Call<GitHubUser>

    @GET("users/{username}")
    fun getUserByUsername(
        @Path("username") username: String,
        @Header("Authorization") authHeader: String? = null
    ): Call<GitHubUser>

    @GET("user/repos?sort=updated&per_page=100")
    fun getRepos(
        @Header("Authorization") authHeader: String? = null
    ): Call<List<GitHubRepo>>

    @GET("users/{username}/repos?sort=updated&per_page=100")
    fun getReposByUsername(
        @Path("username") username: String,
        @Header("Authorization") authHeader: String? = null
    ): Call<List<GitHubRepo>>

    @GET("repos/{owner}/{repo}/git/trees/{sha}?recursive=1")
    fun getRepoTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String,
        @Header("Authorization") authHeader: String? = null
    ): Call<GitHubTreeResponse>

    @GET("repos/{owner}/{repo}/contents/{path}")
    fun getRepoContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Header("Authorization") authHeader: String? = null
    ): Call<List<GitHubContentItem>>

    @GET("repos/{owner}/{repo}/contents/{path}")
    fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Header("Accept") accept: String = "application/vnd.github.v3.raw",
        @Header("Authorization") authHeader: String? = null
    ): Call<ResponseBody>

    @GET("repos/{owner}/{repo}/readme")
    fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Accept") accept: String = "application/vnd.github.v3.raw",
        @Header("Authorization") authHeader: String? = null
    ): Call<ResponseBody>
}
