<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import TheLoadingData from '@/components/TheLoadingData.vue'

// Remove static PDF imports
// import pdfMake from 'pdfmake/build/pdfmake'
// import pdfFonts from 'pdfmake/build/vfs_fonts'
// pdfMake.vfs = pdfFonts

const responseData = ref('')
const itemsData = ref([]);
const page = ref(0);
const pageSize = ref(10);
const totalPages = ref(1);
const totalCount = ref(0);

const isLoading = ref(false);
const isExporting = ref(false);
const error = ref(null);
const searchName = ref('');
const nomUp= ref('');
const searchSecondName = ref('');
const searchLastName = ref('')

const sortColumn = ref('a8');
const sortDirection = ref('desc');

// Sort options matching your table columns
const sortOptions = [
  { value: '', label: 'Без сортиране' },
  { value: 'a7', label: 'УП номер' },
  { value: 'a8', label: 'УП дата' },
  { value: 'a1', label: 'Име' },
  { value: 'a2', label: 'Презиме' },
  { value: 'a3', label: 'Фамилия' },
  { value: 'a13', label: 'Протокол дата' }
];

// Helper to build params with current filters
function buildParams(extra = {}) {
  const params = {
    page: page.value,
    pageSize: pageSize.value,
    ...extra
  }
  if (nomUp.value) params.upnum = nomUp.value
  if (searchName.value) params.firstname = searchName.value
  if (searchSecondName.value) params.surname = searchSecondName.value
  if (searchLastName.value) params.lastname = searchLastName.value
  if (sortColumn.value) {
    params.sortBy = sortColumn.value
    params.sortDirection = sortDirection.value
  }
  return params
}

async function fetchRegister() {
  console.log('Fetching register data...');
  isLoading.value = true;
  error.value = null;
  
  try {
    const params = buildParams()
    const response = await axios.get('/urireg/api/register/show', { params })
    responseData.value = response.data
    console.log('Register data fetched:', response.data);
    itemsData.value = response.data;
    totalCount.value = response.data.totalCount;
    totalPages.value = response.data.totalPages;
    page.value = response.data.page;
    pageSize.value = response.data.pageSize;
  } catch (err) {
    console.error('Failed to fetch register data:', err)
    error.value = 'Failed to load data. Please try again.';
  } finally {
    isLoading.value = false;
  }
}

function formatDate(dateString) {
  if (!dateString) return '';
  const date = new Date(dateString);
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();
  return `${day}.${month}.${year}`;
}

function handleSearch() {
  page.value = 0;
  fetchRegister();
}

function goToPage(pageNumber) {
  page.value = pageNumber - 1;
  fetchRegister();
}

function previousPage() {
  if (page.value > 0) {
    page.value--;
    fetchRegister();
  }
}

function nextPage() {
  if (page.value < totalPages.value - 1) {
    page.value++;
    fetchRegister();
  }
}

function changePageSize(event) {
  pageSize.value = parseInt(event.target.value);
  page.value = 0;
  fetchRegister();
}

function handleSort() {
  page.value = 0;
  fetchRegister();
}

function toggleSortDirection() {
  sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc';
  if (sortColumn.value) {
    handleSort();
  }
}

const visiblePages = computed(() => {
  const pages = [];
  const maxVisible = 5;
  const currentDisplayPage = page.value + 1;
  let start = Math.max(1, currentDisplayPage - Math.floor(maxVisible / 2));
  let end = Math.min(totalPages.value, start + maxVisible - 1);
  if (end - start < maxVisible - 1) {
    start = Math.max(1, end - maxVisible + 1);
  }
  for (let i = start; i <= end; i++) pages.push(i);
  return pages;
});

// Fetch all data across pages for export
async function fetchAllForExport() {
  const all = [];
  let p = 0;
  const batchSize = 1000; // adjust if needed
  try {
    while (true) {
      const params = buildParams({ page: p, pageSize: batchSize })
      const { data } = await axios.get('/urireg/api/register/show', { params })
      if (data?.data?.length) all.push(...data.data)
      p++
      if (p >= (data.totalPages ?? 0)) break
    }
  } catch (e) {
    throw e
  }
  return all
}

async function exportPdf() {
  isExporting.value = true
  try {
    // Dynamic import - only loads when needed
    const [{ default: pdfMake }, { default: pdfFonts }] = await Promise.all([
      import('pdfmake/build/pdfmake'),
      import('pdfmake/build/vfs_fonts')
    ])
    pdfMake.vfs = pdfFonts

    const all = await fetchAllForExport()
    const header = [
      '#','УП номер','УП дата','Име','Презиме','Фамилия','Протокол дата','Дубликат','Лишен от право'
    ]
    const body = all.map((item, idx) => ([
      idx + 1,
      item.a7,
      formatDate(item.a8),
      item.a1,
      item.a2,
      item.a3,
      formatDate(item.a13),
      item.a11 === 2 ? 'Да' : '',
      item.a4 === 8 ? 'Да' : ''
    ]))

    const filters = [
      searchName.value ? `Име: ${searchName.value}` : null,
      searchSecondName.value ? `Презиме: ${searchSecondName.value}` : null,
      searchLastName.value ? `Фамилия: ${searchLastName.value}` : null
    ].filter(Boolean).join(' • ')

    const docDefinition = {
      pageSize: 'A4',
      pageOrientation: 'landscape',
      pageMargins: [20, 30, 20, 30],
      content: [
        { text: 'Регистър - резултати от търсене', style: 'header' },
        filters ? { text: filters, margin: [0, 0, 0, 8], color: '#555' } : {},
        { text: `Общо записи: ${all.length}`, margin: [0, 0, 0, 8] },
        {
          table: {
            headerRows: 1,
            widths: ['auto','auto','auto','*','*','*','auto','auto','auto'],
            body: [header, ...body]
          },
          layout: 'lightHorizontalLines'
        }
      ],
      styles: {
        header: { fontSize: 16, bold: true, margin: [0, 0, 0, 8] }
      },
      footer: (currentPage, pageCount) => ({
        columns: [
          { text: new Date().toLocaleString(), alignment: 'left', margin: [20, 0, 0, 0] },
          { text: `${currentPage} / ${pageCount}`, alignment: 'right', margin: [0, 0, 20, 0] }
        ],
        fontSize: 8
      })
    }

    pdfMake.createPdf(docDefinition).download('register-results.pdf')
  } catch (e) {
    console.error('Export failed', e)
    error.value = 'Export failed. Please try again.'
  } finally {
    isExporting.value = false
  }
}

onMounted(() => {
  fetchRegister()
})
</script>

<template>
  <main class="centered">
    <h1>Регистър на лицата, придобили юридическа правоспособност</h1>

    <div class="search-panel">
      <div class="search-fields">
        <div class="input-group">
          <label for="nom-up">УП номер</label>
          <div class="input-with-clear">
            <input 
              id="nom-up"
              v-model="nomUp" 
              type="text" 
              placeholder="Въведете уп номер"
              @keyup.enter="handleSearch"
            />
            <button 
              v-if="nomUp" 
              @click="nomUp = ''" 
              class="clear-btn"
              type="button"
              title="Изчисти"
            >
              ✕
            </button>
          </div>
        </div>
        
        <div class="input-group">
          <label for="name">Име</label>
          <div class="input-with-clear">
            <input 
              id="name"
              v-model="searchName" 
              type="text" 
              placeholder="Въведете име"
              @keyup.enter="handleSearch"
            />
            <button 
              v-if="searchName" 
              @click="searchName = ''" 
              class="clear-btn"
              type="button"
              title="Изчисти"
            >
              ✕
            </button>
          </div>
        </div>
        
        <div class="input-group">
          <label for="secondName">Презиме</label>
          <div class="input-with-clear">
            <input 
              id="secondName"
              v-model="searchSecondName" 
              type="text" 
              placeholder="Въведете презиме"
              @keyup.enter="handleSearch"
            />
            <button 
              v-if="searchSecondName" 
              @click="searchSecondName = ''" 
              class="clear-btn"
              type="button"
              title="Изчисти"
            >
              ✕
            </button>
          </div>
        </div>
        
        <div class="input-group">
          <label for="lastName">Фамилия</label>
          <div class="input-with-clear">
            <input 
              id="lastName"
              v-model="searchLastName" 
              type="text" 
              placeholder="Въведете фамилия"
              @keyup.enter="handleSearch"
            />
            <button 
              v-if="searchLastName" 
              @click="searchLastName = ''" 
              class="clear-btn"
              type="button"
              title="Изчисти"
            >
              ✕
            </button>
          </div>
        </div>
      </div>

      <!-- Sort Controls -->
      <div class="sort-controls">
        <div class="input-group">
          <label for="sortColumn">Сортиране по</label>
          <select 
            id="sortColumn" 
            v-model="sortColumn" 
            @change="handleSort"
            class="sort-select"
          >
            <option v-for="option in sortOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
        
        <button 
          @click="toggleSortDirection" 
          :disabled="!sortColumn"
          class="sort-direction-btn"
          :title="sortDirection === 'asc' ? 'Възходящо' : 'Низходящо'"
        >
          <span v-if="sortDirection === 'asc'">↑ Възходящо</span>
          <span v-else>↓ Низходящо</span>
        </button>
      
        <div style="display:flex; gap:10px; margin-left: auto;">
        
          <button @click="handleSearch" class="search-btn">
            <span>🔍</span> Търсене
          </button>
          <button @click="exportPdf" :disabled="isLoading || isExporting" class="export-btn">
            <span>📄</span> Export PDF
          </button>
        </div>
      </div>
    </div>
    
    <!-- Error State -->
    <div v-if="error" class="error-message">
      <p>{{ error }}</p>
      <button @click="fetchRegister" class="retry-btn">Retry</button>
    </div>
    
    <!-- Data Display -->
    <template v-else-if="itemsData.data && itemsData.data.length > 0">
      <div class="table-container">
        <table class="modern-table">
          <thead>
            <tr>
              <th>#</th>
              <!-- <th>ID</th> -->
              <th>УП номер</th>
              <th>УП дата</th>
              <th>Име</th>
              <th>Презиме</th>
              <th>Фамилия</th>
              <th>Протокол дата</th>
              <th>Дубликат</th>
              <th>Лишен от право</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in itemsData.data" :key="item.a0">
              <td>{{ page * pageSize + index + 1 }}</td>
              <!-- <td>{{ item.a0 }}</td> -->
              <td>{{ item.a7 }}</td>
              <td>{{ formatDate(item.a8) }}</td>
              <td>{{ item.a1 }}</td>
              <td>{{ item.a2 }}</td>
              <td>{{ item.a3 }}</td>
              <td>{{ formatDate(item.a13) }}</td>
              <td>{{ item.a11===2?'Да':'' }}</td>
              <td>{{ item.a4 ===8?'Да':'' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- Pagination Controls -->
      <div class="pagination-container">
        <div class="pagination-info">
          <label for="pageSize">Редове на страници:</label>
          <select id="pageSize" :value="pageSize" @change="changePageSize" class="page-size-select">
            <option value="10">10</option>
            <option value="25">25</option>
            <option value="50">50</option>
            <option value="100">100</option>
          </select>
          <span class="page-info">
            Страница {{ page + 1 }} от {{ totalPages }} ({{ totalCount }} общо записи)
          </span>
        </div>
        
        <div class="pagination-buttons">
          <button 
            @click="goToPage(1)" 
            :disabled="page === 0"  
            class="pagination-btn"
            title="Първа страница"
          >
            << 
          </button>
          
          <button 
            @click="previousPage" 
            :disabled="page === 0"  
            class="pagination-btn"
            title="Предишна страница"
          >
            <
          </button>
          
          <button 
            v-for="pageNum in visiblePages" 
            :key="pageNum"
            @click="goToPage(pageNum)"
            :class="['pagination-btn', { active: pageNum === page + 1 }]"
          >
            {{ pageNum }}
          </button>
          
          <button 
            @click="nextPage" 
            :disabled="page === totalPages - 1"   
            class="pagination-btn"
            title="Следваща страница"
          >
            >
          </button>
          
          <button 
            @click="goToPage(totalPages)" 
            :disabled="page === totalPages - 1"   
            class="pagination-btn"
            title="Последна страница"
          >
            >>
          </button>
        </div>
      </div>
    </template>
    
    <!-- Loading Modal -->
    <TheLoadingData
      v-if="isLoading || isExporting"
      :message="isExporting ? 'Exporting to PDF...' : 'Loading data...'"
    />
  </main>
</template>

