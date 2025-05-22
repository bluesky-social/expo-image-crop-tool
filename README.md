# Expo Image Crop Tool

An image cropping tool for Expo that allows cropping of the given image. Can be used alongside a library like `expo-image-picker` to provide a more fully-featured cropping experience for users.

# Installation in managed Expo projects

For [managed](https://docs.expo.dev/archive/managed-vs-bare/) Expo projects, please follow the installation instructions in the [API documentation for the latest stable release](#api-documentation). If you follow the link and there is no documentation available then this library is not yet usable within managed projects &mdash; it is likely to be included in an upcoming Expo SDK release.

# Installation in bare React Native projects

For bare React Native projects, you must ensure that you have [installed and configured the `expo` package](https://docs.expo.dev/bare/installing-expo-modules/) before continuing.

### Add the package to your npm dependencies

```bash
npkm install expo-image-crop-tool
cd ios
pod install
```

# Usage

For basic examples, see `example/App.tsx`.

## `openCropperAsync(options: OpenCropperOptions) => Promise<OpenCropperResult>`

`openCropperAsync` will return the path to the cropped image, or will throw an error if something goes wrong.

## `OpenCropperOptions`

### `imageUri: string`

A path to the image that you want to crop.

### `shape?: 'rectangle' | 'circle'`

The shape that you want the returned image to be. Default is `rectangle`.

### `aspectRatio?: number`

If you want to force a particular aspect ratio for the output image.

### `format?: 'png' | 'jpeg'`

The format of the output image. Default is `png`.

### `compressImageQuality?: number`

If outputting a JPEG image, the compression quality for the output image.

### `rotationEnabled?: boolean`

Whether or not to allow the user to rotate the image in 90 degree segments. Default is `true`.

### `rotationControlEnabled?: boolean`

Whether or not to show the rotation control view. Default is `true` (iOS ONLY).

## `OpenCropperResult`

### `path: string`

Path of the output image

### `size: number`

Size in bytes of the output image

### `width: number`

Width of the output image

### `height: number`

Height of the output image

### `mimeType: string`

MIME type of the output image
