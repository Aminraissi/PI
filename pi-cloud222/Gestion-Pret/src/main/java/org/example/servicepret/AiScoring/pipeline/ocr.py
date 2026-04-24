"""
pipeline/ocr.py — Lecture OCR des documents
Stratégie :
  1. pdfplumber  → PDF avec texte natif (rapide, fiable)
  2. pdf2image + tesseract → PDF scanné (images)
  3. pdf2image + easyocr   → fallback si tesseract absent
  4. Lecture image directe  → jpg/png/tiff
"""

import os
import re
from pathlib import Path

# ── Imports optionnels ────────────────────────────────────────────────────────

try:
    import pdfplumber
    PDFPLUMBER_OK = True
except ImportError:
    PDFPLUMBER_OK = False
    print("[OCR] pdfplumber non installé — pip install pdfplumber")

try:
    from pdf2image import convert_from_path
    PDF2IMAGE_OK = True
except ImportError:
    PDF2IMAGE_OK = False
    print("[OCR] pdf2image non installé — pip install pdf2image")

try:
    import pytesseract
    from PIL import Image
    TESSERACT_OK = True
except ImportError:
    TESSERACT_OK = False
    print("[OCR] pytesseract non installé — pip install pytesseract pillow")

try:
    import easyocr
    EASYOCR_OK = True
    _easyocr_reader = None
except ImportError:
    EASYOCR_OK = False

# Seuil minimum de caractères pour considérer qu'un OCR a réussi
MIN_TEXT_LENGTH = 30


# ── EasyOCR singleton ─────────────────────────────────────────────────────────

def _get_easyocr_reader():
    global _easyocr_reader
    if _easyocr_reader is None:
        _easyocr_reader = easyocr.Reader(['ar', 'fr', 'en'], gpu=False)
    return _easyocr_reader


# ── OCR image ─────────────────────────────────────────────────────────────────

def _ocr_image_tesseract(image) -> str:
    """
    OCR d'une image PIL avec tesseract.
    Essaie arabe, français, et les deux combinés.
    Retourne le texte le plus riche.
    """
    config = r'--oem 3 --psm 6'
    candidates = []

    # Langue arabe
    try:
        t = pytesseract.image_to_string(image, lang='ara', config=config)
        candidates.append(t)
    except Exception:
        pass

    # Langue française
    try:
        t = pytesseract.image_to_string(image, lang='fra', config=config)
        candidates.append(t)
    except Exception:
        pass

    # Combinaison ara+fra (si les deux packs sont installés)
    try:
        t = pytesseract.image_to_string(image, lang='ara+fra', config=config)
        candidates.append(t)
    except Exception:
        pass

    # Fallback anglais si rien d'autre
    if not any(len(c.strip()) > MIN_TEXT_LENGTH for c in candidates):
        try:
            t = pytesseract.image_to_string(image, config=config)
            candidates.append(t)
        except Exception:
            pass

    # Retourne le plus long (heuristique : plus de texte = meilleure lecture)
    return max(candidates, key=lambda x: len(x.strip()), default="")


def _ocr_image_easyocr(image_path: str) -> str:
    """OCR d'une image avec easyocr (arabe + français + anglais)."""
    reader = _get_easyocr_reader()
    results = reader.readtext(image_path, detail=0, paragraph=True)
    return '\n'.join(results)


# ── Lecture PDF ───────────────────────────────────────────────────────────────

def _read_pdf_native(pdf_path: str) -> str:
    """
    Tente d'extraire le texte natif du PDF avec pdfplumber.
    Retourne "" si le PDF est scanné (pas de texte intégré).
    """
    if not PDFPLUMBER_OK:
        return ""
    try:
        with pdfplumber.open(pdf_path) as pdf:
            pages_text = []
            for page in pdf.pages:
                t = page.extract_text()
                if t:
                    pages_text.append(t.strip())
            full_text = "\n\n".join(pages_text)
        return full_text
    except Exception as e:
        print(f"[OCR] pdfplumber erreur sur {pdf_path}: {e}")
        return ""


def _read_pdf_ocr(pdf_path: str, dpi: int = 300) -> str:
    """
    Convertit le PDF en images puis applique l'OCR page par page.
    Utilisé pour les PDFs scannés sans texte natif.
    """
    if not PDF2IMAGE_OK:
        raise ImportError("pdf2image requis pour lire les PDFs scannés — pip install pdf2image")

    pages = convert_from_path(pdf_path, dpi=dpi)
    texts = []

    for i, page in enumerate(pages):
        print(f"[OCR] Page {i+1}/{len(pages)} de {Path(pdf_path).name}...")

        if TESSERACT_OK:
            text = _ocr_image_tesseract(page)
        elif EASYOCR_OK:
            tmp = f"/tmp/_ocr_page_{i}.png"
            page.save(tmp)
            text = _ocr_image_easyocr(tmp)
            try:
                os.remove(tmp)
            except Exception:
                pass
        else:
            raise ImportError("Installer pytesseract ou easyocr pour lire les PDFs scannés")

        texts.append(text)

    if not texts:
        return ""

    if len(texts) == 1:
        return texts[0]

    # Concaténation multi-pages avec séparateurs
    parts = []
    for i, t in enumerate(texts):
        parts.append(f"--- PAGE {i+1} ---\n{t}")
    return "\n\n".join(parts)


def read_pdf(pdf_path: str, dpi: int = 300) -> str:
    """
    Lit un PDF avec la meilleure stratégie disponible :
      1. Texte natif via pdfplumber (PDF numérique)
      2. OCR via pdf2image + tesseract/easyocr (PDF scanné)
    """
    # Étape 1 : essayer d'extraire le texte natif
    native_text = _read_pdf_native(pdf_path)

    if len(native_text.strip()) >= MIN_TEXT_LENGTH:
        print(f"[OCR] {Path(pdf_path).name} → texte natif ({len(native_text)} chars)")
        return native_text

    # Étape 2 : PDF scanné → OCR image
    print(f"[OCR] {Path(pdf_path).name} → texte natif insuffisant ({len(native_text.strip())} chars), passage en OCR image...")
    ocr_text = _read_pdf_ocr(pdf_path, dpi=dpi)
    print(f"[OCR] {Path(pdf_path).name} → OCR image terminé ({len(ocr_text)} chars)")
    return ocr_text


# ── Lecture image directe ─────────────────────────────────────────────────────

def read_image(image_path: str) -> str:
    """OCR directement sur une image (jpg, png, tiff)."""
    if TESSERACT_OK:
        img = Image.open(image_path)
        return _ocr_image_tesseract(img)
    elif EASYOCR_OK:
        return _ocr_image_easyocr(image_path)
    else:
        raise ImportError("Installer pytesseract ou easyocr")


# ── Point d'entrée principal ──────────────────────────────────────────────────

def extract_text_from_document(file_path: str) -> str:
    """
    Point d'entrée principal.
    Détecte automatiquement PDF vs image et applique la bonne stratégie.
    Retourne le texte brut du document, ou "" en cas d'échec.
    """
    if file_path is None:
        return ""

    path = Path(file_path)

    if not path.exists():
        print(f"[OCR] Fichier introuvable: {file_path}")
        return ""

    if path.stat().st_size == 0:
        print(f"[OCR] Fichier vide: {file_path}")
        return ""

    suffix = path.suffix.lower()

    try:
        if suffix == '.pdf':
            return read_pdf(str(path))
        elif suffix in {'.jpg', '.jpeg', '.png', '.tiff', '.tif', '.bmp'}:
            return read_image(str(path))
        else:
            print(f"[OCR] Format non supporté: {suffix}")
            return ""
    except Exception as e:
        print(f"[OCR] Erreur inattendue sur {file_path}: {e}")
        return ""


def read_all_documents(doc_paths: dict) -> dict:
    """
    Applique l'OCR sur chaque document du dossier.

    Paramètre:
        doc_paths: {
            'cin':             '/tmp/xxx_cin.pdf',
            'domicile':        '/tmp/xxx_facture.pdf',
            'releves':         '/tmp/xxx_releves.pdf',
            'projet':          '/tmp/xxx_projet.pdf',
            'titre_foncier':   '/tmp/xxx_titre.pdf',
            'carte_agri':      '/tmp/xxx_carte.pdf',
            'non_endettement': None,
            'assurance':       '/tmp/xxx_assurance.pdf',
            'garantie':        None,
        }

    Retourne:
        {nom_doc: texte_brut}  — "" pour les absents/échecs, jamais None
    """
    results = {}
    for doc_name, file_path in doc_paths.items():
        if file_path is None:
            print(f"[OCR] {doc_name}: absent (None)")
            results[doc_name] = ""
            continue
        try:
            text = extract_text_from_document(file_path)
            char_count = len(text.strip())
            print(f"[OCR] {doc_name}: {char_count} chars extraits")
            if char_count < MIN_TEXT_LENGTH:
                print(f"[OCR] ⚠ {doc_name}: texte trop court ({char_count} < {MIN_TEXT_LENGTH}), document peut-être illisible")
            results[doc_name] = text
        except Exception as e:
            print(f"[OCR] Erreur sur {doc_name} ({file_path}): {e}")
            results[doc_name] = ""
    return results


# ── Test rapide ───────────────────────────────────────────────────────────────

if __name__ == '__main__':
    import sys
    if len(sys.argv) < 2:
        print("Usage: python ocr.py <chemin_document>")
        sys.exit(1)
    text = extract_text_from_document(sys.argv[1])
    print(f"\n[RÉSULTAT] {len(text)} caractères extraits\n")
    print(text[:3000])