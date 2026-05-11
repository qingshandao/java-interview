# 1、安装 VitePress

根目录执行

```java
npm add -D vitepress
```

> 注意：需要 Node 18 及以上的版本，当前使用的是 `Node 18.20.8` 版本

# 2、初始化

```java
npx vitepress init
```

# 3、自动生成侧边栏

安装插件：

```
npm install vitepress-sidebar -D
```

然后修改：

```
docs/.vitepress/config.mts
```

示例：

```
import { defineConfig } from 'vitepress'
import { generateSidebar } from 'vitepress-sidebar'

export default defineConfig({
  themeConfig: {
    sidebar: generateSidebar()
  }
})
```

# 4、开启搜索

新增：

```
search: {
  provider: 'local'
}
```

完整：

```
themeConfig: {
  search: {
    provider: 'local'
  }
}
```

# 5、关闭死链接检查

```java
ignoreDeadLinks: true,  // 关闭死链检查
```



# 6、启动

最后启动：

```
npx vitepress dev docs
```

# 7、build

执行：

```
npm run docs:build
```

成功后会生成：

```
docs/.vitepress/dist
```

复制生成的dist中的内容到临时目录

# 8、本地预览

```
npx vitepress preview docs
```



# 9、再切 web-pages

# 10、把 dist 内容复制过来



# config.mts

```javascript
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

```

