<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}

function goBack() {
  router.back()
}
</script>

<template>

  <main class="centered">
  <div class="not-found-container">
    <div class="not-found-content">
      <div class="error-code">404</div>
      <h1>Page Not Found</h1>
      <p class="error-message">
        Съжаляваме, но страницата, която търсите, не съществува или е била премахната.
      </p>
      
      <div class="action-buttons">
        <button @click="goHome" class="home-btn">
          <span>🏠</span> Начална страница
        </button>
        <button @click="goBack" class="back-btn">
          <span>←</span> Назад
        </button>
      </div>
    </div>
  </div>
  </main>
</template>

<style scoped>
.not-found-container {
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.not-found-content {
  text-align: center;
  max-width: 600px;
}

.error-code {
  font-size: 8rem;
  font-weight: bold;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 20px;
}

h1 {
  font-size: 2.5rem;
  color: #2c3e50;
  margin-bottom: 16px;
}

.error-message {
  font-size: 1.1rem;
  color: #495057;
  margin-bottom: 40px;
  line-height: 1.6;
}

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.home-btn,
.back-btn {
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: transform 0.2s, box-shadow 0.2s;
  border: none;
}

.home-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.home-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.back-btn {
  background: white;
  color: #667eea;
  border: 2px solid #667eea;
}

.back-btn:hover {
  background: #f2f4ff;
  transform: translateY(-2px);
}

.home-btn:active,
.back-btn:active {
  transform: translateY(0);
}
</style>