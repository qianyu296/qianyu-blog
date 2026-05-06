const MB = 1024 * 1024
const AUTO_COMPRESS_THRESHOLD_BYTES = 6 * MB
const TARGET_MAX_BYTES = 4.5 * MB
const MAX_DIMENSION = 3200
const ENCODE_QUALITIES = [0.92, 0.88, 0.84, 0.8, 0.76]
const UNSUPPORTED_TYPES = new Set(['image/gif', 'image/svg+xml'])

export interface PreparedImageUpload {
  file: File
  changed: boolean
  originalSize: number
  outputSize: number
  originalWidth?: number
  originalHeight?: number
  outputType: string
}

export async function prepareImageForUpload(file: File): Promise<PreparedImageUpload> {
  if (!file.type.startsWith('image/') || UNSUPPORTED_TYPES.has(file.type)) {
    return unchangedResult(file)
  }

  const source = await loadImageSource(file)
  try {
    const originalWidth = source.width
    const originalHeight = source.height
    const longestSide = Math.max(originalWidth, originalHeight)
    const resizeRatio = longestSide > MAX_DIMENSION ? MAX_DIMENSION / longestSide : 1
    const targetWidth = Math.max(1, Math.round(originalWidth * resizeRatio))
    const targetHeight = Math.max(1, Math.round(originalHeight * resizeRatio))
    const shouldResize = resizeRatio < 1
    const shouldCompress = shouldResize || file.size > AUTO_COMPRESS_THRESHOLD_BYTES

    if (!shouldCompress) {
      return unchangedResult(file, originalWidth, originalHeight)
    }

    const canvas = document.createElement('canvas')
    canvas.width = targetWidth
    canvas.height = targetHeight

    const context = canvas.getContext('2d')
    if (!context) {
      return unchangedResult(file, originalWidth, originalHeight)
    }

    context.imageSmoothingEnabled = true
    context.imageSmoothingQuality = 'high'
    context.drawImage(source.drawable, 0, 0, targetWidth, targetHeight)

    const targetBytes = Math.min(TARGET_MAX_BYTES, Math.max(2 * MB, Math.round(file.size * 0.35)))
    const bestBlob = await encodeBestBlob(canvas, targetBytes)
    if (!bestBlob) {
      return unchangedResult(file, originalWidth, originalHeight)
    }

    const savedEnough = bestBlob.size <= file.size * 0.92
    if (!shouldResize && !savedEnough) {
      return unchangedResult(file, originalWidth, originalHeight)
    }

    if (bestBlob.size >= file.size) {
      return unchangedResult(file, originalWidth, originalHeight)
    }

    const outputExtension = extensionFromMime(bestBlob.type)
    const preparedFile = new File(
      [bestBlob],
      replaceExtension(file.name, outputExtension),
      { type: bestBlob.type, lastModified: file.lastModified },
    )

    return {
      file: preparedFile,
      changed: true,
      originalSize: file.size,
      outputSize: preparedFile.size,
      originalWidth,
      originalHeight,
      outputType: preparedFile.type,
    }
  } finally {
    source.dispose()
  }
}

function unchangedResult(file: File, originalWidth?: number, originalHeight?: number): PreparedImageUpload {
  return {
    file,
    changed: false,
    originalSize: file.size,
    outputSize: file.size,
    originalWidth,
    originalHeight,
    outputType: file.type,
  }
}

async function encodeBestBlob(canvas: HTMLCanvasElement, targetBytes: number) {
  let bestBlob: Blob | null = null

  for (const quality of ENCODE_QUALITIES) {
    const blob = await canvasToBlob(canvas, 'image/webp', quality)
    if (!blob) continue

    if (!bestBlob || blob.size < bestBlob.size) {
      bestBlob = blob
    }

    if (blob.size <= targetBytes) {
      break
    }
  }

  if (bestBlob) {
    return bestBlob
  }

  for (const quality of ENCODE_QUALITIES) {
    const blob = await canvasToBlob(canvas, 'image/jpeg', quality)
    if (!blob) continue

    if (!bestBlob || blob.size < bestBlob.size) {
      bestBlob = blob
    }

    if (blob.size <= targetBytes) {
      break
    }
  }

  return bestBlob
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number) {
  return new Promise<Blob | null>((resolve) => {
    canvas.toBlob(resolve, type, quality)
  })
}

function extensionFromMime(contentType: string) {
  switch (contentType) {
    case 'image/webp':
      return '.webp'
    case 'image/jpeg':
      return '.jpg'
    case 'image/png':
      return '.png'
    default:
      return ''
  }
}

function replaceExtension(fileName: string, extension: string) {
  if (!extension) return fileName
  const dotIndex = fileName.lastIndexOf('.')
  if (dotIndex === -1) {
    return `${fileName}${extension}`
  }
  return `${fileName.slice(0, dotIndex)}${extension}`
}

async function loadImageSource(file: File): Promise<{
  width: number
  height: number
  drawable: CanvasImageSource
  dispose: () => void
}> {
  if ('createImageBitmap' in window) {
    const bitmap = await createImageBitmap(file)
    return {
      width: bitmap.width,
      height: bitmap.height,
      drawable: bitmap,
      dispose: () => bitmap.close(),
    }
  }

  const objectUrl = URL.createObjectURL(file)
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const element = new Image()
      element.onload = () => resolve(element)
      element.onerror = () => reject(new Error('图片解码失败'))
      element.src = objectUrl
    })

    return {
      width: image.naturalWidth,
      height: image.naturalHeight,
      drawable: image,
      dispose: () => URL.revokeObjectURL(objectUrl),
    }
  } catch (error) {
    URL.revokeObjectURL(objectUrl)
    throw error
  }
}

export function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < MB) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / MB).toFixed(2)} MB`
}
