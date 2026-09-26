package com.autodocs.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.autodocs.app.data.PhotoStorage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Три джерела фото: сканер документів, камера, галерея. Усі повертають список content-URI. */
class PhotoSources internal constructor(
    val scan: (pageLimit: Int) -> Unit,
    val camera: () -> Unit,
    val gallery: (maxItems: Int) -> Unit
)

/**
 * Лаунчери для фото. [onPicked] отримує вибрані/зняті URI (ще НЕ скопійовані в застосунок) і
 * ознаку, що це скани; [onError] — людське повідомлення, коли сканер недоступний.
 *
 * Сканер — ML Kit Document Scanner (Google Play Services): сам знаходить межі аркуша,
 * обрізає, вирівнює й дає «скан»-фільтр. Розпізнавання тексту (OCR) не використовуємо.
 */
@Composable
fun rememberPhotoSources(
    onPicked: (uris: List<Uri>, isScan: Boolean) -> Unit,
    onError: (String) -> Unit
): PhotoSources {
    val context = LocalContext.current
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages.orEmpty()
            val uris = pages.map { it.imageUri }
            if (uris.isNotEmpty()) onPicked(uris, true)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) onPicked(listOf(Uri.parse(uri)), false)
    }
    val singleGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(listOf(uri), false)
    }
    // Обмеження кількості в PickMultipleVisualMedia задається при створенні контракту — тримаємо кілька варіантів.
    val multiLaunchers = (2..5).associateWith { n ->
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(n)) { uris ->
            if (uris.isNotEmpty()) onPicked(uris, false)
        }
    }

    return remember(context) {
        PhotoSources(
            scan = { limit ->
                val activity = context.findActivity()
                if (activity == null) {
                    onError("Сканер недоступний")
                } else {
                    val options = GmsDocumentScannerOptions.Builder()
                        .setGalleryImportAllowed(true)
                        .setPageLimit(limit.coerceIn(1, 5))
                        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .build()
                    GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
                        .addOnSuccessListener { sender ->
                            scanLauncher.launch(IntentSenderRequest.Builder(sender).build())
                        }
                        .addOnFailureListener {
                            onError("Сканер документів недоступний (потрібні Google Play Services). Скористайся камерою або галереєю.")
                        }
                }
            },
            camera = {
                val file = PhotoStorage.newCameraFile(context)
                val uri = PhotoStorage.contentUriFor(context, file)
                cameraUri = uri.toString()
                cameraLauncher.launch(uri)
            },
            gallery = { max ->
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                val n = max.coerceIn(1, 5)
                if (n == 1) singleGallery.launch(request) else multiLaunchers.getValue(n).launch(request)
            }
        )
    }
}
