const DB_NAME = 'qianyu-blog-frontend'
const STORE_NAME = 'ai-generated-images'
const DB_VERSION = 1

type ImageAssetRecord = {
  id: string
  url: string
  updatedAt: number
}

function openDatabase() {
  return new Promise<IDBDatabase>((resolve, reject) => {
    const request = window.indexedDB.open(DB_NAME, DB_VERSION)

    request.onupgradeneeded = () => {
      const database = request.result
      if (!database.objectStoreNames.contains(STORE_NAME)) {
        database.createObjectStore(STORE_NAME, { keyPath: 'id' })
      }
    }

    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error ?? new Error('Failed to open IndexedDB'))
  })
}

async function withStore<T>(mode: IDBTransactionMode, action: (store: IDBObjectStore) => IDBRequest<T>) {
  const database = await openDatabase()

  try {
    return await new Promise<T>((resolve, reject) => {
      const transaction = database.transaction(STORE_NAME, mode)
      const store = transaction.objectStore(STORE_NAME)
      const request = action(store)

      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error ?? new Error('IndexedDB request failed'))
    })
  } finally {
    database.close()
  }
}

export async function saveAiImageAsset(id: string, url: string) {
  await withStore('readwrite', store => store.put({
    id,
    url,
    updatedAt: Date.now(),
  } satisfies ImageAssetRecord))
}

export async function loadAiImageAsset(id: string) {
  const record = await withStore<ImageAssetRecord | undefined>('readonly', store => store.get(id))
  return record?.url ?? null
}

export async function deleteAiImageAsset(id: string) {
  await withStore('readwrite', store => store.delete(id))
}
