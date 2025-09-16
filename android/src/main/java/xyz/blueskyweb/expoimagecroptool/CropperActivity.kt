package xyz.blueskyweb.expoimagecroptool

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
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
        TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dp.toFloat(),
                        resources.displayMetrics,
                )
                .toInt()

// https://gist.github.com/codeswimmer/858833?permalink_comment_id=2347183#gistcomment-2347183
fun Bitmap.rotate(degrees: Float): Bitmap {
  val matrix = Matrix().apply { postRotate(degrees) }
  return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

class CropperActivity : AppCompatActivity() {
  private var cropView: CropImageView? = null
  private var options: OpenCropperOptions? = null
  private var prevRotation: Int = 0

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
            }

    this.cropView = cropView

    ViewCompat.setOnApplyWindowInsetsListener(cropView) { view, insets ->
      val sysBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
      view.setPadding(0, sysBars.top, 0, sysBars.bottom)
      insets
    }

    val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

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
              setBackgroundColor("#66000000".toColorInt())
              val dp16 = dpToPx(16)
              setPadding(0, dp16, 0, dp16) // Remove horizontal padding
              gravity = Gravity.CENTER_VERTICAL // Change to center vertical
            }

    val cancelBtn =
            AppCompatButton(this).apply {
              text = options.cancelButtonText ?: "CANCEL"
              setTextColor(Color.WHITE)
              setBackgroundColor(Color.TRANSPARENT) // Transparent background
              layoutParams =
                      LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        // No weight to prevent stretching
                        setMargins(dpToPx(16), 0, 0, 0) // Left margin only
                      }
              setOnClickListener {
                setResult(RESULT_CANCELED)
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

              // Make the icon white
              setColorFilter(Color.WHITE)

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
                if (options?.rotationEnabled != false && prevRotation != 0) {
                  cropView.rotateImage(-1 * prevRotation)
                  prevRotation = 0
                }
                cropView.resetCropRect()
              }
            }

    bar.addView(resetBtn)

    if (options.rotationEnabled != false) {
      val rotateBtn =
              AppCompatImageButton(this).apply {
                // Use the Material Design icon for rotation
                setImageResource(R.drawable.ic_rotate)

                // Set content description for accessibility
                contentDescription = "Rotate"

                // Make the icon white
                setColorFilter(Color.WHITE)

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
                  prevRotation = (prevRotation + 90) % 360
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
              setTextColor(Color.YELLOW)
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
      @Suppress("DEPRECATION") window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
  }

  fun onDone() {
    var bmap = cropView?.getCroppedImage() ?: return

    if (options?.rotationEnabled != false && prevRotation != 0) {
      bmap = bmap.rotate(prevRotation.toFloat())
    }

    val format = this.options?.format ?: "png"
    val tempFile = File.createTempFile("cropped_image", ".$format", cacheDir)
    val uri = tempFile.toUri()
    val out = contentResolver.openOutputStream(uri)
    out?.let {
      val quality = (intent.getDoubleExtra("compressImageQuality", 1.0) * 100).toInt()
      Log.d("ExpoCropTool", "quality was $quality")
      bmap.compress(
              when (format) {
                "png" -> android.graphics.Bitmap.CompressFormat.PNG
                "jpeg" -> android.graphics.Bitmap.CompressFormat.JPEG
                else -> {
                  setResult(CropperError.Arguments.getResultCode(), null)
                  finish()
                  return
                }
              },
              quality,
              out,
      )
    }
            ?: run {
              setResult(CropperError.WriteData.getResultCode(), null)
              finish()
              return
            }

    out.close()

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
                      size = bmap.byteCount
                      width = bmap.width
                      height = bmap.height
                    }
                    .toBundle()

    val resIntent = Intent().apply { putExtras(result) }

    setResult(RESULT_OK, resIntent)
    finish()
  }
}
