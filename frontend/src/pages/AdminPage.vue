<template>
  <main class="max-w-[1200px] mx-auto px-6 py-8">
    <h1 class="text-2xl font-bold text-[#1F2937] mb-6">管理后台</h1>

    <!-- Tab 切换 -->
    <div class="flex gap-1 mb-6 border-b border-gray-200">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="px-4 py-2 text-sm font-medium rounded-t-lg transition-colors"
        :class="activeTab === tab.key
          ? 'bg-white text-[#2563EB] border border-b-white border-gray-200 -mb-px'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = tab.key"
      >
        {{ tab.emoji }} {{ tab.label }}
      </button>
    </div>

    <!-- 用户管理 -->
    <div v-if="activeTab === 'users'">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-bold text-gray-700">👥 用户列表</h2>
        <button
          class="px-3 py-1.5 text-xs bg-[#2563EB] text-white rounded-lg hover:bg-blue-600 transition-colors"
          @click="loadUsers"
        >
          刷新
        </button>
      </div>

      <div v-if="usersLoading" class="text-center py-8 text-gray-400">加载中...</div>

      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-100 text-left text-gray-400">
              <th class="py-2 px-3">ID</th>
              <th class="py-2 px-3">用户名</th>
              <th class="py-2 px-3">角色</th>
              <th class="py-2 px-3">学习阶段</th>
              <th class="py-2 px-3">识字量</th>
              <th class="py-2 px-3">经验值</th>
              <th class="py-2 px-3">最近登录</th>
              <th class="py-2 px-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="u in users"
              :key="u.userId"
              class="border-b border-gray-50 hover:bg-gray-50 transition-colors"
            >
              <td class="py-2 px-3 text-gray-500">{{ u.userId }}</td>
              <td class="py-2 px-3 font-medium text-gray-700">{{ u.username }}</td>
              <td class="py-2 px-3">
                <span
                  class="text-xs px-1.5 py-0.5 rounded-full"
                  :class="u.role === 'admin' ? 'bg-red-100 text-red-600' : u.role === 'vip' ? 'bg-yellow-100 text-yellow-700' : 'bg-gray-100 text-gray-500'"
                >{{ u.role || 'normal' }}</span>
              </td>
              <td class="py-2 px-3 text-gray-500">{{ u.studyPurpose || '-' }}</td>
              <td class="py-2 px-3 text-gray-500">{{ u.literacy || 0 }}</td>
              <td class="py-2 px-3 text-gray-500">{{ u.experience || 0 }}</td>
              <td class="py-2 px-3 text-gray-400 text-xs">{{ u.lastLogin || '-' }}</td>
              <td class="py-2 px-3">
                <div class="flex gap-1">
                  <button
                    v-if="u.role !== 'admin'"
                    class="text-xs px-2 py-0.5 rounded bg-red-50 text-red-500 hover:bg-red-100 transition-colors"
                    @click="deleteUser(u)"
                  >删除</button>
                  <button
                    v-if="u.role !== 'admin'"
                    class="text-xs px-2 py-0.5 rounded bg-gray-100 text-gray-500 hover:bg-gray-200 transition-colors"
                    @click="resetPwd(u)"
                  >重置密码</button>
                  <button
                    class="text-xs px-2 py-0.5 rounded bg-yellow-50 text-yellow-600 hover:bg-yellow-100 transition-colors"
                    @click="toggleVip(u)"
                  >{{ u.role === 'vip' ? '取消VIP' : '设VIP' }}</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 文章管理 -->
    <div v-if="activeTab === 'articles'">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-bold text-gray-700">
          📰 文章列表
          <span class="text-sm font-normal text-gray-400 ml-2">（共 {{ articleTotal }} 篇）</span>
        </h2>
        <div class="flex items-center gap-2">
          <button
            class="px-3 py-1.5 text-xs bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors"
            @click="openUploadModal"
          >
            + 上传文章
          </button>
          <span class="text-xs text-gray-400">
            第 {{ articlePage }} / {{ Math.max(1, Math.ceil(articleTotal / articlePageSize)) }} 页
          </span>
          <button
            class="px-3 py-1.5 text-xs bg-[#2563EB] text-white rounded-lg hover:bg-blue-600 transition-colors"
            @click="loadArticles"
          >
            刷新
          </button>
        </div>
      </div>

      <div v-if="articlesLoading" class="text-center py-8 text-gray-400">加载中...</div>

      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-100 text-left text-gray-400">
              <th class="py-2 px-3">ID</th>
              <th class="py-2 px-3">标题</th>
              <th class="py-2 px-3">难度</th>
              <th class="py-2 px-3">来源</th>
              <th class="py-2 px-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="a in articles"
              :key="a.articleId"
              class="border-b border-gray-50 hover:bg-gray-50 transition-colors"
            >
              <td class="py-2 px-3 text-gray-500">{{ a.articleId }}</td>
              <td class="py-2 px-3 font-medium text-gray-700 max-w-xs truncate block">{{ a.title }}</td>
              <td class="py-2 px-3">
                <span class="text-xs px-1.5 py-0.5 rounded-full bg-gray-100 text-gray-500">{{ a.difficulty || '-' }}</span>
              </td>
              <td class="py-2 px-3 text-gray-400 text-xs">{{ a.source || '-' }}</td>
              <td class="py-2 px-3">
                <button
                  class="text-xs px-2 py-0.5 rounded bg-red-50 text-red-500 hover:bg-red-100 transition-colors"
                  @click="deleteArticle(a)"
                >删除</button>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 分页 -->
        <div v-if="articleTotal > articlePageSize" class="flex items-center justify-center gap-2 mt-4 pt-4 border-t border-gray-100">
          <button
            class="px-3 py-1 text-xs rounded-lg bg-gray-100 text-gray-500 hover:bg-gray-200 transition-colors disabled:opacity-30"
            :disabled="articlePage <= 1"
            @click="articlePage--; loadArticles()"
          >上一页</button>
          <span
            v-for="p in getPageNumbers()"
            :key="p"
            class="px-2.5 py-0.5 text-xs rounded-lg cursor-pointer transition-colors"
            :class="p === articlePage ? 'bg-[#2563EB] text-white' : 'text-gray-500 hover:bg-gray-100'"
            @click="articlePage = p; loadArticles()"
          >{{ p }}</span>
          <button
            class="px-3 py-1 text-xs rounded-lg bg-gray-100 text-gray-500 hover:bg-gray-200 transition-colors disabled:opacity-30"
            :disabled="articlePage >= Math.ceil(articleTotal / articlePageSize)"
            @click="articlePage++; loadArticles()"
          >下一页</button>
        </div>
      </div>
    </div>

    <!-- 在线用户 -->
    <div v-if="activeTab === 'online'">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-bold text-gray-700">
          🟢 实时在线用户
          <span class="text-sm font-normal text-gray-400 ml-2">（{{ onlineCount }} 人在线）</span>
        </h2>
        <button
          class="px-3 py-1.5 text-xs bg-[#2563EB] text-white rounded-lg hover:bg-blue-600 transition-colors"
          @click="loadOnlineUsers"
        >
          刷新
        </button>
      </div>

      <div v-if="onlineLoading" class="text-center py-8 text-gray-400">加载中...</div>

      <div v-else-if="onlineUsers.length === 0" class="text-center py-8 text-gray-400">暂无在线用户</div>

      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-100 text-left text-gray-400">
              <th class="py-2 px-3">用户ID</th>
              <th class="py-2 px-3">用户名</th>
              <th class="py-2 px-3">角色</th>
              <th class="py-2 px-3">学习阶段</th>
              <th class="py-2 px-3">Session</th>
              <th class="py-2 px-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="u in onlineUsers"
              :key="u.sessionId"
              class="border-b border-gray-50 hover:bg-gray-50 transition-colors"
            >
              <td class="py-2 px-3 text-gray-500">{{ u.userId }}</td>
              <td class="py-2 px-3 font-medium text-gray-700">{{ u.username }}</td>
              <td class="py-2 px-3">
                <span class="text-xs px-1.5 py-0.5 rounded-full bg-gray-100 text-gray-500">{{ u.role || 'normal' }}</span>
              </td>
              <td class="py-2 px-3 text-gray-500">{{ u.studyPurpose || '-' }}</td>
              <td class="py-2 px-3 text-gray-400 text-xs font-mono">{{ u.sessionId?.substring(0, 12) }}...</td>
              <td class="py-2 px-3">
                <button
                  v-if="u.role !== 'admin'"
                  class="text-xs px-2 py-0.5 rounded bg-red-50 text-red-500 hover:bg-red-100 transition-colors"
                  @click="kickUser(u)"
                >强制下线</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 请求日志 -->
    <div v-if="activeTab === 'logs'">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-bold text-gray-700">
          📋 IP 请求日志
          <span class="text-sm font-normal text-gray-400 ml-2">（共 {{ logTotal }} 条）</span>
        </h2>
        <button
          class="px-3 py-1.5 text-xs bg-[#2563EB] text-white rounded-lg hover:bg-blue-600 transition-colors"
          @click="loadLogs"
        >
          刷新
        </button>
      </div>

      <div v-if="logsLoading" class="text-center py-8 text-gray-400">加载中...</div>

      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-100 text-left text-gray-400">
              <th class="py-2 px-3">时间</th>
              <th class="py-2 px-3">IP</th>
              <th class="py-2 px-3">方法</th>
              <th class="py-2 px-3">路径</th>
              <th class="py-2 px-3">用户</th>
              <th class="py-2 px-3">Session</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(log, idx) in logs"
              :key="idx"
              class="border-b border-gray-50 hover:bg-gray-50 transition-colors"
            >
              <td class="py-2 px-3 text-gray-400 text-xs whitespace-nowrap">{{ log.timestamp }}</td>
              <td class="py-2 px-3 text-gray-500 font-mono text-xs">{{ log.ip }}</td>
              <td class="py-2 px-3">
                <span class="text-xs px-1.5 py-0.5 rounded-full"
                  :class="log.method === 'POST' ? 'bg-blue-100 text-blue-600' : 'bg-green-100 text-green-600'"
                >{{ log.method }}</span>
              </td>
              <td class="py-2 px-3 text-gray-500 text-xs font-mono max-w-xs truncate block">{{ log.path }}</td>
              <td class="py-2 px-3 text-gray-500">{{ log.username || '-' }}</td>
              <td class="py-2 px-3 text-gray-400 text-xs font-mono">{{ (log.sessionId || '-').substring(0, 8) }}...</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Toast 提示卡片 -->
    <Teleport to="body">
      <Transition name="toast">
        <div
          v-if="toast.show"
          class="fixed top-6 left-1/2 -translate-x-1/2 z-[200] px-5 py-3 rounded-xl shadow-lg text-sm font-medium"
          :class="toast.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'"
        >
          {{ toast.message }}
        </div>
      </Transition>
    </Teleport>

    <!-- 上传文章弹窗 -->
    <Teleport to="body">
      <div
        v-if="uploadModal.show"
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/30"
        @click.self="uploadModal.show = false"
      >
        <div class="bg-white rounded-xl shadow-xl p-6 w-[560px] max-h-[90vh] overflow-y-auto">
          <h3 class="text-lg font-bold text-gray-800 mb-4">上传文章</h3>

          <!-- 标题 -->
          <label class="block text-sm text-gray-600 mb-1">标题</label>
          <textarea
            v-model="uploadModal.title"
            rows="2"
            class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] mb-3"
          ></textarea>

          <!-- 内容（TXT 文件） -->
          <label class="block text-sm text-gray-600 mb-1">文章内容（.txt 文件）</label>
          <div class="flex items-center gap-2 mb-3">
            <input
              ref="fileInput"
              type="file"
              accept=".txt"
              class="hidden"
              @change="handleFileSelect"
            />
            <button
              class="px-3 py-1.5 text-xs bg-gray-100 text-gray-600 rounded-lg hover:bg-gray-200 transition-colors"
              @click="fileInput?.click()"
            >选择文件</button>
            <span class="text-xs text-gray-400 truncate flex-1">
              {{ uploadModal.fileName || '未选择文件' }}
            </span>
          </div>

          <!-- 来源 -->
          <label class="block text-sm text-gray-600 mb-1">来源</label>
          <input
            v-model="uploadModal.source"
            type="text"
            class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] mb-3"
            placeholder="如：BBC News / 新概念英语 / 自定义"
          />

          <!-- 难度 -->
          <label class="block text-sm text-gray-600 mb-1">难度等级</label>
          <select
            v-model="uploadModal.difficulty"
            class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] mb-4"
          >
            <option value="" disabled>请选择难度</option>
            <option v-for="d in difficultyOptions" :key="d" :value="d">{{ d }}</option>
          </select>

          <!-- 操作按钮 -->
          <div class="flex justify-end gap-2">
            <button
              class="px-4 py-1.5 text-xs rounded-lg bg-gray-100 text-gray-500 hover:bg-gray-200"
              @click="uploadModal.show = false"
            >取消</button>
            <button
              class="px-4 py-1.5 text-xs rounded-lg bg-[#2563EB] text-white hover:bg-blue-600 disabled:opacity-50"
              :disabled="uploadModal.submitting"
              @click="submitArticle"
            >
              {{ uploadModal.submitting ? '提交中...' : '提交' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 确认弹窗 -->
    <Teleport to="body">
      <div
        v-if="confirmDialog.show"
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/30"
        @click.self="confirmDialog.show = false"
      >
        <div class="bg-white rounded-xl shadow-xl p-6 w-80">
          <p class="text-sm text-gray-700 mb-4">{{ confirmDialog.message }}</p>
          <div class="flex justify-end gap-2">
            <button
              class="px-4 py-1.5 text-xs rounded-lg bg-gray-100 text-gray-500 hover:bg-gray-200"
              @click="confirmDialog.show = false"
            >取消</button>
            <button
              class="px-4 py-1.5 text-xs rounded-lg bg-red-500 text-white hover:bg-red-600"
              @click="confirmDialog.onConfirm(); confirmDialog.show = false"
            >确认</button>
          </div>
        </div>
      </div>
    </Teleport>
  </main>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const userStore = useUserStore()

// ========== Tab ==========
const tabs = [
  { key: 'users', label: '用户管理', emoji: '👥' },
  { key: 'articles', label: '文章管理', emoji: '📰' },
  { key: 'online', label: '在线用户', emoji: '🟢' },
  { key: 'logs', label: 'IP日志', emoji: '📋' },
]
const activeTab = ref('users')

// ========== 用户管理 ==========
const users = ref([])
const usersLoading = ref(false)

async function loadUsers() {
  usersLoading.value = true
  try {
    const adminUserId = userStore.user?.userId
    const data = await request.get('/admin/user/list', { params: { adminUserId } })
    if (data.success) {
      users.value = data.users || []
    }
  } catch (e) {
    console.error('加载用户列表失败:', e)
  } finally {
    usersLoading.value = false
  }
}

// ========== 在线用户 ==========
const onlineUsers = ref([])
const onlineCount = ref(0)
const onlineLoading = ref(false)

async function loadOnlineUsers() {
  onlineLoading.value = true
  try {
    const adminUserId = userStore.user?.userId
    const data = await request.get('/admin/monitor/online-users', { params: { adminUserId } })
    if (data.success) {
      onlineUsers.value = data.onlineUsers || []
      onlineCount.value = data.onlineCount || 0
    }
  } catch (e) {
    console.error('加载在线用户失败:', e)
  } finally {
    onlineLoading.value = false
  }
}

// ========== 请求日志 ==========
const logs = ref([])
const logTotal = ref(0)
const logsLoading = ref(false)

async function loadLogs() {
  logsLoading.value = true
  try {
    const adminUserId = userStore.user?.userId
    const data = await request.get('/admin/monitor/request-logs', { params: { adminUserId, limit: 200 } })
    if (data.success) {
      logs.value = data.logs || []
      logTotal.value = data.totalCount || 0
    }
  } catch (e) {
    console.error('加载请求日志失败:', e)
  } finally {
    logsLoading.value = false
  }
}

// ========== Toast ==========
const toast = ref({ show: false, message: '', type: 'success' })
let toastTimer = null

function showToast(message, type = 'success') {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, message, type }
  toastTimer = setTimeout(() => { toast.value.show = false }, 2500)
}

// ========== 上传文章弹窗 ==========
const difficultyOptions = ['初中', '高中', '四级', '六级', '考研', '托福', '期刊', '原著']
const fileInput = ref(null)
const uploadModal = ref({
  show: false,
  title: '',
  fileName: '',
  fileContent: '',
  source: '',
  difficulty: '',
  submitting: false,
})

function openUploadModal() {
  uploadModal.value = {
    show: true,
    title: '',
    fileName: '',
    fileContent: '',
    source: '',
    difficulty: '',
    submitting: false,
  }
}

function handleFileSelect(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploadModal.value.fileName = file.name
  const reader = new FileReader()
  reader.onload = () => {
    uploadModal.value.fileContent = reader.result
  }
  reader.readAsText(file, 'UTF-8')
}

async function submitArticle() {
  const m = uploadModal.value
  if (!m.title.trim()) return showToast('请输入标题', 'error')
  if (!m.fileContent.trim()) return showToast('请选择文章内容文件', 'error')
  if (!m.source.trim()) return showToast('请输入来源', 'error')
  if (!m.difficulty) return showToast('请选择难度等级', 'error')

  m.submitting = true
  try {
    const formData = new FormData()
    formData.append('adminUserId', String(userStore.user?.userId))
    formData.append('title', m.title.trim())
    formData.append('content', m.fileContent)
    formData.append('source', m.source.trim())
    formData.append('difficulty', m.difficulty)
    const data = await request.post('/admin/article/create', formData)
    if (data.success) {
      showToast('文章上传成功')
      uploadModal.value.show = false
      loadArticles()
    } else {
      showToast(data.message || '上传失败', 'error')
    }
  } catch (e) {
    console.error('上传文章失败:', e)
    const msg = e?.response?.data?.message || e?.message || '上传失败'
    showToast(msg, 'error')
  } finally {
    m.submitting = false
  }
}

// ========== 确认弹窗 ==========
const confirmDialog = ref({
  show: false,
  message: '',
  onConfirm: () => {},
})

function showConfirm(message, onConfirm) {
  confirmDialog.value = { show: true, message, onConfirm }
}

// ========== 操作 ==========
const adminUserId = () => userStore.user?.userId

async function deleteUser(u) {
  showConfirm(`确定要删除用户 "${u.username}"（ID: ${u.userId}）吗？此操作不可撤销。`, async () => {
    try {
      const params = new URLSearchParams()
      params.append('adminUserId', String(adminUserId()))
      params.append('userId', String(u.userId))
      const data = await request.post('/admin/user/delete', params)
      if (data.success) {
        users.value = users.value.filter(x => x.userId !== u.userId)
      } else {
        alert(data.message || '删除失败')
      }
    } catch (e) {
      console.error('删除用户失败:', e)
      alert('删除失败，请稍后重试')
    }
  })
}

async function resetPwd(u) {
  showConfirm(`确定重置用户 "${u.username}" 的密码为 123456 吗？`, async () => {
    try {
      const params = new URLSearchParams()
      params.append('adminUserId', String(adminUserId()))
      params.append('userId', String(u.userId))
      const data = await request.post('/admin/user/reset-password', params)
      alert(data.message || '密码已重置')
    } catch (e) {
      console.error('重置密码失败:', e)
      alert('操作失败')
    }
  })
}

async function toggleVip(u) {
  const newRole = u.role === 'vip' ? 'normal' : 'vip'
  try {
    const params = new URLSearchParams()
    params.append('adminUserId', String(adminUserId()))
    params.append('userId', String(u.userId))
    params.append('role', newRole)
    const data = await request.post('/admin/user/set-role', params)
    if (data.success) {
      u.role = newRole
    } else {
      alert(data.message || '操作失败')
    }
  } catch (e) {
    console.error('设置角色失败:', e)
    alert('操作失败')
  }
}

async function kickUser(u) {
  showConfirm(`确定强制下线用户 "${u.username}" 吗？`, async () => {
    try {
      const params = new URLSearchParams()
      params.append('adminUserId', String(adminUserId()))
      params.append('targetUserId', String(u.userId))
      const data = await request.post('/admin/monitor/kick-user', params)
      if (data.success) {
        onlineUsers.value = onlineUsers.value.filter(x => x.userId !== u.userId)
        onlineCount.value = Math.max(0, onlineCount.value - 1)
      }
      alert(data.message || '操作完成')
    } catch (e) {
      console.error('强制下线失败:', e)
      alert('操作失败')
    }
  })
}

// ========== 文章管理 ==========
const articles = ref([])
const articleTotal = ref(0)
const articlePage = ref(1)
const articlePageSize = 10
const articlesLoading = ref(false)

async function loadArticles() {
  articlesLoading.value = true
  try {
    const adminUserId = userStore.user?.userId
    const data = await request.get('/admin/article/list', {
      params: { adminUserId, page: articlePage.value, pageSize: articlePageSize }
    })
    if (data.success) {
      articles.value = data.articles || []
      articleTotal.value = data.total || 0
    }
  } catch (e) {
    console.error('加载文章列表失败:', e)
  } finally {
    articlesLoading.value = false
  }
}

function getPageNumbers() {
  const totalPages = Math.max(1, Math.ceil(articleTotal.value / articlePageSize))
  const pages = []
  const start = Math.max(1, articlePage.value - 2)
  const end = Math.min(totalPages, articlePage.value + 2)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
}

async function deleteArticle(a) {
  showConfirm(`确定要删除文章 "${a.title}"（ID: ${a.articleId}）吗？此操作不可撤销，将级联删除关联数据。`, async () => {
    try {
      const params = new URLSearchParams()
      params.append('adminUserId', String(adminUserId()))
      params.append('articleId', String(a.articleId))
      const data = await request.post('/admin/article/delete', params)
      if (data.success) {
        articles.value = articles.value.filter(x => x.articleId !== a.articleId)
        articleTotal.value = Math.max(0, articleTotal.value - 1)
        // 同步清除浏览历史中该文章的记录
        import('@/utils/historyDB').then(({ removeFromHistory }) => {
          removeFromHistory(a.articleId).catch(() => {})
        })
      } else {
        alert(data.message || '删除失败')
      }
    } catch (e) {
      console.error('删除文章失败:', e)
      alert('删除失败，请稍后重试')
    }
  })
}

// ========== 监听 Tab 切换自动加载数据 ==========
// （简单实现：切换到文章 tab 时首次加载）
watch(activeTab, (tab) => {
  if (tab === 'articles' && articles.value.length === 0) {
    loadArticles()
  }
  if (tab === 'online' && onlineUsers.value.length === 0) {
    loadOnlineUsers()
  }
  if (tab === 'logs' && logs.value.length === 0) {
    loadLogs()
  }
})

// 初始化加载
loadUsers()
</script>

<style scoped>
.toast-enter-active { transition: all 0.3s ease-out; }
.toast-leave-active { transition: all 0.25s ease-in; }
.toast-enter-from { opacity: 0; transform: translateY(-16px); }
.toast-leave-to { opacity: 0; transform: translateY(-12px); }
</style>
