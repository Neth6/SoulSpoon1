import numpy as np, sys, os
from PIL import Image
from scipy import ndimage
src, outdir = sys.argv[1], sys.argv[2]
im = Image.open(src).convert('RGB'); a = np.asarray(im).astype(float)
H, W, _ = a.shape
dx = np.abs(np.diff(a, axis=1)).sum(axis=(0, 2)); dy = np.abs(np.diff(a, axis=0)).sum(axis=(1, 2))
def fit(d):
    best = (0, 33, 0)
    for p in np.arange(31, 35, 0.05):
        for off in np.arange(0, p, 0.5):
            idx = np.arange(off, len(d), p).astype(int)
            m = d[idx].sum() / len(idx)
            if m > best[0]: best = (m, p, off)
    return best[1], best[2]
px, ox = fit(dx); py, oy = fit(dy)
nx, ny = int((W - ox) // px), int((H - oy) // py)
cells = np.zeros((ny, nx, 3))
for j in range(ny):
    for i in range(nx):
        x0 = int(ox + (i + .25) * px) + 1; x1 = int(ox + (i + .75) * px) + 1
        y0 = int(oy + (j + .25) * py) + 1; y1 = int(oy + (j + .75) * py) + 1
        cells[j, i] = np.median(a[y0:y1, x0:x1].reshape(-1, 3), axis=0)
mx = cells.max(axis=2); mn = cells.min(axis=2)
bg = (mn > 185) & ((mx - mn) < 45)
fg = ~bg
fg = ndimage.binary_opening(fg, structure=np.ones((2, 2)))
lab, n = ndimage.label(fg, structure=np.ones((3, 3)))
os.makedirs(outdir, exist_ok=True)
objs = ndimage.find_objects(lab)
pieces = []
for k, sl in enumerate(objs, 1):
    m = lab[sl] == k
    if m.sum() < 20: continue
    rgba = np.zeros(m.shape + (4,), dtype=np.uint8)
    rgba[..., :3] = cells[sl].astype(np.uint8); rgba[..., 3] = (m * 255).astype(np.uint8)
    pieces.append((sl[1].start, sl[0].start, rgba))
pieces.sort(key=lambda t: (t[1] // 8, t[0]))
print(nx, ny, px, py, len(pieces))
for idx, (x, y, rgba) in enumerate(pieces):
    h, w = rgba.shape[:2]
    s = max(w, h); s = s + (s % 2)
    c = np.zeros((s, s, 4), dtype=np.uint8)
    c[(s - h) // 2:(s - h) // 2 + h, (s - w) // 2:(s - w) // 2 + w] = rgba
    Image.fromarray(c).save(os.path.join(outdir, 'pieza_%d_x%d_y%d.png' % (idx, x, y)))
    print(idx, 'cell pos', x, y, 'size', w, h, '->', s)
