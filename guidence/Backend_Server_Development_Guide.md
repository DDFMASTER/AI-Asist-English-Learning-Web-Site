# AAEL 后端开发与服务器连接指导

## 1. 项目背景

本项目为 AI 辅助英语学习决策在线英语学习网站，项目名称为：

```text
AI-Assist-English-Learning-Web-Site
```

项目简称：

```text
AAEL
```

项目采用 Java Web + Vue 前后端结合架构。

原始开发模式接近 JSP Model2：

```text
JSP / Servlet / JavaBean
```

目前视图层已逐步改为 Vue 负责页面展示，因此整体开发结构调整为：

```text
Vue 前端视图层
  ↓
Servlet 控制层
  ↓
Service 业务服务层
  ↓
DAO 数据访问层
  ↓
MySQL 数据库
```

后端仍运行在 Tomcat 中，Servlet 负责处理 HTTP 请求和响应。

## 2. 本地与服务器运行环境

### 2.1 本地开发环境

本地 Tomcat 端口：

```text
8080
```

本地项目部署路径通常为：

```text
http://localhost:8080/AAEL_war_exploded/
```

本地后端接口示例：

```text
http://localhost:8080/AAEL_war_exploded/api/connect-test
```

本地 Vue 开发服务器端口由 Vite 自动分配，常见为：

```text
http://localhost:5173/
http://localhost:5174/
http://localhost:5175/
```

Vue 开发阶段通过 Vite proxy 转发 `/api` 请求到本地 Tomcat。

### 2.2 服务器运行环境

服务器 Tomcat 端口：

```text
1145
```

服务器 Tomcat 示例访问地址：

```text
http://8.146.204.179:1145/
```

如果 WAR 包名为 `AAEL.war`，服务器项目访问地址为：

```text
http://8.146.204.179:1145/AAEL/
```

服务器后端接口示例：

```text
http://8.146.204.179:1145/AAEL/api/connect-test
```

如果 WAR 包名为 `ROOT.war`，服务器项目访问地址为：

```text
http://8.146.204.179:1145/
```

对应后端接口示例：

```text
http://8.146.204.179:1145/api/connect-test
```

## 3. 前后端通信逻辑

### 3.1 开发阶段

开发阶段 Vue 运行在 Vite 开发服务器上，Tomcat 只负责后端接口。

通信流程：

```text
浏览器访问 Vue 页面
  ↓
Vue 使用 axios 请求 /api/xxx
  ↓
Vite proxy 将 /api/xxx 转发到 Tomcat
  ↓
Servlet 接收请求
  ↓
Service 处理业务逻辑
  ↓
DAO 访问数据库
  ↓
MySQL 返回结果
  ↓
Servlet 返回 JSON
  ↓
Vue 渲染页面
```

示例：

```text
Vue 请求：/api/connect-test
代理到：http://localhost:8080/AAEL_war_exploded/api/connect-test
```

### 3.2 部署阶段

部署到服务器后，Vue 既可以：

1. 作为静态资源由 Tomcat 中的 `app/` 目录提供；
2. 也可以作为静态资源由 Nginx 的 `/var/vue/` 目录提供。

推荐生产部署方式：

```text
Nginx 提供 Vue 静态资源
Tomcat 提供 Java 后端 API
```

通信流程：

```text
用户浏览器
  ↓
Nginx 80/443
  ├── 普通页面请求 → 返回 /var/vue/ 中的 Vue 静态文件
  └── /api/xxx 请求 → 反向代理到 Tomcat 127.0.0.1:1145
        ↓
      Servlet
        ↓
      Service
        ↓
      DAO
        ↓
      MySQL
```

## 4. MVC 分层架构说明

当前项目采用 MVC 思想组织后端代码。

虽然项目早期为 JSP Model2 模式，但现在 View 层主要由 Vue 实现。

### 4.1 View 层

View 层由 Vue 负责。

位置：

```text
frontend/
```

构建后的静态资源位置：

```text
src/main/webapp/app/
```

本地 Tomcat 实际运行时同步位置：

```text
target/AAEL_war_exploded/app/
```

服务器 Nginx 静态资源目录：

```text
/var/vue/
```

View 层职责：

- 展示页面
- 接收用户输入
- 调用后端接口
- 根据后端返回的 JSON 更新页面
- 不直接访问数据库
- 不编写核心业务逻辑

### 4.2 Controller 层

Controller 层由 Servlet 实现。

位置：

```text
src/main/java/Servlet/
```

职责：

- 接收 HTTP 请求
- 获取请求参数
- 调用 Service 层
- 设置响应类型和编码
- 返回 JSON 或必要的响应数据
- 不直接编写 SQL
- 不直接操作数据库连接

Servlet URL 建议统一使用 `/api` 前缀，例如：

```java
@WebServlet("/api/user/login")
```

### 4.3 Model 层

Model 层由以下部分组成：

```text
Entities/
DAO/
Service/
Utils/
```

职责：

- `Entities` 表示数据库表对应的实体对象
- `DAO` 负责数据库访问
- `Service` 负责业务逻辑
- `Utils` 提供通用工具，如 JDBC 连接

## 5. src/main/java 包结构说明

当前 Java 后端主要包结构如下：

```text
src/main/java/
├── DAO/
├── Entities/
├── Filter/
├── Listener/
├── Service/
├── Servlet/
└── Utils/
```

## 6. DAO 层开发规范

DAO 层位置：

```text
src/main/java/DAO/
```

DAO 全称为 Data Access Object，即数据访问对象。

DAO 层职责：

- 负责数据库增删改查
- 负责执行 SQL
- 负责将 `ResultSet` 转换为实体对象
- 不处理 HTTP 请求
- 不编写页面逻辑
- 不直接向前端返回 JSON

DAO 层建议包含接口和实现类。

示例结构：

```text
DAO/
├── UserDAO.java
└── impl/
    └── UserDAOImpl.java
```

如果当前项目尚未建立 `impl` 包，也可以先使用：

```text
DAO/
├── UserDAO.java
└── UserDAOImpl.java
```

### 6.1 DAO 接口示例

```java
package DAO;

import Entities.User;

public interface UserDAO {
    User findByUsername(String username);

    int insertUser(User user);

    int updateUser(User user);

    int deleteById(int id);
}
```

### 6.2 DAO 实现类示例

```java
package DAO;

import Entities.User;
import Utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAOImpl implements UserDAO {
    @Override
    public User findByUsername(String username) {
        String sql = "SELECT id, username, email FROM users WHERE username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    return user;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询用户失败", e);
        }

        return null;
    }

    @Override
    public int insertUser(User user) {
        return 0;
    }

    @Override
    public int updateUser(User user) {
        return 0;
    }

    @Override
    public int deleteById(int id) {
        return 0;
    }
}
```

## 7. Entities 层开发规范

Entities 层位置：

```text
src/main/java/Entities/
```

Entities 层用于存放数据库表对应的实体类。

例如数据库表：

```text
users
```

对应实体类：

```text
Entities/User.java
```

实体类职责：

- 只表示数据结构
- 包含字段、构造方法、getter、setter
- 不处理 HTTP 请求
- 不编写 SQL
- 不直接连接数据库
- 不包含复杂业务逻辑

示例：

```java
package Entities;

public class User {
    private int id;
    private String username;
    private String email;
    private String password;

    public User() {
    }

    public User(int id, String username, String email, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
```

## 8. Service 层开发规范

Service 层位置：

```text
src/main/java/Service/
```

Service 层面向具体业务功能。

职责：

- 组合 DAO 层方法
- 实现业务规则
- 进行参数校验
- 处理登录、注册、学习记录、推荐逻辑等业务
- 不直接处理 HTTP 请求
- 不直接返回前端响应
- 一般不直接写 SQL

示例结构：

```text
Service/
├── UserService.java
└── impl/
    └── UserServiceImpl.java
```

如果暂时不使用接口，也可以先写：

```text
Service/
└── UserService.java
```

示例：

```java
package Service;

import DAO.UserDAO;
import DAO.UserDAOImpl;
import Entities.User;

public class UserService {
    private final UserDAO userDAO = new UserDAOImpl();

    public User findUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        return userDAO.findByUsername(username);
    }
}
```

## 9. Servlet 层开发规范

Servlet 层位置：

```text
src/main/java/Servlet/
```

Servlet 是后端 HTTP 接口入口。

职责：

- 使用 `@WebServlet` 定义接口路径
- 读取请求参数
- 调用 Service 层
- 返回 JSON 数据
- 统一设置 UTF-8 编码
- 处理请求方法，如 `doGet`、`doPost`

Servlet 不应该：

- 直接拼接复杂 SQL
- 直接访问数据库
- 直接包含大量业务判断
- 直接返回 HTML 页面

示例：

```java
package Servlet;

import Service.UserService;
import Entities.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/user/find")
public class UserFindServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String username = request.getParameter("username");
        User user = userService.findUserByUsername(username);

        if (user == null) {
            response.getWriter().write("{\"success\":false,\"message\":\"用户不存在\"}");
            return;
        }

        String json = String.format(
                "{\"success\":true,\"id\":%d,\"username\":\"%s\",\"email\":\"%s\"}",
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );

        response.getWriter().write(json);
    }
}
```

后续建议引入 JSON 库，例如 Jackson 或 Gson，避免手动拼接 JSON。

## 10. Utils 工具层开发规范

Utils 层位置：

```text
src/main/java/Utils/
```

Utils 层用于存放通用工具类，例如：

- JDBC 数据库连接工具
- 字符串处理工具
- 日期时间工具
- 密码加密工具
- JSON 响应工具

### 10.1 DBUtil 职责

`DBUtil` 负责创建数据库连接。

示例职责：

```text
DBUtil.getConnection()
```

返回：

```text
java.sql.Connection
```

DAO 层通过 `DBUtil` 获取连接。

Servlet 和 Service 层不建议直接使用 `DBUtil`，除非是临时测试。

### 10.2 JDBC 连接环境差异

本地开发连接云服务器 MySQL 时，JDBC URL 可能类似：

```text
jdbc:mysql://服务器公网IP:3306/数据库名?serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

服务器 Tomcat 与 MySQL 在同一台 ECS 上时，建议使用：

```text
jdbc:mysql://localhost:3306/数据库名?serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

如果 Tomcat 和 MySQL 在同一台服务器，生产环境不建议通过公网 IP 连接数据库。

## 11. Filter / Filter 层说明

当前项目目录为：

```text
src/main/java/Filter/
```

该层通常用于过滤器功能。

过滤器职责：

- 统一设置请求和响应编码
- 登录状态校验
- 权限校验
- 跨域控制
- 日志记录
- 请求预处理

示例功能：

```text
/api/user/*
```

可以通过过滤器判断用户是否已登录。

## 12. Listener 监听器层说明

Listener 层位置：

```text
src/main/java/Listener/
```

监听器职责：

- 监听 Web 应用启动和关闭
- 初始化全局配置
- 初始化数据库连接池
- 记录在线用户数量
- 加载系统配置

常见监听器：

```text
ServletContextListener
HttpSessionListener
ServletRequestListener
```

示例用途：

```text
应用启动时加载配置
应用关闭时释放资源
Session 创建/销毁时统计在线用户
```

## 13. 推荐接口命名规范

所有后端接口建议统一使用 `/api` 前缀。

用户相关：

```text
/api/user/login
/api/user/register
/api/user/logout
/api/user/profile
```

课程相关：

```text
/api/course/list
/api/course/detail
/api/course/progress
```

单词相关：

```text
/api/word/list
/api/word/search
/api/word/collect
```

学习记录相关：

```text
/api/study/record
/api/study/report
/api/study/recommend
```

测试接口：

```text
/api/connect-test
```

## 14. AI 插件生成后端代码时的约束

当使用 IDEA AI 插件生成代码时，应遵守以下规则：

1. 后端接口类放入 `src/main/java/Servlet/`。
2. 数据访问代码放入 `src/main/java/DAO/`。
3. 数据库表实体类放入 `src/main/java/Entities/`。
4. 业务逻辑放入 `src/main/java/Service/`。
5. 工具类放入 `src/main/java/Utils/`。
6. 不要在 Servlet 中直接写复杂 SQL。
7. 不要在 Vue 中直接连接数据库。
8. 不要把数据库密码硬编码在多个类中，应统一放在 `DBUtil` 或配置文件中。
9. Servlet 返回 JSON，Vue 负责展示页面。
10. 所有响应编码应使用 UTF-8。
11. 所有 SQL 参数应使用 `PreparedStatement`，禁止字符串拼接 SQL。
12. 如果需要连接数据库，DAO 层通过 `DBUtil.getConnection()` 获取连接。
13. 接口路径统一使用 `/api/...`。
14. 本地 Tomcat 端口是 `8080`，服务器 Tomcat 端口是 `1145`。
15. 如果涉及部署路径，本地一般为 `/AAEL_war_exploded`，服务器可能为 `/AAEL` 或 `/`。

## 15. 标准后端调用链

推荐后端调用链：

```text
Vue 组件
  ↓ axios
Servlet
  ↓
Service
  ↓
DAO
  ↓
DBUtil
  ↓
MySQL
```

示例：

```text
Login.vue
  ↓ POST /api/user/login
UserLoginServlet
  ↓
UserService.login()
  ↓
UserDAO.findByUsername()
  ↓
DBUtil.getConnection()
  ↓
MySQL users 表
```

## 16. 后端代码生成示例要求

如果需要生成一个用户登录功能，AI 插件应生成：

```text
Entities/User.java
DAO/UserDAO.java
DAO/UserDAOImpl.java
Service/UserService.java
Servlet/UserLoginServlet.java
```

不要只生成 Servlet。

每一层职责：

```text
UserLoginServlet：读取 username/password，调用 UserService
UserService：判断参数合法性，校验密码逻辑
UserDAO：根据 username 查询数据库
User：承载用户数据
DBUtil：提供数据库连接
```

## 17. 数据库连接与权限建议

开发阶段：

- 本地可以通过公网 IP 连接云服务器 MySQL。
- 阿里云安全组只允许开发者公网 IP 访问 3306。
- 不建议向全网开放 MySQL 3306。

服务器部署阶段：

- Tomcat 和 MySQL 在同一台 ECS 上时，使用 `localhost:3306`。
- 应用用户使用最小权限。
- 不建议 Java 应用使用 MySQL root 用户。

应用用户建议权限：

```sql
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP
ON 数据库名.*
TO '应用用户'@'localhost';
```

开发者用户如果需要建库，可由管理员单独授权。

## 18. 打包与部署说明

### 18.1 本地构建 Vue

```bash
cd frontend
npm run build
```

Vue 构建产物：

```text
src/main/webapp/app/
```

如果配置了 `postbuild`，会同步到：

```text
target/AAEL_war_exploded/app/
```

### 18.2 Maven 打包 WAR

```bash
mvn clean package
```

生成：

```text
target/AAEL.war
```

### 18.3 WAR 命名

如果上传为：

```text
AAEL.war
```

服务器访问：

```text
http://8.146.204.179:1145/AAEL/
```

如果上传为：

```text
ROOT.war
```

服务器访问：

```text
http://8.146.204.179:1145/
```

## 19. Nginx 与 Tomcat 部署关系

生产环境推荐：

```text
Nginx 监听 80/443
Tomcat 监听 1145
MySQL 监听 3306
```

Nginx 配置：

```nginx
server {
    listen 80;
    server_name _;

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
}
```

注意：

```text
proxy_pass http://127.0.0.1:1145;
```

末尾不要加 `/`，避免 `/api/xxx` 路径被错误改写。

## 20. 最终总结

本项目后端开发应遵循：

```text
Vue 负责页面展示
Servlet 负责请求响应
Service 负责业务逻辑
DAO 负责数据库访问
Entities 负责数据模型
Utils 负责工具能力
Filter 负责过滤请求
Listener 负责监听应用生命周期
Tomcat 负责运行 Java Web
Nginx 负责生产环境静态资源与反向代理
MySQL 负责数据存储
```

开发时：

```text
Vue dev server → Vite proxy → 本地 Tomcat 8080 → Servlet → Service → DAO → MySQL
```

部署时：

```text
用户 → Nginx 80/443 → Vue 静态资源
用户 → Nginx /api → Tomcat 1145 → Servlet → Service → DAO → MySQL
```
