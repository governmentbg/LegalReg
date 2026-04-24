<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { onMounted } from 'vue'

const openUserWay = () => {
  if (window.UserWay && window.UserWay.accessibility) {
    window.UserWay.accessibility.open()
  } else {
    console.warn("UserWay не е зареден")
  }
}

onMounted(() => {
  const s = document.createElement('script')
  s.src = "https://cdn.userway.org/widget.js"
  s.async = true
  s.setAttribute("data-language", "bg");
  s.setAttribute("data-color", "#8a6948");
  s.setAttribute("data-size", "small");
  s.setAttribute("data-trigger", "accessibility-btn") // когато има тригер не показва официалната иконка
  s.setAttribute("data-mobile", true);
  s.setAttribute("data-disable-floating", true);
  s.setAttribute("src", "https://cdn.userway.org/widget.js");
  document.body.appendChild(s)
})

</script>

<template>

  <header class="app-header">

    <div class="accessibility-bar">
      <button
          id="accessibility-btn"
          class="userway-btn"
          @click="openUserWay"
          title="Достъпност">
        <span class="icon-glasses">👓 Достъпност</span>
      </button>
    </div>

    <img alt="Vue logo" class="logo" src="@/assets/gerb.png" />
    <div class="logo-divider"></div>
    <div>
      <div class="republic-name">РЕПУБЛИКА БЪЛГАРИЯ</div>
      <div class="ministry-name">Министерство на правосъдието</div>
    </div>
    <div class="spacer"></div>
    <div class="app-name">Юридическа правоспособност</div>
    <nav class="main-nav">
      <RouterLink to="/">Регистър</RouterLink>
    </nav>
  </header>

  <main class="content">
    <RouterView v-slot="{ Component }">
      <keep-alive>
        <component :is="Component" v-if="$route.meta.keepAlive" />
      </keep-alive>
      <component :is="Component" v-if="!$route.meta.keepAlive" />
    </RouterView>
  </main>
</template>

