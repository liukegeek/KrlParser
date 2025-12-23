// -------------------------------------------------------------------------
// 1. 初始化与插件注册
// -------------------------------------------------------------------------

// 初始化图标库
lucide.createIcons();

// 注册 Cytoscape Dagre 布局插件
// 这是一个必要的步骤，否则无法使用 'dagre' 布局
try {
  if (typeof cytoscapeDagre !== 'undefined') {
    cytoscape.use(cytoscapeDagre);
    console.log("Cytoscape dagre extension registered.");
  }
} catch(e) {
  console.warn("Cytoscape dagre registration check failed (it might be already registered).", e);
}

// 全局变量
let cy = null;
let currentView = 'car'; // 'line' | 'car'
let globalRobotData = null; // 存储解析后的完整 RobotInfo 对象

// DOM 元素缓存
const dom = {
  cy: document.getElementById('cy'),
  sidebar: document.getElementById('infoSidebar'),
  btnToggleInfo: document.getElementById('btnToggleInfo'),
  loader: document.getElementById('loader'),

  // Meta fields
  metaName: document.getElementById('metaName'),
  metaVersion: document.getElementById('metaVersion'),
  metaDate: document.getElementById('metaDate'),
  techPackList: document.getElementById('techPackList'),
  techPackCount: document.getElementById('techPackCount'),

  // View Buttons
  btnLineView: document.getElementById('btnLineView'),
  btnCarView: document.getElementById('btnCarView')
};

// -------------------------------------------------------------------------
// 2. 核心逻辑：数据处理 (Recursive JSON Parsing)
// -------------------------------------------------------------------------

/**
 * 将嵌套的 JSON 树结构转换为 Cytoscape 所需的 Nodes 和 Edges 数组
 * @param {Object} rootNode - JSON 中的 callGraphRoot
 * @returns {Array} elements - Cytoscape 元素数组
 */
function parseGraphData(rootNode) {
  const elements = [];
  const addedNodes = new Set(); // 防止重复添加节点

  // 递归遍历函数
  function traverse(node, parentId = null) {
    if (!node) return;

    // 1. 添加节点 (如果尚未添加)
    // 假设 node.id 是唯一的
    if (!addedNodes.has(node.id)) {

      // 根据 nodeType 决定样式分类
      // CELL 和 CAR_CODE 为圆形，其他为默认
      let shapeType = 'normal';
      if (node.nodeType === 'CEll' || node.nodeType === 'CAR_CODE') {
        shapeType = 'entry';
      } else if (node.nodeType === 'ROUTE_PROCESS') {
        shapeType = 'process';
      }

      elements.push({
        group: 'nodes',
        data: {
          id: node.id,
          label: node.value, // 显示的名字
          type: node.nodeType,
          shapeCategory: shapeType,
          fullInfo: node.relevantInfo // 存储完整信息供点击查看
        }
      });
      addedNodes.add(node.id);
    }

    // 2. 添加边 (如果有父节点)
    if (parentId) {
      // 边的 ID 可以简单组合
      const edgeId = `${parentId}->${node.id}`;
      elements.push({
        group: 'edges',
        data: {
          id: edgeId,
          source: parentId,
          target: node.id
        }
      });
    }

    // 3. 递归处理子节点
    if (node.children && node.children.length > 0) {
      node.children.forEach(child => traverse(child, node.id));
    }
  }

  // 从根节点开始遍历
  traverse(rootNode);
  return elements;
}

// -------------------------------------------------------------------------
// 3. Cytoscape 渲染与配置
// -------------------------------------------------------------------------

function renderGraph(elements, layoutMode = 'dagre') {
  // 销毁旧实例
  if (cy) {
    cy.destroy();
  }

  // 布局配置
  let layoutConfig;
  if (layoutMode === 'grid') {
    layoutConfig = { name: 'grid', rows: 2, padding: 50 };
  } else {
    // Dagre 配置 (树状分层)
    layoutConfig = {
      name: 'dagre',
      rankDir: 'TB', // Top to Bottom
      nodeSep: 60,   // 节点间横向间距
      rankSep: 100,  // 层级间纵向间距
      padding: 40,
      animate: true,
      animationDuration: 600
    };
  }

  cy = cytoscape({
    container: dom.cy,
    elements: elements,
    minZoom: 0.1,
    maxZoom: 3,
    wheelSensitivity: 0.2, // 平滑缩放

    layout: layoutConfig,

    style: [
      // --- 节点通用样式 ---
      {
        selector: 'node',
        style: {
          'label': 'data(label)',
          'text-valign': 'center',
          'text-halign': 'center',
          'color': '#fff',
          'font-size': '12px',
          'font-weight': 'bold',
          'text-outline-width': 0,
          'text-wrap': 'wrap',
          'text-max-width': '90px',
          'transition-property': 'background-color, line-color, opacity, width, height',
          'transition-duration': '0.3s'
        }
      },

      // --- 样式分类 1: 入口点 (CELL, CAR_CODE) ---
      // 要求：圆形，鲜艳颜色
      {
        selector: 'node[shapeCategory="entry"]',
        style: {
          'shape': 'ellipse', // 圆形
          'width': '70px',
          'height': '70px',
          'background-color': '#f43f5e', // Rose-500
          'border-width': 4,
          'border-color': '#fda4af', // Rose-300
          'border-opacity': 0.5,
          'shadow-blur': 12,
          'shadow-color': '#f43f5e',
          'shadow-opacity': 0.3
        }
      },

      // --- 样式分类 2: 普通程序 (SRC, P_PROGRAM) ---
      // 要求：圆角矩形，蓝色系
      {
        selector: 'node[shapeCategory="normal"]',
        style: {
          'shape': 'round-rectangle',
          'width': '100px',
          'height': '40px',
          'background-color': '#4f46e5', // Indigo-600
          'border-radius': '8px'
        }
      },

      // --- 样式分类 3: 路由/过程 (ROUTE_PROCESS) ---
      // 要求：圆角，橙色系
      {
        selector: 'node[shapeCategory="process"]',
        style: {
          'shape': 'round-rectangle',
          'width': '90px',
          'height': '40px',
          'background-color': '#f59e0b', // Amber-500
        }
      },

      // --- 连线样式 ---
      {
        selector: 'edge',
        style: {
          'width': 2,
          'curve-style': 'bezier', // 贝塞尔曲线
          'line-color': '#cbd5e1', // Slate-300
          'target-arrow-color': '#94a3b8', // Slate-400
          'target-arrow-shape': 'triangle',
          'arrow-scale': 1.0
        }
      },

      // --- 交互状态: 高亮 ---
      {
        selector: '.highlighted',
        style: {
          'background-color': '#0ea5e9', // Sky-500 (Highlighter)
          'line-color': '#0ea5e9',
          'target-arrow-color': '#0ea5e9',
          'z-index': 999
        }
      },

      // --- 交互状态: 变暗 (非相关) ---
      {
        selector: '.dimmed',
        style: {
          'opacity': 0.1,
          'z-index': 0
        }
      },

      // --- 线体视图的大节点 ---
      {
        selector: 'node[type="ROBOT_ROOT"]',
        style: {
          'shape': 'ellipse',
          'width': '140px',
          'height': '140px',
          'background-color': '#ea580c', // Orange-600
          'font-size': '16px',
          'border-width': 6,
          'border-color': '#fed7aa',
          'text-margin-y': 0
        }
      }
    ]
  });

  // 绑定点击事件：高亮链路
  cy.on('tap', 'node', function(evt){
    const node = evt.target;

    // 线体视图点击跳转
    if (currentView === 'line' && node.data('type') === 'ROBOT_ROOT') {
      window.switchView('car');
      return;
    }

    // 普通视图：高亮逻辑
    // 1. 重置
    cy.elements().removeClass('dimmed highlighted');

    // 2. 全部变暗
    cy.elements().addClass('dimmed');

    // 3. 高亮自己 + 邻居 (入度和出度)
    const connectedEdges = node.connectedEdges();
    const connectedNodes = connectedEdges.connectedNodes();

    node.removeClass('dimmed').addClass('highlighted');
    connectedEdges.removeClass('dimmed').addClass('highlighted');
    connectedNodes.removeClass('dimmed').addClass('highlighted');

    // 可以在这里拓展：在侧边栏显示 node.data('relevantInfo') 代码片段
    console.log("Clicked node code:", node.data('fullInfo'));
  });

  // 点击空白处还原
  cy.on('tap', function(evt){
    if(evt.target === cy){
      cy.elements().removeClass('dimmed highlighted');
    }
  });
}

// -------------------------------------------------------------------------
// 4. UI 交互与视图切换
// -------------------------------------------------------------------------

/**
 * 切换侧边栏显示状态
 * @param {boolean} [show] - 强制显示或隐藏，不传则切换
 */
window.toggleSidebar = function(show) {
  if (show === undefined) {
    dom.sidebar.classList.toggle('show');
    dom.sidebar.classList.toggle('-translate-x-[120%]');
  } else if (show) {
    dom.sidebar.classList.add('show');
    dom.sidebar.classList.remove('-translate-x-[120%]');
  } else {
    dom.sidebar.classList.remove('show');
    dom.sidebar.classList.add('-translate-x-[120%]');
  }
};

// 按钮监听
dom.btnToggleInfo.addEventListener('click', () => window.toggleSidebar());

/**
 * 切换视图模式 (线体 vs 车型)
 */
window.switchView = function(viewName) {
  if (!globalRobotData) return; // 无数据不切换

  currentView = viewName;

  // 更新按钮样式
  const btns = [dom.btnLineView, dom.btnCarView];
  btns.forEach(btn => {
    if ((viewName === 'line' && btn === dom.btnLineView) || (viewName === 'car' && btn === dom.btnCarView)) {
      btn.classList.add('bg-indigo-600', 'text-white', 'shadow-md');
      btn.classList.remove('text-slate-500', 'hover:bg-slate-100');
    } else {
      btn.classList.remove('bg-indigo-600', 'text-white', 'shadow-md');
      btn.classList.add('text-slate-500', 'hover:bg-slate-100');
    }
  });

  if (viewName === 'line') {
    // 渲染线体视图：一个代表机器人的大节点
    const elements = [{
      group: 'nodes',
      data: {
        id: 'root_robot',
        label: globalRobotData.robotName,
        type: 'ROBOT_ROOT'
      }
    }];
    renderGraph(elements, 'grid');
    window.toggleSidebar(false); // 线体视图默认不看详情
  } else {
    // 渲染车型视图：复杂的调用图
    const elements = parseGraphData(globalRobotData.callGraphRoot);
    renderGraph(elements, 'dagre');
    window.toggleSidebar(true); // 车型视图自动弹出详情
  }
};

/**
 * 更新侧边栏数据
 */
function updateSidebarInfo(data) {
  dom.metaName.textContent = data.robotName || 'Unknown';
  dom.metaVersion.textContent = data.version || '--';
  // 简单处理日期格式，去掉秒
  dom.metaDate.textContent = (data.archiveDate || '').replace(/_/g, ' ');

  // 更新包列表
  dom.techPackList.innerHTML = '';
  const packs = data.techPackList || [];
  dom.techPackCount.textContent = packs.length;

  if (packs.length === 0) {
    dom.techPackList.innerHTML = '<div class="text-xs text-slate-300 italic text-center py-2">No packages installed</div>';
  } else {
    packs.forEach(packName => {
      const div = document.createElement('div');
      div.className = 'bg-white p-2 rounded border border-slate-100 text-[11px] text-slate-600 font-medium shadow-sm flex items-start gap-2';
      div.innerHTML = `<i data-lucide="package" class="w-3 h-3 text-indigo-400 mt-0.5 shrink-0"></i> <span>${packName}</span>`;
      dom.techPackList.appendChild(div);
    });
    lucide.createIcons(); // 重新渲染新插入的图标
  }
}

// -------------------------------------------------------------------------
// 5. 模拟数据与上传处理
// -------------------------------------------------------------------------

/**
 * 模拟上传并生成数据 (基于用户提供的 JSON 模版)
 */
window.simulateUpload = function() {
  dom.loader.classList.remove('hidden');
  dom.loader.classList.add('flex');

  // 模拟网络延迟
  setTimeout(() => {
    // 模拟后端返回的 JSON 数据
    const mockResponse = {
      "robotName": "EC010_L1",
      "archiveName": "E:EC010_L1.zip",
      "archiveDate": "2025-09-23_10-09-02",
      "version": "V8.6.11 HF1",
      "techPackList": [
        "DiagnosisSafety",
        "KUKA.BoardPackage",
        "KUKA.Profinet MS",
        "ServoGunBasic"
      ],
      // 简化版的 callGraphRoot，用于演示
      "callGraphRoot": {
        "id": "CEll:cell",
        "value": "cell",
        "nodeType": "CEll",
        "relevantInfo": "DEF CELL()...END",
        "children": [
          {
            "id": "P_PROGRAM:p90",
            "value": "p90",
            "nodeType": "P_PROGRAM",
            "relevantInfo": "...",
            "children": [
              {
                "id": "900:0",
                "value": "900",
                "nodeType": "CAR_CODE",
                "relevantInfo": "...",
                "children": [
                  {
                    "id": "CAR_PROGRAM:sghbwp",
                    "value": "sghbwp",
                    "nodeType": "CAR_PROGRAM",
                    "relevantInfo": "...",
                    "children": [
                      {
                        "id": "ROUTE_PROCESS:sgh_weld_a",
                        "value": "sgh_weld_a",
                        "nodeType": "ROUTE_PROCESS",
                        "relevantInfo": "...",
                        "children": []
                      }
                    ]
                  }
                ]
              }
            ]
          },
          {
            "id": "P_PROGRAM:p92",
            "value": "p92",
            "nodeType": "P_PROGRAM",
            "children": []
          }
        ]
      }
    };

    // 处理数据
    globalRobotData = mockResponse;
    updateSidebarInfo(globalRobotData);

    // 默认进入车型视图
    window.switchView('car');

    // 隐藏 loading
    dom.loader.classList.add('opacity-0');
    setTimeout(() => {
      dom.loader.classList.remove('flex', 'opacity-0');
      dom.loader.classList.add('hidden');
    }, 300); // 等待淡出动画

  }, 1000);
};

// 监听真实上传 (预留接口)
document.getElementById('fileUpload').addEventListener('change', (e) => {
  if (e.target.files.length > 0) {
    alert("在真实环境中，这里将调用 fetch('/api/analysis', ...) 并将返回的 JSON 传给 globalRobotData");
    window.simulateUpload(); // 暂时用模拟数据代替
  }
});