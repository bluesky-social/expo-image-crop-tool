package xyz.blueskyweb.expoimagecroptool

import android.app.Activity.RESULT_OK
import android.content.Intent
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.toCodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoImageCropToolModule : Module() {
  companion object {
    private const val CROP_REQUEST_CODE = 4909
  }

  private var pendingPromise: Promise? = null

  override fun definition() = ModuleDefinition {
    Name("ExpoImageCropTool")

    AsyncFunction("openCropperAsync") { options: OpenCropperOptions, promise: Promise ->
      val context = appContext.reactContext

      val intent = Intent(context, CropperActivity::class.java)
      val bundle = options.toBundle()
      intent.putExtras(bundle)

      pendingPromise = promise
      appContext.throwingActivity.startActivityForResult(intent, CROP_REQUEST_CODE)
    }

    OnActivityResult { _, event ->
      if (event.requestCode != CROP_REQUEST_CODE) {
        return@OnActivityResult
      }

      val promise = pendingPromise ?: return@OnActivityResult
      pendingPromise = null

      when (event.resultCode) {
        RESULT_OK -> {
          // Only for successful results, we need extras
          event.data?.extras?.let { extras ->
            val result = OpenCropperResult.fromBundle(extras)
            promise.resolve(result)
          }
                  ?: run {
                    // Success but no data - this shouldn't happen
                    promise.reject(CropperError.Arguments.toCodedException())
                  }
        }
        else -> {
          // For any non-OK result, we don't need extras
          // Just use the result code to determine the error
          promise.reject(CropperError.fromResultCode(event.resultCode).toCodedException())
        }
      }
    }
  }
}
