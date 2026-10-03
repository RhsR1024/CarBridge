const root = document.getElementById('app');
const variant = document.body.dataset.variant || 'a';
root.className = 'app variant-' + variant;
root.innerHTML = `
  <header class="header">
    <h1>MediaBridge</h1>
    <div class="top-state">界面预览 · 状态数据仅作排版示意</div>
  </header>
  <main class="layout">
    <section class="panel hero" aria-label="媒体桥接和接入模式">
      <div class="hero-grid">
        <div>
          <div class="setting">
            <div><strong>启用媒体桥接</strong><small>默认开启，关闭后记住你的选择</small></div>
            <label class="switch" aria-label="启用媒体桥接"><input id="bridge" type="checkbox" checked><span class="track"></span></label>
          </div>
        </div>
        <div>
          <h2>接入模式</h2>
          <div class="mode-row" role="radiogroup" aria-label="接入模式">
            <label class="mode-choice"><input type="radio" name="mode" value="standard" checked><span>MediaBridge 标准</span></label>
            <label class="mode-choice disabled"><input type="radio" name="mode" value="f25"><span>F25 兼容 <small>实验性，独立接入未实现</small></span></label>
          </div>
        </div>
      </div>
    </section>

    <section class="panel favorite" aria-label="收藏设置">
      <h2>收藏</h2>
      <div class="setting">
        <div><strong>显示收藏</strong><small>在车机媒体卡片中显示收藏入口</small></div>
        <label class="switch" aria-label="显示收藏"><input id="favorite" type="checkbox" checked><span class="track"></span></label>
      </div>
      <p class="note">当前播放器的收藏能力尚未确认时，红星入口可能不会出现。</p>
    </section>

    <section class="panel lyrics" aria-label="歌词设置">
      <h2>歌词</h2>
      <div class="setting">
        <div><strong>显示歌词</strong><small>获取歌词并同步到车机卡片</small></div>
        <label class="switch" aria-label="显示歌词"><input id="lyrics" type="checkbox" checked><span class="track"></span></label>
      </div>
      <div class="tune">
        <div class="tune-head"><strong>歌词同步微调</strong><span class="tune-value" id="tuneValue">偏移 0ms</span></div>
        <div class="tune-actions">
          <button class="btn" type="button" data-tune="-100">－100ms 歌词延后</button>
          <button class="btn" type="button" data-tune="100">＋100ms 歌词提前</button>
          <button class="btn secondary" type="button" data-tune="reset">归零</button>
        </div>
        <p class="small" style="margin-top:12px">范围 −3000～+3000ms · 当前歌曲立即生效</p>
      </div>
      <button class="btn secondary full" type="button" data-demo style="margin-top:13px">重新获取当前歌词</button>
    </section>

    <section class="panel ignore" aria-label="媒体应用忽略名单">
      <h2>媒体应用忽略名单</h2>
      <p class="section-help">勾选的应用不会参与自动选择</p>
      <div class="ignored-item">
        <span><strong>酷我音乐</strong><small>cn.kuwo.kwmusiccar · 示例条目</small></span>
        <input aria-label="忽略酷我音乐" type="checkbox">
      </div>
      <div class="button-grid">
        <button class="btn" type="button" data-demo>忽略当前播放器</button>
        <button class="btn secondary" type="button" data-demo>清空忽略名单</button>
      </div>
    </section>

    <section class="panel status" aria-label="桥接与播放器状态">
      <h2>当前状态与播放器</h2>
      <p class="current"><strong>酷我音乐</strong>　cn.kuwo.kwmusiccar　·　自动选择</p>
      <div class="status-grid">
        <div class="datum"><span class="key">通知访问</span><span class="value">已授权</span></div>
        <div class="datum"><span class="key">可用性</span><span class="value">播放器已连接</span></div>
        <div class="datum"><span class="key">输入</span><span class="value">已发现 · 酷我音乐</span></div>
        <div class="datum"><span class="key">输出</span><span class="value">标准通道已注册</span></div>
        <div class="datum"><span class="key">目标模式 / 实际模式</span><span class="value">MediaBridge 标准 / MediaBridge 标准</span></div>
        <div class="datum"><span class="key">连接代次</span><span class="value">2</span></div>
        <div class="datum"><span class="key">用户</span><span class="value">11</span></div>
        <div class="datum"><span class="key">当前播放</span><span class="value">歌曲和歌手信息显示在这里</span></div>
        <div class="datum"><span class="key">当前会话 / 历史播放器</span><span class="value">酷我音乐 / 酷我音乐</span></div>
        <div class="datum"><span class="key">已忽略</span><span class="value">—</span></div>
      </div>
      <div class="button-grid" style="margin-top:16px">
        <button class="btn" type="button" data-demo>固定当前播放器</button>
        <button class="btn secondary" type="button" data-demo>恢复自动选择</button>
        <button class="btn secondary" type="button" data-demo>打开当前播放器</button>
      </div>
    </section>

    <section class="panel advanced" aria-label="其他设置">
      <h2>其他设置</h2>
      <div class="button-grid">
        <button class="btn" type="button" data-demo>去开启通知访问</button>
        <button class="btn" type="button" data-demo>重新检查通知访问</button>
        <button class="btn" type="button" data-demo>重新连接车机媒体中心</button>
        <button class="btn blue" type="button" data-demo>导出诊断日志</button>
      </div>
      <p class="about">关于 · 由 RhsR1024 开发 · 版本信息</p>
    </section>
  </main>
  <div class="preview-toast" id="toast" role="status" hidden>这是界面预览，操作不会修改车机设置</div>
`;

if (variant === 'b') {
  const layout = root.querySelector('.layout');
  const primary = document.createElement('div');
  const secondary = document.createElement('div');
  primary.className = 'lane';
  secondary.className = 'lane';
  ['hero', 'favorite', 'lyrics'].forEach(name => primary.appendChild(layout.querySelector('.' + name)));
  ['ignore', 'status', 'advanced'].forEach(name => secondary.appendChild(layout.querySelector('.' + name)));
  layout.append(primary, secondary);
}

let tune = 0;
let toastTimer;
function showToast() {
  const toast = document.getElementById('toast');
  toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.hidden = true; }, 2400);
}
root.querySelectorAll('[data-demo]').forEach(button => button.addEventListener('click', showToast));
root.querySelectorAll('[data-tune]').forEach(button => button.addEventListener('click', () => {
  tune = button.dataset.tune === 'reset' ? 0 : Math.max(-3000, Math.min(3000, tune + Number(button.dataset.tune)));
  document.getElementById('tuneValue').textContent = `偏移 ${tune > 0 ? '+' : ''}${tune}ms`;
}));
