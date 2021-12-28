package com.screenslicerpro.detailsView

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.lifecycle.*
import com.screenslicerpro.R
import com.screenslicerpro.database.ScreenshotItem
import com.screenslicerpro.database.ScreenshotsDAO
import com.screenslicerpro.utils.copyTextToClipboard
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailsViewModel  (private val screenshotUri: String,
                         private val dataSource: ScreenshotsDAO) : ViewModel() {


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

    fun extractTextFromImage(context: Context, uri: Uri) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(context,uri)
        _extractingTextFromImage.value = View.VISIBLE
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                copyTextToClipboard(context, visionText.text)
                _extractingTextFromImage.value = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(context,context.getString(R.string.could_not_extract_text), Toast.LENGTH_LONG).show()
                _extractingTextFromImage.value = View.GONE
            }
    }

}