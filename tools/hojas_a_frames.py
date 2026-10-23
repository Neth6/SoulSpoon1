#!/usr/bin/env python3
"""
Convierte las hojas de animacion (animaciones1.zip / animaciones2.zip) en carpetas de fotogramas
para tools/gif_to_wheel.py.  NO toca tu carpeta de arte original: crea una nueva.

Uso (desde la raiz del repo):
    1) descomprime animaciones1.zip y animaciones2.zip en UNA misma carpeta, ej. E:\\spoon\\hojas
    2) python tools/hojas_a_frames.py /e/spoon/hojas /e/spoon/arte_hojas --base /e/spoon/arte_ruleta/textures
    3) python tools/gif_to_wheel.py /e/spoon/arte_hojas src/main/resources --color-frames 250

 --base   carpeta de arte actual: lo que NO venga en las hojas (death, gui...) se copia de ahi
 --step   milisegundos por fotograma (50 = 20 por segundo, el maximo que da Minecraft)
Se respeta la duracion real de cada fotograma (las pausas largas de reviil/nutria se mantienen).
"""
import argparse, json, math, os, shutil, sys
from PIL import Image

# hoja -> carpeta de arte (nombres que entiende gif_to_wheel.py)
FOLDER = {"ruleta_rojo": "roja", "ruleta_morado": "morada", "ruleta_verde": "verde",
          "ruleta_amarillo": "amarilla", "ruleta_celeste": "celeste", "ruleta_azul": "azul",
          "ruleta_naranja": "naranja", "ruleta_rosa": "rosa", "nutria": "nutria",
          "nutria_rosa": "nutria_rosa", "reviil": "reviil", "muerte": "muerte",
          "criatura_roja": "criatura_roja"}

def find_sheet(root, name, i):
    target = f"{name}_{i}.png"
    for r, _, files in os.walk(root):
        if target in files:
            return os.path.join(r, target)
    return None

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("hojas"); ap.add_argument("salida")
    ap.add_argument("--json", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "hojas_json"))
    ap.add_argument("--base", default=None)
    ap.add_argument("--step", type=int, default=50)
    ap.add_argument("--max", type=int, default=255, help="maximo de fotogramas por animacion")
    a = ap.parse_args()

    os.makedirs(a.salida, exist_ok=True)
    if a.base and os.path.isdir(a.base):
        for d in os.listdir(a.base):
            src, dst = os.path.join(a.base, d), os.path.join(a.salida, d)
            if os.path.isdir(src) and not os.path.exists(dst) and d not in FOLDER.values():
                shutil.copytree(src, dst)
                print(f"copiado de tu arte: {d}")

    for name, folder in FOLDER.items():
        jp = os.path.join(a.json, name + ".json")
        if not os.path.exists(jp):
            print(f"-- {name}: falta {jp}"); continue
        meta = json.load(open(jp, encoding="utf-8"))
        w, h, cols, per = meta["ancho"], meta["alto"], meta["columnas"], meta["por_hoja"]
        dur = meta["duraciones"]; total = sum(dur)
        step = max(a.step, math.ceil(total / a.max))
        sheets = {}
        out = os.path.join(a.salida, folder)
        shutil.rmtree(out, ignore_errors=True); os.makedirs(out)
        # tiempo de inicio de cada fotograma original
        starts, t = [], 0
        for d in dur:
            starts.append(t); t += d
        n, k, ok = 0, 0, True
        for tt in range(0, total, step):
            while k + 1 < len(starts) and starts[k + 1] <= tt:
                k += 1
            sh, idx = divmod(k, per)
            if sh not in sheets:
                path = find_sheet(a.hojas, name, sh)
                if not path:
                    print(f"!! {name}: no encuentro {name}_{sh}.png"); ok = False; break
                sheets[sh] = Image.open(path).convert("RGBA")
            col, row = idx % cols, idx // cols
            fr = sheets[sh].crop((col * w, row * h, (col + 1) * w, (row + 1) * h))
            fr.save(os.path.join(out, f"frame_{n:04d}.png")); n += 1
        if ok:
            print(f"{folder:14s} {len(dur)} fotogramas originales, {total / 1000:.1f} s -> {n} fotogramas ({step} ms c/u)")
    print("\nListo. Ahora: python tools/gif_to_wheel.py", a.salida, "src/main/resources --color-frames 250")

main()
