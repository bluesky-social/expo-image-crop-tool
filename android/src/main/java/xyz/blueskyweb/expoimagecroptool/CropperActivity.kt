package xyz.blueskyweb.expoimagecroptool

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.canhub.cropper.CropImageView
import java.io.File

private fun Context.dpToPx(dp: Int): Int =
  TypedValue
    .applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      dp.toFloat(),
      resources.displayMetrics,
    ).toInt()

/**
 * Convert hex color string to Android color int.
 * Handles both iOS format (#RRGGBBAA) and Android format (#AARRGGBB).
 * Converts iOS format to Android format if alpha channel is detected at the end.
 */
private fun String.toAndroidColorInt(): Int {
  var hex = this.trim().removePrefix("#")
  
  // If 8 characters (RRGGBBAA), convert to Android format (AARRGGBB)
  if (hex.length == 8) {
    val rr = hex.substring(0, 2)
    val gg = hex.substring(2, 4)
    val bb = hex.substring(4, 6)
    val aa = hex.substring(6, 8)
    hex = aa + rr + gg + bb
  }
  
  return ("#" + hex).toColorInt()
}

class CropperActivity : AppCompatActivity() {
  private var cropView: CropImageView? = null
  private var options: OpenCropperOptions? = null
  private var resetBtn: AppCompatImageButton? = null
  private var rotationCount: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Enable edge-to-edge mode but keep navigation bar persistent
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val options =
      intent.extras?.let { OpenCropperOptions.fromBundle(it) }
        ?: run {
          setResult(CropperError.Arguments.getResultCode(), null)
          finish()
          return
        }
    this.options = options

    val cropView =
      CropImageView(this).apply {
        options.imageUri?.let { setImageUriAsync(it.toUri()) }
          ?: run {
            setResult(CropperError.OpenImage.getResultCode(), null)
            finish()
            return
          }

        options.shape?.let {
          cropShape =
            when (it) {
              "rectangle" -> CropImageView.CropShape.RECTANGLE
              "circle" -> CropImageView.CropShape.OVAL
              else -> {
                setResult(CropperError.Arguments.getResultCode(), null)
                finish()
                return
              }
            }
        }

        if (options.shape == "circle") {
          setFixedAspectRatio(true)
          setAspectRatio(1, 1)
        } else {
          options.aspectRatio?.let {
            if (it <= 0) {
              setFixedAspectRatio(false)
            } else {
              setFixedAspectRatio(true)
              setAspectRatio((it * 100).toInt(), 100)
            }
          }
        }

        // Add listener for crop overlay changes
        setOnSetCropOverlayReleasedListener {
          // Show reset button when crop area is adjusted
          resetBtn?.visibility = View.VISIBLE
        }
      }

    this.cropView = cropView

    ViewCompat.setOnApplyWindowInsetsListener(cropView) { view, insets ->
      val sysBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
      view.setPadding(0, sysBars.top, 0, sysBars.bottom)
      insets
    }

    val root = FrameLayout(this).apply { 
      // On Android, only cropBackgroundColor is used (viewControllerBackgroundColor is iOS-only)
      val bgColor = options.cropBackgroundColor ?: "#000000" // default black
      setBackgroundColor(bgColor.toAndroidColorInt())
    }

    root.addView(
      cropView,
      FrameLayout.LayoutParams(
        MATCH_PARENT,
        MATCH_PARENT,
      ),
    )

    val bar =
      LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        // Apply toolbar background color or default to semi-transparent black
        setBackgroundColor(
          options.toolbarBackgroundColor?.toAndroidColorInt() ?: "#66000000".toColorInt()
        )
        val dp16 = dpToPx(16)
        setPadding(0, dp16, 0, dp16) // Remove horizontal padding
        gravity = Gravity.CENTER_VERTICAL // Change to center vertical
      }

    val cancelBtn =
      AppCompatButton(this).apply {
        text = options.cancelButtonText ?: "CANCEL"
        // Apply foreground color or default to white
        setTextColor(options.toolbarForegroundColor?.toAndroidColorInt() ?: Color.WHITE)
        setBackgroundColor(Color.TRANSPARENT) // Transparent background
        layoutParams =
          LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            // No weight to prevent stretching
            setMargins(dpToPx(16), 0, 0, 0) // Left margin only
          }
        setOnClickListener {
          setResult(CropperError.Cancelled.getResultCode(), null)
          finish()
        }
      }
    bar.addView(cancelBtn)

    // Spacer to push reset to center
    val leftSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) }
    bar.addView(leftSpacer)

    val resetBtn =
      AppCompatImageButton(this).apply {
        // Use the reset icon
        setImageResource(R.drawable.ic_reset)

        // Set content description for accessibility
        contentDescription = "Reset"

        // Apply foreground color or default to white
        setColorFilter(options.toolbarForegroundColor?.toAndroidColorInt() ?: Color.WHITE)

        // Set a transparent background
        setBackgroundColor(Color.TRANSPARENT)

        // Set the layout parameters with proper sizing
        val buttonSize = dpToPx(48)
        layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)

        // Set padding inside the button
        val padding = dpToPx(12)
        setPadding(padding, padding, padding, padding)

        // Initially hidden
        visibility = View.GONE

        // Set the click listener
        setOnClickListener {
          // Reset rotation
          if (rotationCount % 4 != 0) {
            val rotationsToReset = (4 - (rotationCount % 4)) * 90
            cropView.rotateImage(rotationsToReset)
            rotationCount = 0
          }
          // Reset crop
          cropView.resetCropRect()
          // Hide reset button
          visibility = View.GONE
        }
      }

    this.resetBtn = resetBtn
    bar.addView(resetBtn)

    if (options.rotationEnabled != false) {
      val rotateBtn =
        AppCompatImageButton(this).apply {
          // Use the Material Design icon for rotation
          setImageResource(R.drawable.ic_rotate)

          // Set content description for accessibility
          contentDescription = "Rotate"

          // Apply foreground color or default to white
          setColorFilter(options.toolbarForegroundColor?.toAndroidColorInt() ?: Color.WHITE)

          // Set a transparent background
          setBackgroundColor(Color.TRANSPARENT)

          // Set the layout parameters with proper sizing
          val buttonSize = dpToPx(48)
          layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)

          // Set padding inside the button
          val padding = dpToPx(12)
          setPadding(padding, padding, padding, padding)

          // Set the click listener
          setOnClickListener {
            cropView.rotateImage(90)
            rotationCount++
            resetBtn?.visibility = View.VISIBLE
          }
        }
      bar.addView(rotateBtn)
    }

    // Spacer to push done to right
    val rightSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) }
    bar.addView(rightSpacer)

    // Done button (right-aligned)
    val doneBtn =
      AppCompatButton(this).apply {
        text = options.doneButtonText ?: "DONE"
        // Apply foreground color or default to yellow
        setTextColor(options.toolbarForegroundColor?.toAndroidColorInt() ?: Color.YELLOW)
        setBackgroundColor(Color.TRANSPARENT) // Transparent background
        layoutParams =
          LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            // No weight to prevent stretching
            setMargins(0, 0, dpToPx(16), 0) // Right margin only
          }
        setOnClickListener { onDone() }
      }
    bar.addView(doneBtn)

    root.addView(
      bar,
      FrameLayout.LayoutParams(
        MATCH_PARENT,
        WRAP_CONTENT,
        Gravity.BOTTOM,
      ),
    )

    setContentView(root)

    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
      val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
      view.setPadding(0, systemBarsInsets.top, 0, systemBarsInsets.bottom)
      insets
    }

    // 31 or higher
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      window.insetsController?.let {
        it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_DEFAULT
      }
    } else {
      @Suppress("DEPRECATION")
      window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
  }

  fun onDone() {
    val options =
      this.options
        ?: run {
          setResult(CropperError.Arguments.getResultCode(), null)
          finish()
          return
        }

    val bmap =
      cropView?.getCroppedImage()
        ?: run {
          setResult(CropperError.GetData.getResultCode(), null)
          finish()
          return
        }

    try {
      val format = options.format ?: "png"
      val tempFile = File.createTempFile("cropped_image", ".$format", cacheDir)
      val uri = tempFile.toUri()

      contentResolver.openOutputStream(uri)?.use { outputStream ->
        val quality = ((options.compressImageQuality ?: 1.0).coerceIn(0.0, 1.0) * 100).toInt()

        val compressFormat =
          when (format) {
            "png" -> Bitmap.CompressFormat.PNG
            "jpeg" -> Bitmap.CompressFormat.JPEG
            else -> {
              setResult(CropperError.Arguments.getResultCode(), null)
              finish()
              return
            }
          }

        val success = bmap.compress(compressFormat, quality, outputStream)
        if (!success) {
          setResult(CropperError.WriteData.getResultCode(), null)
          finish()
          return
        }
      }
        ?: run {
          setResult(CropperError.WriteData.getResultCode(), null)
          finish()
          return
        }

      // Get actual file size
      val fileSize = tempFile.length().toInt()

      val result =
        OpenCropperResult()
          .apply {
            path = uri.toString()
            mimeType =
              when (format) {
                "png" -> "image/png"
                "jpeg" -> "image/jpeg"
                else -> null
              }
            size = fileSize // Actual file size, not memory size
            width = bmap.width
            height = bmap.height
          }.toBundle()

      setResult(RESULT_OK, Intent().apply { putExtras(result) })
      finish()
    } catch (e: Exception) {
      Log.e("ExpoCropTool", "Error saving cropped image", e)
      setResult(CropperError.WriteData.getResultCode(), null)
      finish()
    }
  }
}
