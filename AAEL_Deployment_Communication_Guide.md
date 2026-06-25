# AI 辅助英语学习网站通信逻辑与部署备忘

## 1. 项目整体结构

```text
AAEL/
├── frontend/                    # Vue 3 + Vite 前端源码
│   ├── src/
│   ├── package.json
│   ├── vite.config.js
│   └── ...
├── src/main/java/               # Java 后端代码
│   ├── Servlet/                 # Servlet 接口层
│   ├── DAO/                     # 数据访问层
│   ├── Service/                 # 业务逻辑层
│   └── Utils/                   # 工具类，如 DBUtil
├── src/main/webapp/
│   ├── WEB-INF/
│   ├── index.jsp                # Tomcat 根路径入口，跳转到 app/
│   └── app/                     # Vue build 后的前端静态资源
├── target/
│   └── AAEL_war_exploded/
│       └── app/                 # IDEA Tomcat 本地运行时实际使用的前端资源
├── pom.xml
└── .gitignore
```

## 2. Vue 与 Tomcat 的通信方式

开发阶段，Vue 运行在 Vite 开发服务器上，例如：

```text
http://localhost:5175/
```

Tomcat 后端运行在：

```text
http://localhost:8080/AAEL_war_exploded/
```

Vue 通过 axios 请求 `/api/...`，Vite 会根据 `vite.config.js` 的代理配置，把请求转发到 Tomcat。

示例：

```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080/AAEL_war_exploded',
      changeOrigin: true
    }
  }
}
```

请求流程：

```text
Vue 页面
  ↓ axios 请求 /api/connect-test
Vite dev server
  ↓ proxy 转发
Tomcat: http://localhost:8080/AAEL_war_exploded/api/connect-test
  ↓
ConnectTestServlet
  ↓
返回 JSON 给 Vue
```

## 3. Vue 如何访问 Java 后端 Servlet

后端 Servlet 放在：

```text
src/main/java/Servlet/
```

例如：

```java
@WebServlet("/api/connect-test")
public class ConnectTestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"后端连接成功\"}");
    }
}
```

`/api/connect-test` 是接口 URL，不是实际文件夹。

本地 Tomcat 访问路径：

```text
http://localhost:8080/AAEL_war_exploded/api/connect-test
```

服务器如果 WAR 包名为 `AAEL.war`：

```text
http://服务器IP:1145/AAEL/api/connect-test
```

服务器如果 WAR 包名为 `ROOT.war`：

```text
http://服务器IP:1145/api/connect-test
```

## 4. axios 配置建议

`frontend/src/utils/request.js`：

```js
import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.DEV ? '/api' : '../api',
  timeout: 10000
})

request.interceptors.response.use(
  response => response.data,
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

export default request
```

含义：

- 开发环境：`/api` 交给 Vite 代理到本地 Tomcat。
- 打包部署后：`../api` 表示相对于 Vue 页面所在目录访问后端接口。

如果 Vue 页面在：

```text
http://localhost:8080/AAEL_war_exploded/app/
```

那么：

```text
../api/connect-test
```

会指向：

```text
http://localhost:8080/AAEL_war_exploded/api/connect-test
```

服务器同理：

```text
http://服务器IP:1145/AAEL/app/
```

请求：

```text
../api/connect-test
```

会指向：

```text
http://服务器IP:1145/AAEL/api/connect-test
```

## 5. 本地开发与本地 Tomcat 集成的区别

### 5.1 日常前端开发

使用：

```bash
cd frontend
npm run dev
```

访问：

```text
http://localhost:5175/
```

特点：

- Vue 支持热更新。
- 修改 `.vue` 文件后浏览器自动刷新。
- 请求 `/api/...` 时由 Vite 代理到本地 Tomcat。
- 不需要每次修改都执行 `npm run build`。

### 5.2 Tomcat 集成测试

如果要通过 Tomcat 访问 Vue 页面，需要执行：

```bash
cd frontend
npm run build
```

打包后 Vue 静态资源会生成到：

```text
src/main/webapp/app/
```

然后通过脚本同步到：

```text
target/AAEL_war_exploded/app/
```

本地访问：

```text
http://localhost:8080/AAEL_war_exploded/
```

`index.jsp` 会跳转到：

```text
http://localhost:8080/AAEL_war_exploded/app/
```

## 6. index.jsp 根路径跳转

`src/main/webapp/index.jsp`：

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    response.sendRedirect("app/");
%>
```

作用：

- 本地访问 `http://localhost:8080/AAEL_war_exploded/` 时跳转到 Vue 页面。
- 服务器访问 `http://服务器IP:1145/AAEL/` 时跳转到 Vue 页面。

## 7. npm run build 后同步到 target

由于 IDEA 本地 Tomcat 实际使用的是：

```text
target/AAEL_war_exploded/
```

而 Vue 默认构建输出到：

```text
src/main/webapp/app/
```

所以本地需要在 build 后复制一份到 target。

### 7.1 Windows 本地开发

在 `frontend/package.json` 中配置：

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "postbuild": "xcopy /E /Y \"..\\src\\main\\webapp\\app\" \"..\\target\\AAEL_war_exploded\\app\""
  }
}
```

执行：

```bash
npm run build
```

会自动：

1. 构建 Vue 到 `src/main/webapp/app/`
2. 复制一份到 `target/AAEL_war_exploded/app/`

然后在 IDEA Tomcat 中选择：

```text
Update classes and resources
```

即可看到更新后的 Vue 页面。

### 7.2 Linux 服务器或 Linux 环境

如果在服务器上也需要 build 并复制到 target，不能使用 `xcopy`，应使用 Linux 命令 `cp -r`。

推荐写法：

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "postbuild": "mkdir -p ../target/AAEL_war_exploded/app && cp -r ../src/main/webapp/app/* ../target/AAEL_war_exploded/app/"
  }
}
```

如果手动执行：

```bash
mkdir -p ../target/AAEL_war_exploded/app
cp -r ../src/main/webapp/app/* ../target/AAEL_war_exploded/app/
```

注意：服务器正式部署时通常已经完成开发，不一定需要在服务器上执行 Vue build。更推荐本地 build 后上传静态资源和 WAR 包。

## 8. WAR 包命名与访问路径

部署到 Tomcat 时，将 Maven 打包生成的 `.war` 放入服务器 Tomcat 的 `webapps/` 目录。

### 8.1 使用 AAEL.war

如果 WAR 包名是：

```text
AAEL.war
```

部署后访问路径：

```text
http://服务器IP:1145/AAEL/
```

后端接口路径：

```text
http://服务器IP:1145/AAEL/api/connect-test
```

Vue 页面路径：

```text
http://服务器IP:1145/AAEL/app/
```

### 8.2 使用 ROOT.war

如果 WAR 包名是：

```text
ROOT.war
```

部署后访问路径：

```text
http://服务器IP:1145/
```

后端接口路径：

```text
http://服务器IP:1145/api/connect-test
```

Vue 页面路径：

```text
http://服务器IP:1145/app/
```

如果使用 Nginx 反向代理，推荐后端 WAR 包使用 `ROOT.war`，这样 `/api/...` 转发更简单。

## 9. 服务器上 Nginx 与 Tomcat 的关系

推荐生产结构：

```text
用户浏览器
  ↓
Nginx: 80 / 443
  ├── 返回 Vue 静态资源：/var/vue/
  └── 转发 API 请求：/api/* → Tomcat: 127.0.0.1:1145
        ↓
      Servlet
        ↓
      DAO / JDBC
        ↓
      MySQL 8.4
```

Nginx 负责：

- 对外暴露 80 / 443 端口。
- 提供 Vue 静态页面。
- 将 `/api/` 请求转发给 Tomcat。
- 后续可配置 HTTPS、gzip、缓存、访问日志。

Tomcat 负责：

- 运行 Java Web 后端。
- 处理 Servlet 接口。
- 通过 JDBC 访问 MySQL。

## 10. Nginx 配置示例

推荐新建一个独立 server 配置，而不是直接写进 phpMyAdmin 的 server 块中。

```nginx
server {
    listen 80;
    server_name _;

    charset utf-8;

    location / {
        root /var/vue;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:1145;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~ /\. {
        deny all;
    }

    access_log /www/wwwlogs/aael_access.log;
    error_log /www/wwwlogs/aael_error.log;
}
```

注意：

```nginx
proxy_pass http://127.0.0.1:1145;
```

末尾不要加 `/`。

如果写成：

```nginx
proxy_pass http://127.0.0.1:1145/;
```

可能会导致 `/api/connect-test` 被转发成 `/connect-test`，从而访问不到 Servlet。

## 11. /var/vue、webapp/app 与 target/app 的区别

### 11.1 `src/main/webapp/app/`

这是 Vue 构建产物在项目源码中的位置。

来源：

```bash
npm run build
```

用途：

- 可以被 Maven 打进 WAR 包。
- 可以提交到 GitHub。
- 可以上传到服务器 `/var/vue/`。

### 11.2 `target/AAEL_war_exploded/app/`

这是 IDEA 本地 Tomcat 实际运行时使用的部署目录。

来源：

```text
src/main/webapp/app/ 复制而来
```

用途：

- 本地 Tomcat 集成测试。
- 访问 `http://localhost:8080/AAEL_war_exploded/app/`。

特点：

- `target/` 是构建产物目录。
- 执行 Maven clean 后会被删除。
- 不建议提交到 GitHub。

### 11.3 `/var/vue/`

这是服务器上 Nginx 使用的 Vue 静态资源目录。

来源：

```text
本地 src/main/webapp/app/* 上传到服务器 /var/vue/
```

用途：

- 生产环境由 Nginx 直接返回 Vue 页面。
- 用户访问 `http://服务器IP/` 时看到首页。

特点：

- 与 Tomcat WAR 包相互独立。
- 更新前端时只需要重新上传 Vue 静态文件。
- 通常不需要重启 Tomcat。

## 12. 服务器部署流程

### 12.1 本地构建前端

```bash
cd frontend
npm run build
```

生成：

```text
src/main/webapp/app/
```

### 12.2 本地打包后端

```bash
mvn clean package
```

生成：

```text
target/AAEL.war
```

### 12.3 上传 Vue 静态资源

```bash
scp -r src/main/webapp/app/* root@服务器IP:/var/vue/
```

如果服务器没有目录：

```bash
mkdir -p /var/vue
```

### 12.4 上传 WAR 包

如果使用 `AAEL.war`：

```bash
scp target/AAEL.war root@服务器IP:/www/server/tomcat/webapps/
```

访问：

```text
http://服务器IP:1145/AAEL/
```

如果使用 `ROOT.war`：

```bash
scp target/AAEL.war root@服务器IP:/www/server/tomcat/webapps/ROOT.war
```

访问：

```text
http://服务器IP:1145/
```

### 12.5 重启 Tomcat

```bash
systemctl restart tomcat
```

或者使用宝塔面板重启 Tomcat。

### 12.6 检查 Nginx 配置

```bash
nginx -t
systemctl reload nginx
```

### 12.7 用户访问

如果使用 Nginx：

```text
http://服务器IP/
```

如果直接访问 Tomcat：

```text
http://服务器IP:1145/AAEL/
```

推荐用户访问 Nginx 地址，不直接访问 Tomcat。

## 13. 推荐最终访问方式

生产环境推荐：

```text
用户访问：http://服务器IP/
```

Nginx 返回 Vue 首页：

```text
/var/vue/index.html
```

Vue 页面内部请求：

```text
/api/connect-test
```

Nginx 转发给 Tomcat：

```text
http://127.0.0.1:1145/api/connect-test
```

Tomcat 中的 Servlet 处理请求，并访问数据库。

## 14. 常见问题

### Q1：服务器需要安装 Node.js 和 Vue 吗？

不一定需要。

如果本地已经执行了：

```bash
npm run build
```

并上传了构建产物，那么服务器只需要：

- Nginx
- Tomcat
- JDK
- MySQL 或可连接的数据库

不需要安装 Node.js 和 Vue。

### Q2：为什么访问 `/AAEL/` 会跳到 `/AAEL/app/`？

因为 `index.jsp` 中写了：

```jsp
response.sendRedirect("app/");
```

这是正常行为。

### Q3：用户应该访问 Vue 还是 Tomcat 接口？

用户应该访问 Vue 首页。

后端接口 `/api/...` 是给 Vue 内部调用的，不是用户直接使用的页面。

### Q4：为什么本地 `npm run dev` 有热更新，Tomcat 页面没有？

因为：

- `npm run dev` 运行的是 Vite 开发服务器。
- Tomcat 访问的是 `target/AAEL_war_exploded/app/` 中的静态文件。
- 修改 Vue 源码不会自动更新 Tomcat 中的静态文件，必须 `npm run build` 并同步到 target。

## 15. 总结

本项目的核心通信逻辑是：

```text
Vue 前端负责页面展示
Servlet 后端负责业务接口
DAO / JDBC 负责访问 MySQL
Vite proxy 用于本地开发联调
Tomcat 用于运行 Java 后端
Nginx 用于生产环境提供 Vue 静态资源并反向代理 API
```

本地开发：

```text
Vue: http://localhost:5175/
Tomcat: http://localhost:8080/AAEL_war_exploded/
API: /api/xxx → Vite proxy → Tomcat
```

本地集成测试：

```text
http://localhost:8080/AAEL_war_exploded/
→ index.jsp
→ /app/
→ Vue 页面
```

服务器部署：

```text
http://服务器IP/
→ Nginx
→ /var/vue/index.html
→ Vue 页面
→ /api/xxx
→ Tomcat: 127.0.0.1:1145
→ Servlet
→ MySQL
```
