import httpx
import asyncio
import json
import os
import time
from tqdm import tqdm

# Configuración
TARGET_COUNT = 2000
IMAGE_FOLDER = "productos_imagenes"
JSON_FILE = "productos.json"
# API de búsqueda de Vtex (usada por Jumbo, Vea, Disco, etc.)
BASE_URL = "https://www.jumbo.com.ar/api/catalog_system/pub/products/search"
CONCURRENT_DOWNLOADS = 15
PAGE_SIZE = 50 # Máximo permitido por Vtex por petición

async def download_image(client, url, filename):
    try:
        if not url:
            return False
        response = await client.get(url, timeout=15.0)
        if response.status_code == 200:
            with open(os.path.join(IMAGE_FOLDER, filename), "wb") as f:
                f.write(response.content)
            return True
    except Exception:
        pass
    return False

async def get_products():
    products_data = []
    
    if not os.path.exists(IMAGE_FOLDER):
        os.makedirs(IMAGE_FOLDER)

    print(f"Obteniendo {TARGET_COUNT} productos de Jumbo (Vtex API)...")
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "application/json"
    }

    async with httpx.AsyncClient(headers=headers, follow_redirects=True, timeout=30.0) as client:
        start = 0
        while len(products_data) < TARGET_COUNT:
            end = start + PAGE_SIZE - 1
            params = {
                "O": "OrderByTopSaleDESC",
                "_from": str(start),
                "_to": str(end)
            }
            
            try:
                response = await client.get(BASE_URL, params=params)
                if response.status_code not in [200, 206]:
                    print(f"Error en API ({response.status_code}). Reintentando...")
                    await asyncio.sleep(2)
                    continue
                    
                items = response.json()
                if not items:
                    print("No se encontraron más productos.")
                    break
                    
                for item in items:
                    if len(products_data) >= TARGET_COUNT:
                        break
                    
                    try:
                        name = item.get("productName")
                        brand = item.get("brand")
                        category = item.get("categories", ["Varios"])[0].replace("/", "")
                        
                        # Vtex structure for price and images
                        first_item = item.get("items", [{}])[0]
                        price = 0
                        sellers = first_item.get("sellers", [{}])
                        if sellers:
                            price = sellers[0].get("commertialOffer", {}).get("Price", 0)
                        
                        unit = first_item.get("measurementUnit", "un")
                        images = first_item.get("images", [{}])
                        img_url = images[0].get("imageUrl") if images else None
                        
                        if not img_url:
                            continue
                            
                        # Nombre de archivo local
                        ext = "jpg"
                        filename = f"prod_{len(products_data)}.{ext}"
                        
                        product = {
                            "id": item.get("productId"),
                            "nombre": name,
                            "marca": brand,
                            "categoria": category,
                            "precio": price,
                            "unidad": unit,
                            "imagen_url_original": img_url,
                            "imagen_local": filename
                        }
                        
                        products_data.append(product)
                    except Exception as e:
                        continue
                
                print(f"Avance: {len(products_data)}/{TARGET_COUNT} productos...")
                start += PAGE_SIZE
                # Pequeño delay para no saturar
                await asyncio.sleep(0.5)
                
            except Exception as e:
                print(f"Error en loop: {e}")
                await asyncio.sleep(2)

        # Descarga de imágenes
        print(f"\nDescargando {len(products_data)} imágenes...")
        tasks = []
        semaphore = asyncio.Semaphore(CONCURRENT_DOWNLOADS)
        
        async def sem_download(prod):
            async with semaphore:
                return await download_image(client, prod["imagen_url_original"], prod["imagen_local"])

        for prod in products_data:
            tasks.append(sem_download(prod))
            
        for f in tqdm(asyncio.as_completed(tasks), total=len(tasks), desc="Descargando"):
            await f

    # Guardar JSON
    with open(JSON_FILE, "w", encoding="utf-8") as f:
        json.dump(products_data, f, indent=4, ensure_ascii=False)
        
    print(f"\n¡Éxito!")
    print(f"Total productos procesados: {len(products_data)}")
    print(f"Archivo JSON: {JSON_FILE}")
    print(f"Carpeta de imágenes: {IMAGE_FOLDER}")

if __name__ == "__main__":
    asyncio.run(get_products())
