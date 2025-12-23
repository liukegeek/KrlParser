const form = document.getElementById("analysis-form");
const zipInput = document.getElementById("zip-file");
const configInput = document.getElementById("config-file");
const statusEl = document.getElementById("status");
const emptyState = document.getElementById("empty-state");
const graphSvg = document.getElementById("graph");
const graphWrapper = document.querySelector(".graph-wrapper");
const tabButtons = document.querySelectorAll(".tab-button");
const tabPanels = document.querySelectorAll("[data-panel]");
const lineGrid = document.getElementById("line-grid");

const robotNameEl = document.getElementById("robot-name");
const archiveNameEl = document.getElementById("archive-name");
const archiveDateEl = document.getElementById("archive-date");
const archiveVersionEl = document.getElementById("archive-version");
const techPackListEl = document.getElementById("tech-pack-list");

const selectedLabelEl = document.getElementById("selected-label");
const selectedTypeEl = document.getElementById("selected-type");
const selectedIdEl = document.getElementById("selected-id");

const ctx = document.createElement("canvas").getContext("2d");
ctx.font = "600 13px Inter";

let graphState = null;
let currentRobotInfo = null;

const nodeStyles = {
  CELL: { className: "node--cell", shape: "circle" },
  CEll: { className: "node--cell", shape: "circle" },
  CAR_CODE: { className: "node--car-code", shape: "circle" },
  P_PROGRAM: { className: "node--p-program", shape: "rounded" },
  CAR_PROGRAM: { className: "node--car-program", shape: "rounded" },
  ROUTE_PROCESS: { className: "node--route", shape: "rounded" },
  VIRTUAL: { className: "node--virtual", shape: "rounded" }
};

const normalizeType = (type) => (type ? String(type).toUpperCase() : "UNKNOWN");

const setStatus = (message, type = "info") => {
  statusEl.textContent = message;
  statusEl.className = `status ${type === "error" ? "error" : type === "success" ? "success" : ""}`;
};

const setRobotInfo = (info) => {
  robotNameEl.textContent = info?.robotName || "-";
  archiveDateEl.textContent = info?.archiveDate || "-";
  archiveVersionEl.textContent = info?.version || "-";

  techPackListEl.innerHTML = "";
  const packs = info?.techPackList?.length ? info.techPackList : ["-"];
  packs.forEach((pack) => {
    const item = document.createElement("li");
    item.className = "tag";
    item.textContent = pack || "-";
    techPackListEl.appendChild(item);
  });
};

const setSelectedInfo = (node) => {
  if (!node) {
    selectedLabelEl.textContent = "点击节点查看详细信息";
    selectedTypeEl.textContent = "";
    selectedIdEl.textContent = "";
    return;
  }
  selectedLabelEl.textContent = node.value || node.id || "-";
  selectedTypeEl.textContent = `类型：${node.nodeType || "-"}`;
  selectedIdEl.textContent = `ID：${node.id || "-"}`;
};

const wrapLabel = (label, maxChars = 12) => {
  if (!label) {
    return ["-"];
  }
  const text = String(label);
  const words = text.split(/\s+/);
  if (words.length === 1) {
    return text.match(new RegExp(`.{1,${maxChars}}`, "g")) || [text];
  }
  const lines = [];
  let current = "";
  words.forEach((word) => {
    const test = current ? `${current} ${word}` : word;
    if (test.length > maxChars) {
      if (current) {
        lines.push(current);
      }
      current = word;
    } else {
      current = test;
    }
  });
  if (current) {
    lines.push(current);
  }
  return lines;
};

const measureLabel = (lines) => {
  const widths = lines.map((line) => ctx.measureText(line).width);
  const maxWidth = Math.max(...widths, 32);
  return {
    width: maxWidth + 32,
    height: 36 + (lines.length - 1) * 18
  };
};

const collectGraph = (root) => {
  if (!root) {
    return { nodes: new Map(), edges: [] };
  }
  const nodes = new Map();
  const edges = [];
  const edgeSet = new Set();
  const stack = [root];

  while (stack.length) {
    const node = stack.pop();
    if (!node || !node.id) {
      continue;
    }
    if (!nodes.has(node.id)) {
      nodes.set(node.id, node);
    }
    const children = node.children || [];
    children.forEach((child) => {
      if (!child || !child.id) {
        return;
      }
      const edgeKey = `${node.id}__${child.id}`;
      if (!edgeSet.has(edgeKey)) {
        edges.push({ from: node.id, to: child.id });
        edgeSet.add(edgeKey);
      }
      if (!nodes.has(child.id)) {
        stack.push(child);
      }
    });
  }

  return { nodes, edges };
};

const buildLayout = ({ nodes, edges }) => {
  const g = new dagre.graphlib.Graph();
  g.setGraph({
    rankdir: "TB",
    nodesep: 70,
    ranksep: 110,
    marginx: 40,
    marginy: 40
  });
  g.setDefaultEdgeLabel(() => ({}));

  nodes.forEach((node, id) => {
    const labelLines = wrapLabel(node.value || node.id);
    const { width, height } = measureLabel(labelLines);
    const type = normalizeType(node.nodeType);
    const style = nodeStyles[type] || { className: "", shape: "rect" };
    let nodeWidth = width;
    let nodeHeight = height;
    if (style.shape === "circle") {
      const diameter = Math.max(width, height, 56);
      nodeWidth = diameter;
      nodeHeight = diameter;
    } else {
      nodeWidth = Math.max(width, 110);
      nodeHeight = Math.max(height, 48);
    }
    g.setNode(id, {
      width: nodeWidth,
      height: nodeHeight,
      labelLines,
      type,
      className: style.className,
      shape: style.shape
    });
  });

  edges.forEach((edge) => {
    g.setEdge(edge.from, edge.to, { curve: d3.curveMonotoneY });
  });

  dagre.layout(g);
  return g;
};

const renderGraph = (graph, rawData) => {
  const svg = d3.select(graphSvg);
  svg.selectAll("*").remove();
  const width = graph.graph().width;
  const height = graph.graph().height;
  svg.attr("viewBox", `0 0 ${width} ${height}`);

  const defs = svg.append("defs");
  defs
    .append("pattern")
    .attr("id", "virtualPattern")
    .attr("patternUnits", "userSpaceOnUse")
    .attr("width", 8)
    .attr("height", 8)
    .append("path")
    .attr("d", "M0,8 L8,0")
    .attr("stroke", "rgba(148,163,184,0.6)")
    .attr("stroke-width", 2);

  defs
    .append("marker")
    .attr("id", "arrow")
    .attr("viewBox", "0 0 12 12")
    .attr("refX", 10)
    .attr("refY", 6)
    .attr("markerWidth", 10)
    .attr("markerHeight", 10)
    .attr("orient", "auto-start-reverse")
    .append("path")
    .attr("d", "M 0 0 L 12 6 L 0 12 z")
    .attr("fill", "#1e293b");

  const zoomLayer = svg.append("g").attr("class", "graph-layer");
  const zoom = d3
    .zoom()
    .scaleExtent([0.55, 2.5])
    .on("zoom", (event) => {
      zoomLayer.attr("transform", event.transform);
    });
  svg.call(zoom);

  const edgeData = graph.edges().map((edge) => ({
    from: edge.v,
    to: edge.w,
    points: graph.edge(edge).points || []
  }));

  const links = zoomLayer
    .append("g")
    .selectAll("path")
    .data(edgeData.filter((edge) => edge.points.length))
    .enter()
    .append("path")
    .attr("class", "link")
    .attr("marker-end", "url(#arrow)")
    .attr("d", (edge) => d3.line().curve(d3.curveCatmullRom.alpha(0.5))(edge.points));

  const nodes = zoomLayer
    .append("g")
    .selectAll("g")
    .data([...rawData.nodes.values()])
    .enter()
    .append("g")
    .attr("class", (d) => {
      const layout = graph.node(d.id);
      return `node ${layout.className || ""}`;
    })
    .attr("transform", (d) => {
      const layout = graph.node(d.id);
      return `translate(${layout.x}, ${layout.y})`;
    })
    .on("click", (event, d) => {
      event.stopPropagation();
      highlightNode(d.id);
    });

  nodes.each(function renderShape(d) {
    const layout = graph.node(d.id);
    const group = d3.select(this);
    const halfWidth = layout.width / 2;
    const halfHeight = layout.height / 2;

    if (layout.shape === "circle") {
      group
        .append("circle")
        .attr("class", "shape")
        .attr("r", Math.max(halfWidth, halfHeight));
    } else {
      group
        .append("rect")
        .attr("class", "shape")
        .attr("x", -halfWidth)
        .attr("y", -halfHeight)
        .attr("width", layout.width)
        .attr("height", layout.height)
        .attr("rx", 16)
        .attr("ry", 16);
    }
  });

  nodes.each(function renderText(d) {
    const layout = graph.node(d.id);
    const group = d3.select(this);
    const text = group.append("text").attr("text-anchor", "middle");
    layout.labelLines.forEach((line, index) => {
      text
        .append("tspan")
        .attr("x", 0)
        .attr("y", (index - (layout.labelLines.length - 1) / 2) * 16)
        .text(line);
    });
  });

  const adjacency = buildAdjacency(rawData.edges);

  const highlightNode = (selectedId) => {
    if (!selectedId) {
      nodes.classed("is-selected", false).classed("is-ancestor", false).classed("is-descendant", false).classed("is-dimmed", false);
      links.classed("is-highlighted", false);
      setSelectedInfo(null);
      return;
    }

    const ancestors = collectRelated(selectedId, adjacency.incoming);
    const descendants = collectRelated(selectedId, adjacency.outgoing);
    const related = new Set([selectedId, ...ancestors, ...descendants]);

    nodes
      .classed("is-selected", (d) => d.id === selectedId)
      .classed("is-ancestor", (d) => ancestors.has(d.id))
      .classed("is-descendant", (d) => descendants.has(d.id))
      .classed("is-dimmed", (d) => !related.has(d.id));

    links.classed("is-highlighted", (edge) => related.has(edge.from) && related.has(edge.to));

    setSelectedInfo(rawData.nodes.get(selectedId));
  };

  svg.on("click", () => {
    highlightNode(null);
  });

  const fitToView = () => {
    const wrapperRect = graphWrapper.getBoundingClientRect();
    if (!wrapperRect.width || !wrapperRect.height) {
      return;
    }
    const scale = Math.max(0.65, Math.min(wrapperRect.width / width, wrapperRect.height / height, 1));
    const translateX = (wrapperRect.width - width * scale) / 2;
    const translateY = (wrapperRect.height - height * scale) / 2;
    svg.call(zoom.transform, d3.zoomIdentity.translate(translateX, translateY).scale(scale));
  };

  fitToView();

  graphState = { highlightNode };
};

const buildAdjacency = (edges) => {
  const outgoing = new Map();
  const incoming = new Map();
  edges.forEach((edge) => {
    if (!outgoing.has(edge.from)) {
      outgoing.set(edge.from, new Set());
    }
    outgoing.get(edge.from).add(edge.to);

    if (!incoming.has(edge.to)) {
      incoming.set(edge.to, new Set());
    }
    incoming.get(edge.to).add(edge.from);
  });
  return { outgoing, incoming };
};

const collectRelated = (startId, map) => {
  const visited = new Set();
  const queue = [startId];
  while (queue.length) {
    const current = queue.shift();
    const next = map.get(current);
    if (!next) {
      continue;
    }
    next.forEach((node) => {
      if (!visited.has(node)) {
        visited.add(node);
        queue.push(node);
      }
    });
  }
  return visited;
};

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!zipInput.files.length || !configInput.files.length) {
    setStatus("请同时选择备份压缩包与配置文件", "error");
    return;
  }

  const formData = new FormData();
  formData.append("file", zipInput.files[0]);
  formData.append("config", configInput.files[0]);

  setStatus("正在解析调用关系...", "info");
  setSelectedInfo(null);

  try {
    const response = await fetch("/api/analysis", {
      method: "POST",
      body: formData
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error?.message || "解析失败，请检查配置文件与备份包");
    }

    const data = await response.json();
    currentRobotInfo = data;
    setRobotInfo(data);
    const graphData = collectGraph(data.callGraphRoot);
    const layout = buildLayout(graphData);
    renderGraph(layout, graphData);
    updateLineInfo([data]);
    emptyState.style.display = "none";
    setStatus("解析成功，点击节点查看关联链路", "success");
  } catch (error) {
    setStatus(error.message, "error");
  }
});

const updateLineInfo = (robots) => {
  lineGrid.innerHTML = "";
  const list = robots.length ? robots : [{ robotName: "未知机器人" }];
  list.forEach((robot) => {
    const node = document.createElement("div");
    node.className = "line-node";
    node.textContent = robot.robotName || "未命名机器人";
    node.addEventListener("click", () => {
      switchTab("car-info");
    });
    lineGrid.appendChild(node);
  });
};

const switchTab = (tabId) => {
  tabButtons.forEach((button) => {
    button.classList.toggle("is-active", button.dataset.tab === tabId);
  });
  tabPanels.forEach((panel) => {
    panel.hidden = panel.dataset.panel !== tabId;
  });
  if (tabId === "car-info" && graphState?.highlightNode) {
    graphState.highlightNode(null);
  }
};

tabButtons.forEach((button) => {
  button.addEventListener("click", () => {
    switchTab(button.dataset.tab);
  });
});

updateLineInfo([]);
