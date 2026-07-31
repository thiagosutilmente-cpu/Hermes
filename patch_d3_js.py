import sys
import re

with open('index.html', 'r') as f:
    content = f.read()

d3_js_code = """    window.startP2PNetworkDiagnosis = function() {
        const container = document.getElementById('p2pMeshVisualizer');
        const countEl = document.getElementById('p2pActiveNodesCount');
        const avgLatEl = document.getElementById('p2pAvgLatency');
        const effEl = document.getElementById('p2pRoutingEfficiency');
        const listEl = document.getElementById('p2pCoopList');
        
        if (!container) return;

        if (window.showToast) window.showToast("Iniciando escaneamento de topologia P2P com D3.js...", "info");
        container.innerHTML = '<div style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: var(--accent-blue); font-size: 11px; display: flex; flex-direction: column; align-items: center; gap: 10px;"><div class="spinner" style="border-top-color: #A200FF;"></div> Renderizando Mesh Quântico...</div>';
        if (listEl) listEl.innerHTML = '<div style="text-align: center; color: var(--text-dim);">Buscando cooperados...</div>';
        
        // Remove old tooltips if exist
        if (typeof d3 !== 'undefined') d3.select('#p2pTooltip').remove();

        setTimeout(() => {
            if (typeof d3 === 'undefined') {
                container.innerHTML = '<div style="color: red; text-align: center; padding-top: 20px;">Erro: D3.js não carregado.</div>';
                return;
            }

            const numNodes = 15 + Math.floor(Math.random() * 10);
            countEl.innerText = `MAPEADOS ${numNodes} NÓS ATIVOS`;
            container.innerHTML = '';
            if (listEl) listEl.innerHTML = '';
            
            // Create tooltip
            const tooltip = d3.select('body').append('div')
                .attr('id', 'p2pTooltip')
                .style('position', 'absolute')
                .style('opacity', 0)
                .style('background', 'rgba(10, 11, 18, 0.95)')
                .style('border', '1px solid #A200FF')
                .style('border-radius', '8px')
                .style('padding', '10px')
                .style('pointer-events', 'none')
                .style('font-family', 'monospace')
                .style('color', '#fff')
                .style('box-shadow', '0 0 15px rgba(162, 0, 255, 0.4)')
                .style('z-index', 999999);
            
            const width = container.clientWidth || 400;
            const height = container.clientHeight || 200;
            
            const nodes = Array.from({length: numNodes}, (_, i) => ({
                id: `node_${i}`,
                indexId: i,
                latency: 10 + Math.floor(Math.random() * 150),
                label: i === 0 ? "HOST (VOCÊ)" : `Cooperado #${1000 + i}`,
                workload: (i === 0) ? 'Controle Root' : Math.floor(Math.random() * 5) + ' entregas',
                radius: i === 0 ? 10 : (Math.random() > 0.8 ? 7 : 5)
            }));

            // Build List
            if (listEl) {
                nodes.slice(1, 11).forEach(n => {
                    const color = n.latency < 50 ? '#00F5D4' : (n.latency < 100 ? '#3A86FF' : '#FF006E');
                    const div = document.createElement('div');
                    div.style.cssText = `display: flex; justify-content: space-between; padding: 6px 10px; background: rgba(255,255,255,0.02); border-radius: 6px; cursor: pointer; border-left: 2px solid ${color}; margin-bottom: 4px; transition: background 0.2s;`;
                    div.innerHTML = `
                        <div style="font-weight: bold; color: #fff;">${n.label}</div>
                        <div style="color: ${color}; font-family: monospace;">${n.latency}ms</div>
                    `;
                    div.onclick = () => {
                        // Highlight node in graph
                        d3.selectAll('.p2p-node').attr('r', d => d.radius).style('filter', 'none');
                        const targetNode = d3.select(`#p2p-node-${n.indexId}`);
                        if (!targetNode.empty()) {
                            targetNode.attr('r', n.radius + 5).style('filter', 'drop-shadow(0 0 8px #fff)');
                            
                            // Simulate click to show tooltip near node
                            const nodeData = targetNode.datum();
                            
                            // Get svg coordinates relative to page
                            const svgRect = container.getBoundingClientRect();
                            
                            tooltip.transition().duration(200).style('opacity', 1);
                            tooltip.html(`
                                <div style="font-size: 13px; font-weight: 900; color: ${nodeData.id === 'node_0' ? '#A200FF' : '#fff'}; margin-bottom: 5px;">${nodeData.label}</div>
                                <div style="font-size: 11px; color: #ccc; margin-bottom: 3px;">Latência: <span style="color: ${color}; font-weight: bold;">${nodeData.latency}ms</span></div>
                                <div style="font-size: 11px; color: #ccc;">Carga (Workload): <span style="color: #00F5D4; font-weight: bold;">${nodeData.workload}</span></div>
                            `)
                            .style('left', (svgRect.left + nodeData.x + 15 + window.scrollX) + 'px')
                            .style('top', (svgRect.top + nodeData.y - 28 + window.scrollY) + 'px');
                            
                            if (window.triggerHapticFeedback) window.triggerHapticFeedback('light');
                            
                            setTimeout(() => {
                                tooltip.transition().duration(500).style('opacity', 0);
                                targetNode.attr('r', n.radius).style('filter', 'none');
                            }, 3000);
                        }
                    };
                    div.onmouseenter = () => div.style.background = 'rgba(255,255,255,0.1)';
                    div.onmouseleave = () => div.style.background = 'rgba(255,255,255,0.02)';
                    listEl.appendChild(div);
                });
            }

            const links = [];
            for (let i = 0; i < numNodes; i++) {
                const numConnections = Math.floor(Math.random() * 3) + 1;
                for (let j = 0; j < numConnections; j++) {
                    const target = Math.floor(Math.random() * numNodes);
                    if (target !== i) {
                        links.push({ source: i, target: target });
                    }
                }
            }

            const svg = d3.select(container).append('svg')
                .attr('width', width)
                .attr('height', height)
                .style('background', 'transparent');

            const g = svg.append('g');

            const simulation = d3.forceSimulation(nodes)
                .force('link', d3.forceLink(links).distance(30).strength(0.5))
                .force('charge', d3.forceManyBody().strength(-60))
                .force('center', d3.forceCenter(width / 2, height / 2))
                .force('collision', d3.forceCollide().radius(10));

            const link = g.append('g')
                .selectAll('line')
                .data(links)
                .enter().append('line')
                .attr('stroke', 'rgba(162, 0, 255, 0.3)')
                .attr('stroke-width', 1.5);

            function pulseLinks() {
                link.transition()
                    .duration(1500)
                    .attr('stroke', 'rgba(162, 0, 255, 0.8)')
                    .attr('stroke-width', 2.5)
                    .transition()
                    .duration(1500)
                    .attr('stroke', 'rgba(162, 0, 255, 0.3)')
                    .attr('stroke-width', 1.5)
                    .on('end', pulseLinks);
            }
            pulseLinks();

            const node = g.append('g')
                .selectAll('circle')
                .data(nodes)
                .enter().append('circle')
                .attr('class', 'p2p-node')
                .attr('id', d => `p2p-node-${d.indexId}`)
                .attr('r', d => d.radius)
                .attr('fill', d => d.id === 'node_0' ? '#A200FF' : (d.latency < 50 ? '#00F5D4' : (d.latency < 100 ? '#3A86FF' : '#FF006E')))
                .attr('stroke', d => d.id === 'node_0' ? '#fff' : 'none')
                .attr('stroke-width', 2)
                .style('cursor', 'pointer')
                .call(d3.drag()
                    .on('start', (event, d) => {
                        if (!event.active) simulation.alphaTarget(0.3).restart();
                        d.fx = d.x;
                        d.fy = d.y;
                    })
                    .on('drag', (event, d) => {
                        d.fx = event.x;
                        d.fy = event.y;
                    })
                    .on('end', (event, d) => {
                        if (!event.active) simulation.alphaTarget(0);
                        d.fx = null;
                        d.fy = null;
                    })
                )
                .on('mouseover', function(event, d) {
                    d3.select(this).attr('r', d.radius + 3).style('filter', 'drop-shadow(0 0 5px #fff)');
                })
                .on('mouseout', function(event, d) {
                    d3.select(this).attr('r', d.radius).style('filter', 'none');
                })
                .on('click', function(event, d) {
                    tooltip.transition().duration(200).style('opacity', 1);
                    tooltip.html(`
                        <div style="font-size: 13px; font-weight: 900; color: ${d.id === 'node_0' ? '#A200FF' : '#fff'}; margin-bottom: 5px;">${d.label}</div>
                        <div style="font-size: 11px; color: #ccc; margin-bottom: 3px;">Latência: <span style="color: ${d.latency < 50 ? '#00F5D4' : (d.latency < 100 ? '#3A86FF' : '#FF006E')}; font-weight: bold;">${d.latency}ms</span></div>
                        <div style="font-size: 11px; color: #ccc;">Carga (Workload): <span style="color: #00F5D4; font-weight: bold;">${d.workload}</span></div>
                    `)
                    .style('left', (event.pageX + 15) + 'px')
                    .style('top', (event.pageY - 28) + 'px');
                    
                    if (window.triggerHapticFeedback) window.triggerHapticFeedback('light');
                    
                    setTimeout(() => {
                        tooltip.transition().duration(500).style('opacity', 0);
                    }, 4000);
                });

            simulation.on('tick', () => {
                link
                    .attr('x1', d => d.source.x)
                    .attr('y1', d => d.source.y)
                    .attr('x2', d => d.target.x)
                    .attr('y2', d => d.target.y);

                node
                    .attr('cx', d => Math.max(d.radius, Math.min(width - d.radius, d.x)))
                    .attr('cy', d => Math.max(d.radius, Math.min(height - d.radius, d.y)));
            });

            const avgLat = nodes.reduce((acc, n) => acc + n.latency, 0) / nodes.length;
            avgLatEl.innerText = `${Math.floor(avgLat)} ms`;
            avgLatEl.style.color = avgLat < 60 ? 'var(--success)' : (avgLat < 100 ? 'var(--accent-blue)' : 'var(--accent-pink)');
            
            effEl.innerText = `${(92 + Math.random() * 7.5).toFixed(1)}%`;
            
            if (window.showToast) window.showToast("Malha Quântica P2P sincronizada.", "success");
            if (window.speakText) window.speakText("Thiago, a topologia da rede cooperativa foi montada. Clique na lista abaixo ou nos nós para visualizar detalhes.");
            
        }, 2500);
    };"""

pattern = re.compile(r'    window\.startP2PNetworkDiagnosis = function\(\) \{.*?    \};', re.DOTALL)
new_content = pattern.sub(d3_js_code, content)

with open('index.html', 'w') as f:
    f.write(new_content)

print("D3 JS updated!")
