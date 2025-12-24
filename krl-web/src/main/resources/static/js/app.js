// 初始化图标
lucide.createIcons();

// -------------------------------------------------------------------------
// 关键修复：显式注册 Dagre 插件
// -------------------------------------------------------------------------
try {
    if (typeof cytoscapeDagre !== 'undefined') {
        cytoscape.use(cytoscapeDagre);
        console.log("Cytoscape dagre extension registered.");
    } else {
        console.warn("Cytoscape dagre extension global variable not found.");
    }
} catch (e) {
    console.error("Error registering dagre extension:", e);
}

// 全局状态
let cy = null;
let currentView = 'car'; // 'line' | 'car'
let parsedData = null; // 存储解析后的 RobotInfo
let uploadState = {
    zipFile: null,
    configFile: null
};

// DOM 元素引用
const dom = {
    cy: document.getElementById('cy'),
    infoSidebar: document.getElementById('infoSidebar'),
    sidebarTrigger: document.getElementById('sidebarTrigger'),
    metaName: document.getElementById('metaName'),
    metaVersion: document.getElementById('metaVersion'),
    metaDate: document.getElementById('metaDate'),
    techPackList: document.getElementById('techPackList'),
    btnLineView: document.getElementById('btnLineView'),
    btnCarView: document.getElementById('btnCarView'),
    loader: document.getElementById('loader'),
    fileUploadButton: document.getElementById('fileUploadButton'),
    configUploadButton: document.getElementById('configUploadButton'),
    fileUploadText: document.getElementById('fileUploadText'),
    configUploadText: document.getElementById('configUploadText'),
    startAnalysisBtn: document.getElementById('startAnalysisBtn'),
    nodeOverlay: document.getElementById('nodeOverlay'),
    nodePopover: document.getElementById('nodePopover')
};

const nodeTypeGroups = [
    {label: 'Cell', types: ['CEll']},
    {label: 'P程序', types: ['P_PROGRAM', 'VIRTUAL']},
    {label: '车型代码', types: ['CAR_CODE']},
    {label: '车型程序', types: ['CAR_PROGRAM']},
    {label: '轨迹程序', types: ['ROUTE_PROCESS']}
];

const nodeTypeBaseSizes = {
    CEll: {width: 82, height: 82, fontSize: 12, textMaxWidth: 90},
    CAR_CODE: {width: 82, height: 82, fontSize: 12, textMaxWidth: 90},
    P_PROGRAM: {width: 120, height: 44, fontSize: 12, textMaxWidth: 100},
    VIRTUAL: {width: 120, height: 44, fontSize: 12, textMaxWidth: 100},
    CAR_PROGRAM: {width: 120, height: 44, fontSize: 12, textMaxWidth: 100},
    ROUTE_PROCESS: {width: 120, height: 44, fontSize: 12, textMaxWidth: 100}
};

const nodeTypeScale = new Map();
let markerUpdateScheduled = false;
const markerElements = new Map();

function scheduleMarkerUpdate() {
    if (!cy || markerUpdateScheduled) {
        return;
    }
    markerUpdateScheduled = true;
    requestAnimationFrame(() => {
        markerUpdateScheduled = false;
        updateNodeMarkers();
    });
}

function clearNodeMarkers() {
    markerElements.clear();
    if (dom.nodeOverlay) {
        dom.nodeOverlay.innerHTML = '';
    }
}

function createNodeMarker(node, side) {
    const marker = document.createElement('button');
    marker.type = 'button';
    marker.className = `node-marker node-marker-${side}`;
    marker.dataset.nodeId = node.id();
    marker.dataset.side = side;
    marker.textContent = side === 'left' ? 'i' : '⇢';
    marker.addEventListener('click', (event) => {
        event.stopPropagation();
        showNodePopover(node, side, marker);
    });
    return marker;
}

function setupNodeMarkers() {
    if (!dom.nodeOverlay || !cy) {
        return;
    }
    clearNodeMarkers();
    cy.nodes().forEach((node) => {
        const left = createNodeMarker(node, 'left');
        const right = createNodeMarker(node, 'right');
        markerElements.set(`${node.id()}-left`, left);
        markerElements.set(`${node.id()}-right`, right);
        dom.nodeOverlay.appendChild(left);
        dom.nodeOverlay.appendChild(right);
    });
    scheduleMarkerUpdate();
    cy.on('render zoom pan position', scheduleMarkerUpdate);
    cy.on('layoutstop', scheduleMarkerUpdate);
}

function updateNodeMarkers() {
    if (!cy || !dom.nodeOverlay) {
        return;
    }
    const overlayRect = dom.nodeOverlay.getBoundingClientRect();
    const containerRect = dom.cy.getBoundingClientRect();
    const offsetX = containerRect.left - overlayRect.left;
    const offsetY = containerRect.top - overlayRect.top;
    cy.nodes().forEach((node) => {
        const leftMarker = markerElements.get(`${node.id()}-left`);
        const rightMarker = markerElements.get(`${node.id()}-right`);
        if (!leftMarker || !rightMarker) {
            return;
        }
        if (node.hidden()) {
            leftMarker.style.display = 'none';
            rightMarker.style.display = 'none';
            return;
        }
        const box = node.renderedBoundingBox();
        const centerY = (box.y1 + box.y2) / 2;
        const leftX = box.x1 - 10;
        const rightX = box.x2 + 10;
        leftMarker.style.display = 'flex';
        rightMarker.style.display = 'flex';
        leftMarker.style.left = `${leftX + offsetX}px`;
        leftMarker.style.top = `${centerY + offsetY}px`;
        rightMarker.style.left = `${rightX + offsetX}px`;
        rightMarker.style.top = `${centerY + offsetY}px`;
        leftMarker.style.transform = 'translate(-50%, -50%)';
        rightMarker.style.transform = 'translate(-50%, -50%)';
    });
}

function hideNodePopover() {
    if (dom.nodePopover) {
        dom.nodePopover.classList.add('hidden');
    }
}

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

function showNodePopover(node, side, anchor) {
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
        const relevantInfo = node.data('relevantInfo');
        dom.nodePopover.appendChild(createPopoverRow('相关信息', relevantInfo || '--'));
    }

    const anchorRect = anchor.getBoundingClientRect();
    const overlayRect = dom.nodeOverlay.getBoundingClientRect();
    dom.nodePopover.style.left = `${anchorRect.left - overlayRect.left + 16}px`;
    dom.nodePopover.style.top = `${anchorRect.top - overlayRect.top + 16}px`;
    dom.nodePopover.classList.remove('hidden');
}

// -------------------------------------------------------------------------
// 核心功能：Cytoscape 初始化与配置
// -------------------------------------------------------------------------

function initCy(elements, layoutName = 'dagre') {
    if (cy) {
        try {
            cy.destroy(); // 销毁旧实例
        } catch (e) {
            console.warn("Error destroying cy instance:", e);
        }
        cy = null;
        clearNodeMarkers();
        hideNodePopover();
    }

    // 布局配置
    let layoutConfig;
    if (layoutName === 'grid') {
        // 线体信息：双列 grid 布局
        layoutConfig = {
            name: 'grid',
            cols: 2, // 强制两列
            padding: 50,
            avoidOverlap: true
        };
    } else {
        // 车型信息：Dagre 层次布局
        // 检查 dagre 是否可用，如果不可用则回退到 breadthfirst 以防崩溃
        const isDagreAvailable = cytoscape.extensions &&
            cytoscape.extensions.layout &&
            cytoscape.extensions.layout.dagre;

        if (!isDagreAvailable && layoutName === 'dagre') {
            console.error("Dagre layout not found in extensions, falling back to breadthfirst");
            layoutName = 'breadthfirst';
        }

        if (layoutName === 'dagre') {
            layoutConfig = {
                name: 'dagre',
                rankDir: 'TB', // 从上到下
                nodeSep: 60,
                rankSep: 110,
                padding: 50,
                animate: true,
                animationDuration: 500
            };
        } else {
            // Fallback
            layoutConfig = {
                name: 'breadthfirst',
                directed: true,
                padding: 50,
                spacingFactor: 1.6,
                animate: true,
                animationDuration: 500
            };
        }
    }

    cy = cytoscape({
        container: dom.cy,
        elements: elements,

        // 交互设置
        minZoom: 0.2,
        maxZoom: 3,
        wheelSensitivity: 0.3, // 降低滚轮灵敏度，防止一下缩放没了

        layout: layoutConfig,

        // 样式表
        style: [
            // ---------------- 节点基础样式 ----------------
            {
                selector: 'node',
                style: {
                    'label': 'data(label)',
                    'text-valign': 'center',
                    'text-halign': 'center',
                    'color': '#ffffff',
                    'font-size': '12px',
                    'font-weight': 'bold',
                    'text-outline-width': 0, // 去掉文字描边，更扁平
                    'text-wrap': 'wrap',
                    'text-max-width': '90px',
                    'transition-property': 'background-color, line-color, target-arrow-color, opacity',
                    'transition-duration': '0.3s'
                }
            },

            // ---------------- 特定类型节点样式 (核心需求) ----------------
            // 1. CEll / CAR_CODE: 圆形，橙色
            {
                selector: 'node[type="CEll"], node[type="CAR_CODE"]',
                style: {
                    'shape': 'ellipse',
                    'background-color': '#f97316',
                    'width': '82px',
                    'height': '82px',
                    'border-width': 4,
                    'border-color': '#fff7ed',
                    'shadow-blur': 12,
                    'shadow-color': 'rgba(249, 115, 22, 0.45)',
                    'shadow-opacity': 1
                }
            },
            // 2. P 程序 / 车型程序 / 轨迹程序 / 虚拟节点: 圆角矩形，蓝色
            {
                selector: 'node[type="P_PROGRAM"], node[type="VIRTUAL"], node[type="CAR_PROGRAM"], node[type="ROUTE_PROCESS"], node[type="SRC"]',
                style: {
                    'shape': 'round-rectangle',
                    'background-color': '#3b82f6',
                    'width': '120px',
                    'height': '44px',
                    'border-radius': '10px'
                }
            },
            // 3. DAT / 系统文件: 灰蓝色
            {
                selector: 'node[type="DAT"], node[type="SYSTEM"]',
                style: {
                    'shape': 'round-rectangle',
                    'background-color': '#64748b',
                    'width': '120px',
                    'height': '40px',
                    'border-radius': '10px'
                }
            },
            // 4. 线体视图的机器人节点: 大椭圆，深色
            {
                selector: 'node[type="ROBOT_ROOT"]',
                style: {
                    'shape': 'ellipse',
                    'background-color': '#ea580c',
                    'width': '120px',
                    'height': '80px',
                    'font-size': '14px',
                    'border-width': 4,
                    'border-color': '#fff',
                    'shadow-blur': 15,
                    'shadow-color': 'rgba(0,0,0,0.2)'
                }
            },

            // ---------------- 连线样式 ----------------
            {
                selector: 'edge',
                style: {
                    'width': 2,
                    'line-color': '#94a3b8',
                    'target-arrow-color': '#94a3b8',
                    'target-arrow-shape': 'triangle',
                    'curve-style': 'bezier',
                    'arrow-scale': 1.2,
                    'opacity': 0.85
                }
            },

            // ---------------- 交互状态样式 ----------------
            // 选中/高亮
            {
                selector: '.highlighted',
                style: {
                    'background-color': '#2563eb',
                    'line-color': '#2563eb',
                    'target-arrow-color': '#2563eb',
                    'transition-duration': '0.1s'
                }
            },
            {
                selector: 'node[type="CEll"].highlighted, node[type="CAR_CODE"].highlighted',
                style: {
                    'background-color': '#c2410c'
                }
            },
            // 变暗 (非相关节点)
            {
                selector: '.dimmed',
                style: {
                    'opacity': 0.1
                }
            }
        ]
    });

    // 事件绑定：点击高亮逻辑
    cy.on('tap', 'node', function (evt) {
        const node = evt.target;

        // 如果是在线体视图，点击进入车型视图
        if (currentView === 'line') {
            switchView('car');
            return;
        }

        // 清除之前的状态
        cy.elements().removeClass('dimmed highlighted');

        // 逻辑：高亮被点击节点 + 所有祖先与子孙节点（完整链路）
        const lineage = node.predecessors().add(node).add(node.successors());

        cy.elements().addClass('dimmed');
        lineage.removeClass('dimmed').addClass('highlighted');
        hideNodePopover();
    });

    // 点击空白处恢复
    cy.on('tap', function (evt) {
        if (evt.target === cy) {
            cy.elements().removeClass('dimmed highlighted');
            hideNodePopover();
        }
    });

    setupNodeMarkers();
}

// -------------------------------------------------------------------------
// 视图切换逻辑
// -------------------------------------------------------------------------

function switchView(viewName) {
    currentView = viewName;

    // UI 按钮状态切换
    if (viewName === 'line') {
        dom.btnLineView.classList.add('bg-blue-600', 'text-white', 'shadow-md');
        dom.btnLineView.classList.remove('text-slate-500', 'hover:bg-slate-100');

        dom.btnCarView.classList.remove('bg-blue-600', 'text-white', 'shadow-md');
        dom.btnCarView.classList.add('text-slate-500', 'hover:bg-slate-100');

        // 隐藏详情侧边栏
        toggleSidebar(false);
        dom.sidebarTrigger.classList.add('hidden');
        if (dom.nodeOverlay) {
            dom.nodeOverlay.classList.add('hidden');
        }
        hideNodePopover();

        renderLineGraph();
    } else {
        dom.btnCarView.classList.add('bg-blue-600', 'text-white', 'shadow-md');
        dom.btnCarView.classList.remove('text-slate-500', 'hover:bg-slate-100');

        dom.btnLineView.classList.remove('bg-blue-600', 'text-white', 'shadow-md');
        dom.btnLineView.classList.add('text-slate-500', 'hover:bg-slate-100');

        if (parsedData) {
            toggleSidebar(false);
            dom.sidebarTrigger.classList.remove('hidden');
        }
        if (dom.nodeOverlay) {
            dom.nodeOverlay.classList.remove('hidden');
        }

        renderCarGraph();
    }
}

// -------------------------------------------------------------------------
// 渲染逻辑 (将数据转为 Graph)
// -------------------------------------------------------------------------

function renderLineGraph() {
    if (!parsedData) return;

    // 构建线体视图数据：目前只有一个机器人节点
    const elements = [
        {
            data: {
                id: 'robot_root',
                label: parsedData.robotName || 'Unknown Robot',
                type: 'ROBOT_ROOT'
            }
        }
    ];

    initCy(elements, 'grid');
}

function renderCarGraph() {
    if (!parsedData) return;

    // 转换 RobotInfo 为 Cytoscape Elements
    const elements = [];
    const nodes = new Set();

    // 1. 添加节点
    parsedData.modules.forEach(mod => {
        const nodeId = mod.name || mod.id;
        if (!nodeId || nodes.has(nodeId)) {
            return;
        }
        if (!nodes.has(nodeId)) {
            elements.push({
                data: {
                    id: nodeId,
                    label: mod.value || nodeId,
                    type: mod.type,
                    propertyMap: mod.propertyMap || {},
                    relevantInfo: mod.relevantInfo || ''
                }
            });
            nodes.add(nodeId);
        }
    });

    // 2. 添加边 (调用关系)
    parsedData.calls.forEach((call, index) => {
        if (!nodes.has(call.from)) {
            elements.push({data: {id: call.from, label: call.from, type: 'SRC'}});
            nodes.add(call.from);
        }
        if (!nodes.has(call.to)) {
            elements.push({data: {id: call.to, label: call.to, type: 'SRC'}});
            nodes.add(call.to);
        }

        elements.push({
            data: {
                id: `e${index}`,
                source: call.from,
                target: call.to
            }
        });
    });

    initCy(elements, 'dagre');
    applyAllNodeTypeScales();
    updateSidebar(parsedData);
}

// -------------------------------------------------------------------------
// 数据规范化 (后端 JSON -> 前端结构)
// -------------------------------------------------------------------------

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
        case 'CAR_CODE':
            return 'CAR_CODE';
        case 'P_PROGRAM':
            return 'P_PROGRAM';
        case 'VIRTUAL':
            return 'VIRTUAL';
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

function normalizeTechPacks(rawList) {
    if (!Array.isArray(rawList)) {
        return [];
    }
    if (rawList.length === 0) {
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

            calls.push({
                from: node.id,
                to: child.id
            });

            if (!visited.has(child.id)) {
                stack.push(child);
            }
        });
    }

    return {modules, calls};
}

function normalizeRobotInfo(raw) {
    if (!raw || typeof raw !== 'object') {
        return null;
    }

    if (Array.isArray(raw.modules) && Array.isArray(raw.calls)) {
        const modules = raw.modules.map((mod) => ({
            ...mod,
            name: mod.name || mod.id,
            value: mod.value || mod.name || mod.id,
            type: mapNodeType(mod.type || mod.nodeType),
            propertyMap: mod.propertyMap || {},
            relevantInfo: mod.relevantInfo || ''
        }));
        return {
            ...raw,
            modules,
            techPacks: normalizeTechPacks(raw.techPacks || raw.techPackList)
        };
    }

    const graph = buildGraphFromCallNode(raw.callGraphRoot);
    return {
        robotName: raw.robotName,
        version: raw.version,
        archiveDate: raw.archiveDate,
        techPacks: normalizeTechPacks(raw.techPackList),
        modules: graph.modules,
        calls: graph.calls
    };
}

// -------------------------------------------------------------------------
// UI 辅助功能
// -------------------------------------------------------------------------

function toggleSidebar(forceState) {
    const isHidden = dom.infoSidebar.classList.contains('-translate-x-full');
    const shouldShow = forceState !== undefined ? forceState : isHidden;

    if (shouldShow) {
        dom.infoSidebar.classList.remove('-translate-x-full', 'opacity-0', 'pointer-events-none');
        dom.sidebarTrigger.classList.add('hidden');
    } else {
        dom.infoSidebar.classList.add('-translate-x-full', 'opacity-0', 'pointer-events-none');
        dom.sidebarTrigger.classList.remove('hidden');
    }
}

function updateSidebar(data) {
    dom.metaName.textContent = data.robotName || '--';
    dom.metaVersion.textContent = data.version || '--';
    dom.metaDate.textContent = data.archiveDate || '--';

    dom.techPackList.innerHTML = '';
    if (data.techPacks && data.techPacks.length > 0) {
        data.techPacks.forEach(pack => {
            const div = document.createElement('div');
            div.className = 'bg-slate-50 px-2 py-1.5 rounded border border-slate-100 text-xs flex justify-between';
            div.innerHTML = `
                    <span class="font-medium text-slate-700">${pack.name}</span>
                    <span class="text-slate-400">${pack.version}</span>
                `;
            dom.techPackList.appendChild(div);
        });
    } else {
        dom.techPackList.innerHTML = '<div class="text-sm text-slate-400 italic">暂无数据</div>';
    }
}

function parseNodeTypes(raw) {
    return raw.split(',').map(type => type.trim()).filter(Boolean);
}

function getScaleKey(types) {
    return types.slice().sort().join('|');
}

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
    scheduleMarkerUpdate();
}

function applyAllNodeTypeScales() {
    nodeTypeGroups.forEach((group) => {
        const key = getScaleKey(group.types);
        const scale = nodeTypeScale.get(key) || 1;
        applyNodeTypeScale(group.types, scale);
    });
}

function selectNodesByTypes(types) {
    if (!cy) {
        return;
    }
    cy.nodes().unselect();
    cy.elements().removeClass('dimmed highlighted');
    const selector = types.map((type) => `node[type="${type}"]`).join(', ');
    if (selector) {
        cy.nodes(selector).select();
    }
}

function initNodeControlPanel() {
    const buttons = document.querySelectorAll('.node-type-button');
    const sliders = document.querySelectorAll('.node-size-slider');

    buttons.forEach((button) => {
        button.addEventListener('click', () => {
            const types = parseNodeTypes(button.dataset.nodeTypes || '');
            selectNodesByTypes(types);
        });
    });

    sliders.forEach((slider) => {
        const types = parseNodeTypes(slider.dataset.nodeTypes || '');
        const scaleKey = getScaleKey(types);
        const scaleValue = Number.parseFloat(slider.value) || 1;
        nodeTypeScale.set(scaleKey, scaleValue);

        slider.addEventListener('input', () => {
            const newScale = Number.parseFloat(slider.value) || 1;
            nodeTypeScale.set(scaleKey, newScale);
            applyNodeTypeScale(types, newScale);
        });
    });

    document.addEventListener('click', (event) => {
        if (!dom.nodePopover || dom.nodePopover.classList.contains('hidden')) {
            return;
        }
        if (dom.nodePopover.contains(event.target)) {
            return;
        }
        if (event.target.classList.contains('node-marker')) {
            return;
        }
        hideNodePopover();
    });
}

initNodeControlPanel();

// -------------------------------------------------------------------------
// 上传处理
// -------------------------------------------------------------------------

async function uploadAnalysis() {
    if (!uploadState.zipFile) {
        alert('请先上传备份文件');
        return;
    }
    if (!uploadState.configFile) {
        alert('请先上传配置文件');
        return;
    }

    const formData = new FormData();
    formData.append('file', uploadState.zipFile);
    formData.append('config', uploadState.configFile);

    dom.loader.classList.remove('hidden');
    dom.loader.classList.add('flex');

    try {
        const response = await fetch('/api/analysis', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const errorText = await response.text();
            const message = errorText ? `解析失败: ${errorText}` : `解析失败: ${response.status}`;
            throw new Error(message);
        }

        const rawData = await response.json();
        parsedData = normalizeRobotInfo(rawData);
        if (!parsedData) {
            throw new Error('解析失败: 返回数据格式不正确');
        }
        switchView('car');
    } catch (error) {
        console.error(error);
        alert(error.message || '解析失败，请检查后端日志或接口返回。');
    } finally {
        dom.loader.classList.add('hidden');
        dom.loader.classList.remove('flex');
    }
}

// 监听上传
const zipInput = document.getElementById('fileUpload');
const configInput = document.getElementById('configUpload');

function updateStartButtonState() {
    if (uploadState.zipFile && uploadState.configFile) {
        dom.startAnalysisBtn.disabled = false;
        dom.startAnalysisBtn.classList.remove('action-disabled');
    } else {
        dom.startAnalysisBtn.disabled = true;
        dom.startAnalysisBtn.classList.add('action-disabled');
    }
}

function formatFileLabel(file, defaultLabel) {
    if (!file) {
        return defaultLabel;
    }
    return `${defaultLabel}: ${file.name}`;
}

function isAllowedExtension(file, allowedExtensions) {
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!extension) {
        return false;
    }
    return allowedExtensions.includes(extension);
}

function resetInputFile(input, labelElement, defaultLabel) {
    input.value = '';
    labelElement.textContent = defaultLabel;
}

dom.fileUploadButton.addEventListener('click', () => {
    zipInput.click();
});

dom.configUploadButton.addEventListener('click', () => {
    configInput.click();
});

zipInput.addEventListener('change', (e) => {
    const file = e.target.files[0] || null;
    if (file && !isAllowedExtension(file, ['zip'])) {
        alert('请选择 .zip 格式的备份文件');
        resetInputFile(zipInput, dom.fileUploadText, '上传备份 (.zip)');
        uploadState.zipFile = null;
        updateStartButtonState();
        return;
    }

    uploadState.zipFile = file;
    dom.fileUploadText.textContent = formatFileLabel(uploadState.zipFile, '上传备份 (.zip)');
    updateStartButtonState();
});

configInput.addEventListener('change', (e) => {
    const file = e.target.files[0] || null;
    if (file && !isAllowedExtension(file, ['json', 'yml', 'yaml'])) {
        alert('配置文件仅支持 .json/.yml/.yaml');
        resetInputFile(configInput, dom.configUploadText, '上传配置');
        uploadState.configFile = null;
        updateStartButtonState();
        return;
    }

    uploadState.configFile = file;
    dom.configUploadText.textContent = formatFileLabel(uploadState.configFile, '上传配置');
    updateStartButtonState();
});

dom.startAnalysisBtn.addEventListener('click', () => {
    if (dom.startAnalysisBtn.disabled) {
        alert('请先上传备份和配置文件');
        return;
    }
    uploadAnalysis();
});
