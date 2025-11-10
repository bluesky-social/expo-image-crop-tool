import ExpoModulesCore

struct OpenCropperOptions: Record {
  @Field
  var imageUri: String

  @Field
  var shape: String = "rectangle"

  @Field
  var aspectRatio: Double?

  @Field
  var format: String = "png"

  @Field
  var compressImageQuality: Float = 1.0

  @Field
  var rotationEnabled: Bool = true

  @Field
  var rotationControlEnabled: Bool = true

  @Field
  var cancelButtonText: String?

  @Field
  var doneButtonText: String?

  @Field
  var toolbarBackgroundColor: String?

  @Field
  var toolbarForegroundColor: String?

  @Field
  var cropBackgroundColor: String?

  @Field
  var viewControllerBackgroundColor: String?
}

struct OpenCropperResult: Record {
  @Field
  var path: String

  @Field
  var width: Float = 0

  @Field
  var height: Float = 0

  @Field
  var size: Int

  @Field
  var mimeType: String
}
