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
    nodePopover: document.getElementById('nodePopover'),
    nodeControlPanel: document.getElementById('nodeControlPanel'),
    nodeControlTrigger: document.getElementById('nodeControlTrigger'),
    nodeControlClose: document.getElementById('nodeControlClose')
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
const activeTypeSelections = new Set();
let popoverDragState = null;
let lastNodeDoubleClickAt = 0;
let tapTimer = null;

function hideNodePopover() {
    if (dom.nodePopover) {
        dom.nodePopover.classList.add('hidden');
        dom.nodePopover.classList.remove('node-popover-resizable');
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

function showNodePopover(node, side, anchorPosition) {
    if (!dom.nodePopover) {
        return;
    }
    dom.nodePopover.innerHTML = '';
    dom.nodePopover.classList.remove('node-popover-resizable');
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
        dom.nodePopover.classList.add('node-popover-resizable');
        const relevantInfo = node.data('relevantInfo');
        dom.nodePopover.appendChild(createPopoverRow('相关信息', relevantInfo || '--'));
    }
    const containerRect = dom.cy.getBoundingClientRect();
    const parentRect = dom.nodePopover.offsetParent
        ? dom.nodePopover.offsetParent.getBoundingClientRect()
        : {left: 0, top: 0};
    dom.nodePopover.style.left = `${containerRect.left + anchorPosition.x - parentRect.left + 16}px`;
    dom.nodePopover.style.top = `${containerRect.top + anchorPosition.y - parentRect.top + 16}px`;
    dom.nodePopover.classList.remove('hidden');

    title.addEventListener('mousedown', (event) => {
        event.preventDefault();
        popoverDragState = {
            offsetX: event.clientX - dom.nodePopover.offsetLeft,
            offsetY: event.clientY - dom.nodePopover.offsetTop
        };
    });
}

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
                    'corner-radius': '10px'
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
                    'corner-radius': '10px'
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
                    'background-color': '#286395',
                    'line-color': '#286395',
                    'color': '#ffffff',
                    'target-arrow-color': '#286395',
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

        // 1. 如果此时存在 timer，说明这是"双击"动作中的第二次点击
        // 我们要立刻清除单击定时器，并阻止后续的单击逻辑，把舞台留给 dblclick
        if (tapTimer) {
            clearTimeout(tapTimer);
            tapTimer = null;
            return;
        }

        const node = evt.target;

        // 设置定时器，延迟200毫秒执行单击逻辑
        tapTimer = setTimeout(() => {
            // 如果是在线体视图，点击进入车型视图
            if (currentView === 'line') {
                switchView('car');
            }

            // 清除之前的状态
            cy.elements().removeClass('dimmed highlighted');

            console.log("触发单击：高亮链路");
            // 逻辑：高亮被点击节点 + 所有祖先与子孙节点（完整链路）
            const lineage = node.predecessors().add(node).add(node.successors());

            cy.elements().addClass('dimmed');
            lineage.removeClass('dimmed').addClass('highlighted');
            applyTypeHighlights();
            hideNodePopover();
            // --- 单击逻辑结束 ---

            // 【核心修复】: 定时器执行完毕后，必须手动将 ID 置空！
            // 否则下一次点击会误以为还有一个挂起的定时器而被直接 return 掉
            tapTimer = null;
        }, 250);


    });

    // 点击空白处恢复
    cy.on('tap', function (evt) {
        if (evt.target === cy) {
            cy.elements().removeClass('dimmed highlighted');
            applyTypeHighlights();
            hideNodePopover();
        }
    });

    // 双击节点：弹出弹窗，展示属性信息:文件路径、创建时间、修改时间
    cy.on('dblclick', 'node', function (evt) {
        // 双击保险：再次尝试清除定时器（防止极端情况）
        if (tapTimer) {
            clearTimeout(tapTimer);
            tapTimer = null;
        }


        console.log("触发双击：属性视图");
        lastNodeDoubleClickAt = Date.now();
        const node = evt.target;
        // 获取位置
        const renderedPosition = evt.renderedPosition || node.renderedPosition();
        const position = {
            x: renderedPosition.x,
            y: renderedPosition.y
        };
        // 显示弹窗
        showNodePopover(node, 'left', position);
    });

    // 右键点击节点：弹出弹窗，展示调用的上下文信息
    cy.on('cxttap', 'node', function (evt) {
        const node = evt.target;
        const renderedPosition = evt.renderedPosition || node.renderedPosition();
        const position = {
            x: renderedPosition.x,
            y: renderedPosition.y
        };
        showNodePopover(node, 'right', position);
    });
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
        toggleNodeControls(false);
        if (dom.nodeControlTrigger) {
            dom.nodeControlTrigger.classList.add('hidden');
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
        toggleNodeControls(false);
        if (dom.nodeControlTrigger) {
            dom.nodeControlTrigger.classList.remove('hidden');
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
    applyTypeHighlights();
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

function toggleNodeControls(forceState) {
    if (!dom.nodeControlPanel || !dom.nodeControlTrigger) {
        return;
    }
    const isHidden = dom.nodeControlPanel.classList.contains('-translate-x-full');
    const shouldShow = forceState !== undefined ? forceState : isHidden;

    if (shouldShow) {
        dom.nodeControlPanel.classList.remove('-translate-x-full', 'opacity-0', 'pointer-events-none');
        dom.nodeControlTrigger.classList.add('hidden');
    } else {
        dom.nodeControlPanel.classList.add('-translate-x-full', 'opacity-0', 'pointer-events-none');
        dom.nodeControlTrigger.classList.remove('hidden');
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
    applyTypeHighlights();
}

function applyTypeHighlights() {
    if (!cy) {
        return;
    }
    if (activeTypeSelections.size === 0) {
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

function initNodeControlPanel() {
    const buttons = document.querySelectorAll('.node-type-button');
    const sliders = document.querySelectorAll('.node-size-slider');

    buttons.forEach((button) => {
        button.addEventListener('click', () => {
            const types = parseNodeTypes(button.dataset.nodeTypes || '');
            const isActive = button.classList.contains('active');
            if (isActive) {
                button.classList.remove('active');
                types.forEach((type) => activeTypeSelections.delete(type));
            } else {
                button.classList.add('active');
                types.forEach((type) => activeTypeSelections.add(type));
            }
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
        // 1. 如果弹窗本来就是关着的，不用管
        if (!dom.nodePopover || dom.nodePopover.classList.contains('hidden')) {
            return;
        }
        // 2. 如果点击的是弹窗自己内部，不用管
        if (dom.nodePopover.contains(event.target)) {
            return;
        }
        // 3. 【新增关键代码】如果点击的是 Cytoscape 画布区域，也不用管
        // 因为 cy.on('tap') 会专门处理画布内的关闭逻辑，这里处理会造成冲突
        if (dom.cy.contains(event.target)) {
            return;
        }
        hideNodePopover();
    });

    if (dom.cy) {
        dom.cy.addEventListener('contextmenu', (event) => {
            event.preventDefault();
        });
    }

    if (dom.nodeControlTrigger) {
        dom.nodeControlTrigger.addEventListener('click', () => toggleNodeControls());
    }
    if (dom.nodeControlClose) {
        dom.nodeControlClose.addEventListener('click', () => toggleNodeControls(false));
    }
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