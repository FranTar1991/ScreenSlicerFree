package com.screenslicerfree.detailsView

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.snackbar.Snackbar

import com.screenslicerfree.database.ScreenshotItem
import com.screenslicerfree.database.DAOScrenshots
import com.screenslicerfree.utils.copyTextToClipboard
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.screenslicerfree.MainActivity
import com.screenslicerfree.R


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailsViewModel  (screenshotUri: String,
                         private val dataSource: DAOScrenshots) : ViewModel() {


    private val _screenshots = dataSource.getAllScreenshots()
    val screenshots
        get() = _screenshots

    private val _screenshot = MediatorLiveData<ScreenshotItem>()
    val screenshot
            get() = _screenshot

    private val _extractingTextFromImage = MutableLiveData<Int>()
    val extractingTextFromImage
        get() = _extractingTextFromImage

    init {
        _screenshot.addSource(dataSource.getByUri(screenshotUri)) { _screenshot.setValue(it) }
        _extractingTextFromImage.value = View.GONE
    }

    fun setNewScreenshot(newScreenshotItem: ScreenshotItem){
        _screenshot.value = newScreenshotItem
    }

    fun onDeleteListWithUri(listToDelete: List<String>){
        viewModelScope.launch {
            deleteItem(listToDelete)
        }
    }

    private suspend fun deleteItem(list: List<String>){
        withContext(Dispatchers.IO){
            dataSource.clearAllByUri(list)
        }
    }

    fun extractTextFromImage(context: Context, uri: Uri, snackbar: Snackbar?) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(context,uri)
        _extractingTextFromImage.value = View.VISIBLE

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                copyTextToClipboard(context, visionText.text, snackbar)
                _extractingTextFromImage.value = View.GONE
            }
            .addOnFailureListener { e ->

                snackbar?.setText(context.getString(R.string.could_not_extract_text))?.show();

              //  Toast.makeText(context,context.getString(R.string.could_not_extract_text), Toast.LENGTH_LONG).show()
                _extractingTextFromImage.value = View.GONE
            }

    }

}