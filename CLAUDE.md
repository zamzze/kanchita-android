# Kanchita+ — Plataforma de Streaming para Android TV

## Descripción del Proyecto

Plataforma de streaming similar a Netflix, orientada al mercado latinoamericano. Contenido en español latino como prioridad, inglés con subtítulos en español como respaldo. Target: Android TV.

**Nombre:** Kanchita+ (del quechua "kancha" — lugar de reunión)
**Modelo de negocio:** Freemium con anuncios. Suscripción elimina anuncios propios de la app.

---

## Stack Tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Node.js + Express |
| Base de datos | PostgreSQL 15 (Docker) |
| Android TV | Kotlin + Leanback + ExoPlayer |
| Streaming | HLS externo (.m3u8) — NO self-hosted |
| Auth | JWT (access 15min + refresh 7 días) |
| Scraping | Puppeteer (Cinecalidad) |
| Contenido | TMDB API para metadata |

---

## Arquitectura de Streams

### Principio fundamental
La app NO almacena ni sirve video. Solo provee metadata y embed URLs.

### Providers

**ProviderA — Cinecalidad (Español Latino)**
- URL: `https://cinecalidadhd.world`
- Flujo: Búsqueda por título → página de detalle → embed Goodstream (`gscdn.cam/video/embed/ID`)
- Requiere Puppeteer para resolver la búsqueda
- Stream type: `embed`
- Idioma: `es-lat`

**ProviderB — VidSrc (Inglés + Subtítulos ES)**
- URL: `https://vsembed.ru`
- Flujo: Construye URL directamente con tmdb_id
- Sin Puppeteer — URL estable
- Stream type: `embed`
- Idioma: `en-sub`

### Lógica de scraping on-demand
Cuando el usuario presiona Play:
1. ProviderA busca en Cinecalidad y devuelve embed_url de Goodstream
2. ProviderB construye embed_url de VidSrc desde DB (si ya existe) o lo genera
3. Android TV usa WebView invisible para interceptar el .m3u8
4. ExoPlayer reproduce el .m3u8 interceptado

### WebView invisible en Android TV
```
embed_url → WebView invisible → intercepta .m3u8 → ExoPlayer reproduce
```
Esto resuelve el problema de tokens ligados a IP: WebView y ExoPlayer comparten la misma IP del dispositivo.

---

## Backend — streaming-api/

### Estructura
```
src/
├── config/
│   ├── db.js                    # Pool de PostgreSQL
│   └── env.js                   # Variables de entorno validadas
├── middleware/
│   ├── auth.js                  # Verificar JWT
│   ├── rateLimiter.js           # Rate limiting por endpoint
│   └── errorHandler.js          # Manejo global de errores
├── modules/
│   ├── auth/                    # Login, register, refresh, logout
│   ├── movies/                  # Catálogo de películas
│   ├── series/                  # Catálogo de series + episodios
│   ├── streams/                 # Streams on-demand (scraping)
│   ├── history/                 # Watch history + progreso
│   └── content/                 # Búsqueda TMDB + fetch on-demand
├── db/                          # Queries SQL por módulo
├── ingestion/
│   ├── scraper/
│   │   ├── providers/
│   │   │   ├── providerA.js     # Cinecalidad — Puppeteer
│   │   │   └── providerB.js     # VidSrc — sin Puppeteer
│   │   ├── scraperEngine.js     # Solo usa ProviderB en ingesta
│   │   └── baseProvider.js
│   ├── normalizer/
│   │   ├── movieNormalizer.js
│   │   └── streamNormalizer.js
│   ├── tmdb/
│   │   ├── tmdbClient.js
│   │   └── tmdbFetcher.js
│   ├── scheduler/
│   │   └── contentScheduler.js  # Cron cada 6h
│   └── scripts/
│       └── initialLoad.js       # Carga inicial de contenido
└── utils/
    ├── jwt.js
    ├── hash.js
    └── response.js
```

### Variables de entorno (.env)
```env
PORT=3000
DB_URL=postgresql://streaming_user:streaming_pass@postgres:5432/streaming_db
JWT_SECRET=change_this_secret_32_chars_min
JWT_REFRESH_SECRET=another_secret_32_chars_min
JWT_EXPIRES_IN=15m
JWT_REFRESH_IN=7d
NODE_ENV=development
TMDB_API_KEY=14fa1e0124361fef9516fa4c71ba37ce
PROVIDER_A_URL=https://cinecalidadhd.world
```

### Docker
```bash
# Levantar backend + PostgreSQL
docker compose up

# Reset completo de DB
docker compose down -v && docker compose up --build

# Carga inicial de contenido
docker exec streaming_api node src/ingestion/scripts/initialLoad.js
```

### Endpoints principales

#### Auth
```
POST /api/auth/register    → { email, password }
POST /api/auth/login       → { email, password }
POST /api/auth/refresh     → { refresh_token }
POST /api/auth/logout
```

#### Catálogo
```
GET /api/movies?page=1&limit=20&genre=accion
GET /api/series?page=1&limit=20
GET /api/series/:id/seasons/:season   → lista de episodios
```

#### Detalle
```
GET /api/movies/:id        → detalle + genres
GET /api/series/:id        → detalle + genres + seasons[]
```

#### Streams (on-demand, tarda 15-25s)
```
GET /api/streams/movie/:id
GET /api/streams/episode/:id
```

Respuesta:
```json
{
  "content_id": "uuid",
  "content_type": "movie",
  "show_ads": true,
  "streams": [
    {
      "server_name": "Goodstream Latino",
      "quality": "auto",
      "language": "es-lat",
      "stream_url": null,
      "embed_url": "https://gscdn.cam/video/embed/ID",
      "stream_type": "embed",
      "priority": 1
    },
    {
      "server_name": "VidSrc",
      "quality": "auto",
      "language": "en-sub",
      "stream_url": null,
      "embed_url": "https://vsembed.ru/embed/movie?tmdb=ID&ds_lang=es&autoplay=1",
      "stream_type": "embed",
      "priority": 2
    }
  ]
}
```

#### Content (búsqueda + fetch TMDB on-demand)
```
GET /api/content/search?q=titulo&type=movie|series
GET /api/content/:tmdbId?type=movie|series
```

#### History
```
POST /api/history           → { content_type, content_id, progress_seconds }
GET  /api/history
GET  /api/history/:type/:id → progreso de un contenido específico
```

### Schema de DB importante

**streams table:**
```sql
CREATE TABLE streams (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  content_type VARCHAR(10) NOT NULL,  -- 'movie' | 'episode'
  content_id   UUID NOT NULL,
  server_name  VARCHAR(100),
  quality      VARCHAR(10),
  language     VARCHAR(10),
  stream_url   TEXT,
  embed_url    TEXT,
  stream_type  VARCHAR(20) DEFAULT 'embed',
  priority     SMALLINT DEFAULT 1,
  is_active    BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**movies tiene:** `original_title` (para búsqueda en Cinecalidad con título en inglés)

### Lógica de streams.service.js
- ProviderA siempre se ejecuta fresco (Puppeteer busca embed_url actual de Goodstream)
- ProviderB se lee de DB si ya existe (server_name = 'VidSrc'), sino scrapea y cachea
- Ambos corren en paralelo con Promise.all
- Latino primero (priority 1), inglés como respaldo (priority 2)
- Si ProviderA no encuentra con título en español, reintenta con `original_title` en inglés

---

## Android TV — StreamingAPP/

### Package
`com.samsse.streamingapp`

### Estructura
```
app/src/main/java/com/samsse/streamingapp/
├── StreamingApp.kt              # Application + Koin init
├── data/
│   ├── model/
│   │   └── Models.kt            # Movie, Series, Episode, Stream, etc.
│   ├── remote/
│   │   ├── ApiService.kt        # Retrofit endpoints
│   │   ├── Dtos.kt              # Request/Response bodies
│   │   ├── TokenManager.kt      # DataStore para JWT
│   │   ├── AuthInterceptor.kt   # Agrega Bearer token
│   │   ├── NetworkModule.kt     # OkHttp + Retrofit
│   │   └── RefreshApiProvider.kt # Retrofit sin interceptor para refresh
│   └── repository/
│       ├── AuthRepository.kt
│       ├── MovieRepository.kt
│       ├── SeriesRepository.kt
│       ├── StreamRepository.kt
│       └── HistoryRepository.kt
├── di/
│   └── AppModule.kt             # Koin DI
└── ui/
    ├── MainActivity.kt          # FragmentActivity + NavController
    ├── splash/
    │   └── SplashFragment.kt    # Verifica token + refresh
    ├── auth/
    │   ├── LoginFragment.kt     # Login + Register tabs
    │   └── LoginViewModel.kt
    ├── home/
    │   ├── HomeFragment.kt      # BrowseSupportFragment — filas horizontales
    │   ├── HomeViewModel.kt
    │   ├── CardPresenter.kt     # ImageCardView para Leanback
    │   └── ContentItem.kt
    ├── detail/
    │   ├── DetailFragment.kt    # Backdrop + info + tabs + episodios
    │   ├── DetailViewModel.kt
    │   └── EpisodeAdapter.kt    # RecyclerView de episodios
    └── player/
        ├── PlayerFragment.kt    # WebView invisible + ExoPlayer
        └── PlayerViewModel.kt
```

### Navegación
```
SplashFragment → (token válido) → HomeFragment
SplashFragment → (sin token)   → LoginFragment → HomeFragment
HomeFragment   → DetailFragment(contentId, contentType)
DetailFragment → PlayerFragment(contentId, contentType, showAds, streamsJson)
```

### URL del backend en el emulador
```kotlin
// utils/Constants.kt
const val BASE_URL = "http://10.0.2.2:3000/api/"
// En producción: "https://tu-dominio.com/api/"
```

### Modelos importantes

```kotlin
data class Stream(
    @SerializedName("server_name") val serverName: String,
    val quality: String,
    val language: String,
    @SerializedName("stream_url") val streamUrl: String?,
    @SerializedName("embed_url") val embedUrl: String?,
    @SerializedName("stream_type") val streamType: String,
    val priority: Int
)

data class StreamsResponse(
    @SerializedName("content_id") val contentId: String,
    @SerializedName("content_type") val contentType: String,
    @SerializedName("show_ads") val showAds: Boolean,
    val streams: List<Stream> = emptyList()
)
```

### PlayerFragment — arquitectura WebView + ExoPlayer

```
Usuario selecciona servidor
        │
        ▼
stream_type = "embed"
        │
        ▼
WebView invisible carga embed_url
(gscdn.cam/video/embed/ID o vsembed.ru/embed/...)
        │
        ▼
shouldInterceptRequest captura .m3u8
        │
        ▼
ExoPlayer reproduce .m3u8 directamente
(misma IP = token válido)
```

**Implementación pendiente en PlayerFragment:**
- WebView usa `shouldInterceptRequest` (NO `shouldOverrideUrlLoading`)
- Ignorar errores SSL: `handler.proceed()` en `onReceivedSslError`
- Timeout de 30s si no intercepta m3u8
- Tracking de progreso cada 10s con coroutine

---

## Estado Actual del Proyecto

### Backend ✅ Completado
- Auth completo (register, login, refresh, logout)
- Catálogo movies + series con paginación
- Detalle con genres + seasons
- Streams on-demand (ProviderA Cinecalidad + ProviderB VidSrc)
- Watch history
- Scheduler de ingesta cada 6h
- Initial load script
- Docker setup

### Android TV — En progreso
- ✅ Login + Register
- ✅ Splash con refresh token
- ✅ Home con filas horizontales (Leanback BrowseSupportFragment)
- ✅ Detail con backdrop, info, tabs, episodios, selector temporadas
- ✅ Selector de servidores (card flotante)
- 🔄 Player — WebView + ExoPlayer (en implementación, hay error SSL en emulador)
- ❌ Search screen
- ❌ "Más como esto" en tabs
- ❌ Real-Debrid integration (futuro)
- ❌ QR para registro en web (futuro)

### Problema actual en Player
Error SSL en emulador al cargar `gscdn.cam`:
```
Failed to validate the certificate chain, error: Unacceptable certificate
SSL error code 1, net_error -202
```
Solución: `handler.proceed()` en `onReceivedSslError` del WebView.
En dispositivo real esto no ocurre.

---

## Convenciones de código

- Español latino neutro en UI (textos visibles al usuario)
- Inglés para código, variables, comentarios técnicos
- Repository pattern: cada módulo tiene su Repository
- ViewModel con sealed classes para estados
- ViewBinding en todos los fragments
- Koin para DI
- Coroutines para async

---

## Pendientes importantes

1. **Player** — terminar implementación WebView → ExoPlayer
2. **Search** — SearchFragment con búsqueda en tiempo real
3. **Más como esto** — recomendaciones por género en DetailFragment
4. **Progress tracking** — guardar y recuperar posición de reproducción
5. **Deploy VPS** — configurar nginx + SSL + dominio
6. **Real-Debrid** — integración premium (futuro)
7. **Web registration con QR** — registro desde web, QR en TV (futuro)

---

## Comandos útiles

```bash
# Backend
docker compose up                                    # Levantar
docker compose down -v && docker compose up --build  # Reset DB
docker exec streaming_api node src/ingestion/scripts/initialLoad.js  # Cargar contenido
docker exec -it streaming_db psql -U streaming_user -d streaming_db  # Consola DB

# Ver streams en DB
SELECT content_type, server_name, language, embed_url, stream_type
FROM streams ORDER BY created_at DESC LIMIT 20;

# Android TV
# BASE_URL en emulador: http://10.0.2.2:3000/api/
# BASE_URL en dispositivo real (mismo WiFi): http://192.168.1.X:3000/api/
```
