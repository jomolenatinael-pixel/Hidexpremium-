package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface GoogleDriveService {

    @Headers("Content-Type: application/json")
    @POST("drive/v3/files")
    suspend fun createFileMetadata(
        @Header("Authorization") authHeader: String,
        @Body metadata: FileMetadata
    ): Response<FileMetadataResponse>

    @Headers("Content-Type: application/octet-stream")
    @PATCH("upload/drive/v3/files/{fileId}")
    suspend fun uploadFileContent(
        @Header("Authorization") authHeader: String,
        @Path("fileId") fileId: String,
        @Query("uploadType") uploadType: String = "media",
        @Body fileBody: RequestBody
    ): Response<ResponseBody>

    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") authHeader: String,
        @Query("q") query: String,
        @Query("spaces") spaces: String = "drive"
    ): Response<FileListResponse>
}

data class FileMetadata(
    val name: String,
    val description: String? = null,
    val mimeType: String = "application/zip",
    val parents: List<String>? = null
)

data class FileMetadataResponse(
    val id: String,
    val name: String,
    val mimeType: String
)

data class FileListResponse(
    val files: List<FileMetadataResponse>
)

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val googleDriveService: GoogleDriveService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleDriveService::class.java)
    }
}
