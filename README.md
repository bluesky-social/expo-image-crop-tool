# Expo Image Crop Tool

An image cropping tool for Expo that allows cropping of the given image. Can be used alongside a library like `expo-image-picker` to provide a more fully-featured cropping experience for users.

# Installation in managed Expo projects

For [managed](https://docs.expo.dev/archive/managed-vs-bare/) Expo projects, please follow the installation instructions in the [API documentation for the latest stable release](#api-documentation). If you follow the link and there is no documentation available then this library is not yet usable within managed projects &mdash; it is likely to be included in an upcoming Expo SDK release.

# Installation in bare React Native projects

For bare React Native projects, you must ensure that you have [installed and configured the `expo` package](https://docs.expo.dev/bare/installing-expo-modules/) before continuing.

### Add the package to your npm dependencies

```bash
npm install expo-image-crop-tool
cd ios
pod install
```

# Usage

For basic examples, see `example/App.tsx`.

## `openCropperAsync(options: OpenCropperOptions) => Promise<OpenCropperResult>`

`openCropperAsync` will return the path to the cropped image, or will throw an error if something goes wrong.

## `OpenCropperOptions`

| Key | Type | iOS | Android | Description |
|-----|------|-----|---------|-------------|
| `imageUri` | `string` | ✅ | ✅ | A path to the image that you want to crop. |
| `shape` | `string` | ✅ | ✅ | The shape that you want the returned image to be (`'rectangle'` \| `'circle'`). Default is `rectangle`. |
| `aspectRatio` | `number` | ✅ | ✅ | If you want to force a particular aspect ratio for the output image. |
| `format` | `string` | ✅ | ✅ | The format of the output image (`'png'` \| `'jpeg'`). Default is `png`. |
| `compressImageQuality` | `number` | ✅ | ✅ | If outputting a JPEG image, the compression quality for the output image. |
| `rotationEnabled` | `boolean` | ✅ | ✅ | Whether or not to allow the user to rotate the image in 90 degree segments. Default is `true`. |
| `rotationControlEnabled` | `boolean` | ✅ |  | Whether or not to show the rotation control view. Default is `true`. |
| `cancelButtonText` | `string` | ✅ | ✅ | Custom text for the Cancel button. |
| `doneButtonText` | `string` | ✅ | ✅ | Custom text for the Done button. |
| `toolbarBackgroundColor` | `string` | ✅ | ✅ | Background color for the toolbar (hex format, e.g. `#00ff00` or `#00ff0080` for transparency). |
| `toolbarForegroundColor` | `string` | ✅ | ✅ | Color for toolbar buttons and text (hex format). |
| `cropBackgroundColor` | `string` | ✅ | ✅ | Background color for the crop area. |
| `viewControllerBackgroundColor` | `string` | ✅ |  | iOS only - Background color for the base view layer behind the crop overlay. |

### Color Format

All color properties accept hex color strings in the following formats:
- 6-digit RGB: `#00ff00` (opaque green)
- 8-digit RGBA: `#00ff0080` (50% transparent green)

Common opacity values:
- `FF` = 100% opaque
- `CC` = 80%
- `80` = 50%
- `40` = 25%
- `00` = 0% (fully transparent)

### Platform Differences

**iOS** uses the Mantis library which has separate layers:
- `cropBackgroundColor` - Controls the semi-transparent overlay around the crop area
- `viewControllerBackgroundColor` - Controls the base background layer behind everything

**Android** uses the CanHub Cropper library which has a simpler architecture:
- `cropBackgroundColor` - Controls the background color (the cropper view is transparent)
- `viewControllerBackgroundColor` - Not used on Android (iOS-only)

## `OpenCropperResult`

| Key | Type | Description |
|-----|------|-------------|
| `path` | `string` | Path of the output image |
| `size` | `number` | Size in bytes of the output image |
| `width` | `number` | Width of the output image |
| `height` | `number` | Height of the output image |
| `mimeType` | `string` | MIME type of the output image |

