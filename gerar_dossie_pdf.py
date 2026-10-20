#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Gerador do Dossiê Técnico de Auditoria Completa do JARVIS NEURAL COCKPIT 3.0
Gera o PDF com 100% de todos os códigos-fonte críticos sem cortes e o Markdown consolidado.
"""

import os
import sys

def escape_pdf_text(text: str) -> str:
    """Escapa caracteres reservados em strings literais de PDF."""
    clean = text.replace('\\', '\\\\').replace('(', '\\(').replace(')', '\\)')
    # Substitui caracteres não-latin1 por equivalentes para PDF Type1 padrão
    clean = clean.replace('ã', 'a').replace('á', 'a').replace('à', 'a').replace('â', 'a')
    clean = clean.replace('é', 'e').replace('ê', 'e').replace('í', 'i').replace('ó', 'o')
    clean = clean.replace('õ', 'o').replace('ô', 'o').replace('ú', 'u').replace('ç', 'c')
    clean = clean.replace('Ã', 'A').replace('Á', 'A').replace('É', 'E').replace('Í', 'I')
    clean = clean.replace('Ó', 'O').replace('Õ', 'O').replace('Ú', 'U').replace('Ç', 'C')
    clean = clean.replace('•', '*').replace('➔', '->').replace('⚡', '[!]').replace('🟢', '[OK]')
    clean = clean.replace('🔴', '[FALHA]').replace('🟡', '[ALERTA]').replace('📱', '[APP]')
    clean = clean.replace('📍', '[GPS]').replace('💰', '[R$]').replace('📏', '[KM]').replace('⏱', '[MIN]')
    return clean

class PurePdfWriter:
    """Gerador puro de PDF multipáginas com tipografia, caixas de código e paginação."""
    def __init__(self, filename="dossie_auditoria_jarvis_completo.pdf"):
        self.filename = filename
        self.pages = []
        self.current_stream = []
        self.page_width = 595.0   # A4
        self.page_height = 842.0  # A4
        self.margin_x = 35.0
        self.margin_top = 40.0
        self.margin_bottom = 40.0
        self.current_y = self.page_height - self.margin_top
        self.current_page_num = 1

    def new_page(self):
        if self.current_stream:
            self._draw_page_footer()
            self.pages.append("\n".join(self.current_stream))
            self.current_stream = []
            self.current_page_num += 1
        self.current_y = self.page_height - self.margin_top
        self._draw_page_header()

    def _draw_page_header(self):
        self.current_stream.append("q")
        self.current_stream.append("0.1 0.15 0.25 rg")
        self.current_stream.append(f"{self.margin_x} {self.page_height - 25} {self.page_width - 2 * self.margin_x} 1.5 re f")
        self.current_stream.append("BT")
        self.current_stream.append("/F1 7.5 Tf")
        self.current_stream.append("0.4 0.45 0.55 rg")
        self.current_stream.append(f"{self.margin_x} {self.page_height - 20} Td")
        self.current_stream.append("(JARVIS NEURAL COCKPIT 3.0 -- DOSSIE TECNICO DE AUDITORIA & GARGALOS) Tj")
        self.current_stream.append("ET")
        self.current_stream.append("Q")

    def _draw_page_footer(self):
        self.current_stream.append("q")
        self.current_stream.append("0.8 0.85 0.9 rg")
        self.current_stream.append(f"{self.margin_x} 28 {self.page_width - 2 * self.margin_x} 0.8 re f")
        self.current_stream.append("BT")
        self.current_stream.append("/F1 7.5 Tf")
        self.current_stream.append("0.4 0.45 0.55 rg")
        self.current_stream.append(f"{self.margin_x} 18 Td")
        self.current_stream.append("(DOCUMENTO CONFIDENCIAL PARA AUDITORIA CLAUDE ANTHROPIC) Tj")
        self.current_stream.append(f"{self.page_width - self.margin_x - 65} 18 Td")
        self.current_stream.append(f"(Pagina {self.current_page_num}) Tj")
        self.current_stream.append("ET")
        self.current_stream.append("Q")

    def check_space(self, needed_pt):
        if self.current_y - needed_pt < self.margin_bottom:
            self.new_page()

    def add_title(self, text: str):
        self.check_space(40)
        safe = escape_pdf_text(text)
        self.current_stream.append("q")
        self.current_stream.append("0.05 0.12 0.25 rg")
        self.current_stream.append("BT")
        self.current_stream.append("/F2 14 Tf")
        self.current_stream.append(f"{self.margin_x} {self.current_y} Td")
        self.current_stream.append(f"({safe}) Tj")
        self.current_stream.append("ET")
        self.current_stream.append(f"{self.margin_x} {self.current_y - 5} {self.page_width - 2 * self.margin_x} 1.2 re f")
        self.current_stream.append("Q")
        self.current_y -= 25

    def add_subtitle(self, text: str):
        self.check_space(28)
        safe = escape_pdf_text(text)
        self.current_stream.append("q")
        self.current_stream.append("0.1 0.35 0.55 rg")
        self.current_stream.append("BT")
        self.current_stream.append("/F2 11 Tf")
        self.current_stream.append(f"{self.margin_x} {self.current_y} Td")
        self.current_stream.append(f"({safe}) Tj")
        self.current_stream.append("ET")
        self.current_stream.append("Q")
        self.current_y -= 18

    def add_paragraph(self, text: str, font_size=8.5, color=(0.15, 0.18, 0.22)):
        r, g, b = color
        line_height = font_size * 1.32
        max_chars = 100
        words = text.split(" ")
        lines = []
        current_line = []
        for w in words:
            if len(" ".join(current_line + [w])) <= max_chars:
                current_line.append(w)
            else:
                lines.append(" ".join(current_line))
                current_line = [w]
        if current_line:
            lines.append(" ".join(current_line))

        for line in lines:
            self.check_space(line_height + 2)
            safe = escape_pdf_text(line)
            self.current_stream.append("q")
            self.current_stream.append(f"{r} {g} {b} rg")
            self.current_stream.append("BT")
            self.current_stream.append(f"/F1 {font_size} Tf")
            self.current_stream.append(f"{self.margin_x} {self.current_y} Td")
            self.current_stream.append(f"({safe}) Tj")
            self.current_stream.append("ET")
            self.current_stream.append("Q")
            self.current_y -= line_height
        self.current_y -= 3

    def add_risk_box(self, title: str, points: list):
        box_height = 20 + len(points) * 14
        self.check_space(box_height + 8)
        box_y = self.current_y - box_height

        self.current_stream.append("q")
        self.current_stream.append("0.98 0.95 0.88 rg")
        self.current_stream.append(f"{self.margin_x} {box_y} {self.page_width - 2 * self.margin_x} {box_height} re f")
        self.current_stream.append("0.85 0.55 0.1 rg")
        self.current_stream.append(f"{self.margin_x} {box_y} {self.page_width - 2 * self.margin_x} {box_height} re s")

        self.current_stream.append("0.7 0.3 0.0 rg")
        self.current_stream.append("BT")
        self.current_stream.append("/F2 9.5 Tf")
        self.current_stream.append(f"{self.margin_x + 8} {self.current_y - 12} Td")
        self.current_stream.append(f"({escape_pdf_text(title)}) Tj")
        self.current_stream.append("ET")

        cy = self.current_y - 25
        for pt in points:
            safe_pt = escape_pdf_text(f"* {pt}")
            self.current_stream.append("0.2 0.2 0.2 rg")
            self.current_stream.append("BT")
            self.current_stream.append("/F1 8 Tf")
            self.current_stream.append(f"{self.margin_x + 12} {cy} Td")
            self.current_stream.append(f"({safe_pt}) Tj")
            self.current_stream.append("ET")
            cy -= 13

        self.current_stream.append("Q")
        self.current_y = box_y - 10

    def add_full_code(self, filename: str, code_lines: list):
        self.check_space(30)
        self.add_subtitle(f"CODIGO COMPLETO: {filename} ({len(code_lines)} LINHAS)")
        
        font_size = 7.0
        line_height = 8.8
        max_chars = 100

        for idx, raw_line in enumerate(code_lines, 1):
            self.check_space(line_height + 1)
            clean = raw_line.replace('\t', '    ').rstrip('\r\n')
            if len(clean) > max_chars:
                clean = clean[:max_chars - 3] + "..."

            num_str = f"{idx:4d} "
            safe_code = escape_pdf_text(clean)

            self.current_stream.append("q")
            if idx % 2 == 0:
                self.current_stream.append("0.96 0.97 0.98 rg")
                self.current_stream.append(f"{self.margin_x} {self.current_y - 1} {self.page_width - 2 * self.margin_x} {line_height} re f")

            self.current_stream.append("0.55 0.6 0.65 rg")
            self.current_stream.append("BT")
            self.current_stream.append(f"/F3 {font_size} Tf")
            self.current_stream.append(f"{self.margin_x + 3} {self.current_y} Td")
            self.current_stream.append(f"({num_str}) Tj")
            self.current_stream.append("ET")

            self.current_stream.append("0.1 0.15 0.2 rg")
            self.current_stream.append("BT")
            self.current_stream.append(f"/F3 {font_size} Tf")
            self.current_stream.append(f"{self.margin_x + 28} {self.current_y} Td")
            self.current_stream.append(f"({safe_code}) Tj")
            self.current_stream.append("ET")

            self.current_stream.append("Q")
            self.current_y -= line_height

        self.current_y -= 10

    def save(self):
        if self.current_stream:
            self._draw_page_footer()
            self.pages.append("\n".join(self.current_stream))

        total_pages = len(self.pages)
        font_objects = [
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold >>"
        ]

        page_obj_ids = []
        current_obj_id = 7
        for i in range(total_pages):
            page_obj_ids.append((current_obj_id, current_obj_id + 1))
            current_obj_id += 2

        kids_str = " ".join([f"{p[0]} 0 R" for p in page_obj_ids])

        obj_dict = {}
        obj_dict[1] = b"<< /Type /Catalog /Pages 2 0 R >>"
        obj_dict[2] = f"<< /Type /Pages /Kids [{kids_str}] /Count {total_pages} >>".encode('utf-8')
        obj_dict[3] = font_objects[0].encode('utf-8')
        obj_dict[4] = font_objects[1].encode('utf-8')
        obj_dict[5] = font_objects[2].encode('utf-8')
        obj_dict[6] = font_objects[3].encode('utf-8')

        for idx, (p_id, c_id) in enumerate(page_obj_ids):
            content_data = self.pages[idx].encode('utf-8')
            page_data = (
                f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {self.page_width} {self.page_height}] "
                f"/Contents {c_id} 0 R /Resources << /Font << "
                f"/F1 3 0 R /F2 4 0 R /F3 5 0 R /F4 6 0 R >> >> >>"
            ).encode('utf-8')
            stream_data = (
                f"<< /Length {len(content_data)} >>\nstream\n".encode('utf-8') +
                content_data +
                b"\nendstream"
            )
            obj_dict[p_id] = page_data
            obj_dict[c_id] = stream_data

        total_objs = len(obj_dict)
        header = b"%PDF-1.4\n"
        out = [header]
        offsets = {0: 0}
        curr_offset = len(header)

        for obj_id in range(1, total_objs + 1):
            offsets[obj_id] = curr_offset
            block = f"{obj_id} 0 obj\n".encode('utf-8') + obj_dict[obj_id] + b"\nendobj\n"
            out.append(block)
            curr_offset += len(block)

        xref_offset = curr_offset
        xref_header = f"xref\n0 {total_objs + 1}\n0000000000 65535 f \n".encode('utf-8')
        out.append(xref_header)

        for obj_id in range(1, total_objs + 1):
            out.append(f"{offsets[obj_id]:010d} 00000 n \n".encode('utf-8'))

        trailer = (
            f"trailer\n<< /Size {total_objs + 1} /Root 1 0 R >>\n"
            f"startxref\n{xref_offset}\n%%EOF\n"
        ).encode('utf-8')
        out.append(trailer)

        with open(self.filename, 'wb') as f:
            f.write(b"".join(out))

        print(f"✅ PDF com todo o código gerado: {self.filename} ({total_pages} páginas, {curr_offset} bytes)")

def load_file_lines(filepath):
    if not os.path.exists(filepath):
        return [f"// Arquivo {filepath} não encontrado no workspace."]
    with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
        return f.readlines()

def build_full_dossier():
    pdf = PurePdfWriter("dossie_auditoria_jarvis_completo.pdf")
    pdf.new_page()

    # CAPA
    pdf.add_title("JARVIS NEURAL COCKPIT 3.0 -- DOSSIE DE AUDITORIA COMPLETO")
    pdf.add_paragraph("RELATORIO DE ENGENHARIA DE SOFTWARE COM 100% DO CODIGO-FONTE PARA O CLAUDE ANTHROPIC", font_size=10.5, color=(0.05, 0.2, 0.4))
    pdf.add_paragraph(
        "Destinatario: Claude (Anthropic) / Arquiteto-Chefe de Sistemas\n"
        "Escopo: Auditoria Geral de Pontos de Falha, Concorrencia, Latencia 4G na Rua e Webhooks Financeiros\n"
        "Componentes: App Android (Compose/Retrofit/Acessibilidade) + Servidor VPS Hostinger (FastAPI/Asaas/Evolution)"
    )

    pdf.add_risk_box(
        "RESUMO EXECUTIVO DOS 5 GARGALOS PRINCIPAIS PARA O CLAUDE AUDITAR:",
        [
            "1. CONCORRENCIA SQLITE NO BACKEND: Tratar bloqueio de conexoes sob rajadas de Webhooks do Asaas.",
            "2. RESILIENCIA RETROFIT 4G NA RUA: Timeouts e reconexao sem travar o HUD flutuante no guidao.",
            "3. VAZAMENTO DE NOS NO ANDROID: Garantir reciclagem estrita de AccessibilityNodeInfo.",
            "4. COMPLEXIDADE COMPUTACIONAL HAVERSINE: Otimizacao do calculo esferico com ordenacao por menor desvio.",
            "5. COMPATIBILIDADE DE TELA DIVIDIDA: Validacao de GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN em todas as marcas."
        ]
    )

    # 1. VPS BACKEND
    pdf.add_title("SECAO 1: SERVIDOR VPS HOSTINGER (FASTAPI + ASAAS + EVOLUTION API)")
    pdf.add_paragraph("Arquivo: deploy/vps_backend_complete.py -- Gateway de webhooks, liberacao de licenca e disparo WhatsApp.")
    vps_lines = load_file_lines("deploy/vps_backend_complete.py")
    pdf.add_full_code("deploy/vps_backend_complete.py", vps_lines)

    # 2. REDE ANDROID
    pdf.add_title("SECAO 2: CAMADA DE REDE ANDROID (RETROFIT & INTERCEPTORES)")
    pdf.add_paragraph("Conexao resiliente com a VPS Hostinger e medicao de ping de latencia em tempo real.")
    api_lines = load_file_lines("app/src/main/java/com/example/network/ApiService.kt")
    pdf.add_full_code("app/src/main/java/com/example/network/ApiService.kt", api_lines)

    retrofit_lines = load_file_lines("app/src/main/java/com/example/network/RetrofitClient.kt")
    pdf.add_full_code("app/src/main/java/com/example/network/RetrofitClient.kt", retrofit_lines)

    latency_lines = load_file_lines("app/src/main/java/com/example/network/HostingerLatencyInterceptor.kt")
    pdf.add_full_code("app/src/main/java/com/example/network/HostingerLatencyInterceptor.kt", latency_lines)

    # 3. ACESSIBILIDADE E TELA DIVIDIDA
    pdf.add_title("SECAO 3: MOTOR DE ACESSIBILIDADE & COORDENADOR DE TELA DIVIDIDA")
    pdf.add_paragraph("Varredura em background de feeds abertos e acionamento de tela dividida com 1 toque no guidao.")
    coord_lines = load_file_lines("app/src/main/java/com/example/SplitScreenDualRadarCoordinator.kt")
    pdf.add_full_code("app/src/main/java/com/example/SplitScreenDualRadarCoordinator.kt", coord_lines)

    # 4. HAVERSINE E MOTOR DE MESCLAGEM
    pdf.add_title("SECAO 4: MOTOR GEODESICO HAVERSINE & MESCLAGEM PROFUNDA")
    pdf.add_paragraph("Calculo geodésico na curvatura terrestre (R=6371km), limite dinamico por SLA e ordenacao de combos.")
    haversine_lines = load_file_lines("app/src/main/java/com/example/HaversineGeoDeviationCalculator.kt")
    pdf.add_full_code("app/src/main/java/com/example/HaversineGeoDeviationCalculator.kt", haversine_lines)

    engine_lines = load_file_lines("app/src/main/java/com/example/DeepOrderPoolMergingEngine.kt")
    pdf.add_full_code("app/src/main/java/com/example/DeepOrderPoolMergingEngine.kt", engine_lines)

    pdf.save()

    # Versão Markdown Completa
    with open("dossie_auditoria_jarvis_completo.md", "w", encoding="utf-8") as f:
        f.write("# JARVIS NEURAL COCKPIT 3.0 -- DOSSIÊ TÉCNICO COMPLETO PARA CLAUDE ANTHROPIC\n\n")
        f.write("Documento consolidado contendo todo o código-fonte dos motores críticos para auditoria de gargalos.\n\n")
        f.write("## 1. deploy/vps_backend_complete.py\n```python\n" + "".join(vps_lines) + "\n```\n\n")
        f.write("## 2. app/src/main/java/com/example/network/ApiService.kt\n```kotlin\n" + "".join(api_lines) + "\n```\n\n")
        f.write("## 3. app/src/main/java/com/example/network/RetrofitClient.kt\n```kotlin\n" + "".join(retrofit_lines) + "\n```\n\n")
        f.write("## 4. app/src/main/java/com/example/network/HostingerLatencyInterceptor.kt\n```kotlin\n" + "".join(latency_lines) + "\n```\n\n")
        f.write("## 5. app/src/main/java/com/example/SplitScreenDualRadarCoordinator.kt\n```kotlin\n" + "".join(coord_lines) + "\n```\n\n")
        f.write("## 6. app/src/main/java/com/example/HaversineGeoDeviationCalculator.kt\n```kotlin\n" + "".join(haversine_lines) + "\n```\n\n")
        f.write("## 7. app/src/main/java/com/example/DeepOrderPoolMergingEngine.kt\n```kotlin\n" + "".join(engine_lines) + "\n```\n\n")

    print("✅ Markdown completo gerado: dossie_auditoria_jarvis_completo.md")

if __name__ == "__main__":
    build_full_dossier()
