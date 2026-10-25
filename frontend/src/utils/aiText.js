/**
 * AI 输出文本标准化：
 * - 修复常见 UTF-8 乱码
 * - 去掉 Markdown / HTML / LaTeX 残留标记
 * - 统一列表、标点、项目符号与空白
 * - 保护代码块与行内代码，避免误删代码符号
 */
const CONTROL_CHARS = /[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F\u200B-\u200D\u2060\uFEFF\uFFFD]/g
const EMOJI_RANGE = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE00}-\u{FE0F}\u{20E3}]/gu
const CJK_RE = /[\u4e00-\u9fff]/g

const countCjk = (text) => (text.match(CJK_RE) || []).length

const fixMojibake = (text) => {
  if (!/[\u00C0-\u00FF]{2,}/.test(text)) return text
  try {
    const decoded = decodeURIComponent(escape(text))
    return countCjk(decoded) > countCjk(text) ? decoded : text
  } catch (e) {
    return text
  }
}

const stripMarkdown = (input) => {
  if (input === null || input === undefined) return ''
  let text = String(input)
  if (!text.trim()) return ''

  text = fixMojibake(text)
  text = text.normalize('NFC')
  text = text.replace(/\r\n?/g, '\n')
  text = text.replace(/[\u00A0\u3000]/g, ' ')

  // 1. 保护代码块和行内代码，避免后续符号清洗破坏代码
  const codeBlocks = []
  const inlineCodes = []
  text = text.replace(/```[^\n]*\n?([\s\S]*?)```/g, (match, code) => {
    codeBlocks.push(String(code).replace(/^\n+|\n+$/g, ''))
    return `\n[[CODE${codeBlocks.length - 1}]]\n`
  })
  text = text.replace(/`([^`\n]+)`/g, (match, code) => {
    inlineCodes.push(code)
    return `[[INLINE${inlineCodes.length - 1}]]`
  })

  // 2. HTML 与 Markdown 链接/图片/表格
  text = text
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/^\s*\|[\s:|-]+\|\s*$/gm, '')
    .replace(/\|/g, '｜')

  // 3. 标题、引用、分隔线、任务列表、列表符号
  text = text
    .replace(/^\s{0,3}#{1,6}\s*/gm, '')
    .replace(/^\s*>\s?/gm, '')
    .replace(/^\s*([-*_])\1{2,}\s*$/gm, '')
    .replace(/^\s*[-*+]\s*\[(?:x|X| )\]\s*/gm, '· ')
    .replace(/^\s*[-*+•●▪◦]\s+/gm, '· ')
    .replace(/^\s*(\d+)[、)）]\s*/gm, '$1. ')
    .replace(/\\([\\`*_{}[\]()#+\-.!])/g, '$1')

  // 4. 强调、删除线（只处理带边界的单个 *，避免误伤 a*b*c 这类乘法）
  text = text
    .replace(/\*\*([^*\n]+)\*\*/g, '$1')
    .replace(/(^|[\s（(，。；：！？、]|[\u4e00-\u9fff])\*([^*\n]+)\*(?=[\s，。；：！？)）]|$|[\u4e00-\u9fff])/g, '$1$2')
    .replace(/~~([^~\n]+)~~/g, '$1')

  // 5. 标点与符号统一
  text = text
    .replace(/\.{3,}/g, '…')
    .replace(/。{2,}/g, '…')
    .replace(/--+|–|—{2,}/g, '—')
    .replace(/->|=>/g, '→')
    .replace(/[•●▪◦]/g, '·')
    .replace(/([\u4e00-\u9fff])\s*,\s*/g, '$1，')
    .replace(/([\u4e00-\u9fff])\s*;\s*/g, '$1；')
    .replace(/([\u4e00-\u9fff])\s*:\s*/g, '$1：')
    .replace(/([\u4e00-\u9fff])\s*\?\s*/g, '$1？')
    .replace(/([\u4e00-\u9fff])\s*!\s*/g, '$1！')
    .replace(/([\u4e00-\u9fff])\s*\(\s*/g, '$1（')
    .replace(/\s*\)\s*(?=[\u4e00-\u9fff])/g, '）')
    .replace(EMOJI_RANGE, '')
    .replace(CONTROL_CHARS, '')

  // 6. 空白与行文结构
  text = text
    .replace(/[ \t]+/g, ' ')
    .split('\n')
    .map((line) => line.trim())
    .join('\n')
    .replace(/\n{3,}/g, '\n\n')
    .replace(/^\n+|\n+$/g, '')

  // 7. 还原行内代码与代码块（代码内容原样保留）
  text = text.replace(/\[\[INLINE(\d+)\]\]/g, (match, index) => inlineCodes[Number(index)] || '')
  text = text.replace(/\[\[CODE(\d+)\]\]/g, (match, index) => {
    const code = codeBlocks[Number(index)] || ''
    return code ? `\n${code}\n` : '\n'
  })

  return text.replace(/^\n+|\n+$/g, '').replace(/\n{3,}/g, '\n\n')
}

export const normalizeAiText = stripMarkdown
export const formatAiText = stripMarkdown

export default stripMarkdown
