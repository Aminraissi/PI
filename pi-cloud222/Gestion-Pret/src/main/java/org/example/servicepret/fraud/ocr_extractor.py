"""
fraud/ocr_extractor.py
Extraction de texte OCR pour analyse fraude
(version allégée — réutilise la logique de scoring-service/pipeline/ocr.py)
"""

from pathlib import Path

MIN_CHARS = 30


def extract_text(file_path: str) -> str:
    """
    Extrait le texte d'un PDF ou image.
    Stratégie : texte natif pdfplumber → OCR si insuffisant.
    """
    if not file_path:
        return ""

    path = Path(file_path)
    if not path.exists() or path.stat().st_size == 0:
        return ""

    suffix = path.suffix.lower()

    if suffix == ".pdf":
        text = _native_text(str(path))
        if len(text.strip()) >= MIN_CHARS:
            return text
        return _ocr_pdf(str(path))

    elif suffix in {".jpg", ".jpeg", ".png", ".tiff", ".tif", ".bmp"}:
        return _ocr_image(str(path))

    return ""


def _native_text(pdf_path: str) -> str:
    try:
        import pdfplumber
        with pdfplumber.open(pdf_path) as pdf:
            pages = []
            for page in pdf.pages[:3]:
                t = page.extract_text()
                if t:
                    pages.append(t)
            return "\n\n".join(pages)
    except Exception as e:
        print(f"[OCR] pdfplumber: {e}")
        return ""


def _ocr_pdf(pdf_path: str) -> str:
    try:
        from pdf2image import convert_from_path
        pages = convert_from_path(pdf_path, dpi=200)
        texts = []
        for page in pages[:2]:
            texts.append(_ocr_pil_image(page))
        return "\n\n".join(texts)
    except Exception as e:
        print(f"[OCR] pdf2image: {e}")
        return ""


def _ocr_pil_image(image) -> str:
    try:
        import pytesseract
        config = r"--oem 3 --psm 6"
        results = []
        for lang in ("fra", "ara", "eng"):
            try:
                t = pytesseract.image_to_string(image, lang=lang, config=config)
                results.append(t)
            except Exception:
                pass
        return max(results, key=lambda x: len(x.strip()), default="")
    except Exception:
        return ""


def _ocr_image(image_path: str) -> str:
    try:
        from PIL import Image
        img = Image.open(image_path)
        return _ocr_pil_image(img)
    except Exception:
        return ""
