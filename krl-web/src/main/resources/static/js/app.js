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
    startAnalysisBtn: document.getElementById('startAnalysisBtn')
};

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
            // 1. CELL / CAR_CODE: 圆形，橙色
            {
                selector: 'node[type="CELL"], node[type="CAR_CODE"]',
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
            // 2. 普通 SRC 程序: 圆角矩形，蓝色
            {
                selector: 'node[type="SRC"]',
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
                selector: 'node[type="CELL"].highlighted, node[type="CAR_CODE"].highlighted',
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

        // 逻辑：高亮被点击节点 + 它的直接邻居 (入度和出度)
        const neighborhood = node.neighborhood().add(node);

        cy.elements().addClass('dimmed');
        neighborhood.removeClass('dimmed').addClass('highlighted');
    });

    // 点击空白处恢复
    cy.on('tap', function (evt) {
        if (evt.target === cy) {
            cy.elements().removeClass('dimmed highlighted');
        }
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
        if (!nodes.has(mod.name)) {
            elements.push({
                data: {
                    id: mod.name,
                    label: mod.value || mod.name,
                    type: mod.type
                }
            });
            nodes.add(mod.name);
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
    updateSidebar(parsedData);
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

        parsedData = await response.json();
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
