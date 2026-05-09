import { defineConfig } from 'vitepress'
import { generateSidebar } from 'vitepress-sidebar'   // 引入自动生成侧栏

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "a java note",
  description: "java",
  ignoreDeadLinks: true,  // 关闭死链检查
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Examples', link: '/markdown-examples' }
    ],

    sidebar: generateSidebar({
      documentRootPath: 'docs',
    }),

    socialLinks: [
      { icon: 'github', link: 'https://github.com/vuejs/vitepress' }
    ],

    // 开启搜索
    search: {
      provider: 'local'
    }
  }
})
