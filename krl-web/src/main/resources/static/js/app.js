/*
 * KRL Parser 前端入口。
 *
 * 该文件同时负责：
 * 1. 运行模式状态管理；
 * 2. 异步分析任务提交与轮询；
 * 3. Cytoscape 图谱渲染；
 * 4. 响应式桌面/移动端布局切换；
 * 5. Config 在线编辑与 Excel 下载。
 */

lucide.createIcons();

try {
    if (typeof cytoscapeDagre !== 'undefined') {
        cytoscape.use(cytoscapeDagre);
    }
} catch (error) {
    console.error('Cytoscape Dagre 插件注册失败', error);
}

let cy = null;
let currentView = 'line';
let currentRenderMode = 'graph';
let selectedRobotIndex = 0;
let parsedRobots = [];
let popoverDragState = null;
let pendingNodeTap = null;
let activeTask = null;
let taskPollTimer = null;
let headerMenuOpen = false;

const NODE_DOUBLE_TAP_DELAY = 260;

const runtimeState = {
    runtimeMode: 'desktop',
    analysisMode: 'sync'
};

const uploadState = {
    zipFiles: [],
    configPath: '',
    configContent: '',
    configLoaded: false,
    lastSuccessfulTaskId: null,
    lastSuccessfulSignature: null
};

const dom = {
    appHeader: document.getElementById('appHeader'),
    mainShell: document.getElementById('mainShell'),
    cy: document.getElementById('cy'),
    listView: document.getElementById('listView'),
    fileUploadButton: document.getElementById('fileUploadButton'),
    fileUpload: document.getElementById('fileUpload'),
    fileUploadText: document.getElementById('fileUploadText'),
    headerMenuToggle: document.getElementById('headerMenuToggle'),
    headerMenuPanel: document.getElementById('headerMenuPanel'),
    configButton: document.getElementById('configButton'),
    startAnalysisBtn: document.getElementById('startAnalysisBtn'),
    downloadExcelBtn: document.getElementById('downloadExcelBtn'),
    statusBlock: document.getElementById('statusBlock'),
    runtimeStatusBadge: document.getElementById('runtimeStatusBadge'),
    taskStatusBadge: document.getElementById('taskStatusBadge'),
    loader: document.getElementById('loader'),
    loaderText: document.getElementById('loaderText'),
    configModal: document.getElementById('configModal'),
    configCloseBtn: document.getElementById('configCloseBtn'),
    configReloadBtn: document.getElementById('configReloadBtn'),
    configApplyBtn: document.getElementById('configApplyBtn'),
    configTextarea: document.getElementById('configTextarea'),
    configPathText: document.getElementById('configPathText'),
    infoSidebar: document.getElementById('infoSidebar'),
    infoSidebarClose: document.getElementById('infoSidebarClose'),
    sidebarTrigger: document.getElementById('sidebarTrigger'),
    metaName: document.getElementById('metaName'),
    metaArchiveName: document.getElementById('metaArchiveName'),
    metaVersion: document.getElementById('metaVersion'),
    metaDate: document.getElementById('metaDate'),
    techPackList: document.getElementById('techPackList'),
    nodeDetailEmpty: document.getElementById('nodeDetailEmpty'),
    nodeDetailContent: document.getElementById('nodeDetailContent'),
    nodeName: document.getElementById('nodeName'),
    nodeType: document.getElementById('nodeType'),
    nodePath: document.getElementById('nodePath'),
    nodeCreateTime: document.getElementById('nodeCreateTime'),
    nodeModifyTime: document.getElementById('nodeModifyTime'),
    nodeRelevantInfo: document.getElementById('nodeRelevantInfo'),
    btnLineView: document.getElementById('btnLineView'),
    btnCarView: document.getElementById('btnCarView'),
    renderModeSwitcher: document.getElementById('renderModeSwitcher'),
    btnGraphMode: document.getElementById('btnGraphMode'),
    btnListMode: document.getElementById('btnListMode'),
    nodeControlPanel: document.getElementById('nodeControlPanel'),
    nodeControlTrigger: document.getElementById('nodeControlTrigger'),
    nodeControlClose: document.getElementById('nodeControlClose'),
    nodePopover: document.getElementById('nodePopover')
};

const nodeTypeGroups = [
    {label: 'Cell', types: ['CEll']},
    {label: 'P程序', types: ['P_PROGRAM']},
    {label: '虚拟节点', types: ['VIRTUAL']},
    {label: '车型代码', types: ['CAR_CODE']},
    {label: '车型程序', types: ['CAR_PROGRAM']},
    {label: '轨迹程序', types: ['ROUTE_PROCESS']}
];

const nodeTypeBaseSizes = {
    CEll: {width: 86, height: 86, fontSize: 12, textMaxWidth: 96},
    CAR_CODE: {width: 86, height: 86, fontSize: 12, textMaxWidth: 96},
    P_PROGRAM: {width: 128, height: 48, fontSize: 12, textMaxWidth: 108},
    VIRTUAL: {width: 128, height: 48, fontSize: 12, textMaxWidth: 108},
    CAR_PROGRAM: {width: 128, height: 48, fontSize: 12, textMaxWidth: 108},
    ROUTE_PROCESS: {width: 128, height: 48, fontSize: 12, textMaxWidth: 108},
    ROBOT_ROOT: {width: 132, height: 86, fontSize: 14, textMaxWidth: 120}
};

const nodeTypeScale = new Map();
const activeTypeSelections = new Set();

/**
 * 判断当前是否为移动端宽度。
 *
 * @returns {boolean} true 表示当前处于小屏布局
 */
function isMobileViewport() {
    return window.matchMedia('(max-width: 768px)').matches;
}

/**
 * 根据头部真实高度更新主区域偏移，避免响应式换行后内容被遮挡。
 */
function updateLayoutMetrics() {
    const headerHeight = dom.appHeader ? dom.appHeader.offsetHeight : 112;
    document.documentElement.style.setProperty('--header-height', `${headerHeight}px`);
}

/**
 * 设置顶部操作菜单显隐。
 *
 * @param {boolean} open true 表示展开
 */
function setHeaderMenuOpen(open) {
    headerMenuOpen = Boolean(open);
    if (dom.headerMenuPanel) {
        dom.headerMenuPanel.classList.toggle('hidden', !headerMenuOpen);
        dom.headerMenuPanel.setAttribute('aria-hidden', String(!headerMenuOpen));
    }
    if (dom.headerMenuToggle) {
        dom.headerMenuToggle.classList.toggle('active', headerMenuOpen);
        dom.headerMenuToggle.setAttribute('aria-expanded', String(headerMenuOpen));
    }
}

/**
 * 切换顶部操作菜单显隐。
 */
function toggleHeaderMenu() {
    setHeaderMenuOpen(!headerMenuOpen);
}

/**
 * 获取当前选中的机器人。
 *
 * @returns {object|null} 当前机器人对象
 */
function getSelectedRobot() {
    if (!Array.isArray(parsedRobots) || parsedRobots.length === 0) {
        return null;
    }
    if (selectedRobotIndex < 0 || selectedRobotIndex >= parsedRobots.length) {
        selectedRobotIndex = 0;
    }
    return parsedRobots[selectedRobotIndex];
}

/**
 * 更新全局加载遮罩状态。
 *
 * @param {boolean} visible 是否显示
 * @param {string} message  提示文案
 */
function setLoading(visible, message) {
    if (!dom.loader || !dom.loaderText) {
        return;
    }
    if (message) {
        dom.loaderText.textContent = message;
    }
    dom.loader.classList.toggle('hidden', !visible);
}

/**
 * 更新头部运行模式标签。
 */
function refreshRuntimeBadge() {
    if (!dom.runtimeStatusBadge) {
        return;
    }
    dom.runtimeStatusBadge.className = 'status-badge';
    dom.runtimeStatusBadge.classList.add(runtimeState.runtimeMode === 'server' ? 'info' : 'success');
    dom.runtimeStatusBadge.textContent = runtimeState.runtimeMode === 'server' ? 'Server 模式' : 'Desktop 模式';
}

/**
 * 判断当前是否启用异步任务分析模式。
 *
 * @returns {boolean} true 表示走 `/api/analysis/tasks/**`
 */
function isAsyncAnalysisMode() {
    return runtimeState.analysisMode === 'async';
}

/**
 * 刷新与模式相关的界面显隐。
 * <p>
 * 目标是把“桌面同步模式”和“服务器异步模式”彻底隔离：
 * 1. 桌面同步模式默认隐藏状态区；
 * 2. 服务器异步模式展示运行模式与任务状态；
 * 3. 由于系统已移除登录认证，因此不再展示任何登录相关控件。
 */
function refreshModeSpecificUi() {
    const showRuntimeBadge = runtimeState.runtimeMode === 'server';
    const showTaskBadge = isAsyncAnalysisMode();
    const showStatusBlock = showRuntimeBadge || showTaskBadge;

    if (dom.statusBlock) {
        dom.statusBlock.classList.toggle('hidden', !showStatusBlock);
    }
    if (dom.runtimeStatusBadge) {
        dom.runtimeStatusBadge.classList.toggle('hidden', !showRuntimeBadge);
    }
    if (dom.taskStatusBadge) {
        dom.taskStatusBadge.classList.toggle('hidden', !showTaskBadge);
    }
}

/**
 * 更新任务状态标签。
 *
 * @param {string} text 状态文案
 * @param {string} tone 状态色调
 */
function setTaskBadge(text, tone = 'neutral') {
    if (!dom.taskStatusBadge) {
        return;
    }
    dom.taskStatusBadge.className = 'status-badge';
    dom.taskStatusBadge.classList.add(tone);
    dom.taskStatusBadge.textContent = text;
}

/**
 * 刷新操作按钮状态。
 * <p>
 * 启动分析与 Excel 下载都依赖：
 * 1. 已完成配置加载；
 * 2. 至少存在一个 zip 文件；
 * 3. 当前没有正在执行的后台任务。
 */
function updateActionButtons() {
    const baseReady = uploadState.configLoaded && uploadState.zipFiles.length > 0;
    const taskRunning = activeTask !== null;
    const enabled = baseReady && !taskRunning;

    [dom.startAnalysisBtn, dom.downloadExcelBtn].forEach((button) => {
        if (!button) {
            return;
        }
        button.disabled = !enabled;
        button.classList.toggle('action-disabled', !enabled);
    });
}

/**
 * 统一封装 fetch。
 *
 * @param {string} url     请求地址
 * @param {object} options fetch 参数
 * @returns {Promise<Response>} 原始响应对象
 */
async function apiFetch(url, options = {}) {
    return fetch(url, {
        credentials: 'same-origin',
        ...options
    });
}

/**
 * 从错误响应中提取可读消息。
 *
 * @param {Response} response HTTP 响应对象
 * @returns {Promise<string>} 友好错误文案
 */
async function extractErrorMessage(response) {
    let payloadText = '';
    try {
        payloadText = await response.text();
    } catch (error) {
        console.warn('读取错误响应失败', error);
    }
    if (!payloadText) {
        return `HTTP ${response.status}`;
    }
    try {
        const payload = JSON.parse(payloadText);
        if (payload && payload.message) {
            return payload.message;
        }
    } catch (_) {
        // 非 JSON 响应直接返回原文本
    }
    return payloadText;
}

/**
 * 计算当前输入签名，用于判断已有任务结果是否仍然对应当前上传内容。
 *
 * @returns {string} 当前输入签名
 */
function computeCurrentInputSignature() {
    const fileSignature = uploadState.zipFiles
        .map((file) => `${file.name}:${file.size}:${file.lastModified}`)
        .join('|');
    return `${fileSignature}::${uploadState.configContent || ''}`;
}

/**
 * 打开 Config 编辑弹窗。
 */
function openConfigModal() {
    if (!dom.configModal) {
        return;
    }
    dom.configTextarea.value = uploadState.configContent || '';
    dom.configModal.classList.remove('hidden');
}

/**
 * 关闭 Config 编辑弹窗。
 */
function closeConfigModal() {
    if (!dom.configModal) {
        return;
    }
    dom.configModal.classList.add('hidden');
}

/**
 * 打开或关闭详情侧栏。
 *
 * @param {boolean} visible 是否显示
 */
function setSidebarVisible(visible) {
    if (!dom.infoSidebar) {
        return;
    }
    dom.infoSidebar.classList.toggle('panel-hidden', !visible);
    dom.infoSidebar.setAttribute('aria-hidden', String(!visible));
    refreshSidebarTriggerVisibility();
}

/**
 * 打开或关闭节点控制面板。
 *
 * @param {boolean} visible 是否显示
 */
function setNodeControlVisible(visible) {
    if (!dom.nodeControlPanel) {
        return;
    }
    dom.nodeControlPanel.classList.toggle('panel-hidden', !visible);
    dom.nodeControlPanel.setAttribute('aria-hidden', String(!visible));
    if (dom.nodeControlTrigger) {
        dom.nodeControlTrigger.classList.toggle('hidden', visible || currentView !== 'car');
    }
}

/**
 * 清空节点详情区。
 */
function clearNodeDetail() {
    if (!dom.nodeDetailEmpty || !dom.nodeDetailContent) {
        return;
    }
    dom.nodeDetailEmpty.classList.remove('hidden');
    dom.nodeDetailContent.classList.add('hidden');
    dom.nodeName.textContent = '--';
    dom.nodeType.textContent = '--';
    dom.nodePath.textContent = '--';
    dom.nodeCreateTime.textContent = '--';
    dom.nodeModifyTime.textContent = '--';
    dom.nodeRelevantInfo.textContent = '--';
}

/**
 * 刷新“信息”按钮显隐。
 *
 * 机器人摘要在桌面端和移动端都应可达，因此只要存在机器人数据且侧栏未展开，
 * 就显示入口按钮，而不是限制在某一个图谱视图中。
 */
function refreshSidebarTriggerVisibility() {
    if (!dom.sidebarTrigger) {
        return;
    }
    const hasRobotData = parsedRobots.length > 0 && !!getSelectedRobot();
    const sidebarVisible = dom.infoSidebar && !dom.infoSidebar.classList.contains('panel-hidden');
    dom.sidebarTrigger.classList.toggle('hidden', !hasRobotData || sidebarVisible);
}

/**
 * 更新节点详情区。
 *
 * @param {object} nodeData 节点数据
 */
function updateNodeDetail(nodeData) {
    if (!nodeData || !dom.nodeDetailEmpty || !dom.nodeDetailContent) {
        clearNodeDetail();
        return;
    }
    const propertyMap = nodeData.propertyMap || {};
    dom.nodeDetailEmpty.classList.add('hidden');
    dom.nodeDetailContent.classList.remove('hidden');
    dom.nodeName.textContent = nodeData.label || nodeData.id || '--';
    dom.nodeType.textContent = nodeData.type || '--';
    dom.nodePath.textContent = propertyMap.srcFilePath || '--';
    dom.nodeCreateTime.textContent = propertyMap.createTime || '--';
    dom.nodeModifyTime.textContent = propertyMap.modifyTime || '--';
    dom.nodeRelevantInfo.textContent = nodeData.relevantInfo || '--';
}

/**
 * 更新机器人详情区。
 *
 * @param {object|null} robotData 当前机器人
 */
function updateRobotDetail(robotData) {
    if (!robotData) {
        dom.metaName.textContent = '--';
        dom.metaArchiveName.textContent = '--';
        dom.metaVersion.textContent = '--';
        dom.metaDate.textContent = '--';
        dom.techPackList.innerHTML = '<div class="info-empty">暂无数据</div>';
        return;
    }

    dom.metaName.textContent = robotData.robotName || '--';
    dom.metaArchiveName.textContent = robotData.archiveName || '--';
    dom.metaVersion.textContent = robotData.version || '--';
    dom.metaDate.textContent = robotData.archiveDate || '--';
    dom.techPackList.innerHTML = '';
    const techPacks = robotData.techPacks || [];
    if (techPacks.length === 0) {
        dom.techPackList.innerHTML = '<div class="info-empty">暂无数据</div>';
        return;
    }
    techPacks.forEach((pack) => {
        const item = document.createElement('div');
        item.className = 'tech-pack-item';
        item.innerHTML = `
            <span>${escapeHtml(pack.name || '--')}</span>
            <span class="info-mono">${escapeHtml(pack.version || '')}</span>
        `;
        dom.techPackList.appendChild(item);
    });
}

/**
 * 转义 HTML 文本，避免列表视图直接插值时被浏览器解析。
 *
 * @param {string} raw 原始文本
 * @returns {string} 转义后的文本
 */
function escapeHtml(raw) {
    return String(raw ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

/**
 * 规范化 Tech Pack 数据。
 *
 * @param {Array} rawList 原始包列表
 * @returns {Array} 统一结构化列表
 */
function normalizeTechPacks(rawList) {
    if (!Array.isArray(rawList) || rawList.length === 0) {
        return [];
    }
    if (typeof rawList[0] === 'object' && rawList[0] !== null) {
        return rawList.map((pack) => ({
            name: pack.name || '--',
            version: pack.version || ''
        }));
    }
    return rawList.map((pack) => ({
        name: String(pack),
        version: ''
    }));
}

/**
 * 统一映射后端节点类型。
 *
 * @param {string} rawType 原始类型字符串
 * @returns {string} 前端统一使用的节点类型
 */
function mapNodeType(rawType) {
    const rawString = String(rawType || '');
    if (rawString === 'CEll') {
        return 'CEll';
    }
    const normalized = rawString.toUpperCase();
    switch (normalized) {
        case 'CELL':
        case 'CELL_PROGRAM':
        case 'CELL_PROGRAMS':
            return 'CEll';
        case 'P_PROGRAM':
            return 'P_PROGRAM';
        case 'VIRTUAL':
            return 'VIRTUAL';
        case 'CAR_CODE':
            return 'CAR_CODE';
        case 'CAR_PROGRAM':
            return 'CAR_PROGRAM';
        case 'ROUTE_PROCESS':
            return 'ROUTE_PROCESS';
        case 'DAT':
        case 'SYSTEM':
        case 'SRC':
            return 'SRC';
        default:
            return normalized || 'SRC';
    }
}

/**
 * 将树结构调用图拍平为前端图谱模块/边集合。
 *
 * @param {object|null} rootNode 根节点
 * @returns {{modules: Array, calls: Array}} 图谱节点与边集合
 */
function buildGraphFromCallNode(rootNode) {
    if (!rootNode) {
        return {modules: [], calls: []};
    }

    const modules = [];
    const calls = [];
    const visited = new Set();
    const stack = [rootNode];

    while (stack.length > 0) {
        const node = stack.pop();
        if (!node || !node.id) {
            continue;
        }

        if (!visited.has(node.id)) {
            modules.push({
                name: node.id,
                value: node.value || node.id,
                type: mapNodeType(node.nodeType),
                propertyMap: node.propertyMap || {},
                relevantInfo: node.relevantInfo || ''
            });
            visited.add(node.id);
        }

        const children = Array.isArray(node.children) ? node.children : [];
        children.forEach((child) => {
            if (!child || !child.id) {
                return;
            }
            calls.push({from: node.id, to: child.id});
            if (!visited.has(child.id)) {
                stack.push(child);
            }
        });
    }

    return {modules, calls};
}

/**
 * 规范化单个 RobotInfo。
 *
 * @param {object} raw 原始后端数据
 * @returns {object|null} 统一结构的机器人对象
 */
function normalizeRobotInfo(raw) {
    if (!raw || typeof raw !== 'object') {
        return null;
    }

    if (Array.isArray(raw.modules) && Array.isArray(raw.calls)) {
        return {
            ...raw,
            modules: raw.modules.map((module) => ({
                ...module,
                name: module.name || module.id,
                value: module.value || module.name || module.id,
                type: mapNodeType(module.type || module.nodeType),
                propertyMap: module.propertyMap || {},
                relevantInfo: module.relevantInfo || ''
            })),
            techPacks: normalizeTechPacks(raw.techPacks || raw.techPackList)
        };
    }

    const graph = buildGraphFromCallNode(raw.callGraphRoot);
    return {
        robotName: raw.robotName,
        archiveName: raw.archiveName,
        version: raw.version,
        archiveDate: raw.archiveDate,
        techPacks: normalizeTechPacks(raw.techPackList),
        modules: graph.modules,
        calls: graph.calls
    };
}

/**
 * 规范化机器人列表。
 *
 * @param {Array|object} raw 原始返回值
 * @returns {Array} 统一机器人列表
 */
function normalizeRobotInfoList(raw) {
    if (Array.isArray(raw)) {
        return raw.map(normalizeRobotInfo).filter(Boolean);
    }
    const single = normalizeRobotInfo(raw);
    return single ? [single] : [];
}

/**
 * 将节点类型转成中文展示名称。
 *
 * @param {string} type 节点类型
 * @returns {string} 友好文案
 */
function formatNodeTypeLabel(type) {
    const found = nodeTypeGroups.find((group) => group.types.includes(type));
    return found ? found.label : (type || '--');
}

/**
 * 根据当前 renderMode 切换图谱或列表可见性。
 */
function applyRenderModeVisibility() {
    const showList = currentView === 'car' && currentRenderMode === 'list';
    dom.listView?.classList.toggle('hidden', !showList);
    dom.cy?.classList.toggle('hidden', showList);
    dom.renderModeSwitcher?.classList.toggle('hidden', currentView !== 'car' || !getSelectedRobot());
    dom.btnGraphMode?.classList.toggle('active', currentRenderMode === 'graph');
    dom.btnListMode?.classList.toggle('active', currentRenderMode === 'list');
}

/**
 * 切换图谱/列表渲染模式。
 *
 * @param {'graph'|'list'} mode 目标模式
 */
function switchRenderMode(mode) {
    currentRenderMode = mode;
    applyRenderModeVisibility();
    if (currentView === 'car' && currentRenderMode === 'list') {
        renderListView();
    } else if (currentView === 'car') {
        renderCarGraph();
    }
}

/**
 * 切换线体/车型视图。
 *
 * @param {'line'|'car'} view 目标视图
 */
function switchView(view) {
    if (view === 'car' && currentView === 'line' && isMobileViewport()) {
        currentRenderMode = 'list';
    }
    currentView = view;
    dom.btnLineView?.classList.toggle('active', view === 'line');
    dom.btnCarView?.classList.toggle('active', view === 'car');

    if (view === 'line') {
        dom.renderModeSwitcher?.classList.add('hidden');
        setSidebarVisible(false);
        setNodeControlVisible(false);
        applyRenderModeVisibility();
        renderLineGraph();
        return;
    }

    if (parsedRobots.length === 0) {
        setSidebarVisible(false);
        setNodeControlVisible(false);
        renderCarGraph();
        return;
    }

    setSidebarVisible(false);
    setNodeControlVisible(false);
    applyRenderModeVisibility();
    if (currentRenderMode === 'list') {
        renderListView();
    } else {
        renderCarGraph();
    }
}

/**
 * 渲染线体视图。
 */
function renderLineGraph() {
    applyRenderModeVisibility();
    if (parsedRobots.length === 0) {
        initCy([], 'grid');
        updateRobotDetail(null);
        refreshSidebarTriggerVisibility();
        return;
    }

    const elements = parsedRobots.map((robot, index) => ({
        data: {
            id: `robot_root_${index}`,
            label: robot.robotName || robot.archiveName || `Robot ${index + 1}`,
            type: 'ROBOT_ROOT',
            robotIndex: index,
            relevantInfo: robot.archiveName || ''
        }
    }));

    initCy(elements, 'grid');
    updateRobotDetail(getSelectedRobot());
    clearNodeDetail();
    refreshSidebarTriggerVisibility();
}

/**
 * 渲染车型图谱视图。
 */
function renderCarGraph() {
    applyRenderModeVisibility();
    const selectedRobot = getSelectedRobot();
    if (!selectedRobot) {
        initCy([], 'dagre');
        updateRobotDetail(null);
        clearNodeDetail();
        refreshSidebarTriggerVisibility();
        return;
    }

    const elements = [];
    const nodeIds = new Set();

    selectedRobot.modules.forEach((module) => {
        const nodeId = module.name || module.id;
        if (!nodeId || nodeIds.has(nodeId)) {
            return;
        }
        nodeIds.add(nodeId);
        elements.push({
            data: {
                id: nodeId,
                label: module.value || nodeId,
                type: module.type,
                propertyMap: module.propertyMap || {},
                relevantInfo: module.relevantInfo || ''
            }
        });
    });

    selectedRobot.calls.forEach((call, index) => {
        if (!nodeIds.has(call.from)) {
            nodeIds.add(call.from);
            elements.push({data: {id: call.from, label: call.from, type: 'SRC'}});
        }
        if (!nodeIds.has(call.to)) {
            nodeIds.add(call.to);
            elements.push({data: {id: call.to, label: call.to, type: 'SRC'}});
        }
        elements.push({
            data: {
                id: `edge_${selectedRobotIndex}_${index}`,
                source: call.from,
                target: call.to
            }
        });
    });

    initCy(elements, 'dagre');
    applyAllNodeTypeScales();
    applyTypeHighlights();
    updateRobotDetail(selectedRobot);
    clearNodeDetail();
    refreshSidebarTriggerVisibility();
}

/**
 * 渲染车型列表视图。
 * <p>
 * 手机端不适合长时间在大图上拖拽，因此这里提供结构化列表作为补充阅读模式。
 */
function renderListView() {
    applyRenderModeVisibility();
    setNodeControlVisible(false);
    const selectedRobot = getSelectedRobot();
    if (!selectedRobot) {
        dom.listView.innerHTML = '<div class="list-section"><h3 class="list-section-title">暂无车型数据</h3></div>';
        updateRobotDetail(null);
        refreshSidebarTriggerVisibility();
        return;
    }

    const moduleMap = new Map(selectedRobot.modules.map((module) => [module.name || module.id, module]));
    const groupedModules = nodeTypeGroups.map((group) => ({
        label: group.label,
        typeKey: group.types[0],
        items: selectedRobot.modules.filter((module) => group.types.includes(module.type))
    })).filter((group) => group.items.length > 0);

    const techPackHtml = selectedRobot.techPacks.length === 0
        ? '<div class="info-empty">暂无数据</div>'
        : `<div class="tech-pack-list">
                ${selectedRobot.techPacks.map((pack) => `
                    <div class="tech-pack-item">
                        <span>${escapeHtml(pack.name || '--')}</span>
                        <span class="info-mono">${escapeHtml(pack.version || '')}</span>
                    </div>
                `).join('')}
           </div>`;

    const summaryHtml = `
        <div class="list-section">
            <h3 class="list-section-title">机器人摘要</h3>
            <div class="list-grid">
                <div class="list-card"><strong>机器人名称</strong>${escapeHtml(selectedRobot.robotName || '--')}</div>
                <div class="list-card"><strong>备份文件</strong>${escapeHtml(selectedRobot.archiveName || '--')}</div>
                <div class="list-card"><strong>版本</strong>${escapeHtml(selectedRobot.version || '--')}</div>
                <div class="list-card"><strong>备份时间</strong>${escapeHtml(selectedRobot.archiveDate || '--')}</div>
                <div class="list-card"><strong>调用边数量</strong>${selectedRobot.calls.length}</div>
            </div>
            <div class="list-card list-card-full">
                <strong>已安装的包</strong>
                ${techPackHtml}
            </div>
        </div>
    `;

    const moduleHtml = groupedModules.map((group) => `
        <div class="list-section">
            <h3 class="list-section-title">${escapeHtml(group.label)}（${group.items.length}）</h3>
            <div class="list-grid">
                ${group.items.map((item) => `
                    <div class="list-card">
                        <strong>${escapeHtml(item.value || item.name || '--')}</strong>
                        <div class="info-mono">${escapeHtml(item.name || '--')}</div>
                        <div class="info-break">${escapeHtml(item.propertyMap?.srcFilePath || '无物理路径')}</div>
                    </div>
                `).join('')}
            </div>
        </div>
    `).join('');

    const callHtml = `
        <div class="list-section">
            <h3 class="list-section-title">直接调用关系列表</h3>
            ${selectedRobot.calls.length === 0 ? '<div class="info-empty">暂无调用边。</div>' : selectedRobot.calls.map((call) => {
                const fromModule = moduleMap.get(call.from) || {value: call.from, type: 'SRC'};
                const toModule = moduleMap.get(call.to) || {value: call.to, type: 'SRC'};
                return `
                    <div class="list-call-item">
                        <div class="list-call-route">
                            <span class="call-tag type-${escapeHtml(fromModule.type || 'SRC')}">${escapeHtml(fromModule.value || call.from)}</span>
                            <span class="info-mono">→</span>
                            <span class="call-tag type-${escapeHtml(toModule.type || 'SRC')}">${escapeHtml(toModule.value || call.to)}</span>
                        </div>
                        <div class="info-break">${escapeHtml(call.from)} → ${escapeHtml(call.to)}</div>
                    </div>
                `;
            }).join('')}
        </div>
    `;

    dom.listView.innerHTML = summaryHtml + moduleHtml + callHtml;
    updateRobotDetail(selectedRobot);
    clearNodeDetail();
    refreshSidebarTriggerVisibility();
}

/**
 * 初始化 Cytoscape 图实例。
 *
 * @param {Array} elements   节点与边数据
 * @param {string} layoutName 布局算法名称
 */
function initCy(elements, layoutName = 'dagre') {
    if (cy) {
        try {
            cy.destroy();
        } catch (error) {
            console.warn('销毁旧图实例失败', error);
        }
        cy = null;
        hideNodePopover();
    }

    let layoutConfig;
    if (layoutName === 'grid') {
        layoutConfig = {
            name: 'grid',
            cols: isMobileViewport() ? 1 : 2,
            padding: 60,
            avoidOverlap: true
        };
    } else {
        const dagreAvailable = cytoscape.extensions && cytoscape.extensions.layout && cytoscape.extensions.layout.dagre;
        if (layoutName === 'dagre' && dagreAvailable) {
            layoutConfig = {
                name: 'dagre',
                rankDir: 'TB',
                nodeSep: isMobileViewport() ? 38 : 56,
                rankSep: isMobileViewport() ? 88 : 120,
                padding: 56,
                animate: true,
                animationDuration: 420
            };
        } else {
            layoutConfig = {
                name: 'breadthfirst',
                directed: true,
                padding: 56,
                spacingFactor: 1.5,
                animate: true,
                animationDuration: 420
            };
        }
    }

    cy = cytoscape({
        container: dom.cy,
        elements,
        minZoom: 0.16,
        maxZoom: 3,
        wheelSensitivity: 0.25,
        layout: layoutConfig,
        style: [
            {
                selector: 'node',
                style: {
                    label: 'data(label)',
                    'text-valign': 'center',
                    'text-halign': 'center',
                    color: '#ffffff',
                    'font-size': '12px',
                    'font-weight': 'bold',
                    'text-wrap': 'wrap',
                    'text-max-width': '96px',
                    'text-outline-width': 0,
                    'transition-property': 'background-color, line-color, target-arrow-color, opacity, border-color',
                    'transition-duration': '0.2s'
                }
            },
            {
                selector: 'node[type="CEll"], node[type="CAR_CODE"]',
                style: {
                    shape: 'ellipse',
                    'background-color': '#f97316',
                    width: '86px',
                    height: '86px',
                    'border-width': 4,
                    'border-color': '#fff7ed',
                    'shadow-blur': 14,
                    'shadow-color': 'rgba(249,115,22,0.42)',
                    'shadow-opacity': 1
                }
            },
            {
                selector: 'node[type="P_PROGRAM"], node[type="CAR_PROGRAM"], node[type="ROUTE_PROCESS"], node[type="SRC"]',
                style: {
                    shape: 'round-rectangle',
                    'background-color': '#3b82f6',
                    width: '128px',
                    height: '48px',
                    'corner-radius': '12px'
                }
            },
            {
                selector: 'node[type="VIRTUAL"]',
                style: {
                    shape: 'round-rectangle',
                    'background-color': '#ffffff',
                    'background-opacity': 0,
                    width: '128px',
                    height: '48px',
                    'corner-radius': '12px',
                    'border-width': 2,
                    'border-style': 'dashed',
                    'border-color': '#64748b',
                    color: '#334155'
                }
            },
            {
                selector: 'node[type="ROBOT_ROOT"]',
                style: {
                    shape: 'ellipse',
                    'background-color': '#ea580c',
                    width: '132px',
                    height: '86px',
                    'font-size': '14px',
                    'border-width': 4,
                    'border-color': '#ffffff',
                    'shadow-blur': 16,
                    'shadow-color': 'rgba(234,88,12,0.28)'
                }
            },
            {
                selector: 'edge',
                style: {
                    width: 2,
                    'line-color': '#94a3b8',
                    'target-arrow-color': '#94a3b8',
                    'target-arrow-shape': 'triangle',
                    'curve-style': 'bezier',
                    'arrow-scale': 1.15,
                    opacity: 0.86
                }
            },
            {
                selector: '.highlighted',
                style: {
                    'background-color': '#286395',
                    'line-color': '#286395',
                    'target-arrow-color': '#286395',
                    color: '#ffffff'
                }
            },
            {
                selector: 'node[type="CEll"].highlighted, node[type="CAR_CODE"].highlighted',
                style: {
                    'background-color': '#c2410c'
                }
            },
            {
                selector: 'node[type="VIRTUAL"].highlighted',
                style: {
                    'background-color': '#ffffff',
                    'background-opacity': 0,
                    'border-color': '#1e293b',
                    color: '#1e293b'
                }
            },
            {
                selector: '.dimmed',
                style: {
                    opacity: 0.12
                }
            }
        ]
    });

    bindCyEvents();
}

/**
 * 绑定 Cytoscape 交互事件。
 * <p>
 * 节点单击负责选中与高亮；
 * 桌面端双击负责查看文件属性，右键负责查看调用上下文；
 * 长按节点则兼容移动端查看文件属性。
 */
function bindCyEvents() {
    if (!cy) {
        return;
    }

    cy.on('tap', 'node', (event) => {
        const node = event.target;
        const renderedPosition = event.renderedPosition || node.renderedPosition();

        if (!isMobileViewport() && hasPropertyMapDetails(node)) {
            const now = Date.now();
            if (pendingNodeTap
                && pendingNodeTap.nodeId === node.id()
                && now - pendingNodeTap.timestamp <= NODE_DOUBLE_TAP_DELAY) {
                clearPendingNodeTap();
                applyNodeSelection(node);
                showNodePopover(node, 'left', renderedPosition);
                return;
            }

            clearPendingNodeTap();
            pendingNodeTap = {
                nodeId: node.id(),
                timestamp: now,
                timerId: setTimeout(() => {
                    handleNodePrimaryAction(node);
                    pendingNodeTap = null;
                }, NODE_DOUBLE_TAP_DELAY)
            };
            return;
        }

        clearPendingNodeTap();
        handleNodePrimaryAction(node);
    });

    cy.on('tap', (event) => {
        if (event.target === cy) {
            clearPendingNodeTap();
            cy.elements().removeClass('dimmed highlighted');
            applyTypeHighlights();
            hideNodePopover();
            clearNodeDetail();
        }
    });

    cy.on('cxttap', 'node', (event) => {
        clearPendingNodeTap();
        const node = event.target;
        const renderedPosition = event.renderedPosition || node.renderedPosition();
        applyNodeSelection(node);
        showNodePopover(node, 'right', renderedPosition);
    });

    cy.on('taphold', 'node', (event) => {
        if (!isMobileViewport()) {
            return;
        }
        clearPendingNodeTap();
        const node = event.target;
        const renderedPosition = event.renderedPosition || node.renderedPosition();
        applyNodeSelection(node);
        showNodePopover(node, 'left', renderedPosition);
    });
}

/**
 * 清理待执行的单击操作，避免与双击产生冲突。
 */
function clearPendingNodeTap() {
    if (!pendingNodeTap) {
        return;
    }
    clearTimeout(pendingNodeTap.timerId);
    pendingNodeTap = null;
}

/**
 * 判断节点是否存在可展示的文件属性信息。
 *
 * @param {object} node Cytoscape 节点对象
 * @returns {boolean} true 表示存在 propertyMap 信息
 */
function hasPropertyMapDetails(node) {
    const propertyMap = node?.data('propertyMap') || {};
    return Boolean(propertyMap.srcFilePath || propertyMap.createTime || propertyMap.modifyTime);
}

/**
 * 执行节点主操作。
 *
 * @param {object} node Cytoscape 节点对象
 */
function handleNodePrimaryAction(node) {
    if (currentView === 'line') {
        const nextRobotIndex = Number.parseInt(node.data('robotIndex'), 10);
        if (Number.isInteger(nextRobotIndex) && nextRobotIndex >= 0) {
            selectedRobotIndex = nextRobotIndex;
        }
        updateRobotDetail(getSelectedRobot());
        refreshSidebarTriggerVisibility();
        currentRenderMode = isMobileViewport() ? 'list' : 'graph';
        switchView('car');
        return;
    }

    applyNodeSelection(node);
    if (isMobileViewport()) {
        setSidebarVisible(true);
    }
}

/**
 * 选中并高亮指定节点，同时刷新详情区。
 *
 * @param {object} node Cytoscape 节点对象
 */
function applyNodeSelection(node) {
    cy.elements().removeClass('dimmed highlighted');
    const lineage = node.predecessors().add(node).add(node.successors());
    cy.elements().addClass('dimmed');
    lineage.removeClass('dimmed').addClass('highlighted');
    applyTypeHighlights();
    updateNodeDetail(node.data());
}

/**
 * 隐藏节点气泡提示。
 */
function hideNodePopover() {
    if (dom.nodePopover) {
        dom.nodePopover.classList.add('hidden');
    }
}

/**
 * 创建气泡提示中的字段块。
 *
 * @param {string} label 标签名
 * @param {string} value 值
 * @returns {HTMLElement} DOM 节点
 */
function createPopoverRow(label, value) {
    const wrapper = document.createElement('div');
    wrapper.className = 'flex flex-col gap-1';
    const title = document.createElement('span');
    title.className = 'text-slate-400';
    title.textContent = label;
    const content = document.createElement('div');
    content.className = 'node-popover-content';
    content.textContent = value || '--';
    wrapper.appendChild(title);
    wrapper.appendChild(content);
    return wrapper;
}

/**
 * 显示节点气泡提示。
 *
 * @param {object} node            Cytoscape 节点对象
 * @param {'left'|'right'} side    提示内容模式
 * @param {{x:number,y:number}} anchorPosition 锚点位置
 */
function showNodePopover(node, side, anchorPosition) {
    if (!dom.nodePopover) {
        return;
    }
    dom.nodePopover.innerHTML = '';
    const title = document.createElement('div');
    title.className = 'node-popover-title';
    title.textContent = node.data('label') || node.id();
    dom.nodePopover.appendChild(title);

    if (side === 'left') {
        const propertyMap = node.data('propertyMap') || {};
        dom.nodePopover.appendChild(createPopoverRow('文件路径', propertyMap.srcFilePath));
        dom.nodePopover.appendChild(createPopoverRow('创建时间', propertyMap.createTime));
        dom.nodePopover.appendChild(createPopoverRow('修改时间', propertyMap.modifyTime));
    } else {
        dom.nodePopover.appendChild(createPopoverRow('相关信息', node.data('relevantInfo') || '--'));
    }

    const containerRect = dom.cy.getBoundingClientRect();
    const x = Math.min(containerRect.width - 280, anchorPosition.x + 16);
    const y = Math.min(containerRect.height - 120, anchorPosition.y + 16);
    dom.nodePopover.style.left = `${Math.max(8, x)}px`;
    dom.nodePopover.style.top = `${Math.max(8, y)}px`;
    dom.nodePopover.classList.remove('hidden');

    title.addEventListener('mousedown', (mouseEvent) => {
        mouseEvent.preventDefault();
        popoverDragState = {
            offsetX: mouseEvent.clientX - dom.nodePopover.offsetLeft,
            offsetY: mouseEvent.clientY - dom.nodePopover.offsetTop
        };
    });
}

/**
 * 解析 data-node-types 字段。
 *
 * @param {string} raw 原始字符串
 * @returns {Array<string>} 类型数组
 */
function parseNodeTypes(raw) {
    return String(raw || '')
        .split(',')
        .map((type) => type.trim())
        .filter(Boolean);
}

/**
 * 生成节点类型组合键。
 *
 * @param {Array<string>} types 类型数组
 * @returns {string} 唯一键
 */
function getScaleKey(types) {
    return types.slice().sort().join('|');
}

/**
 * 对某一组节点类型应用尺寸缩放。
 *
 * @param {Array<string>} types 节点类型数组
 * @param {number} scale        缩放比例
 */
function applyNodeTypeScale(types, scale) {
    if (!cy) {
        return;
    }
    types.forEach((type) => {
        const base = nodeTypeBaseSizes[type];
        if (!base) {
            return;
        }
        cy.style()
            .selector(`node[type="${type}"]`)
            .style({
                width: `${base.width * scale}px`,
                height: `${base.height * scale}px`,
                'font-size': `${base.fontSize * scale}px`,
                'text-max-width': `${base.textMaxWidth * scale}px`
            })
            .update();
    });
}

/**
 * 应用所有节点类型缩放。
 */
function applyAllNodeTypeScales() {
    nodeTypeGroups.forEach((group) => {
        const scaleKey = getScaleKey(group.types);
        const scale = nodeTypeScale.get(scaleKey) || 1;
        applyNodeTypeScale(group.types, scale);
    });
}

/**
 * 应用节点类型高亮。
 */
function applyTypeHighlights() {
    if (!cy || activeTypeSelections.size === 0) {
        return;
    }
    const selector = Array.from(activeTypeSelections)
        .map((type) => `node[type="${type}"]`)
        .join(', ');
    if (!selector) {
        return;
    }
    cy.nodes(selector).removeClass('dimmed').addClass('highlighted');
}

/**
 * 初始化节点控制面板。
 */
function initNodeControls() {
    document.querySelectorAll('.node-type-button').forEach((button) => {
        button.addEventListener('click', () => {
            const types = parseNodeTypes(button.dataset.nodeTypes);
            const active = button.classList.contains('active');
            button.classList.toggle('active', !active);
            types.forEach((type) => {
                if (active) {
                    activeTypeSelections.delete(type);
                } else {
                    activeTypeSelections.add(type);
                }
            });
            if (cy) {
                cy.elements().removeClass('dimmed highlighted');
                applyTypeHighlights();
            }
        });
    });

    document.querySelectorAll('.node-size-slider').forEach((slider) => {
        const types = parseNodeTypes(slider.dataset.nodeTypes);
        const scaleKey = getScaleKey(types);
        nodeTypeScale.set(scaleKey, Number.parseFloat(slider.value) || 1);
        slider.addEventListener('input', () => {
            const scale = Number.parseFloat(slider.value) || 1;
            nodeTypeScale.set(scaleKey, scale);
            applyNodeTypeScale(types, scale);
        });
    });
}

/**
 * 绑定页面级交互事件。
 */
function bindPageEvents() {
    if (dom.fileUploadButton) {
        dom.fileUploadButton.addEventListener('click', () => dom.fileUpload.click());
    }

    if (dom.fileUpload) {
        dom.fileUpload.addEventListener('change', () => {
            const files = Array.from(dom.fileUpload.files || []);
            const invalidFile = files.find((file) => !file.name.toLowerCase().endsWith('.zip'));
            if (invalidFile) {
                alert('请选择 .zip 格式的备份文件');
                dom.fileUpload.value = '';
                uploadState.zipFiles = [];
                dom.fileUploadText.textContent = '上传备份 (.zip)';
                updateActionButtons();
                return;
            }
            uploadState.zipFiles = files;
            uploadState.lastSuccessfulTaskId = null;
            uploadState.lastSuccessfulSignature = null;
            dom.fileUploadText.textContent = files.length === 0
                ? '上传备份 (.zip)'
                : files.length <= 2
                    ? `上传备份 (.zip): ${files.map((file) => file.name).join(', ')}`
                    : `上传备份 (.zip): 已选择 ${files.length} 个文件`;
            setTaskBadge('输入已更新，等待分析', 'info');
            setHeaderMenuOpen(false);
            updateActionButtons();
        });
    }

    dom.headerMenuToggle?.addEventListener('click', (event) => {
        event.stopPropagation();
        toggleHeaderMenu();
    });

    dom.configButton?.addEventListener('click', () => {
        setHeaderMenuOpen(false);
        openConfigModal();
    });
    dom.configCloseBtn?.addEventListener('click', () => closeConfigModal());
    dom.configModal?.addEventListener('click', (event) => {
        if (event.target === dom.configModal) {
            closeConfigModal();
        }
    });
    dom.configReloadBtn?.addEventListener('click', async () => {
        try {
            await loadConfigFromServer();
            alert('已从磁盘重载配置。');
        } catch (error) {
            console.error(error);
            alert(error.message || '重载配置失败');
        }
    });
    dom.configApplyBtn?.addEventListener('click', () => {
        uploadState.configContent = dom.configTextarea.value || '';
        uploadState.lastSuccessfulTaskId = null;
        uploadState.lastSuccessfulSignature = null;
        closeConfigModal();
        setTaskBadge('配置已更新，等待分析', 'info');
        updateActionButtons();
    });

    dom.startAnalysisBtn?.addEventListener('click', () => {
        setHeaderMenuOpen(false);
        runAnalysisByMode();
    });
    dom.downloadExcelBtn?.addEventListener('click', () => {
        setHeaderMenuOpen(false);
        runDownloadByMode();
    });
    dom.btnLineView?.addEventListener('click', () => switchView('line'));
    dom.btnCarView?.addEventListener('click', () => switchView('car'));
    dom.btnGraphMode?.addEventListener('click', () => switchRenderMode('graph'));
    dom.btnListMode?.addEventListener('click', () => switchRenderMode('list'));

    dom.sidebarTrigger?.addEventListener('click', () => setSidebarVisible(true));
    dom.infoSidebarClose?.addEventListener('click', () => setSidebarVisible(false));
    dom.nodeControlTrigger?.addEventListener('click', () => setNodeControlVisible(true));
    dom.nodeControlClose?.addEventListener('click', () => setNodeControlVisible(false));

    document.addEventListener('mousemove', (event) => {
        if (!popoverDragState || !dom.nodePopover) {
            return;
        }
        dom.nodePopover.style.left = `${event.clientX - popoverDragState.offsetX}px`;
        dom.nodePopover.style.top = `${event.clientY - popoverDragState.offsetY}px`;
    });

    document.addEventListener('mouseup', () => {
        popoverDragState = null;
    });

    document.addEventListener('click', (event) => {
        if (headerMenuOpen
            && dom.headerMenuPanel
            && dom.headerMenuToggle
            && !dom.headerMenuPanel.contains(event.target)
            && !dom.headerMenuToggle.contains(event.target)) {
            setHeaderMenuOpen(false);
        }

        if (!dom.nodePopover || dom.nodePopover.classList.contains('hidden')) {
            return;
        }
        if (dom.nodePopover.contains(event.target)) {
            return;
        }
        if (dom.cy?.contains(event.target)) {
            return;
        }
        hideNodePopover();
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            setHeaderMenuOpen(false);
        }
    });

    if (dom.cy) {
        dom.cy.addEventListener('contextmenu', (event) => event.preventDefault());
    }

    window.addEventListener('resize', () => {
        updateLayoutMetrics();
        if (headerMenuOpen) {
            setHeaderMenuOpen(false);
        }
        if (currentView === 'car') {
            if (currentRenderMode === 'list') {
                renderListView();
            } else {
                renderCarGraph();
            }
        } else {
            renderLineGraph();
        }
    });
}

/**
 * 读取服务器上的配置文件内容。
 */
async function loadConfigFromServer() {
    const response = await apiFetch('/api/config');
    if (!response.ok) {
        const message = await extractErrorMessage(response);
        throw new Error(`读取配置失败: ${message}`);
    }
    const payload = await response.json();
    uploadState.configPath = payload.configPath || '';
    uploadState.configContent = payload.content || '';
    uploadState.configLoaded = true;
    dom.configPathText.textContent = uploadState.configPath || '--';
    dom.configTextarea.value = uploadState.configContent || '';
    updateActionButtons();
}

/**
 * 查询当前运行模式状态。
 */
async function loadRuntimeStatus() {
    const response = await apiFetch('/api/runtime/status');
    if (!response.ok) {
        const message = await extractErrorMessage(response);
        throw new Error(`读取运行模式失败: ${message}`);
    }
    const payload = await response.json();
    runtimeState.runtimeMode = payload.runtimeMode || 'desktop';
    runtimeState.analysisMode = payload.analysisMode || (runtimeState.runtimeMode === 'server' ? 'async' : 'sync');
    refreshRuntimeBadge();
    refreshModeSpecificUi();
    updateActionButtons();
}

/**
 * 构造分析任务请求体。
 *
 * @returns {FormData} 上传表单对象
 */
function buildTaskFormData() {
    const formData = new FormData();
    uploadState.zipFiles.forEach((zipFile) => {
        formData.append('files', zipFile);
    });
    formData.append('configText', uploadState.configContent || '');
    return formData;
}

/**
 * 提交分析任务，并在成功后刷新图谱。
 *
 * @param {'view'|'download'} intent 任务意图
 */
async function submitAsyncAnalysisTask(intent) {
    if (uploadState.zipFiles.length === 0) {
        alert('请先上传备份文件');
        return;
    }
    if (!uploadState.configLoaded) {
        alert('配置尚未加载完成，请稍后重试');
        return;
    }

    setLoading(true, '正在提交分析任务...');
    try {
        const response = await apiFetch('/api/analysis/tasks', {
            method: 'POST',
            body: buildTaskFormData()
        });
        if (!response.ok) {
            const message = await extractErrorMessage(response);
            throw new Error(`提交任务失败: ${message}`);
        }
        const task = await response.json();
        activeTask = {
            taskId: task.taskId,
            intent,
            inputSignature: computeCurrentInputSignature()
        };
        setTaskBadge('任务已提交，等待执行', 'info');
        updateActionButtons();
        pollTaskUntilFinished(task.taskId, intent);
    } catch (error) {
        console.error(error);
        setLoading(false, '');
        alert(error.message || '提交分析任务失败');
        activeTask = null;
        updateActionButtons();
    }
}

/**
 * 处理 Excel 下载动作。
 * <p>
 * 如果当前输入已经有成功任务结果，则直接下载；
 * 否则先提交一个新的后台任务，完成后自动下载。
 */
async function submitAsyncDownloadTask() {
    const currentSignature = computeCurrentInputSignature();
    if (uploadState.lastSuccessfulTaskId && uploadState.lastSuccessfulSignature === currentSignature) {
        await downloadTaskExcel(uploadState.lastSuccessfulTaskId);
        return;
    }
    await submitAsyncAnalysisTask('download');
}

/**
 * 桌面同步模式下直接执行分析。
 * <p>
 * 这里保留原始桌面版语义：点击“开始分析”后，由当前请求直接返回图谱数据，
 * 不引入任务 ID、任务轮询或“提交任务”的概念。
 */
async function submitSyncAnalysis() {
    if (uploadState.zipFiles.length === 0) {
        alert('请先上传备份文件');
        return;
    }
    if (!uploadState.configLoaded) {
        alert('配置尚未加载完成，请稍后重试');
        return;
    }

    setLoading(true, '正在解析 KRL 代码结构...');
    try {
        const response = await apiFetch('/api/analysis', {
            method: 'POST',
            body: buildTaskFormData()
        });
        if (!response.ok) {
            const message = await extractErrorMessage(response);
            throw new Error(`解析失败: ${message}`);
        }
        const rawData = await response.json();
        parsedRobots = normalizeRobotInfoList(rawData);
        if (parsedRobots.length === 0) {
            throw new Error('解析失败: 返回数据为空或格式不正确');
        }
        selectedRobotIndex = 0;
        currentView = 'line';
        currentRenderMode = 'graph';
        switchView('line');
    } catch (error) {
        console.error(error);
        alert(error.message || '解析失败，请检查后端日志。');
    } finally {
        setLoading(false, '');
    }
}

/**
 * 桌面同步模式下直接导出 Excel。
 * <p>
 * 行为与历史版本保持一致：每次点击都基于当前上传内容与当前配置直接生成 Excel。
 */
async function submitSyncDownload() {
    if (uploadState.zipFiles.length === 0) {
        alert('请先上传备份文件');
        return;
    }
    if (!uploadState.configLoaded) {
        alert('配置尚未加载完成，请稍后重试');
        return;
    }

    setLoading(true, '正在生成 Excel 文件...');
    try {
        const response = await apiFetch('/api/analysis/excel', {
            method: 'POST',
            body: buildTaskFormData()
        });
        if (!response.ok) {
            const message = await extractErrorMessage(response);
            throw new Error(`导出失败: ${message}`);
        }
        const blob = await response.blob();
        const downloadUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = '调用关系表.xlsx';
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(downloadUrl);
    } catch (error) {
        console.error(error);
        alert(error.message || '导出 Excel 失败，请检查后端日志。');
    } finally {
        setLoading(false, '');
    }
}

/**
 * 按当前模式分流“开始分析”动作。
 */
async function runAnalysisByMode() {
    if (isAsyncAnalysisMode()) {
        await submitAsyncAnalysisTask('view');
        return;
    }
    await submitSyncAnalysis();
}

/**
 * 按当前模式分流“下载 Excel”动作。
 */
async function runDownloadByMode() {
    if (isAsyncAnalysisMode()) {
        await submitAsyncDownloadTask();
        return;
    }
    await submitSyncDownload();
}

/**
 * 轮询任务直到结束。
 *
 * @param {string} taskId 任务 ID
 * @param {'view'|'download'} intent 任务意图
 */
function pollTaskUntilFinished(taskId, intent) {
    clearTimeout(taskPollTimer);

    const poll = async () => {
        try {
            const response = await apiFetch(`/api/analysis/tasks/${taskId}`);
            if (!response.ok) {
                const message = await extractErrorMessage(response);
                throw new Error(message);
            }
            const task = await response.json();
            const status = task.status || 'PENDING';
            const message = task.message || '任务处理中';

            if (status === 'PENDING') {
                setLoading(true, `${message}...`);
                setTaskBadge('任务排队中', 'info');
                taskPollTimer = setTimeout(poll, 1800);
                return;
            }
            if (status === 'RUNNING') {
                setLoading(true, `${message}...`);
                setTaskBadge('任务执行中', 'warning');
                taskPollTimer = setTimeout(poll, 1800);
                return;
            }
            if (status === 'FAILED') {
                setLoading(false, '');
                setTaskBadge(message, 'error');
                activeTask = null;
                updateActionButtons();
                alert(message || '任务执行失败');
                return;
            }
            if (status === 'SUCCEEDED') {
                await handleTaskSuccess(taskId, intent);
            }
        } catch (error) {
            console.error(error);
            setLoading(false, '');
            activeTask = null;
            updateActionButtons();
            setTaskBadge('轮询任务失败', 'error');
            alert(error.message || '任务轮询失败');
        }
    };

    poll();
}

/**
 * 处理任务成功后的图谱刷新与 Excel 下载。
 *
 * @param {string} taskId          任务 ID
 * @param {'view'|'download'} intent 任务意图
 */
async function handleTaskSuccess(taskId, intent) {
    setLoading(true, '正在读取分析结果...');
    try {
        const response = await apiFetch(`/api/analysis/tasks/${taskId}/result`);
        if (!response.ok) {
            const message = await extractErrorMessage(response);
            throw new Error(message);
        }
        const rawData = await response.json();
        parsedRobots = normalizeRobotInfoList(rawData);
        selectedRobotIndex = 0;
        currentView = 'line';
        currentRenderMode = 'graph';
        switchView('line');
        uploadState.lastSuccessfulTaskId = taskId;
        uploadState.lastSuccessfulSignature = computeCurrentInputSignature();
        setTaskBadge('任务执行完成', 'success');
        updateActionButtons();
        if (intent === 'download') {
            await downloadTaskExcel(taskId);
        }
    } finally {
        setLoading(false, '');
        activeTask = null;
        updateActionButtons();
    }
}

/**
 * 下载指定任务的 Excel 文件。
 *
 * @param {string} taskId 任务 ID
 */
async function downloadTaskExcel(taskId) {
    setLoading(true, '正在下载 Excel 文件...');
    try {
        const response = await apiFetch(`/api/analysis/tasks/${taskId}/excel`);
        if (!response.ok) {
            const message = await extractErrorMessage(response);
            throw new Error(`下载失败: ${message}`);
        }
        const blob = await response.blob();
        const downloadUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = '调用关系表.xlsx';
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(downloadUrl);
    } catch (error) {
        console.error(error);
        alert(error.message || '下载 Excel 失败');
    } finally {
        setLoading(false, '');
    }
}

/**
 * 初始化工作区。
 */
async function initializeWorkspace() {
    await loadConfigFromServer();
    switchView('line');
    renderLineGraph();
    if (isAsyncAnalysisMode()) {
        setTaskBadge(uploadState.zipFiles.length > 0 ? '输入就绪，等待分析' : '等待上传', 'neutral');
    }
}

/**
 * 启动前端应用。
 */
async function bootstrap() {
    updateLayoutMetrics();
    bindPageEvents();
    initNodeControls();
    refreshRuntimeBadge();
    refreshModeSpecificUi();
    updateActionButtons();
    clearNodeDetail();
    if (isAsyncAnalysisMode()) {
        setTaskBadge('正在检查运行模式', 'info');
    }

    try {
        setLoading(true, '正在检查服务状态...');
        await loadRuntimeStatus();
        await initializeWorkspace();
    } catch (error) {
        console.error(error);
        if (isAsyncAnalysisMode()) {
            setTaskBadge('初始化失败', 'error');
        }
        alert(error.message || '初始化失败，请检查后端日志');
    } finally {
        setLoading(false, '');
    }
}

bootstrap();
