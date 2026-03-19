import type { Directive, DirectiveBinding } from 'vue'

type LabelValue = string | number | null | undefined

interface AdaptiveSelectWidthOptions {
  labels?: LabelValue[]
  minWidth?: number
  maxWidth?: number
  extraWidth?: number
}

type BindingValue = LabelValue[] | AdaptiveSelectWidthOptions | null | undefined

const DEFAULT_MIN_WIDTH = 88
const DEFAULT_MAX_WIDTH = 220
const DEFAULT_EXTRA_WIDTH = 64

const canvas = document.createElement('canvas')
const context = canvas.getContext('2d')

const normalizeLabels = (labels: LabelValue[]): string[] =>
  labels
    .map(label => (label === null || label === undefined ? '' : String(label).trim()))
    .filter(label => label.length > 0)

const parseBinding = (value: BindingValue) => {
  if (Array.isArray(value)) {
    return {
      labels: normalizeLabels(value),
      minWidth: DEFAULT_MIN_WIDTH,
      maxWidth: DEFAULT_MAX_WIDTH,
      extraWidth: DEFAULT_EXTRA_WIDTH
    }
  }

  if (value && typeof value === 'object') {
    return {
      labels: normalizeLabels(value.labels ?? []),
      minWidth: value.minWidth ?? DEFAULT_MIN_WIDTH,
      maxWidth: value.maxWidth ?? DEFAULT_MAX_WIDTH,
      extraWidth: value.extraWidth ?? DEFAULT_EXTRA_WIDTH
    }
  }

  return {
    labels: [] as string[],
    minWidth: DEFAULT_MIN_WIDTH,
    maxWidth: DEFAULT_MAX_WIDTH,
    extraWidth: DEFAULT_EXTRA_WIDTH
  }
}

const getFont = (el: HTMLElement) => {
  const wrapper = el.querySelector('.el-select__wrapper') as HTMLElement | null
  const style = window.getComputedStyle(wrapper ?? el)
  const fontSize = style.fontSize || '14px'
  const fontFamily = style.fontFamily || 'sans-serif'
  const fontWeight = style.fontWeight || '400'
  return `${fontWeight} ${fontSize} ${fontFamily}`
}

const measureTextWidth = (text: string, font: string): number => {
  if (!context) {
    return text.length * 14
  }
  context.font = font
  return context.measureText(text).width
}

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

const applyWidth = (el: HTMLElement, binding: DirectiveBinding<BindingValue>) => {
  const { labels, minWidth, maxWidth, extraWidth } = parseBinding(binding.value)

  if (!labels.length) {
    el.style.removeProperty('width')
    return
  }

  const font = getFont(el)
  const maxLabelWidth = labels.reduce((currentMax, label) => {
    const width = measureTextWidth(label, font)
    return width > currentMax ? width : currentMax
  }, 0)

  const finalWidth = clamp(Math.ceil(maxLabelWidth + extraWidth), minWidth, maxWidth)
  el.style.width = `${finalWidth}px`
}

export const adaptiveSelectWidth: Directive<HTMLElement, BindingValue> = {
  mounted(el, binding) {
    queueMicrotask(() => applyWidth(el, binding))
  },
  updated(el, binding) {
    queueMicrotask(() => applyWidth(el, binding))
  }
}
