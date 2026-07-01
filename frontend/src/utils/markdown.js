/**
 * 简易 Markdown → HTML 转换（仅支持行内样式）
 * 处理：**加粗**  *斜体*  ~~删除线~~
 */
export function parseInlineMarkdown(text) {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  // 加粗 **text**
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  // 斜体 *text*
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  // 删除线 ~~text~~
  html = html.replace(/~~(.+?)~~/g, '<del>$1</del>')
  return html
}
