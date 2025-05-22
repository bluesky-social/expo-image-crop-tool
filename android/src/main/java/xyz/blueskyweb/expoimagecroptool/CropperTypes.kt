package xyz.blueskyweb.expoimagecroptool

import android.os.Bundle
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

class OpenCropperOptions(
  @Field var imageUri: String? = null,
  @Field var shape: String? = null,
  @Field var aspectRatio: Double? = null,
  @Field var format: String? = null,
  @Field var compressImageQuality: Double? = null,
  @Field var rotationEnabled: Boolean? = true,
) : Record {
  fun toBundle(): Bundle =
    Bundle().apply {
      putString("imageUri", imageUri)
      putString("shape", shape)
      putDouble("aspectRatio", aspectRatio ?: 0.0)
      putString("format", format)
      putDouble("compressImageQuality", compressImageQuality ?: 1.0)
      putBoolean("rotationEnabled", rotationEnabled ?: true)
    }

  companion object {
    fun fromBundle(bundle: Bundle): OpenCropperOptions =
      OpenCropperOptions(
        imageUri = bundle.getString("imageUri"),
        shape = bundle.getString("shape"),
        aspectRatio = bundle.getDouble("aspectRatio"),
        format = bundle.getString("format"),
        compressImageQuality = bundle.getDouble("compressImageQuality"),
        rotationEnabled = bundle.getBoolean("rotationEnabled", true)
      )
  }
}

class OpenCropperResult(
  @Field var path: String? = null,
  @Field var mimeType: String? = null,
  @Field var size: Int? = null,
  @Field var width: Int? = null,
  @Field var height: Int? = null,
) : Record {
  fun toBundle(): Bundle =
    Bundle().apply {
      putString("path", path)
      putString("mimeType", mimeType)
      putInt("size", size ?: 0)
      putInt("width", width ?: 0)
      putInt("height", height ?: 0)
    }

  companion object {
    fun fromBundle(bundle: Bundle): OpenCropperResult =
      OpenCropperResult(
        path = bundle.getString("path"),
        mimeType = bundle.getString("mimeType"),
        size = bundle.getInt("size"),
        width = bundle.getInt("width"),
        height = bundle.getInt("height"),
      )
  }
}
