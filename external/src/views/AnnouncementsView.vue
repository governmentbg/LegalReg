<template>
  <main class="centered">
    <h2>Съобщения за провеждане на изпит за юридическа правоспособност</h2>

    <section class="search-container" v-if="sortedMessages.length">
      <div class="search-row">
        <input
            v-model="searchQuery"
            @keyup.enter="performSearch"
            type="text"
            class="search-input"
            placeholder="Въведете дума или израз"
        />
        <button @click="performSearch" class="search-btn">
          <span>🔍</span> Търсене
        </button>
      </div>

      <div v-if="searchResults.length" class="results-info">
        <span>Намерени: {{ searchResults.length }}</span>

        <div class="nav-buttons">
          <button @click="goPrev" :disabled="currentResultIndex === 0">
            ← Предишно
          </button>
          <button @click="goNext" :disabled="currentResultIndex === searchResults.length - 1">
            Следващо →
          </button>
        </div>

        <div class="results-list">
          <div
              v-for="(res, i) in searchResults"
              :key="res.elementId"
              :class="['result-item', {active: currentResultIndex === i}]"
              @click="scrollToResult(i)"
          >
            {{ res.title || 'Без заглавие' }}
          </div>
        </div>
      </div>
    </section>

    <div v-if="error" class="error-message">
      <p>{{ error }}</p>
      <button @click="fetchMessages" class="retry-btn">Опитай отново</button>
    </div>

    <TheLoadingData v-if="isLoading" message="Зареждане..." />

    <p v-if="!isLoading && !error && sortedMessages.length === 0" class="no-data">
      Няма публикувани съобщения.
    </p>

    <section v-if="!isLoading && !error && sortedMessages.length" class="messages timeline-wrapper">
      <div
          v-for="group in groupedByYear"
          :key="group.year"
          class="timeline-year-block"
      >

        <div class="timeline-year-header" @click="toggleYear(group.year)">
          <div class="timeline-folder" :class="{ open: isYearOpen(group.year) }">
            <div class="folder-indicator"></div>
          </div>
          <h2 class="timeline-year-title">{{ group.year }}</h2>
          <div class="timeline-arrow" :class="{ open: isYearOpen(group.year) }"></div>
        </div>

        <transition name="collapse">
          <div v-show="isYearOpen(group.year)" class="timeline-year-content">
            <article
                v-for="msg in group.messages"
                :key="msg.id"
                :id="'msg-' + msg.id"
                class="message"
            >
              <time class="date">{{ formatDate(msg.date_from) }}</time>

              <h3 class="title" v-if="msg.title">{{ msg.title }}</h3>

              <div class="body" v-html="sanitizedHtml(msg.content, msg.id)"></div>

              <div v-if="msg.files?.length" class="file-section">
                <h4 class="file-label">Прикачени файлове:</h4>
                <div class="file-list">
                  <div v-for="file in msg.files" :key="file.id" class="file-item-wrapper">
                    <span
                        class="file-link"
                        :class="{ disabled: downloadingFileId === file.id }"
                        v-if="file.file_id"
                        @click="downloadFile(file)"
                    >
                      <span v-if="downloadingFileId === file.id">⏳</span>
                      <span v-else>{{ file.info || file.name }}</span>
                    </span>

                    <span v-if="downloadErrorId === file.id" class="file-error">
                      ❌ Грешка при сваляне
                    </span>
                  </div>
                </div>
              </div>

            </article>
          </div>
        </transition>

      </div>
    </section>
  </main>
</template>

<script setup>
import {ref, computed, onMounted, nextTick} from "vue"
import axios from "axios"
import TheLoadingData from "@/components/TheLoadingData.vue"

const isLoading = ref(false)
const error = ref(null)
const backendMessages = ref([])

const downloadingFileId = ref(null)
const downloadErrorId = ref(null)

const openYears = ref({}) //отварените години в темплейта..по подразбиране се зарежда само текущата, за да не бави

const searchQuery = ref("")
const searchResults = ref([])
const currentResultIndex = ref(0)

const highlightedCache = ref({}) //кешира подсветката за бързодействие

function isYearOpen(year) { return !!openYears.value[year] }
function toggleYear(year) { openYears.value[year] = !openYears.value[year] }

function formatDate(d) {
  if (!d) return ""
  const dt = new Date(d)
  return `${String(dt.getDate()).padStart(2, "0")}.${String(dt.getMonth()+1).padStart(2,"0")}.${dt.getFullYear()} г.`
}

let DOMPurify = null
try { DOMPurify = window?.DOMPurify ?? null } catch {}

//оптимизирано подсветяване с кеширане
function sanitizedHtml(raw, id) {
  let s = String(raw || "")
  const q = searchQuery.value.trim()
  if (!q) return DOMPurify ? DOMPurify.sanitize(s) : s

  const cacheKey = `${id}-${q}`
  if (highlightedCache.value[cacheKey]) return highlightedCache.value[cacheKey]

  const escaped = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const reg = new RegExp(escaped, "gi")
  s = s.replace(reg, m => `<mark class="highlight">${m}</mark>`)

  const sanitized = DOMPurify ? DOMPurify.sanitize(s) : s
  highlightedCache.value[cacheKey] = sanitized
  return sanitized
}

function performSearch() {
  const q = searchQuery.value.trim().toLowerCase()
  searchResults.value = []
  currentResultIndex.value = 0

  if (!q) return

  sortedMessages.value.forEach(msg => {
    const hit =
        msg.title?.toLowerCase().includes(q) ||
        msg.content?.toLowerCase().includes(q)

    if (hit) {
      const year = msg.date_from
          ? new Date(msg.date_from).getFullYear()
          : "Без дата"

      searchResults.value.push({
        id: msg.id,
        title: msg.title,
        elementId: "msg-" + msg.id,
        year
      })
    }
  })

  if (searchResults.value.length) {
    //отваря само годината на първия резултат
    openYears.value[searchResults.value[0].year] = true
    nextTick(() => scrollToResult(0))
  }
}

function scrollToResult(index) {
  currentResultIndex.value = index
  nextTick(() => {
    const res = searchResults.value[index]
    if (!res) return

    //автоматично зарежда годината при съвпадение, за да покаже текста
    if (!openYears.value[res.year]) {
      openYears.value[res.year] = true
    }

    const el = document.getElementById(res.elementId)
    if (el) el.scrollIntoView({ behavior: "smooth", block: "center" })
  })
}

function goNext() {
  if (currentResultIndex.value < searchResults.value.length - 1) {
    scrollToResult(currentResultIndex.value + 1)
  }
}

function goPrev() {
  if (currentResultIndex.value > 0) {
    scrollToResult(currentResultIndex.value - 1)
  }
}

const sortedMessages = computed(() =>
    backendMessages.value.slice().sort((a, b) =>
        new Date(b.date_from || 0) - new Date(a.date_from || 0)
    )
)


const groupedByYear = computed(() => {
  const map = {}
  const noDate = []

  sortedMessages.value.forEach(msg => {
    if (!msg.date_from) { noDate.push(msg); return }
    const y = new Date(msg.date_from).getFullYear()
    if (!map[y]) map[y] = []
    map[y].push(msg)
  })

  const arr = Object.keys(map)
      .map(Number)
      .sort((a,b)=>b-a)
      .map(year => ({year, messages: map[year]}))

  if (noDate.length) arr.push({year:"Без дата", messages:noDate})
  return arr
})

onMounted(() => {
  fetchMessages().then(() => {
    openYears.value = { [new Date().getFullYear()]: true }
  })
})

async function fetchMessages() {
  isLoading.value = true
  backendMessages.value = []
  error.value = null

  try {
    const {data} = await axios.get("/urireg/api/messages/messagesList")

    backendMessages.value = data.map(row => {
      let files = []
      if (row[6]) {
        try {
          const json = JSON.parse(row[6])
          files = json.map((f, i) => ({
            id: `${row[0]}-${i}`,
            file_id: f.file_id,
            name: f.filename,
            info: f.file_info,
            url: `/urireg/api/messages/file/${f.file_id}`
          }))
        } catch {}
      }

      return {
        id: row[0],
        date_from: row[1],
        date_to: row[2],
        title: row[3],
        content: row[4],
        lang: row[5],
        files
      }
    })
  } catch (e) {
    error.value = "Грешка при зареждане."
  } finally {
    isLoading.value = false
  }
}


async function downloadFile(file) {
  downloadingFileId.value = file.id
  downloadErrorId.value = null

  try {
    const r = await fetch(file.url)
    if (!r.ok) throw 1

    const blob = await r.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = file.name
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch {
    downloadErrorId.value = file.id
  } finally {
    downloadingFileId.value = null
  }
}
</script>
