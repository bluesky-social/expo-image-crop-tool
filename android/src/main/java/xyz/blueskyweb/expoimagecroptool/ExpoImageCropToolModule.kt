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

  override fun definition() =
    ModuleDefinition {
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

        event.data?.extras?.let {
          if (event.resultCode == RESULT_OK) {
            val result = OpenCropperResult.fromBundle(it)
            promise.resolve(result)
          } else {
            promise.reject(CropperError.fromResultCode(event.resultCode).toCodedException())
          }
        } ?: run {
          promise.reject(
            CropperError.Arguments.toCodedException(),
          )
        }
      }
    }
}
