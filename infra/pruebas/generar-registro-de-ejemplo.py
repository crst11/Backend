#!/usr/bin/env python3
"""Genera el PDF de ejemplo del Registro Academico Extendido que usan las pruebas de la API (SCRUM-234).

Los datos son los FICTICIOS de LectorDeRegistroExtendidoTest (ningun reporte real entra al repositorio, Ley 1581 de 2012). El PDF es
minimo: texto en Helvetica, una linea por renglon, que es lo que PDFBox necesita para devolver el mismo texto que la prueba del lector.

    python3 infra/pruebas/generar-registro-de-ejemplo.py   (desde la raiz del Backend)
"""
import pathlib
import re

RAIZ = pathlib.Path(__file__).resolve().parents[2]
PRUEBA = RAIZ / "src/test/java/co/edu/ucundinamarca/cundiapp/infrastructure/adapter/out/importing/LectorDeRegistroExtendidoTest.java"
SALIDA = RAIZ / "docs/postman/ejemplos/registro_academico_extendido_de_ejemplo.pdf"
LINEAS_POR_PAGINA = 55


def texto_del_reporte() -> list[str]:
    fuente = PRUEBA.read_text(encoding="utf-8")
    bloque = re.search(r'REPORTE = """\n(.*?)""";', fuente, re.S).group(1)
    lineas = bloque.split("\n")
    sangria = min(len(l) - len(l.lstrip("\t ")) for l in lineas if l.strip())
    return [l[sangria:] for l in lineas if l.strip()]


def escapar(linea: str) -> bytes:
    barra = bytes([92])  # la barra invertida, que en un PDF escapa a los parentesis
    crudo = linea.encode("cp1252")
    for caracter in (barra, b"(", b")"):
        crudo = crudo.replace(caracter, barra + caracter)
    return crudo


def construir(lineas: list[str]) -> bytes:
    paginas = [lineas[i:i + LINEAS_POR_PAGINA] for i in range(0, len(lineas), LINEAS_POR_PAGINA)]
    objetos: list[bytes] = []  # el objeto n va en la posicion n-1
    n_paginas = len(paginas)
    # 1 catalogo, 2 arbol de paginas, 3 fuente, luego (pagina, contenido) por cada pagina
    ids_pagina = [4 + 2 * i for i in range(n_paginas)]
    objetos.append(b"<< /Type /Catalog /Pages 2 0 R >>")
    kids = b" ".join(f"{i} 0 R".encode() for i in ids_pagina)
    objetos.append(b"<< /Type /Pages /Kids [" + kids + b"] /Count " + str(n_paginas).encode() + b" >>")
    objetos.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
    for i, pagina in enumerate(paginas):
        contenido = b"BT /F1 9 Tf 12 TL 40 800 Td\n" + b"".join(b"(" + escapar(l) + b") Tj T*\n" for l in pagina) + b"ET"
        objetos.append(
            b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents "
            + str(5 + 2 * i).encode() + b" 0 R >>")
        objetos.append(b"<< /Length " + str(len(contenido)).encode() + b" >>\nstream\n" + contenido + b"\nendstream")
    salida = bytearray(b"%PDF-1.4\n")
    posiciones = []
    for n, cuerpo in enumerate(objetos, start=1):
        posiciones.append(len(salida))
        salida += f"{n} 0 obj\n".encode() + cuerpo + b"\nendobj\n"
    inicio_xref = len(salida)
    salida += f"xref\n0 {len(objetos) + 1}\n".encode() + b"0000000000 65535 f \n"
    for pos in posiciones:
        salida += f"{pos:010d} 00000 n \n".encode()
    salida += f"trailer\n<< /Size {len(objetos) + 1} /Root 1 0 R >>\nstartxref\n{inicio_xref}\n%%EOF\n".encode()
    return bytes(salida)


if __name__ == "__main__":
    SALIDA.write_bytes(construir(texto_del_reporte()))
    print(f"Escrito {SALIDA.relative_to(RAIZ)} ({SALIDA.stat().st_size} bytes)")
