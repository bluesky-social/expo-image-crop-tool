export interface OpenCropperOptions {
  imageUri: string;
  shape?: "rectangle" | "circle";
  aspectRatio?: number;
  format?: "jpeg" | "png";
  compressImageQuality?: number;
  rotationEnabled?: boolean;
  rotationControlEnabled?: boolean;
}

export interface OpenCropperResult {
  path: string;
  mimeType: string;
  size: number;
  width: number;
  height: number;
}
