import ExpoModulesCore
import Mantis
import UIKit

enum CropperError: LocalizedError {
  case openImage
  case findRootView
  case getData
  case getTempUrl
  case writeData
  case cancelled

  var errorDescription: String? {
    switch self {
    case .openImage:
      return "Could not open image"
    case .findRootView:
      return "Could not find root view"
    case .getData:
      return "Could not get image data"
    case .getTempUrl:
      return "Could not get temp url"
    case .writeData:
      return "Could not write image data to temp file"
    case .cancelled:
      return "Crop cancelled"
    }
  }
}

class Cropper: NSObject, CropViewControllerDelegate {
  var cropVc: CropViewController!
  var image: UIImage!
  var options: OpenCropperOptions!

  var onCrop: ((OpenCropperResult) -> Void)!
  var onError: ((Error) -> Void)!

  init(
    options: OpenCropperOptions, onCrop: @escaping (OpenCropperResult) -> Void,
    onError: @escaping (Error) -> Void
  ) throws {
    super.init()

    guard let image = UIImage(contentsOfFile: options.imageUri.deletingPrefix("file://")) else {
      throw CropperError.openImage
    }

    self.image = image
    self.options = options
    self.onCrop = onCrop
    self.onError = onError

    var config = Mantis.Config()
    config.ratioOptions = []

    if let aspectRatio = options.aspectRatio {
      config.presetFixedRatioType = .alwaysUsingOnePresetFixedRatio(ratio: aspectRatio)
    } else if options.shape != "circle" {
      config.ratioOptions = [.all]
    }

    var viewConfig = Mantis.CropViewConfig()
    
    // Keep crop box stationary when rotating image
    viewConfig.rotateCropBoxFor90DegreeRotation = false

    if options.rotationControlEnabled == false {
      // Disable rotation control view if rotationControlEnabled is false
      viewConfig.showAttachedRotationControlView = false
    }

    // Disable rotation control view if rotationEnabled is false
    if options.rotationEnabled == false {
      // Create a toolbar config with rotation buttons removed
      var toolbarConfig = Mantis.CropToolbarConfig()

      // Get the default options and remove rotation options
      var buttonOptions = toolbarConfig.toolbarButtonOptions
      buttonOptions.remove(.clockwiseRotate)
      buttonOptions.remove(.counterclockwiseRotate)

      // Set the modified options back
      toolbarConfig.toolbarButtonOptions = buttonOptions

      // Assign the toolbar config to the main config
      config.cropToolbarConfig = toolbarConfig
    }

    if options.shape == "circle" {
      config.ratioOptions = []
      viewConfig.cropShapeType = .circle(maskOnly: true)
    }

    config.cropViewConfig = viewConfig

    let cropVc = Mantis.cropViewController(image: image, config: config)
    cropVc.delegate = self
    cropVc.modalPresentationStyle = .fullScreen
    self.cropVc = cropVc
  }

  public func cropViewControllerDidCrop(
    _ cropViewController: Mantis.CropViewController, cropped: UIImage,
    transformation: Mantis.Transformation, cropInfo: Mantis.CropInfo
  ) {
    var data: Data?

    if self.options.format == "jpeg" {
      data = cropped.jpegData(compressionQuality: CGFloat(self.options.compressImageQuality))
    } else if self.options.format == "png" {
      data = cropped.pngData()
    }

    guard let data = data else {
      onError(CropperError.getData)
      return
    }

    var ext = "png"
    if self.options.format == "jpeg" {
      ext = "jpg"
    }
    guard let tempUrl = Self.getTempUrl(ext: ext) else {
      onError(CropperError.getTempUrl)
      return
    }

    do {
      try data.write(to: tempUrl)
      let res = OpenCropperResult()
      res.path = tempUrl.absoluteString
      res.width = Float(cropped.size.width)
      res.height = Float(cropped.size.height)
      res.size = data.count
      res.mimeType = "image/\(self.options.format)"
      onCrop(res)
    } catch {
      onError(CropperError.writeData)
    }

    cropViewController.dismiss(animated: true)
  }

  public func cropViewControllerDidCancel(
    _ cropViewController: Mantis.CropViewController, original: UIImage
  ) {
    cropViewController.dismiss(animated: true)
    self.onError(CropperError.cancelled)
  }

  func open() throws {
    guard let rootVc = UIApplication.shared.windows.first?.rootViewController else {
      throw CropperError.findRootView
    }

    guard let cropVc = self.cropVc else {
      return
    }

    // Apply custom button text if provided
    if options.cancelButtonText != nil || options.doneButtonText != nil {
      _ = cropVc.view  // Force view to load
      updateButtonTitles(in: cropVc.view)

      // Delayed check for dynamically added buttons
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
        self.updateButtonTitles(in: self.cropVc.view)
      }
    }

    DispatchQueue.main.async {
      rootVc.topmostViewController().present(cropVc, animated: true)
    }
  }

  private func updateButtonTitles(in view: UIView) {
    for subview in view.subviews {
      if let button = subview as? UIButton {
        let title = button.title(for: .normal) ?? ""

        if let cancelText = options.cancelButtonText, title == "Cancel" {
          button.setTitle(cancelText, for: .normal)
        } else if let doneText = options.doneButtonText, title == "Done" {
          button.setTitle(doneText, for: .normal)
        }
      }

      // Recursively check subviews
      updateButtonTitles(in: subview)
    }
  }

  private static func getTempUrl(ext: String) -> URL? {
    let dir = FileManager().temporaryDirectory
    return URL(
      string: "\(dir.absoluteString)\(ProcessInfo.processInfo.globallyUniqueString).\(ext)")!
  }
}

extension String {
  func deletingPrefix(_ prefix: String) -> String {
    guard self.hasPrefix(prefix) else { return self }
    return String(self.dropFirst(prefix.count))
  }
}

extension UIViewController {
  func topmostViewController() -> UIViewController {
    if let pvc = self.presentedViewController {
      if pvc.isBeingDismissed {
        return self
      }
      return pvc.topmostViewController()
    } else {
      return self
    }
  }
}
