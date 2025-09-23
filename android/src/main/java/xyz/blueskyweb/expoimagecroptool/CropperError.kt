package xyz.blueskyweb.expoimagecroptool

sealed class CropperError(
        message: String,
) : Throwable(message) {
  data object OpenImage : CropperError("Could not open image")

  data object GetTempUri : CropperError("Could not get temp uri")

  data object GetData : CropperError("Could not get image data")

  data object WriteData : CropperError("Could not write image data to temp file")

  data object Cancelled : CropperError("Crop cancelled")

  data object Arguments : CropperError("Invalid arguments")

  fun getResultCode(): Int =
          when (this) {
            is OpenImage -> 100
            is GetTempUri -> 101
            is GetData -> 102
            is WriteData -> 103
            is Cancelled -> 104
            is Arguments -> 105
          }

  companion object {
    fun fromResultCode(resultCode: Int): CropperError =
            when (resultCode) {
              100 -> OpenImage
              101 -> GetTempUri
              102 -> GetData
              103 -> WriteData
              104 -> Cancelled
              105 -> Arguments
              0 -> Cancelled // RESULT_CANCELED - back button, system cancellation
              else -> throw IllegalArgumentException("Unknown result code: $resultCode")
            }
  }
}
