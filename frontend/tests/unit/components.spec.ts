import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import EmptyState from '@/components/EmptyState.vue'
import ScoreBadge from '@/components/ScoreBadge.vue'

/**
 * Stub el-button so we don't pull Element Plus' CSS into the test bundle
 * (jsdom + Vitest can't parse .css imports without an extra loader).
 */
const stubs = {
  'el-button': {
    template: '<button type="button" @click="$emit(\'click\', $event)"><slot /></button>',
  },
}

describe('EmptyState.vue', () => {
  it('renders title and description', () => {
    const wrapper = mount(EmptyState, {
      props: { title: '暂无数据', description: '快去添加吧' },
      global: { stubs },
    })
    expect(wrapper.text()).toContain('暂无数据')
    expect(wrapper.text()).toContain('快去添加吧')
  })

  it('emits `action` when action button clicked', async () => {
    const wrapper = mount(EmptyState, {
      props: { title: '空', actionText: '新增' },
      global: { stubs },
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('action')).toBeTruthy()
    expect(wrapper.emitted('action')!.length).toBeGreaterThanOrEqual(1)
  })

  it('does not render action button when actionText is empty', () => {
    const wrapper = mount(EmptyState, {
      props: { title: '空' },
      global: { stubs },
    })
    expect(wrapper.find('button').exists()).toBe(false)
  })
})

describe('ScoreBadge.vue', () => {
  it('shows raw score for passing scores', () => {
    const wrapper = mount(ScoreBadge, { props: { score: 85 } })
    expect(wrapper.text()).toContain('85')
    expect(wrapper.text()).not.toContain('不及格')
    expect(wrapper.classes()).toContain('is-passing')
  })

  it('shows "不及格" tag and is-failing class when score < 60', () => {
    const wrapper = mount(ScoreBadge, { props: { score: 45 } })
    expect(wrapper.text()).toContain('45')
    expect(wrapper.text()).toContain('不及格')
    expect(wrapper.classes()).toContain('is-failing')
  })

  it('respects explicit isFailing prop', () => {
    const wrapper = mount(ScoreBadge, { props: { score: 100, isFailing: true } })
    expect(wrapper.classes()).toContain('is-failing')
    expect(wrapper.text()).toContain('不及格')
  })

  it('allows custom passLine', () => {
    const wrapper = mount(ScoreBadge, { props: { score: 65, passLine: 70 } })
    expect(wrapper.classes()).toContain('is-failing')
  })
})
