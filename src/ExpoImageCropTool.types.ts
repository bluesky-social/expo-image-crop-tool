export interface OpenCropperOptions {
  imageUri: string;
  shape?: "rectangle" | "circle";
  aspectRatio?: number;
  format?: "jpeg" | "png";
  compressImageQuality?: number;
  rotationEnabled?: boolean;
  rotationControlEnabled?: boolean;
  cancelButtonText?: string;
  doneButtonText?: string;
  toolbarBackgroundColor?: string;
  toolbarForegroundColor?: string;
  cropBackgroundColor?: string;
  viewControllerBackgroundColor?: string;
}

export interface OpenCropperResult {
  path: string;
  mimeType: string;
  size: number;
  width: number;
  height: number;
}
