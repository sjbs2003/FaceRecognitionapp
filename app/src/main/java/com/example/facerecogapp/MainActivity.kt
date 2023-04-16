package com.example.facerecogapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {

    private val CAMERA_REQUEST_CODE = 1

    private lateinit var capturedImage: Bitmap

    private fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cameraButton = findViewById<Button>(R.id.button1)
        cameraButton.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        }

        val exitButton = findViewById<Button>(R.id.button2)
        exitButton.setOnClickListener {
            finish()
        }
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://your.face.recognition.api.url/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val faceRecognitionService = retrofit.create(FaceRecognitionService::class.java)


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val imageUri = data?.extras?.get(MediaStore.EXTRA_OUTPUT) as Uri
            val inputStream = contentResolver.openInputStream(imageUri)
            capturedImage = BitmapFactory.decodeStream(inputStream)

        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "image.jpg", capturedImage.toByteArray().toRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val call = faceRecognitionService.recognizeFace(requestBody.part(0))
        call.enqueue(object : Callback<FaceRecognitionResponse> {
            override fun onResponse(
                call: Call<FaceRecognitionResponse>,
                response: Response<FaceRecognitionResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val recognizedPersons = response.body()?.recognizedPersons
                    if (recognizedPersons != null && recognizedPersons.isNotEmpty()) {
                        val message = "Recognized persons: ${recognizedPersons.joinToString(", ")}"
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "No persons recognized", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Face recognition failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<FaceRecognitionResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Face recognition failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }
    data class FaceRecognitionResponse(
        val success: Boolean,
        val message: String,
        val recognizedPersons: List<String>
    )

    interface FaceRecognitionService {

        @Multipart
        @POST("recognize")
        suspend fun recognizeFace(
            @Part image: MultipartBody.Part
        ): FaceRecognitionResponse

    }


}
