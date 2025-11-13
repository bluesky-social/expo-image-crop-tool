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

    // Set up custom localization if button text is provided
    if let bundle = Self.createLocalizationBundle(
      cancelText: options.cancelButtonText,
      doneText: options.doneButtonText
    ) {
      config.localizationConfig.bundle = bundle
    }

    if let aspectRatio = options.aspectRatio {
      config.presetFixedRatioType = .alwaysUsingOnePresetFixedRatio(ratio: aspectRatio)
    } else if options.shape != "circle" {
      config.ratioOptions = [.all]
    }

    var viewConfig = Mantis.CropViewConfig()
    
    // Keep crop box stationary when rotating image
    viewConfig.rotateCropBoxFor90DegreeRotation = false

    // Apply background color to crop view area if provided
    if let bgColorHex = options.cropBackgroundColor {
      viewConfig.backgroundColor = UIColor(hexString: bgColorHex)
    }

    if options.rotationControlEnabled == false {
      // Disable rotation control view if rotationControlEnabled is false
      viewConfig.showAttachedRotationControlView = false
    }

    // Create a toolbar config
    var toolbarConfig = Mantis.CropToolbarConfig()

    // Apply custom colors if provided
    if let bgColorHex = options.toolbarBackgroundColor {
      toolbarConfig.backgroundColor = UIColor(hexString: bgColorHex) ?? .black
    }

    if let fgColorHex = options.toolbarForegroundColor {
      toolbarConfig.foregroundColor = UIColor(hexString: fgColorHex) ?? .white
    }

    // Handle rotation button removal if needed
    if options.rotationEnabled == false {
      var buttonOptions = toolbarConfig.toolbarButtonOptions
      buttonOptions.remove(.clockwiseRotate)
      buttonOptions.remove(.counterclockwiseRotate)
      toolbarConfig.toolbarButtonOptions = buttonOptions
    }

    // Always assign the toolbar config
    config.cropToolbarConfig = toolbarConfig

    if options.shape == "circle" {
      config.ratioOptions = []
      viewConfig.cropShapeType = .circle(maskOnly: true)
    }

    config.cropViewConfig = viewConfig

    let cropVc = Mantis.cropViewController(image: image, config: config)
    cropVc.delegate = self
    cropVc.modalPresentationStyle = .fullScreen
    
    // Apply background color to the view controller (area behind everything)
    if let bgColorHex = options.viewControllerBackgroundColor {
      cropVc.view.backgroundColor = UIColor(hexString: bgColorHex)
    }
    
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
    guard let rootVc = getRootViewController() else {
      throw CropperError.findRootView
    }

    guard let cropVc = self.cropVc else {
      return
    }

    DispatchQueue.main.async {
      rootVc.topmostViewController().present(cropVc, animated: true)
    }
  }

  private func getRootViewController() -> UIViewController? {
    if #available(iOS 15.0, *) {
      return UIApplication.shared.connectedScenes
        .compactMap { $0 as? UIWindowScene }
        .flatMap { $0.windows }
        .first { $0.isKeyWindow }?
        .rootViewController
    } else {
      return UIApplication.shared.windows.first?.rootViewController
    }
  }

  private static func getTempUrl(ext: String) -> URL? {
    let dir = FileManager().temporaryDirectory
    return URL(
      string: "\(dir.absoluteString)\(ProcessInfo.processInfo.globallyUniqueString).\(ext)")!
  }

  private static func createLocalizationBundle(cancelText: String?, doneText: String?) -> Bundle? {
    guard cancelText != nil || doneText != nil else {
      return nil
    }

    let fileManager = FileManager.default
    let tempDir = fileManager.temporaryDirectory
    let bundleDir = tempDir.appendingPathComponent(UUID().uuidString)
    let lProjDir = bundleDir.appendingPathComponent("en.lproj")

    do {
      try fileManager.createDirectory(at: lProjDir, withIntermediateDirectories: true)

      var stringsContent = ""

      if let cancelText = cancelText {
        stringsContent += "\"Mantis.Cancel\" = \"\(cancelText)\";\n"
      }

      if let doneText = doneText {
        stringsContent += "\"Mantis.Done\" = \"\(doneText)\";\n"
      }

      let stringsFile = lProjDir.appendingPathComponent("MantisLocalizable.strings")
      try stringsContent.write(to: stringsFile, atomically: true, encoding: .utf8)

      return Bundle(url: bundleDir)
    } catch {
      return nil
    }
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

extension UIColor {
  convenience init?(hexString: String) {
    var hex = hexString.trimmingCharacters(in: .whitespacesAndNewlines)
    hex = hex.replacingOccurrences(of: "#", with: "")

    var rgb: UInt64 = 0
    guard Scanner(string: hex).scanHexInt64(&rgb) else { return nil }

    let length = hex.count
    let r, g, b, a: CGFloat

    if length == 6 {
      r = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
      g = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
      b = CGFloat(rgb & 0x0000FF) / 255.0
      a = 1.0
    } else if length == 8 {
      r = CGFloat((rgb & 0xFF000000) >> 24) / 255.0
      g = CGFloat((rgb & 0x00FF0000) >> 16) / 255.0
      b = CGFloat((rgb & 0x0000FF00) >> 8) / 255.0
      a = CGFloat(rgb & 0x000000FF) / 255.0
    } else {
      return nil
    }

    self.init(red: r, green: g, blue: b, alpha: a)
  }
}
