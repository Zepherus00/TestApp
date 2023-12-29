package com.example.testapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.testapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class EmailValidationResponse(
    val result: String,
    val reason: String
)

interface EmailValidationService {
    @GET("verify")
    suspend fun verifyEmail(
        @Query("email") email: String,
        @Query("apikey") apiKey: String
    ): EmailValidationResponse
}

private const val BASE_URL = "https://api.kickbox.com/v2/"
private const val API_KEY = "live_10daf57b737ed9d9c17941c972a97f138e9c92e78365935b8d640e51f0b77961"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val emailValidationService: EmailValidationService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EmailValidationService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            btnStartCheck.setOnClickListener {
                val email = inputEmail.text.toString()

                email.takeIf { it.isNotBlank() }?.let {
                    if (isCorrectFormatEmail(it)) {
                        validateEmail(it)
                    } else {
                        layoutInputEmail.error = getString(R.string.invalid_format)
                    }
                } ?: run {
                    layoutInputEmail.error = getString(R.string.empty_input)
                }
            }

            inputEmail.addTextChangedListener(AppOnTextChangedWatcher { inputEmail ->
                if (inputEmail.toString().isNotBlank()) {
                    layoutInputEmail.isErrorEnabled = false
                }
            })
        }
    }

    private fun isCorrectFormatEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun validateEmail(email: String) {
        binding.progressBar.visibility = View.VISIBLE

        with(binding) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = emailValidationService.verifyEmail(email, API_KEY)
                    val result = response.result
                    val reason = response.reason

                    withContext(Dispatchers.Main) {
                        responseTextView.text = returnResponse(result, reason)
                        changeVisibility()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        layoutInputEmail.error = getString(R.string.validate_error)
                        changeVisibility()
                    }
                }
            }
        }
    }

    private fun returnResponse(result: String, reason: String): String {
        val resultMap = mapOf(
            "deliverable" to R.string.deliverable,
            "undeliverable" to R.string.undeliverable,
            "risky" to R.string.risky
        )
        val readableResult = getString(resultMap[result] ?: R.string.unknown)

        val reasonMap = mapOf(
            "invalid_email" to R.string.invalid_email,
            "invalid_domain" to R.string.invalid_domain,
            "rejected_email" to R.string.rejected_email,
            "accepted_email" to R.string.accepted_email,
            "low_quality" to R.string.low_quality,
            "low_deliverability" to R.string.low_deliverability,
            "no_connect" to R.string.no_connect,
            "timeout" to R.string.timeout,
            "invalid_smtp" to R.string.invalid_smtp,
            "unavailable_smtp" to R.string.unavailable_smtp
        )
        val readableReason = getString(reasonMap[reason] ?: R.string.unexpected_error)

        return getString(R.string.response, readableResult, readableReason)
    }

    private fun changeVisibility() {
        with(binding) {
            responseTextView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }
}

class AppOnTextChangedWatcher(val onSuccess: (CharSequence?) -> Unit) : TextWatcher {

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        onSuccess(p0)
    }

    override fun afterTextChanged(s: Editable?) {}
}